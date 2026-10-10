package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** P4-DESERT: the Grub - its gem colour and how much of its rocky shell is left. */
public class GrubRenderState extends SiftRenderState {
    /** 0 amethyst, 1 emerald, 2 diamond, 3 prism. */
    public int gem;
    /** 0 whole shell ... 4 free (see {@link com.thesift.entity.dunes.Grub#crackStage}). */
    public int crackStage;
    public boolean cracked;
    public float seed;
    public final AnimationState creak = new AnimationState();
    public final AnimationState burst = new AnimationState();
    public final AnimationState drink = new AnimationState();
}
