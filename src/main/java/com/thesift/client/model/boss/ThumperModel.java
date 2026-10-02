package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.client.renderer.state.ThumperRenderState;
import com.thesift.entity.boss.Thumper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Thumper Titan (agent B2). Everything it does carries its weight:
 *
 * <ul>
 *   <li>walk: diagonal pairs of legs, each foot lifted on a bent knee, carried forward and planted
 *   flat; the shell drops onto every footfall and rolls towards the planted side; head and tail
 *   counter-sway, the moss curtains swing</li>
 *   <li>idle: the body is still - only its sculk vents breathe, lids lifting and the pits glowing
 *   on each slow exhale; the tendrils twitch, the head follows its gaze</li>
 *   <li>emerge, stomp (rears back, forefeet pawing, crashes down and squashes, settles with a
 *   wobble), charge, tail sweep, beam, burrow and erupt</li>
 *   <li>boulders: rakes the ground, then for each throw scoops low and tosses its head</li>
 *   <li>roar (phase change): rears up, stamps twice, throat open, head thrashing, every vent
 *   flaring</li>
 *   <li>stagger: a cannonball on a vent - it lurches away from the blow, knees buckling, head
 *   recoiling, that vent's lid flung open</li>
 *   <li>death: rears weakly, its forelegs buckle, the shell crashes down, legs splay, the head
 *   lolls and the vents gutter out</li>
 * </ul>
 */
public class ThumperModel extends EntityModel<MiniBossRenderState> {
    private static final String[] VENTS = {"vent_top", "vent_left", "vent_right"};
    private static final String[] LEGS = {"front_left", "front_right", "hind_left", "hind_right"};
    /** Walk phase offsets: diagonal pairs move together. */
    private static final float[] PHASE = {0.0F, Mth.PI, Mth.PI, 0.0F};
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftBrow;
    private final ModelPart rightBrow;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];
    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart[] lids = new ModelPart[3];
    private final ModelPart[] lit = new ModelPart[3];
    private final ModelPart[] moss = new ModelPart[10];

    public ThumperModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        ModelPart shell = this.body.getChild("shell");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftBrow = this.head.getChild("left_brow");
        this.rightBrow = this.head.getChild("right_brow");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(LEGS[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(LEGS[i] + "_shin");
            this.feet[i] = this.shins[i].getChild(LEGS[i] + "_foot");
        }
        this.tendrils[0] = this.head.getChild("left_tendril");
        this.tendrils[1] = this.head.getChild("right_tendril");
        for (int i = 0; i < 3; i++) {
            ModelPart vent = shell.getChild(VENTS[i]);
            this.lids[i] = vent.getChild(VENTS[i] + "_lid");
            this.lit[i] = vent.getChild(VENTS[i] + "_lit");
        }
        for (int i = 0; i < this.moss.length; i++) {
            this.moss[i] = shell.getChild("moss_" + i);
        }
    }

    @Override
    public void setupAnim(MiniBossRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 0.45F;
        float t = s.stateTime;
        int st = s.bossState;
        boolean dying = s.dying > 0.0F;
        if (dying) {
            walk = 0.0F;
        }

        // --- the heavy walk
        for (int i = 0; i < 4; i++) {
            float ph = pos + PHASE[i];
            float lift = Math.max(0.0F, Mth.cos(ph));
            this.legs[i].xRot = -Mth.sin(ph) * 0.42F * walk;
            this.shins[i].xRot = lift * lift * 0.75F * walk;
            this.legs[i].y -= lift * 0.6F * walk;
        }
        float fall = 1.0F - Math.abs(Mth.cos(pos));
        this.body.y += fall * fall * 1.1F * walk;
        this.body.zRot = Mth.sin(pos) * 0.055F * walk;
        this.body.yRot = Mth.cos(pos) * 0.035F * walk;
        this.neck.yRot = -this.body.yRot * 0.8F + Mth.sin(pos - 0.6F) * 0.05F * walk;
        this.neck.zRot = -this.body.zRot * 0.7F;
        this.neck.xRot = Mth.sin(age * 0.045F) * 0.03F + fall * 0.06F * walk;
        this.head.yRot = Mth.clamp(s.yRot, -50.0F, 50.0F) * Anim.DEG * 0.75F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F;
        this.tail.yRot = -Mth.sin(pos) * 0.22F * walk + Mth.sin(age * 0.06F) * 0.08F;
        this.tailTip.yRot = -Mth.sin(pos - 0.9F) * 0.3F * walk + Mth.sin(age * 0.06F - 0.8F) * 0.1F;
        this.tail.xRot = -fall * 0.05F * walk;
        for (int i = 0; i < this.moss.length; i++) {
            this.moss[i].xRot += Mth.sin(age * 0.07F + i * 1.3F) * 0.05F + Mth.sin(pos * 2.0F + i) * 0.12F * walk;
        }
        this.jaw.xRot = Math.max(0.0F, Mth.sin(age * 0.035F) - 0.6F) * 0.15F;
        boolean angry = st != Thumper.IDLE && st != Thumper.EXPOSED;
        this.leftBrow.zRot = angry || s.enraged ? -0.3F : 0.0F;
        this.rightBrow.zRot = angry || s.enraged ? 0.3F : 0.0F;

        // --- the Warden's tendrils: a restless twitch, a shiver when hurt or attacking
        float shiver = (s.hurtTicks >= 0.0F ? 1.0F : 0.0F) + (angry ? 0.5F : 0.0F);
        for (int i = 0; i < 2; i++) {
            float sgn = i == 0 ? 1.0F : -1.0F;
            float tw = Mth.sin(age * (0.11F + i * 0.02F) + i) * 0.16F + Mth.sin(age * 1.7F + i) * 0.08F * shiver;
            this.tendrils[i].zRot += sgn * tw;
            this.tendrils[i].xRot += Mth.sin(age * 0.07F + i * 2.0F) * 0.12F;
        }

        if (!dying) {
            switch (st) {
                case Thumper.EMERGE -> this.emerge(t, age);
                case Thumper.STOMP -> this.stomp(t, age);
                case Thumper.CHARGE_WINDUP -> {
                    float k = Anim.smooth(t / 8.0F);
                    this.neck.xRot += 0.35F * k;
                    this.neck.z -= 1.0F * k;
                    for (int i = 2; i < 4; i++) {
                        this.legs[i].xRot = Mth.sin(age * 0.9F + i * Mth.PI) * 0.55F * k;
                        this.shins[i].xRot = Math.max(0.0F, Mth.sin(age * 0.9F + i * Mth.PI)) * 0.5F * k;
                    }
                    this.body.zRot += Mth.sin(age * 1.3F) * 0.03F * k;
                    this.body.xRot = 0.08F * k;
                    this.body.y += 1.0F * k;
                    this.jaw.xRot += 0.25F * k;
                }
                case Thumper.CHARGE -> {
                    float gallop = age * 1.1F;
                    for (int i = 0; i < 4; i++) {
                        float ph = gallop + PHASE[i];
                        this.legs[i].xRot = -Mth.sin(ph) * 0.9F;
                        this.shins[i].xRot = Math.max(0.0F, Mth.cos(ph)) * 0.9F;
                    }
                    this.body.y -= Math.abs(Mth.sin(gallop)) * 1.5F;
                    this.body.xRot = 0.08F;
                    this.body.zRot = Mth.sin(gallop) * 0.05F;
                    this.neck.xRot = 0.4F;
                    this.neck.z -= 1.5F;
                    this.jaw.xRot = 0.35F;
                    this.tail.yRot = Mth.sin(gallop * 2.0F) * 0.35F;
                    this.tailTip.yRot = Mth.sin(gallop * 2.0F - 1.0F) * 0.4F;
                }
                case Thumper.TAIL_SWEEP -> {
                    float coil = Anim.smooth(t / Thumper.SWEEP_START) * (1.0F - Anim.smooth((t - Thumper.SWEEP_END) / 10.0F));
                    boolean whip = t >= Thumper.SWEEP_START && t <= Thumper.SWEEP_END + 2;
                    this.tail.yRot = whip ? -0.9F + Mth.sin(age * 2.0F) * 0.1F : 1.1F * coil;
                    this.tailTip.yRot = whip ? -0.7F : 0.6F * coil;
                    this.tail.xRot = -0.35F * coil;
                    this.body.zRot += (whip ? 0.12F : -0.08F) * coil;
                    this.neck.z += 2.0F * coil;
                    this.neck.xRot += 0.3F * coil;
                    for (int i = 0; i < 4; i++) {
                        this.legs[i].zRot = (i % 2 == 0 ? -0.25F : 0.25F) * coil;
                    }
                }
                case Thumper.EXPOSED -> {
                    float k = Anim.smooth(t / 6.0F) * (1.0F - Anim.smooth((t - Thumper.EXPOSED_TICKS + 10.0F) / 10.0F));
                    this.body.y += 3.0F * k;
                    for (int i = 0; i < 4; i++) {
                        this.legs[i].zRot = (i % 2 == 0 ? -0.55F : 0.55F) * k;
                        this.shins[i].zRot = (i % 2 == 0 ? 0.35F : -0.35F) * k;
                    }
                    this.neck.xRot = Mth.lerp(k, this.neck.xRot, 0.55F);
                    this.head.zRot = Mth.sin(age * 0.25F) * 0.3F * k;
                    this.head.yRot = Mth.lerp(k, this.head.yRot, Mth.sin(age * 0.15F) * 0.3F);
                    this.jaw.xRot = (0.4F + Math.max(0.0F, Mth.sin(age * 0.5F)) * 0.25F) * k;
                    this.tail.yRot *= 1.0F - 0.8F * k;
                }
                case Thumper.BEAM -> {
                    float charge = Anim.smooth(t / Thumper.BEAM_CHARGE);
                    boolean firing = t >= Thumper.BEAM_CHARGE && t < Thumper.BEAM_END;
                    float k = Math.min(charge, t < Thumper.BEAM_END ? 1.0F : 1.0F - Anim.smooth((t - Thumper.BEAM_END) / 12.0F));
                    this.neck.xRot -= 0.3F * k;
                    this.head.xRot -= 0.15F * k;
                    this.jaw.xRot = (firing ? 1.0F : 0.5F * charge) * k;
                    this.legs[0].xRot = -0.25F * k;
                    this.legs[1].xRot = -0.25F * k;
                    this.legs[2].xRot = 0.25F * k;
                    this.legs[3].xRot = 0.25F * k;
                    this.body.y += 1.0F * k;
                    if (firing) {
                        this.neck.xRot += Mth.sin(age * 2.2F) * 0.03F;
                        this.body.zRot += Mth.sin(age * 1.9F) * 0.02F;
                    }
                }
                case Thumper.BOULDERS -> this.boulders(t, age);
                case Thumper.BURROW -> {
                    float k = Anim.smooth(t / Thumper.BURROW_TICKS);
                    this.body.y += 22.0F * k * k;
                    this.body.xRot = 0.35F * Anim.smooth(t / 6.0F);
                    for (int i = 0; i < 4; i++) {
                        float dig = Mth.sin(age * (i < 2 ? 1.3F : 1.1F) + PHASE[i] + i);
                        this.legs[i].xRot = (i < 2 ? -0.8F : 0.4F) + dig * (i < 2 ? 0.9F : 0.5F);
                        this.shins[i].xRot = Math.max(0.0F, dig) * 0.7F;
                    }
                    this.neck.xRot = 0.5F;
                }
                case Thumper.ERUPT -> {
                    float out = Anim.backOut(Anim.clamp01(t / Thumper.ERUPT_IMPACT));
                    float rear = 1.0F - Anim.smooth((t - 4.0F) / 12.0F);
                    this.body.y += 22.0F * (1.0F - out);
                    this.body.xRot = -0.55F * rear;
                    this.legs[0].xRot = Mth.lerp(rear, this.legs[0].xRot, -1.2F);
                    this.legs[1].xRot = Mth.lerp(rear, this.legs[1].xRot, -1.0F);
                    this.shins[0].xRot = 0.8F * rear;
                    this.shins[1].xRot = 0.8F * rear;
                    this.neck.xRot -= 0.5F * rear;
                    this.jaw.xRot = 0.9F * rear;
                }
                case Thumper.ROAR -> this.roar(t, age);
                default -> {
                }
            }
            this.stagger(s);
        } else {
            this.collapse(s.dying, age);
        }
        // feet stay flat on the ground whatever the leg above them does
        for (int i = 0; i < 4; i++) {
            this.feet[i].xRot = -(this.legs[i].xRot + this.shins[i].xRot + this.body.xRot) * 0.85F;
            this.feet[i].zRot = -(this.legs[i].zRot + this.shins[i].zRot) * 0.7F;
        }
        this.vents(s, st, t, age);
    }

    /** Buried to the eyes, it claws its way up out of the floor and roars. */
    private void emerge(float t, float age) {
        float climb = Anim.smooth(t / (Thumper.EMERGE_TICKS - 22.0F));
        float under = 1.0F - climb;
        this.body.y += 30.0F * under;
        this.body.xRot = -0.6F * Mth.sin(climb * Mth.PI);
        float claw = Mth.sin(climb * Mth.PI) + 0.2F * under;
        for (int i = 0; i < 2; i++) {
            float pull = Mth.sin(age * 0.45F + i * Mth.PI);
            this.legs[i].xRot = -1.0F * claw + pull * 0.7F * claw;
            this.shins[i].xRot = Math.max(0.0F, pull) * 0.9F * claw;
        }
        this.legs[2].xRot = 0.5F * claw;
        this.legs[3].xRot = 0.5F * claw;
        this.neck.xRot -= 0.25F * claw;
        this.head.yRot = Mth.sin(age * 0.2F) * 0.3F * under;
        float roar = Anim.envelope(t, Thumper.EMERGE_TICKS - 22.0F, 3.0F, 12.0F, 6.0F);
        this.neck.xRot -= 0.6F * roar;
        this.head.xRot -= 0.35F * roar;
        this.jaw.xRot = Math.max(this.jaw.xRot, 1.1F * roar);
        this.head.zRot = Mth.sin(age * 1.8F) * 0.08F * roar;
    }

    /** Rears back onto its hind legs, forefeet pawing the air, then crashes down and settles. */
    private void stomp(float t, float age) {
        float up = Anim.envelope(t, 0.0F, 18.0F, 2.0F, 2.5F);
        float hit = Anim.envelope(t, Thumper.STOMP_IMPACT - 2.0F, 1.5F, 2.0F, 10.0F);
        // anticipation: a crouch before the rear
        float crouch = Anim.envelope(t, 0.0F, 4.0F, 0.0F, 5.0F);
        this.body.y += 1.5F * crouch - 3.0F * up;
        this.body.xRot = -0.55F * up + 0.08F * hit;
        for (int i = 0; i < 2; i++) {
            float paw = Mth.sin(age * 0.6F + i * Mth.PI) * 0.3F;
            this.legs[i].xRot = Mth.lerp(up, this.legs[i].xRot, -1.1F + paw);
            this.shins[i].xRot = Mth.lerp(up, this.shins[i].xRot, 0.9F - paw);
            this.legs[i].zRot = (i == 0 ? -0.28F : 0.28F) * hit;
        }
        for (int i = 2; i < 4; i++) {
            this.legs[i].xRot = Mth.lerp(up, this.legs[i].xRot, 0.55F);
            this.shins[i].xRot = Mth.lerp(up, this.shins[i].xRot, -0.15F);
        }
        this.neck.xRot += -0.4F * up + 0.35F * hit;
        this.jaw.xRot += 0.35F * up + 0.6F * hit;
        this.tail.xRot += 0.3F * up - 0.2F * hit;
        // the crash: squash, then a heavy wobble as it settles
        this.body.yScale *= 1.0F - 0.13F * hit;
        this.body.xScale = 1.0F + 0.07F * hit;
        this.body.zScale = 1.0F + 0.07F * hit;
        float after = t - Thumper.STOMP_IMPACT;
        if (after > 0.0F) {
            this.body.y += Mth.sin(after * 0.9F) * 0.8F * (float) Math.exp(-after * 0.18F);
            for (ModelPart m : this.moss) {
                m.xRot += Mth.sin(after * 1.2F) * 0.35F * (float) Math.exp(-after * 0.15F);
            }
        }
    }

    /** Rakes the ground, then for each boulder scoops low and flings it with a toss of the head. */
    private void boulders(float t, float age) {
        float rake = Anim.envelope(t, 0.0F, 4.0F, 30.0F, 8.0F);
        for (int i = 0; i < 2; i++) {
            float scrape = Math.max(0.0F, Mth.sin(age * 0.9F + i * Mth.PI));
            this.legs[i].xRot = -0.5F * rake + scrape * 0.6F * rake;
            this.shins[i].xRot = (1.0F - scrape) * 0.6F * rake;
        }
        this.body.y += 1.0F * rake;
        for (int throwAt = 16; throwAt <= 32; throwAt += 8) {
            float dip = Anim.envelope(t, throwAt - 6.0F, 4.0F, 0.5F, 1.5F);
            float toss = Anim.envelope(t, throwAt, 1.0F, 1.0F, 4.0F);
            this.neck.xRot += 0.7F * dip - 0.75F * toss;
            this.head.xRot += 0.2F * dip - 0.3F * toss;
            this.jaw.xRot += 0.5F * dip + 0.35F * toss;
            this.body.xRot += 0.08F * dip - 0.12F * toss;
            this.legs[0].xRot -= 0.25F * toss;
            this.legs[1].xRot -= 0.25F * toss;
        }
    }

    /** The phase change: it rears up, stamps twice, roars with its head thrashing. */
    private void roar(float t, float age) {
        float k = Anim.envelope(t, 0.0F, 6.0F, 24.0F, 10.0F);
        float stamp = Anim.envelope(t, 6.0F, 3.0F, 1.0F, 3.0F) + Anim.envelope(t, 14.0F, 3.0F, 1.0F, 3.0F);
        this.body.xRot = -0.3F * k + 0.15F * stamp;
        this.body.y -= 1.5F * k;
        for (int i = 0; i < 2; i++) {
            this.legs[i].xRot = -0.8F * k + 0.7F * stamp;
            this.shins[i].xRot = 0.6F * k * (1.0F - stamp);
        }
        this.legs[2].xRot = 0.35F * k;
        this.legs[3].xRot = 0.35F * k;
        float bellow = Anim.envelope(t, 12.0F, 4.0F, 18.0F, 6.0F);
        this.neck.xRot -= 0.55F * bellow;
        this.head.xRot -= 0.3F * bellow;
        this.jaw.xRot = 1.1F * bellow + 0.3F * stamp;
        this.head.zRot = Mth.sin(age * 1.6F) * 0.12F * bellow;
        this.neck.yRot += Mth.sin(age * 0.8F) * 0.15F * bellow;
        this.body.zRot += Mth.sin(age * 2.3F) * 0.03F * bellow;
        this.tail.yRot += Mth.sin(age * 1.4F) * 0.4F * bellow;
    }

    /** A cannonball on a vent: it lurches away from the blow, knees buckling, head recoiling. */
    private void stagger(MiniBossRenderState s) {
        if (!(s instanceof ThumperRenderState ts) || ts.staggerVent < 0 || ts.staggerTime > 18.0F) {
            return;
        }
        float x = ts.staggerTime;
        float k = Anim.smooth(x / 2.5F) * (1.0F - Anim.smooth((x - 3.0F) / 15.0F));
        float side = ts.staggerVent == 1 ? -1.0F : ts.staggerVent == 2 ? 1.0F : 0.0F;
        this.body.zRot += 0.2F * side * k + Mth.sin(x * 1.8F) * 0.03F * k;
        this.body.xRot += (side == 0.0F ? 0.14F : 0.04F) * k;
        this.body.y += 2.0F * k;
        for (int i = 0; i < 4; i++) {
            this.shins[i].xRot += 0.35F * k;
            this.legs[i].zRot += (i % 2 == 0 ? -0.2F : 0.2F) * k;
        }
        this.neck.xRot -= 0.4F * k;
        this.head.zRot -= 0.25F * side * k;
        this.jaw.xRot = Math.max(this.jaw.xRot, 0.85F * k);
        this.tail.yRot += 0.5F * side * k;
    }

    /** Death: a last weak rear, the forelegs buckle, the shell crashes down and it lies still. */
    private void collapse(float d, float age) {
        float rear = Anim.envelope(d, 0.0F, 6.0F, 2.0F, 6.0F);
        float buckle = Anim.smooth((d - 10.0F) / 14.0F);
        float settle = Anim.smooth((d - 24.0F) / 16.0F);
        float crash = d > 24.0F ? Mth.sin((d - 24.0F) * 1.1F) * (float) Math.exp(-(d - 24.0F) * 0.25F) : 0.0F;
        this.body.xRot = -0.3F * rear + 0.18F * buckle - 0.12F * settle;
        this.body.y += 6.0F * buckle + 0.6F * crash;
        this.body.zRot = 0.06F * settle;
        for (int i = 0; i < 4; i++) {
            float sx = i % 2 == 0 ? -1.0F : 1.0F;
            float splay = i < 2 ? buckle : settle;
            this.legs[i].zRot = sx * 1.15F * splay;
            this.shins[i].zRot = -sx * 0.6F * splay;
            this.legs[i].xRot = (i < 2 ? -0.5F : 0.3F) * splay - (i < 2 ? 0.5F * rear : 0.0F);
        }
        this.neck.xRot = -0.5F * rear + 0.65F * buckle;
        this.jaw.xRot = 0.9F * rear + 0.45F * settle;
        this.head.zRot = 0.45F * settle;
        this.head.yRot = Mth.lerp(settle, this.head.yRot, 0.25F);
        this.tail.yRot = 0.3F * settle;
        this.tail.xRot = 0.15F * settle;
        for (int i = 0; i < 2; i++) {
            this.tendrils[i].xRot += 0.9F * settle;
            this.tendrils[i].zRot *= 1.0F - settle;
        }
    }

    /** The vents: lids breathe while idle, gape when it strains, flare as it roars, gutter as it dies. */
    private void vents(MiniBossRenderState s, int st, float t, float age) {
        float open = switch (st) {
            case Thumper.EXPOSED -> Anim.smooth(t / 6.0F) * (1.0F - Anim.smooth((t - Thumper.EXPOSED_TICKS + 8.0F) / 8.0F));
            case Thumper.STOMP -> Anim.smooth((t - Thumper.STOMP_IMPACT) / 4.0F);
            case Thumper.BEAM -> Anim.backOut(Anim.clamp01(t / 10.0F)) * (1.0F - Anim.smooth((t - Thumper.BEAM_END - 4.0F) / 8.0F));
            case Thumper.ERUPT -> Anim.smooth((t - Thumper.ERUPT_IMPACT) / 4.0F);
            case Thumper.ROAR -> 0.55F * Anim.envelope(t, 10.0F, 4.0F, 18.0F, 8.0F);
            default -> 0.0F;
        };
        float rattle = s.hurtTicks >= 0.0F ? Mth.sin(s.hurtTicks * 3.0F) * 0.12F * (1.0F - Math.min(1.0F, s.hurtTicks / 10.0F)) : 0.0F;
        if (st == Thumper.BEAM && t < Thumper.BEAM_CHARGE) {
            rattle += Mth.sin(age * 2.5F) * 0.05F;
        }
        boolean dying = s.dying > 0.0F;
        int hitVent = s instanceof ThumperRenderState ts && ts.staggerTime < 14.0F ? ts.staggerVent : -1;
        for (int i = 0; i < 3; i++) {
            // a slow breath through each vent in turn
            float breath = 0.5F + 0.5F * Mth.sin(age * 0.075F + i * 2.1F);
            float lift = 0.16F * breath * breath;
            float o = Math.max(open, i == hitVent ? 1.0F : 0.0F);
            if (dying) {
                o = 0.5F * (1.0F - Anim.smooth((s.dying - 30.0F) / 25.0F));
                lift = 0.0F;
            }
            this.lids[i].xRot = -Math.max(lift, 1.9F * o) + rattle * (i == 0 ? 1.0F : -1.0F);
            boolean glow = o > 0.25F && (o > 0.7F || Mth.sin(age * 2.0F + i) > 0.0F);
            if (dying) {
                glow = s.dying < 50.0F && Mth.sin(age * (1.5F + s.dying * 0.05F) + i) > -0.2F + s.dying / 50.0F;
            } else if (!glow && open <= 0.0F) {
                glow = breath > 0.88F || (st == Thumper.BEAM && t < Thumper.BEAM_CHARGE && Mth.sin(age * 1.5F + i * 2.0F) > 0.4F);
            }
            this.lit[i].visible = glow;
        }
    }
}
