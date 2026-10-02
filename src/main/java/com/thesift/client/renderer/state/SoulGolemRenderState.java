package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SoulGolemRenderState extends SiftRenderState {
    /** Soul energy, 0..1 (the eyes, cracks and lamp glow with it). */
    public float energy = 1.0F;
    /** 0 upright, 1 slumped (run down). */
    public float slump;
    /** {@link com.thesift.entity.SoulGolem#NORMAL}, DIGGING or PEEKING. */
    public int mode;
    public final AnimationState happy = new AnimationState();
    public float seed;
}
