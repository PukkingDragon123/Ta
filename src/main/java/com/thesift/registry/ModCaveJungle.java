package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.entity.jungle.ColossusPonder;
import com.thesift.entity.jungle.Crocotodo;
import com.thesift.entity.jungle.Cruncher;
import com.thesift.entity.jungle.GlowFly;
import com.thesift.entity.jungle.Mantis;
import com.thesift.entity.jungle.PonderTadpole;
import com.thesift.music.Instrument;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.Consumables;
import net.minecraft.world.item.consume_effects.ApplyStatusEffectsConsumeEffect;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * P4 Cave Jungle: the creatures of the Cave Jungle - the Glow Fly, the Crocotodo, the Mantis, the Colossus Ponder
 * and its tadpoles, and the Cruncher (which also roams the ordinary Sift Caves) - with their sounds, spawn rules,
 * band voices, the foods of the jungle (Pitcher Pods, the Colossus Ponder's giant egg) and the item tag of what a
 * Stomper eats. The Giant Pitcher Plant, its sprout and the tadpole-bored log are blocks from tools/spec.py
 * (tools/cave_jungle.py); their Java lives in block/GiantPitcherBlock, PitcherSproutBlock and BoredLogBlock.
 */
public final class ModCaveJungle {
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GlowFly>> GLOW_FLY = ENTITIES.registerEntityType("glow_fly", GlowFly::new,
            MobCategory.CREATURE, b -> b.sized(0.8F, 0.7F).eyeHeight(0.4F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Crocotodo>> CROCOTODO = ENTITIES.registerEntityType("crocotodo", Crocotodo::new,
            MobCategory.CREATURE, b -> b.sized(0.75F, 1.3F).eyeHeight(1.15F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<Mantis>> MANTIS = ENTITIES.registerEntityType("mantis", Mantis::new,
            MobCategory.MONSTER, b -> b.sized(1.3F, 2.5F).eyeHeight(2.3F).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<ColossusPonder>> COLOSSUS_PONDER = ENTITIES.registerEntityType("colossus_ponder",
            ColossusPonder::new, MobCategory.CREATURE, b -> b.sized(3.0F, 2.4F).eyeHeight(2.0F).clientTrackingRange(12));
    public static final DeferredHolder<EntityType<?>, EntityType<PonderTadpole>> PONDER_TADPOLE = ENTITIES.registerEntityType("ponder_tadpole",
            PonderTadpole::new, MobCategory.CREATURE, b -> b.sized(0.6F, 0.45F).eyeHeight(0.3F).clientTrackingRange(8));
    public static final DeferredHolder<EntityType<?>, EntityType<Cruncher>> CRUNCHER = ENTITIES.registerEntityType("cruncher", Cruncher::new,
            MobCategory.MONSTER, b -> b.sized(0.7F, 1.0F).eyeHeight(0.8F).clientTrackingRange(10));

    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_FLY_AMBIENT = reg("entity.glow_fly.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_FLY_HURT = reg("entity.glow_fly.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_FLY_DEATH = reg("entity.glow_fly.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_FLY_FLASH = reg("entity.glow_fly.flash");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_FLY_ABSORB = reg("entity.glow_fly.absorb");
    public static final DeferredHolder<SoundEvent, SoundEvent> GLOW_FLY_GIVE = reg("entity.glow_fly.give");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCOTODO_AMBIENT = reg("entity.crocotodo.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCOTODO_HURT = reg("entity.crocotodo.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCOTODO_DEATH = reg("entity.crocotodo.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCOTODO_SNAP = reg("entity.crocotodo.snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCOTODO_PECK = reg("entity.crocotodo.peck");
    public static final DeferredHolder<SoundEvent, SoundEvent> CROCOTODO_STEP = reg("entity.crocotodo.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_AMBIENT = reg("entity.mantis.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_HURT = reg("entity.mantis.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_DEATH = reg("entity.mantis.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_STRIKE = reg("entity.mantis.strike");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_SLICE = reg("entity.mantis.slice");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_DIVE = reg("entity.mantis.dive");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_CRUNCH = reg("entity.mantis.crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> MANTIS_STEP = reg("entity.mantis.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> PONDER_CROAK = reg("entity.colossus_ponder.croak");
    public static final DeferredHolder<SoundEvent, SoundEvent> PONDER_HURT = reg("entity.colossus_ponder.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PONDER_DEATH = reg("entity.colossus_ponder.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PONDER_STOMP = reg("entity.colossus_ponder.stomp");
    public static final DeferredHolder<SoundEvent, SoundEvent> PONDER_ROAR = reg("entity.colossus_ponder.roar");
    public static final DeferredHolder<SoundEvent, SoundEvent> PONDER_LAY = reg("entity.colossus_ponder.lay");
    public static final DeferredHolder<SoundEvent, SoundEvent> TADPOLE_AMBIENT = reg("entity.ponder_tadpole.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> TADPOLE_HURT = reg("entity.ponder_tadpole.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> TADPOLE_DEATH = reg("entity.ponder_tadpole.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> TADPOLE_BITE = reg("entity.ponder_tadpole.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> TADPOLE_BURROW = reg("entity.ponder_tadpole.burrow");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_AMBIENT = reg("entity.cruncher.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_HURT = reg("entity.cruncher.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_DEATH = reg("entity.cruncher.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_BITE = reg("entity.cruncher.bite");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_CRUNCH = reg("entity.cruncher.crunch");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_WASTE = reg("entity.cruncher.waste");
    public static final DeferredHolder<SoundEvent, SoundEvent> CRUNCHER_STEP = reg("entity.cruncher.step");
    public static final DeferredHolder<SoundEvent, SoundEvent> PITCHER_SNAP = reg("block.giant_pitcher.snap");
    public static final DeferredHolder<SoundEvent, SoundEvent> PITCHER_GULP = reg("block.giant_pitcher.gulp");

    /** What a Stomper eats (the Stomper reads it): Pitcher Pods, roasted or not, and flowers. */
    public static final TagKey<Item> STOMPER_FOOD = TagKey.create(Registries.ITEM, TheSift.id("stomper_food"));

    // the foods of the jungle
    public static final FoodProperties PITCHER_POD_FOOD = new FoodProperties.Builder().nutrition(2).saturationModifier(0.2F).build();
    public static final FoodProperties ROASTED_PITCHER_POD_FOOD = new FoodProperties.Builder().nutrition(6).saturationModifier(0.7F).build();
    /** A whole giant egg, baked in its shell: a feast for one. */
    public static final FoodProperties BAKED_PONDER_EGG_FOOD = new FoodProperties.Builder().nutrition(14).saturationModifier(1.2F).build();
    public static final Consumable BAKED_PONDER_EGG_CONSUMABLE = Consumables.defaultFood()
            .consumeSeconds(3.2F)
            .onConsume(new ApplyStatusEffectsConsumeEffect(List.of(
                    new MobEffectInstance(MobEffects.ABSORPTION, 1200, 1),
                    new MobEffectInstance(MobEffects.REGENERATION, 160, 0))))
            .build();

    private ModCaveJungle() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    public static void register(IEventBus bus) {
        ENTITIES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(ModCaveJungle::attributes);
        bus.addListener(ModCaveJungle::spawnPlacements);
        // band voices: the jungle's gentle creatures play along; the hunters have a voice, never a seat
        BandRegistry.voice(GLOW_FLY, SoundEvents.NOTE_BLOCK_CHIME).transpose(12).volume(0.7F)
                .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.3F)
                .families(Instrument.Family.CHIMES, Instrument.Family.FLUTE)
                .movement(BandVoice.Movement.FLY).instrument("glow_chimes").colour(0xFFE45C)
                .when(m -> m instanceof GlowFly g && !g.isFleeing()).register();
        BandRegistry.voice(CROCOTODO, SoundEvents.NOTE_BLOCK_XYLOPHONE).volume(0.9F)
                .layer(SoundEvents.NOTE_BLOCK_HAT, 0.4F)
                .families(Instrument.Family.DRUM, Instrument.Family.STRINGS)
                .temper(BandVoice.Temper.SHY).instrument("beak_clapper").colour(0x3E866C)
                .when(m -> m instanceof Crocotodo c && c.getTarget() == null).register();
        BandRegistry.voice(COLOSSUS_PONDER, SoundEvents.NOTE_BLOCK_BASS).transpose(-12).volume(1.6F)
                .layer(SoundEvents.FROG_AMBIENT, 0.6F)
                .families(Instrument.Family.DRUM, Instrument.Family.FLUTE)
                .temper(BandVoice.Temper.SHY).instrument("bog_tuba").colour(0x45B4B8)
                .when(m -> m instanceof ColossusPonder p && !p.isRampaging()).register();
        BandRegistry.voice(PONDER_TADPOLE, SoundEvents.NOTE_BLOCK_BIT).transpose(12).volume(0.6F)
                .layer(SoundEvents.TADPOLE_FLOP, 0.4F)
                .families(Instrument.Family.FLUTE)
                .temper(BandVoice.Temper.SHY).instrument("bubble_pipe").colour(0x565E34)
                .when(m -> m instanceof PonderTadpole t && t.getTarget() == null).register();
        BandRegistry.voice(MANTIS, SoundEvents.NOTE_BLOCK_HARP).transpose(-5)
                .families(Instrument.Family.STRINGS).temper(BandVoice.Temper.HOSTILE).instrument("scythe_strings").colour(0x52AED8).register();
        BandRegistry.voice(CRUNCHER, SoundEvents.NOTE_BLOCK_BASEDRUM)
                .families(Instrument.Family.DRUM).temper(BandVoice.Temper.HOSTILE).instrument("rock_crusher").colour(0xD3C7B8).register();
    }

    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(GLOW_FLY.get(), GlowFly.createAttributes().build());
        event.put(CROCOTODO.get(), Crocotodo.createAttributes().build());
        event.put(MANTIS.get(), Mantis.createAttributes().build());
        event.put(COLOSSUS_PONDER.get(), ColossusPonder.createAttributes().build());
        event.put(PONDER_TADPOLE.get(), PonderTadpole.createAttributes().build());
        event.put(CRUNCHER.get(), Cruncher.createAttributes().build());
    }

    private static void spawnPlacements(RegisterSpawnPlacementsEvent event) {
        event.register(GLOW_FLY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveJungle::checkJungle,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CROCOTODO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveJungle::checkJungle,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(COLOSSUS_PONDER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveJungle::checkPonder,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(PONDER_TADPOLE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveJungle::checkJungle,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(MANTIS.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveJungle::checkMantis,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(CRUNCHER.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ModCaveJungle::checkHunter,
                RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    /** Underground (no sky overhead) on solid ground: the biome lists keep them to the Cave Jungle (and the Cruncher to the caves). */
    public static boolean underground(ServerLevelAccessor level, BlockPos pos) {
        return !level.canSeeSky(pos) && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean checkJungle(EntityType<T> type, ServerLevelAccessor level,
            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return EntitySpawnReason.isSpawner(reason) || underground(level, pos);
    }

    /** Colossus Ponders are rare: never two within 40 blocks. */
    private static boolean checkPonder(EntityType<ColossusPonder> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return underground(level, pos) && level.getEntitiesOfClass(ColossusPonder.class, new AABB(pos).inflate(40.0)).isEmpty();
    }

    private static <T extends net.minecraft.world.entity.Entity> boolean checkHunter(EntityType<T> type, ServerLevelAccessor level,
            EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return EntitySpawnReason.isSpawner(reason) || level.getDifficulty() != Difficulty.PEACEFUL && underground(level, pos);
    }

    /** The Mantis hunts alone: never two within 32 blocks. */
    private static boolean checkMantis(EntityType<Mantis> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        if (EntitySpawnReason.isSpawner(reason)) {
            return true;
        }
        return level.getDifficulty() != Difficulty.PEACEFUL && underground(level, pos)
                && level.getEntitiesOfClass(Mantis.class, new AABB(pos).inflate(32.0)).isEmpty();
    }
}
