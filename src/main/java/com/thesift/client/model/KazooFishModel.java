package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** Kazoo Fish: a quick tail beat, googly eyes that wobble on their own, a buzzing kazoo snout. */
public class KazooFishModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart dorsal;
    private final ModelPart kazoo;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart tuft;
    private final ModelPart sprout;

    public KazooFishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.dorsal = this.body.getChild("dorsal");
        this.kazoo = this.body.getChild("kazoo");
        this.leftEye = this.body.getChild("left_eye");
        this.rightEye = this.body.getChild("right_eye");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.tuft = this.body.getChild("tuft");
        this.sprout = this.tuft.getChild("tuft_sprout");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.effort;
        float beat = age * (0.45F + e * 0.9F);
        this.tail.yRot = Mth.sin(beat) * (0.35F + e * 0.45F);
        this.body.yRot = -Mth.sin(beat) * 0.06F * (1.0F + e);
        this.body.xRot = s.xRot * Anim.DEG;
        this.dorsal.zRot = Mth.sin(beat * 0.5F) * 0.1F;
        this.leftFin.zRot += Mth.sin(age * 0.5F) * 0.35F;
        this.rightFin.zRot -= Mth.sin(age * 0.5F + 0.4F) * 0.35F;
        // googly eyes, each on its own wobble
        this.leftEye.zRot = Mth.sin(age * 0.37F) * 0.25F;
        this.rightEye.zRot = Mth.sin(age * 0.53F + 1.0F) * 0.3F;
        this.leftEye.xRot = Mth.cos(age * 0.29F) * 0.15F;
        // the kazoo buzzes now and then
        float buzz = Math.max(0.0F, Mth.sin(age * 0.07F));
        buzz = buzz > 0.9F ? (buzz - 0.9F) * 10.0F : 0.0F;
        this.kazoo.xScale = 1.0F + Mth.sin(age * 4.0F) * 0.08F * buzz;
        this.kazoo.yScale = this.kazoo.xScale;
        this.kazoo.zScale = 1.0F + 0.15F * buzz;
        // the moss tuft's sprout trails and wobbles in the current
        this.sprout.xRot = 0.15F + e * 0.35F + Mth.sin(age * 0.23F) * 0.12F;
        this.sprout.zRot = Mth.sin(age * 0.31F + 0.7F) * 0.2F;
        this.tuft.yScale = 1.0F + Mth.sin(age * 0.2F) * 0.08F;
        this.dorsal.xRot = e * 0.2F + Mth.sin(beat * 0.5F + 1.0F) * 0.08F;
        if (!s.inLiquid) {
            this.body.zRot = (float) Math.PI * 0.5F;
            this.body.y += 1.5F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 1.2F) * 0.8F;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = (float) Math.PI * die;
        }
    }
}
