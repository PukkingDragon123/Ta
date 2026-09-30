package com.thesift.client.model;

import com.thesift.client.renderer.state.SifterRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Sifter: a skittering, squid-headed ambusher. Four quick legs, bobbing antenna-ears with glowing
 * tips, dangling feelers and a big yellow chomping maw. When burrowed only the top pokes out.
 */
public class SifterModel extends EntityModel<SifterRenderState> {
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart leftAntenna;
    private final ModelPart leftAntennaTip;
    private final ModelPart rightAntenna;
    private final ModelPart rightAntennaTip;
    private final ModelPart[] tentacles = new ModelPart[4];
    private final ModelPart[] legs = new ModelPart[4];

    public SifterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.leftAntenna = this.body.getChild("left_antenna");
        this.leftAntennaTip = this.leftAntenna.getChild("left_antenna_tip");
        this.rightAntenna = this.body.getChild("right_antenna");
        this.rightAntennaTip = this.rightAntenna.getChild("right_antenna_tip");
        for (int i = 0; i < 4; i++) {
            this.tentacles[i] = this.body.getChild("tentacle_" + i);
        }
        this.legs[0] = this.body.getChild("left_front_leg");
        this.legs[1] = this.body.getChild("right_front_leg");
        this.legs[2] = this.body.getChild("left_hind_leg");
        this.legs[3] = this.body.getChild("right_hind_leg");
    }

    @Override
    public void setupAnim(SifterRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
        float pos = s.walkAnimationPos * 1.25F;

        float sq = Mth.clamp(s.squash, -0.4F, 0.5F);
        this.body.yScale = 1.0F + sq;
        this.body.xScale = 1.0F - sq * 0.5F;
        this.body.zScale = 1.0F - sq * 0.5F;
        this.body.yRot = s.yRot * Anim.DEG * 0.35F;
        this.body.xRot = s.xRot * Anim.DEG * 0.2F + Mth.sin(pos * 2.0F) * 0.04F * walk;
        this.body.y += Math.abs(Mth.sin(pos)) * -1.2F * walk;

        // --- skittering legs (diagonal pairs)
        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : (float) Math.PI;
            this.legs[i].xRot = Mth.cos(pos + phase) * 0.8F * walk;
            this.legs[i].zRot += Math.max(0.0F, Mth.sin(pos + phase)) * 0.25F * walk * (i % 2 == 0 ? -1 : 1);
        }

        // --- antennae: spring wobble + curious twitching
        float twitch = Mth.sin(age * 0.21F) * Mth.sin(age * 0.047F);
        this.leftAntenna.xRot += s.antennaLeft + twitch * 0.12F;
        this.rightAntenna.xRot += s.antennaRight - twitch * 0.1F;
        this.leftAntenna.zRot += Mth.sin(age * 0.09F) * 0.06F;
        this.rightAntenna.zRot -= Mth.sin(age * 0.09F + 1.3F) * 0.06F;
        this.leftAntennaTip.xRot += s.antennaLeft * 0.8F;
        this.rightAntennaTip.xRot += s.antennaRight * 0.8F;
        for (int i = 0; i < 4; i++) {
            this.tentacles[i].xRot = Mth.sin(age * 0.15F + i * 1.1F) * 0.2F - walk * 0.5F - sq * 0.6F;
            this.tentacles[i].zRot = Mth.cos(age * 0.12F + i) * 0.1F;
        }

        // --- chomp
        float chomp = Anim.seconds(s.chomp, age);
        float bite = chomp >= 0 ? Anim.envelope(chomp, 0.0F, 0.1F, 0.05F, 0.2F) : 0.0F;
        this.jaw.xRot += bite * 1.0F + Mth.sin(age * 0.1F) * 0.03F;
        this.body.xRot += bite * 0.18F;

        // --- burrowed: sink into the sand with only the mantle and antennae showing
        float emerge = Anim.seconds(s.emerge, age);
        if (s.burrowed) {
            this.body.y += 9.0F;
            this.body.yRot = Mth.sin(age * 0.03F) * 0.3F;
            this.leftAntenna.zRot += 0.25F;
            this.rightAntenna.zRot -= 0.25F;
        } else if (emerge >= 0 && emerge < 0.6F) {
            float t = Anim.backOut(emerge / 0.45F);
            this.body.y += 9.0F * (1.0F - t);
            this.leftAntenna.xRot -= (1.0F - t) * 0.8F;
            this.rightAntenna.xRot -= (1.0F - t) * 0.8F;
        }
    }
}
