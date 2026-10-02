package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class NibRenderState extends LivingEntityRenderState {
    /** {@link com.thesift.entity.Nib#FLYING}, RESTING or SWIRLING. */
    public int state;
    public float phase;
}
