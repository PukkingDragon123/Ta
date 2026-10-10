package com.thesift.client.model;

import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * P4 Glow Fly (geometry: tools/jungle_mobs.py). It hovers, bobbing, its two pairs of wings a blur and its wing
 * cases half open; the lantern sways and swells with each slow pulse, the plumed antennae feel the air and the
 * legs dangle. Drinking light (animA) it spreads its antennae and the lantern swells and brightens; pouring light
 * into a plant (animB) it dips and its lantern bows; its flash (animC) throws everything out at once.
 * State: valueA = charge (0 - 1), flagA = fleeing.
 */
public class GlowFlyModel extends EntityModel<JungleRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart abdomen;
    private final ModelPart tip;
    private final ModelPart[] antennae = new ModelPart[2];
    private final ModelPart[] plumes = new ModelPart[2];
    private final ModelPart[] cases = new ModelPart[2];
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[] hindwings = new ModelPart[2];
    private final ModelPart[][] legs = new ModelPart[2][3];
    private final ModelPart[][] feet = new ModelPart[2][3];

    public GlowFlyModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.abdomen = this.body.getChild("abdomen");
        this.tip = this.abdomen.getChild("abdomen_tip");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.antennae[i] = this.head.getChild(s + "_antenna");
            this.plumes[i] = this.antennae[i].getChild(s + "_plume");
            this.cases[i] = this.body.getChild(s + "_wing_case");
            this.wings[i] = this.body.getChild(s + "_wing");
            this.hindwings[i] = this.body.getChild(s + "_hindwing");
            for (int j = 0; j < 3; j++) {
                this.legs[i][j] = this.body.getChild(s + "_leg_" + j);
                this.feet[i][j] = this.legs[i][j].getChild(s + "_foot_" + j);
            }
        }
    }

    @Override
    public void setupAnim(JungleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float speed = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        float flee = s.flagA ? 1.0F : 0.0F;
        // ---- hovering: a slow bob and roll, nose dipping as it flies forward
        this.body.y += Mth.sin(age * 0.11F) * 1.2F;
        this.body.zRot += Mth.sin(age * 0.07F) * 0.06F;
        this.body.xRot += 0.18F * speed + 0.25F * flee;
        this.head.yRot += s.yRot * Anim.DEG * 0.6F;
        this.head.xRot += s.xRot * Anim.DEG * 0.4F;
        // ---- wings: a fast beat (a blur), the hind pair a half beat behind; cases quiver
        float beat = Mth.sin(age * (2.4F + 0.8F * flee));
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.wings[i].zRot += sx * (0.15F + beat * 0.65F);
            this.wings[i].yRot += sx * beat * 0.12F;
            this.hindwings[i].zRot += sx * (0.1F + Mth.sin(age * (2.4F + 0.8F * flee) - 1.1F) * 0.55F);
            this.cases[i].zRot += sx * (Mth.sin(age * 0.6F) * 0.04F - 0.1F * speed);
            // antennae feel the air, plumes trailing
            this.antennae[i].zRot += sx * Mth.sin(age * 0.09F + i * 1.7F) * 0.12F;
            this.antennae[i].xRot += Mth.sin(age * 0.13F + i) * 0.08F + 0.25F * speed;
            this.plumes[i].xRot += Mth.sin(age * 0.13F + i - 0.8F) * 0.12F;
            // legs dangle and swing a little behind the body's motion
            for (int j = 0; j < 3; j++) {
                this.legs[i][j].xRot += 0.35F * speed + Mth.sin(age * 0.12F + j * 0.9F + i) * 0.08F;
                this.feet[i][j].xRot += Mth.sin(age * 0.12F + j * 0.9F + i - 0.7F) * 0.12F;
            }
        }
        // ---- the lantern: sways like a pendulum and swells with each pulse
        float pulse = 0.5F + 0.5F * Mth.sin(age * 0.12F);
        this.abdomen.xRot += Mth.sin(age * 0.11F - 0.9F) * 0.07F - 0.12F * speed;
        this.abdomen.zRot += Mth.sin(age * 0.07F - 0.6F) * 0.05F;
        float swell = 1.0F + 0.04F * pulse * (0.5F + s.valueA);
        this.tip.xScale = swell;
        this.tip.yScale = swell;
        this.tip.zScale = swell;
        this.tip.xRot += Mth.sin(age * 0.11F - 1.6F) * 0.08F;

        // ---- drinking light: antennae spread wide, lantern swelling, the whole body leaning in
        float ab = Anim.seconds(s.animA, s.ageInTicks);
        if (ab >= 0.0F && ab < 1.4F) {
            float e = Anim.envelope(ab, 0.0F, 0.2F, 0.8F, 0.4F);
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.antennae[i].zRot += sx * 0.5F * e;
                this.antennae[i].xRot -= 0.4F * e;
            }
            this.body.xRot -= 0.2F * e;
            float sw = 1.0F + 0.12F * e * (0.5F + 0.5F * Mth.sin(ab * 18.0F));
            this.abdomen.xScale = sw;
            this.abdomen.yScale = sw;
            this.abdomen.zScale = sw;
        }
        // ---- pouring light out: it dips and bows its lantern down towards the plant
        float gv = Anim.seconds(s.animB, s.ageInTicks);
        if (gv >= 0.0F && gv < 1.0F) {
            float e = Anim.envelope(gv, 0.0F, 0.15F, 0.45F, 0.4F);
            this.body.y += 1.5F * e;
            this.abdomen.xRot += 0.7F * e;
            this.tip.xRot += 0.4F * e;
            this.body.xRot += 0.15F * e;
        }
        // ---- the flash: wings flung open, antennae back, the lantern blown up and shaking
        float fl = Anim.seconds(s.animC, s.ageInTicks);
        if (fl >= 0.0F && fl < 0.7F) {
            float e = Anim.envelope(fl, 0.0F, 0.04F, 0.15F, 0.5F);
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.wings[i].zRot = sx * -0.9F * e + this.wings[i].zRot * (1.0F - e);
                this.hindwings[i].zRot = sx * -0.7F * e + this.hindwings[i].zRot * (1.0F - e);
                this.cases[i].zRot -= sx * 0.6F * e;
                this.antennae[i].xRot += 0.7F * e;
            }
            float big = 1.0F + 0.3F * e;
            this.abdomen.xScale = big + Mth.sin(fl * 60.0F) * 0.05F * e;
            this.abdomen.yScale = big;
            this.abdomen.zScale = big;
            this.abdomen.xRot -= 0.5F * e;
        }
    }
}
