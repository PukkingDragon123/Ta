package com.thesift.registry;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.block.SculkWaterFluidType;
import com.thesift.worldgen.BiomeBlendFeature;
import com.thesift.worldgen.RelicCacheFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * W1 World &amp; terrain: the Sculk Swamp and the Sculk Ocean. Sculk Water (a real fluid: dark teal,
 * full of glowing motes, slowly corrupting whatever soaks in it), the worldgen feature types that
 * soften every biome edge and bury the Sift's relics, the swamp's sounds, and its infestation:
 * Sculk Parasites and Sculk Spiders skitter out of the mud around anyone wading through it.
 * Plain blocks and items (Sculk Mud, the corals, Blightwood, the bucket) come from tools/spec.py
 * via tools/sculk_world.py.
 */
public final class ModSculkSwamp {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TheSift.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, TheSift.MODID);
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, TheSift.MODID);

    public static final DeferredHolder<FluidType, FluidType> SCULK_WATER_TYPE = FLUID_TYPES.register("sculk_water", SculkWaterFluidType::new);
    public static final DeferredHolder<Fluid, FlowingFluid> SCULK_WATER = FLUIDS.register("sculk_water",
            () -> new BaseFlowingFluid.Source(fluidProperties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_SCULK_WATER = FLUIDS.register("flowing_sculk_water",
            () -> new BaseFlowingFluid.Flowing(fluidProperties()));

    /** Dithered, noise-broken bands where two biomes' ground covers meet (worldgen/BiomeBlendFeature). */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<BiomeBlendFeature>> BIOME_BLEND = FEATURE_TYPES.register("biome_blend",
            () -> BiomeBlendFeature.CODEC);
    /** Suspicious Chime Sand buried just under the ground, each with its archaeology loot (worldgen/RelicCacheFeature). */
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<RelicCacheFeature>> RELIC_CACHE = FEATURE_TYPES.register("relic_cache",
            () -> RelicCacheFeature.CODEC);

    public static final DeferredHolder<SoundEvent, SoundEvent> MUSIC_SCULK_SWAMP = reg("music.sculk_swamp");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWAMP_LOOP = reg("ambient.sculk_swamp.loop");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWAMP_ADDITIONS = reg("ambient.sculk_swamp.additions");
    public static final DeferredHolder<SoundEvent, SoundEvent> SWAMP_MOOD = reg("ambient.sculk_swamp.mood");
    public static final DeferredHolder<SoundEvent, SoundEvent> SCULK_WATER_AMBIENT = reg("block.sculk_water.ambient");

    public static final ResourceKey<Biome> SCULK_SWAMP = ResourceKey.create(Registries.BIOME, TheSift.id("sculk_swamp"));
    public static final ResourceKey<Biome> SCULK_OCEAN = ResourceKey.create(Registries.BIOME, TheSift.id("deep_dark_ocean"));
    /** Creatures Sculk Water does not corrupt. */
    public static final TagKey<EntityType<?>> SCULK_WATER_DWELLERS = TagKey.create(Registries.ENTITY_TYPE, TheSift.id("sculk_water_dwellers"));

    /** How many swamp crawlers may be around one player before the mud stops giving up more. */
    private static final int LOCAL_CAP = 5;

    private ModSculkSwamp() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> reg(String name) {
        return SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(TheSift.id(name)));
    }

    private static BaseFlowingFluid.Properties fluidProperties() {
        return new BaseFlowingFluid.Properties(SCULK_WATER_TYPE::value, SCULK_WATER, FLOWING_SCULK_WATER)
                .bucket(ModItems.SCULK_WATER_BUCKET)
                .block(ModBlocks.SCULK_WATER)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(1)
                .tickRate(8)
                .explosionResistance(100.0F);
    }

    public static void register(IEventBus bus) {
        FLUID_TYPES.register(bus);
        FLUIDS.register(bus);
        FEATURE_TYPES.register(bus);
        SOUNDS.register(bus);
        bus.addListener(ModSculkSwamp::registerSpawnPlacements);
        NeoForge.EVENT_BUS.addListener(ModSculkSwamp::onLevelTick);
    }

    private static void registerSpawnPlacements(RegisterSpawnPlacementsEvent event) {
        // the swamp's crawlers come up out of the ground, never out of thin air
        event.register(ModEntities.SCULK_PARASITE.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSculkSwamp::checkCrawler, RegisterSpawnPlacementsEvent.Operation.REPLACE);
        event.register(ModEntities.STRUMLING.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                ModSculkSwamp::checkCrawler, RegisterSpawnPlacementsEvent.Operation.REPLACE);
    }

    private static <T extends Mob> boolean checkCrawler(EntityType<T> type, ServerLevelAccessor level, EntitySpawnReason reason, BlockPos pos,
            RandomSource random) {
        return level.getDifficulty() != Difficulty.PEACEFUL && Mob.checkMobSpawnRules(type, level, reason, pos, random);
    }

    /**
     * The swamp is never quiet: every few seconds, near each player wading through it, a Sculk
     * Parasite (now and then a Sculk Spider) crawls up out of the mud - whatever the light.
     */
    private static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 80 != 0 || level.getDifficulty() == Difficulty.PEACEFUL
                || level.dimension() != ModDimensions.THE_SIFT) {
            return;
        }
        RandomSource random = level.getRandom();
        for (Player player : level.players()) {
            if (player.isSpectator() || player.isCreative() || random.nextInt(3) != 0 || !level.getBiome(player.blockPosition()).is(SCULK_SWAMP)) {
                continue;
            }
            AABB around = player.getBoundingBox().inflate(40.0);
            int crawlers = level.getEntitiesOfClass(com.thesift.entity.boss.SculkParasite.class, around, Mob::isAlive).size()
                    + level.getEntitiesOfClass(com.thesift.entity.boss.Strumling.class, around, Mob::isAlive).size();
            if (crawlers >= LOCAL_CAP) {
                continue;
            }
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist = 14.0 + random.nextDouble() * 14.0;
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos at = new BlockPos(x, y, z);
            BlockState ground = level.getBlockState(at.below());
            if (!level.getBiome(at).is(SCULK_SWAMP) || !ground.getFluidState().isEmpty() || !level.getBlockState(at).isAir()
                    || !(ground.is(ModBlocks.SCULK_MUD.get()) || ground.is(net.minecraft.world.level.block.Blocks.SCULK))) {
                continue;
            }
            EntityType<? extends Mob> type = random.nextInt(4) == 0 ? ModEntities.STRUMLING.get() : ModEntities.SCULK_PARASITE.get();
            Mob m = type.create(level, EntitySpawnReason.NATURAL);
            if (m == null) {
                continue;
            }
            m.snapTo(x + 0.5, y, z + 0.5, random.nextFloat() * 360.0F, 0.0F);
            if (m.checkSpawnObstruction(level)) {
                level.addFreshEntity(m);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SCULK_CHARGE_POP, x + 0.5, y + 0.2, z + 0.5, 8, 0.4, 0.1, 0.4, 0.02);
            } else {
                m.discard();
            }
        }
    }
}
