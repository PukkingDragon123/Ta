package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftSnifferRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * E1 the Sift Sniffer: a six-legged tripod amble (the body rolls and bobs, the floppy ears and the
 * garden on its back lag a beat behind), a nose that never stops wiggling, and its moods - grazing
 * (head down, chewing), sniffing (nose sweeping low, nostrils twitching), digging (front feet
 * scraping, the whole body shoving), trumpeting (a dip, then the head flung up, the nose raised and
 * its bell flared, ears lifting, and a wobbly settle), the ram (head down, a hoof pawing the ground,
 * ears flat, then a gallop) and laying an egg (a long squat). Rotting, it droops: head low, ears
 * limp, its flowers wilting over.
 */
public class SiftSnifferModel extends EntityModel<SiftSnifferRenderState> {
    private static final int FLOWERS = 5;
    private static final int TUFTS = 4;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart noseTip;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart tail;
    private final ModelPart headFlower;
    private final ModelPart[] flowers = new ModelPart[FLOWERS];
    private final ModelPart[] tufts = new ModelPart[TUFTS];
    /** front left, middle left, back left, front right, middle right, back right */
    private final ModelPart[] legs = new ModelPart[6];

    public SiftSnifferModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.nose = this.head.getChild("nose");
        this.noseTip = this.nose.getChild("nose_tip");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.headFlower = this.head.getChild("head_flower");
        this.tail = this.body.getChild("tail");
        ModelPart garden = this.body.getChild("garden");
        for (int i = 0; i < FLOWERS; i++) {
            this.flowers[i] = garden.getChild("flower_" + i);
        }
        for (int i = 0; i < TUFTS; i++) {
            this.tufts[i] = garden.getChild("tuft_" + i);
        }
        String[] names = {"front_left_leg", "middle_left_leg", "back_left_leg", "front_right_leg", "middle_right_leg", "back_right_leg"};
        for (int i = 0; i < 6; i++) {
            this.legs[i] = root.getChild(names[i]);
        }
    }

    @Override
    public void setupAnim(SiftSnifferRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float alive = 1.0F - s.rot * 0.6F;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.8F);
        float pos = s.walkAnimationPos * (s.charging ? 1.1F : 0.75F);
        float stride = s.charging ? 0.85F : 0.55F;

        // --- the amble: a tripod gait (front-left, middle-right, back-left together), body rolling over the steps
        for (int i = 0; i < 6; i++) {
            boolean left = i < 3;
            int row = i % 3;
            float phase = ((row % 2 == 0) == left) ? 0.0F : Mth.PI;
            this.legs[i].xRot += Mth.sin(pos + phase) * stride * walk;
            this.legs[i].y -= Math.max(0.0F, Mth.cos(pos + phase)) * 0.8F * walk;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.7F * walk;
        this.body.zRot += Mth.sin(pos) * 0.04F * walk;
        this.body.xRot += (s.charging ? 0.08F : 0.0F);

        // --- looking around (the whole head turns; the nose adds a little of its own)
        this.head.yRot += s.yRot * Anim.DEG * 0.65F;
        this.head.xRot += Mth.clamp(s.xRot * Anim.DEG * 0.5F, -0.4F, 0.4F) + Mth.sin(pos * 2.0F) * 0.05F * walk;
        // the nose never rests: a sniffy wiggle
        this.nose.xRot += Mth.sin(age * 0.17F) * 0.06F * alive;
        this.nose.yRot += Mth.sin(age * 0.11F) * 0.07F * alive;
        // floppy ears lag behind the step; a slow sway at rest
        float flop = Mth.sin(pos - 0.7F) * 0.2F * walk + Mth.sin(age * 0.07F) * 0.04F;
        this.leftEar.zRot += flop;
        this.rightEar.zRot -= flop;
        this.leftEar.xRot += Mth.cos(pos - 0.7F) * 0.12F * walk;
        this.rightEar.xRot += Mth.cos(pos - 0.7F) * 0.12F * walk;
        this.tail.yRot += Mth.sin(age * 0.13F) * 0.18F * alive + Mth.sin(pos) * 0.25F * walk;
        // the garden sways a beat after the body
        for (int i = 0; i < FLOWERS; i++) {
            this.flowers[i].visible = i < s.garden;
            this.flowers[i].zRot += Mth.sin(age * 0.06F + i * 1.3F) * 0.07F + Mth.sin(pos - 1.2F + i * 0.4F) * 0.12F * walk;
            this.flowers[i].xRot += Mth.cos(age * 0.05F + i) * 0.05F + 0.7F * s.rot;
        }
        for (int i = 0; i < TUFTS; i++) {
            this.tufts[i].zRot += Mth.sin(age * 0.08F + i * 2.1F) * 0.06F + Mth.sin(pos - 1.0F) * 0.08F * walk;
            this.tufts[i].xRot += 0.5F * s.rot;
        }
        this.headFlower.zRot += Mth.sin(age * 0.09F) * 0.1F + Mth.sin(pos - 1.4F) * 0.15F * walk + 0.6F * s.rot;

        // --- grazing: down to the grass, a good chew
        float g = Anim.seconds(s.graze, s.ageInTicks);
        if (g >= 0.0F) {
            float e = Anim.envelope(g, 0.0F, 0.35F, 1.6F, 0.4F);
            this.head.xRot += 0.95F * e;
            this.head.y += 2.5F * e;
            this.body.xRot += 0.07F * e;
            this.nose.xRot += (0.25F + Mth.sin(g * 15.0F) * 0.12F) * e;
            this.leftEar.zRot -= 0.25F * e;
            this.rightEar.zRot += 0.25F * e;
        }

        // --- sniffing: nose low, sweeping side to side, nostrils twitching
        float sn = Anim.seconds(s.sniff, s.ageInTicks);
        if (sn >= 0.0F) {
            float e = Anim.envelope(sn, 0.0F, 0.3F, 10.0F, 0.3F);
            this.head.xRot += 0.6F * e;
            this.head.y += 1.5F * e;
            this.head.yRot += Mth.sin(sn * 3.0F) * 0.4F * e;
            this.nose.xRot += (0.3F + Mth.sin(sn * 30.0F) * 0.08F) * e;
            float flare = 1.0F + Math.max(0.0F, Mth.sin(sn * 30.0F)) * 0.12F * e;
            this.noseTip.xScale = flare;
            this.noseTip.yScale = flare;
        }

        // --- digging: shoving its face in, front feet scraping in turn, the whole body heaving
        float d = Anim.seconds(s.dig, s.ageInTicks);
        if (d >= 0.0F) {
            float e = Anim.envelope(d, 0.0F, 0.25F, 10.0F, 0.3F);
            this.head.xRot += 0.9F * e;
            this.head.y += 3.0F * e;
            this.body.xRot += 0.12F * e;
            this.body.y += 0.8F * e;
            this.body.zRot += Mth.sin(d * 18.0F) * 0.03F * e;
            this.nose.xRot += 0.4F * e;
            this.legs[0].xRot += (-0.7F + Mth.sin(d * 11.0F) * 0.6F) * e;
            this.legs[3].xRot += (-0.7F + Mth.sin(d * 11.0F + Mth.PI) * 0.6F) * e;
            this.leftEar.zRot -= Mth.sin(d * 18.0F) * 0.1F * e;
            this.rightEar.zRot += Mth.sin(d * 18.0F) * 0.1F * e;
        }

        // --- the ram: head down, ears pinned back, a hoof pawing the ground
        float w = Anim.seconds(s.windup, s.ageInTicks);
        if (w >= 0.0F) {
            float e = Anim.envelope(w, 0.0F, 0.25F, 10.0F, 0.2F);
            this.head.xRot += 0.55F * e;
            this.head.y += 1.5F * e;
            this.body.z += 1.2F * e;
            this.body.xRot -= 0.05F * e;
            float paw = Math.max(0.0F, Mth.sin(w * 12.0F));
            this.legs[3].xRot += (-0.5F - paw * 0.5F) * e;
            this.legs[3].y -= paw * 1.5F * e;
            this.leftEar.xRot += 0.6F * e;
            this.rightEar.xRot += 0.6F * e;
        }
        if (s.charging) {
            this.head.xRot += 0.5F;
            this.head.y += 1.0F;
            this.leftEar.xRot += 0.9F;
            this.rightEar.xRot += 0.9F;
            this.leftEar.zRot -= 0.3F;
            this.rightEar.zRot += 0.3F;
        }

        // --- trumpeting: a dip, then the head flung up, nose raised, bell flared, ears lifting; a wobbly settle
        float t = Anim.seconds(s.trumpet, s.ageInTicks);
        if (t >= 0.0F && t < 1.6F) {
            float dip = Anim.envelope(t, 0.0F, 0.15F, 0.0F, 0.15F);
            float blow = Anim.envelope(t, 0.15F, 0.15F, 0.75F, 0.5F);
            float settle = Anim.envelope(t, 1.05F, 0.15F, 0.0F, 0.4F);
            this.head.xRot += 0.2F * dip - 0.55F * blow + Mth.sin(t * 14.0F) * 0.06F * settle;
            this.nose.xRot += 0.35F * dip - 1.0F * blow;
            float bell = 1.0F + 0.4F * blow + Mth.sin(t * 45.0F) * 0.05F * blow;
            this.noseTip.xScale = bell;
            this.noseTip.yScale = bell;
            this.noseTip.zScale = 1.0F + 0.6F * blow;
            this.leftEar.zRot -= 0.55F * blow;
            this.rightEar.zRot += 0.55F * blow;
            this.body.y += 0.6F * dip - 0.4F * blow;
        }

        // --- laying an egg: a long, careful squat
        float l = Anim.seconds(s.lay, s.ageInTicks);
        if (l >= 0.0F && l < 2.0F) {
            float e = Anim.envelope(l, 0.0F, 0.4F, 0.8F, 0.6F);
            this.body.y += 2.0F * e;
            this.body.xRot -= 0.06F * e;
            this.tail.xRot -= 0.5F * e;
        }

        // --- rotting: lifeless, everything hangs
        if (s.rot > 0.0F) {
            this.head.xRot += 0.35F * s.rot;
            this.head.y += 1.0F * s.rot;
            this.leftEar.zRot += 0.35F * s.rot;
            this.rightEar.zRot -= 0.35F * s.rot;
            this.body.y += 0.8F * s.rot;
            this.tail.xRot += 0.6F * s.rot;
        }

        // babies: a bigger head on a small body
        if (s.isBaby) {
            this.head.xScale = 1.3F;
            this.head.yScale = 1.3F;
            this.head.zScale = 1.3F;
        }
    }
}
