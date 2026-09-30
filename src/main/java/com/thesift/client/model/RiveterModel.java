package com.thesift.client.model;

import com.thesift.client.renderer.state.RiveterRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Riveter: hangs from its claws like a pendulum, wings wrapped around it like a cloak. The scream
 * snaps the wings open, drops the head and gapes the glowing throat before everything refolds.
 */
public class RiveterModel extends EntityModel<RiveterRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart leftWing;
    private final ModelPart leftWingTip;
    private final ModelPart rightWing;
    private final ModelPart rightWingTip;

    public RiveterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftHorn = this.head.getChild("left_horn");
        this.rightHorn = this.head.getChild("right_horn");
        this.leftWing = this.body.getChild("left_wing");
        this.leftWingTip = this.leftWing.getChild("left_wing_tip");
        this.rightWing = this.body.getChild("right_wing");
        this.rightWingTip = this.rightWing.getChild("right_wing_tip");
    }

    @Override
    public void setupAnim(RiveterRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;

        if (!s.hanging) {
            // Flying: flip right way up and flap hard.
            this.body.xRot = (float) Math.PI;
            this.body.y = 22.0F;
            float flap = Mth.sin(age * 1.3F);
            this.leftWing.yRot = 0.2F + flap * 0.9F;
            this.rightWing.yRot = -0.2F - flap * 0.9F;
            this.leftWingTip.yRot = flap * 0.5F;
            this.rightWingTip.yRot = -flap * 0.5F;
            return;
        }

        // --- the pendulum sway of a hanging creature
        this.body.zRot = s.sway;
        this.body.xRot = Mth.sin(age * 0.023F) * 0.05F;
        this.head.xRot += -s.sway * 0.6F;
        this.head.yRot = s.yRot * Anim.DEG * 0.5F;
        float breathe = Mth.sin(age * 0.07F);
        this.leftWing.yRot += breathe * 0.04F;
        this.rightWing.yRot -= breathe * 0.04F;
        this.leftHorn.zRot += Mth.sin(age * 0.11F) * 0.08F;
        this.rightHorn.zRot -= Mth.sin(age * 0.11F + 0.4F) * 0.08F;

        // --- scream (1.7s): wings flare open, head drops, throat gapes
        float t = Anim.seconds(s.scream, age);
        if (t >= 0 && t < 1.8F) {
            float open = Anim.envelope(t, 0.0F, 0.18F, 1.05F, 0.45F);
            float snap = t < 0.3F ? Anim.backOut(t / 0.25F) : 1.0F;
            float wings = open * snap;
            float tremble = Mth.sin(age * 2.9F) * 0.06F * open;
            this.leftWing.yRot += wings * 1.0F + tremble;
            this.leftWing.zRot += -wings * 0.4F;
            this.rightWing.yRot += -wings * 1.0F - tremble;
            this.rightWing.zRot += wings * 0.4F;
            float tips = Anim.envelope(t, 0.08F, 0.2F, 0.95F, 0.45F);
            this.leftWingTip.yRot += tips * 1.2F;
            this.rightWingTip.yRot += -tips * 1.2F;
            this.jaw.xRot += open * 1.0F + tremble;
            this.head.xRot += open * 0.35F;
            this.leftHorn.zRot += -open * 0.5F;
            this.rightHorn.zRot += open * 0.5F;
            this.body.xScale = 1.0F + open * 0.06F;
        }
    }
}
