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

public class HarmonerRenderer extends MobRenderer<Harmoner, HarmonerRenderState, HarmonerModel> {
    private static final Identifier[] TEXTURES = new Identifier[Harmoner.VARIANTS];

    static {
        for (int i = 0; i < Harmoner.VARIANTS; i++) {
            TEXTURES[i] = TheSift.id("textures/entity/harmoner/harmoner_" + Harmoner.NAMES[i] + ".png");
        }
    }

    public HarmonerRenderer(EntityRendererProvider.Context context) {
        super(context, new HarmonerModel(context.bakeLayer(ModModelLayers.HARMONER)), 0.3F);
    }

    @Override
    public Identifier getTextureLocation(HarmonerRenderState state) {
        return TEXTURES[state.variant];
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
