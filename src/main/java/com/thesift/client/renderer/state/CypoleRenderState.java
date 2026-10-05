package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class CypoleRenderState extends SiftRenderState {
    /** {@link com.thesift.entity.swamp.Cypole#NONE}, WARN, CLASH or TONGUE. */
    public int action;
    public boolean angry;
    public boolean onGround = true;
    /** Floating in water (or Sculk Water): legs paddle, plates drift. */
    public boolean afloat;
    /** 0..1: squatting before a hop; 0..1 in mid-hop. */
    public float crouch;
    public float air;
    /** Spring offsets: squash and stretch of the body, the ringing of the plates. */
    public float squash;
    public float ring;
    /** 0..1 how shut the eye is (blinks). */
    public float blink;
    public float seed;
    public final AnimationState croak = new AnimationState();
    public final AnimationState clash = new AnimationState();
    public final AnimationState crash = new AnimationState();
    public final AnimationState tongue = new AnimationState();
    public final AnimationState warn = new AnimationState();
    /** The tongue: how far out it is (0..1) and where its tip goes, in the model's own space (units, y down). */
    public float tongueOut;
    public float tongueX;
    public float tongueY;
    public float tongueZ = -40.0F;
}
