package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.DictatorRenderState;
import com.thesift.entity.boss.Dictator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * C3 Conductor: the colossus he swells into for his last movement (geometry: tools/conductor.py
 * dictator_kaiju) - a hunched sculk giant, his mask split in two over a maw of notes, organ
 * pipes rising from his back.
 *
 * <ul>
 *   <li>emerging: bursts out of the swelling Conductor curled up tight, unfolds to his full height,
 *   the mask halves blown wide and slowly closing, then throws his head back and roars</li>
 *   <li>idle: slow heavy heaves of the shoulders, the heart in his ribs pulsing, the mask halves
 *   breathing open a crack, horn-ears and tendrils drifting</li>
 *   <li>move: slow, heavy strides with a side-to-side roll of the shoulders, arms swinging
 *   against the legs</li>
 *   <li>slam: rears back with both fists raised high (a long anticipation), brings them down on
 *   the floor in front of him, holds the blow, heaves back up</li>
 *   <li>roar beam: the head draws back and the mask halves swing open, the jaw drops on the maw of
 *   notes, the arms brace wide; the head thrusts forward and shudders while he roars</li>
 *   <li>shockwave: rears up on his hind legs, arms wide, and stamps down with both fists</li>
 *   <li>rain and finale: plays his organ - the pipes swell one after another in a rolling wave,
 *   arms raised; at the finale everything trembles harder until the climax</li>
 *   <li>summon: one claw raised high, conducting a wide sweep, then clawed down at the floor</li>
 *   <li>rest: spent, he sinks to one knee, head hung, the mask shut</li>
 *   <li>death: knees buckle, he topples forward and the mask falls open for the last time</li>
 * </ul>
 */
public class DictatorKaijuModel extends EntityModel<DictatorRenderState> {
    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart heart;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart maskLeft;
    private final ModelPart maskRight;
    private final ModelPart[] pipes = new ModelPart[13];
    private final ModelPart[] banks = new ModelPart[3];
    private final ModelPart[] ears = new ModelPart[4];
    private final ModelPart[] tendrils = new ModelPart[5];
    private final ModelPart[] tatters = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[][] fingers = new ModelPart[2][3];
    private final ModelPart[] thumbs = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public DictatorKaijuModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.torso = this.body.getChild("torso");
        this.heart = this.torso.getChild("heart");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.maskLeft = this.head.getChild("mask_left");
        this.maskRight = this.head.getChild("mask_right");
        this.banks[0] = this.torso.getChild("pipes");
        this.banks[1] = this.torso.getChild("left_pipes");
        this.banks[2] = this.torso.getChild("right_pipes");
        for (int i = 0; i < 13; i++) {
            this.pipes[i] = this.banks[i < 7 ? 0 : i < 10 ? 1 : 2].getChild("pipe_" + i);
        }
        String[] sides = {"left", "right"};
        this.ears[0] = this.head.getChild("left_ear_upper");
        this.ears[1] = this.head.getChild("right_ear_upper");
        this.ears[2] = this.head.getChild("left_ear_lower");
        this.ears[3] = this.head.getChild("right_ear_lower");
        this.tendrils[0] = this.head.getChild("left_tendril");
        this.tendrils[1] = this.head.getChild("right_tendril");
        for (int i = 0; i < 3; i++) {
            this.tendrils[2 + i] = this.torso.getChild("back_tendril_" + i);
        }
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.tatters[i] = this.body.getChild(s + "_tatter");
            this.arms[i] = this.torso.getChild(s + "_arm");
            this.forearms[i] = this.arms[i].getChild(s + "_forearm");
            this.hands[i] = this.forearms[i].getChild(s + "_hand");
            for (int k = 0; k < 3; k++) {
                this.fingers[i][k] = this.hands[i].getChild(s + "_finger_" + k);
            }
            this.thumbs[i] = this.hands[i].getChild(s + "_thumb");
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
        }
    }

    /** Clenches (positive) or splays (negative) a hand. */
    private void fist(int side, float k) {
        for (int f = 0; f < 3; f++) {
            this.fingers[side][f].xRot -= k * 1.1F;
        }
        this.thumbs[side].xRot -= k * 0.7F;
        this.thumbs[side].zRot *= 1.0F - 0.5F * k;
    }

    /** Swings the mask halves open (0 shut, 1 wide) and drops the jaw with them. */
    private void openMask(float k) {
        this.maskLeft.yRot -= 1.05F * k;
        this.maskRight.yRot += 1.05F * k;
        this.maskLeft.zRot -= 0.12F * k;
        this.maskRight.zRot += 0.12F * k;
        this.jaw.xRot += 0.75F * k;
    }

    /** The organ: a wave of swelling running along the pipes. */
    private void playPipes(float age, float strength, float speed) {
        for (int i = 0; i < 13; i++) {
            float phase = i < 7 ? Math.abs(i - 3) : (i < 10 ? i - 6 : i - 9);
            float w = Math.max(0.0F, Mth.sin(age * speed - phase * 0.9F));
            this.pipes[i].yScale = 1.0F + 0.22F * strength * w * w;
            this.pipes[i].xScale = 1.0F + 0.08F * strength * w;
            this.pipes[i].zScale = this.pipes[i].xScale;
        }
    }

    @Override
    public void setupAnim(DictatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 0.32F;
        float cw = Mth.cos(pos);
        float sw = Mth.sin(pos);
        int act = s.action;
        float at = s.actionTime;

        // --- idle: slow heaves, the heart pulsing, the mask breathing open a crack
        float heave = Mth.sin(age * 0.045F);
        this.torso.xRot += heave * 0.03F;
        this.neck.xRot -= heave * 0.02F;
        for (int i = 0; i < 2; i++) {
            this.arms[i].zRot += (i == 0 ? -1.0F : 1.0F) * (0.03F + heave * 0.02F);
        }
        float beat = Anim.envelope(Mth.positiveModulo(age, 26.0F), 0.0F, 1.5F, 0.5F, 4.0F)
                + 0.6F * Anim.envelope(Mth.positiveModulo(age - 6.0F, 26.0F), 0.0F, 1.5F, 0.5F, 4.0F);
        this.heart.xScale = 1.0F + 0.14F * beat;
        this.heart.yScale = 1.0F + 0.14F * beat;
        this.openMask(0.06F + 0.05F * Math.max(0.0F, heave));
        for (int i = 0; i < 4; i++) {
            float sgn = i % 2 == 0 ? 1.0F : -1.0F;
            float twitch = Anim.envelope(Mth.positiveModulo(age + i * 31.0F, 90.0F + i * 11.0F), 0.0F, 2.0F, 1.0F, 5.0F);
            this.ears[i].zRot += sgn * (Mth.sin(age * 0.05F + i) * 0.04F + twitch * 0.15F);
        }
        for (int i = 0; i < 5; i++) {
            this.tendrils[i].xRot += Mth.sin(age * 0.08F + i * 1.7F) * 0.15F;
            this.tendrils[i].zRot += Mth.sin(age * 0.06F + i) * 0.1F;
        }
        for (int i = 0; i < 2; i++) {
            this.tatters[i].xRot += Mth.sin(age * 0.07F + i * 1.4F) * 0.05F;
        }

        // --- head turns to you, as far as that neck allows
        this.head.yRot += Mth.clamp(s.yRot, -50.0F, 50.0F) * Anim.DEG * 0.7F;
        this.head.xRot += s.xRot * Anim.DEG * 0.5F;

        // --- heavy strides with a roll of the shoulders
        this.legs[0].xRot += cw * 0.5F * walk;
        this.legs[1].xRot -= cw * 0.5F * walk;
        this.shins[0].xRot += Math.max(0.0F, -sw) * 0.55F * walk;
        this.shins[1].xRot += Math.max(0.0F, sw) * 0.55F * walk;
        this.feet[0].xRot -= Math.max(0.0F, -sw) * 0.25F * walk;
        this.feet[1].xRot -= Math.max(0.0F, sw) * 0.25F * walk;
        this.body.y -= Math.abs(cw) * 1.5F * walk;
        this.body.zRot += sw * 0.06F * walk;
        this.torso.yRot -= sw * 0.1F * walk;
        this.arms[0].xRot -= cw * 0.35F * walk;
        this.arms[1].xRot += cw * 0.35F * walk;
        this.forearms[0].xRot -= Math.max(0.0F, cw) * 0.3F * walk;
        this.forearms[1].xRot -= Math.max(0.0F, -cw) * 0.3F * walk;
        this.fist(0, 0.4F);
        this.fist(1, 0.4F);

        if (s.hasRedOverlay) {
            this.head.zRot += 0.25F;
            this.head.xRot += 0.15F;
            this.torso.xRot += 0.06F;
        }

        switch (act) {
            case Dictator.SLAM -> {
                float up = Anim.envelope(at, 0.0F, Dictator.SLAM_HIT - 3.0F, 1.0F, 2.0F);
                float down = Anim.envelope(at, Dictator.SLAM_HIT - 2.0F, 2.0F, 8.0F, 14.0F);
                float shake = Mth.sin(age * 2.2F) * 0.04F * up;
                this.torso.xRot += -0.5F * up + 0.55F * down;
                this.body.y += -2.0F * up + 3.0F * down;
                this.neck.xRot += 0.2F * up - 0.25F * down;
                for (int i = 0; i < 2; i++) {
                    float sgn = i == 0 ? 1.0F : -1.0F;
                    this.arms[i].xRot += -2.6F * up - 0.9F * down + shake * sgn;
                    this.arms[i].zRot += sgn * (-0.25F * up + 0.15F * down);
                    this.forearms[i].xRot += -0.3F * up + 0.2F * down;
                    this.fist(i, up + down);
                }
                this.legs[0].xRot -= 0.25F * down;
                this.legs[1].xRot += 0.15F * down;
                this.openMask(0.4F * down);
            }
            case Dictator.BEAM -> {
                float draw = Anim.envelope(at, 0.0F, Dictator.BEAM_CHARGE - 4.0F, 2.0F, 4.0F);
                float open = Anim.smooth(at / (float) Dictator.BEAM_CHARGE) * (1.0F - Anim.smooth((at - Dictator.BEAM_TICKS + 8.0F) / 8.0F));
                float fire = Anim.envelope(at, Dictator.BEAM_CHARGE - 1.0F, 2.0F, Dictator.BEAM_TICKS - Dictator.BEAM_CHARGE - 12.0F, 6.0F);
                float shudder = (Mth.sin(age * 2.7F) * 0.05F + Mth.sin(age * 4.3F) * 0.03F) * fire;
                this.head.xRot += -0.45F * draw + 0.25F * fire + shudder;
                this.neck.xRot += -0.25F * draw + 0.2F * fire;
                this.torso.xRot += -0.15F * draw + 0.1F * fire + shudder * 0.5F;
                this.openMask(open);
                this.jaw.xRot += 0.25F * fire;
                for (int i = 0; i < 2; i++) {
                    float sgn = i == 0 ? 1.0F : -1.0F;
                    this.arms[i].zRot += sgn * -0.55F * Math.max(draw, fire);
                    this.arms[i].xRot -= 0.4F * Math.max(draw, fire);
                    this.fist(i, -0.6F * fire);
                }
                this.playPipes(age, draw + fire * 0.5F, 0.5F);
            }
            case Dictator.QUAKE -> {
                float rear = Anim.envelope(at, 0.0F, Dictator.QUAKE_HIT - 4.0F, 2.0F, 2.0F);
                float stamp = Anim.envelope(at, Dictator.QUAKE_HIT - 2.0F, 2.0F, 6.0F, 16.0F);
                this.torso.xRot += -0.6F * rear + 0.45F * stamp;
                this.body.y += -3.0F * rear + 2.5F * stamp;
                this.legs[0].xRot += 0.35F * rear - 0.2F * stamp;
                this.legs[1].xRot += 0.35F * rear - 0.2F * stamp;
                this.shins[0].xRot += 0.3F * stamp;
                this.shins[1].xRot += 0.3F * stamp;
                for (int i = 0; i < 2; i++) {
                    float sgn = i == 0 ? 1.0F : -1.0F;
                    this.arms[i].xRot += -1.9F * rear - 0.4F * stamp;
                    this.arms[i].zRot += sgn * (-0.9F * rear - 0.35F * stamp);
                    this.fist(i, rear + stamp);
                }
                this.head.xRot -= 0.4F * rear;
                this.openMask(0.7F * rear);
                this.playPipes(age, stamp * 1.5F, 0.9F);
            }
            case Dictator.RAIN, Dictator.FINALE -> {
                boolean finale = act == Dictator.FINALE;
                float k = Anim.smooth(at / 10.0F) * (finale ? 1.0F - Anim.smooth((at - Dictator.FINALE_TICKS - 2.0F) / 8.0F) : 1.0F);
                float climb = finale ? Math.min(1.0F, at / Dictator.FINALE_TICKS) : 0.5F;
                float tremble = Mth.sin(age * 2.6F) * 0.04F * k * climb;
                this.torso.xRot -= 0.3F * k;
                this.head.xRot -= 0.4F * k;
                for (int i = 0; i < 2; i++) {
                    float sgn = i == 0 ? 1.0F : -1.0F;
                    float play = Mth.sin(age * 0.5F + i * Mth.PI) * 0.3F * (finale ? 0.3F : 1.0F);
                    this.arms[i].xRot += (-2.2F + play) * k + tremble * sgn;
                    this.arms[i].zRot += sgn * -0.45F * k;
                    this.fist(i, -0.7F * k);
                }
                this.openMask(k * (finale ? 0.3F + 0.7F * climb : 0.35F));
                this.playPipes(age, k * (0.6F + climb), finale ? 0.35F + climb * 0.3F : 0.45F);
            }
            case Dictator.REST -> {
                float k = Anim.smooth(at / 12.0F) * (1.0F - Anim.smooth((at - 78.0F) / 12.0F));
                this.body.y += 7.0F * k;
                this.legs[0].xRot -= 1.1F * k;
                this.shins[0].xRot += 1.4F * k;
                this.legs[1].xRot += 0.3F * k;
                this.shins[1].xRot += 1.1F * k;
                this.torso.xRot += 0.3F * k;
                this.head.xRot += 0.45F * k;
                this.arms[0].xRot += 0.3F * k;
                this.arms[1].xRot -= 0.2F * k;
                this.openMask(-0.06F * k);
            }
            default -> {
            }
        }

        float sum = Anim.seconds(s.summon, age) * 20.0F;
        if (sum >= 0.0F && sum < Dictator.SUMMON_TICKS) {
            // one claw raised high, a wide conducting sweep, then clawed down at the floor
            float raise = Anim.envelope(sum, 0.0F, 8.0F, 6.0F, 10.0F);
            float sweep = Anim.smooth((sum - 4.0F) / 12.0F);
            float claw = Anim.envelope(sum, Dictator.SUMMON_AT - 1.0F, 2.0F, 4.0F, 10.0F);
            this.arms[1].xRot += -2.4F * raise + 1.2F * claw;
            this.arms[1].zRot += Mth.lerp(sweep, 0.6F, -0.5F) * raise;
            this.fist(1, Mth.lerp(sweep, -0.8F, 0.3F) * raise + claw);
            this.arms[0].xRot += -0.6F * raise + 0.9F * claw;
            this.fist(0, -0.6F * raise + claw);
            this.torso.xRot += -0.2F * raise + 0.3F * claw;
            this.head.xRot -= 0.3F * raise;
            this.openMask(0.25F * raise);
        }
        float t;
        if ((t = Anim.seconds(s.roar, age)) >= 0 && t < 2.2F) {
            float rear = Anim.envelope(t, 0.0F, 0.35F, 1.1F, 0.6F);
            float shake = Mth.sin(age * 3.0F) * 0.04F * rear;
            this.torso.xRot -= 0.35F * rear;
            this.neck.xRot -= 0.3F * rear;
            this.head.xRot -= 0.35F * rear + shake;
            this.openMask(rear);
            for (int i = 0; i < 2; i++) {
                this.arms[i].zRot += (i == 0 ? -0.7F : 0.7F) * rear;
                this.fist(i, -0.8F * rear);
            }
            this.playPipes(age, rear, 0.8F);
        }

        if (s.transform >= 0.0F) {
            // bursting out of the Conductor: curled tight, then unfolding to his full height
            float burst = Dictator.KAIJU_BURST / (float) Dictator.KAIJU_TICKS;
            float u = Anim.clamp01((s.transform - burst) / (1.0F - burst));
            float curl = 1.0F - Anim.smooth(u / 0.55F);
            this.body.y += 9.0F * curl;
            this.torso.xRot += 0.7F * curl;
            this.head.xRot += 0.5F * curl;
            for (int i = 0; i < 2; i++) {
                float sgn = i == 0 ? 1.0F : -1.0F;
                this.arms[i].xRot -= 0.9F * curl;
                this.arms[i].zRot += sgn * 0.6F * curl;
                this.forearms[i].xRot -= 0.9F * curl;
                this.legs[i].xRot -= 0.8F * curl;
                this.shins[i].xRot += 1.2F * curl;
                this.fist(i, curl);
            }
            this.openMask(1.0F - Anim.smooth(u / 0.7F));
            for (ModelPart bank : this.banks) {
                bank.yScale = Anim.backOut(Anim.clamp01((u - 0.15F) / 0.4F));
            }
            for (ModelPart ear : this.ears) {
                float k = Anim.backOut(Anim.clamp01((u - 0.3F) / 0.4F));
                ear.xScale = k;
                ear.yScale = k;
                ear.zScale = k;
            }
        }

        // --- death: the knees buckle, he topples forward, the mask falls open
        if (s.dying > 0.0F) {
            float kneel = Anim.smooth(s.dying / 8.0F);
            float topple = Anim.smooth((s.dying - 6.0F) / 12.0F);
            this.body.y += 9.0F * kneel;
            for (int i = 0; i < 2; i++) {
                this.legs[i].xRot = -1.3F * kneel;
                this.shins[i].xRot = 1.7F * kneel;
                this.arms[i].xRot = Mth.lerp(topple, this.arms[i].xRot, -0.6F);
                this.fist(i, -topple);
            }
            this.torso.xRot += 0.7F * topple;
            this.head.xRot += 0.5F * topple;
            this.openMask(topple);
            this.playPipes(age, 1.0F - topple, 1.4F);
        }
    }
}
