package com.thesift.client.renderer.state;


public class EnchoerRenderState extends SiftRenderState {
    /** 0 arms at rest, 1 fully into the trading or singing pose. */
    public float wingSpread;
    public boolean singing;
    public boolean blink;
    /** Per-entity offset so a crowd of Enchoers does not sigh in unison. */
    public float seed;
}
