package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** CR3 Fish &amp; Coral Organs (WATER remake): what the Sculk Coral Organ's model needs to breathe, stare, gape and bite. */
public class CoralOrganRenderState extends SiftRenderState {
    public final AnimationState chord = new AnimationState();
    public final AnimationState charge = new AnimationState();
    public final AnimationState fire = new AnimationState();
    public final AnimationState clamp = new AnimationState();
    public final AnimationState gape = new AnimationState();
    public final AnimationState bite = new AnimationState();
    /** Where its eyes look, relative to the body (radians; yaw to the organ's right, pitch up). */
    public float aimYaw;
    public float aimPitch;
    /** Hunting: its eyes are fixed on prey. */
    public boolean aiming;
    /** Something to look at (its prey, or the nearest player swimming by). */
    public boolean watching;
    public float seed;
}
