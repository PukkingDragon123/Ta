package com.thesift.client.model;

import com.thesift.client.renderer.state.SkyWhaleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Sky Whale: a slow vertical wave rolls down the tail to the flukes, the flipper-wings beat like a
 * bird's, the fur cushions and ears ripple in the wind; when it sings the jaw falls open and the
 * head lifts, and when it spits a gem it gives one big gulp first.
 */
public class SkyWhaleModel extends EntityModel<SkyWhaleRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart noseRing;
    private final ModelPart leftFlipper;
    private final ModelPart rightFlipper;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftCheek;
    private final ModelPart rightCheek;
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart[] tufts = new ModelPart[3];

    public SkyWhaleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
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
        for (int i = 0; i < 3; i++) {
            this.tufts[i] = this.body.getChild("back_tuft_" + i);
        }
    }

    @Override
    public void setupAnim(SkyWhaleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float swim = age * 0.06F;
        this.body.xRot = Mth.sin(swim) * 0.04F;
        this.body.y += Mth.sin(swim) * 1.2F;
        for (int i = 0; i < 4; i++) {
            this.tail[i].xRot = Mth.sin(swim - 0.8F * (i + 1)) * (0.1F + 0.06F * i);
        }
        this.tail[0].yRot = Mth.sin(age * 0.02F) * 0.05F;
        float flap = Mth.sin(age * 0.09F);
        this.leftFlipper.zRot += flap * 0.45F;
        this.rightFlipper.zRot -= flap * 0.45F;
        this.leftFlipper.xRot = Mth.cos(age * 0.09F) * 0.12F;
        this.rightFlipper.xRot = Mth.cos(age * 0.09F) * 0.12F;
        this.leftEar.zRot += Mth.sin(age * 0.13F) * 0.12F;
        this.rightEar.zRot -= Mth.sin(age * 0.13F + 0.5F) * 0.12F;
        this.leftCheek.zRot -= Mth.sin(age * 0.07F) * 0.05F;
        this.rightCheek.zRot += Mth.sin(age * 0.07F) * 0.05F;
        for (int i = 0; i < 3; i++) {
            this.tufts[i].yScale = 1.0F + Mth.sin(age * 0.08F - i * 0.9F) * 0.08F;
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
            this.head.z -= 2.0F * heave;
        }
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
