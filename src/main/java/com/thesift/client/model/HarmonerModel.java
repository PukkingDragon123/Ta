package com.thesift.client.model;

import com.thesift.client.renderer.state.HarmonerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Harmoner: a chunky songbird with a block beak, a three-plume crest and a fanned tail.
 *
 * <ul>
 *   <li>idle: quick bird-like head turns that settle in place, a wagging tail, soft breathing</li>
 *   <li>move: hops along the ground on little legs; in the air the wings beat hard, legs tuck up
 *   and the body pitches forward</li>
 *   <li>sing: head thrown back, beak chattering on every note, crest raised and fanned, chest
 *   puffed out</li>
 *   <li>guiding: crest held up the whole way, excited</li>
 *   <li>hurt: crest slicked back, wings jolt open</li>
 * </ul>
 */
public class HarmonerModel extends EntityModel<HarmonerRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart crest;
    private final ModelPart[] plumes = new ModelPart[3];
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart tail;
    private final ModelPart tailFan;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public HarmonerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.crest = this.head.getChild("crest");
        for (int i = 0; i < 3; i++) {
            this.plumes[i] = this.crest.getChild("plume_" + i);
        }
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.tail = this.body.getChild("tail");
        this.tailFan = this.tail.getChild("tail_fan");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
    }

    /** A repeatable pseudo-random value in [-1, 1] for the n-th head turn. */
    private static float turn(int n, int salt) {
        int h = n * 374761393 + salt * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h >>> 8) & 0xFFFF) / 32767.5F - 1.0F;
    }

    @Override
    public void setupAnim(HarmonerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float fly = s.flap;
        float ground = 1.0F - fly;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F) * ground;
        float pos = s.walkAnimationPos * 1.5F;


        // --- head: snaps to a new direction every ~1.2 s and settles there, like a real bird
        int n = Mth.floor(age / 24.0F);
        float blend = Anim.smooth(Mth.frac(age / 24.0F) * 6.0F);
        float yaw = Mth.lerp(blend, turn(n - 1, 1), turn(n, 1));
        float tilt = Mth.lerp(blend, turn(n - 1, 2), turn(n, 2));
        // S1: the look is clamped, so the head never twists round past its shoulders
        this.head.yRot = Mth.clamp(s.yRot * Anim.DEG * 0.7F, -0.9F, 0.9F) + yaw * 0.45F * ground;
        this.head.xRot = Mth.clamp(s.xRot * Anim.DEG * 0.6F, -0.6F, 0.6F);
        this.head.zRot = tilt * 0.2F * ground;

        // --- hopping along the ground
        this.body.y -= Math.abs(Mth.sin(pos)) * 1.2F * walk;
        this.leftLeg.y -= Math.abs(Mth.sin(pos)) * 1.2F * walk;
        this.rightLeg.y -= Math.abs(Mth.sin(pos)) * 1.2F * walk;
        this.leftLeg.xRot = Mth.cos(pos) * 0.6F * walk;
        this.rightLeg.xRot = Mth.cos(pos) * 0.6F * walk;
        // S1 land: the fluffy little body squashes as it lands and stretches as it springs off again
        float squash = (1.0F - Math.abs(Mth.sin(pos))) * 0.12F * walk;
        this.body.yScale = 1.0F - squash;
        this.body.xScale = 1.0F + squash * 0.6F;
        this.body.zScale = 1.0F + squash * 0.6F;

        // --- tail wag, spreading in flight
        this.tail.yRot = Mth.sin(age * 0.15F) * 0.12F * ground;
        this.tail.xRot += Mth.sin(age * 0.08F) * 0.05F + fly * 0.25F;
        this.tailFan.xScale = 1.0F + fly * 0.25F;

        // --- flight: hard wing beats, legs tucked, body pitched forward
        float beat = Mth.sin(age * 1.6F) * 0.5F + 0.5F;
        this.leftWing.zRot = -(0.15F + beat * 1.25F) * fly;
        this.rightWing.zRot = (0.15F + beat * 1.25F) * fly;
        this.leftWing.xRot = -0.1F * fly;
        this.rightWing.xRot = -0.1F * fly;
        this.leftLeg.xRot += 1.0F * fly;
        this.rightLeg.xRot += 1.0F * fly;
        this.body.xRot = 0.25F * fly;
        this.head.xRot -= 0.25F * fly;
        // folded wings give a little shuffle now and then while perched
        float shuffle = Math.max(0.0F, Mth.sin(age * 0.05F) - 0.92F) * 6.0F * ground;
        this.leftWing.zRot -= shuffle * 0.35F;
        this.rightWing.zRot += shuffle * 0.35F;

        // --- crest: raised while leading the way
        float crestUp = s.guiding ? 1.0F : 0.0F;

        // --- singing: head back, beak chattering, crest fanned, chest puffed
        float t = Anim.seconds(s.sing, s.ageInTicks);
        if (t >= 0.0F && t < 1.6F) {
            float open = Anim.envelope(t, 0.0F, 0.1F, 1.05F, 0.35F);
            this.head.xRot -= 0.5F * open;
            this.jaw.xRot += (0.2F + 0.4F * Math.max(0.0F, Mth.sin(s.ageInTicks * 1.6F))) * open;
            crestUp = Math.max(crestUp, open);
            this.body.xScale = 1.0F + 0.07F * open;
            this.body.zScale = 1.0F + 0.07F * open;
            this.leftWing.zRot -= 0.2F * open * ground;
            this.rightWing.zRot += 0.2F * open * ground;
        }
        this.crest.xRot += 0.45F * crestUp;
        this.plumes[0].zRot -= 0.3F * crestUp;
        this.plumes[2].zRot += 0.3F * crestUp;
        this.plumes[1].xRot = Mth.sin(age * 0.3F) * 0.08F * crestUp;

        // --- pecking for seeds: a quick bob down to the ground and back
        float peck = Anim.seconds(s.peck, s.ageInTicks);
        if (peck >= 0.0F && peck < 0.5F) {
            float e = Anim.envelope(peck, 0.0F, 0.08F, 0.06F, 0.3F);
            this.body.xRot += 0.55F * e;
            this.head.xRot += 0.6F * e;
            this.tail.xRot -= 0.4F * e;
            this.jaw.xRot += 0.25F * Anim.envelope(peck, 0.1F, 0.04F, 0.03F, 0.1F);
        }
        // --- preening a flock-mate: the head turns aside and the beak nibbles
        float preen = Anim.seconds(s.preen, s.ageInTicks);
        if (preen >= 0.0F && preen < 2.2F) {
            float e = Anim.envelope(preen, 0.0F, 0.25F, 1.5F, 0.4F);
            this.head.yRot += 0.9F * e;
            this.head.xRot += 0.35F * e;
            this.jaw.xRot += Math.max(0.0F, Mth.sin(preen * 24.0F)) * 0.25F * e;
            this.leftWing.zRot -= 0.15F * e;
            this.crest.xRot -= 0.2F * e;
        }
        // --- roosting: fluffed up into a ball, head tucked back under a wing, legs hidden
        if (s.roosting) {
            this.body.xScale = this.body.zScale = 1.12F;
            this.body.yScale = 1.06F + Mth.sin(age * 0.05F) * 0.015F;
            this.body.y += 1.5F;
            this.head.yRot = 2.4F;
            this.head.xRot = 0.45F;
            this.head.zRot = 0.2F;
            this.jaw.xRot = 0.0F;
            this.crest.xRot = -0.5F;
            this.leftWing.zRot = 0.08F;
            this.rightWing.zRot = -0.08F;
            this.tail.xRot -= 0.2F;
            this.leftLeg.visible = false;
            this.rightLeg.visible = false;
        } else {
            this.leftLeg.visible = true;
            this.rightLeg.visible = true;
        }

        // --- hurt: crest slicked back, wings jolt open
        if (s.hasRedOverlay) {
            this.crest.xRot -= 0.6F;
            this.leftWing.zRot -= 0.6F;
            this.rightWing.zRot += 0.6F;
            this.head.xRot += 0.25F;
        }
    }
}
