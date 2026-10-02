package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
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
    /** The Sift's pink-and-cyan coat and the White Forest's snowy one (index = Stomper.getCoat()). */
    private static final String[] COATS = {"stomper", "stomper_white"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("stomper", COATS, Expression.BLINK, Expression.HAPPY, Expression.ANGRY,
            Expression.HURT, Expression.DEAD);
    private static final ExpressionTextures GLOW = new ExpressionTextures("stomper", COATS, "_glow", Expression.BLINK,
            Expression.HAPPY, Expression.ANGRY, Expression.HURT, Expression.DEAD);

    public StomperRenderer(EntityRendererProvider.Context context) {
        super(context, new StomperModel(context.bakeLayer(ModModelLayers.STOMPER)), 1.3F);
        // glowing eyes, sculk veins and sprouts; the spiracles shine brighter with the Chrome it has stored
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.coat, s.expression), (s, age) -> 0.5F + 0.5F * s.chrome * (0.7F + 0.3F * Mth.sin(age * 0.12F)), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(StomperRenderState state) {
        return TEXTURES.get(state.coat, state.expression);
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
            poseStack.scale(0.5F, 0.5F, 0.5F);
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
        state.coat = entity.getCoat();
        state.shake.copyFrom(entity.shakeAnimation);
        state.sniff.copyFrom(entity.sniffAnimation);
        state.dancing = entity.isDancing();
        state.danceTicks = entity.getDanceTicks() - partialTicks;
        state.sitting = entity.isInSittingPose();
        state.seed = (entity.getId() * 31) % 113;
        state.drink.copyFrom(entity.drinkAnimation);
        state.spray.copyFrom(entity.sprayAnimation);
        state.stomp.copyFrom(entity.stompAnimation);
        state.puff.copyFrom(entity.puffAnimation);
        state.slap.copyFrom(entity.slapAnimation);
        state.drum.copyFrom(entity.drumAnimation);
        state.drumBeats = entity.drumBeats;
        state.ridden = entity.isVehicle();
    }
}
