package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SlumblerRenderState extends SiftRenderState {
    public boolean sleeping;
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
    /** Per-creature offset, so a group of Slumblers never shimmer or flutter in step. */
    public int seed;
}
