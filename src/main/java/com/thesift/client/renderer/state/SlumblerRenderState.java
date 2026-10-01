package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class SlumblerRenderState extends LivingEntityRenderState {
    public boolean sleeping;
    public boolean blink;
    /** Death progress in ticks; drives the belly-up roll instead of the usual tip-over. */
    public float dying;
    public final AnimationState yawn = new AnimationState();
    public final AnimationState bite = new AnimationState();
}
