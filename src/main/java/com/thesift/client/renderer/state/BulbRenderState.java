package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.world.entity.AnimationState;

public class BulbRenderState extends SiftRenderState {
    /** Bulb.BLUE or Bulb.WHITE. */
    public int variant;
    public float squash;
    public float earLeft;
    public float earRight;
    public float earPerk;
    public boolean dancing;
    public boolean airborne;
    /** Vertical speed (blocks per tick): it tips its nose up rising and down falling. */
    public float fallSpeed;
    /** Curled up asleep for the night. */
    public boolean sleepy;
    /** Ticks since it last bounced to a note (large when it has not). */
    public float beat = 100.0F;
    public final AnimationState sniff = new AnimationState();
    public final AnimationState groom = new AnimationState();
    public final AnimationState wiggle = new AnimationState();
    public final AnimationState nibble = new AnimationState();
    public final AnimationState place = new AnimationState();
    public final AnimationState pluck = new AnimationState();
    /** The flowers on its back, as block models (like a Mooshroom's mushrooms), and their pop-in (0..1). */
    public final BlockModelRenderState[] flowers = {new BlockModelRenderState(), new BlockModelRenderState(), new BlockModelRenderState()};
    public final float[] flowerPop = {1.0F, 1.0F, 1.0F};
    /** The flower it is nibbling from your hand. */
    public final BlockModelRenderState held = new BlockModelRenderState();
    /** How far the back flowers nod (a spring kicked by every hop and landing). */
    public float flowerSway;
}
