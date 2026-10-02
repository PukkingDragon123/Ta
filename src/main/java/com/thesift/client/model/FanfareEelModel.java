package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Fanfare Eel: a travelling wave runs down its five segments (faster and wider when it darts in),
 * the trumpet bell flares on every bite, and out of the liquid it thrashes on its side.
 */
public class FanfareEelModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart head;
    private final ModelPart bell;
    private final ModelPart pipe;
    private final ModelPart crest;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart[] segments = new ModelPart[5];
    private final ModelPart[] bellFins = new ModelPart[4];
    private final ModelPart tailFin;
    private final ModelPart leftGill;
    private final ModelPart rightGill;
    private final ModelPart leftWhisker;
    private final ModelPart rightWhisker;

    public FanfareEelModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.pipe = this.head.getChild("pipe");
        this.bell = this.pipe.getChild("bell");
        this.crest = this.head.getChild("crest");
        this.leftFin = this.head.getChild("left_fin");
        this.rightFin = this.head.getChild("right_fin");
        for (int i = 0; i < 4; i++) {
            this.bellFins[i] = this.bell.getChild("bell_fin_" + i);
        }
        ModelPart p = this.head;
        for (int i = 0; i < 5; i++) {
            p = p.getChild("segment_" + i);
            this.segments[i] = p;
        }
        this.tailFin = this.segments[4].getChild("tail_fin");
        this.leftGill = this.head.getChild("left_gill");
        this.rightGill = this.head.getChild("right_gill");
        this.leftWhisker = this.pipe.getChild("left_whisker");
        this.rightWhisker = this.pipe.getChild("right_whisker");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float effort = s.effort;
        float speed = 0.25F + effort * 0.45F;
        float amp = 0.12F + effort * 0.22F;
        this.head.yRot = Mth.sin(age * speed + 0.9F) * amp * 0.35F;
        this.head.xRot = s.xRot * Anim.DEG;
        // an anguilliform wave: it starts small behind the head and grows down the body, with a
        // gentle vertical ripple on top so the eel never looks like a stiff zigzag
        for (int i = 0; i < 5; i++) {
            this.segments[i].yRot = Mth.sin(age * speed - i * 0.85F) * amp * (0.55F + i * 0.22F);
            this.segments[i].xRot = Mth.sin(age * speed * 0.5F - i * 0.6F) * 0.04F;
        }
        this.tailFin.yRot = Mth.sin(age * speed - 5.0F * 0.85F) * amp * 1.2F;
        float breath = Math.max(0.0F, Mth.sin(age * 0.3F));
        this.leftGill.yRot = 0.5F + breath * 0.35F;
        this.rightGill.yRot = -0.5F - breath * 0.35F;
        // the bone whiskers trail behind and stream back when it speeds up
        this.leftWhisker.yRot = 0.6F + Mth.sin(age * 0.2F) * 0.15F + effort * 0.4F;
        this.rightWhisker.yRot = -0.6F - Mth.sin(age * 0.2F + 1.0F) * 0.15F - effort * 0.4F;
        this.leftWhisker.xRot = 0.5F - effort * 0.3F;
        this.rightWhisker.xRot = 0.5F - effort * 0.3F;
        this.leftFin.zRot += Mth.sin(age * 0.6F) * 0.3F;
        this.rightFin.zRot -= Mth.sin(age * 0.6F) * 0.3F;
        this.crest.yRot = Mth.sin(age * speed) * 0.1F;
        // the glowing bell fins ripple like a flower opening and closing
        for (int i = 0; i < 4; i++) {
            this.bellFins[i].xRot = -0.25F + Mth.sin(age * 0.18F + i * 1.6F) * 0.12F - effort * 0.15F;
        }
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.6F) {
            float lunge = Anim.envelope(bite, 0.0F, 0.06F, 0.05F, 0.3F);
            float flare = Anim.backOut(Math.min(1.0F, bite / 0.12F)) * (1.0F - Anim.smooth((bite - 0.15F) / 0.4F));
            this.head.z -= 2.5F * lunge;
            this.pipe.z -= 1.5F * lunge;
            this.bell.xScale += 0.6F * flare;
            this.bell.yScale += 0.6F * flare;
            this.bell.zScale = 1.0F + 0.8F * flare;
            for (int i = 0; i < 4; i++) {
                this.bellFins[i].xRot -= 0.9F * flare;
            }
            for (int i = 0; i < 5; i++) {
                this.segments[i].yRot *= 1.0F - lunge * 0.6F;
            }
        }
        if (!s.inLiquid) {
            // stranded: thrashing on its side
            this.head.zRot = (float) Math.PI * 0.5F;
            this.head.y += 2.0F;
            for (int i = 0; i < 5; i++) {
                this.segments[i].yRot = Mth.sin(s.ageInTicks * 0.9F - i) * 0.5F;
            }
        }
        if (s.hasRedOverlay) {
            for (int i = 0; i < 5; i++) {
                this.segments[i].yRot += Mth.sin(s.ageInTicks * 2.5F - i) * 0.3F;
            }
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.head.zRot = (float) Math.PI * die;
            for (int i = 0; i < 5; i++) {
                this.segments[i].yRot *= 1.0F - die;
            }
        }
    }
}
