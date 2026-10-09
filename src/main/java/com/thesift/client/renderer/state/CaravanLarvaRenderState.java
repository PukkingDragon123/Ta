package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class CaravanLarvaRenderState extends SiftRenderState {
    public int variant;
    public int seed;
    public final AnimationState bite = new AnimationState();
    public final AnimationState emerge = new AnimationState();
}
