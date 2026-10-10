package com.thesift.client.music;

import com.thesift.registry.ModInstrumentFx;
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
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import org.joml.Quaternionf;

/**
 * INS free play: the notes, rings and breath of a played instrument ({@link ModInstrumentFx}).
 * xa is the colour (packed RGB), ya the direction the player faces (degrees, Minecraft yaw), za the
 * strength (notes, breath) or size (rings).
 *
 * <ul>
 *   <li>NOTE / NOTES: a note glyph pops out of the instrument, flies off ahead and to the side, slows,
 *   then floats up swaying, glowing in its colour, and fades.</li>
 *   <li>RING: a soft ring of sound, upright and facing where the player faces, spreading and fading.</li>
 *   <li>BREATH: a pale wisp drifting forward and up, growing as it thins out.</li>
 * </ul>
 */
public class InstrumentParticle extends SingleQuadParticle {
    public enum Kind { NOTE, RING, BREATH }

    private final Kind kind;
    private final float base;
    private final float strength;
    private final float yawRad;
    private final float sway;

    protected InstrumentParticle(Kind kind, ClientLevel level, double x, double y, double z, double xa, double ya, double za, SpriteSet sprites,
            RandomSource random) {
        super(level, x, y, z, 0.0, 0.0, 0.0, sprites.first());
        this.kind = kind;
        this.hasPhysics = false;
        this.gravity = 0.0F;
        int rgb = (int) xa;
        this.setColor((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F);
        this.yawRad = (float) ya * Mth.DEG_TO_RAD;
        this.strength = (float) Mth.clamp(za, 0.2, 3.0);
        this.sway = random.nextFloat() * Mth.TWO_PI;
        // the way the player faces, and to their sides
        float fx = -Mth.sin(this.yawRad);
        float fz = Mth.cos(this.yawRad);
        switch (kind) {
            case NOTE -> {
                this.lifetime = 30 + random.nextInt(12);
                float side = (random.nextFloat() - 0.5F) * 1.4F;
                float speed = (0.07F + random.nextFloat() * 0.05F) * this.strength;
                this.xd = (fx + fz * side) * speed;
                this.zd = (fz - fx * side) * speed;
                this.yd = 0.04 + random.nextFloat() * 0.04;
                this.friction = 0.88F;
                this.base = 0.13F + 0.04F * this.strength;
                this.roll = (random.nextFloat() - 0.5F) * 0.5F;
            }
            case RING -> {
                this.lifetime = 14;
                this.friction = 1.0F;
                this.base = 0.12F * this.strength;
                this.xd = fx * 0.02;
                this.zd = fz * 0.02;
            }
            default -> {
                this.lifetime = 18 + random.nextInt(10);
                float speed = 0.03F * this.strength;
                this.xd = fx * speed + (random.nextFloat() - 0.5F) * 0.01F;
                this.zd = fz * speed + (random.nextFloat() - 0.5F) * 0.01F;
                this.yd = 0.006 + random.nextFloat() * 0.01;
                this.friction = 0.94F;
                this.base = 0.09F;
            }
        }
        this.quadSize = this.base * 0.4F;
        this.alpha = this.kind == Kind.BREATH ? 0.0F : 1.0F;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float life = this.age / (float) this.lifetime;
        switch (this.kind) {
            case NOTE -> {
                // pop in with a little overshoot, then float up and sway while fading
                float pop = life < 0.12F ? Mth.sin(life / 0.12F * Mth.HALF_PI) * 1.15F : 1.15F - Math.min(0.15F, (life - 0.12F) * 1.5F);
                this.quadSize = this.base * pop;
                if (life > 0.25F) {
                    this.yd += 0.0022;
                    this.xd += Mth.sin(this.age * 0.32F + this.sway) * 0.0016;
                    this.zd += Mth.cos(this.age * 0.29F + this.sway) * 0.0016;
                }
                this.oRoll = this.roll;
                this.roll = Mth.sin(this.age * 0.2F + this.sway) * 0.3F;
                this.alpha = life > 0.6F ? Math.max(0.0F, 1.0F - (life - 0.6F) / 0.4F) : 1.0F;
            }
            case RING -> {
                float out = 1.0F - (1.0F - life) * (1.0F - life);
                this.quadSize = this.base + 0.55F * this.strength * out;
                this.alpha = 0.7F * (1.0F - life) * (1.0F - life);
            }
            default -> {
                this.quadSize = this.base * (1.0F + life * 2.2F);
                this.alpha = 0.35F * Mth.sin(life * Mth.PI);
            }
        }
    }

    @Override
    public SingleQuadParticle.FacingCameraMode getFacingCameraMode() {
        if (this.kind == Kind.RING) {
            // upright, facing the way the player faces
            float yaw = this.yawRad;
            return (Quaternionf target, Camera camera, float partial) -> target.rotationY(-yaw);
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
        return this.kind == Kind.BREATH ? base : LightCoordsUtil.withBlock(base, 15);
    }

    /** Registers the providers (from FreePlay.register). */
    public static void register(IEventBus modBus) {
        modBus.addListener(InstrumentParticle::registerProviders);
    }

    private static void registerProviders(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModInstrumentFx.NOTE.get(), sprites -> new Provider(Kind.NOTE, sprites));
        event.registerSpriteSet(ModInstrumentFx.NOTES.get(), sprites -> new Provider(Kind.NOTE, sprites));
        event.registerSpriteSet(ModInstrumentFx.RING.get(), sprites -> new Provider(Kind.RING, sprites));
        event.registerSpriteSet(ModInstrumentFx.BREATH.get(), sprites -> new Provider(Kind.BREATH, sprites));
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
            return new InstrumentParticle(this.kind, level, x, y, z, xa, ya, za, this.sprites, random);
        }
    }
}
