package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Tubafish: it swells like a balloon (with a jelly wobble) and its folded spikes spring out; the
 * tuba bell on its back throbs, the little fins flutter and the mouthpiece pouts.
 */
public class TubafishModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart mouth;
    private final ModelPart tuba;
    private final ModelPart tubaBell;
    private final ModelPart tail;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] spikes = new ModelPart[13];
    private final ModelPart[] tentacles = new ModelPart[6];

    public TubafishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.mouth = this.body.getChild("mouth");
        this.tuba = this.body.getChild("tuba");
        this.tubaBell = this.tuba.getChild("tuba_bell");
        this.tail = this.body.getChild("tail");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        for (int i = 0; i < this.spikes.length; i++) {
            this.spikes[i] = this.body.getChild("spike_" + i);
        }
        for (int i = 0; i < this.tentacles.length; i++) {
            this.tentacles[i] = this.tubaBell.getChild("tentacle_" + i);
        }
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float p = s.puff;
        // swelling with a wobble while it changes size
        float wobble = Mth.sin(s.ageInTicks * 0.9F) * 0.06F * p * (1.0F - p) * 4.0F;
        float size = 1.0F + 0.75F * p;
        this.body.xScale = size * (1.0F + wobble);
        this.body.yScale = size * (1.0F - wobble);
        this.body.zScale = size * (1.0F + wobble * 0.5F);
        this.body.xRot = s.xRot * Anim.DEG * 0.5F;
        this.body.y -= p * 2.0F;
        for (int i = 0; i < this.spikes.length; i++) {
            float sp = 0.25F + 1.45F * p;
            this.spikes[i].yScale = sp;
            this.spikes[i].xScale = 0.6F + 0.5F * p;
            this.spikes[i].zScale = 0.6F + 0.5F * p;
        }
        float fin = 0.4F + s.effort * 0.6F + p * 0.6F;
        this.leftFin.yRot += Mth.sin(age * (0.5F + p)) * 0.45F * fin;
        this.rightFin.yRot -= Mth.sin(age * (0.5F + p)) * 0.45F * fin;
        this.tail.yRot = Mth.sin(age * (0.3F + s.effort * 0.6F)) * 0.4F;
        // the tuba bell throbs - hard while it blows bubbles
        float throb = Mth.sin(age * (0.15F + p * 0.6F));
        this.tubaBell.xScale = 1.0F + throb * (0.04F + 0.12F * p);
        this.tubaBell.zScale = this.tubaBell.xScale;
        this.tubaBell.yScale = 1.0F - throb * (0.04F + 0.1F * p);
        this.tuba.xRot = Mth.sin(age * 0.08F) * 0.06F;
        // the anemone crown waves, and flares wide when it puffs
        for (int i = 0; i < this.tentacles.length; i++) {
            float a = i * (float) Math.PI / 3.0F;
            float wave = Mth.sin(age * (0.16F + p * 0.3F) + i * 1.1F) * (0.18F + 0.2F * p);
            this.tentacles[i].xRot += -Mth.sin(a) * (wave + 0.4F * p);
            this.tentacles[i].zRot += Mth.cos(a) * (wave + 0.4F * p);
            this.tentacles[i].yScale = 1.0F + Mth.sin(age * 0.2F + i) * 0.1F + 0.2F * p;
        }
        this.mouth.zScale = 1.0F + Math.max(0.0F, Mth.sin(age * 0.2F)) * 0.3F;
        if (!s.inLiquid) {
            this.body.zRot = (float) Math.PI * 0.5F * 0.8F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 1.1F) * 0.7F;
        }
        if (s.hasRedOverlay) {
            this.body.xScale *= 1.08F;
            this.body.zScale *= 1.08F;
            this.body.yScale *= 0.9F;
        }
    }
}
