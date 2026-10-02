package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.world.entity.AnimationState;

public class SculklingRenderState extends SiftRenderState {
    /** Hands over its ears (music!). */
    public boolean scared;
    /** It is running off with something shiny, held in its right claw. */
    public boolean hasLoot;
    public final ItemStackRenderState loot = new ItemStackRenderState();
    /** Spring offsets of the ears (twitches and flops), radians. */
    public float leftEar;
    public float rightEar;
    public float seed;
    public final AnimationState snatch = new AnimationState();
    public final AnimationState cover = new AnimationState();
    public final AnimationState giggle = new AnimationState();
    public final AnimationState screech = new AnimationState();
}
