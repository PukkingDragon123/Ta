package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.SlumblerClient;
import com.thesift.client.model.SlumblerEggsModel;
import com.thesift.client.renderer.state.SlumblerEggsRenderState;
import com.thesift.entity.slumbler.SlumblerEggs;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** CR2: a clutch of Slumbler eggs, clear jelly with the rainbow tadpoles showing through. */
public class SlumblerEggsRenderer extends SiftMobRenderer<SlumblerEggs, SlumblerEggsRenderState, SlumblerEggsModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/slumbler_eggs/slumbler_eggs.png");

    public SlumblerEggsRenderer(EntityRendererProvider.Context context) {
        super(context, new SlumblerEggsModel(context.bakeLayer(SlumblerClient.SLUMBLER_EGGS)), 0.3F);
    }

    @Override
    public Identifier getTextureLocation(SlumblerEggsRenderState state) {
        return TEXTURE;
    }

    @Override
    protected float bounciness() {
        return 0.5F;
    }

    @Override
    public SlumblerEggsRenderState createRenderState() {
        return new SlumblerEggsRenderState();
    }

    @Override
    public void extractRenderState(SlumblerEggs entity, SlumblerEggsRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.progress = entity.progress();
        state.afloat = entity.isInFluidType();
        state.seed = (entity.getId() * 29) % 89;
    }
}
