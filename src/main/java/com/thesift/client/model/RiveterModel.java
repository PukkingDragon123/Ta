package com.thesift.client.model;

import com.thesift.client.renderer.state.RiveterRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Riveter, the sculk bat: a tall, starved, hunched thing whose arms are folded wings ending in four
 * long teal claws. It is built standing upright, pivoted at its feet, and flipped to hang head-down
 * from the ceiling by those feet.
 *
 * <ul>
 *   <li>idle (hanging): a slow pendulum sway, the dangling arms lag behind it, each claw twitches on
 *   its own, the head turns to follow you</li>
 *   <li>move (flying): wings flap wide with fanned claws, legs trail</li>
 *   <li>attack (scream): the arms fling open, the claws fan out like fingers, the jaw gapes and the
 *   whole body trembles</li>
 *   <li>hurt: flinches, wings jolt open, head snaps back</li>
 *   <li>death: shrivels up, wings wrapping tight and claws curling, dissolving into sculk souls</li>
 * </ul>
 */
public class RiveterModel extends EntityModel<RiveterRenderState> {
    private static final float PI = (float) Math.PI;
    /** Model y of the top of the hitbox: the feet grip the ceiling there while hanging. */
    private static final float CEILING_Y = 24.0F - 1.9F * 16.0F;

    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[][] claws = new ModelPart[2][4];

    public RiveterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.torso = this.body.getChild("torso");
        this.head = this.torso.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHorn = this.head.getChild("right_horn");
        String[] sides = {"left", "right"};
        for (int s = 0; s < 2; s++) {
            this.legs[s] = this.body.getChild(sides[s] + "_leg");
            this.shins[s] = this.legs[s].getChild(sides[s] + "_shin");
            this.wings[s] = this.torso.getChild(sides[s] + "_wing");
            for (int i = 0; i < 4; i++) {
                this.claws[s][i] = this.wings[s].getChild(sides[s] + "_claw_" + i);
            }
        }
    }

    @Override
    public void setupAnim(RiveterRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float hurt = s.hasRedOverlay ? 1.0F : 0.0F;
        float die = Anim.smooth(s.dying / 18.0F);
        boolean hang = s.hanging;
        // sgn below is +1 for the left side. Flipping a wing to dangle (xRot = PI) mirrors which zRot
        // swings it away from the body, so hanging flares with +sgn and flight with -sgn.
        float breathe = Mth.sin(age * 0.07F);

        // --- scream (1.7s): wings fling open, claws fan, the jaw gapes, everything trembles
        float scream = 0.0F;
        float fan = 0.0F;
        float t = Anim.seconds(s.scream, age);
        if (t >= 0 && t < 1.8F) {
            float open = Anim.envelope(t, 0.0F, 0.18F, 1.05F, 0.45F);
            float snap = t < 0.3F ? Anim.backOut(t / 0.25F) : 1.0F;
            scream = open * snap;
            fan = Anim.envelope(t, 0.06F, 0.2F, 0.95F, 0.45F);
        }
        float tremble = Mth.sin(age * 2.9F) * 0.06F * scream;

        if (hang) {
            // hanging head-down by the feet; the pendulum sway pivots on them
            this.body.zRot = PI + s.sway + tremble * 0.5F;
            this.body.y = CEILING_Y;
            this.body.xRot = Mth.sin(age * 0.023F) * 0.05F;
            this.torso.xRot = 0.08F;
            this.head.xRot = -0.45F - s.xRot * Anim.DEG * 0.4F;
            this.head.yRot = -s.yRot * Anim.DEG * 0.7F;
            this.head.xRot += -s.sway * 0.6F;
            for (int side = 0; side < 2; side++) {
                float sgn = side == 0 ? 1.0F : -1.0F;
                // arms dangle below the head and lag behind the swing
                this.wings[side].xRot = PI - 0.12F + breathe * 0.03F;
                this.wings[side].zRot = sgn * 0.06F - s.sway * 0.7F + sgn * (scream * 1.25F + hurt * 0.35F);
                this.legs[side].xRot = 0.12F;
                this.shins[side].xRot = -0.1F;
            }
        } else {
            // flying: pitched forward, wings beating wide, legs trailing
            float flap = Mth.sin(age * 1.3F);
            this.body.xRot = 0.35F;
            this.body.y -= Mth.cos(age * 1.3F) * 0.6F;
            this.head.xRot = s.xRot * Anim.DEG * 0.5F - 0.2F;
            this.head.yRot = s.yRot * Anim.DEG * 0.7F;
            for (int side = 0; side < 2; side++) {
                float sgn = side == 0 ? 1.0F : -1.0F;
                this.wings[side].zRot = -sgn * (1.15F + flap * 0.6F + scream * 0.25F + hurt * 0.3F);
                this.wings[side].xRot = -0.2F + flap * 0.15F;
                this.legs[side].xRot = 0.55F + Mth.sin(age * 0.2F + side) * 0.08F;
                this.shins[side].xRot = 0.5F;
            }
            fan = Math.max(fan, 0.4F + flap * 0.3F);
        }

        // --- claws: each finger twitches on its own phase; scream and flight fan them out
        for (int side = 0; side < 2; side++) {
            float sgn = side == 0 ? 1.0F : -1.0F;
            float outSign = -sgn; // spreads a claw away from the body in its wing's own frame
            for (int i = 0; i < 4; i++) {
                ModelPart c = this.claws[side][i];
                c.zRot += Mth.sin(age * 0.09F + i * 1.3F + side * 2.1F) * 0.04F + outSign * (i - 1.5F) * 0.28F * (fan + hurt * 0.6F);
                c.xRot += Mth.sin(age * 0.05F + i * 0.7F) * 0.03F - fan * 0.25F + die * 0.9F;
            }
        }

        // --- head: jaw gapes and the horns flatten while screaming, snaps back when hurt
        this.jaw.xRot += scream * 0.95F + tremble;
        this.head.xRot += (hang ? 0.3F : -0.3F) * scream + (hang ? 0.35F : -0.35F) * hurt;
        this.leftHorn.xRot += -scream * 0.4F + Mth.sin(age * 0.11F) * 0.05F;
        this.rightHorn.xRot += -scream * 0.4F + Mth.sin(age * 0.11F + 0.4F) * 0.05F;
        this.torso.xScale = 1.0F + scream * 0.05F + breathe * 0.015F;
        this.torso.zScale = 1.0F + breathe * 0.02F;

        // --- death: shrivel, wrap the wings tight, droop the head
        if (die > 0.0F) {
            for (int side = 0; side < 2; side++) {
                float sgn = side == 0 ? 1.0F : -1.0F;
                this.wings[side].yRot += -sgn * die * 0.8F;
                this.wings[side].zRot *= 1.0F - die;
            }
            this.head.xRot += (hang ? -0.6F : 0.6F) * die;
            this.jaw.xRot += die * 0.5F;
            this.body.xScale = 1.0F - die * 0.25F;
            this.body.zScale = 1.0F - die * 0.25F;
            this.body.yScale = 1.0F - die * 0.35F;
        }
    }
}
