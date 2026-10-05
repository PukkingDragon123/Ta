package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Kazoo Fish (CR3: a hand-drawn sprite extruded into a rounded body, built at twice vanilla scale - see
 * tools/fish_art.py): a travelling wave down the body into the stem and the fan tail, coral fins that scull
 * and flutter, googly eyes that wobble on their own, a kazoo that buzzes now and then and a sprout that
 * trails in the current.
 */
public class KazooFishModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart tailStem;
    private final ModelPart tail;
    private final ModelPart analFin;
    private final ModelPart leftPelvic;
    private final ModelPart rightPelvic;
    private final ModelPart dorsal;
    private final ModelPart kazoo;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart tuft;
    private final ModelPart sprout;
    private final ModelPart sprout2;

    public KazooFishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.tailStem = this.body.getChild("tail_stem");
        this.tail = this.tailStem.getChild("tail");
        this.analFin = this.body.getChild("anal_fin");
        this.leftPelvic = this.body.getChild("left_pelvic");
        this.rightPelvic = this.body.getChild("right_pelvic");
        this.dorsal = this.body.getChild("dorsal");
        this.kazoo = this.body.getChild("kazoo");
        this.leftEye = this.body.getChild("left_eye");
        this.rightEye = this.body.getChild("right_eye");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.tuft = this.body.getChild("tuft");
        this.sprout = this.tuft.getChild("tuft_sprout");
        this.sprout2 = this.tuft.getChild("tuft_sprout2");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.effort;
        float beat = age * (0.45F + e * 0.9F);
        float amp = 0.3F + e * 0.4F;
        // a travelling wave from the snout back: body, tail stem, then the fan tail a beat later
        this.body.yRot = -Mth.sin(beat) * 0.07F * (1.0F + e);
        this.tailStem.yRot = Mth.sin(beat - 0.7F) * amp * 0.6F;
        this.tail.yRot = Mth.sin(beat - 1.4F) * amp;
        this.analFin.yRot = Mth.sin(beat - 1.0F) * amp * 0.4F;
        this.body.xRot = s.xRot * Anim.DEG;
        // bursts of speed bank the body into the stroke
        this.body.zRot = Mth.sin(beat * 0.5F) * 0.05F * e;
        // pectoral fins scull, pelvic fins flutter for balance
        float scull = Mth.sin(age * (0.5F + e * 0.4F));
        this.leftFin.yRot += scull * 0.35F;
        this.rightFin.yRot -= scull * 0.35F;
        this.leftFin.zRot += Mth.sin(age * 0.5F + 0.6F) * 0.2F;
        this.rightFin.zRot -= Mth.sin(age * 0.5F + 1.0F) * 0.2F;
        this.leftPelvic.zRot += Mth.sin(age * 0.7F) * 0.25F;
        this.rightPelvic.zRot -= Mth.sin(age * 0.7F + 0.5F) * 0.25F;
        this.dorsal.zRot = Mth.sin(beat * 0.5F) * 0.12F;
        this.dorsal.xRot = e * 0.25F + Mth.sin(beat * 0.5F + 1.0F) * 0.06F;
        // googly eyes, each on its own wobble
        this.leftEye.zRot = Mth.sin(age * 0.37F) * 0.25F;
        this.rightEye.zRot = Mth.sin(age * 0.53F + 1.0F) * 0.3F;
        this.leftEye.xRot = Mth.cos(age * 0.29F) * 0.15F;
        this.rightEye.yRot = Mth.cos(age * 0.41F) * 0.2F;
        // the kazoo buzzes now and then
        float buzz = Math.max(0.0F, Mth.sin(age * 0.07F));
        buzz = buzz > 0.9F ? (buzz - 0.9F) * 10.0F : 0.0F;
        this.kazoo.xScale = 1.0F + Mth.sin(age * 4.0F) * 0.08F * buzz;
        this.kazoo.yScale = this.kazoo.xScale;
        this.kazoo.zScale = 1.0F + 0.15F * buzz;
        // the sprout trails and wobbles in the current
        this.sprout.xRot = 0.15F + e * 0.35F + Mth.sin(age * 0.23F) * 0.12F;
        this.sprout.zRot = Mth.sin(age * 0.31F + 0.7F) * 0.2F;
        this.sprout2.xRot = this.sprout.xRot;
        this.sprout2.zRot = this.sprout.zRot;
        this.tuft.yScale = 1.0F + Mth.sin(age * 0.2F) * 0.08F;
        if (!s.inLiquid) {
            // stranded: on its side, flopping
            this.body.zRot = (float) Math.PI * 0.5F;
            this.body.y += 3.0F;
            this.tailStem.yRot = Mth.sin(s.ageInTicks * 1.2F) * 0.5F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 1.2F - 0.8F) * 0.8F;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = (float) Math.PI * die;
        }
    }
}
