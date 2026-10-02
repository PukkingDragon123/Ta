package com.thesift.client.model;

import com.thesift.client.renderer.state.EnchoerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Enchoer: a big, gentle, melancholy mound of mint fur with a pale sad face and moose antlers. It
 * moves like something heavy and tired.
 *
 * <ul>
 *   <li>idle: slow breathing, and every so often a deep sigh that lifts the shoulders and drops the
 *   head</li>
 *   <li>move: a lumbering side-to-side waddle, arms swinging loosely</li>
 *   <li>trading: one paw raised thoughtfully to its chin, head tilted</li>
 *   <li>singing: arms spread wide, face lifted, rocking to the music</li>
 *   <li>hurt: hunches and hides its face behind both paws</li>
 *   <li>death: goes limp as it topples</li>
 * </ul>
 */
public class EnchoerModel extends EntityModel<EnchoerRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;

    public EnchoerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.leftAntler = this.head.getChild("left_antler");
        this.rightAntler = this.head.getChild("right_antler");
    }

    @Override
    public void setupAnim(EnchoerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
        float pos = s.walkAnimationPos * 0.55F;
        float sw = Mth.sin(pos);
        float cw = Mth.cos(pos);

        // --- a deep sigh every ten seconds or so
        float sighT = (age + s.seed) % 210.0F / 20.0F;
        float sigh = Anim.envelope(sighT, 0.0F, 0.9F, 0.3F, 1.4F) * (1.0F - walk);

        // --- lumbering waddle: the whole mound rocks from foot to foot
        this.body.zRot = sw * 0.07F * walk;
        this.body.y -= Math.abs(cw) * 1.0F * walk;
        this.leftLeg.xRot = cw * 0.7F * walk;
        this.rightLeg.xRot = -cw * 0.7F * walk;
        this.leftLeg.y -= Math.max(0.0F, -cw) * 1.0F * walk;
        this.rightLeg.y -= Math.max(0.0F, cw) * 1.0F * walk;

        // --- head: follows the player, droops on a sigh
        this.head.yRot = s.yRot * Anim.DEG * 0.8F;
        this.head.xRot = s.xRot * Anim.DEG * 0.7F + 0.05F + sigh * 0.3F;
        this.head.zRot = sw * 0.05F * walk;
        this.leftAntler.zRot += Mth.sin(age * 0.07F) * 0.02F;
        this.rightAntler.zRot -= Mth.sin(age * 0.07F + 0.5F) * 0.02F;

        // --- arms hang heavy, swinging loosely against the stride
        float idle = Mth.sin(age * 0.05F) * 0.03F;
        this.leftArm.xRot = -cw * 0.45F * walk + idle;
        this.rightArm.xRot = cw * 0.45F * walk - idle;
        this.leftArm.zRot += -sigh * 0.08F;
        this.rightArm.zRot += sigh * 0.08F;
        this.leftForearm.xRot = -0.12F - Math.max(0.0F, cw) * 0.35F * walk;
        this.rightForearm.xRot = -0.12F - Math.max(0.0F, -cw) * 0.35F * walk;

        float open = s.wingSpread;
        if (s.singing) {
            // --- singing: arms spread wide, face lifted, rocking to the music
            float rock = Mth.sin(age * 0.25F);
            this.leftArm.zRot += -open * 1.05F;
            this.rightArm.zRot += open * 1.05F;
            this.leftArm.xRot += -open * 0.3F;
            this.rightArm.xRot += -open * 0.3F;
            this.leftForearm.zRot += -open * (0.4F + rock * 0.15F);
            this.rightForearm.zRot += open * (0.4F - rock * 0.15F);
            this.body.zRot += rock * 0.06F * open;
            this.head.xRot -= open * 0.5F;
            this.head.zRot += Mth.sin(age * 0.12F) * 0.12F * open;
        } else if (open > 0.0F) {
            // --- trading: right paw raised to the chin, the left one resting on its belly
            this.rightArm.xRot += -1.25F * open;
            this.rightArm.yRot += 0.45F * open;
            this.rightForearm.xRot += -1.3F * open;
            this.leftArm.xRot += -0.35F * open;
            this.leftArm.yRot += -0.3F * open;
            this.leftForearm.xRot += -0.9F * open;
            this.head.xRot += 0.12F * open;
            this.head.zRot += 0.12F * open;
        }

        // --- hurt: hunch over and hide the face behind both paws
        if (s.hasRedOverlay) {
            this.leftArm.xRot = -1.55F;
            this.leftArm.yRot = -0.5F;
            this.leftForearm.xRot = -0.9F;
            this.rightArm.xRot = -1.55F;
            this.rightArm.yRot = 0.5F;
            this.rightForearm.xRot = -0.9F;
            this.head.xRot += 0.35F;
            this.body.xRot += 0.12F;
        }

        // --- death: go limp while toppling over
        if (s.deathTime > 0.0F) {
            float limp = Anim.smooth(s.deathTime / 12.0F);
            this.leftArm.zRot += -limp * 0.5F;
            this.rightArm.zRot += limp * 0.3F;
            this.leftForearm.xRot = 0.0F;
            this.rightForearm.xRot = 0.0F;
            this.head.xRot += limp * 0.5F;
        }
    }
}
