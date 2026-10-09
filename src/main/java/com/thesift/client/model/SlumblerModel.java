package com.thesift.client.model;

import com.thesift.client.Expression;
import com.thesift.client.renderer.state.SlumblerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * S2: the Slumbler (geometry and hand-painted texture in tools/slumbler.py), a big sleepy amphibian
 * sitting up like a toad. Every pose blends in and out (sleep and swimming are smoothed amounts from
 * the entity, the one-shot moves use envelopes), so nothing snaps.
 *
 * <ul>
 *   <li>always: slow breaths swell the chest and the throat pouch, the frilled gills rise and fall
 *   with them and drift, the heavy lids sit half down</li>
 *   <li>sleep: it settles forward onto its folded hands, haunches spread, tail curled closer, eyes
 *   shut, gills drooping, breathing slower and deeper; afloat in Chrome it sinks to the chin</li>
 *   <li>wake: it yawns hugely (the yawn also plays now and then while awake)</li>
 *   <li>walk: a heavy waddle tied to limbSwing - diagonal limbs together, hands and feet lifting, the
 *   body rolling and bobbing, the tail swinging a beat behind</li>
 *   <li>swim: the chest levels out, arms and feet sweep back, the tail straightens and drives, gills
 *   stream back</li>
 *   <li>spit: it rears back, cheeks puffing and throat swelling, then whips forward, jaw open</li>
 *   <li>eat (gulp), bite, hum, nuzzle, wet shake, laying eggs, a flinch when hurt, a slump on death</li>
 * </ul>
 * The Python mirror used for the animation strips is kept in step by hand.
 */
public class SlumblerModel extends EntityModel<SlumblerRenderState> {
    private static final String[] SIDES = {"left", "right"};
    /** The tail's resting curl (tools/slumbler.py), straightened out while it swims. */
    private static final float[] CURL = {0.25F, 0.35F, 0.4F};
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart[] tail = new ModelPart[3];
    private final ModelPart[] cheeks = new ModelPart[2];
    private final ModelPart[] lids = new ModelPart[2];
    private final ModelPart[][] gills = new ModelPart[2][3];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public SlumblerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.chest = this.body.getChild("chest");
        this.head = this.chest.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.throat = this.jaw.getChild("throat");
        this.tail[0] = this.body.getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail3");
        for (int k = 0; k < 2; k++) {
            String side = SIDES[k];
            this.cheeks[k] = this.head.getChild(side + "_cheek");
            this.lids[k] = this.head.getChild(side + "_eye").getChild(side + "_eyelid");
            for (int i = 0; i < 3; i++) {
                this.gills[k][i] = this.head.getChild(side + "_gill_" + i);
            }
            this.arms[k] = this.chest.getChild(side + "_arm");
            this.forearms[k] = this.arms[k].getChild(side + "_forearm");
            this.hands[k] = this.forearms[k].getChild(side + "_hand");
            this.legs[k] = this.body.getChild(side + "_leg");
            this.feet[k] = this.legs[k].getChild(side + "_foot");
        }
    }

    @Override
    public void setupAnim(SlumblerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float sleep = s.sleep;
        float awake = 1.0F - sleep;
        float swim = s.swim * awake;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F) * awake * (1.0F - swim);
        float c = s.walkAnimationPos * 0.6662F;
        float sw = Mth.sin(c);
        float cw = Mth.cos(c);
        boolean angry = s.expression == Expression.ANGRY;

        // --- breathing: slow and deep asleep; the throat pouch swells on every breath
        float br = Mth.sin(age * Mth.lerp(sleep, 0.1F, 0.05F));
        float swell = 1.0F + 0.012F * br * (1.0F + sleep);
        this.chest.xScale = swell;
        this.chest.zScale = swell;
        this.throat.yScale = 1.0F + 0.18F * Math.max(0.0F, br) * (1.0F + sleep);
        float gillFlare = 0.1F * br * (0.6F + 0.6F * sleep);
        float lid = Mth.lerp(sleep, angry ? 0.5F : 0.8F, 1.5F);
        if (s.expression == Expression.BLINK) {
            lid = 1.5F;
        }

        // --- sleep: settles forward, chin over its folded hands, haunches spread, tail curled closer
        this.chest.xRot += 0.5F * sleep + br * 0.015F * sleep;
        this.head.xRot -= 0.38F * sleep;
        this.body.y += 1.2F * sleep + (s.inChrome ? 2.5F * sleep : 0.0F);
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.arms[k].xRot -= 0.25F * sleep;
            this.arms[k].zRot -= sx * 0.2F * sleep;
            this.forearms[k].xRot -= 0.75F * sleep;
            this.hands[k].xRot += 0.5F * sleep;
            this.legs[k].zRot -= sx * 0.12F * sleep;
            this.feet[k].zRot += sx * 0.12F * sleep;
        }
        for (int i = 0; i < 3; i++) {
            this.tail[i].yRot += 0.12F * sleep;
        }

        // --- the waddle: diagonal limbs together, the heavy body rolling and bobbing, the tail swinging behind
        for (int k = 0; k < 2; k++) {
            float ph = k == 0 ? sw : -sw;
            this.arms[k].xRot -= ph * 0.5F * walk;
            this.forearms[k].xRot += Math.max(0.0F, ph) * 0.45F * walk;
            this.hands[k].xRot -= Math.max(0.0F, ph) * 0.3F * walk;
            this.legs[k].xRot += ph * 0.3F * walk;
            this.feet[k].xRot += Math.max(0.0F, -ph) * 0.35F * walk;
            this.feet[k].y -= Math.max(0.0F, -ph) * 1.2F * walk;
        }
        this.body.zRot += cw * 0.07F * walk;
        this.body.yRot += sw * 0.05F * walk;
        this.body.y -= Math.abs(cw) * 0.8F * walk;
        this.head.zRot -= cw * 0.05F * walk;
        this.head.xRot += Math.abs(sw) * 0.05F * walk;
        for (int i = 0; i < 3; i++) {
            this.tail[i].yRot -= Mth.sin(c - 0.7F * (i + 1)) * (0.12F + 0.06F * i) * walk;
            this.tail[i].yRot += Mth.sin(age * 0.04F - i * 0.6F) * 0.05F * awake;
        }

        // --- swimming: the body levels out, limbs sweep back, the tail straightens and drives
        float t = age * 0.2F + s.walkAnimationPos * 0.3F;
        this.chest.xRot += 0.5F * swim;
        this.head.xRot -= 0.45F * swim;
        this.body.yRot += Mth.sin(t) * 0.07F * swim;
        for (int k = 0; k < 2; k++) {
            this.arms[k].xRot += (0.95F + Mth.sin(t + k * Mth.PI) * 0.25F) * swim;
            this.forearms[k].xRot += 0.5F * swim;
            this.legs[k].xRot += 0.35F * swim;
            this.feet[k].xRot += (-0.8F + Mth.sin(t + 1.0F + k * Mth.PI) * 0.2F) * swim;
        }
        for (int i = 0; i < 3; i++) {
            this.tail[i].yRot += (Mth.sin(t - (i + 1) * 0.9F) * (0.25F + 0.1F * i) - CURL[i]) * swim;
        }
        float gillSweep = 0.45F * swim;

        // --- head look, clamped, never while asleep
        this.head.yRot += Mth.clamp(s.yRot, -35.0F, 35.0F) * Anim.DEG * 0.8F * awake;
        this.head.xRot += Mth.clamp(s.xRot, -25.0F, 25.0F) * Anim.DEG * 0.5F * awake;

        float jawOpen = 0.0F;
        float cheek = 0.0F;
        // --- yawn (2.4 s, also on waking): head back, jaw wide, arms reaching, gills flared, eyes squeezed
        float yawn = Anim.seconds(s.yawn, s.ageInTicks);
        if (yawn >= 0.0F && yawn < 2.5F) {
            float o = Anim.envelope(yawn, 0.1F, 0.7F, 0.6F, 0.8F);
            jawOpen = Math.max(jawOpen, 0.85F * o);
            this.head.xRot -= 0.45F * o;
            this.chest.xRot -= 0.08F * o;
            this.throat.yScale += 0.35F * o;
            gillFlare += 0.35F * o;
            lid = Mth.lerp(o, lid, 1.4F);
            for (int k = 0; k < 2; k++) {
                this.arms[k].xRot -= 0.2F * o * awake;
            }
        }
        // --- spit (1.1 s, the gob leaves at 0.6 s): rear back, cheeks puff, throat swells ... whip forward
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        if (spit >= 0.0F && spit < 1.2F) {
            float draw = Anim.envelope(spit, 0.0F, 0.45F, 0.1F, 0.08F);
            float whip = Anim.envelope(spit, 0.55F, 0.07F, 0.1F, 0.35F);
            jawOpen = Math.max(jawOpen, Anim.envelope(spit, 0.55F, 0.05F, 0.12F, 0.25F) * 0.7F);
            this.head.xRot += -draw * 0.35F + whip * 0.3F;
            this.chest.xRot += -draw * 0.12F + whip * 0.1F;
            cheek = Math.max(cheek, draw);
            this.throat.yScale += 0.9F * draw;
            gillFlare += 0.4F * draw;
        }
        // --- eat (2.2 s): the head dips, the jaw scoops, a big swallow down the throat
        float gulp = Anim.seconds(s.gulp, s.ageInTicks);
        if (gulp >= 0.0F && gulp < 2.3F) {
            float dip = Anim.envelope(gulp, 0.0F, 0.4F, 0.9F, 0.6F);
            float swallow = Anim.envelope(gulp, 1.15F, 0.12F, 0.1F, 0.4F);
            jawOpen = Math.max(jawOpen, Anim.envelope(gulp, 0.3F, 0.2F, 0.3F, 0.15F) * 0.6F);
            this.chest.xRot += 0.3F * dip;
            this.head.xRot += 0.25F * dip;
            this.throat.yScale += 0.8F * swallow;
            gillFlare += 0.3F * swallow;
        }
        // --- bite (0.45 s): a lunge and a snap
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.5F) {
            float o = Anim.envelope(bite, 0.0F, 0.12F, 0.05F, 0.15F);
            jawOpen = Math.max(jawOpen, 0.75F * o);
            this.chest.xRot += 0.15F * o;
            this.head.xRot -= 0.1F * o;
        }
        // --- humming along to music (1.6 s): the head sways, the jaw hums, the throat trembles with the note
        float hum = Anim.seconds(s.hum, s.ageInTicks);
        if (hum >= 0.0F && hum < 1.6F) {
            float e = Anim.envelope(hum, 0.0F, 0.2F, 0.8F, 0.5F);
            jawOpen = Math.max(jawOpen, 0.25F * e + Mth.sin(hum * 12.0F) * 0.04F * e);
            this.head.zRot += Mth.sin(hum * 3.5F) * 0.12F * e;
            this.throat.yScale += 0.3F * e * (0.5F + 0.5F * Mth.sin(hum * 20.0F));
            gillFlare += 0.3F * e;
        }
        // --- nuzzle (2 s): a slow sideways rub of the snout
        float nuzzle = Anim.seconds(s.nuzzle, s.ageInTicks);
        if (nuzzle >= 0.0F && nuzzle < 2.0F) {
            float e = Anim.envelope(nuzzle, 0.0F, 0.3F, 1.2F, 0.4F);
            this.head.yRot += Mth.sin(nuzzle * 4.0F) * 0.3F * e;
            this.head.zRot += Mth.sin(nuzzle * 4.0F) * 0.12F * e;
        }
        // --- the wet shake after climbing out of the Chrome (1.2 s)
        float shake = Anim.seconds(s.shake, s.ageInTicks);
        if (shake >= 0.0F && shake < 1.2F) {
            float e = Anim.envelope(shake, 0.0F, 0.08F, 0.5F, 0.6F);
            float w = Mth.sin(shake * 34.0F) * e;
            this.body.zRot += w * 0.12F;
            this.head.zRot -= w * 0.25F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot += Mth.sin(shake * 34.0F - (i + 1) * 0.8F) * 0.3F * e;
            }
            gillFlare += w * 0.4F;
        }
        // --- laying eggs (2.5 s): it settles low, the tail lifts and quivers
        float lay = Anim.seconds(s.lay, s.ageInTicks);
        if (lay >= 0.0F && lay < 2.5F) {
            float low = Anim.envelope(lay, 0.0F, 0.4F, 1.4F, 0.6F);
            float quiver = Mth.sin(lay * 30.0F) * Anim.envelope(lay, 0.5F, 0.2F, 0.9F, 0.3F);
            this.body.y += low * 1.5F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].xRot += low * 0.12F;
                this.tail[i].yRot += quiver * 0.06F * (i + 1);
            }
        }
        // --- hurt: a flinch that eases out over the red flash (no snapping)
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float h = Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F);
            float k = Mth.sin(h * Mth.PI) * (1.0F - 0.4F * h);
            this.head.xRot -= 0.3F * k;
            this.chest.xRot -= 0.1F * k;
            jawOpen = Math.max(jawOpen, 0.35F * k);
            gillFlare += 0.5F * k;
            lid = Mth.lerp(k, lid, 1.4F);
        }
        // --- death: it slumps forward onto its side, jaw slack, gills drooping (the renderer then pops it)
        float roll = Anim.smooth(s.dying / 14.0F);
        if (roll > 0.0F) {
            this.chest.xRot += 0.5F * roll;
            this.head.xRot -= 0.2F * roll;
            this.body.zRot += 0.3F * roll;
            jawOpen = Math.max(jawOpen, 0.45F * roll);
            lid = Mth.lerp(roll, lid, 1.5F);
            this.arms[0].zRot += 0.35F * roll;
            this.arms[1].zRot -= 0.35F * roll;
            gillFlare -= 0.4F * roll;
        }

        this.jaw.xRot += jawOpen;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.cheeks[k].xScale = 1.0F + 0.9F * cheek;
            this.cheeks[k].zScale = 1.0F + 0.25F * cheek;
            this.lids[k].yScale = lid;
            this.lids[k].zRot = angry ? -sx * 0.3F * awake : 0.0F;
            for (int i = 0; i < 3; i++) {
                ModelPart g = this.gills[k][i];
                g.yRot += sx * (gillFlare + Mth.sin(age * 0.11F + i * 1.3F) * 0.05F - gillSweep);
                g.zRot += sx * (Mth.sin(age * 0.13F + i * 1.1F + k) * 0.06F + 0.28F * sleep + 0.35F * roll);
                g.xRot += Mth.sin(age * 0.17F + i) * 0.04F;
            }
        }
    }
}
