package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import com.thesift.client.renderer.state.SculkSpiderRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * A Sculk Spider (entity id strumling). It skitters on a fast alternating-tetrapod gait - four legs
 * planted while the other four swing, each lifted knee-first and set down tip-first - with little
 * nervous twitches running through the legs even when it stands still, palps tasting the air and
 * fangs working. On a wall it turns its face up the wall. Before it pounces it sinks down and rears
 * its front legs, fangs spread (the anticipation); in the air it throws its front legs forward and
 * trails the back ones, and the abdomen swings after it (the follow-through).
 */
public class StrumlingModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart abdomen;
    private final ModelPart leftFang;
    private final ModelPart rightFang;
    private final ModelPart leftPalp;
    private final ModelPart rightPalp;
    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart[] femur = new ModelPart[8];
    private final ModelPart[] tibia = new ModelPart[8];
    private final ModelPart[] tarsus = new ModelPart[8];

    public StrumlingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.abdomen = this.body.getChild("abdomen");
        this.leftFang = this.head.getChild("left_fang");
        this.rightFang = this.head.getChild("right_fang");
        this.leftPalp = this.head.getChild("left_palp");
        this.rightPalp = this.head.getChild("right_palp");
        this.tendrils[0] = this.abdomen.getChild("left_tendril");
        this.tendrils[1] = this.abdomen.getChild("right_tendril");
        for (int i = 0; i < 4; i++) {
            for (int side = 0; side < 2; side++) {
                String s = side == 0 ? "left" : "right";
                int k = i * 2 + side;
                this.femur[k] = this.body.getChild(s + "_leg_" + i);
                this.tibia[k] = this.femur[k].getChild(s + "_tibia_" + i);
                this.tarsus[k] = this.tibia[k].getChild(s + "_tarsus_" + i);
            }
        }
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        boolean climbing = s instanceof SculkSpiderRenderState ss && ss.climbing;
        boolean airborne = s instanceof SculkSpiderRenderState ss2 && ss2.airborne;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        if (climbing) {
            walk = Math.max(walk, 0.8F);
        }
        float pos = s.walkAnimationPos * 1.6F + (climbing ? age * 0.5F : 0.0F);

        // --- the gait: legs L0 R1 L2 R3 swing together while the other four are planted
        for (int i = 0; i < 4; i++) {
            for (int side = 0; side < 2; side++) {
                int k = i * 2 + side;
                float sx = side == 0 ? 1.0F : -1.0F;
                float ph = pos * 2.0F + ((i + side) % 2 == 0 ? 0.0F : Mth.PI);
                float swing = Mth.cos(ph) * 0.42F * walk;
                float lift = Math.max(0.0F, Mth.sin(ph)) * walk;
                this.femur[k].yRot += swing * sx;
                // knee first: the femur rises, the tibia folds, then the tip reaches down again
                this.femur[k].zRot -= (0.35F * lift) * sx;
                this.tibia[k].zRot -= (0.25F * lift) * sx;
                this.tarsus[k].zRot += (0.2F * lift) * sx;
                // nervous twitches, never quite in time with each other
                float tw = Mth.sin(age * 0.9F + k * 2.3F) * Mth.sin(age * 0.23F + k) * 0.05F;
                this.femur[k].zRot += tw * sx;
                this.tarsus[k].zRot -= tw * 1.5F * sx;
            }
        }
        this.body.y -= Math.abs(Mth.sin(pos * 2.0F)) * 0.6F * walk;
        this.body.zRot = Mth.sin(pos * 2.0F) * 0.04F * walk;
        this.head.yRot = s.yRot * Anim.DEG * 0.5F;
        this.head.xRot = s.xRot * Anim.DEG * 0.4F;
        // palps tasting the air, fangs working, the abdomen breathing a little out of time with it all
        this.leftPalp.xRot += Mth.sin(age * 0.31F) * 0.2F;
        this.rightPalp.xRot += Mth.sin(age * 0.29F + 1.7F) * 0.2F;
        this.leftPalp.yRot += Mth.sin(age * 0.17F) * 0.12F;
        this.rightPalp.yRot -= Mth.sin(age * 0.19F + 0.6F) * 0.12F;
        float chew = Math.max(0.0F, Mth.sin(age * 0.45F)) * 0.12F;
        this.leftFang.zRot = -chew;
        this.rightFang.zRot = chew;
        this.abdomen.xRot += Mth.sin(age * 0.12F) * 0.05F - Mth.sin(pos * 2.0F + 0.8F) * 0.06F * walk;
        this.abdomen.yRot = Mth.sin(pos * 2.0F + 1.2F) * 0.05F * walk;
        for (int i = 0; i < 2; i++) {
            float sgn = i == 0 ? 1.0F : -1.0F;
            this.tendrils[i].zRot += sgn * (Mth.sin(age * (0.13F + i * 0.03F) + i) * 0.2F + (s.windingUp ? Mth.sin(age * 1.9F) * 0.15F : 0.0F));
            this.tendrils[i].xRot += Mth.sin(age * 0.09F + i * 2.0F) * 0.15F;
        }

        // --- on a wall: face up it, legs spread flat against it
        if (climbing) {
            this.body.xRot -= Mth.HALF_PI * 0.9F;
            this.body.z += 3.0F;
            this.body.y -= 2.0F;
            for (int k = 0; k < 8; k++) {
                float sx = k % 2 == 0 ? 1.0F : -1.0F;
                this.femur[k].zRot += 0.35F * sx;
            }
        }

        // --- the pounce: sink and rear (anticipation), then fly with legs thrown forward
        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 0.45F) {
            float crouch = Anim.envelope(t, 0.0F, 0.3F, 0.1F, 0.05F);
            this.body.y += 2.5F * crouch;
            this.body.xRot -= 0.2F * crouch;
            for (int k = 0; k < 8; k++) {
                float sx = k % 2 == 0 ? 1.0F : -1.0F;
                if (k < 2) {
                    // the front pair rear up high, claws raised
                    this.femur[k].zRot -= 0.9F * crouch * sx;
                    this.femur[k].yRot += 0.45F * crouch * sx;
                    this.tibia[k].zRot -= 0.6F * crouch * sx;
                } else {
                    this.femur[k].zRot -= 0.45F * crouch * sx;
                    this.tibia[k].zRot += 0.25F * crouch * sx;
                }
            }
            this.leftFang.zRot -= 0.45F * crouch;
            this.rightFang.zRot += 0.45F * crouch;
            this.abdomen.xRot -= 0.35F * crouch;
        }
        if (airborne && !climbing) {
            float fly = t >= 0.0F ? Anim.envelope(t, 0.35F, 0.12F, 1.0F, 0.3F) : 0.6F;
            for (int k = 0; k < 8; k++) {
                float sx = k % 2 == 0 ? 1.0F : -1.0F;
                int i = k / 2;
                float fwd = i < 2 ? 0.55F : -0.5F;
                this.femur[k].yRot += fwd * fly * sx;
                this.femur[k].zRot += 0.25F * fly * sx;
                this.tibia[k].zRot -= 0.5F * fly * sx;
            }
            this.leftFang.zRot -= 0.5F * fly;
            this.rightFang.zRot += 0.5F * fly;
            this.abdomen.xRot += 0.3F * fly;
        }
        if (s.hurtTicks >= 0.0F) {
            float h = 1.0F - Math.min(1.0F, s.hurtTicks / 8.0F);
            this.body.y += 0.8F * h;
            this.abdomen.xRot -= 0.2F * h;
            for (int k = 0; k < 8; k++) {
                float sx = k % 2 == 0 ? 1.0F : -1.0F;
                this.femur[k].zRot -= 0.3F * h * sx;
            }
        }
    }
}
