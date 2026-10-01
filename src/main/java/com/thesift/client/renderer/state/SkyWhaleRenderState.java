package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SkyWhaleRenderState extends SiftRenderState {
    public float seed;
    public boolean answering;
    public final AnimationState sing = new AnimationState();
    public final AnimationState spit = new AnimationState();
}
