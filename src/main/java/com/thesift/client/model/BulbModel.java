package com.thesift.client.model;

import com.thesift.client.renderer.state.BulbRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Bulb: everything hangs off a single "body" pivoted at the feet, so squash & stretch scales the
 * whole jelly from the ground up. Ears are two-segment springs that lag behind the body.
 */
public class BulbModel extends EntityModel<BulbRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart eyelids;
    private final ModelPart leftEar;
    private final ModelPart leftEarTip;
    private final ModelPart rightEar;
    private final ModelPart rightEarTip;
    private final ModelPart stalk;
    private final ModelPart lure;
    private final ModelPart tail;
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart leftPaw;
    private final ModelPart rightPaw;

    public BulbModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.eyelids = this.head.getChild("eyelids");
        this.leftEar = this.head.getChild("left_ear");
        this.leftEarTip = this.leftEar.getChild("left_ear_tip");
        this.rightEar = this.head.getChild("right_ear");
        this.rightEarTip = this.rightEar.getChild("right_ear_tip");
        this.stalk = this.head.getChild("stalk");
        this.lure = this.stalk.getChild("lure");
        this.tail = this.body.getChild("tail");
        this.leftFoot = this.body.getChild("left_foot");
        this.rightFoot = this.body.getChild("right_foot");
        this.leftPaw = this.body.getChild("left_paw");
        this.rightPaw = this.body.getChild("right_paw");
    }

    @Override
    public void setupAnim(BulbRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos;

        // --- squash & stretch (plus a slow jelly "breath")
        float sq = Mth.clamp(s.squash, -0.45F, 0.6F);
        float breathe = Mth.sin(age * 0.11F) * 0.03F;
        this.body.yScale = 1.0F + sq + breathe;
        float wide = 1.0F - sq * 0.55F - breathe * 0.5F;
        this.body.xScale = wide;
        this.body.zScale = wide;
        this.body.zRot = Mth.sin(pos * 0.6F) * 0.07F * walk;

        // --- head follows the look direction, softened
        this.head.yRot = s.yRot * Anim.DEG * 0.8F;
        this.head.xRot = s.xRot * Anim.DEG * 0.5F - sq * 0.25F;

        // --- ears: springy two-segment wobble, perk up near players
        float perk = s.earPerk;
        float idle = Mth.sin(age * 0.08F) * 0.05F;
        this.leftEar.xRot += s.earLeft * 1.2F - perk * 0.3F + idle;
        this.leftEar.zRot += s.earLeft * 0.25F - perk * 0.08F;
        this.leftEarTip.xRot += s.earLeft * 0.9F + Mth.sin(age * 0.08F - 0.8F) * 0.07F + (1.0F - perk) * 0.35F;
        this.rightEar.xRot += s.earRight * 1.2F - perk * 0.3F + Mth.sin(age * 0.08F + 1.7F) * 0.05F;
        this.rightEar.zRot += -s.earRight * 0.25F + perk * 0.08F;
        this.rightEarTip.xRot += s.earRight * 0.9F + Mth.sin(age * 0.08F + 0.9F) * 0.07F + (1.0F - perk) * 0.35F;

        // --- dancing: head bop and flailing ears
        if (s.dancing) {
            float beat = Mth.sin(age * 0.55F);
            this.head.zRot = beat * 0.28F;
            this.leftEar.zRot += beat * 0.45F;
            this.rightEar.zRot += beat * 0.45F;
            this.leftEarTip.xRot += Mth.cos(age * 0.55F) * 0.3F;
            this.rightEarTip.xRot -= Mth.cos(age * 0.55F) * 0.3F;
        }

        // --- feet: tucked in the air, pattering on the ground
        float air = s.airborne ? 1.0F : 0.0F;
        float step = Mth.cos(pos * 0.9F) * 0.6F * walk * (1.0F - air);
        this.leftFoot.xRot = air * 0.9F + step;
        this.rightFoot.xRot = air * 0.9F - step;
        this.leftPaw.xRot = -air * 0.7F - step;
        this.rightPaw.xRot = -air * 0.7F + step;

        // --- tail wiggles, the lure bobs on its stalk
        this.tail.yRot = Mth.sin(age * 0.6F) * (0.12F + walk * 0.4F);
        this.tail.xRot = -sq * 0.9F;
        this.stalk.xRot += -sq * 1.3F + Mth.sin(age * 0.13F) * 0.08F;
        this.stalk.zRot = Mth.sin(age * 0.09F) * 0.07F;
        this.lure.xRot = sq * 0.8F;

        this.eyelids.visible = s.blink;
    }
}
