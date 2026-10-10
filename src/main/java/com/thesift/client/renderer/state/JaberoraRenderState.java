package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

/** P4-DESERT: the Jaberora - hops, arias, its piercing note, naps and rides. */
public class JaberoraRenderState extends SiftRenderState {
    /** 0..1 crouched before a hop; 0..1 in the air. */
    public float crouch;
    public float air;
    /** 0..1 how shut its eyes are (blinks; naps shut them). */
    public float blink;
    public boolean singing;
    public boolean napping;
    public boolean sitting;
    /** On a Kerkorer's back. */
    public boolean riding;
    public float seed;
    /** Its colour (Jaberora#getVariant). */
    public int variant;
    public final AnimationState pulse = new AnimationState();
    public final AnimationState eat = new AnimationState();
    public final AnimationState sing = new AnimationState();
}
