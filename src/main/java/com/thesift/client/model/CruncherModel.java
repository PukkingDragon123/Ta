package com.thesift.client.model;

import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * P4 Cruncher (geometry: tools/jungle_mobs.py). A quick raptor stride - head level, body tilting into each step,
 * the stiff tail counterbalancing, little arms tucked. Idle, it sniffs about with sharp, birdlike head turns.
 * animA bites (head thrust forward, jaw snapping); animB crunches rock (head down, jaw grinding); animC is the
 * squat as it leaves its Magnesium behind.
 */
public class CruncherModel extends EntityModel<JungleRenderState> {
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tip;
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] claws = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public CruncherModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tip = this.tail.getChild("tail_tip");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.arms[i] = this.body.getChild(s + "_arm");
            this.claws[i] = this.arms[i].getChild(s + "_claw");
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
        }
    }

    @Override
    public void setupAnim(JungleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 1.4F;
        float sw = Mth.sin(pos);
        for (int i = 0; i < 2; i++) {
            float a = i == 0 ? sw : -sw;
            this.legs[i].xRot += a * 0.85F * walk;
            this.shins[i].xRot += Math.max(0.0F, a) * 0.5F * walk;
            this.feet[i].xRot -= a * 0.45F * walk;
            this.arms[i].xRot += Mth.sin(age * 0.1F + i) * 0.06F + 0.2F * walk;
            this.claws[i].xRot += Mth.sin(age * 0.13F + i) * 0.08F;
        }
        this.body.y -= Math.abs(Mth.cos(pos)) * 0.7F * walk;
        this.body.zRot += sw * 0.06F * walk;
        this.body.xRot += 0.1F * walk;
        this.tail.yRot += Mth.sin(pos) * 0.2F * walk + Mth.sin(age * 0.06F) * 0.06F;
        this.tip.yRot += Mth.sin(pos - 0.8F) * 0.25F * walk + Mth.sin(age * 0.06F - 0.8F) * 0.1F;
        this.tail.xRot -= 0.08F * walk;
        // sharp, birdlike looks about
        float tick = (float) Math.floor((age + s.seed) / 18.0F);
        float jerk = Mth.sin(tick * 2.3F) * 0.4F * (1.0F - walk);
        this.head.yRot += s.yRot * Anim.DEG * 0.7F + jerk;
        this.head.xRot += s.xRot * Anim.DEG * 0.5F;
        this.neck.xRot -= 0.1F * walk;
        this.jaw.xRot += Math.max(0.0F, Mth.sin(age * 0.08F)) * 0.08F;
        this.body.yScale = 1.0F + Mth.sin(age * 0.12F) * 0.015F;

        float bt = Anim.seconds(s.animA, s.ageInTicks);
        if (bt >= 0.0F && bt < 0.5F) {
            float open = Anim.envelope(bt, 0.0F, 0.08F, 0.04F, 0.08F);
            float lunge = Anim.envelope(bt, 0.1F, 0.05F, 0.08F, 0.25F);
            this.jaw.xRot += 0.9F * open;
            this.neck.xRot += 0.4F * lunge - 0.2F * open;
            this.neck.z -= 1.5F * lunge;
            this.body.xRot += 0.15F * lunge;
        }
        float cr = Anim.seconds(s.animB, s.ageInTicks);
        if (cr >= 0.0F && cr < 1.0F) {
            float e = Anim.envelope(cr, 0.0F, 0.15F, 0.55F, 0.3F);
            this.neck.xRot += 0.7F * e;
            this.head.xRot += 0.35F * e;
            this.jaw.xRot += (0.25F + 0.25F * Mth.sin(cr * 36.0F)) * e;
            this.head.zRot += Mth.sin(cr * 18.0F) * 0.08F * e;
            this.body.xRot += 0.15F * e;
        }
        float ws = Anim.seconds(s.animC, s.ageInTicks);
        if (ws >= 0.0F && ws < 1.0F) {
            float e = Anim.envelope(ws, 0.0F, 0.2F, 0.4F, 0.3F);
            this.body.y += 1.2F * e;
            this.tail.xRot -= 0.5F * e;
            this.body.xRot -= 0.15F * e;
        }
    }
}
