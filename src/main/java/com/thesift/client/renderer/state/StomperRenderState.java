package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class StomperRenderState extends SiftRenderState {
    /** Stored Chrome, 0..1: the spiracles glow with it. */
    public float chrome;
    public boolean dancing;
    /** Ticks of dance left. */
    public float danceTicks;
    public boolean sitting;
    public boolean drinking;
    public float seed;
    public final AnimationState drink = new AnimationState();
    public final AnimationState spray = new AnimationState();
    public final AnimationState stomp = new AnimationState();
    public final AnimationState puff = new AnimationState();
    public final AnimationState slap = new AnimationState();
}
