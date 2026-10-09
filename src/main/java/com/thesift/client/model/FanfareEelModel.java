package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * S2: the Fanfare Eel (geometry and hand-painted variants in tools/waterfolk.py). It swims with an S-wave
 * running from the head down its five overlapping segments into the tail fin - longer and quicker the harder
 * it swims, the head steadying against it - while the scalloped crest ripples along its back and the
 * pectorals paddle. Its trumpet-bell mouth breathes; on the hunt the crest stands up. The bite: the head
 * lunges and the bell flares wide with the blast. Out of the water it writhes in big slow coils; hurt, it
 * kinks; dying, it rolls belly-up.
 */
public class FanfareEelModel extends EntityModel<SiftFishRenderState> {
    private static final int SEGMENTS = 5;
    private final ModelPart head;
    private final ModelPart bell;
    private final ModelPart tailFin;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];
    private final ModelPart[] frills = new ModelPart[SEGMENTS];
    private final ModelPart[] fins = new ModelPart[2];

    public FanfareEelModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.bell = this.head.getChild("pipe").getChild("bell");
        this.fins[0] = this.head.getChild("left_fin");
        this.fins[1] = this.head.getChild("right_fin");
        ModelPart prev = this.head;
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i] = prev.getChild("segment_" + i);
            this.frills[i] = this.segments[i].getChild("frill_" + i);
            prev = this.segments[i];
        }
        this.tailFin = prev.getChild("tail_fin");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.inLiquid ? s.effort : 1.0F;
        float hunt = s.aggressive ? 1.0F : 0.0F;
        float t = s.inLiquid ? age * (0.22F + 0.3F * e) : s.ageInTicks * 0.35F;
        float amp = s.inLiquid ? 0.18F + 0.22F * e : 0.5F;
        this.head.xRot = s.xRot * Anim.DEG;
        this.head.yRot = Mth.sin(t + 0.6F) * amp * 0.35F;
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i].yRot = Mth.sin(t - i * 0.85F) * amp * (0.6F + 0.15F * i);
            this.segments[i].xRot = Mth.sin(t * 0.5F - i * 0.7F) * 0.03F;
            this.frills[i].zRot = Mth.sin(t * 2.0F - i * 1.1F) * 0.12F;
            this.frills[i].yScale = 1.0F + 0.35F * hunt;
        }
        this.tailFin.yRot = Mth.sin(t - SEGMENTS * 0.85F) * amp * 1.4F;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.fins[k].zRot += sx * Mth.sin(age * 0.4F + k) * 0.3F;
            this.fins[k].yRot += sx * 0.3F * e;
        }
        // the trumpet bell breathes
        float breathe = 1.0F + Mth.sin(age * 0.15F) * 0.03F;
        this.bell.xScale = breathe;
        this.bell.yScale = breathe;
        // the bite (0.6 s): the head lunges and the bell flares with the blast
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.6F) {
            float lunge = Anim.envelope(bite, 0.0F, 0.08F, 0.1F, 0.35F);
            float flare = Anim.envelope(bite, 0.05F, 0.06F, 0.12F, 0.3F);
            this.head.z -= 2.5F * lunge;
            this.head.xRot -= 0.15F * lunge;
            this.bell.xScale += 0.35F * flare;
            this.bell.yScale += 0.35F * flare;
            this.segments[0].yRot *= 1.0F - lunge;
        }
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float k = Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI);
            for (int i = 0; i < SEGMENTS; i++) {
                this.segments[i].yRot += 0.25F * k * (i % 2 == 0 ? 1.0F : -1.0F);
            }
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.head.zRot = Mth.PI * die;
        }
    }
}
