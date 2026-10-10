package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** P4-DESERT: the Reservoir - how full of Chrome it is (it swells, pulses and glows with it) and how open its flower is. */
public class ReservoirRenderState extends SiftRenderState {
    /** 0 empty - 1 full. */
    public float fill;
    /** 0 closed - 1 open. */
    public float bloom;
    public boolean monarch;
    public float seed;
    public final AnimationState burst = new AnimationState();
    public final AnimationState gulp = new AnimationState();
}
