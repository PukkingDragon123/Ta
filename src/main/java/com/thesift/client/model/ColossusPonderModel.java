package com.thesift.client.model;

import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * P4 Colossus Ponder (geometry: tools/jungle_mobs.py). A slow, heavy crawl - one foot lifted and planted at a time,
 * the great body rolling and sinking onto each step, the garden on its back swaying. It breathes through its
 * throat sac, blinks its big eyes and turns its head to watch you. animA croaks (the sac balloons, the jaw drops);
 * animB is a footfall (the body sinks and the ferns shake); animC lays an egg; animD roars (rearing, mouth wide).
 * Rampaging (flagA) it hunches forward with its mouth half open.
 */
public class ColossusPonderModel extends EntityModel<JungleRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] lids = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final ModelPart[] ferns = new ModelPart[4];
    private final ModelPart[] vines = new ModelPart[3];

    public ColossusPonderModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.throat = this.jaw.getChild("throat");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.eyes[i] = this.head.getChild(s + "_eye");
            this.lids[i] = this.eyes[i].getChild(s + "_eyelid");
            this.arms[i] = this.body.getChild(s + "_arm");
            this.hands[i] = this.arms[i].getChild(s + "_hand");
            this.legs[i] = this.body.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
        }
        for (int i = 0; i < 4; i++) {
            this.ferns[i] = this.body.getChild("fern_" + i);
        }
        for (int i = 0; i < 3; i++) {
            this.vines[i] = this.body.getChild("vine_" + i);
        }
    }

    @Override
    public void setupAnim(JungleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        float pos = s.walkAnimationPos * 0.9F;
        float sw = Mth.sin(pos);
        float rage = s.flagA ? 1.0F : 0.0F;
        // ---- the crawl: diagonal pairs, the body rolling onto each planted foot
        for (int i = 0; i < 2; i++) {
            float a = i == 0 ? sw : -sw;
            this.arms[i].xRot -= a * 0.45F * walk;
            this.hands[i].xRot += a * 0.3F * walk;
            this.legs[i].xRot -= -a * 0.35F * walk;
            this.shins[i].xRot += Math.max(0.0F, a) * 0.3F * walk;
            this.feet[i].xRot += -a * 0.2F * walk;
        }
        this.body.zRot += sw * 0.05F * walk;
        this.body.y += Math.abs(Mth.cos(pos)) * 0.9F * walk;
        // breathing: the throat sac pulses, the body swells a touch
        float breathe = Mth.sin(age * 0.07F);
        this.throat.yScale = 1.0F + 0.12F * breathe;
        this.throat.xScale = 1.0F + 0.06F * breathe;
        this.body.yScale = 1.0F + 0.01F * breathe;
        this.head.yRot += s.yRot * Anim.DEG * 0.35F;
        this.head.xRot += s.xRot * Anim.DEG * 0.2F + 0.12F * rage;
        this.jaw.xRot += 0.18F * rage;
        // the garden sways (more with every step)
        for (int i = 0; i < 4; i++) {
            this.ferns[i].zRot += Mth.sin(age * 0.06F + i * 1.3F) * 0.06F + sw * 0.08F * walk;
            this.ferns[i].xRot += Mth.sin(age * 0.05F + i) * 0.05F;
        }
        for (int i = 0; i < 3; i++) {
            this.vines[i].xRot += Mth.sin(age * 0.05F + i * 2.0F) * 0.12F + sw * 0.1F * walk;
        }
        // blinking: every few seconds, both lids close for a moment
        float blinkT = (age + s.seed * 13.0F) % 110.0F;
        boolean blink = blinkT < 5.0F;
        for (int i = 0; i < 2; i++) {
            this.lids[i].visible = blink;
            this.eyes[i].yRot += Mth.sin(age * 0.03F + i) * 0.05F;
        }

        // ---- croaking: the sac balloons, the jaw drops a little
        float cr = Anim.seconds(s.animA, s.ageInTicks);
        if (cr >= 0.0F && cr < 1.6F) {
            float e = Anim.envelope(cr, 0.0F, 0.25F, 0.6F, 0.6F);
            float throb = 1.0F + Mth.sin(cr * 22.0F) * 0.08F;
            this.throat.xScale = 1.0F + 0.7F * e * throb;
            this.throat.yScale = 1.0F + 1.4F * e * throb;
            this.throat.zScale = 1.0F + 0.5F * e;
            this.jaw.xRot += 0.08F * e;
            this.head.xRot -= 0.06F * e;
        }
        // ---- a footfall: it sinks onto the foot, the garden shakes
        float sp = Anim.seconds(s.animB, s.ageInTicks);
        if (sp >= 0.0F && sp < 0.5F) {
            float e = Anim.envelope(sp, 0.0F, 0.05F, 0.05F, 0.35F);
            this.body.y += 0.8F * e;
            for (int i = 0; i < 4; i++) {
                this.ferns[i].zRot += Mth.sin(sp * 40.0F + i) * 0.2F * e;
            }
        }
        // ---- laying: it squats and its hind legs splay
        float ly = Anim.seconds(s.animC, s.ageInTicks);
        if (ly >= 0.0F && ly < 1.5F) {
            float e = Anim.envelope(ly, 0.0F, 0.3F, 0.6F, 0.5F);
            this.body.y += 1.5F * e;
            this.body.xRot -= 0.12F * e;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.legs[i].zRot -= sx * 0.25F * e;
                this.lids[i].visible = e > 0.5F || this.lids[i].visible;
            }
        }
        // ---- the roar (and every slam): rearing back, mouth gaping
        float ro = Anim.seconds(s.animD, s.ageInTicks);
        if (ro >= 0.0F && ro < 1.4F) {
            float e = Anim.envelope(ro, 0.0F, 0.2F, 0.7F, 0.4F);
            this.body.xRot -= 0.3F * e;
            this.head.xRot -= 0.35F * e;
            this.jaw.xRot += 0.8F * e + Mth.sin(ro * 30.0F) * 0.05F * e;
            this.throat.yScale += 0.6F * e;
            for (int i = 0; i < 2; i++) {
                this.arms[i].xRot -= 0.4F * e;
            }
        }
    }
}
