package com.thesift.client.model;

import com.thesift.client.renderer.state.EnchoerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Enchoer: a tall crystalline wanderer. Its arms are folded wings; each wing and wing-tip opens on
 * its own slightly staggered curve (left leads, tips lag), so spreading looks organic.
 */
public class EnchoerModel extends EntityModel<EnchoerRenderState> {
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;
    private final ModelPart tail;
    private final ModelPart tailTip;

    public EnchoerModel(ModelPart root) {
        super(root);
        this.torso = root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftWing = this.torso.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.rightWing = this.torso.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
        this.leftAntler = this.head.getChild("left_antler");
        this.rightAntler = this.head.getChild("right_antler");
        this.tail = this.torso.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
    }

    @Override
    public void setupAnim(EnchoerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.3F);
        float pos = s.walkAnimationPos * 0.6F;

        // --- long, graceful stride with a soft bob
        this.leftLeg.xRot = Mth.cos(pos) * 0.9F * walk;
        this.rightLeg.xRot = -Mth.cos(pos) * 0.9F * walk;
        this.torso.y += Math.abs(Mth.cos(pos)) * -0.8F * walk + Mth.sin(age * 0.06F) * 0.25F;
        this.torso.yRot = Mth.sin(pos) * 0.06F * walk;
        this.torso.xRot = 0.05F * walk;

        this.head.yRot = s.yRot * Anim.DEG;
        this.head.xRot = s.xRot * Anim.DEG;
        if (s.singing) {
            this.head.xRot -= 0.35F + Mth.sin(age * 0.3F) * 0.06F;
            this.head.zRot = Mth.sin(age * 0.15F) * 0.12F;
        }
        this.leftAntler.zRot += Mth.sin(age * 0.07F) * 0.03F;
        this.rightAntler.zRot -= Mth.sin(age * 0.07F + 0.5F) * 0.03F;

        // --- wings: individually staggered unfolding
        float spread = s.wingSpread;
        float l = Anim.smooth(spread * 1.15F);
        float r = Anim.smooth(spread * 1.15F - 0.15F);
        float lt = Anim.smooth(spread * 1.5F - 0.45F);
        float rt = Anim.smooth(spread * 1.5F - 0.6F);
        float flutter = s.singing ? Mth.sin(age * 0.7F) * 0.08F : 0.0F;
        float sway = Mth.cos(pos) * 0.25F * walk * (1.0F - spread);
        this.leftWing.xRot += -sway;
        this.rightWing.xRot += sway;
        this.leftWing.yRot += l * 1.4F;
        this.leftWing.zRot += -l * (0.9F + flutter) - Mth.sin(age * 0.05F) * 0.02F;
        this.rightWing.yRot += -r * 1.4F;
        this.rightWing.zRot += r * (0.9F + flutter) + Mth.sin(age * 0.05F) * 0.02F;
        this.leftWingTip.xRot += -lt * 0.35F + (1.0F - lt) * 0.12F;
        this.leftWingTip.zRot += -lt * 0.25F - flutter;
        this.rightWingTip.xRot += -rt * 0.35F + (1.0F - rt) * 0.12F;
        this.rightWingTip.zRot += rt * 0.25F + flutter;

        // --- crystalline tail sways behind
        this.tail.yRot = Mth.sin(age * 0.06F) * 0.15F + Mth.sin(pos) * 0.2F * walk;
        this.tailTip.yRot = Mth.sin(age * 0.06F - 0.8F) * 0.2F;
    }
}
