package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SkyWhaleRenderState extends SiftRenderState {
    public float seed;
    public boolean answering;
    /** How hard it is turning (degrees per tick, smoothed): it banks into turns. */
    public float bank;
    /** How fast it is climbing (+) or diving (-), blocks per tick. */
    public float climb;
    public final AnimationState sing = new AnimationState();
    public final AnimationState spit = new AnimationState();
}
