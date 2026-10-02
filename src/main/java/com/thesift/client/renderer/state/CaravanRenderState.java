package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class CaravanRenderState extends SiftRenderState {
    public int variant;
    public boolean soldier;
    public boolean calm;
    public boolean carrying;
    /** The ore chunk held in the mandibles. */
    public final ItemStackRenderState carried = new ItemStackRenderState();
    public final AnimationState tap = new AnimationState();
    public final AnimationState mine = new AnimationState();
    public final AnimationState bite = new AnimationState();
    public final AnimationState build = new AnimationState();
}
