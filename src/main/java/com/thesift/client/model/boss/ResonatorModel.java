package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Resonator.
 *
 * <ul>
 *   <li>idle: the whole frame hums - a faint tremble - and the scroll head cocks side to side</li>
 *   <li>move: skittering spider gait on four long legs</li>
 *   <li>attack: one arm draws back like a bowstring, the frame bends, then it snaps forward and the
 *   strings vibrate hard</li>
 * </ul>
 */
public class ResonatorModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart frame;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart[] legs = new ModelPart[4];

    public ResonatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.frame = this.body.getChild("frame");
        this.head = this.frame.getChild("head");
        this.leftArm = this.frame.getChild("left_arm");
        this.rightArm = this.frame.getChild("right_arm");
        String[] names = {"front_left", "front_right", "hind_left", "hind_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild(names[i] + "_leg");
        }
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.8F);
        float pos = s.walkAnimationPos * 1.4F;

        float hum = Mth.sin(age * 1.7F) * 0.012F;
        this.frame.xScale = 1.0F + hum;
        this.head.zRot = Mth.sin(age * 0.06F) * 0.25F;
        this.head.yRot = s.yRot * Anim.DEG * 0.6F;
        this.body.yRot = s.yRot * Anim.DEG * 0.2F;
        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : Mth.PI;
            this.legs[i].xRot += Mth.sin(pos + phase) * 0.5F * walk;
            this.legs[i].yRot = Mth.cos(pos + phase) * 0.3F * walk;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.8F * walk;
        this.leftArm.zRot += Mth.sin(age * 0.08F) * 0.05F;
        this.rightArm.zRot -= Mth.sin(age * 0.08F + 1.0F) * 0.05F;

        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 1.1F) {
            float draw = Anim.envelope(t, 0.0F, 0.6F, 0.1F, 0.06F);
            float snap = Anim.envelope(t, 0.7F, 0.05F, 0.1F, 0.25F);
            this.rightArm.xRot += 1.2F * draw - 0.9F * snap;
            this.rightArm.zRot -= 0.6F * draw;
            this.frame.xRot += -0.25F * draw + 0.15F * snap;
            float buzz = Mth.sin(age * 4.0F) * 0.08F * (snap + draw * 0.3F);
            this.frame.xScale *= 1.0F + buzz;
            this.leftArm.zRot += buzz * 3.0F;
        }
        if (s.hasRedOverlay) {
            this.head.zRot += 0.5F;
            this.frame.xScale *= 1.06F;
        }
    }
}
