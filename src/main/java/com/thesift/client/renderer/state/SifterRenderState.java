package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** CR1: the Sifter, a living bell (see entity/BellClapper for the rock and swing angles). */
public class SifterRenderState extends SiftRenderState {
    public boolean burrowed;
    public float squash;
    /** The bell rocking on its lip (radians about the model's X and Z axes). */
    public float rockX;
    public float rockZ;
    /** The clapper's swing from the plumb line (radians, same axes). */
    public float swingX;
    public float swingZ;
    /** The last strike's glow (0..1): the runes flare and the bell shivers. */
    public float ring;
    public final AnimationState bonk = new AnimationState();
    public final AnimationState emerge = new AnimationState();
    public final AnimationState burrow = new AnimationState();
    /** Cross with someone: it charges with its bell tipped forward. */
    public boolean chasing;
}
