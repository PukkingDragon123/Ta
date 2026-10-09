package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftSnifferRenderState;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.animation.definitions.SnifferAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * E1 the Sift Sniffer (and its Snifflet): Mojang's Sniffer geometry part for part (tools/sift_sniffer.py),
 * so Mojang's own Sniffer keyframes drive it - the six-legged walk, the nose-down search, the long
 * sniff, the dig and the happy head-toss - with the Sift's moods layered on top: idle nose twitches
 * (vanilla's scenting), grazing (head down, the beak chewing), the trumpet (vanilla's happy toss with
 * the nose plate raised and the beak open), the ram (head down, a front foot pawing, ears back),
 * laying an egg (a long squat), the garden on its back and the fur fringes round its belly swaying a
 * beat behind, and the rot (head and flowers drooping). The Snifflet wears vanilla's baby transform.
 */
public class SiftSnifferModel extends EntityModel<SiftSnifferRenderState> {
    private static final float WALK_SPEED = 9.0F;
    private static final float WALK_SCALE = 100.0F;
    /** Our dig is shorter than vanilla's (3.5 s of DIGGING): its keyframes play this much faster. */
    private static final float DIG_SPEED = 1.6F;
    private static final String[] LEGS = {"right_front_leg", "right_mid_leg", "right_hind_leg", "left_front_leg", "left_mid_leg", "left_hind_leg"};
    private static final String[] FRINGES = {"left_fringe", "right_fringe", "front_fringe", "back_fringe"};

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart nose;
    private final ModelPart lowerBeak;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart[] legs = new ModelPart[6];
    /** left, right, front, back (the Snifflet has no front one: null). */
    private final ModelPart[] fringes = new ModelPart[4];
    private final ModelPart[] flowers;
    private final ModelPart[] tufts;
    private final KeyframeAnimation walk;
    private final KeyframeAnimation search;
    private final KeyframeAnimation dig;
    private final KeyframeAnimation longSniff;
    private final KeyframeAnimation happy;
    private final KeyframeAnimation scent;
    private final KeyframeAnimation babyTransform;

    public SiftSnifferModel(ModelPart root, boolean baby) {
        super(root);
        this.body = root.getChild("bone").getChild("body");
        this.head = this.body.getChild("head");
        this.nose = this.head.getChild("nose");
        this.lowerBeak = this.head.getChild("lower_beak");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        ModelPart bone = root.getChild("bone");
        for (int i = 0; i < 6; i++) {
            this.legs[i] = bone.getChild(LEGS[i]);
        }
        for (int i = 0; i < 4; i++) {
            this.fringes[i] = this.body.hasChild(FRINGES[i]) ? this.body.getChild(FRINGES[i]) : null;
        }
        ModelPart garden = this.body.getChild("garden");
        this.flowers = children(garden, "flower_");
        this.tufts = children(garden, "tuft_");
        this.walk = SnifferAnimation.SNIFFER_WALK.bake(root);
        this.search = SnifferAnimation.SNIFFER_SNIFF_SEARCH.bake(root);
        this.dig = SnifferAnimation.SNIFFER_DIG.bake(root);
        this.longSniff = SnifferAnimation.SNIFFER_LONGSNIFF.bake(root);
        this.happy = SnifferAnimation.SNIFFER_HAPPY.bake(root);
        this.scent = SnifferAnimation.SNIFFER_SNIFFSNIFF.bake(root);
        this.babyTransform = baby ? SnifferAnimation.BABY_TRANSFORM.bake(root) : null;
    }

    private static ModelPart[] children(ModelPart parent, String prefix) {
        int n = 0;
        while (parent.hasChild(prefix + n)) {
            n++;
        }
        ModelPart[] out = new ModelPart[n];
        for (int i = 0; i < n; i++) {
            out[i] = parent.getChild(prefix + i);
        }
        return out;
    }

    @Override
    public void setupAnim(SiftSnifferRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float alive = 1.0F - s.rot * 0.6F;
        float moving = Math.min(1.0F, s.walkAnimationSpeed * 1.8F);
        float pos = s.walkAnimationPos;
        float sn = Anim.seconds(s.sniff, s.ageInTicks);
        float d = Anim.seconds(s.dig, s.ageInTicks);
        float t = Anim.seconds(s.trumpet, s.ageInTicks);
        boolean busy = sn >= 0.0F || d >= 0.0F;

        // --- looking around, then Mojang's walk (the nose-down search walk while it is sniffing)
        this.head.xRot = s.xRot * Anim.DEG;
        this.head.yRot = s.yRot * Anim.DEG;
        (sn >= 0.0F ? this.search : this.walk).applyWalk(pos, s.walkAnimationSpeed, WALK_SPEED, WALK_SCALE);

        // --- the nose never quite rests: vanilla's scenting twitches, out of step from Sniffer to Sniffer
        if (!busy) {
            this.scent.apply((long) (age * 50.0F), alive);
        }

        // --- the fringes and the garden sway a beat behind the body
        float sway = Mth.sin(pos * 0.9F - 0.8F) * moving;
        float idle = Mth.sin(age * 0.06F);
        if (this.fringes[0] != null) {
            this.fringes[0].zRot -= sway * 0.12F + idle * 0.02F;
        }
        if (this.fringes[1] != null) {
            this.fringes[1].zRot += sway * 0.12F + idle * 0.02F;
        }
        if (this.fringes[2] != null) {
            this.fringes[2].xRot += Mth.cos(pos * 0.9F - 0.8F) * 0.1F * moving + idle * 0.02F;
        }
        if (this.fringes[3] != null) {
            this.fringes[3].xRot -= Mth.cos(pos * 0.9F - 0.8F) * 0.1F * moving + idle * 0.02F;
        }
        for (int i = 0; i < this.flowers.length; i++) {
            this.flowers[i].visible = i < s.garden;
            this.flowers[i].zRot += Mth.sin(age * 0.06F + i * 1.3F) * 0.07F + Mth.sin(pos * 0.9F - 1.2F + i * 0.4F) * 0.12F * moving;
            this.flowers[i].xRot += Mth.cos(age * 0.05F + i) * 0.05F + 0.7F * s.rot;
        }
        for (int i = 0; i < this.tufts.length; i++) {
            this.tufts[i].zRot += Mth.sin(age * 0.08F + i * 2.1F) * 0.06F + Mth.sin(pos * 0.9F - 1.0F) * 0.08F * moving;
            this.tufts[i].xRot += 0.5F * s.rot;
        }
        // the ears hang and sway a little at rest
        this.leftEar.zRot -= Math.max(0.0F, Mth.sin(age * 0.045F)) * 0.06F * alive;
        this.rightEar.zRot += Math.max(0.0F, Mth.sin(age * 0.045F + 1.7F)) * 0.06F * alive;

        // --- grazing: down to the grass, the beak chewing
        float g = Anim.seconds(s.graze, s.ageInTicks);
        if (g >= 0.0F) {
            float e = Anim.envelope(g, 0.0F, 0.35F, 1.6F, 0.4F);
            this.head.xRot += 0.85F * e;
            this.head.y += 2.0F * e;
            this.lowerBeak.xRot += (0.12F + Mth.sin(g * 15.0F) * 0.12F) * e;
            this.nose.xRot -= Math.max(0.0F, Mth.sin(g * 15.0F)) * 0.06F * e;
            this.leftEar.zRot -= 0.15F * e;
            this.rightEar.zRot += 0.15F * e;
        }

        // --- sniffing: nose to the ground, sweeping side to side, with vanilla's long sniffs
        if (sn >= 0.0F) {
            float e = Anim.envelope(sn, 0.0F, 0.3F, 10.0F, 0.3F);
            this.head.xRot += 0.55F * e;
            this.head.y += 1.0F * e;
            this.head.yRot += Mth.sin(sn * 3.0F) * 0.3F * e;
            this.longSniff.apply((long) ((sn % 1.25F) * 1000.0F), e);
        }

        // --- digging: vanilla's dig, sped up to fit, fading back up at the end
        if (d >= 0.0F) {
            this.dig.apply((long) (d * 1000.0F * DIG_SPEED), Anim.envelope(d, 0.0F, 0.15F, 3.0F, 0.35F));
        }

        // --- the ram: head down, ears pinned back, a front foot pawing the ground
        float w = Anim.seconds(s.windup, s.ageInTicks);
        if (w >= 0.0F) {
            float e = Anim.envelope(w, 0.0F, 0.25F, 10.0F, 0.2F);
            this.head.xRot += 0.5F * e;
            this.head.y += 1.5F * e;
            this.body.z += 1.0F * e;
            float paw = Math.max(0.0F, Mth.sin(w * 12.0F));
            this.legs[0].xRot += (-0.45F - paw * 0.45F) * e;
            this.legs[0].y -= paw * 1.5F * e;
            this.leftEar.xRot += 0.5F * e;
            this.rightEar.xRot += 0.5F * e;
            this.leftEar.zRot -= 0.25F * e;
            this.rightEar.zRot += 0.25F * e;
        }
        if (s.charging) {
            this.head.xRot += 0.45F;
            this.head.y += 1.0F;
            this.leftEar.xRot += 0.8F;
            this.rightEar.xRot += 0.8F;
        }

        // --- trumpeting: vanilla's happy head-toss and ear flaps, the nose plate raised and the beak open
        if (t >= 0.0F && t < 1.6F) {
            float blow = Anim.envelope(t, 0.1F, 0.15F, 0.85F, 0.5F);
            this.happy.apply((long) (t * 1000.0F), 1.0F);
            this.nose.xRot -= 0.45F * blow + Mth.sin(t * 40.0F) * 0.03F * blow;
            this.lowerBeak.xRot += 0.35F * blow;
            this.body.y += 0.4F * Anim.envelope(t, 0.0F, 0.1F, 0.0F, 0.15F);
        }

        // --- laying an egg: a long, careful squat
        float l = Anim.seconds(s.lay, s.ageInTicks);
        if (l >= 0.0F && l < 2.0F) {
            float e = Anim.envelope(l, 0.0F, 0.4F, 0.8F, 0.6F);
            this.body.y += 3.0F * e;
            this.head.xRot += 0.15F * e;
        }

        // --- rotting: lifeless, everything hangs
        if (s.rot > 0.0F) {
            this.head.xRot += 0.35F * s.rot;
            this.head.y += 1.0F * s.rot;
            this.body.y += 0.8F * s.rot;
            this.nose.xRot += 0.1F * s.rot;
        }

        // --- the Snifflet: vanilla's baby head (bigger, a little higher and further back)
        if (this.babyTransform != null) {
            this.babyTransform.applyStatic();
        }
    }
}
