package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class CaravanRenderState extends SiftRenderState {
    public int variant;
    public boolean soldier;
    public boolean calm;
    public boolean carrying;
    /** How far its gem crust has grown, 0-1. */
    public float growth = 1.0F;
    /** How far it has turned side-on to scuttle, 0 (standing, facing you) to 1 (running sideways). */
    public float scuttle;
    /** Which way it scuttles: 1 or -1 (each crab has its own side). */
    public float hand = 1.0F;
    public int seed;
    /** The ore chunk held in the claws. */
    public final ItemStackRenderState carried = new ItemStackRenderState();
    public final AnimationState tap = new AnimationState();
    public final AnimationState mine = new AnimationState();
    public final AnimationState bite = new AnimationState();
    /** CR2: laying its ore before the Queen. */
    public final AnimationState offer = new AnimationState();
    /** CR2: rearing up and clacking its claws at an intruder. */
    public final AnimationState warn = new AnimationState();
}
