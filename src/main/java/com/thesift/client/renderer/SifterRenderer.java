package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SifterModel;
import com.thesift.client.renderer.state.SifterRenderState;
import com.thesift.entity.Sifter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class SifterRenderer extends SiftMobRenderer<Sifter, SifterRenderState, SifterModel> {
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("sifter", com.thesift.client.Expression.BLINK, com.thesift.client.Expression.ANGRY, com.thesift.client.Expression.HURT, com.thesift.client.Expression.DEAD);

    public SifterRenderer(EntityRendererProvider.Context context) {
        super(context, new SifterModel(context.bakeLayer(ModModelLayers.SIFTER)), 0.55F);
    }

    @Override
    public Identifier getTextureLocation(SifterRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    public SifterRenderState createRenderState() {
        return new SifterRenderState();
    }

    @Override
    protected float getShadowRadius(SifterRenderState state) {
        return state.burrowed ? 0.0F : super.getShadowRadius(state);
    }

    @Override
    public void extractRenderState(Sifter entity, SifterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.burrowed = entity.isBurrowed();
        state.squash = entity.squash.get(partialTicks);
        state.antennaLeft = entity.antennaLeft.get(partialTicks);
        state.antennaRight = entity.antennaRight.get(partialTicks);
        state.chomp.copyFrom(entity.chompAnimation);
        state.emerge.copyFrom(entity.emergeAnimation);
    }
}
