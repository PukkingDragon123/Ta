package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A Strumling: a skittering eight-legged blur that crouches before it pounces. */
public class StrumlingModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart[] left = new ModelPart[4];
    private final ModelPart[] right = new ModelPart[4];

    public StrumlingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.abdomen = this.body.getChild("abdomen");
        for (int i = 0; i < 4; i++) {
            this.left[i] = this.body.getChild("left_leg_" + i);
            this.right[i] = this.body.getChild("right_leg_" + i);
        }
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.8F);
        float pos = s.walkAnimationPos * 1.4F;
        for (int i = 0; i < 4; i++) {
            float ph = pos * 2.0F + (i % 2 == 0 ? 0.0F : Mth.PI);
            this.left[i].yRot += Mth.cos(ph) * 0.45F * walk;
            this.right[i].yRot -= Mth.cos(ph) * 0.45F * walk;
            float lift = Math.max(0.0F, Mth.sin(ph)) * 0.5F * walk + Mth.sin(age * 0.3F + i) * 0.04F;
            this.left[i].zRot -= lift;
            this.right[i].zRot += lift;
        }
        this.abdomen.xRot = Mth.sin(age * 0.2F) * 0.08F;
        this.body.yRot = s.yRot * Anim.DEG * 0.2F;
        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 0.8F) {
            float crouch = Anim.envelope(t, 0.0F, 0.35F, 0.05F, 0.1F);
            this.body.y += 1.5F * crouch;
            for (int i = 0; i < 4; i++) {
                this.left[i].zRot -= 0.4F * crouch;
                this.right[i].zRot += 0.4F * crouch;
            }
            this.abdomen.xRot -= 0.4F * crouch;
        }
    }
}
