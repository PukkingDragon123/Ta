package com.thesift.client.renderer;

import com.thesift.client.CaravansClient;
import com.thesift.client.model.CaravanLarvaModel;
import com.thesift.client.renderer.state.CaravanLarvaRenderState;
import com.thesift.entity.caravan.CaravanLarva;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** CR2: a Caravan larva in its caravan's colour; its pinprick eyes and gem nubs glow. */
public class CaravanLarvaRenderer extends SiftMobRenderer<CaravanLarva, CaravanLarvaRenderState, CaravanLarvaModel> {
    private static final String[] VARIANTS = {"caravan_larva_amber", "caravan_larva_rose", "caravan_larva_teal", "caravan_larva_violet",
            "caravan_larva_gold"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("caravan_larva", VARIANTS);
    private static final ExpressionTextures GLOW = new ExpressionTextures("caravan_larva", VARIANTS, "_glow");

    public CaravanLarvaRenderer(EntityRendererProvider.Context context) {
        super(context, new CaravanLarvaModel(context.bakeLayer(CaravansClient.CARAVAN_LARVA)), 0.25F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.variant, s.expression),
                (s, age) -> 0.7F + 0.25F * Mth.sin(age * 0.15F + s.seed), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(CaravanLarvaRenderState state) {
        return TEXTURES.get(state.variant, state.expression);
    }

    @Override
    public CaravanLarvaRenderState createRenderState() {
        return new CaravanLarvaRenderState();
    }

    @Override
    public void extractRenderState(CaravanLarva entity, CaravanLarvaRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Mth.clamp(entity.getVariant(), 0, VARIANTS.length - 1);
        state.seed = entity.getId() % 89;
        state.bite.copyFrom(entity.biteAnimation);
        state.emerge.copyFrom(entity.emergeAnimation);
    }
}
