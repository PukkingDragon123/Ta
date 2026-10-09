package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.cave.JailCell;
import com.thesift.entity.cave.Jailer;
import com.thesift.entity.cave.Sculkling;
import com.thesift.entity.swamp.Cypole;
import com.thesift.music.Instrument;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
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
 * CR4's creatures: the Jailer (with the cell it traps you in) and the Sculklings of the dark caves,
 * and the Cypole of the Sculk Swamp - their registration, sounds, spawn rules, the shiny things
 * Sculklings steal, the Cypole's shockwave ring and band voice, and the ears of all three: every
 * note played nearby draws a Jailer and sends Sculklings fleeing, and a percussion song soothes an
 * angry Cypole.
 */
public final class ModCaveCreatures {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);
    public static final DeferredRegister<ParticleType<?>> PARTICLES = DeferredRegister.create(Registries.PARTICLE_TYPE, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<Jailer>> JAILER = ENTITIES.registerEntityType("jailer", Jailer::new,
            MobCategory.MONSTER, b -> b.sized(0.9F, 3.4F).eyeHeight(3.0F).clientTrackingRange(10));
    /** The cell a trapped player stands in: an invisible seat and hitbox riding the Jailer (the Jailer's model draws the bars). */
    public static final DeferredHolder<EntityType<?>, EntityType<JailCell>> JAIL_CELL = ENTITIES.registerEntityType("jail_cell", JailCell::new,
            MobCategory.MISC, b -> b.sized(1.25F, 2.0F).clientTrackingRange(10).noSummon().fireImmune());
    public static final DeferredHolder<EntityType<?>, EntityType<Sculkling>> SCULKLING = ENTITIES.registerEntityType("sculkling", Sculkling::new,
            MobCategory.MONSTER, b -> b.sized(0.6F, 0.95F).eyeHeight(0.7F).clientTrackingRange(8));
    /** CR4 the one-eyed cymbal frog of the Sculk Swamp (neutral, territorial). */
    public static final DeferredHolder<EntityType<?>, EntityType<Cypole>> CYPOLE = ENTITIES.registerEntityType("cypole", Cypole::new,
            MobCategory.CREATURE, b -> b.sized(0.9F, 0.85F).eyeHeight(0.75F).clientTrackingRange(10));

    /** The Cypole's shockwave: a flat brass ring racing out across the ground (xa: its last radius, ya: its speed a tick). */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CYMBAL_RING = PARTICLES.register("cymbal_ring",
            () -> new SimpleParticleType(true));

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
    // CR4 the harder cell: the heartbeat cue, the grip loosening, a good heave, squirming against it, a guard's kick
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_PULSE = reg("entity.jailer.pulse");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_LOOSEN = reg("entity.jailer.loosen");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_HEAVE = reg("entity.jailer.heave");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_TIGHTEN = reg("entity.jailer.tighten");
    public static final DeferredHolder<SoundEvent, SoundEvent> JAILER_KICK = reg("entity.jailer.kick");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_AMBIENT = reg("entity.sculkling.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_SCREECH = reg("entity.sculkling.screech");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_HURT = reg("entity.sculkling.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_DEATH = reg("entity.sculkling.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_STEP = reg("entity.sculkling.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_SNATCH = reg("entity.sculkling.snatch");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_SCARED = reg("entity.sculkling.scared");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULKLING_TWITCH = reg("entity.sculkling.twitch");
    // CR4 the Cypole
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_CROAK = reg("entity.cypole.croak");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_TICK = reg("entity.cypole.tick");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_RATTLE = reg("entity.cypole.rattle");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_ANGRY = reg("entity.cypole.angry");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_CALM = reg("entity.cypole.calm");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_WINDUP = reg("entity.cypole.windup");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_CRASH = reg("entity.cypole.crash");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_SHIMMER = reg("entity.cypole.shimmer");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_SHOCKWAVE = reg("entity.cypole.shockwave");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_TONGUE = reg("entity.cypole.tongue");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_SLAP = reg("entity.cypole.slap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_SHIELDED = reg("entity.cypole.shielded");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_HOP = reg("entity.cypole.hop");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_HURT = reg("entity.cypole.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CYPOLE_DEATH = reg("entity.cypole.death");

    /** What a Sculkling will snatch from your pockets: gold, gems, ingots and other glittering things. */
    public static final TagKey<Item> SHINIES = TagKey.create(Registries.ITEM, TheSift.id("sculkling_shinies"));
    /** Sculkite, the Jailer's dark sculk crystal (and whatever later counts as it): echo gear and Stomper armour are made of it. */
    public static final TagKey<Item> SCULKITE = TagKey.create(Registries.ITEM, TheSift.id("sculkite"));
    private static final ResourceKey<Biome> DEEP_SIFT = ResourceKey.create(Registries.BIOME, TheSift.id("deep_sift"));

    private ModCaveCreatures() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        PARTICLES.register(bus);
        bus.addListener(ModCaveCreatures::attributes);
        bus.addListener(ModCaveCreatures::spawnPlacements);
        // the cell holds: sneaking does not get you out of it (each press is a heave against the bars)
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
        // a percussion song soothes the Cypoles around it
        SongEvents.listenSongs((level, player, at, song) -> {
            if (player == null) {
                return;
            }
            Instrument played = SongEvents.instrument();
            for (Cypole c : level.getEntitiesOfClass(Cypole.class, new AABB(at, at).inflate(16.0), Cypole::isAlive)) {
                c.hearSong(level, player, song, played);
            }
        });
        // the Cypole's voice in a player's band: brass cymbal ticks with a croak under each, for drums and chimes
        BandRegistry.voice(CYPOLE, SoundEvents.NOTE_BLOCK_HAT).volume(0.9F)
                .layer(SoundEvents.FROG_AMBIENT, 0.3F)
                .families(Instrument.Family.DRUM, Instrument.Family.CHIMES)
                .temper(BandVoice.Temper.SHY).instrument("cymbals").colour(0xE3B23C)
                .when(m -> m instanceof Cypole c && !c.isAngry() && c.getAction() == Cypole.NONE).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(JAILER.get(), Jailer.createAttributes().build());
        event.put(SCULKLING.get(), Sculkling.createAttributes().build());
        event.put(CYPOLE.get(), Cypole.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(JAILER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveCreatures::checkJailer,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(SCULKLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveCreatures::checkSculkling,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CYPOLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveCreatures::checkCypole,
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
        if (pos.getY() >= maxY && !level.getBiome(pos).is(DEEP_SIFT) && !level.getBiome(pos).is(ModCaves.SCULK_CAVES)) { // W-deep: and the Sculk Caves
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

    /** Cypoles come up out of the swamp's mud (or ordinary mud, moss and sculk), whatever the light. */
    private static boolean checkCypole(EntityType<Cypole> type, ServerLevelAccessor level, net.minecraft.world.entity.EntitySpawnReason reason,
            BlockPos pos, net.minecraft.util.RandomSource random) {
        if (net.minecraft.world.entity.EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        BlockState ground = level.getBlockState(pos.below());
        return ground.is(ModBlocks.SCULK_MUD.get()) || ground.is(Blocks.MUD) || ground.is(Blocks.MOSS_BLOCK) || ground.is(Blocks.SCULK);
    }
}
