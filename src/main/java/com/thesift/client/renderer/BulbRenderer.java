package com.thesift.client.renderer;

import com.thesift.client.Expression;
import com.thesift.client.model.BulbModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.layers.BulbJellyLayer;
import com.thesift.client.renderer.state.BulbRenderState;
import com.thesift.entity.Bulb;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BulbRenderer extends SiftMobRenderer<Bulb, BulbRenderState, BulbModel> {
    private static final String[] VARIANTS = {"bulb_sky", "bulb_blossom", "bulb_dusk", "bulb_starry"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("bulb", VARIANTS, Expression.BLINK, Expression.HAPPY,
            Expression.HURT, Expression.DEAD, Expression.SLEEP);

    public BulbRenderer(EntityRendererProvider.Context context) {
        super(context, new BulbModel(context.bakeLayer(ModModelLayers.BULB), BulbModel.Pass.CORE), 0.42F);
        this.addLayer(new BulbJellyLayer(this, context.getModelSet(), this::getTextureLocation));
    }

    @Override
    public Identifier getTextureLocation(BulbRenderState state) {
        return TEXTURES.get(state.variant, state.expression);
    }

    @Override
    public BulbRenderState createRenderState() {
        return new BulbRenderState();
    }

    @Override
    protected Expression expression(Bulb entity, BulbRenderState state) {
        return Expression.pick(entity, false, entity.isDancing(), state.sleepy);
    }

    @Override
    public void extractRenderState(Bulb entity, BulbRenderState state, float partialTicks) {
        state.variant = Mth.clamp(entity.getVariant(), 0, VARIANTS.length - 1);
        state.squash = entity.squash.get(partialTicks);
        state.earLeft = entity.earLeft.get(partialTicks);
        state.earRight = entity.earRight.get(partialTicks);
        state.earPerk = entity.earPerk.get(partialTicks);
        state.dancing = entity.isDancing();
        state.airborne = !entity.onGround();
        // left alone and sitting still, it dozes off now and then for half a minute
        boolean idle = !state.dancing && entity.onGround() && entity.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 && state.earPerk < 0.1F;
        state.sleepy = idle && Math.floorMod(entity.tickCount + entity.getId() * 211, 1800) > 1300;
        super.extractRenderState(entity, state, partialTicks);
    }
}
