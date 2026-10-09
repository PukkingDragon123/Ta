package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** E1: what the Sift Sniffer's model needs each frame. */
public class SiftSnifferRenderState extends SiftRenderState {
    public final AnimationState graze = new AnimationState();
    public final AnimationState sniff = new AnimationState();
    public final AnimationState dig = new AnimationState();
    public final AnimationState trumpet = new AnimationState();
    public final AnimationState windup = new AnimationState();
    public final AnimationState lay = new AnimationState();
    /** Charging at its target, head down. */
    public boolean charging;
    /** Flowers left in its back garden. */
    public int garden;
    /** A per-animal phase so a herd never sways in step. */
    public float seed;
}
