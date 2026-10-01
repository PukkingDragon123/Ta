package com.thesift.entity;

import com.thesift.registry.ModParticles;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * The pop a Sift mob makes when it is killed (client side, from makePoofParticles): a ring
 * rolling out over the ground, a spray of bouncy stars, notes, hearts or droplets in the mob's
 * own colours, a few sparkles - and vanilla's puff of smoke.
 */
public final class KillBurst {
    public static final int STAR = 0;
    public static final int NOTE = 1;
    public static final int HEART = 2;
    public static final int DROP = 3;

    private KillBurst() {
    }

    /**
     * @param shape  the main shape of the spray; a third of it is always stars
     * @param extra  an extra particle that suits the mob (slime, souls, feathers...), or null
     */
    public static void pop(LivingEntity e, int colorA, int colorB, int shape, ParticleOptions extra) {
        Level level = e.level();
        if (!level.isClientSide()) {
            return;
        }
        RandomSource r = e.getRandom();
        double x = e.getX();
        double y = e.getY() + e.getBbHeight() * 0.45;
        double z = e.getZ();
        float size = Math.max(e.getBbWidth(), e.getBbHeight() * 0.6F);
        int count = 8 + (int) (size * 8);
        for (int i = 0; i < count; i++) {
            int s = i % 3 == 0 ? STAR : shape;
            int c = i % 2 == 0 ? colorA : colorB;
            level.addParticle(ModParticles.KILL_STAR.get(), x + (r.nextDouble() - 0.5) * e.getBbWidth() * 0.5, y, z + (r.nextDouble() - 0.5) * e.getBbWidth() * 0.5,
                    c, s, 0.8 + size * 0.35 + r.nextDouble() * 0.3);
        }
        level.addParticle(ModParticles.RESONANCE_RING.get(), x, e.getY() + 0.08, z, Math.max(0.8, size * 1.6), 0.0, 0.0);
        for (int i = 0; i < 5 + size * 3; i++) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), e.getRandomX(0.8), e.getY() + r.nextDouble() * e.getBbHeight(), e.getRandomZ(0.8), 0, 0, 0);
        }
        if (extra != null) {
            for (int i = 0; i < 6 + size * 4; i++) {
                level.addParticle(extra, e.getRandomX(0.6), e.getY() + r.nextDouble() * e.getBbHeight(), e.getRandomZ(0.6),
                        (r.nextDouble() - 0.5) * 0.2, r.nextDouble() * 0.2, (r.nextDouble() - 0.5) * 0.2);
            }
        }
        // vanilla's poof, a little lighter than usual so the colours show
        for (int i = 0; i < 10; i++) {
            level.addParticle(ParticleTypes.POOF, e.getRandomX(1.0), e.getRandomY(), e.getRandomZ(1.0), r.nextGaussian() * 0.02, r.nextGaussian() * 0.02,
                    r.nextGaussian() * 0.02);
        }
    }
}
