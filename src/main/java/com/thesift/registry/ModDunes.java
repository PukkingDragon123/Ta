package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.dunes.Grub;
import com.thesift.entity.dunes.Jaberora;
import com.thesift.entity.dunes.Kerkorer;
import com.thesift.entity.dunes.MonarchReservoir;
import com.thesift.entity.dunes.Reservoir;
import com.thesift.music.Instrument;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * P4-DESERT: the creatures of the Rocky Dunes - the Kerkorer (a giant camouflaged chameleon that baits prey with
 * treasure on its tongue), the Jaberora (a singing kangaroo-rat that lives with it), the Reservoir and the Monarch
 * Reservoir (worm-cacti full of Chrome) and the Grubs (gem creatures in rock shells that music cracks open) -
 * with their sounds, spawn rules, band voices, the ears of the Grubs, and the Kerkorer Cloak (stand still in it
 * and nothing can find you).
 */
public final class ModDunes {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Kerkorer>> KERKORER = ENTITIES.registerEntityType("kerkorer", Kerkorer::new,
            MobCategory.CREATURE, b -> b.sized(1.4F, 1.3F).eyeHeight(1.05F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Jaberora>> JABERORA = ENTITIES.registerEntityType("jaberora", Jaberora::new,
            MobCategory.CREATURE, b -> b.sized(0.45F, 0.7F).eyeHeight(0.55F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Reservoir>> RESERVOIR = ENTITIES.registerEntityType("reservoir", Reservoir::new,
            MobCategory.CREATURE, b -> b.sized(0.95F, 2.1F).eyeHeight(1.9F).clientTrackingRange(10));
    /** The huge friendly Reservoir: the same creature, its model drawn 4.4 times as big. */
    public static final DeferredHolder<EntityType<?>, EntityType<MonarchReservoir>> MONARCH_RESERVOIR = ENTITIES.registerEntityType("monarch_reservoir",
            MonarchReservoir::new, MobCategory.CREATURE, b -> b.sized(3.6F, 6.8F).eyeHeight(6.2F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<Grub>> GRUB = ENTITIES.registerEntityType("grub", Grub::new,
            MobCategory.MONSTER, b -> b.sized(0.55F, 0.45F).eyeHeight(0.3F).clientTrackingRange(8));

    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_KER = reg("entity.kerkorer.ker");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_KO = reg("entity.kerkorer.ko");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_RER = reg("entity.kerkorer.rer");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_SNAP = reg("entity.kerkorer.snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_TONGUE = reg("entity.kerkorer.tongue");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_LURE = reg("entity.kerkorer.lure");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_SHIFT = reg("entity.kerkorer.shift");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_HISS = reg("entity.kerkorer.hiss");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_HURT = reg("entity.kerkorer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_DEATH = reg("entity.kerkorer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> KERKORER_STEP = reg("entity.kerkorer.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_CHIRP = reg("entity.jaberora.chirp");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_SING = reg("entity.jaberora.sing");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_PULSE = reg("entity.jaberora.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_HOP = reg("entity.jaberora.hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_EAT = reg("entity.jaberora.eat");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_SNORE = reg("entity.jaberora.snore");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_HURT = reg("entity.jaberora.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> JABERORA_DEATH = reg("entity.jaberora.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_AMBIENT = reg("entity.reservoir.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_GULP = reg("entity.reservoir.gulp");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_DRAIN = reg("entity.reservoir.drain");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_BURST = reg("entity.reservoir.burst");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_SWELL = reg("entity.reservoir.swell");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_BLOOM = reg("entity.reservoir.bloom");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_HUM = reg("entity.reservoir.hum");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_HURT = reg("entity.reservoir.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> RESERVOIR_DEATH = reg("entity.reservoir.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_AMBIENT = reg("entity.grub.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_STEP = reg("entity.grub.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_CREAK = reg("entity.grub.creak");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_CRACK = reg("entity.grub.crack");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_SHINE = reg("entity.grub.shine");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_DRINK = reg("entity.grub.drink");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_HURT = reg("entity.grub.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GRUB_DEATH = reg("entity.grub.death");

    /** What a Jaberora eats (and is tamed with): the Tuning Fruit (tools/wland.py fills the tag). */
    public static final TagKey<Item> JABERORA_FOOD = TagKey.create(Registries.ITEM, TheSift.id("jaberora_food"));
    public static final TagKey<Item> REPAIRS_CLOAK = TagKey.create(Registries.ITEM, TheSift.id("repairs_kerkorer_cloak"));
    public static final ResourceKey<EquipmentAsset> CLOAK_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, TheSift.id("kerkorer_cloak"));
    /** The Kerkorer Cloak: hardly armour at all (leather's chest), but stand still in it and you vanish. */
    public static final ArmorMaterial CLOAK_MATERIAL = new ArmorMaterial(12,
            Map.of(ArmorType.BOOTS, 1, ArmorType.LEGGINGS, 2, ArmorType.CHESTPLATE, 3, ArmorType.HELMET, 1, ArmorType.BODY, 3),
            15, SoundEvents.ARMOR_EQUIP_LEATHER, 0.0F, 0.0F, REPAIRS_CLOAK, CLOAK_ASSET);
    /** Ticks a cloaked player must stand still before they vanish. */
    public static final int CLOAK_STILL = 20;
    private static final Map<Player, Vec3> CLOAK_LAST = new WeakHashMap<>();
    private static final Map<Player, Integer> CLOAK_STILL_TICKS = new WeakHashMap<>();

    private ModDunes() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(ModDunes::attributes);
        bus.addListener(ModDunes::spawnPlacements);
        NeoForge.EVENT_BUS.addListener(ModDunes::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ModDunes::onChangeTarget);
        // music near a Grub cracks its rocky shell, a little with every note
        SongEvents.listenNotes((level, player, at, pitch) -> {
            for (Grub g : level.getEntitiesOfClass(Grub.class, new AABB(at, at).inflate(Grub.HEARING), Grub::isAlive)) {
                g.hearNote(level, at);
            }
        });
        // band voices: the Jaberora's soaring aria, the Reservoir's Chrome gurgle, the Monarch's drone, a cracked Grub's gem chimes,
        // and the Kerkorer's croak (it only ever plays for a song, from where it lies)
        BandRegistry.voice(JABERORA, SoundEvents.NOTE_BLOCK_FLUTE).transpose(12).volume(0.9F)
                .layer(JABERORA_SING, 0.35F)
                .families(Instrument.Family.FLUTE, Instrument.Family.STRINGS).songs(Song.LULLABY)
                .temper(BandVoice.Temper.SHY).instrument("aria").colour(0xF08F98)
                .bond((mob, player) -> mob instanceof Jaberora j && j.isTame()
                        ? (j.isOwnedBy(player) ? BandVoice.Bond.OWN : BandVoice.Bond.OTHER) : BandVoice.Bond.WILD)
                .when(m -> m instanceof Jaberora j && !j.isNapping() && !j.isOrderedToSit()).register();
        BandRegistry.voice(RESERVOIR, SoundEvents.NOTE_BLOCK_DIDGERIDOO).volume(0.8F)
                .layer(SoundEvents.BUBBLE_COLUMN_BUBBLE_POP, 0.3F).every(2)
                .families(Instrument.Family.DRUM, Instrument.Family.FLUTE)
                .temper(BandVoice.Temper.SHY).instrument("chrome_gurgle").colour(0x5F8AD4).register();
        BandRegistry.voice(MONARCH_RESERVOIR, SoundEvents.NOTE_BLOCK_DIDGERIDOO).transpose(-12).volume(1.2F)
                .layer(SoundEvents.NOTE_BLOCK_BASS, 0.5F).every(2)
                .families(Instrument.Family.DRUM, Instrument.Family.FLUTE, Instrument.Family.STRINGS, Instrument.Family.CHIMES)
                .temper(BandVoice.Temper.EAGER).instrument("monarch_drone").colour(0xE4A25E).register();
        BandRegistry.voice(GRUB, SoundEvents.NOTE_BLOCK_CHIME).transpose(12).volume(0.7F)
                .layer(SoundEvents.AMETHYST_CLUSTER_HIT, 0.3F)
                .families(Instrument.Family.CHIMES)
                .temper(BandVoice.Temper.SHY).instrument("gem_chimes").colour(0xB48CF0)
                .when(m -> m instanceof Grub g && g.isCracked()).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(KERKORER.get(), Kerkorer.createAttributes().build());
        event.put(JABERORA.get(), Jaberora.createAttributes().build());
        event.put(RESERVOIR.get(), Reservoir.createAttributes().build());
        event.put(MONARCH_RESERVOIR.get(), MonarchReservoir.createMonarchAttributes().build());
        event.put(GRUB.get(), Grub.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(KERKORER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModDunes::checkKerkorer,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(JABERORA.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModDunes::checkJaberora,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(RESERVOIR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModDunes::checkReservoir,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MONARCH_RESERVOIR.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModDunes::checkMonarch,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(GRUB.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModDunes::checkGrub,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** The dunes' ground: Chime Sand, Suspicious Chime Sand, Chime Sandstone - or any sand. */
    public static boolean dunesGround(BlockState state) {
        return state.is(ModBlocks.CHIME_SAND.get()) || state.is(ModBlocks.SUSPICIOUS_CHIME_SAND.get()) || state.is(ModBlocks.CHIME_SANDSTONE.get())
                || state.is(net.minecraft.tags.BlockTags.SAND);
    }

    private static boolean onDunes(ServerLevelAccessor level, BlockPos pos) {
        return dunesGround(level.getBlockState(pos.below()));
    }

    /** Kerkorers keep their own stretch of the dunes: never another within 32 blocks. */
    private static boolean checkKerkorer(EntityType<Kerkorer> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return onDunes(level, pos) && random.nextInt(3) == 0 && level.getEntitiesOfClass(Kerkorer.class, new AABB(pos).inflate(32.0)).isEmpty();
    }

    private static boolean checkJaberora(EntityType<Jaberora> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return EntitySpawnReason.isSpawner(reason) || onDunes(level, pos);
    }

    private static boolean checkReservoir(EntityType<Reservoir> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return EntitySpawnReason.isSpawner(reason) || onDunes(level, pos);
    }

    /** The Monarch is rare and alone: one in six tries, never another within 128 blocks. */
    private static boolean checkMonarch(EntityType<MonarchReservoir> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return onDunes(level, pos) && random.nextInt(6) == 0
                && level.getEntitiesOfClass(MonarchReservoir.class, new AABB(pos).inflate(128.0)).isEmpty();
    }

    /** Grubs come out of the sand at night, and now and then by day. */
    private static boolean checkGrub(EntityType<Grub> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        if (level.getDifficulty() == net.minecraft.world.Difficulty.PEACEFUL || !onDunes(level, pos)) {
            return false;
        }
        return level.getLevel().isDarkOutside() || random.nextInt(4) == 0;
    }

    // ------------------------------------------------------------------ the Kerkorer Cloak

    /** True while a player wears the cloak and has stood still long enough to vanish. */
    public static boolean hidden(LivingEntity e) {
        return e instanceof Player p && p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.KERKORER_CLOAK.get())
                && CLOAK_STILL_TICKS.getOrDefault(p, 0) >= CLOAK_STILL;
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level)) {
            return;
        }
        if (!p.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.KERKORER_CLOAK.get())) {
            CLOAK_STILL_TICKS.remove(p);
            CLOAK_LAST.remove(p);
            return;
        }
        Vec3 last = CLOAK_LAST.get(p);
        Vec3 now = p.position();
        boolean still = last != null && last.distanceToSqr(now) < 1.0E-4 && !p.swinging && !p.isUsingItem();
        CLOAK_LAST.put(p, now);
        int ticks = still ? CLOAK_STILL_TICKS.getOrDefault(p, 0) + 1 : 0;
        CLOAK_STILL_TICKS.put(p, ticks);
        if (ticks < CLOAK_STILL) {
            return;
        }
        if (ticks == CLOAK_STILL) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.WHITE_ASH, p.getX(), p.getY() + 1.0, p.getZ(), 14, 0.35, 0.6, 0.35, 0.01);
            p.sendOverlayMessage(Component.translatable("message.thesift.cloak.hidden"));
        }
        p.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 12, 0, true, false, true));
        if (ticks % 10 == 0) {
            // whatever was hunting you loses you
            for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(32.0), x -> x.getTarget() == p)) {
                if (m.getLastHurtByMob() != p) {
                    m.setTarget(null);
                }
            }
        }
    }

    /** Nothing can pick a hidden player as its target (unless the player just hurt it). */
    private static void onChangeTarget(LivingChangeTargetEvent event) {
        LivingEntity target = event.getNewAboutToBeSetTarget();
        if (target != null && hidden(target) && event.getEntity().getLastHurtByMob() != target) {
            event.setCanceled(true);
        }
    }

    /** For the Kerkorer's own senses (it sees the cloak too). */
    public static boolean unseen(LivingEntity e) {
        return hidden(e) || e.isInvisible();
    }
}
