package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class EnchoerRenderState extends LivingEntityRenderState {
    /** 0 arms at rest, 1 fully into the trading or singing pose. */
    public float wingSpread;
    public boolean singing;
    public boolean blink;
    /** Per-entity offset so a crowd of Enchoers does not sigh in unison. */
    public float seed;
}
