package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SlumblerModel;
import com.thesift.client.renderer.state.SlumblerRenderState;
import com.thesift.entity.Slumbler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class SlumblerRenderer extends MobRenderer<Slumbler, SlumblerRenderState, SlumblerModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/slumbler/slumbler.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/slumbler/slumbler_glow.png");

    public SlumblerRenderer(EntityRendererProvider.Context context) {
        super(context, new SlumblerModel(context.bakeLayer(ModModelLayers.SLUMBLER)), 1.0F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> (s.sleeping ? 0.45F : 0.25F) + 0.25F * Mth.sin(age * 0.05F),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SlumblerRenderState state) {
        return TEXTURE;
    }

    @Override
    public SlumblerRenderState createRenderState() {
        return new SlumblerRenderState();
    }

    @Override
    public void extractRenderState(Slumbler entity, SlumblerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.sleeping = entity.isSlumbering();
        state.blink = (entity.tickCount + entity.getId() * 31) % 131 < 4;
        state.yawn.copyFrom(entity.yawnAnimation);
        state.bite.copyFrom(entity.biteAnimation);
    }
}
