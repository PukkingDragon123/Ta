package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.SlumblerClient;
import com.thesift.client.model.SlumblerTadpoleModel;
import com.thesift.client.renderer.state.SlumblerTadpoleRenderState;
import com.thesift.entity.slumbler.SlumblerTadpole;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** CR2: a Slumbler tadpole in its rainbow scales; the glowing tip of its tail pulses as it swims. */
public class SlumblerTadpoleRenderer extends SiftMobRenderer<SlumblerTadpole, SlumblerTadpoleRenderState, SlumblerTadpoleModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/slumbler_tadpole/slumbler_tadpole.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/slumbler_tadpole/slumbler_tadpole_glow.png");
    /** A tadpole is drawn smaller than the model is built. */
    private static final float SIZE = 0.8F;

    public SlumblerTadpoleRenderer(EntityRendererProvider.Context context) {
        super(context, new SlumblerTadpoleModel(context.bakeLayer(SlumblerClient.SLUMBLER_TADPOLE)), 0.3F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> 0.6F + 0.4F * Mth.sin(age * 0.2F + s.seed), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SlumblerTadpoleRenderState state) {
        return TEXTURE;
    }

    @Override
    public SlumblerTadpoleRenderState createRenderState() {
        return new SlumblerTadpoleRenderState();
    }

    @Override
    protected void scale(SlumblerTadpoleRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(SIZE, SIZE, SIZE);
    }

    @Override
    public void extractRenderState(SlumblerTadpole entity, SlumblerTadpoleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.effort = Mth.lerp(partialTicks, entity.effortO, entity.effort);
        state.inLiquid = entity.inLiquid();
        state.seed = (entity.getId() * 37) % 101;
        state.bite.copyFrom(entity.biteAnimation);
        state.crash.copyFrom(entity.crashAnimation);
    }
}
