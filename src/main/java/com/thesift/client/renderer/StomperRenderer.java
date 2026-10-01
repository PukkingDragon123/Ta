package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.Expression;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.StomperModel;
import com.thesift.client.renderer.state.StomperRenderState;
import com.thesift.entity.Stomper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class StomperRenderer extends SiftMobRenderer<Stomper, StomperRenderState, StomperModel> {
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("stomper", Expression.BLINK, Expression.HAPPY, Expression.ANGRY,
            Expression.HURT, Expression.DEAD);
    private static final Identifier GLOW = TheSift.id("textures/entity/stomper/stomper_glow.png");

    public StomperRenderer(EntityRendererProvider.Context context) {
        super(context, new StomperModel(context.bakeLayer(ModModelLayers.STOMPER)), 1.3F);
        // the spiracles shine with the Chrome it has stored
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> s.chrome * (0.65F + 0.35F * Mth.sin(age * 0.12F)), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(StomperRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected float bounciness() {
        return 0.45F;
    }

    @Override
    protected Expression expression(Stomper entity, StomperRenderState state) {
        boolean spraying = entity.sprayAnimation.isStarted() && entity.sprayAnimation.getTimeInMillis(entity.tickCount) < 2600;
        boolean drinking = entity.drinkAnimation.isStarted() && entity.drinkAnimation.getTimeInMillis(entity.tickCount) < 3000;
        boolean nap = entity.isInSittingPose() && Math.floorMod(entity.tickCount + entity.getId() * 97, 1600) > 1100;
        return Expression.pick(entity, entity.isAggressive() || spraying, entity.isDancing() || drinking, nap);
    }

    @Override
    protected void scale(StomperRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.isBaby) {
            poseStack.scale(0.45F, 0.45F, 0.45F);
        }
    }

    /** The head and trunk reach well past the hitbox. */
    @Override
    protected AABB getBoundingBoxForCulling(Stomper entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.2, 0.5, 1.2);
    }

    @Override
    public StomperRenderState createRenderState() {
        return new StomperRenderState();
    }

    @Override
    public void extractRenderState(Stomper entity, StomperRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chrome = entity.getChrome();
        state.dancing = entity.isDancing();
        state.danceTicks = entity.getDanceTicks() - partialTicks;
        state.sitting = entity.isInSittingPose();
        state.seed = (entity.getId() * 31) % 113;
        state.drink.copyFrom(entity.drinkAnimation);
        state.spray.copyFrom(entity.sprayAnimation);
        state.stomp.copyFrom(entity.stompAnimation);
        state.puff.copyFrom(entity.puffAnimation);
        state.slap.copyFrom(entity.slapAnimation);
    }
}
