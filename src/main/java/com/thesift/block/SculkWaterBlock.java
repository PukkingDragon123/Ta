package com.thesift.block;

import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSculkSwamp;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * W1 Sculk Water. Glowing motes drift up through it and pop at the surface. Whatever soaks in it
 * is slowly corrupted: a first touch of Sculk Corruption that stays while you stay in, and every
 * few seconds a chance that it bites one level deeper (up to II). Sculk creatures are at home in
 * it (#thesift:sculk_water_dwellers). Its weight and swimming live in {@link SculkWaterFluidType}.
 */
public class SculkWaterBlock extends LiquidBlock {
    /** The corruption a soak gives, in ticks; topped up while it runs below {@link #REFRESH}. */
    private static final int SOAK = 160;
    private static final int REFRESH = 110;
    private static final int MAX_AMPLIFIER = 1;

    public SculkWaterBlock(BlockBehaviour.Properties properties) {
        super(ModSculkSwamp.SCULK_WATER.get(), properties);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        super.entityInside(state, level, pos, entity, effectApplier, isPrecise);
        if (level instanceof ServerLevel server && entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator()
                && !living.getType().builtInRegistryHolder().is(ModSculkSwamp.SCULK_WATER_DWELLERS)) {
            soak(server, living);
        }
    }

    /** Called for every Sculk Water block an entity touches each tick: the duration gate makes it count once. */
    private static void soak(ServerLevel level, LivingEntity living) {
        MobEffectInstance had = living.getEffect(ModEffects.SCULK_CORRUPTION);
        if (had != null && (had.getDuration() >= REFRESH || had.isInfiniteDuration())) {
            return;
        }
        RandomSource random = level.getRandom();
        int amplifier = had == null ? 0 : had.getAmplifier();
        if (had != null && amplifier < MAX_AMPLIFIER && random.nextInt(5) == 0) {
            amplifier++; // the longer you soak, the deeper it bites
        }
        living.addEffect(new MobEffectInstance(ModEffects.SCULK_CORRUPTION, SOAK, amplifier));
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, living.getX(), living.getY() + living.getBbHeight() * 0.5, living.getZ(), 4,
                living.getBbWidth() * 0.4, living.getBbHeight() * 0.3, living.getBbWidth() * 0.4, 0.01);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        boolean open = level.getBlockState(pos.above()).isAir();
        if (random.nextInt(9) == 0) {
            // a glowing mote drifting up through the water
            level.addParticle(ParticleTypes.GLOW, pos.getX() + random.nextDouble(), pos.getY() + random.nextDouble() * 0.8, pos.getZ() + random.nextDouble(),
                    (random.nextDouble() - 0.5) * 0.01, 0.012 + random.nextDouble() * 0.01, (random.nextDouble() - 0.5) * 0.01);
        }
        if (!open) {
            return;
        }
        if (random.nextInt(16) == 0) {
            level.addParticle(ParticleTypes.SCULK_CHARGE_POP, pos.getX() + random.nextDouble(), pos.getY() + 0.92, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
        if (random.nextInt(60) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.1, pos.getZ() + random.nextDouble(),
                    0.0, 0.01, 0.0);
        }
        if (random.nextInt(170) == 0) {
            level.addParticle(ModParticles.SIFT_MIST.get(), pos.getX() + random.nextDouble(), pos.getY() + 1.05, pos.getZ() + random.nextDouble(),
                    0.0, 0.002, 0.0);
        }
        if (random.nextInt(320) == 0) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, ModSculkSwamp.SCULK_WATER_AMBIENT.get(), SoundSource.BLOCKS,
                    0.3F + random.nextFloat() * 0.2F, 0.7F + random.nextFloat() * 0.3F, false);
        }
    }
}
