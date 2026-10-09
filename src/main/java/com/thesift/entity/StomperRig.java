package com.thesift.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * S1 the Stomper's trunk rig, shared by the client model (which poses the trunk from it) and the
 * entity (which holds a grabbed victim at the trunk tip on both sides, so a held player never
 * rubber-bands). The rest geometry mirrors tools/stomper.py (BP, HP, TRUNK; the build checks it).
 *
 * <p>A grab runs through phases, each a fixed number of ticks; the pose is a pure function of the
 * phase and the time in it: {@link #pose}.</p>
 */
public final class StomperRig {
    /** Body pivot (the hind hips), model units. */
    public static final float BODY_Y = -3.0F;
    public static final float BODY_Z = 10.0F;
    /** Head pivot relative to the body pivot; trunk root relative to the head pivot. */
    public static final float HEAD_Y = -7.0F;
    public static final float HEAD_Z = -28.0F;
    public static final float TRUNK_Y = 0.0F;
    public static final float TRUNK_Z = -13.5F;
    /** Pivot-to-pivot length and rest pitch of each trunk segment, root to tip. */
    public static final float[] SEG = {7.0F, 7.0F, 6.0F, 6.0F, 5.0F};
    public static final float[] REST = {-0.05F, 0.0F, -0.05F, -0.3F, -0.6F};
    public static final int SEGMENTS = 5;

    public static final int NONE = 0;
    /** Swings the trunk out at the target (it is caught at the end, or the grab misses). */
    public static final int REACH = 1;
    /** Wraps the tip round the victim and lifts it high. */
    public static final int LIFT = 2;
    /** Holds it up and shakes it. */
    public static final int HOLD = 3;
    /** Slams it into the ground in front. */
    public static final int SLAM = 4;
    /** Whips it away to the side. */
    public static final int FLING = 5;
    /** Nothing caught: the trunk swings back. */
    public static final int MISS = 6;
    /** The victim fought free: the trunk recoils. */
    public static final int DROP = 7;
    public static final int[] LENGTH = {0, 10, 12, 30, 10, 8, 10, 10};
    /** The tick of SLAM / FLING at which the victim is let go. */
    public static final int SLAM_RELEASE = 5;
    public static final int FLING_RELEASE = 3;

    // key poses: head pitch, then each segment's pitch (absolute); the yaw of the first segment comes separately
    private static final float[] P_REST = {0.0F, -0.05F, 0.0F, -0.05F, -0.3F, -0.6F};
    private static final float[] P_REACH = {0.3F, -1.05F, -0.2F, -0.1F, 0.0F, -0.15F};
    private static final float[] P_WRAP = {0.25F, -1.0F, -0.15F, 0.05F, 0.6F, 0.9F};
    private static final float[] P_LIFT = {-0.35F, -2.5F, -0.35F, -0.2F, 0.45F, 0.9F};
    private static final float[] P_SLAM_UP = {-0.45F, -2.8F, -0.3F, -0.1F, 0.4F, 0.8F};
    private static final float[] P_SLAM_DOWN = {0.35F, -0.75F, -0.1F, -0.05F, 0.3F, 0.6F};
    private static final float[] P_FLING = {-0.1F, -1.8F, -0.3F, -0.2F, 0.2F, 0.3F};

    private StomperRig() {
    }

    public static boolean holding(int phase) {
        return phase == LIFT || phase == HOLD || (phase == SLAM) || phase == FLING;
    }

    /**
     * The grab pose at {@code t} ticks into {@code phase}: out[0] head pitch, out[1] first trunk
     * segment yaw, out[2 + i] trunk segment i pitch. Returns false (and leaves out alone) when
     * there is no grab.
     */
    public static boolean pose(int phase, float t, float[] out) {
        float yaw = 0.0F;
        switch (phase) {
            case REACH -> mix(P_REST, P_REACH, smooth(t / 8.0F), out);
            case LIFT -> {
                if (t < 3.0F) {
                    mix(P_REACH, P_WRAP, smooth(t / 3.0F), out);
                } else {
                    mix(P_WRAP, P_LIFT, backOut((t - 3.0F) / 9.0F), out);
                }
            }
            case HOLD -> {
                mix(P_LIFT, P_LIFT, 0.0F, out);
                float shake = Math.min(1.0F, t / 4.0F);
                out[2] += Mth.sin(t * 0.9F) * 0.12F * shake;
                out[0] += Mth.sin(t * 0.45F) * 0.05F * shake;
                yaw = Mth.sin(t * 0.6F) * 0.18F * shake;
            }
            case SLAM -> {
                if (t < 3.0F) {
                    mix(P_LIFT, P_SLAM_UP, smooth(t / 3.0F), out);
                } else if (t < SLAM_RELEASE) {
                    float k = (t - 3.0F) / (SLAM_RELEASE - 3.0F);
                    mix(P_SLAM_UP, P_SLAM_DOWN, k * k, out);
                } else {
                    mix(P_SLAM_DOWN, P_REST, smooth((t - SLAM_RELEASE) / (LENGTH[SLAM] - SLAM_RELEASE)), out);
                }
            }
            case FLING -> {
                if (t < 2.0F) {
                    mix(P_LIFT, P_LIFT, 0.0F, out);
                    yaw = -0.5F * smooth(t / 2.0F);
                } else if (t < 4.0F) {
                    float k = smooth((t - 2.0F) / 2.0F);
                    mix(P_LIFT, P_FLING, k, out);
                    yaw = Mth.lerp(k, -0.5F, 1.1F);
                } else {
                    float k = smooth((t - 4.0F) / 4.0F);
                    mix(P_FLING, P_REST, k, out);
                    yaw = Mth.lerp(k, 1.1F, 0.0F);
                }
            }
            case MISS -> {
                mix(P_REACH, P_REST, smooth(t / 10.0F), out);
                out[6] += Mth.sin(t * 1.3F) * 0.3F * (1.0F - t / 10.0F);
            }
            case DROP -> mix(P_LIFT, P_REST, backOut(t / 10.0F), out);
            default -> {
                return false;
            }
        }
        out[1] = yaw;
        return true;
    }

    private static void mix(float[] a, float[] b, float k, float[] out) {
        out[0] = Mth.lerp(k, a[0], b[0]);
        for (int i = 0; i < SEGMENTS; i++) {
            out[2 + i] = Mth.lerp(k, a[1 + i], b[1 + i]);
        }
    }

    public static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    public static float backOut(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        float c1 = 1.70158F;
        return 1.0F + (c1 + 1.0F) * (t - 1.0F) * (t - 1.0F) * (t - 1.0F) + c1 * (t - 1.0F) * (t - 1.0F);
    }

    /**
     * Where the trunk tip is in the world for a pose (see {@link #pose}), for an adult standing at
     * {@code pos} facing {@code bodyYawDeg}. Model space has y down and the front at -z.
     */
    public static Vec3 tip(Vec3 pos, float bodyYawDeg, float scale, float[] pose) {
        // walk the chain in model space: body (unrotated during a grab) -> head -> trunk segments
        double[] m = {1, 0, 0, 0, 1, 0, 0, 0, 1};
        double px = 0.0;
        double py = BODY_Y + HEAD_Y;
        double pz = BODY_Z + HEAD_Z;
        m = mul(m, rotX(pose[0]));
        double[] o = apply(m, 0.0, TRUNK_Y, TRUNK_Z);
        px += o[0];
        py += o[1];
        pz += o[2];
        for (int i = 0; i < SEGMENTS; i++) {
            if (i == 0) {
                m = mul(m, rotY(pose[1]));
            }
            m = mul(m, rotX(pose[2 + i]));
            float len = SEG[i] + (i == SEGMENTS - 1 ? 1.0F : 0.0F);
            double[] d = apply(m, 0.0, len, 0.0);
            px += d[0];
            py += d[1];
            pz += d[2];
        }
        float yaw = bodyYawDeg * Mth.DEG_TO_RAD;
        double fx = -Mth.sin(yaw);
        double fz = Mth.cos(yaw);
        double lx = Mth.cos(yaw);
        double lz = Mth.sin(yaw);
        double forward = -pz / 16.0 * scale;
        double left = px / 16.0 * scale;
        double up = (24.0 - py) / 16.0 * scale;
        return new Vec3(pos.x + fx * forward + lx * left, pos.y + up, pos.z + fz * forward + lz * left);
    }

    private static double[] rotX(float a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[]{1, 0, 0, 0, c, -s, 0, s, c};
    }

    private static double[] rotY(float a) {
        double c = Math.cos(a);
        double s = Math.sin(a);
        return new double[]{c, 0, s, 0, 1, 0, -s, 0, c};
    }

    private static double[] mul(double[] a, double[] b) {
        double[] r = new double[9];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                r[i * 3 + j] = a[i * 3] * b[j] + a[i * 3 + 1] * b[3 + j] + a[i * 3 + 2] * b[6 + j];
            }
        }
        return r;
    }

    private static double[] apply(double[] m, double x, double y, double z) {
        return new double[]{m[0] * x + m[1] * y + m[2] * z, m[3] * x + m[4] * y + m[5] * z, m[6] * x + m[7] * y + m[8] * z};
    }
}
