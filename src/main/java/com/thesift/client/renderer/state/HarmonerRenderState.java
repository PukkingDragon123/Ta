package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class HarmonerRenderState extends SiftRenderState {
    public int variant;
    /** 0 perched, 1 in flight. */
    public float flap;
    public boolean guiding;
    /** Per-entity offset so a flock does not move in lockstep. */
    public float seed;
    public final AnimationState sing = new AnimationState();
}
