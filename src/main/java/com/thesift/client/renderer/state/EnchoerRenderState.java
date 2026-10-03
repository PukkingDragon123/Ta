package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** The Echoer's pose weights (0..1, already smoothed by the entity), its bow, and (CR1) its wings, drill and cones. */
public class EnchoerRenderState extends SiftRenderState {
    public float inspect;
    public float wait;
    public float dance;
    public float sad;
    public float sleep;
    public float sing;
    public final AnimationState bow = new AnimationState();
    /** Per-entity offset so a flock of Echoers does not move in unison. */
    public float seed;
    /** The wingbeat (radians, a full beat per turn) and how hard it beats (0 folded .. 1 full). */
    public float flap;
    public float beat;
    /** The drill's spin (radians). */
    public float drill;
    /** The speaker cones' throw (a spring, about 0..1). */
    public float pump;
    /** How fast it flies (blocks per tick, horizontal), for the lean into its flight. */
    public float speed;
}
