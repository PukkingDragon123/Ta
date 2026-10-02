package com.thesift.client.particle;

import com.thesift.client.ChromeClient;
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
 * A3 Chrome's particles: rainbow ripples lying on Chrome's surface, rising twinkles and the soft
 * motes of the Rainbow Daze. Each slowly turns through the colours of the rainbow as it lives.
 */
public class ChromeParticle extends SingleQuadParticle {
    public enum Kind { RIPPLE, SPARK, MOTE }

    private final Kind kind;
    private final float hue;
    private final float baseSize;
    private final float spin;
    private float maxAlpha = 1.0F;

    protected ChromeParticle(Kind kind, ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites,
            RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.friction = 0.96F;
        this.spin = (random.nextFloat() - 0.5F) * 0.2F;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        switch (kind) {
            case RIPPLE -> {
                // xa: hue, ya: the size of whatever made it
                this.hue = (float) xa;
                this.lifetime = 22 + random.nextInt(8);
                this.baseSize = 0.15F + (float) Mth.clamp(ya, 0.2, 2.0) * 0.3F;
                this.maxAlpha = 0.85F;
            }
            case SPARK -> {
                // xa: hue, ya: upward speed (0 for a default hop)
                this.hue = (float) xa;
                this.lifetime = 14 + random.nextInt(14);
                this.xd = (random.nextDouble() - 0.5) * 0.03;
                this.yd = ya > 0.0 ? ya : 0.04 + random.nextDouble() * 0.04;
                this.zd = (random.nextDouble() - 0.5) * 0.03;
                this.gravity = 0.06F;
                this.baseSize = 0.06F + random.nextFloat() * 0.05F;
                this.setSprite(sprites.get(random.nextInt(3), 2));
            }
            default -> {
                this.hue = random.nextFloat();
                this.lifetime = 30 + random.nextInt(30);
                this.xd = (random.nextDouble() - 0.5) * 0.02;
                this.yd = 0.008 + random.nextDouble() * 0.012;
                this.zd = (random.nextDouble() - 0.5) * 0.02;
                this.baseSize = 0.05F + random.nextFloat() * 0.06F;
                this.maxAlpha = 0.9F;
                this.setSprite(sprites.get(random.nextInt(2), 1));
            }
        }
        this.quadSize = this.baseSize;
        this.alpha = 0.0F;
        this.applyHue(0.0F);
    }

    private void applyHue(float life) {
        int rgb = ChromeClient.hsv(this.hue + life * 0.35F, this.kind == Kind.RIPPLE ? 0.55F : 0.6F, 1.0F);
        this.setColor(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F);
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
            case RIPPLE -> {
                float e = 1.0F - (1.0F - life) * (1.0F - life);
                this.quadSize = this.baseSize * (1.0F + 3.2F * e);
                this.alpha = this.maxAlpha * (1.0F - life) * (1.0F - life);
            }
            case SPARK -> {
                this.roll += this.spin;
                this.quadSize = this.baseSize * (0.7F + 0.5F * Math.abs(Mth.sin(this.age * 0.5F)));
                this.alpha = this.maxAlpha * fade(life);
            }
            default -> {
                this.xd += Mth.sin(this.age * 0.15F + this.spin * 30.0F) * 0.0012;
                this.zd += Mth.cos(this.age * 0.13F + this.spin * 30.0F) * 0.0012;
                this.alpha = this.maxAlpha * fade(life);
            }
        }
        this.applyHue(life);
    }

    private static float fade(float life) {
        if (life < 0.15F) {
            return life / 0.15F;
        }
        return life > 0.6F ? Math.max(0.0F, (1.0F - life) / 0.4F) : 1.0F;
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        if (this.kind == Kind.RIPPLE) {
            // lie flat on the surface
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
        return LightCoordsUtil.withBlock(super.getLightCoords(partial), 15);
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
            return new ChromeParticle(this.kind, level, x, y, z, xa, ya, za, this.sprites, random);
        }
    }
}
