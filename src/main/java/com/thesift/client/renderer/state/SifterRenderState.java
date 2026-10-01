package com.thesift.client.renderer.state;

import net.minecraft.world.entity.AnimationState;

public class SifterRenderState extends SiftRenderState {
    public boolean burrowed;
    public float squash;
    public float antennaLeft;
    public float antennaRight;
    public final AnimationState chomp = new AnimationState();
    public final AnimationState emerge = new AnimationState();
}
