package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SlumblerRenderState extends SiftRenderState {
    public boolean sleeping;
    public final AnimationState yawn = new AnimationState();
    public final AnimationState bite = new AnimationState();
    public final AnimationState gulp = new AnimationState();
    public final AnimationState nuzzle = new AnimationState();
    public final AnimationState hum = new AnimationState();
    /** Wading in Chrome (it floats lower and naps half-submerged). */
    public boolean inChrome;
}
