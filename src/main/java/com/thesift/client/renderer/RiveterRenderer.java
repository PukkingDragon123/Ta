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

public class RiveterRenderer extends MobRenderer<Riveter, RiveterRenderState, RiveterModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/riveter/riveter.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/riveter/riveter_glow.png");

    public RiveterRenderer(EntityRendererProvider.Context context) {
        super(context, new RiveterModel(context.bakeLayer(ModModelLayers.RIVETER)), 0.4F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> {
            float scream = s.scream.isStarted() ? Math.max(0.0F, 1.0F - s.scream.getTimeInMillis(age) / 1800.0F) : 0.0F;
            return Math.min(1.0F, 0.35F + 0.2F * Mth.sin(age * 0.08F) + scream);
        }, this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(RiveterRenderState state) {
        return TEXTURE;
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
        state.dying = state.deathTime;
        state.deathTime = 0.0F;
    }
}
