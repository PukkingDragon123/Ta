package com.thesift.client.renderer.state;

/** M3: a Nib's state and how far it has settled (rest) or is swirling into treasure (both smoothed, 0..1). */
public class NibRenderState extends SiftRenderState {
    /** {@link com.thesift.entity.Nib#FLYING}, RESTING or SWIRLING. */
    public int state;
    public float phase;
    public float rest;
    public float swirl;
}
