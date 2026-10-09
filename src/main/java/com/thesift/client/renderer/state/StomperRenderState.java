package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class StomperRenderState extends SiftRenderState {
    /** Stored Chrome, 0..1. */
    public float chrome;
    public boolean dancing;
    /** Ticks of dance left. */
    public float danceTicks;
    public boolean sitting;
    public float seed;
    public final AnimationState drink = new AnimationState();
    public final AnimationState spray = new AnimationState();
    /** The rear-up-and-slam stomp (attacks, the rider's stomp, the end of a dance). */
    public final AnimationState stomp = new AnimationState();
    public final AnimationState puff = new AnimationState();
    public final AnimationState slap = new AnimationState();
    /** Idle: shaking the garden out like a wet dog, snuffling at the flowers with the trunk. */
    public final AnimationState shake = new AnimationState();
    public final AnimationState sniff = new AnimationState();
    /** A tame baby's drum beat on the ground with its trunk (restarted every beat). */
    public final AnimationState drum = new AnimationState();
    public boolean ridden;
    /** Stomper.NORMAL or Stomper.WHITE. */
    public int coat;
    /** S1 the trunk grab: StomperRig phase and ticks into it (with the partial tick). */
    public int grabPhase;
    public float grabTime;
    /** S1 a Stompling curled up to roll (0..1) and how far it has rolled (radians). */
    public float roll;
    public float rollAngle;
}
