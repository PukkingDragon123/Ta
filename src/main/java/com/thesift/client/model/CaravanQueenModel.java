package com.thesift.client.model;

import com.thesift.client.renderer.state.CaravanQueenRenderState;
import com.thesift.entity.caravan.CaravanQueen;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR2: the Caravan Queen, a very fat hermit crab (geometry in tools/caravans.py).
 *
 * <ul>
 *   <li>walk: a slow, heavy gait on three pairs of thick legs; her soft body rolls with each step
 *   and the great shell follows a beat behind, swaying on her back</li>
 *   <li>idle: she breathes (the shell rises and settles), her eyes look about on their stalks one
 *   at a time, the long feelers sweep, the mouthparts work, the huge claw flexes its pincer and the
 *   gems on her shell swell and shimmer one after another</li>
 *   <li>tap (playing her part in a jam): the shell gems ring - swell and spring back - and the
 *   feelers flick</li>
 *   <li>spit: she rears back and gulps, holds it, and lunges forward as the gems fly, mouthparts
 *   flared; then she settles</li>
 *   <li>swat: the great claw swings back and up, hangs there, and whips round in front of her,
 *   her whole body twisting into it, the pincer snapping shut</li>
 *   <li>feed: she takes the ore in her claws one after the other and crams it into her working
 *   mouthparts, nodding</li>
 *   <li>warn: she rears up and clacks the great claw at the intruder</li>
 *   <li>roar: she rears up with both claws flung wide, eye stalks splayed and the shell shuddering</li>
 *   <li>settle: she wriggles the shell down into her new lair, rocking it side to side</li>
 *   <li>dying: she swells and shudders harder and harder until she bursts</li>
 * </ul>
 */
public class CaravanQueenModel extends EntityModel<CaravanQueenRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private static final int SHELL_GEMS = 12;
    private static final int BELLY_GEMS = 4;
    private final ModelPart body;
    private final ModelPart shell;
    private final ModelPart head;
    private final ModelPart[] gems = new ModelPart[SHELL_GEMS];
    private final ModelPart[] bellyGems = new ModelPart[BELLY_GEMS];
    private final ModelPart[] stalks = new ModelPart[2];
    private final ModelPart[] feelers = new ModelPart[2];
    private final ModelPart[] feelerTips = new ModelPart[2];
    private final ModelPart[] mouth = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] claws = new ModelPart[2];
    private final ModelPart[] pincers = new ModelPart[2];
    private final ModelPart[][] legs = new ModelPart[2][3];
    private final ModelPart[][] shins = new ModelPart[2][3];

    public CaravanQueenModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.shell = this.body.getChild("shell");
        this.head = this.body.getChild("head");
        for (int i = 0; i < SHELL_GEMS; i++) {
            this.gems[i] = this.shell.getChild("gem_" + i);
        }
        for (int i = 0; i < BELLY_GEMS; i++) {
            this.bellyGems[i] = this.body.getChild("belly_gem_" + i);
        }
        for (int s = 0; s < 2; s++) {
            String side = SIDES[s];
            this.stalks[s] = this.head.getChild(side + "_eye_stalk");
            this.feelers[s] = this.head.getChild(side + "_feeler");
            this.feelerTips[s] = this.feelers[s].getChild(side + "_feeler_tip");
            this.mouth[s] = this.head.getChild(side + "_mouthpart");
            this.arms[s] = this.body.getChild(side + "_arm");
            this.claws[s] = this.arms[s].getChild(side + "_claw");
            this.pincers[s] = this.claws[s].getChild(side + "_pincer");
            for (int i = 0; i < 3; i++) {
                this.legs[s][i] = this.body.getChild(side + "_leg_" + i);
                this.shins[s][i] = this.legs[s][i].getChild(side + "_shin_" + i);
            }
        }
    }

    @Override
    public void setupAnim(CaravanQueenRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed * 7.0F;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.2F);
        float pos = s.walkAnimationPos * 0.8F;

        // --- the heavy gait: legs 0 and 2 of one side with leg 1 of the other; the body rolls, the shell lags behind
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 1.0F : -1.0F;
            for (int i = 0; i < 3; i++) {
                float phase = pos + (((i + side) % 2 == 0) ? 0.0F : Mth.PI) + i * 0.3F;
                float lift = Math.max(0.0F, Mth.sin(phase));
                float reach = Mth.cos(phase);
                this.legs[side][i].zRot -= lift * 0.3F * walk * sx;
                this.legs[side][i].yRot += reach * 0.2F * walk * sx;
                this.shins[side][i].zRot += lift * 0.25F * walk * sx;
            }
        }
        float roll = Mth.sin(pos) * 0.06F * walk;
        this.body.zRot = roll;
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.7F * walk;
        this.shell.zRot = Mth.sin(pos - 0.9F) * 0.07F * walk;
        this.shell.xRot = Mth.cos(pos * 2.0F - 0.6F) * 0.025F * walk;
        for (int k = 0; k < 2; k++) {
            this.arms[k].xRot += Mth.sin(pos + k * Mth.PI) * 0.1F * walk;
        }

        // --- idle: she breathes; the shell rises and settles on her back
        float breath = Mth.sin(age * 0.06F);
        this.shell.y -= breath * 0.35F;
        this.shell.xRot += breath * 0.012F;
        this.head.xRot += s.xRot * Anim.DEG * 0.3F + breath * 0.02F;
        this.head.yRot += s.yRot * Anim.DEG * 0.35F;
        // the eyes look about on their stalks, one at a time
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            float glance = Mth.sin(age * 0.035F + k * 2.6F);
            this.stalks[k].yRot += glance * 0.35F;
            this.stalks[k].xRot += Mth.sin(age * 0.05F + k * 1.4F) * 0.08F;
            this.stalks[k].zRot += Mth.sin(age * 0.045F + k) * 0.06F * sx;
            float twitch = (age + k * 41.0F) % 113.0F;
            if (twitch < 8.0F) {
                this.stalks[k].xRot += Anim.envelope(twitch / 20.0F, 0.0F, 0.06F, 0.08F, 0.2F) * 0.8F;
            }
        }
        // the long feelers sweep the air; the mouthparts work
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            float off = k * 1.9F;
            this.feelers[k].xRot += Mth.sin(age * 0.07F + off) * 0.16F - walk * 0.15F;
            this.feelers[k].yRot += Mth.cos(age * 0.055F + off) * 0.22F * sx;
            this.feelerTips[k].xRot += Mth.sin(age * 0.07F + off - 0.8F) * 0.18F;
            this.mouth[k].yRot = Mth.sin(age * 0.4F + k * Mth.PI) * 0.15F * sx;
            this.mouth[k].xRot += Mth.sin(age * 0.27F + k) * 0.05F;
        }
        // the claws flex at rest (the great right one slowly, opening wide now and then)
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            float flex = k == 1 ? Mth.sin(age * 0.04F) * 0.12F - Anim.envelope((age % 160.0F) / 20.0F, 0.0F, 0.3F, 0.4F, 0.5F) * 0.35F
                    : Mth.sin(age * 0.09F + 1.3F) * 0.08F;
            this.pincers[k].xRot += flex;
            this.arms[k].zRot += Mth.sin(age * 0.045F + k) * 0.025F * sx;
        }
        // the gems on her shell swell and shimmer one after another, and those in her belly sway
        for (int i = 0; i < SHELL_GEMS; i++) {
            float w = Math.max(0.0F, Mth.sin(age * 0.05F - i * 0.9F));
            float k = 1.0F + w * w * w * 0.06F;
            this.gems[i].xScale = k;
            this.gems[i].yScale = k;
            this.gems[i].zScale = k;
        }
        for (int i = 0; i < BELLY_GEMS; i++) {
            this.bellyGems[i].xRot += Mth.sin(age * 0.06F + i * 1.3F) * 0.04F + breath * 0.02F;
        }
        if (s.calm) {
            this.body.zRot += Mth.sin(age * 0.05F) * 0.03F;
            this.shell.zRot += Mth.sin(age * 0.05F - 0.6F) * 0.03F;
        }

        // --- tap: she rings her shell's gems for her note of the jam
        float tap = Anim.seconds(s.tap, s.ageInTicks);
        if (tap >= 0.0F && tap < 0.45F) {
            float ring = Anim.envelope(tap, 0.0F, 0.05F, 0.03F, 0.3F);
            for (int i = 0; i < SHELL_GEMS; i++) {
                float k = 1.0F + ring * (0.12F + 0.05F * (i % 3));
                this.gems[i].xScale *= k;
                this.gems[i].yScale *= k;
                this.gems[i].zScale *= k;
            }
            for (int k = 0; k < 2; k++) {
                this.feelers[k].xRot += Anim.envelope(tap, 0.0F, 0.06F, 0.0F, 0.2F) * 0.6F;
            }
            this.shell.y += ring * 0.4F;
        }

        // --- spit: rear back and gulp ... hold ... lunge as the gems fly, mouthparts flared
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        float spitAt = CaravanQueen.SPIT_AT / 20.0F;
        if (spit >= 0.0F && spit < 1.6F) {
            float rear = Anim.envelope(spit, 0.0F, spitAt - 0.15F, 0.1F, 0.12F);
            float lunge = Anim.envelope(spit, spitAt - 0.05F, 0.08F, 0.12F, 0.5F);
            float gulp = Mth.sin(spit * 30.0F) * Anim.envelope(spit, 0.15F, 0.1F, spitAt - 0.4F, 0.1F);
            this.body.xRot += -rear * 0.18F + lunge * 0.14F;
            this.body.z += rear * 1.2F - lunge * 1.6F;
            this.head.xRot += -rear * 0.25F + lunge * 0.3F;
            this.head.z -= lunge * 1.5F;
            this.head.y += gulp * 0.4F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.mouth[k].yRot += lunge * 0.7F * sx + gulp * 0.2F * sx;
                this.mouth[k].xRot -= lunge * 0.6F;
                this.arms[k].yRot -= rear * 0.35F * sx;
                this.arms[k].xRot -= rear * 0.2F;
                this.stalks[k].xRot -= rear * 0.3F;
            }
        }

        // --- swat: the great right claw swings back and up, hangs, and whips round in front of her
        float swat = Anim.seconds(s.swat, s.ageInTicks);
        float swatAt = CaravanQueen.SWAT_HIT / 20.0F;
        if (swat >= 0.0F && swat < 1.3F) {
            float back = Anim.envelope(swat, 0.0F, swatAt - 0.1F, 0.06F, 0.06F);
            float across = Anim.envelope(swat, swatAt - 0.04F, 0.08F, 0.15F, 0.55F);
            this.arms[1].yRot += -back * 0.9F + across * 1.1F;
            this.arms[1].xRot += -back * 0.6F + across * 0.15F;
            this.pincers[1].xRot += -back * 0.7F + across * 0.3F;
            this.body.yRot += -back * 0.12F + across * 0.2F;
            this.shell.yRot += across * 0.06F - back * 0.03F;
            this.arms[0].xRot -= back * 0.3F;
        }

        // --- feed: claws to the mouth one after the other, mouthparts busy, a nod with each mouthful
        float feed = Anim.seconds(s.feed, s.ageInTicks);
        if (feed >= 0.0F && feed < 1.4F) {
            float fade = Anim.envelope(feed, 0.0F, 0.15F, 0.95F, 0.3F);
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                float bring = Math.max(0.0F, Mth.sin(feed * 9.0F + k * Mth.PI)) * fade;
                this.arms[k].xRot -= bring * 0.5F;
                this.arms[k].yRot += bring * 0.5F * sx;
                this.pincers[k].xRot += bring * 0.25F;
                this.mouth[k].yRot += Mth.sin(feed * 26.0F + k * Mth.PI) * 0.3F * fade * sx;
            }
            this.head.xRot += Math.max(0.0F, Mth.sin(feed * 9.0F)) * 0.12F * fade;
        }

        // --- warn: reared up, the great claw high and clacking at the intruder
        float warn = Anim.seconds(s.warn, s.ageInTicks);
        if (warn >= 0.0F && warn < 1.1F) {
            float rear = Anim.envelope(warn, 0.0F, 0.18F, 0.6F, 0.3F);
            float clack = Mth.abs(Mth.sin(warn * 18.0F)) * Anim.envelope(warn, 0.18F, 0.05F, 0.5F, 0.1F);
            this.body.xRot -= rear * 0.16F;
            this.body.y -= rear * 1.2F;
            this.arms[1].xRot -= rear * 0.8F;
            this.arms[1].yRot += rear * 0.2F;
            this.pincers[1].xRot -= (1.0F - clack) * rear * 0.6F;
            this.arms[0].xRot -= rear * 0.35F;
            for (int k = 0; k < 2; k++) {
                this.stalks[k].xRot -= rear * 0.25F;
            }
        }

        // --- roar: up she rears, both claws flung wide, the eye stalks splayed, the shell shuddering
        float roar = Anim.seconds(s.roar, s.ageInTicks);
        if (roar >= 0.0F && roar < 1.6F) {
            float up = Anim.envelope(roar, 0.0F, 0.25F, 0.8F, 0.45F);
            float shudder = Mth.sin(roar * 55.0F) * up;
            this.body.xRot -= up * 0.22F;
            this.body.y -= up * 1.6F;
            this.shell.zRot += shudder * 0.035F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot -= up * 0.75F;
                this.arms[k].yRot -= up * 0.6F * sx;
                this.pincers[k].xRot -= up * 0.8F;
                this.stalks[k].zRot += up * 0.5F * sx;
                this.feelers[k].xRot -= up * 0.6F;
                this.mouth[k].yRot += up * 0.6F * sx;
            }
        }

        // --- settle: she wriggles the shell down into her lair, rocking it side to side
        float settle = Anim.seconds(s.settle, s.ageInTicks);
        if (settle >= 0.0F && settle < 2.0F) {
            float down = Anim.envelope(settle, 0.0F, 0.4F, 1.0F, 0.6F);
            this.shell.zRot += Mth.sin(settle * 9.0F) * 0.08F * down;
            this.shell.y += down * 0.8F;
            this.body.y += down * 0.9F;
            for (int side = 0; side < 2; side++) {
                float sx = side == 0 ? 1.0F : -1.0F;
                for (int i = 0; i < 3; i++) {
                    this.legs[side][i].zRot += down * 0.12F * sx;
                }
            }
        }

        // --- hurt: the claws snap up to guard her face, the eyes duck
        if (s.hasRedOverlay) {
            for (int k = 0; k < 2; k++) {
                this.arms[k].xRot -= 0.35F;
                this.stalks[k].xRot += 0.6F;
            }
        }

        // --- dying: she swells and shudders, harder and harder, until she bursts
        if (s.dying > 0.0F) {
            float t = Anim.clamp01(s.dying / CaravanQueen.BURST_AT);
            float shake = Mth.sin(s.dying * 7.0F) * t * t;
            this.body.zRot += shake * 0.08F;
            this.shell.zRot += shake * 0.1F;
            this.body.xScale = 1.0F + 0.14F * t * t;
            this.body.yScale = 1.0F + 0.1F * t * t;
            this.body.zScale = 1.0F + 0.14F * t * t;
            for (int side = 0; side < 2; side++) {
                float sx = side == 0 ? 1.0F : -1.0F;
                for (int i = 0; i < 3; i++) {
                    this.legs[side][i].zRot -= t * 0.4F * sx;
                    this.shins[side][i].zRot += t * 0.6F * sx;
                }
            }
            for (int i = 0; i < SHELL_GEMS; i++) {
                float k = 1.0F + t * 0.3F;
                this.gems[i].xScale *= k;
                this.gems[i].yScale *= k;
                this.gems[i].zScale *= k;
            }
        }
    }
}
