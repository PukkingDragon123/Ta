package com.thesift.client.model;

import com.thesift.client.renderer.state.SkyWhaleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Sky Whale: a slow vertical wave rolls from the head down the tail to the flukes, so the whole
 * body undulates as one; it banks into its turns and pitches with its climbs, the flipper-wings
 * sweep in long strokes, the fur cushions, ears and meadow ripple in the wind. When it sings the
 * jaw falls open on its baleen comb, the belly grooves balloon out like a rorqual's and the flukes
 * keep time; when it spits a gem it gives one big gulp first.
 */
public class SkyWhaleModel extends EntityModel<SkyWhaleRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart pleats;
    private final ModelPart dorsal;
    private final ModelPart leftTailFin;
    private final ModelPart rightTailFin;
    private final ModelPart noseRing;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftCheek;
    private final ModelPart rightCheek;
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart[] tufts = new ModelPart[3];
    /** The meadow on its back (and the bloom on its brow). */
    private final ModelPart[] flowers = new ModelPart[11];
    /** Glowbell vines and lullwood strands trailing from its flanks and flippers. */
    private final ModelPart[] vines = new ModelPart[8];

    public SkyWhaleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.pleats = this.body.getChild("throat_pleats");
        this.noseRing = this.head.getChild("snout").getChild("nose_ring");
        this.leftFlipper = this.body.getChild("left_flipper");
        this.rightFlipper = this.body.getChild("right_flipper");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftCheek = this.head.getChild("left_cheek");
        this.rightCheek = this.head.getChild("right_cheek");
        this.tail[0] = this.body.getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail3");
        this.tail[3] = this.tail[2].getChild("flukes");
        this.dorsal = this.tail[0].getChild("dorsal_fin");
        this.leftTailFin = this.tail[0].getChild("left_tail_fin");
        this.rightTailFin = this.tail[0].getChild("right_tail_fin");
        for (int i = 0; i < 3; i++) {
            this.tufts[i] = this.body.getChild("back_tuft_" + i);
        }
        for (int i = 0; i < 10; i++) {
            this.flowers[i] = this.body.getChild("back_flower_" + i);
        }
        for (int i = 0; i < 6; i++) {
            this.vines[i] = this.body.getChild("vine_" + i);
        }
        this.flowers[10] = this.head.getChild("head_flower");
        this.vines[6] = this.leftFlipper.getChild("left_flipper_vine");
        this.vines[7] = this.rightFlipper.getChild("right_flipper_vine");
    }

    @Override
    public void setupAnim(SkyWhaleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float swim = age * 0.055F;
        // one travelling wave: the head leads, the body rises and falls, the tail follows a beat later
        float bank = s.bank * Anim.DEG * 2.2F;
        this.body.xRot = Mth.sin(swim) * 0.035F - s.climb * 0.5F;
        this.body.zRot = -bank;
        this.body.y += Mth.sin(swim) * 1.4F;
        for (int i = 0; i < 4; i++) {
            float amp = 0.06F + 0.05F * i;
            this.tail[i].xRot = Mth.sin(swim - 0.75F * (i + 1)) * amp + s.climb * 0.25F;
            // the tail swings out of the turn, like a rudder
            this.tail[i].yRot = bank * 0.35F * (i + 1) * 0.5F + Mth.sin(age * 0.02F - i * 0.5F) * 0.03F;
        }
        float stroke = age * 0.07F;
        float flap = Mth.sin(stroke);
        // long, slow wing-strokes: a quick, powerful down-sweep and a lazy recovery, the trailing edge lagging
        float sweep = flap + 0.25F * Mth.sin(stroke * 2.0F);
        this.leftFlipper.zRot += sweep * 0.42F - bank * 0.4F;
        this.rightFlipper.zRot -= sweep * 0.42F + bank * 0.4F;
        this.leftFlipper.xRot = Mth.cos(stroke) * 0.14F;
        this.rightFlipper.xRot = Mth.cos(stroke) * 0.14F;
        this.leftFlipper.yRot = Mth.sin(stroke - 0.8F) * 0.08F;
        this.rightFlipper.yRot = -Mth.sin(stroke - 0.8F) * 0.08F;
        this.dorsal.zRot = -bank * 0.6F + Mth.sin(swim - 1.4F) * 0.04F;
        this.leftTailFin.zRot += Mth.sin(swim - 1.2F) * 0.15F;
        this.rightTailFin.zRot -= Mth.sin(swim - 1.2F) * 0.15F;
        this.leftEar.zRot += Mth.sin(age * 0.13F) * 0.12F;
        this.rightEar.zRot -= Mth.sin(age * 0.13F + 0.5F) * 0.12F;
        this.leftCheek.zRot -= Mth.sin(age * 0.07F) * 0.05F;
        this.rightCheek.zRot += Mth.sin(age * 0.07F) * 0.05F;
        for (int i = 0; i < 3; i++) {
            this.tufts[i].zRot = Mth.sin(age * 0.05F - i) * 0.03F;
        }
        this.noseRing.xRot += Mth.sin(age * 0.1F) * 0.2F;
        this.head.xRot = s.xRot * Anim.DEG * 0.3F + Mth.sin(swim + 0.5F) * 0.03F;
        this.head.yRot = s.yRot * Anim.DEG * 0.25F;

        // singing: the jaw falls open, the head lifts, the flippers spread wide
        float sing = Anim.seconds(s.sing, s.ageInTicks);
        if (sing >= 0.0F && sing < 4.0F) {
            float e = Anim.envelope(sing, 0.0F, 0.6F, 2.2F, 1.0F);
            float warble = Mth.sin(sing * 9.0F) * 0.06F * e;
            this.jaw.xRot += 0.5F * e + warble;
            this.head.xRot -= 0.2F * e;
            // the belly grooves balloon out with the song, swelling on each phrase
            float swell = e * (0.6F + 0.4F * Mth.sin(sing * 2.6F));
            this.pleats.yScale = 1.0F + 1.4F * swell;
            this.pleats.xScale = 1.0F + 0.08F * swell;
            this.leftCheek.xScale = 1.0F + 0.2F * swell;
            this.rightCheek.xScale = 1.0F + 0.2F * swell;
            this.dorsal.xRot += 0.15F * e;
            this.leftFlipper.zRot -= 0.35F * e;
            this.rightFlipper.zRot += 0.35F * e;
            this.tail[3].xRot += Mth.sin(sing * 3.0F) * 0.2F * e;
            this.body.yScale = 1.0F + 0.03F * e * Mth.sin(sing * 4.0F);
        }
        // spitting the gem: a gulp (cheeks and body swell), then a big open-mouthed heave
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        if (spit >= 0.0F && spit < 1.6F) {
            float gulp = Anim.envelope(spit, 0.0F, 0.3F, 0.2F, 0.1F);
            float heave = Anim.envelope(spit, 0.55F, 0.08F, 0.3F, 0.5F);
            this.body.xScale = 1.0F + 0.06F * gulp - 0.03F * heave;
            this.body.zScale = 1.0F - 0.03F * gulp + 0.05F * heave;
            this.leftCheek.xScale = 1.0F + 0.5F * gulp;
            this.rightCheek.xScale = 1.0F + 0.5F * gulp;
            this.jaw.xRot = Math.max(this.jaw.xRot, 0.85F * heave);
            this.head.xRot -= 0.15F * heave;
            // a recoil of the whole whale, rather than pushing the head off its neck
            this.body.z += 1.5F * heave;
        }
        // the meadow leans back in the wind and nods; the vines trail behind and swing
        for (int i = 0; i < this.flowers.length; i++) {
            float ph = i * 1.9F;
            this.flowers[i].xRot = 0.18F + Mth.sin(age * 0.09F + ph) * 0.08F;
            this.flowers[i].zRot = Mth.sin(age * 0.07F + ph) * 0.12F;
            this.flowers[i].yScale = 1.0F + Mth.sin(age * 0.05F + ph) * 0.05F;
        }
        for (int i = 0; i < this.vines.length; i++) {
            float ph = i * 1.3F;
            this.vines[i].xRot = 0.3F + Mth.sin(age * 0.06F - ph) * 0.14F;
            this.vines[i].zRot = Mth.sin(age * 0.045F + ph) * 0.1F;
        }
        // the flipper vines stay hanging while the flippers beat
        this.vines[6].zRot -= flap * 0.4F + 0.3F;
        this.vines[7].zRot += flap * 0.4F + 0.3F;

        if (s.hasRedOverlay) {
            this.head.xRot += 0.15F;
            this.leftFlipper.zRot -= 0.3F;
            this.rightFlipper.zRot += 0.3F;
        }
        float die = Anim.smooth(s.dying / 16.0F);
        if (die > 0.0F) {
            this.body.zRot = (float) Math.PI * die;
            this.jaw.xRot = Math.max(this.jaw.xRot, die * 0.4F);
        }
    }
}
