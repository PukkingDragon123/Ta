package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** The Echoer's pose weights (0..1, already smoothed by the entity) and its bow. */
public class EnchoerRenderState extends SiftRenderState {
    public float inspect;
    public float wait;
    public float dance;
    public float sad;
    public float sleep;
    public float sing;
    public final AnimationState bow = new AnimationState();
    /** Per-entity offset so a herd of Echoers does not sway in unison. */
    public float seed;
}
