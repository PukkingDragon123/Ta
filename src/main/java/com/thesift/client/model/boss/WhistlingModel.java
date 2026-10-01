package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** A Whistling: a fluffball that hops on stick legs, flaps furiously in the air and puffs up to pipe. */
public class WhistlingModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart beak;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public WhistlingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.beak = this.head.getChild("beak");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 1.3F;
        this.leftLeg.xRot = Mth.cos(pos) * 1.0F * walk;
        this.rightLeg.xRot = -Mth.cos(pos) * 1.0F * walk;
        this.body.y -= Math.abs(Mth.sin(pos)) * 1.2F * walk;
        this.body.zRot = Mth.sin(pos) * 0.12F * walk;
        // wings flutter while it runs, rest otherwise (with a twitch now and then)
        float flap = walk > 0.2F ? Mth.sin(age * 1.6F) * 0.9F * walk : Anim.envelope(Mth.positiveModulo(age, 70.0F), 0.0F, 2.0F, 2.0F, 2.0F) * 0.5F;
        this.leftWing.zRot = -Math.abs(flap) - 0.05F;
        this.rightWing.zRot = Math.abs(flap) + 0.05F;
        this.head.yRot = s.yRot * Anim.DEG;
        this.head.xRot = s.xRot * Anim.DEG + Mth.sin(age * 0.2F) * 0.05F;
        this.head.zRot = Mth.sin(age * 0.07F) * 0.15F;
        this.body.yScale = 1.0F + Mth.sin(age * 0.15F) * 0.03F;
        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 1.0F) {
            float puff = Anim.envelope(t, 0.0F, 0.5F, 0.1F, 0.2F);
            float toot = Anim.envelope(t, 0.6F, 0.03F, 0.1F, 0.25F);
            this.body.xScale = 1.0F + 0.25F * puff - 0.1F * toot;
            this.body.zScale = 1.0F + 0.25F * puff - 0.1F * toot;
            this.body.yScale = 1.0F + 0.15F * puff;
            this.head.xRot -= 0.4F * puff;
            this.beak.zScale = 1.0F + 0.4F * toot;
            this.leftWing.zRot -= 1.0F * toot;
            this.rightWing.zRot += 1.0F * toot;
        }
    }
}
