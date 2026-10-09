package com.thesift.client.particle;

import com.thesift.registry.ModCaves;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.RandomSource;

/**
 * W-deep caves: acid. A DRIP hangs a moment from an Acid Weeper, falls and splashes into fizz where it lands; a FIZZ is a
 * little glowing bubble that swells as it rises off a puddle (or off whatever the acid is eating) and pops.
 */
public class AcidParticle extends SingleQuadParticle {
    public enum Kind { DRIP, FIZZ }

    private final Kind kind;
    private final SpriteSet sprites;
    private int hang;

    protected AcidParticle(Kind kind, ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites,
            RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.kind = kind;
        this.sprites = sprites;
        this.xd = xa;
        this.yd = ya;
        this.zd = za;
        if (kind == Kind.DRIP) {
            this.hang = 8 + random.nextInt(12);
            this.lifetime = 80;
            this.gravity = 0.0F;
            this.hasPhysics = true;
            this.quadSize = 0.07F + random.nextFloat() * 0.03F;
            this.friction = 1.0F;
        } else {
            this.lifetime = 10 + random.nextInt(10);
            this.gravity = -0.02F;
            this.hasPhysics = false;
            this.quadSize = 0.04F + random.nextFloat() * 0.03F;
            this.xd = (random.nextDouble() - 0.5) * 0.02 + xa;
            this.yd = 0.01 + random.nextDouble() * 0.02 + ya;
            this.zd = (random.nextDouble() - 0.5) * 0.02 + za;
            this.friction = 0.9F;
        }
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        if (this.kind == Kind.DRIP) {
            if (this.hang > 0) {
                this.hang--;
                this.xo = this.x;
                this.yo = this.y;
                this.zo = this.z;
                if (this.hang == 0) {
                    this.gravity = 0.06F;
                }
                if (this.age++ >= this.lifetime) {
                    this.remove();
                }
                return;
            }
            super.tick();
            if (this.onGround || this.removed) {
                // splash: a ring of fizz where the drop lands
                for (int i = 0; i < 4; i++) {
                    this.level.addParticle(ModCaves.ACID_FIZZ.get(), this.x, this.y + 0.05, this.z, (this.random.nextDouble() - 0.5) * 0.08, 0.02,
                            (this.random.nextDouble() - 0.5) * 0.08);
                }
                this.remove();
            }
            return;
        }
        super.tick();
        this.quadSize *= 1.04F;
        this.setSpriteFromAge(this.sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partial) {
        return LightCoordsUtil.withBlock(super.getLightCoords(partial), 12);
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
            return new AcidParticle(this.kind, level, x, y, z, xa, ya, za, this.sprites, random);
        }
    }
}
