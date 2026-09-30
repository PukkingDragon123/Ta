package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.EnchoerModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.EnchoerRenderState;
import com.thesift.entity.Enchoer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class EnchoerRenderer extends MobRenderer<Enchoer, EnchoerRenderState, EnchoerModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/enchoer/enchoer.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/enchoer/enchoer_glow.png");

    public EnchoerRenderer(EntityRendererProvider.Context context) {
        super(context, new EnchoerModel(context.bakeLayer(ModModelLayers.ENCHOER)), 0.5F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW,
                (s, age) -> (s.singing ? 0.85F : 0.55F) + (s.singing ? 0.15F : 0.2F) * Mth.sin(age * (s.singing ? 0.3F : 0.06F)), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(EnchoerRenderState state) {
        return TEXTURE;
    }

    @Override
    public EnchoerRenderState createRenderState() {
        return new EnchoerRenderState();
    }

    @Override
    public void extractRenderState(Enchoer entity, EnchoerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.wingSpread = Mth.lerp(partialTicks, entity.wingSpreadO, entity.wingSpread);
        state.singing = entity.isSinging();
    }
}
