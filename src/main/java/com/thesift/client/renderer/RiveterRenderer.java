package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.RiveterModel;
import com.thesift.client.renderer.state.RiveterRenderState;
import com.thesift.entity.Riveter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class RiveterRenderer extends SiftMobRenderer<Riveter, RiveterRenderState, RiveterModel> {
    private static final com.thesift.client.Expression[] PAINTED = {com.thesift.client.Expression.BLINK, com.thesift.client.Expression.ANGRY, com.thesift.client.Expression.HURT, com.thesift.client.Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("riveter", PAINTED);
    private static final ExpressionTextures GLOW = new ExpressionTextures("riveter", new String[]{"riveter"}, "_glow", PAINTED);

    public RiveterRenderer(EntityRendererProvider.Context context) {
        super(context, new RiveterModel(context.bakeLayer(ModModelLayers.RIVETER)), 0.4F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression), (s, age) -> {
            float scream = s.scream.isStarted() ? Math.max(0.0F, 1.0F - s.scream.getTimeInMillis(age) / 1800.0F) : 0.0F;
            return Math.min(1.0F, 0.35F + 0.2F * Mth.sin(age * 0.08F) + scream);
        }, this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(RiveterRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected com.thesift.client.Expression expression(Riveter entity, RiveterRenderState state) {
        // roosting upside down it sleeps; screaming or hunting it glares
        return com.thesift.client.Expression.pick(entity, entity.isAggressive() || entity.screamAnimation.isStarted(), false, entity.isHanging());
    }

    @Override
    public RiveterRenderState createRenderState() {
        return new RiveterRenderState();
    }

    @Override
    public void extractRenderState(Riveter entity, RiveterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.hanging = entity.isHanging();
        state.sway = entity.sway.get(partialTicks);
        state.scream.copyFrom(entity.screamAnimation);
    }
}
