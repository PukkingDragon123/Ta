package com.thesift.client.renderer.state;

/** Shared by the Thumper, the Whistler and the Strummer: which attack, and how far into it. */
public class MiniBossRenderState extends SiftRenderState {
    public int bossState;
    /** Ticks since the attack state changed. */
    public float stateTime;
    public boolean grounded;
    public boolean enraged;
}
