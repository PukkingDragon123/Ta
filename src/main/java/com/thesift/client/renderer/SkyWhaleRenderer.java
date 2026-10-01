package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.Expression;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SkyWhaleModel;
import com.thesift.client.renderer.state.SkyWhaleRenderState;
import com.thesift.entity.SkyWhale;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class SkyWhaleRenderer extends SiftMobRenderer<SkyWhale, SkyWhaleRenderState, SkyWhaleModel> {
    private static final float SIZE = 1.5F;
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("sky_whale", Expression.BLINK, Expression.HAPPY, Expression.ANGRY,
            Expression.HURT, Expression.DEAD);
    private static final Identifier GLOW = TheSift.id("textures/entity/sky_whale/sky_whale_glow.png");

    public SkyWhaleRenderer(EntityRendererProvider.Context context) {
        super(context, new SkyWhaleModel(context.bakeLayer(ModModelLayers.SKY_WHALE)), 2.2F);
        // freckles and the nose ring glow softly, pulsing slowly
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> 0.55F + 0.35F * Mth.sin(age * 0.05F), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SkyWhaleRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected float bounciness() {
        return 0.3F;
    }

    @Override
    protected Expression expression(SkyWhale entity, SkyWhaleRenderState state) {
        boolean singing = entity.singAnimation.isStarted() && entity.singAnimation.getTimeInMillis(entity.tickCount) < 3200;
        return Expression.pick(entity, false, singing, false);
    }

    @Override
    protected void scale(SkyWhaleRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(SIZE, SIZE, SIZE);
    }

    /** The whale is far longer than its hitbox: keep drawing it while any of it is on screen. */
    @Override
    protected AABB getBoundingBoxForCulling(SkyWhale entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(6.0, 2.0, 6.0);
    }

    @Override
    public SkyWhaleRenderState createRenderState() {
        return new SkyWhaleRenderState();
    }

    @Override
    public void extractRenderState(SkyWhale entity, SkyWhaleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.seed = (entity.getId() * 41) % 127;
        state.sing.copyFrom(entity.singAnimation);
        state.spit.copyFrom(entity.spitAnimation);
    }
}
