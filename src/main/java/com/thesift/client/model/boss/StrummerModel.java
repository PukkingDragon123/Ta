package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.Strummer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Strummer. The spider scuttles on a proper eight-legged gait (alternating fours) with its
 * guitar-body abdomen bobbing behind; on its back the mantis sways, cocks its head, waves its
 * antennae and never stops playing - its right hand strums the strings, its left frets them.
 * Every attack is acted out: scythes raised and brought down for the slash, abdomen raised to
 * spit web, a deep crouch before the pounce, a pulse of the abdomen as the brood pours out, both
 * hands thrashing for the big chord, a string drawn back and snapped.
 */
public class StrummerModel extends EntityModel<MiniBossRenderState> {
    private final ModelPart spider;
    private final ModelPart spiderHead;
    private final ModelPart abdomen;
    private final ModelPart leftFang;
    private final ModelPart rightFang;
    private final ModelPart[] leftLegs = new ModelPart[4];
    private final ModelPart[] rightLegs = new ModelPart[4];
    private final ModelPart[] leftShins = new ModelPart[4];
    private final ModelPart[] rightShins = new ModelPart[4];
    private final ModelPart mantis;
    private final ModelPart mantisHead;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftFemur;
    private final ModelPart rightFemur;
    private final ModelPart leftHand;
    private final ModelPart rightHand;
    private final ModelPart leftStrings;
    private final ModelPart rightStrings;
    private final ModelPart leftWing;
    private final ModelPart rightWing;

    private final ModelPart[] tendrils = new ModelPart[2];

    public StrummerModel(ModelPart root) {
        super(root);
        this.spider = root.getChild("spider");
        this.spiderHead = this.spider.getChild("spider_head");
        this.abdomen = this.spider.getChild("abdomen");
        this.leftFang = this.spiderHead.getChild("left_fang");
        this.rightFang = this.spiderHead.getChild("right_fang");
        for (int i = 0; i < 4; i++) {
            this.leftLegs[i] = this.spider.getChild("left_leg_" + i);
            this.rightLegs[i] = this.spider.getChild("right_leg_" + i);
            this.leftShins[i] = this.leftLegs[i].getChild("left_shin_" + i);
            this.rightShins[i] = this.rightLegs[i].getChild("right_shin_" + i);
        }
        this.mantis = this.spider.getChild("mantis");
        this.mantisHead = this.mantis.getChild("mantis_head");
        this.leftAntenna = this.mantisHead.getChild("left_antenna");
        this.rightAntenna = this.mantisHead.getChild("right_antenna");
        this.leftArm = this.mantis.getChild("left_arm");
        this.rightArm = this.mantis.getChild("right_arm");
        this.leftFemur = this.leftArm.getChild("left_femur");
        this.rightFemur = this.rightArm.getChild("right_femur");
        this.leftHand = this.leftFemur.getChild("left_hand");
        this.rightHand = this.rightFemur.getChild("right_hand");
        this.leftStrings = this.leftHand.getChild("left_strings");
        this.rightStrings = this.rightHand.getChild("right_strings");
        this.leftWing = this.mantis.getChild("left_mantis_wing");
        this.rightWing = this.mantis.getChild("right_mantis_wing");
        this.tendrils[0] = this.abdomen.getChild("left_abdomen_tendril");
        this.tendrils[1] = this.abdomen.getChild("right_abdomen_tendril");
    }

    @Override
    public void setupAnim(MiniBossRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float t = s.stateTime;
        int st = s.bossState;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 0.9F;
        float tempo = s.enraged ? 1.8F : 1.0F;

        // --- eight legs, alternating fours (like vanilla's spider, but with knees)
        for (int i = 0; i < 4; i++) {
            float ph = pos * 2.0F + (i % 2 == 0 ? 0.0F : Mth.PI);
            float swing = Mth.cos(ph) * 0.35F * walk;
            float lift = Math.max(0.0F, Mth.sin(ph)) * 0.45F * walk;
            this.leftLegs[i].yRot += swing;
            this.rightLegs[i].yRot -= swing;
            this.leftLegs[i].zRot -= lift;
            this.rightLegs[i].zRot += lift;
            this.leftShins[i].zRot += lift * 0.5F;
            this.rightShins[i].zRot -= lift * 0.5F;
            // idle twitching
            float tw = Mth.sin(age * 0.11F + i * 1.7F) * 0.03F;
            this.leftLegs[i].zRot += tw;
            this.rightLegs[i].zRot -= tw;
        }
        this.spider.y -= Math.abs(Mth.sin(pos * 2.0F)) * 0.6F * walk;
        this.abdomen.xRot += Mth.sin(age * 0.08F) * 0.05F - Mth.sin(pos * 2.0F) * 0.05F * walk;
        this.spiderHead.yRot = s.yRot * Anim.DEG * 0.3F;
        this.leftFang.xRot = Mth.sin(age * 0.2F) * 0.08F;
        this.rightFang.xRot = Mth.sin(age * 0.2F + 1.0F) * 0.08F;

        // --- the mantis rides: bigger than its seat, swaying, head cocked, antennae feeling the air
        this.mantis.xScale = 1.3F;
        this.mantis.yScale = 1.3F;
        this.mantis.zScale = 1.3F;
        this.mantis.zRot = Mth.sin(age * 0.05F) * 0.05F;
        this.mantis.xRot += Mth.sin(pos * 2.0F) * 0.03F * walk;
        this.mantisHead.yRot = s.yRot * Anim.DEG * 0.7F;
        this.mantisHead.xRot = s.xRot * Anim.DEG * 0.5F;
        this.mantisHead.zRot = Mth.sin(age * 0.04F) * 0.25F;
        this.leftAntenna.xRot += Mth.sin(age * 0.17F) * 0.25F;
        this.rightAntenna.xRot += Mth.sin(age * 0.19F + 1.3F) * 0.25F;
        this.leftAntenna.zRot += Mth.sin(age * 0.13F) * 0.15F;
        this.rightAntenna.zRot -= Mth.sin(age * 0.11F) * 0.15F;
        this.leftWing.yRot += Mth.sin(age * 0.07F) * 0.03F;
        this.rightWing.yRot -= Mth.sin(age * 0.07F) * 0.03F;
        // always playing: right hand strums, left hand frets
        float strum = Mth.sin(age * 0.35F * tempo);
        this.rightFemur.xRot += strum * 0.18F;
        this.rightHand.xRot += Math.max(0.0F, strum) * 0.15F;
        this.leftFemur.xRot += Mth.sin(age * 0.09F) * 0.08F;
        this.leftHand.xRot += (Mth.sin(age * 0.6F) > 0.6F ? 0.12F : 0.0F);
        this.leftStrings.zRot = Mth.sin(age * 3.0F) * 0.03F;
        this.rightStrings.zRot = Mth.sin(age * 3.3F + 1.0F) * 0.03F;

        // the Warden's tendrils: a restless twitch, a shiver when it is hurt or attacking
        float shiver = (s.hurtTicks >= 0.0F ? 1.0F : 0.0F) + (s.bossState != com.thesift.entity.boss.MiniBoss.IDLE ? 0.5F : 0.0F);
        for (int i = 0; i < 2; i++) {
            float sgn = i == 0 ? 1.0F : -1.0F;
            float tw = Mth.sin(s.ageInTicks * (0.11F + i * 0.02F) + i) * 0.18F + Mth.sin(s.ageInTicks * 1.7F + i) * 0.08F * shiver;
            this.tendrils[i].zRot += sgn * tw;
            this.tendrils[i].xRot += Mth.sin(s.ageInTicks * 0.07F + i * 2.0F) * 0.12F;
        }
        switch (st) {
            case Strummer.SLASH -> {
                float raise = Anim.envelope(t, 0.0F, 6.0F, 1.0F, 1.0F) + Anim.envelope(t, 9.0F, 4.0F, 1.0F, 1.0F);
                float hitL = Anim.envelope(t, 7.0F, 1.0F, 1.0F, 4.0F);
                float hitR = Anim.envelope(t, 14.0F, 1.0F, 1.0F, 5.0F);
                this.leftFemur.xRot += 1.0F * raise - 0.4F * hitL;
                this.rightFemur.xRot += 1.0F * raise - 0.4F * hitR;
                this.leftHand.xRot -= 1.4F * hitL;
                this.rightHand.xRot -= 1.4F * hitR;
                this.leftArm.xRot -= 0.6F * raise + 0.4F * hitL;
                this.rightArm.xRot -= 0.6F * raise + 0.4F * hitR;
                this.mantis.xRot += 0.25F * (hitL + hitR);
                this.mantisHead.xRot += 0.3F * (hitL + hitR);
            }
            case Strummer.WEB -> {
                float k = Anim.envelope(t, 0.0F, 10.0F, 4.0F, 8.0F);
                float spit = Anim.envelope(t, 13.0F, 1.0F, 1.0F, 6.0F);
                this.abdomen.xRot -= 0.5F * k;
                this.spiderHead.xRot -= 0.3F * k;
                this.leftFang.zRot = -0.5F * k;
                this.rightFang.zRot = 0.5F * k;
                this.spider.y += 1.0F * spit;
                this.abdomen.xScale = 1.0F + 0.08F * spit;
                this.abdomen.zScale = 1.0F + 0.08F * spit;
            }
            case Strummer.POUNCE_WINDUP -> {
                float k = Anim.smooth(t / 12.0F);
                this.spider.y += 3.0F * k;
                for (int i = 0; i < 4; i++) {
                    this.leftLegs[i].zRot -= 0.35F * k;
                    this.rightLegs[i].zRot += 0.35F * k;
                }
                this.mantis.xRot += 0.3F * k;
                this.leftArm.xRot -= 0.5F * k;
                this.rightArm.xRot -= 0.5F * k;
                this.spider.zRot = Mth.sin(age * 1.5F) * 0.02F * k;
            }
            case Strummer.POUNCE -> {
                for (int i = 0; i < 4; i++) {
                    float fwd = i < 2 ? 0.5F : -0.4F;
                    this.leftLegs[i].yRot += fwd;
                    this.rightLegs[i].yRot -= fwd;
                    this.leftLegs[i].zRot += 0.3F;
                    this.rightLegs[i].zRot -= 0.3F;
                }
                this.leftFemur.xRot += 0.9F;
                this.rightFemur.xRot += 0.9F;
                this.mantis.xRot -= 0.2F;
            }
            case Strummer.BROOD -> {
                float pulse = Mth.sin(t * 0.8F) * Anim.envelope(t, 0.0F, 4.0F, 18.0F, 8.0F);
                this.abdomen.xScale = 1.0F + 0.1F * pulse;
                this.abdomen.yScale = 1.0F + 0.12F * pulse;
                this.abdomen.zScale = 1.0F + 0.1F * pulse;
                this.spider.y += 1.5F * Anim.envelope(t, 0.0F, 4.0F, 18.0F, 8.0F);
            }
            case Strummer.STRUM -> {
                float k = Anim.envelope(t, 0.0F, 4.0F, 28.0F, 8.0F);
                float big = Anim.envelope(t, 28.0F, 1.0F, 2.0F, 8.0F);
                this.rightFemur.xRot += Mth.sin(age * 1.6F) * 0.45F * k;
                this.rightHand.xRot += Math.abs(Mth.sin(age * 1.6F)) * 0.3F * k;
                this.leftHand.xRot += Mth.sin(age * 0.9F) * 0.25F * k;
                this.mantisHead.xRot -= 0.35F * k - 0.3F * big;
                this.mantisHead.zRot = Mth.sin(age * 0.8F) * 0.3F * k;
                this.mantis.zRot += Mth.sin(age * 0.8F) * 0.08F * k;
                this.leftArm.xRot -= 0.9F * big;
                this.rightArm.xRot -= 0.9F * big;
                this.leftStrings.zRot = Mth.sin(age * 6.0F) * 0.12F * (k + big);
                this.rightStrings.zRot = Mth.sin(age * 6.5F) * 0.12F * (k + big);
            }
            case Strummer.SNAP -> {
                float draw = Anim.envelope(t, 0.0F, 14.0F, 1.0F, 1.0F);
                float snap = Anim.envelope(t, 15.0F, 1.0F, 2.0F, 8.0F);
                this.rightArm.xRot += 0.7F * draw - 1.2F * snap;
                this.rightFemur.xRot -= 0.5F * draw;
                this.rightHand.xRot -= 0.8F * snap;
                this.mantis.yRot = -0.3F * draw + 0.2F * snap;
                this.rightStrings.zRot = Mth.sin(age * 8.0F) * 0.2F * snap;
            }
            default -> {
            }
        }
        if (s.hurtTicks >= 0.0F) {
            float h = 1.0F - Math.min(1.0F, s.hurtTicks / 10.0F);
            this.mantis.xRot -= 0.15F * h;
            this.mantisHead.xRot -= 0.3F * h;
            this.spider.y += 0.8F * h;
        }
    }
}
