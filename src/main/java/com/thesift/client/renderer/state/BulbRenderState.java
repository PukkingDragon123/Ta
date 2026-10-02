package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class BulbRenderState extends SiftRenderState {
    public int variant;
    public float squash;
    public float earLeft;
    public float earRight;
    public float earPerk;
    public boolean dancing;
    public boolean airborne;
    /** Curled up asleep for the night. */
    public boolean sleepy;
    /** Ticks since it last bounced to a note (large when it has not). */
    public float beat = 100.0F;
    public final AnimationState sniff = new AnimationState();
    public final AnimationState groom = new AnimationState();
    public final AnimationState wiggle = new AnimationState();
}
