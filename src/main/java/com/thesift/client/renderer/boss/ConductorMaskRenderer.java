package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.boss.ConductorMaskModel;
import com.thesift.client.renderer.state.ConductorMaskRenderState;
import com.thesift.entity.boss.ConductorMask;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The Mask, twice life size, its cracks and eyes glowing brighter as it nears its change. */
public class ConductorMaskRenderer extends MobRenderer<ConductorMask, ConductorMaskRenderState, ConductorMaskModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/conductor_mask/conductor_mask.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/conductor_mask/conductor_mask_glow.png");

    public ConductorMaskRenderer(EntityRendererProvider.Context context) {
        super(context, new ConductorMaskModel(context.bakeLayer(ModModelLayers.CONDUCTOR_MASK)), 0.0F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> Math.min(1.0F, 0.6F + s.transform * 0.4F + 0.2F * Mth.sin(age * 0.2F)),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(ConductorMaskRenderState state) {
        return TEXTURE;
    }

    @Override
    public ConductorMaskRenderState createRenderState() {
        return new ConductorMaskRenderState();
    }

    @Override
    public void extractRenderState(ConductorMask entity, ConductorMaskRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.transform = entity.transform(partialTicks);
    }

    @Override
    protected void scale(ConductorMaskRenderState state, PoseStack poseStack) {
        poseStack.scale(2.0F, 2.0F, 2.0F);
    }
}
