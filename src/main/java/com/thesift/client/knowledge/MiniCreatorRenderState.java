package com.thesift.client.knowledge;

import com.thesift.client.renderer.state.SiftRenderState;
import net.minecraft.world.entity.AnimationState;

/** F3: what the Mini Creator's model needs each frame. */
public class MiniCreatorRenderState extends SiftRenderState {
    public final AnimationState talk = new AnimationState();
    public final AnimationState wave = new AnimationState();
    public final AnimationState celebrate = new AnimationState();
    public final AnimationState appear = new AnimationState();
    public final AnimationState poof = new AnimationState();
    /** S1 land: summoned, he rises out of the dais. */
    public final AnimationState rise = new AnimationState();
    /** His own phase, so two guides never dabble in step. */
    public float seed;
}
