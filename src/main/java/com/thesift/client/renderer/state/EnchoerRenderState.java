package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** M3: the Echoer's pose amounts (0..1, smoothed by the entity), its voice, its bow and its nod. */
public class EnchoerRenderState extends SiftRenderState {
    /** 0 standing on the ground, 1 skipping or hovering in the air. */
    public float air;
    public float dance;
    public float listen;
    public float sleep;
    /** Holding a gift up between its antlers. */
    public float present;
    /** The note it is singing (a spring, about 0..1): its mouth opens, its antler tips flare. */
    public float voice;
    public final AnimationState bow = new AnimationState();
    public final AnimationState nod = new AnimationState();
    /** Per-entity offset so two Echoers never move in unison. */
    public float seed;
}
