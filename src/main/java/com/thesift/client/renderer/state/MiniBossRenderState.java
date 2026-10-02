package com.thesift.client.renderer.state;

/** Shared by the Thumper, the Whistler and the Strummer: which attack, and how far into it. */
public class MiniBossRenderState extends SiftRenderState {
    public int bossState;
    /** Ticks since the attack state changed. */
    public float stateTime;
    public boolean grounded;
    public boolean enraged;
    /** The Thumper's growth into its titan form, 0 to 1. */
    public float titan;
    /** Is anyone standing on the titan's back? */
    public boolean ridden;
}
