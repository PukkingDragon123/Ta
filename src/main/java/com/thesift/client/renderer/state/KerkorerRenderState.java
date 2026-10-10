package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

/** P4-DESERT: the Kerkorer - its hunting state, how far it has melted into the sand, its rolling eyes and its bait. */
public class KerkorerRenderState extends SiftRenderState {
    /** {@link com.thesift.entity.dunes.Kerkorer#HIDE}, PROWL, SNAP or CHASE. */
    public int state;
    /** 0 its own colours - 1 the colours of the sand. */
    public float camo;
    /** The colour of the ground it lies on (the sand skin is tinted with it). */
    public int ground = 0xFFFFFF;
    /** Each turret eye's look (yaw, pitch; radians): left then right. */
    public float eyeLYaw;
    public float eyeLPitch;
    public float eyeRYaw;
    public float eyeRPitch;
    /** The tongue is lashing out (rather than the jaws). */
    public boolean tongue;
    public boolean hasLure;
    public final ItemStackRenderState lure = new ItemStackRenderState();
    public float seed;
    public final AnimationState snap = new AnimationState();
    public final AnimationState call = new AnimationState();
    public final AnimationState glint = new AnimationState();
}
