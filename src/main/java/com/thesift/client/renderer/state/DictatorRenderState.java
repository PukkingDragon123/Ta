package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class DictatorRenderState extends SiftRenderState {
    public int phase;
    public int action;
    public float actionTime;
    /** 0..1 through a transformation between movements, or -1. */
    public float transform = -1.0F;
    public final AnimationState blink = new AnimationState();
    public final AnimationState summon = new AnimationState();
    public final AnimationState crescendo = new AnimationState();
    public final AnimationState slash = new AnimationState();
    public final AnimationState roar = new AnimationState();
}
