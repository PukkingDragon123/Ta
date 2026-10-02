package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class JailerRenderState extends SiftRenderState {
    /** {@link com.thesift.entity.cave.Jailer#IDLE}, EMERGING, HUNTING, SLAMMING, CARRYING or STUNNED. */
    public int mode;
    /** Bars still standing in the cell (0..12). */
    public int cage = 12;
    /** Someone is inside: the cell has a floor. */
    public boolean carrying;
    /** 0 upright .. 1 hunched low and creeping (it is hunting). */
    public float stalk;
    public float seed;
    public final AnimationState emerge = new AnimationState();
    public final AnimationState slam = new AnimationState();
    public final AnimationState trap = new AnimationState();
    public final AnimationState squeeze = new AnimationState();
    public final AnimationState rattle = new AnimationState();
    public final AnimationState cageBreak = new AnimationState();
    public final AnimationState listen = new AnimationState();
}
