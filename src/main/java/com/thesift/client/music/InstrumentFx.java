package com.thesift.client.music;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import com.thesift.registry.ModInstrumentFx;
import com.thesift.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * INS free play: what a note looks like. Every note flies out of the instrument as a glowing note in
 * the colour of the song being played (or the note's own colour, or the Prism light), with a soft
 * ring of sound; the flute breathes a wisp, a struck drum head shakes off dust, chimes glitter,
 * strings glint. Drawn on every client that sees the player, at the instrument itself - in first
 * person where the instrument sits in front of the camera.
 */
public final class InstrumentFx {
    private InstrumentFx() {
    }

    /** Where the instrument is (blocks, relative to the player's feet: x left, y up, z forward), by stance. */
    private static float[] body(Instrument ins) {
        if (ins == Instrument.HARP) {
            return new float[]{-0.05F, 1.1F, 0.48F};
        }
        return switch (ins.family()) {
            case STRINGS -> new float[]{-0.02F, 0.95F, 0.36F};
            case FLUTE -> new float[]{-0.34F, 1.62F, 0.58F};
            case DRUM -> new float[]{0.0F, 0.96F, 0.44F};
            case CHIMES -> new float[]{-0.3F, 1.36F, 0.56F};
        };
    }

    /** The instrument's place in the world: in front of the camera for the local player in first person. */
    public static Vec3 at(Player player, Instrument ins, float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (player == mc.player && mc.options.getCameraType().isFirstPerson()) {
            float[] b = FreePlayHand.base(ins);
            Vec3 eye = player.getEyePosition(partial);
            Vec3 fwd = player.getViewVector(partial);
            Vec3 right = fwd.cross(new Vec3(0.0, 1.0, 0.0));
            if (right.lengthSqr() < 1.0E-6) {
                right = new Vec3(1.0, 0.0, 0.0);
            }
            right = right.normalize();
            Vec3 up = right.cross(fwd).normalize();
            return eye.add(right.scale(b[0] * 0.9)).add(up.scale(b[1] * 0.6 + 0.05)).add(fwd.scale(-b[2]));
        }
        float[] o = body(ins);
        float yaw = player.yBodyRot * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        double lx = fz;
        double lz = -fx;
        Vec3 pos = player.getPosition(partial);
        float crouch = player.isCrouching() ? -0.2F : 0.0F;
        return new Vec3(pos.x + lx * o[0] + fx * o[2], pos.y + o[1] + crouch, pos.z + lz * o[0] + fz * o[2]);
    }

    /** The colour a note flies in: the song's, else the Prism light, else the note's own. */
    public static int colour(int pitch, int light, int tint) {
        if (tint >= 0) {
            return tint;
        }
        if (light >= 0) {
            return PrismLight.rgb(light);
        }
        return Notes.colour(pitch);
    }

    /** A note played (or a held note breathing on, with {@code sustain}). */
    public static void note(Player player, Instrument ins, int pitch, int flags, int light, int tint, boolean sustain) {
        if (!(player.level() instanceof ClientLevel level)) {
            return;
        }
        RandomSource rnd = level.getRandom();
        Vec3 at = at(player, ins, 1.0F);
        float yaw = player.getYRot();
        int rgb = colour(pitch, light, tint);
        boolean strong = (flags & (Notes.STRONG | Notes.CHORD)) != 0;
        if (sustain) {
            level.addParticle(ModInstrumentFx.BREATH.get(), at.x, at.y, at.z, 0xF4F8FF, yaw, 0.8);
            if (rnd.nextInt(3) == 0) {
                level.addParticle(ModInstrumentFx.NOTE.get(), at.x, at.y + 0.05, at.z, rgb, yaw, 0.5);
            }
            return;
        }
        level.addParticle(strong ? ModInstrumentFx.NOTES.get() : ModInstrumentFx.NOTE.get(), at.x, at.y + 0.08, at.z, rgb, yaw, strong ? 1.4 : 1.0);
        level.addParticle(ModInstrumentFx.RING.get(), at.x, at.y, at.z, lighten(rgb), yaw, strong ? 1.5 : 1.0);
        if ((flags & Notes.CHORD) != 0) {
            level.addParticle(ModInstrumentFx.NOTE.get(), at.x, at.y + 0.1, at.z, Notes.colour(Math.min(24, pitch + 4)), yaw + 25.0F, 1.1);
            level.addParticle(ModInstrumentFx.NOTE.get(), at.x, at.y + 0.1, at.z, Notes.colour(Math.min(24, pitch + 7)), yaw - 25.0F, 1.1);
        }
        if (ins == Instrument.HARP || ins.family() == Instrument.Family.STRINGS) {
            // a glint running along the strings
            level.addParticle(ModParticles.STAR_SPARKLE.get(), at.x + (rnd.nextDouble() - 0.5) * 0.3, at.y + (rnd.nextDouble() - 0.3) * 0.3,
                    at.z + (rnd.nextDouble() - 0.5) * 0.3, 0.0, 0.01, 0.0);
            return;
        }
        switch (ins.family()) {
            case FLUTE -> {
                for (int i = 0; i < 2; i++) {
                    level.addParticle(ModInstrumentFx.BREATH.get(), at.x, at.y, at.z, 0xF4F8FF, yaw + (rnd.nextFloat() - 0.5F) * 30.0F, 1.0);
                }
            }
            case DRUM -> {
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ModInstrumentFx.BREATH.get(), at.x + (rnd.nextDouble() - 0.5) * 0.3, at.y + 0.05, at.z + (rnd.nextDouble() - 0.5) * 0.3,
                            0xE8D8B8, yaw + rnd.nextFloat() * 360.0F, 0.6);
                }
            }
            case CHIMES -> {
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ModParticles.STAR_SPARKLE.get(), at.x + (rnd.nextDouble() - 0.5) * 0.5, at.y - rnd.nextDouble() * 0.4,
                            at.z + (rnd.nextDouble() - 0.5) * 0.5, 0.0, -0.005, 0.0);
                }
            }
            default -> {
            }
        }
    }

    /** A soft flash of the chosen Prism light around the instrument (the light was changed). */
    public static void light(Player player, Instrument ins, int light) {
        if (player.level() instanceof ClientLevel level) {
            Vec3 at = at(player, ins, 1.0F);
            level.addParticle(ModInstrumentFx.RING.get(), at.x, at.y, at.z, PrismLight.rgb(light), player.getYRot(), 0.8);
        }
    }

    private static int lighten(int rgb) {
        int r = Math.min(255, (rgb >> 16 & 0xFF) + 60);
        int g = Math.min(255, (rgb >> 8 & 0xFF) + 60);
        int b = Math.min(255, (rgb & 0xFF) + 60);
        return r << 16 | g << 8 | b;
    }
}
