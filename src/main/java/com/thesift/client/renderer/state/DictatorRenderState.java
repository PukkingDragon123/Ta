package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

public class DictatorRenderState extends LivingEntityRenderState {
    public int phase;
    /** Death progress in ticks; drives the collapse instead of the usual tip-over. */
    public float dying;
    public final AnimationState blink = new AnimationState();
    public final AnimationState summon = new AnimationState();
    public final AnimationState crescendo = new AnimationState();
    public final AnimationState slash = new AnimationState();
    public final AnimationState roar = new AnimationState();
}
