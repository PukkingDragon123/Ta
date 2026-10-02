package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.MusicCrystalBlockEntity;
import com.thesift.entity.caravan.Caravan;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * C: the Caravans Cavern - the Caravan, the music crystal's block entity, Prism armour and the song
 * hooks: the Crystal Hymn calms colonies, and every note played heals whoever wears Prism.
 * Registered from one line in {@link TheSift}.
 */
public final class ModCaravans {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Caravan>> CARAVAN = ENTITIES.registerEntityType("caravan", Caravan::new,
            MobCategory.MONSTER, b -> b.sized(0.8F, 0.55F).eyeHeight(0.35F).clientTrackingRange(10));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MusicCrystalBlockEntity>> MUSIC_CRYSTAL_ENTITY = BLOCK_ENTITIES.register(
            "music_crystal", () -> new BlockEntityType<>(MusicCrystalBlockEntity::new, ModBlocks.MUSIC_CRYSTAL.get()));

    public static final ResourceKey<Biome> CAVERN = ResourceKey.create(Registries.BIOME, TheSift.id("caravans_cavern"));
    /** Natural ore a Caravan worker will dig out, and the rock it must be embedded in. */
    public static final TagKey<Block> MINABLE = TagKey.create(Registries.BLOCK, TheSift.id("caravan_minable"));
    public static final TagKey<Block> ROCK = TagKey.create(Registries.BLOCK, TheSift.id("caravan_rock"));
    public static final TagKey<Item> PRISM_ARMOR_ITEMS = TagKey.create(Registries.ITEM, TheSift.id("prism_armor"));
    public static final TagKey<Item> REPAIRS_PRISM_ARMOR = TagKey.create(Registries.ITEM, TheSift.id("repairs_prism_armor"));

    public static final ResourceKey<EquipmentAsset> PRISM_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, TheSift.id("prism"));
    /** A little tougher than diamond (between diamond and netherite), very enchantable, and it heals you to music. */
    public static final ArmorMaterial PRISM_ARMOR = new ArmorMaterial(35,
            Map.of(ArmorType.BOOTS, 3, ArmorType.LEGGINGS, 6, ArmorType.CHESTPLATE, 8, ArmorType.HELMET, 3, ArmorType.BODY, 11),
            22, SoundEvents.ARMOR_EQUIP_DIAMOND, 2.5F, 0.05F, REPAIRS_PRISM_ARMOR, PRISM_ASSET);

    private static final double PRISM_HEARING = 12.0;
    private static final double HYMN_REACH = 32.0;

    private ModCaravans() {
    }

    public static void register(IEventBus modBus) {
        ENTITIES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModCaravans::attributes);
        modBus.addListener(ModCaravans::spawnPlacements);
        NeoForge.EVENT_BUS.addListener(ModCaravans::onBreak);
        SongEvents.listenSongs(ModCaravans::onSong);
        SongEvents.listenNotes(ModCaravans::onNote);
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(CARAVAN.get(), Caravan.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(CARAVAN.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaravans::checkCaravan,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Caravans spawn in their own crystal light (the colony glows): no darkness check, only solid ground. */
    private static boolean checkCaravan(EntityType<Caravan> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    /** Breaking a music crystal (or anything of the colony) where Caravans can see brings the swarm. */
    private static void onBreak(BreakBlockEvent event) {
        if (event.getLevel() instanceof ServerLevel server && event.getState().is(ModBlocks.MUSIC_CRYSTAL.get()) && !event.getPlayer().isCreative()) {
            Caravan.alarm(server, event.getPos(), event.getPlayer());
        }
    }

    /** The Crystal Hymn calms every Caravan that hears it. */
    private static void onSong(ServerLevel level, @org.jspecify.annotations.Nullable Player player, Vec3 at, Song song) {
        if (song != Song.CRYSTAL) {
            return;
        }
        boolean any = false;
        for (Caravan c : level.getEntitiesOfClass(Caravan.class, new AABB(at, at).inflate(HYMN_REACH))) {
            c.calm(2400 + level.getRandom().nextInt(1200));
            any = true;
        }
        if (any && player != null) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("message.thesift.caravan.calm"));
        }
    }

    /** Prism armour drinks in music: two pieces or more heal the wearer while notes ring nearby; the full set heals twice as fast. */
    private static void onNote(ServerLevel level, @org.jspecify.annotations.Nullable Player player, Vec3 at, int pitch) {
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(PRISM_HEARING))) {
            int pieces = 0;
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                if (p.getItemBySlot(slot).is(PRISM_ARMOR_ITEMS)) {
                    pieces++;
                }
            }
            if (pieces < 2 || p.getHealth() >= p.getMaxHealth()) {
                continue;
            }
            MobEffectInstance regen = p.getEffect(MobEffects.REGENERATION);
            if (regen == null || regen.getDuration() < 30) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, pieces == 4 ? 1 : 0, true, false, true));
            }
        }
    }
}
