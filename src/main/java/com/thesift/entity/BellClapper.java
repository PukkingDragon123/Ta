package com.thesift.entity;

import net.minecraft.util.Mth;

/**
 * CR1: the bell of a Sifter, simulated on the client. The bell rocks on its lip like a weeble, sprung
 * towards wherever its gait and its mood lean it; the iron clapper inside hangs free - gravity pulls it
 * plumb, the creature's starts, stops and turns throw it about, and a rocking bell drags its pivot
 * along. When the clapper meets the lip it bounces back and {@link #tick} reports how hard it struck,
 * so the Sifter plays a tink as loud as the blow (tuned in tools: a steady walk tinks every second or
 * so, a charge jangles, a nap now and then).
 *
 * <p>All angles are radians about the model's X axis (positive: the bell's top tips forward, the
 * clapper's bob swings back) and Z axis (positive: the bell's top tips to its left, the bob swings to
 * its right). Renderers interpolate between the {@code ...O} and current values.
 */
public final class BellClapper {
    /** How far the clapper swings off the bell's axis before its bob meets the lip. */
    public static final float REACH = 0.36F;
    private static final float ROCK_SPRING = 0.25F;
    private static final float ROCK_DAMP = 0.25F;
    private static final float GRAVITY = 0.075F;
    private static final float SWING_DAMP = 0.035F;
    /** The clapper's pivot height over the rocking point, over the clapper's length: how hard a rocking bell drags it. */
    private static final float LEVER = 0.95F;
    /** How hard the creature's own acceleration (blocks/tick²) throws the clapper. */
    private static final float THROW = 2.6F;
    private static final float BOUNCE = 0.3F;

    public float rockX;
    public float rockZ;
    public float rockXO;
    public float rockZO;
    public float swingX;
    public float swingZ;
    public float swingXO;
    public float swingZO;
    private float rockVX;
    private float rockVZ;
    private float swingVX;
    private float swingVZ;
    /** The glow of the last strike: 1 right after a hard one, fading out over half a second. */
    public float ring;
    public float ringO;

    /** Pushes the bell over (radians per tick): a footfall, a fidget, a jolt. */
    public void nudge(float x, float z) {
        this.rockVX += x;
        this.rockVZ += z;
    }

    /** Throws the clapper (radians per tick). */
    public void fling(float x, float z) {
        this.swingVX += x;
        this.swingVZ += z;
    }

    /** Lights the runes, as a strike of this strength would. */
    public void glow(float strength) {
        this.ring = Math.max(this.ring, Mth.clamp(strength * 6.0F, 0.15F, 1.0F));
    }

    /**
     * One client tick. {@code leanX}/{@code leanZ}: where the gait and the mood want the bell;
     * {@code accFwd}/{@code accLeft}: the creature's acceleration along its facing and to its left
     * (blocks per tick²). Returns how hard the clapper struck the lip this tick (0: it did not).
     */
    public float tick(float leanX, float leanZ, float accFwd, float accLeft) {
        this.rockXO = this.rockX;
        this.rockZO = this.rockZ;
        this.swingXO = this.swingX;
        this.swingZO = this.swingZ;
        this.ringO = this.ring;
        this.ring = Math.max(0.0F, this.ring - 0.09F);
        // the bell rocks on its lip towards its lean
        float ax = (leanX - this.rockX) * ROCK_SPRING - this.rockVX * ROCK_DAMP;
        float az = (leanZ - this.rockZ) * ROCK_SPRING - this.rockVZ * ROCK_DAMP;
        this.rockVX += ax;
        this.rockVZ += az;
        this.rockX += this.rockVX;
        this.rockZ += this.rockVZ;
        // the clapper: pulled plumb, dragged by the rocking bell, thrown by the creature's starts and turns
        this.swingVX += -GRAVITY * this.swingX - SWING_DAMP * this.swingVX + LEVER * ax + THROW * accFwd;
        this.swingVZ += -GRAVITY * this.swingZ - SWING_DAMP * this.swingVZ + LEVER * az + THROW * accLeft;
        this.swingX += this.swingVX;
        this.swingZ += this.swingVZ;
        // the lip: the clapper may swing REACH off the bell's axis, no further
        float rx = this.swingX - this.rockX;
        float rz = this.swingZ - this.rockZ;
        float d = Mth.sqrt(rx * rx + rz * rz);
        if (d <= REACH) {
            return 0.0F;
        }
        float nx = rx / d;
        float nz = rz / d;
        this.swingX = this.rockX + nx * REACH;
        this.swingZ = this.rockZ + nz * REACH;
        float vn = (this.swingVX - this.rockVX) * nx + (this.swingVZ - this.rockVZ) * nz;
        if (vn <= 0.0F) {
            return 0.0F;
        }
        this.swingVX -= (1.0F + BOUNCE) * vn * nx;
        this.swingVZ -= (1.0F + BOUNCE) * vn * nz;
        // the bell takes a little of the blow
        this.rockVX += vn * nx * 0.15F;
        this.rockVZ += vn * nz * 0.15F;
        return vn;
    }

    public float rockX(float partial) {
        return Mth.lerp(partial, this.rockXO, this.rockX);
    }

    public float rockZ(float partial) {
        return Mth.lerp(partial, this.rockZO, this.rockZ);
    }

    public float swingX(float partial) {
        return Mth.lerp(partial, this.swingXO, this.swingX);
    }

    public float swingZ(float partial) {
        return Mth.lerp(partial, this.swingZO, this.swingZ);
    }

    public float ring(float partial) {
        return Mth.lerp(partial, this.ringO, this.ring);
    }
}
