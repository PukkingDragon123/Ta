package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.DictatorRenderState;
import com.thesift.entity.boss.Dictator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Conductor (C3 remodel: tools/conductor.py): gaunt and stooped in a tattered tailcoat, a
 * cracked porcelain mask, four horn-ears, long clawed fingers and a black baton.
 *
 * <ul>
 *   <li>idle: conducts - the baton traces a slow figure of eight, the left hand keeps time with
 *   its claws, the head tilts to the music, the horn-ears twitch, coat tails and tendrils sway a
 *   beat behind the body</li>
 *   <li>move: long, stooped, loping strides, arms trailing and the tails flicking after them</li>
 *   <li>attack: a backhand slash - wound up across the body, cut, and carried through</li>
 *   <li>summon: crouches, both arms rise, the baton sweeps a wide arc while the left claw opens
 *   and drags upward - the band climbs out of the stage - then a sharp downbeat</li>
 *   <li>blink: folds down into himself and vanishes</li>
 *   <li>rebuilt: only the Mask lies on the floor; it rises as his body forms beneath it</li>
 *   <li>lunge: baton drawn back, weight low, then a full-length thrust</li>
 *   <li>levitating: legs hang, toes pointed, threads of song in his hands; flicks the baton at
 *   each barrage, raises both hands and brings them down for a chord</li>
 *   <li>crescendo: arms and baton thrown high, head back</li>
 *   <li>roar (new movement): doubles over then rears up, the horn-ears flaring</li>
 *   <li>the last transformation: he doubles over, convulsing, arms clutching himself, as he swells
 *   (the renderer scales him) until the colossus bursts out (DictatorKaijuModel)</li>
 *   <li>hurt: head snaps aside, shoulders hunch</li>
 *   <li>death: drops to his knees, slumps forward and crumbles away</li>
 * </ul>
 */
public class DictatorModel extends EntityModel<DictatorRenderState> {
    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart mask;
    private final ModelPart crown;
    private final ModelPart growth;
    private final ModelPart baton;
    /** Upper left, upper right, lower left, lower right; and their tips. */
    private final ModelPart[] ears = new ModelPart[4];
    private final ModelPart[] earTips = new ModelPart[4];
    private final ModelPart[] tails = new ModelPart[2];
    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[][] fingers = new ModelPart[2][3];
    private final ModelPart[] thumbs = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final ModelPart[] strings = new ModelPart[2];

    public DictatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.torso = this.body.getChild("torso");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.mask = this.head.getChild("mask");
        this.crown = this.head.getChild("crown");
        this.growth = this.torso.getChild("back_growth");
        String[] sides = {"left", "right"};
        String[] tiers = {"upper", "lower"};
        for (int i = 0; i < 4; i++) {
            String name = sides[i % 2] + "_ear_" + tiers[i / 2];
            this.ears[i] = this.head.getChild(name);
            this.earTips[i] = this.ears[i].getChild(name + "_tip");
        }
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.tails[i] = this.torso.getChild(s + "_tail");
            this.tendrils[i] = this.growth.getChild(s + "_tendril");
            this.arms[i] = this.torso.getChild(s + "_arm");
            this.forearms[i] = this.arms[i].getChild(s + "_forearm");
            this.hands[i] = this.forearms[i].getChild(s + "_hand");
            for (int k = 0; k < 3; k++) {
                this.fingers[i][k] = this.hands[i].getChild(s + "_finger_" + k);
            }
            this.thumbs[i] = this.hands[i].getChild(s + "_thumb");
            this.strings[i] = this.hands[i].getChild(s + "_strings");
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
        }
        this.baton = this.hands[1].getChild("baton");
    }

    /** Curls (positive) or splays (negative) a hand's claws. */
    private void claws(int side, float curl) {
        for (int k = 0; k < 3; k++) {
            this.fingers[side][k].xRot -= curl * (0.9F + k * 0.15F);
            this.fingers[side][k].zRot *= 1.0F + Math.max(0.0F, -curl) * 2.5F;
        }
        this.thumbs[side].xRot -= curl * 0.6F;
    }

    @Override
    public void setupAnim(DictatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.2F);
        float pos = s.walkAnimationPos * 0.45F;
        float cw = Mth.cos(pos);
        float sw = Mth.sin(pos);
        float rest = 1.0F - walk;
        int act = s.action;
        float at = s.actionTime;

        // --- long stooped strides: the body dips on each footfall, the tails flick a beat late
        this.legs[0].xRot += cw * 0.7F * walk;
        this.legs[1].xRot -= cw * 0.7F * walk;
        this.shins[0].xRot += Math.max(0.0F, -sw) * 0.8F * walk;
        this.shins[1].xRot += Math.max(0.0F, sw) * 0.8F * walk;
        this.feet[0].xRot -= Math.max(0.0F, sw) * 0.3F * walk;
        this.feet[1].xRot -= Math.max(0.0F, -sw) * 0.3F * walk;
        this.body.y -= Math.abs(cw) * 1.2F * walk;
        this.torso.xRot += 0.22F * walk + Mth.sin(age * 0.05F) * 0.025F;
        this.torso.yRot += sw * 0.08F * walk;
        float lag = Mth.cos(pos - 0.9F);
        for (int i = 0; i < 2; i++) {
            float sgn = i == 0 ? 1.0F : -1.0F;
            this.tails[i].xRot += 0.2F * walk + Math.abs(lag) * 0.25F * walk + Mth.sin(age * 0.09F + i * 1.3F) * 0.06F;
            this.tails[i].zRot += sgn * Mth.sin(age * 0.07F + i) * 0.04F;
            this.tendrils[i].xRot += Mth.sin(age * 0.11F + i * 2.0F) * 0.18F;
            this.tendrils[i].zRot += sgn * Mth.sin(age * 0.08F + i) * 0.12F;
        }

        // --- head follows you, tilting to the music
        this.head.yRot = s.yRot * Anim.DEG * 0.8F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F;
        this.head.zRot = Mth.sin(age * 0.045F) * 0.12F * rest;
        this.crown.yRot = Mth.sin(age * 0.03F) * 0.05F;

        // --- four horn-ears: each twitches on its own, all lay back when he is angry
        boolean angry = s.expression == com.thesift.client.Expression.ANGRY;
        for (int i = 0; i < 4; i++) {
            float sgn = i % 2 == 0 ? 1.0F : -1.0F;
            float twitch = Anim.envelope(Mth.positiveModulo(age + i * 23.0F, 70.0F + i * 9.0F), 0.0F, 1.5F, 1.0F, 3.0F);
            this.ears[i].zRot += sgn * (twitch * 0.25F + Mth.sin(age * 0.07F + i) * 0.03F);
            this.ears[i].xRot += angry ? 0.45F : 0.0F;
            this.earTips[i].zRot += sgn * twitch * 0.2F;
        }

        // --- conducting: the baton traces a figure of eight, the left hand keeps the beat
        float beat = age * 0.12F;
        float down = Math.max(0.0F, Mth.sin(beat * 2.0F));
        this.arms[1].xRot = -0.95F * rest + Mth.sin(beat) * 0.35F * rest + cw * 0.5F * walk;
        this.arms[1].zRot += Mth.sin(beat * 2.0F) * 0.25F * rest;
        this.forearms[1].xRot += -0.5F * rest + Mth.cos(beat) * 0.2F * rest;
        this.hands[1].zRot = Mth.sin(beat + 0.6F) * 0.3F * rest;
        this.baton.xRot += Mth.sin(beat * 2.0F + 0.4F) * 0.12F * rest;
        this.arms[0].xRot = -0.4F * rest + down * 0.3F * rest - cw * 0.5F * walk;
        this.forearms[0].xRot += -0.45F * rest;
        this.claws(0, rest * (0.15F + down * 0.5F));
        this.claws(1, 0.35F);

        // --- hurt: head snaps aside, shoulders hunch
        if (s.hasRedOverlay) {
            this.head.zRot += 0.45F;
            this.head.xRot += 0.2F;
            this.torso.xRot += 0.15F;
            this.mask.zRot += 0.08F;
        }

        float t;
        // --- slash (also each note volley): wound up across the body, cut, carried through
        if ((t = Anim.seconds(s.slash, age)) >= 0 && t < 0.6F) {
            float wind = Anim.envelope(t, 0.0F, 0.1F, 0.02F, 0.1F);
            float cut = Anim.envelope(t, 0.12F, 0.07F, 0.05F, 0.3F);
            this.arms[1].xRot += -1.7F * wind + 1.3F * cut;
            this.arms[1].zRot += 1.0F * wind - 1.1F * cut;
            this.torso.yRot += -0.45F * wind + 0.55F * cut;
            this.head.yRot -= 0.25F * cut;
            this.tails[0].zRot -= 0.3F * cut;
            this.tails[1].zRot -= 0.3F * cut;
        }
        // --- crescendo: arms and baton high, head thrown back
        if ((t = Anim.seconds(s.crescendo, age)) >= 0 && t < 3.6F) {
            float e = Anim.envelope(t, 0.0F, 0.5F, 2.4F, 0.6F);
            float shake = Mth.sin(age * 2.5F) * 0.05F * e;
            this.arms[0].xRot = Mth.lerp(e, this.arms[0].xRot, -2.8F) + shake;
            this.arms[1].xRot = Mth.lerp(e, this.arms[1].xRot, -2.9F) - shake;
            this.arms[0].zRot += -0.35F * e;
            this.arms[1].zRot += 0.35F * e;
            this.forearms[0].xRot *= 1.0F - e;
            this.forearms[1].xRot *= 1.0F - e;
            this.claws(0, -0.6F * e);
            this.head.xRot -= 0.6F * e;
            this.torso.xRot -= 0.2F * e;
        }
        // --- roar on entering a new movement
        if ((t = Anim.seconds(s.roar, age)) >= 0 && t < 2.0F) {
            float bow = Anim.envelope(t, 0.0F, 0.25F, 0.2F, 0.3F);
            float rear = Anim.envelope(t, 0.5F, 0.2F, 0.7F, 0.5F);
            this.torso.xRot += 0.7F * bow - 0.35F * rear;
            this.head.xRot += 0.4F * bow - 0.7F * rear;
            this.arms[0].zRot += -0.9F * rear;
            this.arms[1].zRot += 0.9F * rear;
            this.claws(0, -0.8F * rear);
            this.claws(1, -0.4F * rear);
            this.crown.yScale = 1.0F + 0.4F * rear;
            for (int i = 0; i < 4; i++) {
                this.ears[i].xRot -= 0.5F * rear;
            }
        }
        // --- blink: folds down into himself
        if ((t = Anim.seconds(s.blink, age)) >= 0 && t < 0.35F) {
            float e = 1.0F - Anim.smooth(t / 0.35F);
            this.body.yScale *= 0.4F + 0.6F * (1.0F - e);
        }
        // --- summon: the conducting gesture that calls his band up out of the stage
        float sum = Anim.seconds(s.summon, age) * 20.0F;
        if (sum >= 0.0F && sum < Dictator.SUMMON_TICKS) {
            float crouch = Anim.envelope(sum, 0.0F, 5.0F, 3.0F, 6.0F);
            float raise = Anim.envelope(sum, 3.0F, 9.0F, 6.0F, 10.0F);
            float sweep = Anim.smooth((sum - 6.0F) / 10.0F);
            float hit = Anim.envelope(sum, Dictator.SUMMON_AT - 1.0F, 1.5F, 3.0F, 8.0F);
            this.body.y += 1.6F * crouch;
            this.torso.xRot += 0.3F * crouch - 0.3F * raise;
            this.legs[0].xRot -= 0.35F * crouch;
            this.legs[1].xRot -= 0.35F * crouch;
            this.shins[0].xRot += 0.6F * crouch;
            this.shins[1].xRot += 0.6F * crouch;
            // the baton: up behind his head, then a wide arc across and down
            this.arms[1].xRot = Mth.lerp(raise, this.arms[1].xRot, -2.6F) + 1.2F * hit;
            this.arms[1].zRot += Mth.lerp(sweep, 0.9F, -0.7F) * raise;
            this.forearms[1].xRot *= 1.0F - raise;
            // the left claw opens wide and drags upward
            this.arms[0].xRot = Mth.lerp(raise, this.arms[0].xRot, -1.6F - 0.8F * sweep) + 0.6F * hit;
            this.arms[0].zRot -= 0.5F * raise;
            this.claws(0, Mth.lerp(sweep, -0.9F, 1.1F) * raise);
            this.head.xRot -= 0.5F * raise - 0.35F * hit;
            this.torso.xRot += 0.35F * hit;
        }

        // --- his sculk magic: threads of song once he rises
        float grow = s.transform >= 0.0F ? Anim.smooth((s.transform - 0.45F) / 0.2F) : 1.0F;
        for (int i = 0; i < 2; i++) {
            this.setTrait(this.strings[i], s.phase == 2 ? grow : 0.0F);
        }
        if (s.phase == 1) {
            if (act == Dictator.LUNGE_WINDUP) {
                float k = Anim.smooth(at / 8.0F);
                this.arms[1].xRot = Mth.lerp(k, this.arms[1].xRot, 0.9F);
                this.arms[1].zRot += 0.5F * k;
                this.torso.yRot -= 0.5F * k;
                this.body.y += 2.5F * k;
                this.legs[0].xRot -= 0.6F * k;
                this.legs[1].xRot += 0.5F * k;
                this.shins[0].xRot += 0.6F * k;
                this.shins[1].xRot += 0.4F * k;
                this.claws(0, -0.5F * k);
            } else if (act == Dictator.LUNGE) {
                float k = 1.0F - Anim.smooth((at - 6.0F) / 6.0F);
                this.arms[1].xRot = -1.65F * k + this.arms[1].xRot * (1.0F - k);
                this.forearms[1].xRot *= 1.0F - k;
                this.torso.xRot += 0.45F * k;
                this.torso.yRot += 0.35F * k;
                this.legs[0].xRot = -0.9F * k;
                this.legs[1].xRot = 0.7F * k;
                this.tails[0].xRot += 0.7F * k;
                this.tails[1].xRot += 0.6F * k;
            }
        } else if (s.phase == 2) {
            // off his feet: legs hang, toes pointed, a slow bob
            this.legs[0].xRot = 0.2F + Mth.sin(age * 0.07F) * 0.06F;
            this.legs[1].xRot = 0.35F + Mth.sin(age * 0.07F + 1.0F) * 0.06F;
            this.shins[0].xRot = 0.45F;
            this.shins[1].xRot = 0.55F;
            this.feet[0].xRot = 0.5F;
            this.feet[1].xRot = 0.5F;
            for (int i = 0; i < 2; i++) {
                this.tails[i].xRot += 0.2F + Mth.sin(age * 0.11F + i) * 0.08F;
            }
            this.body.y += Mth.sin(age * 0.05F) * 0.8F;
            for (int i = 0; i < 2; i++) {
                this.strings[i].zRot = Mth.sin(age * (0.2F + i * 0.05F)) * 0.08F;
                this.strings[i].xRot = -this.arms[i].xRot - this.forearms[i].xRot;
            }
            if (act == Dictator.BARRAGE) {
                float flick = 0.0F;
                for (int v = 10; v <= 34; v += 12) {
                    flick += Anim.envelope(at, v - 4.0F, 3.0F, 1.0F, 4.0F);
                }
                this.arms[1].xRot = Mth.lerp(Math.min(1.0F, flick), -0.6F, -1.7F);
                this.arms[1].zRot += 0.3F * flick;
                this.torso.yRot += 0.2F * flick;
            } else if (act == Dictator.CHORD) {
                float raise = Anim.envelope(at, 0.0F, 16.0F, 4.0F, 2.0F);
                float strike = Anim.envelope(at, 20.0F, 2.0F, 4.0F, 10.0F);
                this.arms[0].xRot = -2.7F * raise + 0.4F * strike;
                this.arms[1].xRot = -2.8F * raise + 0.4F * strike;
                this.arms[0].zRot -= 0.5F * raise + 0.9F * strike;
                this.arms[1].zRot += 0.5F * raise + 0.9F * strike;
                this.claws(0, -0.8F * raise + 1.0F * strike);
                this.head.xRot -= 0.4F * raise - 0.3F * strike;
                this.torso.xRot += 0.35F * strike;
            }
        }
        if (s.transform >= 0.0F && s.phase == 3) {
            // the last transformation: doubled over, convulsing, clutching himself as he swells
            float e = Anim.smooth(s.transform / 0.15F);
            float jolt = Mth.sin(age * 1.9F) * 0.12F * e + Mth.sin(age * 3.1F) * 0.06F * e;
            this.torso.xRot += 0.6F * e + jolt;
            this.head.xRot -= 0.5F * e - jolt;
            this.head.zRot += jolt;
            this.arms[0].xRot = Mth.lerp(e, this.arms[0].xRot, -0.9F) + jolt;
            this.arms[1].xRot = Mth.lerp(e, this.arms[1].xRot, -1.0F) - jolt;
            this.arms[0].zRot += 0.7F * e;
            this.arms[1].zRot -= 0.7F * e;
            this.claws(0, 1.0F * e);
            this.claws(1, 0.8F * e);
            this.mask.xRot -= 0.15F * e;
            for (int i = 0; i < 4; i++) {
                this.ears[i].zRot += (i % 2 == 0 ? 1.0F : -1.0F) * 0.4F * e;
            }
        } else if (s.transform >= 0.0F) {
            // arms flung wide, head back, a slow turn in the air
            float e = Anim.envelope(s.transform, 0.0F, 0.2F, 0.55F, 0.25F);
            this.arms[0].zRot += -1.6F * e;
            this.arms[1].zRot += 1.6F * e;
            this.arms[0].xRot = Mth.lerp(e, this.arms[0].xRot, -0.3F);
            this.arms[1].xRot = Mth.lerp(e, this.arms[1].xRot, -0.3F);
            this.claws(0, -0.8F * e);
            this.head.xRot -= 0.7F * e;
            this.torso.xRot -= 0.25F * e;
            this.torso.yRot += Mth.sin(age * 0.2F) * 0.2F * e;
            for (int i = 0; i < 2; i++) {
                this.tails[i].xRot += 0.6F * e + Mth.sin(age * 0.9F + i) * 0.15F * e;
            }
        }
        if (s.assemble >= 0.0F) {
            this.assemble(s.assemble, age);
        } else {
            this.torso.skipDraw = false;
            this.neck.skipDraw = false;
            this.body.skipDraw = false;
            // the model is shared: undo what a rebuilding Conductor hid
            for (ModelPart p : new ModelPart[]{this.arms[0], this.arms[1], this.legs[0], this.legs[1], this.tails[0], this.tails[1], this.crown, this.growth}) {
                p.visible = true;
            }
            for (ModelPart ear : this.ears) {
                ear.visible = true;
            }
        }

        // --- death: knees, slump, crumble
        if (s.dying > 0.0F) {
            float kneel = Anim.smooth(s.dying / 8.0F);
            float slump = Anim.smooth((s.dying - 6.0F) / 10.0F);
            this.body.y += 11.0F * kneel;
            this.legs[0].xRot = -1.4F * kneel;
            this.legs[1].xRot = -1.4F * kneel;
            this.shins[0].xRot = 1.9F * kneel;
            this.shins[1].xRot = 1.9F * kneel;
            this.torso.xRot += 0.9F * slump;
            this.head.xRot += 0.6F * slump;
            this.mask.xRot += 0.3F * slump;
            this.arms[0].xRot = Mth.lerp(slump, this.arms[0].xRot, 0.2F);
            this.arms[1].xRot = Mth.lerp(slump, this.arms[1].xRot, 0.2F);
            this.claws(0, 1.0F * slump);
            this.claws(1, 1.0F * slump);
            float crumble = Anim.smooth((s.dying - 12.0F) / 8.0F);
            this.body.xScale = 1.0F - crumble * 0.3F;
            this.body.zScale = 1.0F - crumble * 0.3F;
            this.body.yScale *= 1.0F - crumble * 0.45F;
        }
    }

    /**
     * His body rebuilding around the Mask: at first only the Mask, face up on the floor; it rises
     * as the rest forms under it - torso, arms, legs, coat - and last the crest and horn-ears.
     */
    private void assemble(float a, float age) {
        float lift = Anim.smooth((a - 0.12F) / 0.55F);
        this.body.y += 46.0F * (1.0F - lift);
        this.head.xRot = Mth.lerp(lift, -Mth.HALF_PI, this.head.xRot - 0.5F);
        this.head.yRot *= lift;
        this.head.zRot = Mth.sin(age * 0.3F) * 0.05F * lift;
        this.body.skipDraw = a < 0.25F;
        this.torso.skipDraw = a < 0.25F;
        this.neck.skipDraw = a < 0.3F;
        this.setTrait(this.growth, Anim.smooth((a - 0.3F) / 0.1F));
        for (int i = 0; i < 2; i++) {
            this.setTrait(this.arms[i], Anim.backOut(Anim.clamp01((a - 0.4F) / 0.15F)));
            this.setTrait(this.legs[i], Anim.backOut(Anim.clamp01((a - 0.52F) / 0.15F)));
            this.setTrait(this.strings[i], 0.0F);
            this.setTrait(this.tails[i], Anim.smooth((a - 0.64F) / 0.14F));
            // arms flung wide while the song pours in
            this.arms[i].zRot += (i == 0 ? -1.3F : 1.3F) * (1.0F - Anim.smooth((a - 0.85F) / 0.15F));
            this.arms[i].xRot = -0.3F;
        }
        float crown = Anim.backOut(Anim.clamp01((a - 0.8F) / 0.14F));
        this.setTrait(this.crown, crown);
        for (ModelPart ear : this.ears) {
            this.setTrait(ear, crown);
        }
        this.torso.xRot -= 0.3F * lift * (1.0F - Anim.smooth((a - 0.9F) / 0.1F));
    }

    private void setTrait(ModelPart part, float amount) {
        part.visible = amount > 0.02F;
        part.xScale = amount;
        part.yScale = amount;
        part.zScale = amount;
    }
}
