package com.thesift.client.model;

import com.thesift.client.renderer.state.EnchoerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Echoer: a tall, graceful grazer with a seven-segment neck. Everything moves through the neck
 * as a wave, base first and the head last, so it always looks like one long living line.
 *
 * <ul>
 *   <li>idle: a slow sway rolling up the neck; every few seconds a curious head tilt, ears perking</li>
 *   <li>walk: a light, high-stepping diagonal gait, the neck bobbing against the stride</li>
 *   <li>inspect: the neck lowers all the way to the ground, the head sniffs in little twitches</li>
 *   <li>waiting: head held high and attentive, a slow expectant sway</li>
 *   <li>humming: head lifted, mouth open, the neck swaying to the beat</li>
 *   <li>dance: bobbing, prancing forelegs, the neck sweeping circles, tail swishing</li>
 *   <li>disappointed: neck and ears droop, a slow sad shake of the head</li>
 *   <li>sleep: legs folded, body lowered, the neck curled back to rest the head on its flank</li>
 *   <li>bow: a little rise, then a deep, slow bow with one foreleg forward</li>
 * </ul>
 */
public class EnchoerModel extends EntityModel<EnchoerRenderState> {
    private static final int NECK = 7;
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] neck = new ModelPart[NECK];
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];

    public EnchoerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
        ModelPart p = this.body;
        for (int i = 0; i < NECK; i++) {
            p = p.getChild("neck_" + i);
            this.neck[i] = p;
        }
        this.head = p.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHorn = this.head.getChild("right_horn");
        String[] names = {"front_left", "front_right", "back_left", "back_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild(names[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(names[i] + "_shin");
        }
    }

    @Override
    public void setupAnim(EnchoerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float awake = 1.0F - s.sleep;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F) * awake;
        float pos = s.walkAnimationPos * 0.6F;

        // --- legs: a light diagonal gait (front left with back right), knees folding as each foot lifts
        for (int i = 0; i < 4; i++) {
            float ph = pos + (i == 0 || i == 3 ? 0.0F : Mth.PI);
            this.legs[i].xRot += Mth.sin(ph) * 0.55F * walk;
            this.shins[i].xRot += Math.max(0.0F, -Mth.cos(ph)) * 0.7F * walk * (i < 2 ? 1.0F : -0.6F);
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.7F * walk;
        this.body.zRot += Mth.sin(pos) * 0.03F * walk;

        // --- the neck: a slow wave rolling up from the shoulders, the head looking where it looks
        float yaw = s.yRot * Anim.DEG;
        float pitch = s.xRot * Anim.DEG;
        float calm = awake * (1.0F - 0.6F * s.dance);
        for (int i = 0; i < NECK; i++) {
            ModelPart n = this.neck[i];
            n.zRot += Mth.sin(age * 0.045F + s.seed + i * 0.55F) * 0.035F * calm;
            n.xRot += Mth.sin(age * 0.06F + s.seed + i * 0.4F) * 0.02F * calm;
            n.yRot += yaw * 0.55F / NECK * awake;
        }
        this.neck[0].xRot += Mth.cos(pos * 2.0F) * 0.06F * walk;
        this.head.yRot += yaw * 0.45F * awake;
        this.head.xRot += pitch * 0.6F * awake;

        // --- a curious head tilt every eight seconds or so, to alternating sides, ears perking up
        float cyc = (age + s.seed * 3.0F) / 160.0F;
        float tilt = Anim.envelope((cyc - (float) Math.floor(cyc)) * 8.0F, 1.0F, 0.35F, 1.4F, 0.6F) * awake * (1.0F - s.dance) * (1.0F - s.sad);
        float side = ((int) Math.floor(cyc)) % 2 == 0 ? 1.0F : -1.0F;
        this.head.zRot += tilt * 0.45F * side;
        this.head.xRot -= tilt * 0.12F;
        this.leftEar.zRot -= tilt * 0.3F;
        this.rightEar.zRot += tilt * 0.3F;
        this.tail.yRot += Mth.sin(age * 0.07F + s.seed) * 0.12F * awake;

        // --- inspecting an offering: the whole neck lowers to the ground, the head sniffs
        float in = s.inspect;
        if (in > 0.0F) {
            this.neck[0].xRot += 0.95F * in;
            for (int i = 1; i < NECK; i++) {
                this.neck[i].xRot += (i < 4 ? 0.22F : 0.12F) * in;
            }
            float sniff = Mth.sin(age * 1.4F) * Math.max(0.0F, Mth.sin(age * 0.2F));
            this.head.xRot += (0.45F + sniff * 0.07F) * in;
            this.jaw.xRot += Math.max(0.0F, sniff) * 0.08F * in;
            this.legs[0].xRot -= 0.15F * in;
            this.legs[1].xRot -= 0.15F * in;
        }

        // --- waiting for the song: tall and attentive, a slow expectant sway
        this.neck[0].xRot -= 0.3F * s.wait;
        this.neck[NECK - 1].xRot += 0.1F * s.wait;
        this.head.xRot -= 0.05F * s.wait;
        this.leftEar.zRot -= 0.25F * s.wait;
        this.rightEar.zRot += 0.25F * s.wait;

        // --- humming along: head lifted, mouth open, the neck swaying to the beat
        float sing = s.sing * awake;
        if (sing > 0.0F) {
            for (int i = 0; i < NECK; i++) {
                this.neck[i].zRot += Mth.sin(age * 0.25F - i * 0.3F) * 0.07F * sing;
            }
            this.head.xRot -= 0.35F * sing;
            this.jaw.xRot += (0.3F + 0.12F * Mth.sin(age * 0.5F)) * sing;
        }

        // --- the dance: bobbing on the beat, forelegs prancing, the neck sweeping circles
        float d = s.dance;
        if (d > 0.0F) {
            float beat = age * 0.4F;
            this.body.y -= Math.abs(Mth.sin(beat)) * 1.3F * d;
            this.body.xRot += Mth.sin(beat) * 0.04F * d;
            this.legs[0].xRot -= Math.max(0.0F, Mth.sin(beat)) * 0.75F * d;
            this.shins[0].xRot += Math.max(0.0F, Mth.sin(beat)) * 0.9F * d;
            this.legs[1].xRot -= Math.max(0.0F, -Mth.sin(beat)) * 0.75F * d;
            this.shins[1].xRot += Math.max(0.0F, -Mth.sin(beat)) * 0.9F * d;
            for (int i = 0; i < NECK; i++) {
                this.neck[i].zRot += Mth.sin(beat * 0.5F + i * 0.4F) * 0.11F * d;
                this.neck[i].yRot += Mth.cos(beat * 0.5F + i * 0.4F) * 0.07F * d;
            }
            this.head.zRot += Mth.sin(beat * 0.5F + 2.5F) * 0.3F * d;
            this.tail.yRot += Mth.sin(beat * 2.0F) * 0.55F * d;
            this.tailTip.yRot += Mth.sin(beat * 2.0F - 0.8F) * 0.4F * d;
        }

        // --- disappointed: everything droops, a slow sad shake
        float sad = s.sad;
        if (sad > 0.0F) {
            this.neck[0].xRot += 0.6F * sad;
            for (int i = 1; i < 4; i++) {
                this.neck[i].xRot += 0.28F * sad;
            }
            this.head.xRot += 0.45F * sad;
            this.head.yRot += Mth.sin(age * 0.3F) * 0.25F * sad;
            this.leftEar.zRot += 0.5F * sad;
            this.rightEar.zRot -= 0.5F * sad;
            this.tail.xRot += 0.6F * sad;
        }

        // --- asleep: legs folded under, body lowered, neck curled back so the head rests on the flank
        float z = s.sleep;
        if (z > 0.0F) {
            this.body.y += 8.5F * z;
            for (int i = 0; i < 4; i++) {
                float f = i < 2 ? -1.0F : 1.0F;
                this.legs[i].y += 8.0F * z;
                this.legs[i].xRot += 1.45F * f * z;
                this.shins[i].xRot -= 2.7F * f * z;
            }
            this.neck[0].xRot -= 0.55F * z;
            for (int i = 1; i < NECK; i++) {
                this.neck[i].yRot += 0.42F * z;
                this.neck[i].xRot += 0.32F * z;
            }
            this.head.xRot += 0.7F * z;
            this.head.zRot += 0.3F * z;
            this.leftEar.zRot += 0.3F * z + Mth.sin(age * 0.9F) * Math.max(0.0F, Mth.sin(age * 0.03F) - 0.9F) * 2.0F * z;
            this.rightEar.zRot -= 0.3F * z;
            this.tail.xRot += 0.5F * z;
        }

        // --- the bow: a little rise first, then a deep slow bow with one foreleg stepped forward
        float b = Anim.seconds(s.bow, age);
        if (b >= 0.0F && b < 2.4F) {
            float up = Anim.envelope(b, 0.0F, 0.18F, 0.0F, 0.25F);
            float bow = Anim.envelope(b, 0.25F, 0.55F, 0.6F, 0.9F);
            this.neck[0].xRot += -0.15F * up + 1.15F * bow;
            for (int i = 1; i < NECK; i++) {
                this.neck[i].xRot += 0.18F * bow;
            }
            this.head.xRot += 0.5F * bow;
            this.body.xRot += 0.1F * bow;
            this.legs[0].xRot -= 0.4F * bow;
            this.shins[0].xRot += 0.55F * bow;
            this.leftHorn.xRot -= 0.1F * bow;
            this.rightHorn.xRot -= 0.1F * bow;
        }
    }
}
