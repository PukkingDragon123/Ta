package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Enforcer.
 *
 * <ul>
 *   <li>idle: the drum body breathes, the jaw chatters, mallets tap the rim in a slow rhythm</li>
 *   <li>move: a heavy, rolling waddle; the arms knuckle-walk</li>
 *   <li>attack: both mallets rise overhead and slam down onto the drum head; the whole body
 *   squashes with the beat</li>
 *   <li>hurt: the drum skin shudders</li>
 * </ul>
 */
public class EnforcerModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public EnforcerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.leftArm = this.body.getChild("left_arm");
        this.rightArm = this.body.getChild("right_arm");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos * 0.7F;
        float cw = Mth.cos(pos);

        this.body.yScale = 1.0F + Mth.sin(age * 0.1F) * 0.02F;
        this.body.zRot = Mth.sin(pos) * 0.09F * walk;
        this.body.y -= Math.abs(cw) * 0.8F * walk;
        this.leftLeg.xRot = cw * 0.8F * walk;
        this.rightLeg.xRot = -cw * 0.8F * walk;
        this.jaw.xRot = Math.max(0.0F, Mth.sin(age * 0.3F)) * 0.12F;
        // mallets tap the rim at rest, knuckle-walk when moving
        float tap = Math.max(0.0F, Mth.sin(age * 0.25F)) * (1.0F - walk);
        this.leftArm.xRot = -cw * 0.6F * walk - tap * 0.5F;
        this.rightArm.xRot = cw * 0.6F * walk - Math.max(0.0F, Mth.sin(age * 0.25F + Mth.PI)) * 0.5F * (1.0F - walk);

        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 1.4F) {
            // raise both mallets overhead (0.9s windup), then slam down onto the drum head
            float up = Anim.envelope(t, 0.0F, 0.7F, 0.2F, 0.08F);
            float slam = Anim.envelope(t, 0.9F, 0.06F, 0.15F, 0.3F);
            this.leftArm.xRot = Mth.lerp(up, this.leftArm.xRot, -2.9F) + slam * 0.4F;
            this.rightArm.xRot = Mth.lerp(up, this.rightArm.xRot, -2.9F) + slam * 0.4F;
            this.leftArm.zRot += 0.35F * up;
            this.rightArm.zRot -= 0.35F * up;
            this.body.yScale *= 1.0F + 0.08F * up - 0.22F * slam;
            this.body.xScale = 1.0F + 0.15F * slam;
            this.body.zScale = 1.0F + 0.15F * slam;
            this.jaw.xRot += 0.6F * slam + 0.2F * up;
        }
        if (s.hasRedOverlay) {
            this.body.xScale *= 1.0F + Mth.sin(age * 3.0F) * 0.04F;
            this.jaw.xRot += 0.4F;
        }
    }
}
