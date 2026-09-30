package com.thesift.entity;

/**
 * A tiny damped spring used for the squash-and-stretch and wobble animations of Sift creatures.
 * Lives on the client side of the entity and is ticked once per game tick; renderers interpolate
 * between {@link #prev} and {@link #value}.
 */
public final class Spring {
    public float value;
    public float prev;
    private float velocity;
    private final float stiffness;
    private final float damping;
    private float target;

    public Spring(float stiffness, float damping) {
        this.stiffness = stiffness;
        this.damping = damping;
    }

    public void setTarget(float target) {
        this.target = target;
    }

    public void kick(float impulse) {
        this.velocity += impulse;
    }

    public void tick() {
        this.prev = this.value;
        float force = (this.target - this.value) * this.stiffness;
        this.velocity = (this.velocity + force) * (1.0F - this.damping);
        this.value += this.velocity;
    }

    public float get(float partialTick) {
        return this.prev + (this.value - this.prev) * partialTick;
    }
}
