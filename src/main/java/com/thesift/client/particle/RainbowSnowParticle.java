package com.thesift.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;

/**
 * W-land: a flake of Rainbow Snow. It drifts down slowly with a gentle sway and spin, its pastel colour turning round the
 * rainbow as it falls, and fades out when it lands.
 */
public class RainbowSnowParticle extends SingleQuadParticle {
    private final float hue;
    private final float sway;
    private final float spin;

    protected RainbowSnowParticle(ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites,
            RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.setSprite(sprites.get(random.nextInt(3), 2));
        this.hue = random.nextFloat();
        this.sway = random.nextFloat() * Mth.TWO_PI;
        this.spin = (random.nextFloat() - 0.5F) * 0.08F;
        this.lifetime = 140 + random.nextInt(100);
        this.gravity = 0.01F;
        this.hasPhysics = true;
        this.friction = 1.0F;
        this.xd = xa + (random.nextDouble() - 0.5) * 0.01;
        this.yd = ya - 0.02 - random.nextDouble() * 0.02;
        this.zd = za + (random.nextDouble() - 0.5) * 0.01;
        this.quadSize = 0.05F + random.nextFloat() * 0.04F;
        this.roll = random.nextFloat() * Mth.TWO_PI;
        this.oRoll = this.roll;
        this.alpha = 0.0F;
        this.tint(0);
    }

    private void tint(float t) {
        float h = (this.hue + t) % 1.0F * 6.0F;
        int i = (int) h;
        float f = h - i;
        float s = 0.38F;
        float p = 1.0F - s;
        float q = 1.0F - s * f;
        float u = 1.0F - s * (1.0F - f);
        switch (i) {
            case 0 -> this.setColor(1.0F, u, p);
            case 1 -> this.setColor(q, 1.0F, p);
            case 2 -> this.setColor(p, 1.0F, u);
            case 3 -> this.setColor(p, q, 1.0F);
            case 4 -> this.setColor(u, p, 1.0F);
            default -> this.setColor(1.0F, p, q);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = this.age / (float) this.lifetime;
        this.oRoll = this.roll;
        if (this.onGround) {
            this.xd = 0.0;
            this.zd = 0.0;
            this.age = Math.max(this.age, this.lifetime - 20);
        } else {
            this.xd += Mth.sin(this.age * 0.09F + this.sway) * 0.0016;
            this.zd += Mth.cos(this.age * 0.07F + this.sway) * 0.0016;
            this.xd *= 0.97;
            this.zd *= 0.97;
            this.yd = Math.max(this.yd, -0.045);
            this.roll += this.spin;
        }
        this.tint(life * 0.6F);
        this.alpha = life < 0.08F ? life / 0.08F : (life > 0.85F ? Math.max(0.0F, (1.0F - life) / 0.15F) : 1.0F);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
                RandomSource random) {
            return new RainbowSnowParticle(level, x, y, z, xa, ya, za, this.sprites, random);
        }
    }
}
