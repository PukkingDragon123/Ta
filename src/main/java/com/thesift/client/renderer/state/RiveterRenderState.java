package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class RiveterRenderState extends SiftRenderState {
    public boolean hanging;
    public float sway;
    public final AnimationState scream = new AnimationState();
    public final AnimationState chitter = new AnimationState();
    public final AnimationState snap = new AnimationState();
    /** Asleep on its roost by day, wrapped in its wings. */
    public boolean roosting;
}
