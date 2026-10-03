package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Gobbler, a Warden-kin catfish of sculk (geometry from tools/gobbler.py). A slow, heavy tail
 * wave runs from the torso through two tail segments to the fluke; the soul-lantern lures on its
 * whiskers swing like pendulums behind the motion and bob in the current; the gill covers flare with
 * every breath (wide open when it hunts, lunges or gulps); the sculk sensor spines on its back sway
 * and sweep back as it speeds up, and shiver like the Warden's tendrils while it listens. The lunge
 * has a long wind-up (head reared, body drawn back, mouth gaping, gills flared), a snap forward and a
 * slow settle; the gulp bulges its cheeks; the spit throws the jaw open. Lulled by the Tide Song
 * everything slows and droops. The glow pulse itself is the renderer's (SiftFishRenderState.glowPulse).
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
    private final ModelPart leftLure;
    private final ModelPart rightLure;
    private final ModelPart leftChin;
    private final ModelPart rightChin;
    private final ModelPart leftGill;
    private final ModelPart rightGill;
    private final ModelPart[] spines = new ModelPart[3];

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
        this.leftLure = this.leftBarbelTip.getChild("left_lure");
        this.rightLure = this.rightBarbelTip.getChild("right_lure");
        this.leftChin = this.jaw.getChild("left_chin_barbel");
        this.rightChin = this.jaw.getChild("right_chin_barbel");
        this.leftGill = this.head.getChild("left_gill");
        this.rightGill = this.head.getChild("right_gill");
        for (int i = 0; i < this.spines.length; i++) {
            this.spines[i] = this.body.getChild("spine_" + i);
        }
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
        // breathing: the jaw pumps, the chest of souls swells and the gill covers flare with it
        float breath = (Mth.sin(age * 0.1F) + 1.0F) * 0.5F;
        this.jaw.xRot = 0.05F + breath * 0.08F + (s.hunting ? 0.1F : 0.0F);
        this.chest.yScale = 1.0F + breath * 0.08F;
        float flare = breath * 0.22F + (s.hunting ? 0.25F : 0.0F) + (s.calm ? -0.1F : 0.0F);
        // the sculk spines: a slow sway, swept back by speed, a Warden shiver while it listens
        float shiver = s.hunting ? Mth.sin(s.ageInTicks * 1.9F) * 0.1F : 0.0F;
        for (int i = 0; i < this.spines.length; i++) {
            ModelPart sp = this.spines[i];
            sp.xRot += Mth.sin(age * 0.09F - i * 0.7F) * 0.1F - e * 0.45F + (s.calm ? -0.35F : 0.0F);
            sp.zRot += Mth.sin(age * 0.07F + i * 1.3F) * 0.08F + shiver * (i % 2 == 0 ? 1.0F : -1.0F);
        }
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
        // the soul-lantern lures: pendulums that lag behind the whiskers and swing back past rest
        float swingL = Mth.sin(age * 0.12F - 1.4F);
        float swingR = Mth.sin(age * 0.12F - 0.4F);
        this.leftLure.xRot += -this.leftBarbel.xRot * 0.6F + swingL * 0.22F + drag * 0.9F;
        this.rightLure.xRot += -this.rightBarbel.xRot * 0.6F + swingR * 0.22F + drag * 0.9F;
        this.leftLure.zRot += Mth.sin(age * 0.09F + 0.7F) * 0.18F - this.leftBarbelTip.yRot * 0.3F;
        this.rightLure.zRot += Mth.sin(age * 0.09F + 2.1F) * 0.18F - this.rightBarbelTip.yRot * 0.3F;
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
            flare += 0.7F * wind + 0.4F * strike;
            // the lures fly back on the strike and swing forward again after it
            this.leftLure.xRot += 1.1F * strike - 0.3F * wind;
            this.rightLure.xRot += 1.1F * strike - 0.3F * wind;
            for (ModelPart sp : this.spines) {
                sp.xRot += 0.35F * wind - 0.6F * strike;
            }
        }
        // the gulp: cheeks bulge, the jaw clamps shut, the gills pump the water back out
        float gulp = Anim.seconds(s.gulp, s.ageInTicks);
        if (gulp >= 0.0F && gulp < 2.5F) {
            float bulge = Anim.envelope(gulp, 0.0F, 0.15F, 1.9F, 0.4F);
            this.head.xScale = 1.0F + 0.18F * bulge + Mth.sin(s.ageInTicks * 0.9F) * 0.03F * bulge;
            this.jaw.xScale = this.head.xScale;
            this.jaw.xRot = Mth.lerp(bulge, this.jaw.xRot, 0.0F);
            this.body.xScale = 1.0F + 0.08F * bulge;
            flare += bulge * (0.35F + 0.25F * Mth.sin(s.ageInTicks * 0.9F));
        }
        // the spit: the jaw flies open and the head jerks back
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        if (spit >= 0.0F && spit < 0.8F) {
            float open = Anim.envelope(spit, 0.0F, 0.06F, 0.12F, 0.5F);
            this.jaw.xRot = Math.max(this.jaw.xRot, 1.1F * open);
            this.head.xRot -= 0.25F * open;
            this.body.z += 1.5F * open;
            flare += 0.5F * open;
        }
        // the gill covers open outwards from their hinge, with a little flutter at the trailing edge
        float flutter = Mth.sin(s.ageInTicks * 0.8F) * 0.04F * (0.3F + e);
        this.leftGill.yRot += Math.max(0.0F, flare) + flutter;
        this.rightGill.yRot -= Math.max(0.0F, flare) + flutter;
        this.leftGill.zRot -= flare * 0.15F;
        this.rightGill.zRot += flare * 0.15F;
        if (!s.inLiquid) {
            // stranded: flat on its side, gasping and thrashing its tail, gills gaping
            this.body.zRot = (float) Math.PI * 0.5F;
            this.body.y += 4.0F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 0.6F) * 0.5F;
            this.tail2.yRot = Mth.sin(s.ageInTicks * 0.6F - 0.8F) * 0.6F;
            float gasp = Math.max(0.0F, Mth.sin(s.ageInTicks * 0.3F));
            this.jaw.xRot = 0.3F + gasp * 0.5F;
            this.leftGill.yRot += gasp * 0.5F;
            this.rightGill.yRot -= gasp * 0.5F;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = (float) Math.PI * die;
            this.jaw.xRot = 0.6F * die;
        }
    }
}
