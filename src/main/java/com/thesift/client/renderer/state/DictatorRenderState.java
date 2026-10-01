package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class DictatorRenderState extends SiftRenderState {
    public int phase;
    public final AnimationState blink = new AnimationState();
    public final AnimationState summon = new AnimationState();
    public final AnimationState crescendo = new AnimationState();
    public final AnimationState slash = new AnimationState();
    public final AnimationState roar = new AnimationState();
}
