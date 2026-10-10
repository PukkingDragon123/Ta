package com.thesift.client.knowledge;

import com.thesift.client.model.Anim;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * F3: the Mini Creator (geometry from tools/mini_creator.py). S1 land: an avatar, a saint - he sits cross-legged in the
 * air in meditation, the whole figure floating up and down in a slow bob, leaning a little into the glide when he
 * follows you; a halo of light turns slowly behind his head; four rune blocks circle him, tumbling and bobbing out of
 * step, and two rune plates wheel the other way. Talking, his paws open and his head nods as his bill moves; waving,
 * he raises a paw in blessing; celebrating, he rises and spins while his blocks fly out wide and back; summoned, he
 * rises out of the dais (the rise); appearing, the blocks spiral in; leaving, he spins down to nothing.
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
    /** How long the rise out of the dais lasts (seconds; MiniCreator.RISE_TICKS). */
    private static final float RISE_SECONDS = 3.0F;

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
        float age = s.ageInTicks + s.seed * 20.0F;
        float glide = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);

        // --- floating in meditation: a slow bob of the whole figure (the arms and legs are root parts, so they move too)
        float lift = 0.0F;
        float rs = Anim.seconds(s.rise, s.ageInTicks);
        if (rs >= 0.0F && rs < RISE_SECONDS) {
            // summoned: he rises out of the dais, slowing as he comes to rest
            lift = 20.0F * (1.0F - Anim.smooth(Anim.clamp01(rs / RISE_SECONDS)));
        }
        float dy = Mth.sin(age * 0.06F) * 0.9F + lift;
        this.body.xRot += 0.14F * glide;
        this.tail.yRot += Mth.sin(age * 0.05F) * 0.08F;
        this.tail.xRot += Mth.sin(age * 0.04F) * 0.04F;
        for (int i = 0; i < 4; i++) {
            this.tassels[i].zRot += Mth.sin(age * 0.08F + i) * 0.06F;
            this.tassels[i].xRot += 0.25F * glide + Mth.cos(age * 0.07F + i) * 0.04F;
        }
        // --- looking about, calmly
        this.head.yRot += Mth.clamp(s.yRot * Anim.DEG * 0.6F, -0.8F, 0.8F);
        this.head.xRot += Mth.clamp(s.xRot * Anim.DEG * 0.4F, -0.3F, 0.3F);
        this.halo.zRot += age * 0.015F;
        this.crown.zRot += Mth.sin(age * 0.05F) * 0.02F;

        float radius = 9.0F;
        float spin = 0.0F;
        float orbitSpeed = 0.035F;

        // --- talking: the paws open, the head nods, the bill moves
        float tk = Anim.seconds(s.talk, s.ageInTicks);
        if (tk >= 0.0F) {
            float e = Anim.envelope(tk, 0.0F, 0.3F, 2.2F, 0.4F);
            this.frontLeft.zRot -= 0.5F * e;
            this.frontRight.zRot += 0.5F * e;
            this.frontLeft.xRot -= 0.35F * e;
            this.frontRight.xRot -= 0.35F * e;
            this.head.xRot += (0.12F + Mth.sin(tk * 5.0F) * 0.08F) * e;
            this.jaw.xRot += 0.35F * Math.abs(Mth.sin(tk * 11.0F)) * e;
            orbitSpeed += 0.03F * e;
        }
        // --- waving: a paw raised in blessing
        float wv = Anim.seconds(s.wave, s.ageInTicks);
        if (wv >= 0.0F) {
            float e = Anim.envelope(wv, 0.0F, 0.2F, 1.2F, 0.3F);
            this.frontRight.xRot -= 1.6F * e;
            this.frontRight.zRot += (0.15F + Mth.sin(wv * 6.0F) * 0.12F) * e;
            this.head.zRot += 0.1F * e;
        }
        // --- celebrating: he rises and spins, the blocks flung wide
        float cb = Anim.seconds(s.celebrate, s.ageInTicks);
        if (cb >= 0.0F && cb < 2.0F) {
            float hop = cb < 1.0F ? Mth.sin(cb * Mth.PI) : 0.0F;
            dy -= 5.0F * hop;
            this.body.yRot += Mth.TWO_PI * Anim.smooth(Anim.clamp01(cb / 1.0F));
            float fling = Mth.sin(Anim.clamp01(cb / 1.6F) * Mth.PI);
            radius += 6.0F * fling;
            spin = 4.0F * fling;
        }
        // --- appearing: the blocks spiral in from far out (and when summoned, as he rises)
        float ap = Anim.seconds(s.appear, s.ageInTicks);
        if (ap >= 0.0F && ap < 1.2F) {
            float k = 1.0F - Anim.smooth(Anim.clamp01(ap / 1.2F));
            radius += 10.0F * k;
            spin += 6.0F * k;
        }
        if (rs >= 0.0F && rs < RISE_SECONDS) {
            float k = 1.0F - Anim.smooth(Anim.clamp01(rs / RISE_SECONDS));
            radius += 8.0F * k;
            spin += 5.0F * k;
        }
        // --- leaving: he spins down to nothing in a swirl of his blocks
        float pf = Anim.seconds(s.poof, s.ageInTicks);
        if (pf >= 0.0F) {
            float k = Anim.clamp01(pf / 0.7F);
            this.body.yRot += 9.0F * k * k;
            radius *= 1.0F - k;
            spin += 8.0F * k;
        }
        for (ModelPart p : new ModelPart[]{this.body, this.frontLeft, this.frontRight, this.backLeft, this.backRight}) {
            p.y += dy;
        }

        // --- the rune blocks: a slow ring round him, each tumbling and bobbing out of step
        for (int i = 0; i < 4; i++) {
            float a = age * orbitSpeed + i * Mth.HALF_PI + spin;
            ModelPart o = this.orbs[i];
            o.x = Mth.cos(a) * radius;
            o.z = Mth.sin(a) * radius;
            o.y = 8.0F + dy + Mth.sin(age * 0.09F + i * 1.7F) * 1.2F;
            o.yRot = age * 0.05F + i;
            o.xRot = age * 0.03F * (i % 2 == 0 ? 1 : -1);
        }
        // the rune plates wheel the other way, closer and lower, turning as they go
        for (int i = 0; i < 2; i++) {
            float a = -age * orbitSpeed * 1.4F + i * Mth.PI - spin;
            ModelPart r = this.runes[i];
            r.x = Mth.cos(a) * radius * 0.7F;
            r.z = Mth.sin(a) * radius * 0.7F;
            r.y = 13.0F + dy + Mth.sin(age * 0.12F + i * 2.1F) * 0.8F;
            r.yRot = -a;
        }
    }
}
