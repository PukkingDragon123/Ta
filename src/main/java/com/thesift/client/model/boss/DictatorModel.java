package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.DictatorRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Dictator.
 *
 * <ul>
 *   <li>idle: conducts - the baton traces a slow figure of eight, the other hand keeps time, the
 *   head tilts to the music, coat tails sway</li>
 *   <li>move: long, fast, loping strides, arms trailing</li>
 *   <li>attack: a vicious backhand slash with the baton</li>
 *   <li>blink: folds down into himself and vanishes</li>
 *   <li>summon: both arms sweep outward, calling up the orchestra</li>
 *   <li>crescendo: arms and baton thrown high, head back</li>
 *   <li>roar (new phase): doubles over then rears up, crown flaring</li>
 *   <li>hurt: head snaps aside, shoulders hunch</li>
 *   <li>death: drops to his knees, slumps forward and crumbles away</li>
 * </ul>
 */
public class DictatorModel extends EntityModel<DictatorRenderState> {
    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart crown;
    private final ModelPart coatTail;
    /** Four ears: upper left, upper right, lower left, lower right. */
    private final ModelPart[] ears = new ModelPart[4];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart shell;
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[] wingTips = new ModelPart[2];
    private final ModelPart[] strings = new ModelPart[2];

    public DictatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.torso = this.body.getChild("torso");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.crown = this.head.getChild("crown");
        this.coatTail = this.torso.getChild("coat_tail");
        this.ears[0] = this.head.getChild("left_ear_upper");
        this.ears[1] = this.head.getChild("right_ear_upper");
        this.ears[2] = this.head.getChild("left_ear_lower");
        this.ears[3] = this.head.getChild("right_ear_lower");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            this.arms[i] = this.torso.getChild(sides[i] + "_arm");
            this.forearms[i] = this.arms[i].getChild(sides[i] + "_forearm");
            this.legs[i] = root.getChild(sides[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(sides[i] + "_shin");
            this.wings[i] = this.torso.getChild(sides[i] + "_crane_wing");
            this.wingTips[i] = this.wings[i].getChild(sides[i] + "_crane_wing_tip");
            this.strings[i] = this.forearms[i].getChild(sides[i] + "_hand").getChild(sides[i] + "_strings");
        }
        this.shell = this.torso.getChild("turtle_shell");
    }

    @Override
    public void setupAnim(DictatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.2F);
        float pos = s.walkAnimationPos * 0.5F;
        float cw = Mth.cos(pos);
        float sw = Mth.sin(pos);
        float rest = 1.0F - walk;

        // --- long loping strides
        this.legs[0].xRot += cw * 0.75F * walk;
        this.legs[1].xRot -= cw * 0.75F * walk;
        this.shins[0].xRot += Math.max(0.0F, -sw) * 0.7F * walk;
        this.shins[1].xRot += Math.max(0.0F, sw) * 0.7F * walk;
        this.body.y -= Math.abs(cw) * 1.4F * walk;
        this.torso.xRot += 0.18F * walk + Mth.sin(age * 0.05F) * 0.03F;
        this.coatTail.xRot += 0.25F * walk + Mth.sin(age * 0.09F) * 0.06F + Math.abs(cw) * 0.2F * walk;

        // --- head follows you, tilting to the music
        this.head.yRot = s.yRot * Anim.DEG * 0.8F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F;
        this.head.zRot = Mth.sin(age * 0.045F) * 0.12F * rest;
        this.crown.yRot = Mth.sin(age * 0.03F) * 0.05F;

        // --- four ears: each twitches on its own, all flatten back when he is angry
        boolean angry = s.expression == com.thesift.client.Expression.ANGRY;
        for (int i = 0; i < 4; i++) {
            float sgn = i % 2 == 0 ? 1.0F : -1.0F;
            float twitch = Anim.envelope(Mth.positiveModulo(age + i * 23.0F, 70.0F + i * 9.0F), 0.0F, 1.5F, 1.0F, 3.0F);
            this.ears[i].zRot += sgn * (twitch * 0.35F + Mth.sin(age * 0.07F + i) * 0.04F);
            this.ears[i].yRot += sgn * (angry ? 0.55F : 0.0F);
        }

        // --- conducting: the baton traces a figure of eight, the left hand keeps the beat
        float beat = age * 0.12F;
        this.arms[1].xRot = -0.9F * rest + Mth.sin(beat) * 0.35F * rest + cw * 0.5F * walk;
        this.arms[1].zRot += Mth.sin(beat * 2.0F) * 0.25F * rest;
        this.forearms[1].xRot += -0.5F * rest + Mth.cos(beat) * 0.2F * rest;
        this.arms[0].xRot = -0.35F * rest + Math.max(0.0F, Mth.sin(beat * 2.0F)) * 0.3F * rest - cw * 0.5F * walk;
        this.forearms[0].xRot += -0.4F * rest;

        // --- hurt: head snaps aside, shoulders hunch
        if (s.hasRedOverlay) {
            this.head.zRot += 0.45F;
            this.head.xRot += 0.2F;
            this.torso.xRot += 0.15F;
        }

        float t;
        // --- slash
        if ((t = Anim.seconds(s.slash, age)) >= 0 && t < 0.5F) {
            float wind = Anim.envelope(t, 0.0F, 0.08F, 0.02F, 0.1F);
            float cut = Anim.envelope(t, 0.1F, 0.08F, 0.05F, 0.25F);
            this.arms[1].xRot += -1.6F * wind + 1.2F * cut;
            this.arms[1].zRot += 0.9F * wind - 1.0F * cut;
            this.torso.yRot += -0.4F * wind + 0.5F * cut;
        }
        // --- summon: both arms sweep outward
        if ((t = Anim.seconds(s.summon, age)) >= 0 && t < 1.4F) {
            float e = Anim.envelope(t, 0.0F, 0.35F, 0.6F, 0.4F);
            this.arms[0].zRot += -1.5F * e;
            this.arms[1].zRot += 1.5F * e;
            this.arms[0].xRot -= 0.5F * e;
            this.arms[1].xRot -= 0.5F * e;
            this.head.xRot -= 0.3F * e;
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
            this.head.xRot -= 0.6F * e;
            this.torso.xRot -= 0.2F * e;
        }
        // --- roar on entering a new phase
        if ((t = Anim.seconds(s.roar, age)) >= 0 && t < 2.0F) {
            float bow = Anim.envelope(t, 0.0F, 0.25F, 0.2F, 0.3F);
            float rear = Anim.envelope(t, 0.5F, 0.2F, 0.7F, 0.5F);
            this.torso.xRot += 0.7F * bow - 0.35F * rear;
            this.head.xRot += 0.4F * bow - 0.7F * rear;
            this.arms[0].zRot += -0.9F * rear;
            this.arms[1].zRot += 0.9F * rear;
            this.crown.xScale = 1.0F + 0.3F * rear;
            this.crown.zScale = 1.0F + 0.3F * rear;
            this.crown.yScale = 1.0F + 0.4F * rear;
            for (ModelPart ear : this.ears) {
                ear.xRot -= 0.5F * rear;
            }
        }
        // --- blink: folds down into himself
        if ((t = Anim.seconds(s.blink, age)) >= 0 && t < 0.35F) {
            float e = 1.0F - Anim.smooth(t / 0.35F);
            this.body.yScale *= 0.4F + 0.6F * (1.0F - e);
        }

        // --- what he has stolen: each part only in its movement, growing in as he transforms
        float grow = s.transform >= 0.0F ? Anim.smooth((s.transform - 0.45F) / 0.2F) : 1.0F;
        float fade = s.transform >= 0.0F ? 1.0F - Anim.smooth((s.transform - 0.3F) / 0.2F) : 0.0F;
        this.setTrait(this.shell, s.phase == 1 ? grow : s.phase == 2 ? fade : 0.0F);
        for (int i = 0; i < 2; i++) {
            this.setTrait(this.wings[i], s.phase == 2 ? grow : s.phase == 3 ? fade : 0.0F);
            this.setTrait(this.strings[i], s.phase == 3 ? grow : 0.0F);
        }
        if (s.phase == 1) {
            this.shell.xRot += Mth.sin(age * 0.08F) * 0.02F;
        }
        if (s.phase == 2 && this.wings[0].visible) {
            // the Whistler's wing beat; legs trail behind while he flies
            boolean lock = s.action == com.thesift.entity.boss.Dictator.BEAM_LOCK;
            boolean dive = s.action == com.thesift.entity.boss.Dictator.DIVE && s.actionTime > 18.0F;
            float a = dive ? -1.0F : lock ? 0.5F + Mth.sin(age * 0.12F) * 0.08F : Mth.sin(age * 0.25F) * 0.75F;
            float lag = dive ? -1.0F : lock ? a : Mth.sin((age - 3.0F) * 0.25F) * 0.75F;
            this.wings[0].zRot = -a;
            this.wings[1].zRot = a;
            this.wings[0].yRot = -0.15F;
            this.wings[1].yRot = 0.15F;
            this.wingTips[0].zRot = -(lag - a) * 0.9F;
            this.wingTips[1].zRot = (lag - a) * 0.9F;
            this.legs[0].xRot = 0.6F + Mth.sin(age * 0.1F) * 0.05F;
            this.legs[1].xRot = 0.7F + Mth.sin(age * 0.1F + 1.0F) * 0.05F;
            this.shins[0].xRot = 0.5F;
            this.shins[1].xRot = 0.5F;
            this.body.y += Mth.sin(a) * 1.2F;
            if (lock) {
                this.head.xRot += 0.2F;
                this.arms[1].xRot = -1.5F;
            }
        }
        if (s.phase == 3) {
            for (int i = 0; i < 2; i++) {
                this.strings[i].zRot = Mth.sin(age * (3.0F + i * 0.3F)) * 0.04F;
                this.strings[i].xRot = -this.arms[i].xRot - this.forearms[i].xRot;
            }
            if (s.action == com.thesift.entity.boss.Dictator.STRUM) {
                this.arms[0].xRot = -1.0F + Mth.sin(age * 1.4F) * 0.3F;
                this.arms[1].xRot = -1.0F + Mth.sin(age * 1.4F + Mth.PI) * 0.3F;
                this.head.zRot = Mth.sin(age * 0.6F) * 0.25F;
            } else if (s.action == com.thesift.entity.boss.Dictator.SNAP) {
                float draw = Anim.envelope(s.actionTime, 0.0F, 12.0F, 1.0F, 1.0F);
                float snap = Anim.envelope(s.actionTime, 13.0F, 1.0F, 2.0F, 8.0F);
                this.arms[0].xRot += -0.4F * draw - 1.4F * snap;
                this.torso.yRot += -0.4F * draw + 0.3F * snap;
            }
        }
        if (s.phase == 1 && (s.action == com.thesift.entity.boss.Dictator.CHARGE_WINDUP || s.action == com.thesift.entity.boss.Dictator.CHARGE)) {
            // head down, horns first, like the turtle he robbed
            this.torso.xRot += 0.5F;
            this.head.xRot += 0.4F;
            this.arms[0].xRot = 0.6F;
            this.arms[1].xRot = 0.6F;
        }
        if (s.transform >= 0.0F) {
            // arms flung wide, head back, a slow turn in the air
            float e = Anim.envelope(s.transform, 0.0F, 0.2F, 0.55F, 0.25F);
            this.arms[0].zRot += -1.6F * e;
            this.arms[1].zRot += 1.6F * e;
            this.arms[0].xRot = Mth.lerp(e, this.arms[0].xRot, -0.3F);
            this.arms[1].xRot = Mth.lerp(e, this.arms[1].xRot, -0.3F);
            this.head.xRot -= 0.7F * e;
            this.torso.xRot -= 0.25F * e;
            this.torso.yRot += Mth.sin(age * 0.2F) * 0.2F * e;
            this.legs[0].xRot += 0.3F * e;
            this.legs[1].xRot -= 0.2F * e;
            this.coatTail.xRot += 0.6F * e + Mth.sin(age * 0.9F) * 0.15F * e;
        }

        // --- death: knees, slump, crumble
        if (s.dying > 0.0F) {
            float kneel = Anim.smooth(s.dying / 8.0F);
            float slump = Anim.smooth((s.dying - 6.0F) / 10.0F);
            this.body.y += 9.0F * kneel;
            this.legs[0].xRot = -1.4F * kneel;
            this.legs[1].xRot = -1.4F * kneel;
            this.shins[0].xRot = 1.9F * kneel;
            this.shins[1].xRot = 1.9F * kneel;
            this.torso.xRot += 0.9F * slump;
            this.head.xRot += 0.6F * slump;
            this.arms[0].xRot = Mth.lerp(slump, this.arms[0].xRot, 0.2F);
            this.arms[1].xRot = Mth.lerp(slump, this.arms[1].xRot, 0.2F);
            float crumble = Anim.smooth((s.dying - 12.0F) / 8.0F);
            this.body.xScale = 1.0F - crumble * 0.3F;
            this.body.zScale = 1.0F - crumble * 0.3F;
            this.body.yScale *= 1.0F - crumble * 0.45F;
        }
    }

    private void setTrait(ModelPart part, float amount) {
        part.visible = amount > 0.02F;
        part.xScale = amount;
        part.yScale = amount;
        part.zScale = amount;
    }
}
