package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class SoulGolemRenderState extends SiftRenderState {
    /** Soul energy, 0..1 (the eyes, antenna lamp and vent glow with it). */
    public float energy = 1.0F;
    /** 0 upright, 1 slumped (run down). */
    public float slump;
    /** {@link com.thesift.entity.SoulGolem#NORMAL}, DIGGING, PEEKING, BURROWED or EMERGING. */
    public int mode;
    /** How far into digging and peeking it is, and how far under the ground (smoothed by the entity, so the poses blend). */
    public float dig;
    public float peek;
    public float burrow;
    public final AnimationState happy = new AnimationState();
    /** CAVE v4: popping up out of its burrow. */
    public final AnimationState emerge = new AnimationState();
    /** CAVE v4: its find, held up in its forepaws. */
    public boolean carrying;
    public final ItemStackRenderState carried = new ItemStackRenderState();
    public float seed;
}
