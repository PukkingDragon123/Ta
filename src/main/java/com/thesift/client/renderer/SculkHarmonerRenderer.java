package com.thesift.client.renderer;

import com.thesift.client.Expression;
import com.thesift.client.model.HarmonerModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.HarmonerRenderState;
import com.thesift.entity.SculkHarmoner;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

/** The staff's sculk songbird: always in flight, always singing, fading out as its song ends. */
public class SculkHarmonerRenderer extends MobRenderer<SculkHarmoner, HarmonerRenderState, HarmonerModel> {
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("harmoner", new String[]{"harmoner_sculk"}, Expression.BLINK,
            Expression.HAPPY);

    public SculkHarmonerRenderer(EntityRendererProvider.Context context) {
        super(context, new HarmonerModel(context.bakeLayer(ModModelLayers.HARMONER)), 0.2F);
    }

    @Override
    public Identifier getTextureLocation(HarmonerRenderState state) {
        return TEXTURES.get(0, state.expression);
    }

    @Override
    public HarmonerRenderState createRenderState() {
        return new HarmonerRenderState();
    }

    @Override
    public void extractRenderState(SculkHarmoner entity, HarmonerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.flap = 1.0F;
        state.guiding = true;
        state.seed = (entity.getId() * 53) % 97;
        state.sing.copyFrom(entity.singAnimation);
        state.expression = Expression.HAPPY;
    }

    @Override
    protected int getBlockLightLevel(SculkHarmoner entity, net.minecraft.core.BlockPos pos) {
        return 15;
    }
}
