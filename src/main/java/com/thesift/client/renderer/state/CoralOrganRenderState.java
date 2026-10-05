package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** CR3 Fish &amp; Coral Organs: what the Sculk Coral Organ's model needs to play, aim and fire. */
public class CoralOrganRenderState extends SiftRenderState {
    public final AnimationState chord = new AnimationState();
    public final AnimationState charge = new AnimationState();
    public final AnimationState fire = new AnimationState();
    public final AnimationState clamp = new AnimationState();
    /** Where the harpoon horn points, relative to the body (radians; yaw to the organ's right, pitch up). */
    public float aimYaw;
    public float aimPitch;
    public boolean aiming;
    public float seed;
}
