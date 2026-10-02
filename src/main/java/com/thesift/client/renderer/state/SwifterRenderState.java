package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SwifterRenderState extends SiftRenderState {
    /** {@link com.thesift.entity.Swifter#IDLE}, CROUCH, DASH, CLIMB, FLIP, DIVE, SLEEP or CRY. */
    public int mode;
    /** Pose blends, 0..1: the jet pose, the nap curled in its tails, the pounce crouch, a cub's crying. */
    public float jet;
    public float sleep;
    public float crouch;
    public float cry;
    /** Body pitch in radians: nose up climbing (negative), nose down diving (positive). */
    public float pitch;
    public boolean angry;
    public boolean carrying;
    public final AnimationState grab = new AnimationState();
    public final AnimationState slam = new AnimationState();
    public final AnimationState calm = new AnimationState();
    public final AnimationState snarl = new AnimationState();
    public float seed;
}
