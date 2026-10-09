package com.thesift.client.knowledge;

import com.thesift.client.model.Anim;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * F3: the Mini Creator (geometry from tools/mini_creator.py). A platypus waddle - diagonal legs together, the body
 * rolling over each step, the flat tail swinging the other way - and four little blocks circling over his back,
 * tumbling and bobbing out of step. Talking he rears up on his tail, paws up, and his bill clacks; waving, a front
 * paw paddles the air; celebrating he hops and spins while his blocks fly out wide and back; appearing, the blocks
 * spiral in; leaving, he spins down to nothing.
 */
public class MiniCreatorModel extends EntityModel<MiniCreatorRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart crown;
    private final ModelPart tail;
    private final ModelPart[] tassels = new ModelPart[4];
    private final ModelPart frontLeft;
    private final ModelPart frontRight;
    private final ModelPart backLeft;
    private final ModelPart backRight;
    private final ModelPart[] orbs = new ModelPart[4];
    /** S1 land: two glowing rune plates circling the other way, and the halo over his explorer's hat. */
    private final ModelPart[] runes = new ModelPart[2];
    private final ModelPart halo;

    public MiniCreatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.crown = this.head.getChild("crown");
        this.halo = this.crown.getChild("halo");
        this.runes[0] = root.getChild("rune_0");
        this.runes[1] = root.getChild("rune_1");
        this.tail = this.body.getChild("tail");
        for (int i = 0; i < 4; i++) {
            this.tassels[i] = this.body.getChild("tassel_" + i);
            this.orbs[i] = root.getChild("orb_" + i);
        }
        this.frontLeft = root.getChild("front_left_leg");
        this.frontRight = root.getChild("front_right_leg");
        this.backLeft = root.getChild("back_left_leg");
        this.backRight = root.getChild("back_right_leg");
    }

    @Override
    public void setupAnim(MiniCreatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        float pos = s.walkAnimationPos * 1.2F;

        // --- the waddle
        float step = Mth.sin(pos) * 0.7F * walk;
        this.frontLeft.xRot += step;
        this.backRight.xRot += step;
        this.frontRight.xRot -= step;
        this.backLeft.xRot -= step;
        this.body.zRot += Mth.sin(pos) * 0.09F * walk;
        this.body.y -= Math.abs(Mth.cos(pos)) * 0.6F * walk;
        this.tail.yRot += Mth.sin(pos) * -0.35F * walk + Mth.sin(age * 0.11F) * 0.12F;
        this.tail.xRot += Mth.sin(age * 0.07F) * 0.04F;
        for (int i = 0; i < 4; i++) {
            this.tassels[i].zRot += Mth.sin(pos + i) * 0.3F * walk + Mth.sin(age * 0.1F + i) * 0.05F;
            this.tassels[i].xRot += Mth.cos(pos * 0.5F + i) * 0.2F * walk;
        }
        // --- looking about; now and then he dabbles his bill
        this.head.yRot += s.yRot * Anim.DEG * 0.7F;
        this.head.xRot += Mth.clamp(s.xRot * Anim.DEG * 0.5F, -0.35F, 0.35F);
        float dab = (age + s.seed * 13.0F) % 160.0F;
        if (dab < 20.0F) {
            float e = Mth.sin(dab / 20.0F * Mth.PI);
            this.head.xRot += 0.35F * e;
            this.jaw.xRot += 0.25F * Math.abs(Mth.sin(dab * 0.9F)) * e;
        }
        this.crown.zRot += Mth.sin(age * 0.05F) * 0.03F;

        float radius = 6.5F;
        float spin = 0.0F;
        float orbitSpeed = 0.06F;

        // --- talking: up on his tail, paws raised, bill clacking
        float tk = Anim.seconds(s.talk, s.ageInTicks);
        if (tk >= 0.0F) {
            float e = Anim.envelope(tk, 0.0F, 0.3F, 2.2F, 0.4F);
            this.body.xRot -= 0.6F * e;
            this.body.y -= 2.0F * e;
            this.head.xRot += 0.45F * e;
            this.frontLeft.xRot -= (0.9F + Mth.sin(tk * 7.0F) * 0.25F) * e;
            this.frontRight.xRot -= (0.9F + Mth.cos(tk * 7.0F) * 0.25F) * e;
            this.frontLeft.y -= 2.5F * e;
            this.frontRight.y -= 2.5F * e;
            this.frontLeft.z -= 1.0F * e;
            this.frontRight.z -= 1.0F * e;
            this.jaw.xRot += 0.4F * Math.abs(Mth.sin(tk * 11.0F)) * e;
            this.tail.xRot += 0.5F * e;
            orbitSpeed += 0.04F * e;
        }
        // --- waving: a front paw paddles the air
        float wv = Anim.seconds(s.wave, s.ageInTicks);
        if (wv >= 0.0F) {
            float e = Anim.envelope(wv, 0.0F, 0.2F, 1.2F, 0.3F);
            this.frontRight.xRot -= 1.3F * e;
            this.frontRight.zRot += (0.4F + Mth.sin(wv * 12.0F) * 0.45F) * e;
            this.frontRight.y -= 2.0F * e;
            this.body.zRot -= 0.12F * e;
            this.head.zRot += 0.15F * e;
        }
        // --- celebrating: a hop and a spin, the blocks flung wide
        float cb = Anim.seconds(s.celebrate, s.ageInTicks);
        if (cb >= 0.0F && cb < 2.0F) {
            float hop = cb < 0.8F ? Mth.sin(cb / 0.8F * Mth.PI) : 0.0F;
            this.body.y -= 7.0F * hop;
            for (ModelPart leg : new ModelPart[]{this.frontLeft, this.frontRight, this.backLeft, this.backRight}) {
                leg.y -= 7.0F * hop;
                leg.zRot += (leg == this.frontLeft || leg == this.backLeft ? -0.5F : 0.5F) * hop;
            }
            this.body.yRot += Mth.TWO_PI * Anim.smooth(Anim.clamp01(cb / 0.8F));
            this.jaw.xRot += 0.5F * hop;
            this.tail.xRot -= 0.6F * hop;
            float fling = Mth.sin(Anim.clamp01(cb / 1.6F) * Mth.PI);
            radius += 6.0F * fling;
            spin = 4.0F * fling;
        }
        // --- appearing: the blocks spiral in from far out
        float ap = Anim.seconds(s.appear, s.ageInTicks);
        if (ap >= 0.0F && ap < 1.2F) {
            float k = 1.0F - Anim.smooth(Anim.clamp01(ap / 1.2F));
            radius += 10.0F * k;
            spin += 6.0F * k;
        }
        // --- leaving: he spins down to nothing in a swirl of his blocks
        float pf = Anim.seconds(s.poof, s.ageInTicks);
        if (pf >= 0.0F) {
            float k = Anim.clamp01(pf / 0.7F);
            this.body.yRot += 9.0F * k * k;
            radius *= 1.0F - k;
            spin += 8.0F * k;
            this.frontRight.xRot -= 1.2F * Math.min(1.0F, pf * 4.0F);
        }

        // --- the floating blocks: a slow ring over his back, each tumbling and bobbing out of step
        for (int i = 0; i < 4; i++) {
            float a = age * orbitSpeed + i * Mth.HALF_PI + spin;
            ModelPart o = this.orbs[i];
            o.x = Mth.cos(a) * radius;
            o.z = Mth.sin(a) * radius;
            o.y = 9.0F + Mth.sin(age * 0.09F + i * 1.7F) * 1.2F - (tk >= 0.0F ? 2.0F : 0.0F);
            o.yRot = age * 0.05F + i;
            o.xRot = age * 0.03F * (i % 2 == 0 ? 1 : -1);
        }
        // S1 land: the rune plates wheel the other way, lower and closer, turning as they go; the halo floats and turns slowly
        for (int i = 0; i < 2; i++) {
            float a = -age * orbitSpeed * 1.4F + i * Mth.PI - spin;
            ModelPart r = this.runes[i];
            r.x = Mth.cos(a) * radius * 0.7F;
            r.z = Mth.sin(a) * radius * 0.7F;
            r.y = 12.5F + Mth.sin(age * 0.12F + i * 2.1F) * 0.8F - (tk >= 0.0F ? 2.0F : 0.0F);
            r.yRot = -a;
        }
        this.halo.y -= 0.4F + Mth.sin(age * 0.1F) * 0.4F;
        this.halo.yRot = age * 0.03F;
    }
}
