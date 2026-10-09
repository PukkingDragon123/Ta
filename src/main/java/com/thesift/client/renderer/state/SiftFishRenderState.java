package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** Shared by the music fish. */
public class SiftFishRenderState extends SiftRenderState {
    /** Swim effort 0..1: how hard the tail beats. */
    public float effort;
    public boolean inLiquid;
    public float seed;
    /** CR3: which colour variant the fish is painted in. */
    public int variant;
    /** CR3: the Sculk Fish's snapping jaws, and whether its school is on the hunt. */
    public final AnimationState snap = new AnimationState();
    public boolean aggressive;
    public final AnimationState bite = new AnimationState();
    // the Gobbler: lunge (wind-up then strike), gulp, spit, the Lullaby's lull
    public final AnimationState lunge = new AnimationState();
    public final AnimationState gulp = new AnimationState();
    public final AnimationState spit = new AnimationState();
    public boolean calm;
    public boolean hunting;
    /** C2: emissive brightness 0..1 set by the renderer (the Gobbler's heartbeat); below 0 the soft default pulse. */
    public float glowPulse = -1.0F;
}
