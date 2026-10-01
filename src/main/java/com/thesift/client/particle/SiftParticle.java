package com.thesift.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

/**
 * All of the Sift's particles share one class; the {@link Kind} decides how each one moves, glows,
 * fades and colours itself.
 */
public class SiftParticle extends SingleQuadParticle {
    public enum Kind {
        DRIFTING_SOUL, CHROME_DROPLET, CHROME_BUBBLE, DREAM_POLLEN, SIFT_NOTE, RESONANCE_RING, GLOW_DUST, LEAF, SIFT_MIST, STAR_SPARKLE,
        PORTAL_SOUL, FOOTSTEP_PUFF, GLOW_SPLAT, WISHING_STAR, SLEEP_SPORE, SLIME_TRAIL
    }

    /** Cyan -> pink -> pearl, the colours of Chrome and the Sift sky. */
    private static final float[][] SIFT_COLORS = {
            {0.50F, 0.95F, 1.00F}, {0.62F, 0.85F, 1.00F}, {0.86F, 0.72F, 1.00F}, {1.00F, 0.62F, 0.86F}, {1.00F, 0.82F, 0.93F}, {0.96F, 0.96F, 1.00F}
    };

    private final Kind kind;
    private final SpriteSet sprites;
    private final boolean bright;
    private final float baseSize;
    private final float swayPhase;
    private final float spin;
    private float fadeIn = 0.1F;
    private float fadeOut = 0.35F;
    private float maxAlpha = 1.0F;
    private final float colorShift;
    private final float ringRadius;

    protected SiftParticle(Kind kind, ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites,
            RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.kind = kind;
        this.sprites = sprites;
        this.swayPhase = random.nextFloat() * Mth.TWO_PI;
        this.spin = (random.nextFloat() - 0.5F) * 0.12F;
        this.colorShift = random.nextFloat();
        this.hasPhysics = false;
        this.friction = 0.96F;
        this.gravity = 0.0F;
        this.xd = xa;
        this.yd = ya;
        this.zd = za;
        float size = 0.1F;
        boolean glow = true;
        float ring = 0.0F;
        switch (kind) {
            case DRIFTING_SOUL -> {
                this.lifetime = 90 + random.nextInt(90);
                this.xd = xa + (random.nextDouble() - 0.5) * 0.01;
                this.yd = 0.008 + random.nextDouble() * 0.012 + ya;
                this.zd = za + (random.nextDouble() - 0.5) * 0.01;
                size = 0.12F + random.nextFloat() * 0.08F;
                this.fadeIn = 0.2F;
                this.fadeOut = 0.4F;
                this.maxAlpha = 0.85F;
                this.pickSiftColor(random.nextFloat());
            }
            case CHROME_DROPLET -> {
                this.lifetime = 16 + random.nextInt(20);
                this.gravity = 0.55F;
                this.hasPhysics = true;
                this.friction = 0.98F;
                if (xa == 0 && ya == 0 && za == 0) {
                    this.xd = (random.nextDouble() - 0.5) * 0.08;
                    this.yd = 0.12 + random.nextDouble() * 0.12;
                    this.zd = (random.nextDouble() - 0.5) * 0.08;
                }
                size = 0.05F + random.nextFloat() * 0.04F;
                this.pickSiftColor(this.colorShift);
            }
            case CHROME_BUBBLE -> {
                this.lifetime = 20 + random.nextInt(30);
                this.yd = 0.02 + random.nextDouble() * 0.02;
                size = 0.06F + random.nextFloat() * 0.05F;
                this.maxAlpha = 0.8F;
                this.pickSiftColor(this.colorShift);
                glow = false;
            }
            case DREAM_POLLEN -> {
                this.lifetime = 60 + random.nextInt(80);
                this.xd = xa + (random.nextDouble() - 0.5) * 0.02;
                this.yd = ya + (random.nextDouble() - 0.3) * 0.01;
                this.zd = za + (random.nextDouble() - 0.5) * 0.02;
                size = 0.03F + random.nextFloat() * 0.03F;
                float[] c = random.nextBoolean() ? new float[]{1.0F, 0.93F, 0.55F} : new float[]{1.0F, 0.7F, 0.88F};
                this.setColor(c[0], c[1], c[2]);
                this.fadeIn = 0.25F;
            }
            case SIFT_NOTE -> {
                this.lifetime = 22 + random.nextInt(8);
                this.xd = (random.nextDouble() - 0.5) * 0.02;
                this.yd = 0.035;
                this.zd = (random.nextDouble() - 0.5) * 0.02;
                size = 0.14F;
                this.pickSiftColor((float) Mth.frac(xa));
                this.fadeIn = 0.05F;
            }
            case RESONANCE_RING -> {
                this.lifetime = 16;
                ring = Math.max(0.5F, (float) xa);
                this.xd = 0;
                this.yd = 0;
                this.zd = 0;
                size = 0.2F;
                this.pickSiftColor(this.colorShift);
                this.fadeIn = 0.0F;
                this.fadeOut = 0.8F;
                this.maxAlpha = 0.8F;
            }
            case GLOW_DUST -> {
                this.lifetime = 40 + random.nextInt(60);
                this.yd = ya - 0.004;
                size = 0.025F + random.nextFloat() * 0.03F;
                this.pickSiftColor(this.colorShift);
                this.fadeIn = 0.3F;
            }
            case LEAF -> {
                this.lifetime = 80 + random.nextInt(60);
                this.gravity = 0.04F;
                this.hasPhysics = true;
                this.friction = 1.0F;
                size = 0.08F + random.nextFloat() * 0.03F;
                glow = false;
                this.roll = random.nextFloat() * Mth.TWO_PI;
                this.oRoll = this.roll;
                this.fadeIn = 0.05F;
                this.fadeOut = 0.15F;
            }
            case SIFT_MIST -> {
                this.lifetime = 120 + random.nextInt(80);
                this.xd = xa + (random.nextDouble() - 0.5) * 0.006;
                this.yd = ya + 0.001;
                this.zd = za + (random.nextDouble() - 0.5) * 0.006;
                size = 0.8F + random.nextFloat() * 0.8F;
                this.maxAlpha = 0.18F;
                this.fadeIn = 0.3F;
                this.fadeOut = 0.4F;
                this.pickSiftColor(this.colorShift);
                glow = false;
            }
            case STAR_SPARKLE -> {
                this.lifetime = 12 + random.nextInt(14);
                size = 0.06F + random.nextFloat() * 0.06F;
                this.setColor(1.0F, 0.97F, 0.85F);
                this.fadeIn = 0.2F;
                this.fadeOut = 0.5F;
            }
            case PORTAL_SOUL -> {
                this.lifetime = 30 + random.nextInt(20);
                this.friction = 0.99F;
                size = 0.1F + random.nextFloat() * 0.06F;
                this.pickSiftColor(this.colorShift);
                this.fadeIn = 0.05F;
            }
            case FOOTSTEP_PUFF -> {
                this.lifetime = 10 + random.nextInt(6);
                this.xd = (random.nextDouble() - 0.5) * 0.04;
                this.yd = 0.01 + ya;
                this.zd = (random.nextDouble() - 0.5) * 0.04;
                size = 0.08F;
                this.maxAlpha = 0.7F;
                this.setColor(0.95F, 0.9F, 1.0F);
                glow = false;
            }
            case SLIME_TRAIL -> {
                // a flat jelly splotch left behind by hopping Bulbs; xa/ya/za carry its tint
                this.lifetime = 50 + random.nextInt(30);
                this.xd = 0;
                this.yd = 0;
                this.zd = 0;
                this.setColor((float) xa, (float) ya, (float) za);
                size = 0.16F + random.nextFloat() * 0.1F;
                this.fadeIn = 0.0F;
                this.fadeOut = 0.6F;
                this.maxAlpha = 0.7F;
                this.roll = random.nextInt(4) * Mth.HALF_PI;
                glow = false;
            }
            case GLOW_SPLAT -> {
                this.lifetime = 14 + random.nextInt(12);
                this.gravity = 0.6F;
                this.hasPhysics = true;
                if (xa == 0 && ya == 0 && za == 0) {
                    this.xd = (random.nextDouble() - 0.5) * 0.2;
                    this.yd = 0.1 + random.nextDouble() * 0.15;
                    this.zd = (random.nextDouble() - 0.5) * 0.2;
                }
                size = 0.06F + random.nextFloat() * 0.05F;
                this.setColor(0.8F, 1.0F, 0.6F);
            }
            case WISHING_STAR -> {
                this.lifetime = 30 + random.nextInt(20);
                double ang = random.nextDouble() * Mth.TWO_PI;
                double speed = 0.9 + random.nextDouble() * 0.6;
                this.xd = Math.cos(ang) * speed;
                this.yd = -0.25 - random.nextDouble() * 0.2;
                this.zd = Math.sin(ang) * speed;
                this.friction = 1.0F;
                size = 0.35F + random.nextFloat() * 0.25F;
                this.setColor(1.0F, 0.95F, 0.8F);
                this.fadeIn = 0.15F;
                this.fadeOut = 0.4F;
            }
            case SLEEP_SPORE -> {
                this.lifetime = 40 + random.nextInt(30);
                if (xa == 0 && ya == 0 && za == 0) {
                    this.xd = (random.nextDouble() - 0.5) * 0.08;
                    this.yd = random.nextDouble() * 0.04;
                    this.zd = (random.nextDouble() - 0.5) * 0.08;
                }
                this.friction = 0.93F;
                size = 0.07F + random.nextFloat() * 0.05F;
                this.setColor(0.72F, 0.55F, 1.0F);
                this.maxAlpha = 0.85F;
            }
        }
        this.bright = glow;
        this.baseSize = size;
        this.ringRadius = ring;
        this.quadSize = size;
        this.alpha = 0.0F;
        this.setSpriteFromAge(sprites);
    }

    private void pickSiftColor(float t) {
        float f = Mth.frac(t) * (SIFT_COLORS.length - 1);
        int i = (int) f;
        float k = f - i;
        float[] a = SIFT_COLORS[i];
        float[] b = SIFT_COLORS[Math.min(i + 1, SIFT_COLORS.length - 1)];
        this.setColor(Mth.lerp(k, a[0], b[0]), Mth.lerp(k, a[1], b[1]), Mth.lerp(k, a[2], b[2]));
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = this.age / (float) this.lifetime;
        this.oRoll = this.roll;
        switch (this.kind) {
            case DRIFTING_SOUL -> {
                this.xd += Mth.sin(this.age * 0.08F + this.swayPhase) * 0.0012;
                this.zd += Mth.cos(this.age * 0.07F + this.swayPhase) * 0.0012;
                this.quadSize = this.baseSize * (1.0F + 0.15F * Mth.sin(this.age * 0.2F + this.swayPhase));
                this.pickSiftColor(this.colorShift + life * 0.3F);
            }
            case CHROME_DROPLET -> this.pickSiftColor(this.colorShift + this.age * 0.05F);
            case CHROME_BUBBLE -> {
                this.xd += Mth.sin(this.age * 0.3F + this.swayPhase) * 0.002;
                this.quadSize = this.baseSize * (1.0F + life * 0.4F);
            }
            case DREAM_POLLEN, GLOW_DUST -> {
                this.xd += (this.random.nextDouble() - 0.5) * 0.002;
                this.zd += (this.random.nextDouble() - 0.5) * 0.002;
                if (this.kind == Kind.GLOW_DUST) {
                    this.maxAlpha = 0.6F + 0.4F * Mth.sin(this.age * 0.3F + this.swayPhase);
                }
            }
            case SIFT_NOTE -> this.quadSize = this.baseSize * (life < 0.15F ? 0.4F + life * 4.0F : 1.0F);
            case RESONANCE_RING -> this.quadSize = this.baseSize + this.ringRadius * Anim01.easeOut(life);
            case LEAF -> {
                if (this.onGround) {
                    this.xd = 0;
                    this.zd = 0;
                } else {
                    this.xd += Mth.sin(this.age * 0.1F + this.swayPhase) * 0.0025;
                    this.zd += Mth.cos(this.age * 0.08F + this.swayPhase) * 0.0025;
                    this.roll += this.spin + Mth.sin(this.age * 0.12F) * 0.04F;
                    this.xd *= 0.96;
                    this.zd *= 0.96;
                    this.yd *= 0.9;
                }
            }
            case SIFT_MIST -> this.quadSize = this.baseSize * (1.0F + life * 0.6F);
            case STAR_SPARKLE -> {
                this.quadSize = this.baseSize * (0.6F + 0.6F * Math.abs(Mth.sin(this.age * 0.45F + this.swayPhase)));
                this.roll += 0.1F;
            }
            case PORTAL_SOUL -> {
                this.xd += Mth.sin(this.age * 0.35F + this.swayPhase) * 0.006;
                this.zd += Mth.cos(this.age * 0.35F + this.swayPhase) * 0.006;
                this.pickSiftColor(this.colorShift + life);
            }
            case FOOTSTEP_PUFF -> this.quadSize = this.baseSize * (1.0F + life * 1.5F);
            case WISHING_STAR -> {
                if (this.age % 2 == 0) {
                    this.level.addParticle(net.minecraft.core.particles.ParticleTypes.END_ROD, this.x, this.y, this.z, 0, 0, 0);
                }
            }
            case SLEEP_SPORE -> this.roll += this.spin;
            default -> {
            }
        }
        this.alpha = this.maxAlpha * this.fade(life);
        this.setSpriteFromAge(this.sprites);
    }

    private float fade(float life) {
        if (this.fadeIn > 0 && life < this.fadeIn) {
            return life / this.fadeIn;
        }
        if (life > 1.0F - this.fadeOut) {
            return Math.max(0.0F, (1.0F - life) / this.fadeOut);
        }
        return 1.0F;
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        if (this.kind == Kind.RESONANCE_RING || this.kind == Kind.SLIME_TRAIL) {
            // Lie flat on the ground like a ripple.
            return (Quaternionf target, Camera camera, float partial) -> target.rotationX(-Mth.HALF_PI);
        }
        return super.getFacingCameraMode();
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partial) {
        int base = super.getLightCoords(partial);
        return this.bright ? LightCoordsUtil.withBlock(base, 15) : base;
    }

    /** Tiny easing helper kept local to avoid a client-model dependency. */
    private static final class Anim01 {
        static float easeOut(float t) {
            t = Mth.clamp(t, 0.0F, 1.0F);
            return 1.0F - (1.0F - t) * (1.0F - t);
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final Kind kind;
        private final SpriteSet sprites;

        public Provider(Kind kind, SpriteSet sprites) {
            this.kind = kind;
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
                RandomSource random) {
            return new SiftParticle(this.kind, level, x, y, z, xa, ya, za, this.sprites, random);
        }
    }
}
