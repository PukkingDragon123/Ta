package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** Shared by the Enforcer, Resonator and Howler. */
public class MinionRenderState extends SiftRenderState {
    public boolean windingUp;
    public final AnimationState attack = new AnimationState();
}
