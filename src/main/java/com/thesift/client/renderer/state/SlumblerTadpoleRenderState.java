package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SlumblerTadpoleRenderState extends SiftRenderState {
    /** Swim effort 0..1: how deep and quick the wing strokes are. */
    public float effort;
    public boolean inLiquid;
    public float seed;
    public final AnimationState bite = new AnimationState();
    /** Its note in a band: a clap of the wing-fins. */
    public final AnimationState crash = new AnimationState();
}
