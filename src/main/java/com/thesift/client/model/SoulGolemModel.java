package com.thesift.client.model;

import com.thesift.client.renderer.state.SoulGolemRenderState;
import com.thesift.entity.SoulGolem;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Soul Golem: walks with a rocking waddle-hop (the whole body tips from foot to foot and bounces
 * on each step, arms swinging out for balance), its lamp stalk lagging behind every movement.
 * Digging, it leans in and scoops with both arms in turn; peeking, it crouches low and leans
 * forward, stalk bent over to light the block. Run down, it slumps forward, arms hanging, lamp
 * drooping. When happy (a find, or the Golem Hymn) it does a little spinning hop with arms up.
 */
public class SoulGolemModel extends EntityModel<SoulGolemRenderState> {
    private final ModelPart body;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart stalk;
    private final ModelPart lamp;

    public SoulGolemModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftEye = this.body.getChild("left_eye");
        this.rightEye = this.body.getChild("right_eye");
        this.stalk = this.body.getChild("stalk");
        this.lamp = this.stalk.getChild("lamp");
    }

    @Override
    public void setupAnim(SoulGolemRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float up = 1.0F - s.slump;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F) * up;
        float pos = s.walkAnimationPos * 0.9F;
        float sw = Mth.sin(pos);

        // --- the waddle-hop: tip onto each foot, bounce, arms out for balance
        this.body.zRot += sw * 0.16F * walk;
        this.body.y -= Math.abs(Mth.cos(pos)) * 1.4F * walk;
        this.leftLeg.xRot += sw * 0.7F * walk;
        this.rightLeg.xRot -= sw * 0.7F * walk;
        this.leftLeg.y -= Math.max(0.0F, sw) * 0.8F * walk;
        this.rightLeg.y -= Math.max(0.0F, -sw) * 0.8F * walk;
        this.leftArm.zRot -= (0.25F + sw * 0.2F) * walk;
        this.rightArm.zRot += (0.25F - sw * 0.2F) * walk;
        this.leftArm.xRot -= sw * 0.3F * walk;
        this.rightArm.xRot += sw * 0.3F * walk;
        // the stalk lags behind the body, the lamp a beat behind that
        this.stalk.zRot -= Mth.sin(pos - 0.8F) * 0.25F * walk + Mth.sin(age * 0.08F + s.seed) * 0.06F * up;
        this.stalk.xRot += Mth.cos(age * 0.06F + s.seed) * 0.05F * up;
        this.lamp.zRot -= Mth.sin(pos - 1.6F) * 0.2F * walk;

        // --- looking around: the whole body turns a little, the eye domes a little more
        float yaw = s.yRot * Anim.DEG;
        this.body.yRot += yaw * 0.35F * up;
        this.leftEye.yRot += yaw * 0.25F * up;
        this.rightEye.yRot += yaw * 0.25F * up;
        this.body.xRot += s.xRot * Anim.DEG * 0.2F * up;

        // --- digging: lean in, both arms scooping in turn
        if (s.mode == SoulGolem.DIGGING) {
            float dig = age * 0.9F;
            this.body.xRot += 0.45F;
            this.body.y += 0.8F;
            this.leftArm.xRot += -1.2F + Mth.sin(dig) * 0.6F;
            this.rightArm.xRot += -1.2F + Mth.sin(dig + Mth.PI) * 0.6F;
            this.stalk.xRot += 0.5F + Mth.sin(dig * 2.0F) * 0.08F;
        } else if (s.mode == SoulGolem.PEEKING) {
            // peeking: crouched, leaning far over, stalk bent forward to shine on the block
            float wobble = Mth.sin(age * 0.15F) * 0.05F;
            this.body.xRot += 0.6F + wobble;
            this.body.y += 1.2F;
            this.stalk.xRot += 1.0F;
            this.lamp.xRot += 0.4F;
            this.leftArm.zRot -= 0.3F;
            this.rightArm.zRot += 0.3F;
            this.leftArm.xRot -= 0.6F;
            this.rightArm.xRot -= 0.6F;
        }

        // --- run down: slumped forward, arms hanging, the lamp drooping
        if (s.slump > 0.0F) {
            float k = s.slump;
            this.body.xRot += 0.5F * k;
            this.body.y += 1.2F * k;
            this.leftArm.xRot -= 0.35F * k;
            this.rightArm.xRot -= 0.35F * k;
            this.leftArm.zRot += 0.1F * k;
            this.rightArm.zRot -= 0.1F * k;
            this.stalk.xRot += 0.9F * k;
            this.lamp.xRot += 0.5F * k;
        }

        // --- happy: a crouch, a spinning hop with both arms flung up, a wobbly landing
        float h = Anim.seconds(s.happy, age);
        if (h >= 0.0F && h < 1.2F) {
            float crouch = Anim.envelope(h, 0.0F, 0.12F, 0.0F, 0.12F);
            float air = Anim.envelope(h, 0.12F, 0.12F, 0.25F, 0.3F);
            float land = Anim.envelope(h, 0.65F, 0.08F, 0.0F, 0.45F);
            this.body.y += 1.2F * crouch - 3.0F * air + 0.8F * land;
            this.body.yRot += Anim.smooth((h - 0.12F) / 0.55F) * Mth.TWO_PI;
            this.leftArm.zRot -= 2.2F * air;
            this.rightArm.zRot += 2.2F * air;
            this.body.zRot += Mth.sin(h * 30.0F) * 0.12F * land;
            this.stalk.xRot -= 0.6F * air;
            this.stalk.xRot += 0.4F * land;
        }
    }
}
