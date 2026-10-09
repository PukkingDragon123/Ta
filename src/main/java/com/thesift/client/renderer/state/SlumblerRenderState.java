package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SlumblerRenderState extends SiftRenderState {
    public boolean sleeping;
    /** S2: how far asleep it is (0 awake .. 1 asleep), smoothed so the pose blends instead of snapping. */
    public float sleep;
    /** S2: how far it is swimming (afloat in a fluid, off the bottom), smoothed the same way. */
    public float swim;
    public final AnimationState yawn = new AnimationState();
    public final AnimationState bite = new AnimationState();
    public final AnimationState gulp = new AnimationState();
    public final AnimationState nuzzle = new AnimationState();
    public final AnimationState hum = new AnimationState();
    /** CR2: the wet shake after climbing out of the Chrome. */
    public final AnimationState shake = new AnimationState();
    /** CR2: spitting a gob of Chrome. */
    public final AnimationState spit = new AnimationState();
    /** CR2: laying its eggs. */
    public final AnimationState lay = new AnimationState();
    /** Wading in Chrome (it floats lower, swims and naps half-submerged). */
    public boolean inChrome;
    /** Per-creature offset, so a group of Slumblers never breathe or flutter in step. */
    public int seed;
}
