package com.thesift.client.model;

import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * P4 Crocotodo (geometry: tools/jungle_mobs.py). A waddle: the body rolls side to side over its scaly legs, the
 * neck pumps back and forth with every step like a pigeon's, the fan tail wags and the stubby wings twitch. Idle,
 * it looks about and ruffles its crest. animA pecks (head down three times, jaw clacking); animB is the snap of its
 * crocodile jaws (head cocked back, then lunged forward wide open); animC flaps its useless little wings.
 */
public class CrocotodoModel extends EntityModel<JungleRenderState> {
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart crest;
    private final ModelPart jaw;
    private final ModelPart wattle;
    private final ModelPart tail;
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[] wingTips = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public CrocotodoModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.crest = this.head.getChild("crest");
        this.jaw = this.head.getChild("jaw");
        this.wattle = this.jaw.getChild("wattle");
        this.tail = this.body.getChild("tail");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.wings[i] = this.body.getChild(s + "_wing");
            this.wingTips[i] = this.wings[i].getChild(s + "_wing_tip");
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
        float pos = s.walkAnimationPos * 1.3F;
        float sw = Mth.sin(pos);
        // ---- the waddle
        for (int i = 0; i < 2; i++) {
            float ph = i == 0 ? sw : -sw;
            this.legs[i].xRot += ph * 0.8F * walk;
            this.shins[i].xRot += Math.max(0.0F, -ph) * 0.5F * walk;
            this.feet[i].xRot -= ph * 0.5F * walk;
        }
        this.body.zRot += sw * 0.1F * walk;
        this.body.y -= Math.abs(Mth.cos(pos)) * 0.8F * walk;
        this.body.xRot += 0.12F * walk;
        // the neck pumps: forward on each step, back between
        this.neck.z -= Mth.cos(pos * 2.0F) * 0.8F * walk;
        this.neck.xRot += Mth.cos(pos * 2.0F) * 0.15F * walk - 0.15F * walk;
        this.tail.yRot += Mth.sin(pos) * 0.25F * walk + Mth.sin(age * 0.08F) * 0.08F;
        this.tail.xRot += Mth.sin(age * 0.05F) * 0.05F;
        // ---- looking about, crest and wings twitching
        this.head.yRot += s.yRot * Anim.DEG * 0.8F;
        this.head.xRot += s.xRot * Anim.DEG * 0.5F;
        this.neck.xRot += Mth.sin(age * 0.06F) * 0.04F;
        this.crest.xRot += Mth.sin(age * 0.09F) * 0.06F;
        this.wattle.zRot += Mth.sin(pos) * 0.2F * walk + Mth.sin(age * 0.1F) * 0.06F;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.wings[i].zRot -= sx * (0.08F * walk + Mth.sin(age * 0.11F + i) * 0.03F);
        }
        // breathing
        this.body.yScale = 1.0F + Mth.sin(age * 0.09F) * 0.015F;

        // ---- pecking: head down to the moss three times, jaw clacking
        float pk = Anim.seconds(s.animA, s.ageInTicks);
        if (pk >= 0.0F && pk < 0.7F) {
            float e = Anim.envelope(pk, 0.0F, 0.08F, 0.4F, 0.2F);
            float bob = Math.abs(Mth.sin(pk * 14.0F));
            this.neck.xRot += (0.9F + 0.35F * bob) * e;
            this.head.xRot += (0.5F + 0.3F * bob) * e;
            this.jaw.xRot += 0.3F * bob * e;
        }
        // ---- the snap: head cocked back, then a lunge with the jaws wide
        float sn = Anim.seconds(s.animB, s.ageInTicks);
        if (sn >= 0.0F && sn < 0.6F) {
            float back = Anim.envelope(sn, 0.0F, 0.1F, 0.02F, 0.08F);
            float lunge = Anim.envelope(sn, 0.12F, 0.06F, 0.08F, 0.3F);
            this.neck.xRot += -0.4F * back + 0.6F * lunge;
            this.head.xRot += -0.3F * back + 0.25F * lunge;
            this.jaw.xRot += 0.9F * back + 0.1F * lunge;
            this.neck.z -= 2.0F * lunge;
            this.body.xRot += 0.15F * lunge;
        }
        // ---- flapping its stubby wings
        float fp = Anim.seconds(s.animC, s.ageInTicks);
        if (fp >= 0.0F && fp < 0.8F) {
            float e = Anim.envelope(fp, 0.0F, 0.06F, 0.5F, 0.2F);
            float flap = Mth.sin(fp * 34.0F);
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.wings[i].zRot -= sx * (0.6F + 0.5F * flap) * e;
                this.wingTips[i].zRot -= sx * 0.4F * flap * e;
            }
            this.crest.xRot -= 0.3F * e;
            this.tail.xRot -= 0.3F * e;
            this.body.y -= 0.6F * Math.abs(flap) * e;
        }
    }
}
