package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A Thumpling: a bouncy waddle, a bobbing head, and a rear-back-and-drum for its attack. */
public class ThumplingModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart drum;
    private final ModelPart tail;
    private final ModelPart[] legs = new ModelPart[4];

    public ThumplingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.drum = this.body.getChild("drum");
        this.tail = this.body.getChild("tail");
        String[] n = {"front_left_leg", "front_right_leg", "hind_left_leg", "hind_right_leg"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(n[i]);
        }
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 1.1F;
        float c = Mth.cos(pos);
        this.legs[0].xRot = c * 0.9F * walk;
        this.legs[3].xRot = c * 0.9F * walk;
        this.legs[1].xRot = -c * 0.9F * walk;
        this.legs[2].xRot = -c * 0.9F * walk;
        // a waddle with a little hop on every step
        this.body.zRot = Mth.sin(pos) * 0.15F * walk;
        this.body.y -= Math.abs(c) * 0.9F * walk;
        this.body.yScale = 1.0F + Math.abs(c) * 0.06F * walk + Mth.sin(age * 0.12F) * 0.02F;
        this.head.yRot = s.yRot * Anim.DEG * 0.8F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F + Mth.sin(age * 0.15F) * 0.06F;
        this.tail.yRot = Mth.sin(age * 0.4F + pos) * 0.4F;
        this.drum.yScale = 1.0F + Math.max(0.0F, Mth.sin(age * 0.3F)) * 0.04F;
        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 1.2F) {
            float up = Anim.envelope(t, 0.0F, 0.55F, 0.1F, 0.05F);
            float hit = Anim.envelope(t, 0.7F, 0.04F, 0.1F, 0.3F);
            this.body.xRot = -0.5F * up + 0.1F * hit;
            this.body.y -= 1.5F * up;
            this.legs[0].xRot = -1.0F * up;
            this.legs[1].xRot = -1.0F * up;
            this.drum.yScale *= 1.0F - 0.25F * hit;
            this.body.xScale = 1.0F + 0.15F * hit;
            this.body.zScale = 1.0F + 0.15F * hit;
            this.body.yScale *= 1.0F - 0.15F * hit;
        }
    }
}
