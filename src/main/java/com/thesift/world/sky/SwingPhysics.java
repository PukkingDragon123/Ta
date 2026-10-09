package com.thesift.world.sky;

import net.minecraft.world.phys.Vec3;

/**
 * W-sky: the physics of swinging on a Sky Vine - a real rope pendulum, side-agnostic and free of game state so the CI
 * test can run it (dev/SkyTest) and the client can drive the player with it ({@link SkySwingClient}).
 *
 * <p>The hands hang from a pivot on a rope of a given length. Every tick is split into small steps: gravity pulls the
 * hands down, they move, and if that stretches the rope it pulls them back onto the sphere round the pivot and takes
 * away the outward part of their velocity (a slack rope - when you swing up past the pivot's height - lets you fly
 * freely until it goes taut again). The tangential speed is kept, so a swing loses only a little to the air each tick
 * and you can pump it higher. Units are blocks and ticks, like vanilla movement (vanilla gravity is 0.08).
 */
public final class SwingPhysics {
    /** Vanilla player gravity, blocks per tick per tick. */
    public static final double GRAVITY = 0.08;
    /** The air's gentle drag on a swing, per tick (a swing from 45 degrees comes back to about 40). */
    public static final double DRAG = 0.9985;
    /** No swing gets faster than this (blocks per tick). */
    public static final double MAX_SPEED = 2.4;
    /** Sub-steps per tick: enough that the rope does not bleed energy at full speed. */
    public static final int STEPS = 6;
    /** How hard leaning into the swing pumps it, blocks per tick per tick. */
    public static final double PUMP = 0.012;

    private SwingPhysics() {
    }

    /** The result of one tick: where the hands are now and how fast they move. */
    public record State(Vec3 hand, Vec3 velocity) {
    }

    /**
     * One tick on the rope.
     *
     * @param hand     where the hands are now
     * @param velocity their velocity (blocks per tick)
     * @param pivot    where the rope hangs from
     * @param length   the rope's length
     * @param push     an extra push this tick (the player leaning into the swing), blocks per tick
     */
    public static State step(Vec3 hand, Vec3 velocity, Vec3 pivot, double length, Vec3 push) {
        double vx = velocity.x + push.x;
        double vy = velocity.y + push.y;
        double vz = velocity.z + push.z;
        double px = hand.x;
        double py = hand.y;
        double pz = hand.z;
        double dt = 1.0 / STEPS;
        for (int i = 0; i < STEPS; i++) {
            vy -= GRAVITY * dt;
            px += vx * dt;
            py += vy * dt;
            pz += vz * dt;
            double dx = px - pivot.x;
            double dy = py - pivot.y;
            double dz = pz - pivot.z;
            double r = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (r > length && r > 1.0E-6) {
                double nx = dx / r;
                double ny = dy / r;
                double nz = dz / r;
                px = pivot.x + nx * length;
                py = pivot.y + ny * length;
                pz = pivot.z + nz * length;
                double out = vx * nx + vy * ny + vz * nz;
                if (out > 0.0) {
                    vx -= nx * out;
                    vy -= ny * out;
                    vz -= nz * out;
                }
            }
        }
        vx *= DRAG;
        vy *= DRAG;
        vz *= DRAG;
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed > MAX_SPEED) {
            double k = MAX_SPEED / speed;
            vx *= k;
            vy *= k;
            vz *= k;
        }
        return new State(new Vec3(px, py, pz), new Vec3(vx, vy, vz));
    }

    /**
     * The push of a player leaning into the swing: along the swing when they face the way they are going (that pumps
     * it higher), against it when they lean back (a brake), and from rest along where they look, so a hanging player
     * can start swinging. Only the part across the rope counts - you cannot push along the rope itself.
     *
     * @param forward  +1 leaning forward, -1 leaning back, 0 neither
     * @param strafe   +1 right, -1 left, 0 neither
     * @param look     the horizontal direction the player faces (unit length)
     * @param rope     from the pivot to the hands
     */
    public static Vec3 push(Vec3 velocity, int forward, int strafe, Vec3 look, Vec3 rope) {
        Vec3 axis = rope.lengthSqr() > 1.0E-8 ? rope.normalize() : new Vec3(0.0, -1.0, 0.0);
        Vec3 dir = Vec3.ZERO;
        Vec3 across = tangent(velocity, axis);
        double speed = across.length();
        if (forward > 0) {
            dir = speed > 0.03 && across.dot(look) > 0.0 ? across.scale(1.0 / speed) : tangent(look, axis).normalize();
        } else if (forward < 0 && speed > 0.01) {
            dir = across.scale(-1.5 / speed);
        }
        if (strafe != 0) {
            Vec3 side = new Vec3(-look.z, 0.0, look.x).scale(strafe * 0.6);
            dir = dir.add(tangent(side, axis));
        }
        return dir.scale(PUMP);
    }

    /** The part of v square to the rope. */
    public static Vec3 tangent(Vec3 v, Vec3 axis) {
        return v.subtract(axis.scale(v.dot(axis)));
    }
}
