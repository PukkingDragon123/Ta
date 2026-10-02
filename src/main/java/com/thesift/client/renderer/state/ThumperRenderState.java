package com.thesift.client.renderer.state;

/** The Thumper Titan's extra pose data (agent B2): which vent the last cannonball struck, and when. */
public class ThumperRenderState extends MiniBossRenderState {
    /** 0 the crown vent, 1 the vent on its right flank, 2 on its left; -1 none yet. */
    public int staggerVent = -1;
    /** Ticks since that hit. */
    public float staggerTime = 99.0F;
}
