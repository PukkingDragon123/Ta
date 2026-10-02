package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** Shared by the music fish. */
public class SiftFishRenderState extends SiftRenderState {
    /** Swim effort 0..1: how hard the tail beats. */
    public float effort;
    public boolean inLiquid;
    /** Tubafish swell, 0 deflated .. 1 puffed. */
    public float puff;
    public float seed;
    public final AnimationState bite = new AnimationState();
    // the Gobbler: lunge (wind-up then strike), gulp, spit, the Tide Song's lull
    public final AnimationState lunge = new AnimationState();
    public final AnimationState gulp = new AnimationState();
    public final AnimationState spit = new AnimationState();
    public boolean calm;
    public boolean hunting;
}
