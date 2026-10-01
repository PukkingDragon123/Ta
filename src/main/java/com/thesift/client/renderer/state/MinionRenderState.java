package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Shared by the Enforcer, Resonator and Howler. */
public class MinionRenderState extends LivingEntityRenderState {
    public boolean windingUp;
    public final AnimationState attack = new AnimationState();
}
