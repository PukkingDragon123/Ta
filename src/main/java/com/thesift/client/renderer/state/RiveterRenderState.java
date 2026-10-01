package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class RiveterRenderState extends LivingEntityRenderState {
    public boolean hanging;
    public float sway;
    /** Death progress in ticks; drives the shrivel animation instead of the usual tip-over. */
    public float dying;
    public final AnimationState scream = new AnimationState();
}
