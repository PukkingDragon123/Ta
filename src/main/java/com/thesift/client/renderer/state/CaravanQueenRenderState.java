package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class CaravanQueenRenderState extends SiftRenderState {
    public int variant;
    public boolean calm;
    public int seed;
    /** She rings her shell's gems for her part in a jam. */
    public final AnimationState tap = new AnimationState();
    public final AnimationState spit = new AnimationState();
    public final AnimationState swat = new AnimationState();
    public final AnimationState feed = new AnimationState();
    public final AnimationState warn = new AnimationState();
    public final AnimationState roar = new AnimationState();
    public final AnimationState settle = new AnimationState();
}
