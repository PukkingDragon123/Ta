package com.thesift.client.model.boss;

import com.thesift.client.Expression;
import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Enforcer: a grumpy living drum.
 *
 * <ul>
 *   <li>idle: drums its own belly in a lazy rhythm, head-bobs to it, the brows twitch, the cymbal
 *   on its back tings on every fourth beat</li>
 *   <li>move: a heavy, rolling waddle; knuckle-walks on its mallets</li>
 *   <li>angry: brows slam into a V, jaw juts out</li>
 *   <li>attack: both mallets rise overhead, it sucks in its belly... and slams; the drum
 *   squashes, the jaw flies open, the cymbal shivers</li>
 *   <li>hurt: the drum skin shudders, brows shoot up</li>
 * </ul>
 */
public class EnforcerModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart leftBrow;
    private final ModelPart rightBrow;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftForearm;
    private final ModelPart rightForearm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart cymbal;
    private final ModelPart disc;

    public EnforcerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.leftBrow = this.body.getChild("left_brow");
        this.rightBrow = this.body.getChild("right_brow");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftForearm = this.leftArm.getChild("left_forearm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.cymbal = this.body.getChild("cymbal");
        this.disc = this.cymbal.getChild("cymbal_disc");
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos * 0.7F;
        float cw = Mth.cos(pos);
        float rest = 1.0F - walk;
        boolean angry = s.expression == Expression.ANGRY || s.windingUp;

        // --- breathing, waddle
        this.body.yScale = 1.0F + Mth.sin(age * 0.1F) * 0.02F;
        this.body.zRot = Mth.sin(pos) * 0.1F * walk;
        this.body.y -= Math.abs(cw) * 0.9F * walk;
        this.body.yRot = s.yRot * Anim.DEG * 0.3F;
        this.leftLeg.xRot = cw * 0.8F * walk;
        this.rightLeg.xRot = -cw * 0.8F * walk;

        // --- the belly rhythm: left, right, left, right... with a head-bob on each beat
        float beatL = Math.max(0.0F, Mth.sin(age * 0.3F));
        float beatR = Math.max(0.0F, Mth.sin(age * 0.3F + Mth.PI));
        this.leftArm.xRot = -cw * 0.6F * walk - beatL * 0.55F * rest;
        this.rightArm.xRot = cw * 0.6F * walk - beatR * 0.55F * rest;
        this.leftForearm.xRot = -beatL * 0.7F * rest - 0.2F;
        this.rightForearm.xRot = -beatR * 0.7F * rest - 0.2F;
        this.body.xRot = (beatL + beatR) * 0.02F * rest;
        // the cymbal shivers on every fourth beat
        float ting = Anim.envelope(Mth.positiveModulo(age, 42.0F), 0.0F, 1.0F, 1.0F, 8.0F);
        this.disc.zRot = Mth.sin(age * 2.2F) * 0.18F * ting;
        this.cymbal.xRot += Mth.sin(age * 1.7F) * 0.05F * ting;

        // --- face: brows and jaw act it out
        float twitch = Anim.envelope(Mth.positiveModulo(age + 13.0F, 95.0F), 0.0F, 2.0F, 3.0F, 4.0F);
        float scowl = angry ? 1.0F : 0.0F;
        this.leftBrow.zRot = -0.35F * scowl - twitch * 0.15F;
        this.rightBrow.zRot = 0.35F * scowl;
        this.leftBrow.y += scowl * 0.6F - twitch * 0.5F;
        this.rightBrow.y += scowl * 0.6F;
        this.jaw.xRot = Math.max(0.0F, Mth.sin(age * 0.3F)) * 0.06F + scowl * 0.08F;
        this.jaw.z -= scowl * 0.5F;

        // --- the slam
        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 1.4F) {
            float up = Anim.envelope(t, 0.0F, 0.7F, 0.2F, 0.08F);
            float slam = Anim.envelope(t, 0.9F, 0.06F, 0.15F, 0.3F);
            this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -2.9F) + slam * 0.4F;
            this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -2.9F) + slam * 0.4F;
            this.leftForearm.xRot = Mth.lerp(up, this.leftForearm.xRot, -0.4F);
            this.rightForearm.xRot = Mth.lerp(up, this.rightForearm.xRot, -0.4F);
            this.leftArm.zRot += 0.35F * up;
            this.rightArm.zRot -= 0.35F * up;
            // sucks the belly in, then the whole drum squashes with the hit
            this.body.yScale *= 1.0F + 0.1F * up - 0.24F * slam;
            this.body.xScale = 1.0F - 0.05F * up + 0.16F * slam;
            this.body.zScale = 1.0F - 0.05F * up + 0.16F * slam;
            this.jaw.xRot += 0.7F * slam + 0.15F * up;
            this.leftBrow.y -= 1.0F * slam;
            this.rightBrow.y -= 1.0F * slam;
            this.disc.zRot += Mth.sin(age * 3.0F) * 0.4F * slam;
            this.cymbal.xRot -= 0.3F * slam;
        }
        if (s.hasRedOverlay) {
            this.body.xScale *= 1.0F + Mth.sin(age * 3.0F) * 0.04F;
            this.jaw.xRot += 0.4F;
            this.leftBrow.y -= 0.8F;
            this.rightBrow.y -= 0.8F;
            this.leftBrow.zRot = 0.3F;
            this.rightBrow.zRot = -0.3F;
        }
    }
}
