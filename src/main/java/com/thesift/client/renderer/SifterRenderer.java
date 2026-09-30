package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SifterModel;
import com.thesift.client.renderer.state.SifterRenderState;
import com.thesift.entity.Sifter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class SifterRenderer extends MobRenderer<Sifter, SifterRenderState, SifterModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/sifter/sifter.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/sifter/sifter_glow.png");

    public SifterRenderer(EntityRendererProvider.Context context) {
        super(context, new SifterModel(context.bakeLayer(ModModelLayers.SIFTER)), 0.5F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> 0.8F + 0.2F * Mth.sin(age * 0.2F), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SifterRenderState state) {
        return TEXTURE;
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
