package com.thesift.client.renderer.state;

import com.thesift.client.Expression;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** What every Sift mob renderer knows on top of vanilla: its face, and how squashed it is. */
public class SiftRenderState extends LivingEntityRenderState {
    public Expression expression = Expression.NEUTRAL;
    /** Ticks since the last hit while it still squishes the body (0..10), or -1. */
    public float hurtTicks = -1.0F;
    /** Death progress in ticks (0..20); the body bounces and pops instead of tipping over. */
    public float dying;
    /** E1: how far it has rotted outside the Sift (0 healthy - 1 fully rotten). */
    public float rot;
}
