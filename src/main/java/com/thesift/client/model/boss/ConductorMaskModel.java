package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.ConductorMaskRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/** The Mask: ribbons streaming, horns flexing, trembling harder and harder as it transforms. */
public class ConductorMaskModel extends EntityModel<ConductorMaskRenderState> {
    private final ModelPart mask;
    private final ModelPart leftRibbon;
    private final ModelPart rightRibbon;
    private final ModelPart leftHorn;
    private final ModelPart rightHorn;
    private final ModelPart crown;

    public ConductorMaskModel(ModelPart root) {
        super(root);
        this.mask = root.getChild("mask");
        this.leftRibbon = this.mask.getChild("left_ribbon");
        this.rightRibbon = this.mask.getChild("right_ribbon");
        this.leftHorn = this.mask.getChild("left_horn");
        this.rightHorn = this.mask.getChild("right_horn");
        this.crown = this.mask.getChild("crown");
    }

    @Override
    public void setupAnim(ConductorMaskRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float k = s.transform;
        this.leftRibbon.xRot += 0.3F + Mth.sin(age * 0.15F) * 0.25F + k * 0.6F;
        this.rightRibbon.xRot += 0.3F + Mth.sin(age * 0.15F + 1.3F) * 0.25F + k * 0.6F;
        this.leftRibbon.zRot += Mth.sin(age * 0.1F) * 0.15F - k * 0.5F;
        this.rightRibbon.zRot -= Mth.sin(age * 0.1F + 0.7F) * 0.15F - k * 0.5F;
        float flex = Mth.sin(age * 0.05F) * 0.06F + k * 0.25F;
        this.leftHorn.zRot += flex;
        this.rightHorn.zRot -= flex;
        this.crown.yScale = 1.0F + Mth.sin(age * 0.2F) * 0.05F + k * 0.4F;
        this.mask.xRot = Mth.sin(age * 0.04F) * 0.08F;
        float shake = Anim.smooth(k) * 0.08F;
        this.mask.x += Mth.sin(age * 4.1F) * shake * 10.0F;
        this.mask.y += Mth.cos(age * 3.7F) * shake * 10.0F;
        float swell = 1.0F + k * 0.35F;
        this.mask.xScale = swell;
        this.mask.yScale = swell;
        this.mask.zScale = swell;
    }
}
