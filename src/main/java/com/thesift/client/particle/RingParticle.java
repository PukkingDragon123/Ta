package com.thesift.client.particle;

import com.thesift.registry.ModRings;
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
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Quaternionf;

/**
 * CR1: rings of sound. {@link Kind#BELL}: a gold ring rippling out flat over the ground from a struck
 * Sifter, quick and bright, then gone. {@link Kind#ECHO}: an upright cyan ring that flies out along
 * its path, slows and widens and fades - the Echoer's echolocation. Both always turn their face to
 * the camera's side of their plane, so they read from above, below and behind.
 */
public class RingParticle extends SingleQuadParticle {
    public enum Kind { BELL, ECHO }

    private final Kind kind;
    private final float baseSize;
    private final float spread;
    private final float peak;
    private final float nx;
    private final float ny;
    private final float nz;

    protected RingParticle(Kind kind, ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites,
            RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        if (kind == Kind.BELL) {
            // xa: how far it spreads (blocks), ya: how bright
            this.spread = Mth.clamp((float) xa, 0.3F, 4.0F);
            this.peak = Mth.clamp((float) ya, 0.15F, 1.0F);
            this.baseSize = 0.2F;
            this.lifetime = 12 + random.nextInt(4);
            this.friction = 1.0F;
            this.xd = 0.0;
            this.yd = 0.0;
            this.zd = 0.0;
            this.nx = 0.0F;
            this.ny = 1.0F;
            this.nz = 0.0F;
            this.setColor(1.0F, 0.83F, 0.42F);
        } else {
            // xa, ya, za: its flight (it faces where it flies)
            double len = Math.sqrt(xa * xa + ya * ya + za * za);
            this.spread = 0.85F;
            this.peak = 0.8F;
            this.baseSize = 0.12F;
            this.lifetime = 16 + random.nextInt(5);
            this.friction = 0.9F;
            this.xd = xa;
            this.yd = ya;
            this.zd = za;
            this.nx = len < 1.0E-4 ? 0.0F : (float) (xa / len);
            this.ny = len < 1.0E-4 ? 0.0F : (float) (ya / len);
            this.nz = len < 1.0E-4 ? 1.0F : (float) (za / len);
            this.setColor(0.56F, 0.97F, 1.0F);
        }
        this.quadSize = this.baseSize;
        this.alpha = this.peak;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = this.age / (float) this.lifetime;
        float out = 1.0F - (1.0F - life) * (1.0F - life);
        this.quadSize = this.baseSize + this.spread * out;
        this.alpha = this.kind == Kind.BELL ? this.peak * (1.0F - life) * (1.0F - life) : this.peak * (1.0F - life * life);
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        return (Quaternionf target, Camera camera, float partial) -> {
            Vec3 cam = camera.position();
            double px = Mth.lerp(partial, this.xo, this.x);
            double py = Mth.lerp(partial, this.yo, this.y);
            double pz = Mth.lerp(partial, this.zo, this.z);
            float side = (cam.x - px) * this.nx + (cam.y - py) * this.ny + (cam.z - pz) * this.nz < 0.0 ? -1.0F : 1.0F;
            target.rotationTo(0.0F, 0.0F, 1.0F, this.nx * side, this.ny * side, this.nz * side);
        };
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public int getLightCoords(float partial) {
        return LightCoordsUtil.withBlock(super.getLightCoords(partial), 15);
    }

    /** Registers both rings' providers (from TheSiftClient). */
    public static void register(IEventBus modBus) {
        modBus.addListener(RingParticle::registerProviders);
    }

    private static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModRings.BELL_RING.get(), sprites -> new Provider(Kind.BELL, sprites));
        event.registerSpriteSet(ModRings.ECHO_RING.get(), sprites -> new Provider(Kind.ECHO, sprites));
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
            return new RingParticle(this.kind, level, x, y, z, xa, ya, za, this.sprites, random);
        }
    }
}
