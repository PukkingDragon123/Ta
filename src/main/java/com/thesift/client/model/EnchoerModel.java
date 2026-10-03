package com.thesift.client.model;

import com.thesift.client.renderer.state.EnchoerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Echoer, a furry god-deer: thick mane, a short strong neck, great antlers hung with wind
 * chimes and a halo of light behind them (geometry in tools/echoer.py).
 *
 * <p>The chimes hang from the head but always fall towards the ground: every tilt of the body,
 * neck and head is taken back out of them, and what is left is their own swing - a breeze sway,
 * a lagging swing with each stride, a wide swing in the dance and a forward swing after a bow.
 *
 * <ul>
 *   <li>idle: slow breathing-free sway of the mane and chimes; a curious head tilt every few seconds</li>
 *   <li>walk: a proud high-stepping diagonal gait, the head bobbing against the stride</li>
 *   <li>inspect: the head lowered to the offering, sniffing in little twitches</li>
 *   <li>waiting: head held high, ears forward, the chimes stirring</li>
 *   <li>humming: head lifted, mouth open, swaying to the beat</li>
 *   <li>dance: rising on the beat, forelegs prancing, the head sweeping slow circles so the chimes fly wide</li>
 *   <li>disappointed: head and ears droop, a slow sad shake</li>
 *   <li>sleep: legs folded, body lowered, the head turned back to rest on the flank</li>
 *   <li>bow: a little rise (anticipation), then a deep slow bow with one foreleg forward; the chimes
 *   swing on after the head stops (follow-through)</li>
 * </ul>
 */
public class EnchoerModel extends EntityModel<EnchoerRenderState> {
    private static final int CHIMES = 4;
    private final ModelPart body;
    private final ModelPart ruff;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart neck;
    private final ModelPart mane;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftAntler;
    private final ModelPart rightAntler;
    private final ModelPart halo;
    private final ModelPart[] chimes = new ModelPart[CHIMES * 2];
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private final float neckRest;
    private final float headRest;

    public EnchoerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.ruff = this.body.getChild("ruff");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
        this.neck = this.body.getChild("neck");
        this.mane = this.neck.getChild("mane");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftAntler = this.head.getChild("left_antler");
        this.rightAntler = this.head.getChild("right_antler");
        this.halo = this.head.getChild("halo");
        for (int i = 0; i < CHIMES; i++) {
            this.chimes[i] = this.head.getChild("left_chime_" + i);
            this.chimes[CHIMES + i] = this.head.getChild("right_chime_" + i);
        }
        String[] names = {"front_left", "front_right", "back_left", "back_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild(names[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(names[i] + "_shin");
        }
        this.neckRest = this.neck.xRot;
        this.headRest = this.head.xRot;
    }

    @Override
    public void setupAnim(EnchoerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float awake = 1.0F - s.sleep;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F) * awake;
        float pos = s.walkAnimationPos * 0.6F;

        // --- legs: a proud diagonal gait (front left with back right), knees folding high as each foot lifts
        for (int i = 0; i < 4; i++) {
            float ph = pos + (i == 0 || i == 3 ? 0.0F : Mth.PI);
            this.legs[i].xRot += Mth.sin(ph) * 0.5F * walk;
            this.shins[i].xRot += Math.max(0.0F, -Mth.cos(ph)) * 0.8F * walk * (i < 2 ? 1.0F : -0.6F);
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.6F * walk;
        this.body.zRot += Mth.sin(pos) * 0.025F * walk;
        // the head bobs against the stride, the ruff and mane bounce a beat behind
        this.neck.xRot += Mth.cos(pos * 2.0F) * 0.05F * walk;
        this.ruff.xRot += Mth.cos(pos * 2.0F - 0.9F) * 0.12F * walk;
        this.mane.xRot += Mth.cos(pos * 2.0F - 1.2F) * 0.08F * walk;

        // --- looking around: the neck takes some of the turn, the head the rest
        float yaw = s.yRot * Anim.DEG;
        float pitch = s.xRot * Anim.DEG;
        this.neck.yRot += yaw * 0.4F * awake;
        this.head.yRot += yaw * 0.6F * awake;
        this.head.xRot += pitch * 0.5F * awake;
        // a slow, regal sway of the mane
        this.mane.zRot += Mth.sin(age * 0.05F + s.seed) * 0.04F * awake;
        this.ruff.zRot += Mth.sin(age * 0.05F + s.seed + 1.0F) * 0.03F * awake;
        this.halo.zRot += Mth.sin(age * 0.02F + s.seed) * 0.05F;

        // --- a curious head tilt every eight seconds or so, to alternating sides, ears perking up
        float cyc = (age + s.seed * 3.0F) / 160.0F;
        float tilt = Anim.envelope((cyc - (float) Math.floor(cyc)) * 8.0F, 1.0F, 0.35F, 1.4F, 0.6F) * awake * (1.0F - s.dance) * (1.0F - s.sad);
        float side = ((int) Math.floor(cyc)) % 2 == 0 ? 1.0F : -1.0F;
        this.head.zRot += tilt * 0.3F * side;
        this.head.xRot -= tilt * 0.1F;
        this.leftEar.zRot -= tilt * 0.3F;
        this.rightEar.zRot += tilt * 0.3F;
        this.tail.yRot += Mth.sin(age * 0.07F + s.seed) * 0.12F * awake;

        // --- inspecting an offering: neck lowered all the way, the head sniffs
        float in = s.inspect;
        if (in > 0.0F) {
            this.neck.xRot += 1.05F * in;
            float sniff = Mth.sin(age * 1.4F) * Math.max(0.0F, Mth.sin(age * 0.2F));
            this.head.xRot += (0.35F + sniff * 0.07F) * in;
            this.jaw.xRot += Math.max(0.0F, sniff) * 0.08F * in;
            this.legs[0].xRot -= 0.15F * in;
            this.legs[1].xRot -= 0.15F * in;
        }

        // --- waiting for the song: tall and attentive, ears forward
        this.neck.xRot -= 0.25F * s.wait;
        this.head.xRot += 0.1F * s.wait;
        this.leftEar.yRot -= 0.3F * s.wait;
        this.rightEar.yRot += 0.3F * s.wait;

        // --- humming along: head lifted, mouth open, swaying to the beat
        float sing = s.sing * awake;
        if (sing > 0.0F) {
            this.neck.zRot += Mth.sin(age * 0.25F) * 0.05F * sing;
            this.head.zRot += Mth.sin(age * 0.25F - 0.6F) * 0.08F * sing;
            this.head.xRot -= 0.3F * sing;
            this.jaw.xRot += (0.3F + 0.12F * Mth.sin(age * 0.5F)) * sing;
        }

        // --- the ceremony dance: rising on the beat, forelegs prancing, the head sweeping circles
        float d = s.dance;
        float beat = age * 0.4F;
        if (d > 0.0F) {
            float lift = Math.max(0.0F, Mth.sin(beat));
            this.body.y -= Math.abs(Mth.sin(beat)) * 1.3F * d;
            this.body.xRot -= lift * 0.12F * d;
            this.legs[0].xRot -= lift * 0.85F * d;
            this.shins[0].xRot += lift * 1.0F * d;
            this.legs[1].xRot -= Math.max(0.0F, -Mth.sin(beat)) * 0.85F * d;
            this.shins[1].xRot += Math.max(0.0F, -Mth.sin(beat)) * 1.0F * d;
            this.neck.zRot += Mth.sin(beat * 0.5F) * 0.12F * d;
            this.neck.yRot += Mth.cos(beat * 0.5F) * 0.12F * d;
            this.head.zRot += Mth.sin(beat * 0.5F + 0.8F) * 0.25F * d;
            this.head.xRot -= 0.15F * d;
            this.tail.yRot += Mth.sin(beat * 2.0F) * 0.55F * d;
            this.tailTip.yRot += Mth.sin(beat * 2.0F - 0.8F) * 0.4F * d;
            this.ruff.xRot += Mth.sin(beat * 2.0F - 1.0F) * 0.18F * d;
            this.halo.zRot += age * 0.03F * d;
        }

        // --- disappointed: everything droops, a slow sad shake
        float sad = s.sad;
        if (sad > 0.0F) {
            this.neck.xRot += 0.75F * sad;
            this.head.xRot += 0.35F * sad;
            this.head.yRot += Mth.sin(age * 0.3F) * 0.25F * sad;
            this.leftEar.zRot += 0.5F * sad;
            this.rightEar.zRot -= 0.5F * sad;
            this.tail.xRot -= 0.3F * sad;
        }

        // --- asleep: legs folded under, body lowered, the head turned back to rest on the flank
        float z = s.sleep;
        if (z > 0.0F) {
            this.body.y += 8.0F * z;
            for (int i = 0; i < 4; i++) {
                float f = i < 2 ? -1.0F : 1.0F;
                this.legs[i].y += 7.5F * z;
                this.legs[i].xRot += 1.45F * f * z;
                this.shins[i].xRot -= 2.7F * f * z;
            }
            this.neck.xRot -= 0.35F * z;
            this.neck.yRot += 0.9F * z;
            this.head.yRot += 0.5F * z;
            this.head.xRot += 0.45F * z;
            this.head.zRot += 0.3F * z;
            this.leftEar.zRot += 0.3F * z + Mth.sin(age * 0.9F) * Math.max(0.0F, Mth.sin(age * 0.03F) - 0.9F) * 2.0F * z;
            this.rightEar.zRot -= 0.3F * z;
        }

        // --- the bow: a little rise first, then a deep slow bow with one foreleg stepped forward
        float b = Anim.seconds(s.bow, age);
        float chimeKick = 0.0F;
        if (b >= 0.0F && b < 2.4F) {
            float up = Anim.envelope(b, 0.0F, 0.18F, 0.0F, 0.25F);
            float bow = Anim.envelope(b, 0.25F, 0.55F, 0.6F, 0.9F);
            this.neck.xRot += -0.2F * up + 1.15F * bow;
            this.head.xRot += 0.35F * bow;
            this.body.xRot += 0.1F * bow;
            this.legs[0].xRot -= 0.4F * bow;
            this.shins[0].xRot += 0.55F * bow;
            // the chimes swing on once the head has stopped, and settle
            float after = Math.max(0.0F, b - 0.8F);
            chimeKick = Mth.sin(after * 9.0F) * (float) Math.exp(-after * 2.2F) * 0.5F * (b > 0.8F ? 1.0F : bow);
        }

        // --- the wind chimes: hang plumb, then sway with the breeze, the stride, the dance and the bow
        float tiltX = this.body.xRot + (this.neck.xRot - this.neckRest) + (this.head.xRot - this.headRest);
        float tiltZ = this.body.zRot + this.neck.zRot + this.head.zRot;
        float headTurn = this.neck.yRot + this.head.yRot;
        for (int i = 0; i < this.chimes.length; i++) {
            ModelPart c = this.chimes[i];
            float ph = i * 1.37F + s.seed;
            float breeze = Mth.sin(age * 0.06F + ph) * 0.07F + Mth.sin(age * 0.023F + ph * 2.0F) * 0.04F;
            float stride = Mth.sin(pos * 2.0F - 1.1F - i * 0.25F) * 0.32F * walk;
            float swirl = Mth.sin(beat * 0.5F - 0.9F + i * 0.3F) * 0.55F * d;
            c.xRot = -tiltX * 0.95F + breeze + stride + chimeKick + Mth.cos(beat * 0.5F - 0.9F + i * 0.3F) * 0.25F * d;
            c.zRot = -tiltZ * 0.95F + Mth.cos(age * 0.05F + ph) * 0.06F + swirl + Mth.sin(pos - i) * 0.12F * walk;
            c.yRot = -headTurn * 0.3F;
        }
        // the antlers shiver with the dance
        this.leftAntler.zRot += Mth.sin(beat * 2.0F) * 0.03F * d;
        this.rightAntler.zRot -= Mth.sin(beat * 2.0F) * 0.03F * d;
    }
}
