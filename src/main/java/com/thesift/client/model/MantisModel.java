package com.thesift.client.model;

import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * P4 Mantis (geometry: tools/jungle_mobs.py). It stalks: the body sways forward and back between steps the way a
 * mantis rocks like a leaf in the wind, the four long legs step in a tripod gait, the head swivels to follow you,
 * antennae twitching, scythes folded in prayer. animA is the strike (the scythes cocked back, then flung out and
 * snapped shut); animB eats (head down, mandibles working); animC slices what it holds (the blades scissor). While
 * diving (flagA) the wings fan open and the scythes reach out; while it holds its prey (flagB) the scythes clamp in.
 */
public class MantisModel extends EntityModel<JungleRenderState> {
    private final ModelPart body;
    private final ModelPart abdomen;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[] scythes = new ModelPart[2];
    private final ModelPart[] femurs = new ModelPart[2];
    private final ModelPart[] blades = new ModelPart[2];
    private final ModelPart[] mandibles = new ModelPart[2];
    private final ModelPart[] antennae = new ModelPart[2];
    private final ModelPart[] midLegs = new ModelPart[2];
    private final ModelPart[] hindLegs = new ModelPart[2];
    private final ModelPart[] midShins = new ModelPart[2];
    private final ModelPart[] hindShins = new ModelPart[2];

    public MantisModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.abdomen = this.body.getChild("abdomen");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.wings[i] = this.body.getChild(s + "_wing");
            this.scythes[i] = this.neck.getChild(s + "_scythe");
            this.femurs[i] = this.scythes[i].getChild(s + "_femur");
            this.blades[i] = this.femurs[i].getChild(s + "_blade");
            this.mandibles[i] = this.head.getChild(s + "_mandible");
            this.antennae[i] = this.head.getChild(s + "_antenna");
            this.midLegs[i] = this.body.getChild(s + "_mid_leg");
            this.hindLegs[i] = this.body.getChild(s + "_hind_leg");
            this.midShins[i] = this.midLegs[i].getChild(s + "_mid_shin");
            this.hindShins[i] = this.hindLegs[i].getChild(s + "_hind_shin");
        }
    }

    @Override
    public void setupAnim(JungleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos * 1.1F;
        float sw = Mth.sin(pos);
        float idle = 1.0F - walk;
        // ---- the leaf-in-the-wind sway, the tripod gait
        this.body.z += Mth.sin(age * 0.07F) * 0.6F * idle;
        this.body.xRot += Mth.sin(age * 0.07F) * 0.03F * idle;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            float a = i == 0 ? sw : -sw;
            this.midLegs[i].xRot += a * 0.45F * walk;
            this.hindLegs[i].xRot -= a * 0.45F * walk;
            this.midLegs[i].zRot += sx * Math.max(0.0F, a) * 0.25F * walk;
            this.hindLegs[i].zRot += sx * Math.max(0.0F, -a) * 0.25F * walk;
            this.midShins[i].zRot -= sx * Math.max(0.0F, a) * 0.2F * walk;
            this.hindShins[i].zRot -= sx * Math.max(0.0F, -a) * 0.2F * walk;
        }
        this.body.y -= Math.abs(Mth.cos(pos)) * 0.5F * walk;
        this.abdomen.xRot += Mth.sin(age * 0.05F) * 0.04F - Mth.sin(pos) * 0.03F * walk;
        this.abdomen.yRot += Mth.sin(pos * 0.5F) * 0.04F * walk;
        // ---- the head follows you; antennae and mandibles never still
        this.head.yRot += s.yRot * Anim.DEG * 0.9F;
        this.head.xRot += s.xRot * Anim.DEG * 0.5F;
        this.head.zRot += Mth.sin(age * 0.045F) * 0.12F * idle;
        this.neck.xRot += Mth.sin(age * 0.06F) * 0.03F + 0.08F * walk;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.antennae[i].xRot += Mth.sin(age * 0.15F + i * 2.0F) * 0.12F;
            this.antennae[i].zRot += sx * Mth.sin(age * 0.11F + i) * 0.08F;
            this.mandibles[i].zRot += sx * Mth.sin(age * 0.4F + i) * 0.08F;
            // scythes folded in prayer, rocking a little with the sway
            this.scythes[i].xRot += Mth.sin(age * 0.07F + 0.5F) * 0.05F - 0.1F * walk;
            this.blades[i].xRot += Mth.sin(age * 0.09F + i) * 0.03F;
            this.wings[i].zRot += sx * Mth.sin(age * 0.05F + i) * 0.015F;
        }

        // ---- diving: wings fanned and beating, scythes reaching out ahead, legs drawn up
        if (s.flagA) {
            float beat = Mth.sin(age * 2.2F);
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.wings[i].zRot -= sx * (1.0F + 0.4F * beat);
                this.wings[i].yRot += sx * 0.25F;
                this.scythes[i].xRot -= 1.1F;
                this.blades[i].xRot -= 1.4F;
                this.midLegs[i].xRot -= 0.5F;
                this.hindLegs[i].xRot += 0.5F;
            }
            this.neck.xRot += 0.35F;
        }
        // ---- holding its prey: scythes clamped in front, head lowered over it
        if (s.flagB) {
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.scythes[i].xRot -= 0.7F;
                this.scythes[i].zRot += sx * 0.15F;
                this.blades[i].xRot += 0.25F;
            }
            this.neck.xRot += 0.3F;
            this.head.xRot += 0.4F;
        }
        // ---- the strike: cocked back... then flung out and snapped shut
        float st = Anim.seconds(s.animA, s.ageInTicks);
        if (st >= 0.0F && st < 0.7F) {
            float cock = Anim.envelope(st, 0.0F, 0.12F, 0.03F, 0.06F);
            float out = Anim.envelope(st, 0.14F, 0.05F, 0.1F, 0.35F);
            for (int i = 0; i < 2; i++) {
                this.scythes[i].xRot += 0.6F * cock - 1.5F * out;
                this.femurs[i].xRot += 0.2F * cock - 0.4F * out;
                this.blades[i].xRot += 0.1F * cock - 1.8F * out + 1.2F * Anim.envelope(st, 0.24F, 0.04F, 0.05F, 0.3F);
            }
            this.neck.xRot += -0.15F * cock + 0.35F * out;
            this.body.z -= 2.0F * out;
        }
        // ---- eating: head down, mandibles chewing, scythes holding the crystal to its mouth
        float ea = Anim.seconds(s.animB, s.ageInTicks);
        if (ea >= 0.0F && ea < 1.0F) {
            float e = Anim.envelope(ea, 0.0F, 0.15F, 0.6F, 0.25F);
            this.neck.xRot += 0.6F * e;
            this.head.xRot += 0.5F * e;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.mandibles[i].zRot += sx * Mth.sin(ea * 30.0F) * 0.35F * e;
                this.scythes[i].xRot -= 0.5F * e;
                this.blades[i].xRot += 0.3F * e;
            }
        }
        // ---- slicing what it holds: the blades scissor
        float sl = Anim.seconds(s.animC, s.ageInTicks);
        if (sl >= 0.0F && sl < 0.5F) {
            float e = Anim.envelope(sl, 0.0F, 0.06F, 0.1F, 0.3F);
            this.blades[0].xRot -= 1.2F * e;
            this.blades[1].xRot -= 1.2F * Anim.envelope(sl, 0.08F, 0.06F, 0.1F, 0.26F);
            this.scythes[0].zRot += 0.3F * e;
            this.scythes[1].zRot -= 0.3F * e;
            this.head.xRot += 0.2F * e;
        }
    }
}
