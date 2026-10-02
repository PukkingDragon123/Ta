package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Gobbler. A slow, heavy tail wave that runs from the torso through two tail segments to the
 * fluke; whisker barbels that trail behind the motion; a jaw that pumps as it breathes. Hunting,
 * its crown tendrils twitch like a listening Warden's. The lunge has a long wind-up (head reared,
 * body drawn back, mouth gaping), a snap forward and a slow settle; the gulp bulges its cheeks;
 * the spit throws the jaw open. Lulled by the Tide Song everything slows and droops.
 */
public class GobblerModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart fluke;
    private final ModelPart dorsal;
    private final ModelPart chest;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;
    private final ModelPart leftBarbel;
    private final ModelPart rightBarbel;
    private final ModelPart leftBarbelTip;
    private final ModelPart rightBarbelTip;
    private final ModelPart leftChin;
    private final ModelPart rightChin;

    public GobblerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.fluke = this.tail2.getChild("fluke");
        this.dorsal = this.body.getChild("dorsal");
        this.chest = this.body.getChild("chest");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.leftTendril = this.head.getChild("left_tendril");
        this.rightTendril = this.head.getChild("right_tendril");
        this.leftBarbel = this.head.getChild("left_barbel");
        this.rightBarbel = this.head.getChild("right_barbel");
        this.leftBarbelTip = this.leftBarbel.getChild("left_barbel_tip");
        this.rightBarbelTip = this.rightBarbel.getChild("right_barbel_tip");
        this.leftChin = this.jaw.getChild("left_chin_barbel");
        this.rightChin = this.jaw.getChild("right_chin_barbel");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.calm ? s.effort * 0.4F : s.effort;
        float beat = age * (0.16F + e * 0.32F);
        float amp = (s.calm ? 0.12F : 0.18F) + e * 0.3F;
        // the travelling wave: torso a little, each tail segment more and later
        this.body.yRot = Mth.sin(beat) * amp * 0.18F;
        this.body.xRot = s.xRot * Anim.DEG * 0.8F;
        this.body.y += Mth.sin(age * 0.08F) * 0.6F;
        this.head.yRot = -Mth.sin(beat) * amp * 0.12F;
        this.tail.yRot = Mth.sin(beat - 0.8F) * amp * 0.7F;
        this.tail2.yRot = Mth.sin(beat - 1.6F) * amp * 1.0F;
        this.fluke.yRot = Mth.sin(beat - 2.4F) * amp * 1.3F;
        this.dorsal.zRot = Mth.sin(beat - 0.5F) * amp * 0.25F;
        this.leftFin.zRot += Mth.sin(age * 0.2F) * 0.25F - e * 0.3F;
        this.rightFin.zRot -= Mth.sin(age * 0.2F + 0.6F) * 0.25F - e * 0.3F;
        this.leftFin.yRot += e * 0.4F;
        this.rightFin.yRot -= e * 0.4F;
        // breathing: the jaw pumps, the chest of souls swells with it
        float breath = (Mth.sin(age * 0.1F) + 1.0F) * 0.5F;
        this.jaw.xRot = 0.05F + breath * 0.08F + (s.hunting ? 0.1F : 0.0F);
        this.chest.yScale = 1.0F + breath * 0.08F;
        // barbels trail behind the motion and curl in the current
        float drag = e * 0.5F;
        this.leftBarbel.yRot += drag * 0.6F + Mth.sin(age * 0.13F) * 0.12F;
        this.rightBarbel.yRot -= drag * 0.6F + Mth.sin(age * 0.13F + 1.0F) * 0.12F;
        this.leftBarbel.xRot += Mth.sin(age * 0.11F) * 0.1F - drag * 0.2F;
        this.rightBarbel.xRot += Mth.sin(age * 0.11F + 0.8F) * 0.1F - drag * 0.2F;
        this.leftBarbelTip.yRot += Mth.sin(age * 0.13F - 0.9F) * 0.25F + drag * 0.4F;
        this.rightBarbelTip.yRot -= Mth.sin(age * 0.13F + 0.1F) * 0.25F + drag * 0.4F;
        this.leftChin.xRot += Mth.sin(age * 0.15F) * 0.15F + drag * 0.5F;
        this.rightChin.xRot += Mth.sin(age * 0.15F + 1.3F) * 0.15F + drag * 0.5F;
        // the crown tendrils: slow sway, a fast Warden twitch while it hunts, drooping when lulled
        float twitch = s.hunting ? Mth.sin(s.ageInTicks * 1.6F) * 0.18F : 0.0F;
        this.leftTendril.zRot += Mth.sin(age * 0.09F) * 0.08F + twitch + (s.calm ? 0.5F : 0.0F);
        this.rightTendril.zRot -= Mth.sin(age * 0.09F + 0.5F) * 0.08F + twitch + (s.calm ? 0.5F : 0.0F);
        if (s.calm) {
            this.jaw.xRot = 0.02F;
        }
        // the lunge: 0.6 s wind-up, a snap forward, a slow settle
        float lunge = Anim.seconds(s.lunge, s.ageInTicks);
        if (lunge >= 0.0F && lunge < 1.4F) {
            float wind = Anim.smooth(lunge / 0.6F) * (1.0F - Anim.smooth((lunge - 0.6F) / 0.1F));
            float strike = Anim.envelope(lunge, 0.6F, 0.08F, 0.25F, 0.45F);
            this.head.xRot -= 0.35F * wind - 0.1F * strike;
            this.body.z += 3.0F * wind - 4.0F * strike;
            this.jaw.xRot = Math.max(this.jaw.xRot, 1.05F * wind + 0.9F * strike * (1.0F - Anim.smooth((lunge - 0.85F) / 0.15F)));
            this.leftTendril.zRot += 0.6F * wind;
            this.rightTendril.zRot -= 0.6F * wind;
            this.tail.yRot += Mth.sin(s.ageInTicks * 1.5F) * 0.5F * strike;
            this.tail2.yRot += Mth.sin(s.ageInTicks * 1.5F - 0.8F) * 0.7F * strike;
            this.leftBarbel.yRot += 0.5F * strike;
            this.rightBarbel.yRot -= 0.5F * strike;
        }
        // the gulp: cheeks bulge, the jaw clamps shut
        float gulp = Anim.seconds(s.gulp, s.ageInTicks);
        if (gulp >= 0.0F && gulp < 2.5F) {
            float bulge = Anim.envelope(gulp, 0.0F, 0.15F, 1.9F, 0.4F);
            this.head.xScale = 1.0F + 0.18F * bulge + Mth.sin(s.ageInTicks * 0.9F) * 0.03F * bulge;
            this.jaw.xScale = this.head.xScale;
            this.jaw.xRot = Mth.lerp(bulge, this.jaw.xRot, 0.0F);
            this.body.xScale = 1.0F + 0.08F * bulge;
        }
        // the spit: the jaw flies open and the head jerks back
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        if (spit >= 0.0F && spit < 0.8F) {
            float open = Anim.envelope(spit, 0.0F, 0.06F, 0.12F, 0.5F);
            this.jaw.xRot = Math.max(this.jaw.xRot, 1.1F * open);
            this.head.xRot -= 0.25F * open;
            this.body.z += 1.5F * open;
        }
        if (!s.inLiquid) {
            // stranded: flat on its side, gasping and thrashing its tail
            this.body.zRot = (float) Math.PI * 0.5F;
            this.body.y += 4.0F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 0.6F) * 0.5F;
            this.tail2.yRot = Mth.sin(s.ageInTicks * 0.6F - 0.8F) * 0.6F;
            this.jaw.xRot = 0.3F + Math.max(0.0F, Mth.sin(s.ageInTicks * 0.3F)) * 0.5F;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = (float) Math.PI * die;
            this.jaw.xRot = 0.6F * die;
        }
    }
}
