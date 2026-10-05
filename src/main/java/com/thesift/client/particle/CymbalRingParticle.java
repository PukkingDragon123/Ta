package com.thesift.client.particle;

import com.thesift.entity.swamp.Cypole;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;

/**
 * The Cypole's shockwave: a flat brass ring lying on the ground and racing outwards exactly as fast
 * as the real one (the server's ring in {@link Cypole}), so what you see is what you must jump.
 * Dust and sparks kick up all along its front. xa carries its last radius, ya its speed a tick.
 */
public class CymbalRingParticle extends SingleQuadParticle {
    private final float speed;
    private final SpriteSet sprites;

    protected CymbalRingParticle(ClientLevel level, double x, double y, double z, double radius, double speed, SpriteSet sprites) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.sprites = sprites;
        this.speed = (float) (speed > 0.0 ? speed : Cypole.RING_SPEED);
        float last = (float) (radius > 0.0 ? radius : Cypole.RING_RADIUS);
        this.lifetime = Math.max(4, Mth.ceil((last - Cypole.RING_START) / this.speed));
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        this.quadSize = Cypole.RING_START;
        this.alpha = 0.95F;
        this.setSpriteFromAge(sprites);
    }

    private float radius(float partial) {
        return Cypole.RING_START + this.speed * (this.age + partial);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = this.age / (float) this.lifetime;
        this.alpha = 0.95F * (life < 0.7F ? 1.0F : Math.max(0.0F, (1.0F - life) / 0.3F));
        this.setSpriteFromAge(this.sprites);
        // dust and brass sparks thrown up along the front
        float r = this.radius(0.0F);
        int n = 3 + (int) (r * 1.2F);
        for (int i = 0; i < n; i++) {
            float a = this.random.nextFloat() * Mth.TWO_PI;
            double px = this.x + Mth.cos(a) * r;
            double pz = this.z + Mth.sin(a) * r;
            if (this.random.nextInt(3) == 0) {
                this.level.addParticle(ParticleTypes.WAX_ON, px, this.y + 0.15, pz, Mth.cos(a) * 0.4, 0.3, Mth.sin(a) * 0.4);
            } else {
                this.level.addParticle(ParticleTypes.POOF, px, this.y + 0.1, pz, Mth.cos(a) * 0.08, 0.02, Mth.sin(a) * 0.08);
            }
        }
    }

    @Override
    public float getQuadSize(float partial) {
        return this.radius(partial);
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        // lying flat on the ground
        return (Quaternionf target, Camera camera, float partial) -> target.rotationX(-Mth.HALF_PI);
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
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
                RandomSource random) {
            return new CymbalRingParticle(level, x, y, z, xa, ya, this.sprites);
        }
    }
}
