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
}
