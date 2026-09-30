package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class RiveterRenderState extends LivingEntityRenderState {
    public boolean hanging;
    public float sway;
    public final AnimationState scream = new AnimationState();
}
