package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.BulbModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.BulbRenderState;
import com.thesift.entity.Bulb;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class BulbRenderer extends MobRenderer<Bulb, BulbRenderState, BulbModel> {
    private static final String[] VARIANTS = {"sky", "blossom", "dusk", "starry"};
    private static final Identifier[] TEXTURES = new Identifier[VARIANTS.length];

    static {
        for (int i = 0; i < VARIANTS.length; i++) {
            TEXTURES[i] = TheSift.id("textures/entity/bulb/bulb_" + VARIANTS[i] + ".png");
        }
    }

    public BulbRenderer(EntityRendererProvider.Context context) {
        super(context, new BulbModel(context.bakeLayer(ModModelLayers.BULB)), 0.42F);
    }

    @Override
    public Identifier getTextureLocation(BulbRenderState state) {
        return TEXTURES[state.variant];
    }

    @Override
    public BulbRenderState createRenderState() {
        return new BulbRenderState();
    }

    @Override
    public void extractRenderState(Bulb entity, BulbRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Mth.clamp(entity.getVariant(), 0, VARIANTS.length - 1);
        state.squash = entity.squash.get(partialTicks);
        state.earLeft = entity.earLeft.get(partialTicks);
        state.earRight = entity.earRight.get(partialTicks);
        state.earPerk = entity.earPerk.get(partialTicks);
        state.dancing = entity.isDancing();
        state.airborne = !entity.onGround();
        state.blink = (entity.tickCount + entity.getId() * 37) % 83 < 3;
        // melt into a puddle (BulbModel) rather than the usual tip-over
        state.melt = state.deathTime;
        state.deathTime = 0.0F;
    }
}
