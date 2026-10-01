package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.HarmonerModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.HarmonerRenderState;
import com.thesift.entity.Harmoner;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class HarmonerRenderer extends SiftMobRenderer<Harmoner, HarmonerRenderState, HarmonerModel> {
    private static final String[] VARIANTS = new String[Harmoner.VARIANTS];

    static {
        for (int i = 0; i < Harmoner.VARIANTS; i++) {
            VARIANTS[i] = "harmoner_" + Harmoner.NAMES[i];
        }
    }

    private static final ExpressionTextures TEXTURES = new ExpressionTextures("harmoner", VARIANTS, com.thesift.client.Expression.BLINK, com.thesift.client.Expression.HAPPY, com.thesift.client.Expression.HURT,
            com.thesift.client.Expression.DEAD, com.thesift.client.Expression.SLEEP);

    public HarmonerRenderer(EntityRendererProvider.Context context) {
        super(context, new HarmonerModel(context.bakeLayer(ModModelLayers.HARMONER)), 0.3F);
    }

    @Override
    public Identifier getTextureLocation(HarmonerRenderState state) {
        return TEXTURES.get(state.variant, state.expression);
    }

    @Override
    protected com.thesift.client.Expression expression(Harmoner entity, HarmonerRenderState state) {
        boolean singing = entity.singAnimation.isStarted() && entity.singAnimation.getTimeInMillis(entity.tickCount) < 2500;
        boolean idle = entity.onGround() && entity.getDeltaMovement().horizontalDistanceSqr() < 1.0E-4 && !entity.isGuiding();
        boolean nap = idle && Math.floorMod(entity.tickCount + entity.getId() * 173, 2000) > 1500;
        return com.thesift.client.Expression.pick(entity, false, singing || entity.isGuiding(), nap);
    }

    @Override
    public HarmonerRenderState createRenderState() {
        return new HarmonerRenderState();
    }

    @Override
    public void extractRenderState(Harmoner entity, HarmonerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Mth.clamp(entity.getVariant(), 0, Harmoner.VARIANTS - 1);
        state.flap = Mth.lerp(partialTicks, entity.flapO, entity.flap);
        state.guiding = entity.isGuiding();
        state.seed = (entity.getId() * 53) % 97;
        state.sing.copyFrom(entity.singAnimation);
    }
}
