package com.thesift.client.model;

import com.thesift.client.renderer.state.SwifterRenderState;
import com.thesift.entity.Swifter;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Swifter. One layer holds the adult and the cub (big head, stubby legs) with the same part
 * names; the visible one is animated by the same rig.
 *
 * <p>Idle, its three tails sway each on its own phase, the sway rolling from root to tip; now and
 * then an ear flicks. It trots, then bounds in a gallop that stretches and gathers the spine.
 * Before a dash it crouches (low, haunches gathered, tails flared, a butt-wiggle); in the jet
 * pose the forelegs reach, the hind legs and all three tails stream straight back and the ears
 * flatten, the whole body pitched along its flight. Grabbing, the jaws gape and snap shut and
 * the head shakes; climbing, nose up, its legs dangle; at the top it tucks and flips; landing
 * from a slam it squashes flat, legs splayed, then shakes itself off from nose to tails. Asleep
 * it lies curled up in its tails. A crying cub sits hunched with its head bowed, ears drooping,
 * shoulders heaving, one paw at its eyes.
 */
public class SwifterModel extends EntityModel<SwifterRenderState> {
    private static final String[] TAILS = {"left", "middle", "right"};
    private final Rig adult;
    private final Rig cub;

    public SwifterModel(ModelPart root) {
        super(root);
        this.adult = new Rig(root.getChild("adult"));
        this.cub = new Rig(root.getChild("cub"));
    }

    /** The parts of one Swifter (adult or cub), plus its tails' resting angles. */
    private static final class Rig {
        final ModelPart root;
        final ModelPart body;
        final ModelPart head;
        final ModelPart jaw;
        final ModelPart leftEar;
        final ModelPart rightEar;
        final ModelPart leftEarTip;
        final ModelPart rightEarTip;
        final ModelPart frontLeftLeg;
        final ModelPart frontRightLeg;
        final ModelPart backLeftLeg;
        final ModelPart backRightLeg;
        final ModelPart[][] tails = new ModelPart[3][3];
        final float[] tailPitch = new float[3];
        final float[] tailYaw = new float[3];

        Rig(ModelPart root) {
            this.root = root;
            this.body = root.getChild("body");
            this.head = root.getChild("head");
            this.jaw = this.head.getChild("jaw");
            this.leftEar = this.head.getChild("left_ear");
            this.rightEar = this.head.getChild("right_ear");
            this.leftEarTip = this.leftEar.getChild("left_ear_tip");
            this.rightEarTip = this.rightEar.getChild("right_ear_tip");
            this.frontLeftLeg = root.getChild("front_left_leg");
            this.frontRightLeg = root.getChild("front_right_leg");
            this.backLeftLeg = root.getChild("back_left_leg");
            this.backRightLeg = root.getChild("back_right_leg");
            for (int i = 0; i < 3; i++) {
                String n = "tail_" + TAILS[i];
                ModelPart base = root.getChild(n);
                ModelPart mid = base.getChild(n + "_mid");
                this.tails[i][0] = base;
                this.tails[i][1] = mid;
                this.tails[i][2] = mid.getChild(n + "_tip");
                this.tailPitch[i] = base.xRot;
                this.tailYaw[i] = base.yRot;
            }
        }
    }

    @Override
    public void setupAnim(SwifterRenderState s) {
        super.setupAnim(s);
        boolean young = s.isBaby;
        this.adult.root.visible = !young;
        this.cub.root.visible = young;
        this.animate(young ? this.cub : this.adult, s, young);
    }

    private void animate(Rig r, SwifterRenderState s, boolean young) {
        float age = s.ageInTicks + s.seed;
        float k = young ? 0.55F : 1.0F;
        float speed = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * (young ? 1.4F : 0.85F);
        float jet = s.jet;
        float rest = s.sleep;
        float sob = s.cry;
        float ground = (1.0F - jet) * (1.0F - rest) * (1.0F - sob);
        float gallop = speed * Anim.smooth((speed - 0.45F) / 0.4F) * ground;
        float trot = speed * ground - gallop;
        boolean airborne = s.mode == Swifter.CLIMB || s.mode == Swifter.FLIP;

        // --- the look: the head follows the gaze
        r.head.yRot += s.yRot * Anim.DEG * ground;
        r.head.xRot += s.xRot * Anim.DEG * ground;

        // --- walking: a diagonal trot that becomes a bounding gallop
        float stepA = Mth.sin(pos);
        float stepB = Mth.sin(pos + Mth.PI);
        r.frontLeftLeg.xRot += stepA * 0.9F * trot;
        r.backRightLeg.xRot += stepA * 0.9F * trot;
        r.frontRightLeg.xRot += stepB * 0.9F * trot;
        r.backLeftLeg.xRot += stepB * 0.9F * trot;
        r.root.y -= Math.abs(Mth.cos(pos)) * 0.5F * k * trot;
        float bound = pos * 0.8F;
        float spine = Mth.sin(bound - 0.9F);
        r.frontLeftLeg.xRot += (Mth.sin(bound) * 1.15F + 0.1F) * gallop;
        r.frontRightLeg.xRot += (Mth.sin(bound - 0.25F) * 1.15F + 0.1F) * gallop;
        r.backLeftLeg.xRot -= Mth.sin(bound - 1.9F) * 1.1F * gallop;
        r.backRightLeg.xRot -= Mth.sin(bound - 2.15F) * 1.1F * gallop;
        r.root.xRot += spine * 0.12F * gallop;                       // stretch out, gather in
        r.root.y -= Math.max(0.0F, spine) * 1.6F * k * gallop;     // airborne between bounds
        r.head.xRot -= spine * 0.1F * gallop;                        // the head stays level
        r.leftEar.xRot -= 0.35F * gallop;
        r.rightEar.xRot -= 0.35F * gallop;

        // --- an ear flicks now and then
        float flickL = Mth.sin(age * 0.05F + s.seed);
        float flickR = Mth.sin(age * 0.043F + s.seed * 1.7F);
        if (flickL > 0.97F) {
            r.leftEar.zRot += (flickL - 0.97F) * 12.0F;
        }
        if (flickR > 0.97F) {
            r.rightEar.zRot -= (flickR - 0.97F) * 12.0F;
        }
        r.head.zRot += Mth.sin(age * 0.03F) * 0.04F * ground * (1.0F - speed);

        // --- the three tails: each sways on its own phase, the sway rolling from root to tip
        for (int i = 0; i < 3; i++) {
            ModelPart[] t = r.tails[i];
            float side = i == 0 ? 1.0F : i == 2 ? -1.0F : 0.0F;
            float ph = age * 0.075F + i * 2.1F;
            float amp = (0.22F + 0.12F * speed) * ground;
            t[0].yRot += Mth.sin(ph) * amp;
            t[1].yRot += Mth.sin(ph - 0.8F) * amp * 0.9F;
            t[2].yRot += Mth.sin(ph - 1.6F) * amp * 0.8F;
            t[0].xRot += Mth.cos(ph * 0.7F) * 0.07F * ground - 0.25F * speed * ground;
            t[2].xRot += Mth.sin(ph - 1.2F) * 0.08F * ground;
            // jet: all three stream straight back, fluttering in the slipstream
            t[0].xRot += (0.04F - r.tailPitch[i]) * jet + Mth.sin(age * 1.7F + i) * 0.04F * jet;
            t[0].yRot += -r.tailYaw[i] * 0.75F * jet;
            t[1].xRot -= 0.12F * jet;
            t[2].xRot -= 0.1F * jet;
            t[1].yRot += Mth.sin(age * 2.1F + i * 1.3F) * 0.06F * jet;
            // asleep: the side tails wrap round the flanks, the middle one curls round the front
            t[0].xRot += (-0.25F - r.tailPitch[i]) * rest;
            t[0].yRot += (side * 2.2F - r.tailYaw[i]) * rest;
            t[1].yRot += (side == 0.0F ? 1.0F : side * 0.55F) * rest;
            t[2].yRot += (side == 0.0F ? 0.8F : side * 0.5F) * rest;
            // crouched: raised and flared, twitching
            t[0].xRot += 0.35F * s.crouch;
            t[0].yRot += r.tailYaw[i] * 0.4F * s.crouch;
            t[2].yRot += Mth.sin(age * 1.9F + i) * 0.2F * s.crouch;
            // crying: limp on the ground
            t[0].xRot += (-0.35F - r.tailPitch[i]) * sob;
            if (s.angry) {
                t[0].xRot += 0.2F;
                t[1].xScale *= 1.08F;
                t[1].yScale *= 1.08F;
            }
        }

        // --- the pounce crouch (the anticipation): low, legs braced, chin up, a wiggle
        float c = s.crouch;
        r.root.y += 2.2F * k * c;
        r.root.xRot += 0.1F * c;
        r.root.yRot += Mth.sin(age * 1.4F) * 0.07F * c;
        r.frontLeftLeg.xRot -= 0.8F * c;
        r.frontRightLeg.xRot -= 0.8F * c;
        r.backLeftLeg.xRot += 0.85F * c;
        r.backRightLeg.xRot += 0.85F * c;
        r.head.xRot -= 0.2F * c;
        r.leftEar.xRot += 0.35F * c;
        r.rightEar.xRot += 0.35F * c;

        // --- the jet pose: the whole fox points where it flies
        r.root.xRot += s.pitch;
        r.root.y -= 1.0F * k * jet;
        if (airborne) {
            // climbing nose-up (and flipping at the top): the legs dangle, kicking
            r.frontLeftLeg.xRot += (-s.pitch + Mth.sin(age * 0.9F) * 0.25F) * jet;
            r.frontRightLeg.xRot += (-s.pitch + Mth.sin(age * 0.9F + 1.3F) * 0.25F) * jet;
            r.backLeftLeg.xRot += (-s.pitch + Mth.sin(age * 0.9F + 2.1F) * 0.2F) * jet;
            r.backRightLeg.xRot += (-s.pitch + Mth.sin(age * 0.9F + 3.0F) * 0.2F) * jet;
        } else {
            r.frontLeftLeg.xRot -= 1.2F * jet;
            r.frontRightLeg.xRot -= 1.1F * jet;
            r.backLeftLeg.xRot += 1.35F * jet;
            r.backRightLeg.xRot += 1.45F * jet;
        }
        r.leftEar.xRot -= 1.0F * jet;
        r.rightEar.xRot -= 1.0F * jet;
        if (s.mode == Swifter.FLIP) {
            // tucked tight for the flip; the tails swing round after it (follow-through)
            r.head.xRot += 0.6F;
            for (ModelPart[] tf : r.tails) {
                tf[1].xRot += 0.5F;
                tf[2].xRot += 0.5F;
            }
        }

        // --- the grab: jaws gape and snap, the head lunges, then shakes its catch
        float g = Anim.seconds(s.grab, s.ageInTicks);
        if (g >= 0.0F && g < 0.7F) {
            float open = Anim.envelope(g, 0.0F, 0.06F, 0.02F, 0.08F);
            float lunge = Anim.envelope(g, 0.0F, 0.08F, 0.05F, 0.2F);
            float shake = g > 0.15F ? Mth.sin((g - 0.15F) * 40.0F) * (1.0F - Anim.smooth((g - 0.15F) / 0.5F)) : 0.0F;
            r.jaw.xRot += 0.9F * open;
            r.head.z -= 1.5F * k * lunge;
            r.head.xRot += 0.25F * lunge;
            r.head.yRot += shake * 0.35F;
            r.head.zRot += shake * 0.15F;
        }
        if (s.carrying) {
            r.jaw.xRot += 0.35F; // holding the Bulb
        }

        // --- the slam: squashed flat on impact, legs splayed - then a shake from nose to tails
        float sl = Anim.seconds(s.slam, s.ageInTicks);
        if (sl >= 0.0F && sl < 1.2F) {
            float impact = Anim.envelope(sl, 0.0F, 0.03F, 0.05F, 0.25F);
            float st = sl - 0.35F;
            float shakeOff = st > 0.0F && st < 0.6F ? Mth.sin(st * 32.0F) * (1.0F - st / 0.6F) : 0.0F;
            r.root.y += 2.0F * k * impact;
            r.frontLeftLeg.zRot -= 0.5F * impact;
            r.frontRightLeg.zRot += 0.5F * impact;
            r.backLeftLeg.zRot -= 0.4F * impact;
            r.backRightLeg.zRot += 0.4F * impact;
            r.head.xRot += 0.4F * impact;
            r.root.zRot += shakeOff * 0.22F;
            r.head.zRot -= shakeOff * 0.3F;
            r.leftEar.zRot += shakeOff * 0.4F;
            r.rightEar.zRot += shakeOff * 0.4F;
            for (ModelPart[] ts : r.tails) {
                ts[0].xRot -= 0.4F * impact;
                ts[0].zRot += shakeOff * 0.4F;
                ts[1].yRot += shakeOff * 0.3F;
            }
        }

        // --- asleep, curled up: lying low, legs folded, head turned and resting on a tail
        r.root.y += 3.5F * k * rest;
        r.root.zRot += 0.12F * rest;
        r.frontLeftLeg.xRot -= 1.45F * rest;
        r.frontRightLeg.xRot -= 1.45F * rest;
        r.backLeftLeg.xRot -= 1.3F * rest;
        r.backRightLeg.xRot -= 1.3F * rest;
        r.head.yRot += 1.1F * rest;
        r.head.xRot += 0.35F * rest;
        r.head.y += 2.0F * k * rest;
        r.leftEar.xRot -= 0.5F * rest;
        r.rightEar.xRot -= 0.5F * rest;

        // --- a crying cub: sat hunched, head bowed, ears drooping, shoulders heaving, a paw at its eyes
        float heave = Mth.sin(age * 0.9F);
        r.root.y += 1.0F * k * sob;
        r.root.xRot -= 0.35F * sob;
        r.backLeftLeg.xRot -= 1.2F * sob;
        r.backRightLeg.xRot -= 1.2F * sob;
        r.frontRightLeg.xRot += 0.35F * sob;
        r.frontLeftLeg.xRot -= (1.6F + Mth.sin(age * 0.45F) * 0.25F) * sob;
        r.body.y += Math.max(0.0F, heave) * 0.3F * sob;
        r.head.xRot += (0.55F + heave * 0.08F) * sob;
        r.head.zRot += Mth.sin(age * 0.37F) * 0.08F * sob;
        r.leftEar.zRot += 0.7F * sob;
        r.rightEar.zRot -= 0.7F * sob;
        r.leftEar.xRot -= 0.4F * sob;
        r.rightEar.xRot -= 0.4F * sob;
        r.jaw.xRot += (0.25F + Math.max(0.0F, heave) * 0.15F) * sob;

        // --- calmed (or standing down): a happy little hop and a wag of all three tails
        float cm = Anim.seconds(s.calm, s.ageInTicks);
        if (cm >= 0.0F && cm < 2.0F) {
            float hop = Anim.envelope(cm, 0.05F, 0.12F, 0.05F, 0.2F);
            float wag = 1.0F - Anim.smooth(cm / 2.0F);
            r.root.y -= 1.5F * k * hop;
            r.head.xRot -= 0.2F * hop;
            r.leftEar.xRot += 0.2F * wag;
            r.rightEar.xRot += 0.2F * wag;
            for (int w = 0; w < 3; w++) {
                r.tails[w][0].yRot += Mth.sin(cm * 24.0F + w) * 0.45F * wag;
            }
        }

        // --- the snarl (a den's parents turning on its breaker): rear up, jaws wide, tails puffed
        float sn = Anim.seconds(s.snarl, s.ageInTicks);
        if (sn >= 0.0F && sn < 1.0F) {
            float rear = Anim.envelope(sn, 0.0F, 0.15F, 0.25F, 0.3F);
            r.root.xRot -= 0.35F * rear;
            r.root.y -= 0.5F * k * rear;
            r.frontLeftLeg.xRot -= 0.6F * rear;
            r.frontRightLeg.xRot -= 0.5F * rear;
            r.jaw.xRot += 0.7F * rear;
            r.head.xRot -= 0.2F * rear;
            r.leftEar.xRot -= 0.6F * rear;
            r.rightEar.xRot -= 0.6F * rear;
            float puff = 1.0F + 0.15F * rear;
            for (ModelPart[] tp : r.tails) {
                tp[1].xScale *= puff;
                tp[1].yScale *= puff;
                tp[1].zScale *= puff;
            }
        }
        if (s.angry) {
            r.leftEar.xRot -= 0.45F;
            r.rightEar.xRot -= 0.45F;
            r.jaw.xRot += 0.12F + Mth.sin(age * 0.5F) * 0.04F;
        }
    }
}
