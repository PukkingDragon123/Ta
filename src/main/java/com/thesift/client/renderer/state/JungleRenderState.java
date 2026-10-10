package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/**
 * P4 Cave Jungle: what the Cave Jungle creatures' models read. One state serves all six (see
 * {@link com.thesift.client.CaveJungleClient}, which fills it per creature); the meaning of the
 * generic slots is written next to each model.
 */
public class JungleRenderState extends SiftRenderState {
    /** A per-creature phase offset, so a group never moves in step. */
    public float seed;
    /** Strength of the glow layer (0 - 1). */
    public float glow = 1.0F;
    public boolean flagA;
    public boolean flagB;
    public float valueA;
    public float valueB;
    public final AnimationState animA = new AnimationState();
    public final AnimationState animB = new AnimationState();
    public final AnimationState animC = new AnimationState();
    public final AnimationState animD = new AnimationState();
}
