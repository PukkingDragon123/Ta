package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.cave.JailCell;
import com.thesift.entity.cave.Jailer;
import com.thesift.entity.cave.Sculkling;
import com.thesift.music.SongEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.EntityMountEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * A4 cave creatures: the Jailer (with the cell it traps you in) and the Sculklings - their
 * registration, sounds, spawn rules in the Sift's dark caves, the shiny things Sculklings steal,
 * and the ears of both: every note played nearby draws a Jailer and sends Sculklings fleeing.
 */
public final class ModCaveCreatures {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Jailer>> JAILER = ENTITIES.registerEntityType("jailer", Jailer::new,
            MobCategory.MONSTER, b -> b.sized(0.9F, 3.4F).eyeHeight(3.0F).clientTrackingRange(10));
    /** The cell a trapped player stands in: an invisible seat and hitbox riding the Jailer (the Jailer's model draws the bars). */
    public static final DeferredHolder<EntityType<?>, EntityType<JailCell>> JAIL_CELL = ENTITIES.registerEntityType("jail_cell", JailCell::new,
            MobCategory.MISC, b -> b.sized(1.25F, 2.0F).clientTrackingRange(10).noSummon().fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Sculkling>> SCULKLING = ENTITIES.registerEntityType("sculkling", Sculkling::new,
            MobCategory.MONSTER, b -> b.sized(0.6F, 0.95F).eyeHeight(0.7F).clientTrackingRange(8));

    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_AMBIENT = reg("entity.jailer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_LISTEN = reg("entity.jailer.listen");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_STEP = reg("entity.jailer.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_HURT = reg("entity.jailer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_DEATH = reg("entity.jailer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_EMERGE = reg("entity.jailer.emerge");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_WINDUP = reg("entity.jailer.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_SLAM = reg("entity.jailer.slam");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_TRAP = reg("entity.jailer.trap");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_SQUEEZE = reg("entity.jailer.squeeze");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_RATTLE = reg("entity.jailer.rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_BREAK = reg("entity.jailer.break");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_REGROW = reg("entity.jailer.regrow");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_AMBIENT = reg("entity.sculkling.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_SCREECH = reg("entity.sculkling.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_HURT = reg("entity.sculkling.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_DEATH = reg("entity.sculkling.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_STEP = reg("entity.sculkling.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_SNATCH = reg("entity.sculkling.snatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_SCARED = reg("entity.sculkling.scared");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_TWITCH = reg("entity.sculkling.twitch");

    /** What a Sculkling will snatch from your pockets: gold, gems, ingots and other glittering things. */
    public static final TagKey<Item> SHINIES = TagKey.create(Registries.ITEM, TheSift.id("sculkling_shinies"));
    private static final ResourceKey<Biome> DEEP_SIFT = ResourceKey.create(Registries.BIOME, TheSift.id("deep_sift"));

    private ModCaveCreatures() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(ModCaveCreatures::attributes);
        bus.addListener(ModCaveCreatures::spawnPlacements);
        // the cell holds: sneaking does not get you out of it (breaking the bars does)
        NeoForge.EVENT_BUS.addListener(ModCaveCreatures::onDismount);
        // blind ears: a note draws the Jailer to whoever played it, and sends Sculklings running with their ears covered
        SongEvents.listenNotes((level, player, at, pitch) -> {
            for (Jailer j : level.getEntitiesOfClass(Jailer.class, new AABB(at, at).inflate(Jailer.NOTE_RANGE), Jailer::isAlive)) {
                j.hear(at, player, 1.0F);
            }
            for (Sculkling s : level.getEntitiesOfClass(Sculkling.class, new AABB(at, at).inflate(Sculkling.MUSIC_RANGE), Sculkling::isAlive)) {
                s.scare(at, 1.0F);
            }
        });
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(JAILER.get(), Jailer.createAttributes().build());
        event.put(SCULKLING.get(), Sculkling.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(JAILER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveCreatures::checkJailer,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SCULKLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveCreatures::checkSculkling,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static void onDismount(EntityMountEvent event) {
        if (event.isDismounting() && !event.getLevel().isClientSide() && event.getEntityBeingMounted() instanceof JailCell cell
                && event.getEntityMounting() instanceof Player player && cell.holdsFast(player)) {
            event.setCanceled(true);
            cell.struggle(player);
        }
    }

    /** A dark Sift cave: no sky overhead, no light at all, solid ground underfoot, below {@code maxY} (or anywhere in the Deep Sift). */
    public static boolean darkCave(ServerLevelAccessor level, BlockPos pos, int maxY) {
        if (level.getDifficulty() == Difficulty.PEACEFUL || level.canSeeSky(pos) || level.getRawBrightness(pos, 0) > 0) {
            return false;
        }
        if (pos.getY() >= maxY && !level.getBiome(pos).is(DEEP_SIFT)) {
            return false;
        }
        return level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    /** Jailers are rare: only deep down (below y 0, or in the Deep Sift), and never two near each other. */
    private static boolean checkJailer(EntityType<Jailer> type, ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            BlockPos pos, net.minecraft.util.RandomSource random) {
        if (net.minecraft.world.entity.EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return random.nextInt(3) == 0 && darkCave(level, pos, 0)
                && level.getEntitiesOfClass(Jailer.class, new AABB(pos).inflate(48.0)).isEmpty();
    }

    private static boolean checkSculkling(EntityType<Sculkling> type, ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            BlockPos pos, net.minecraft.util.RandomSource random) {
        return net.minecraft.world.entity.EntitySpawnReason.isSpawner(reason) || darkCave(level, pos, 40);
    }
}
