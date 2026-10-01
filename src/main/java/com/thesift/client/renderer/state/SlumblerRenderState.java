package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SlumblerRenderState extends SiftRenderState {
    public boolean sleeping;
    public final AnimationState yawn = new AnimationState();
    public final AnimationState bite = new AnimationState();
}
