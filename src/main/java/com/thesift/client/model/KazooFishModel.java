package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * S2: the Kazoo Fish (geometry and hand-painted variants in tools/waterfolk.py), swimming like vanilla's
 * tropical fish: a travelling wave from the head back through the peduncle into the forked tail, quicker
 * and deeper the harder it swims; it pitches with its course and rolls a touch into each stroke. The
 * pectorals scull (fastest when it hovers), fold back at speed; the pelvics flutter; the dorsal sail lies
 * back as it darts. The kazoo buzzes now and then. Stranded, it lies on its side and flaps; hurt, it
 * curls; dying, it rolls belly-up.
 */
public class KazooFishModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart kazoo;
    private final ModelPart dorsal;
    private final ModelPart analFin;
    private final ModelPart tailStem;
    private final ModelPart tail;
    private final ModelPart[] fins = new ModelPart[2];
    private final ModelPart[] pelvics = new ModelPart[2];

    public KazooFishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.kazoo = this.body.getChild("kazoo");
        this.dorsal = this.body.getChild("dorsal");
        this.analFin = this.body.getChild("anal_fin");
        this.tailStem = this.body.getChild("tail_stem");
        this.tail = this.tailStem.getChild("tail");
        this.fins[0] = this.body.getChild("left_fin");
        this.fins[1] = this.body.getChild("right_fin");
        this.pelvics[0] = this.body.getChild("left_pelvic");
        this.pelvics[1] = this.body.getChild("right_pelvic");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.inLiquid ? s.effort : 1.0F;
        float beat = age * (0.35F + 0.55F * e);
        float amp = 0.25F + 0.35F * e;
        // the travelling wave: the head yaws a little against the stroke, the stem and the fan follow a beat behind
        this.body.yRot = -Mth.sin(beat) * 0.06F * (1.0F + e);
        this.tailStem.yRot = Mth.sin(beat - 0.8F) * amp * 0.7F;
        this.tail.yRot = Mth.sin(beat - 1.6F) * amp;
        this.analFin.yRot = Mth.sin(beat - 1.0F) * amp * 0.35F;
        this.dorsal.yRot = Mth.sin(beat - 0.9F) * amp * 0.3F;
        this.body.xRot = s.xRot * Anim.DEG;
        this.body.zRot = Mth.sin(beat * 0.5F) * 0.04F * e;
        // the pectorals scull (quickest while it hovers), fold back at speed; the pelvics flutter; the sail lies back
        float scull = Mth.sin(age * (0.55F + 0.25F * (1.0F - e)));
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.fins[k].zRot += sx * scull * 0.35F;
            this.fins[k].yRot += sx * 0.35F * e;
            this.pelvics[k].zRot += sx * Mth.sin(age * 0.7F + k) * 0.2F;
        }
        this.dorsal.xRot = -0.3F * e + Mth.sin(age * 0.2F) * 0.05F;
        // the kazoo buzzes now and then
        float buzz = Math.max(0.0F, Mth.sin(age * 0.07F));
        buzz = buzz > 0.92F ? (buzz - 0.92F) * 12.5F : 0.0F;
        this.kazoo.xScale = 1.0F + Mth.sin(age * 4.0F) * 0.06F * buzz;
        this.kazoo.yScale = this.kazoo.xScale;
        this.kazoo.zScale = 1.0F + 0.12F * buzz;
        // stranded: on its side, flapping
        if (!s.inLiquid) {
            this.body.zRot = Mth.HALF_PI;
            this.body.y += 2.5F;
            this.tailStem.yRot = Mth.sin(s.ageInTicks * 1.1F) * 0.6F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 1.1F - 0.7F) * 0.8F;
        }
        // hurt: it curls, tail towards the head, and eases out
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float k = Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI);
            this.tailStem.yRot += 0.5F * k;
            this.tail.yRot += 0.4F * k;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = Mth.PI * die;
        }
    }
}
