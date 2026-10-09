package com.thesift.client.renderer;

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

/** S1 the Stomper elephant (and its Stompling young, drawn at half size). */
public class StomperRenderer extends SiftMobRenderer<Stomper, StomperRenderState, StomperModel> {
    /** The Sift's teal-and-crimson coat and the White Forest's frosted one (index = Stomper.getCoat()). */
    private static final String[] COATS = {"stomper", "stomper_white"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("stomper", COATS, Expression.BLINK, Expression.HAPPY, Expression.ANGRY,
            Expression.HURT, Expression.DEAD);
    private static final ExpressionTextures GLOW = new ExpressionTextures("stomper", COATS, "_glow", Expression.BLINK,
            Expression.HAPPY, Expression.ANGRY, Expression.HURT, Expression.DEAD);

    public StomperRenderer(EntityRendererProvider.Context context) {
        super(context, new StomperModel(context.bakeLayer(ModModelLayers.STOMPER)), 1.4F);
        // the glowing buds in its garden
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.coat, s.expression), (s, age) -> 0.75F + 0.25F * Mth.sin(age * 0.1F),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(StomperRenderState state) {
        return TEXTURES.get(state.coat, state.expression);
    }

    /** A heavy animal: hits barely squash it, and it topples rather than bounces when it dies. */
    @Override
    protected float bounciness() {
        return 0.2F;
    }

    @Override
    protected Expression expression(Stomper entity, StomperRenderState state) {
        boolean spraying = entity.sprayAnimation.isStarted() && entity.sprayAnimation.getTimeInMillis(entity.tickCount) < 2600;
        boolean drinking = entity.drinkAnimation.isStarted() && entity.drinkAnimation.getTimeInMillis(entity.tickCount) < 3000;
        boolean nap = entity.isInSittingPose() && Math.floorMod(entity.tickCount + entity.getId() * 97, 1600) > 1100;
        return Expression.pick(entity, entity.isAggressive() || spraying || entity.getGrabPhase() != 0, entity.isDancing() || drinking, nap);
    }

    @Override
    protected void scale(StomperRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.isBaby) {
            poseStack.scale(0.5F, 0.5F, 0.5F);
        }
    }

    /** The trunk reaches far past the hitbox, and high above it with a victim in its grip. */
    @Override
    protected AABB getBoundingBoxForCulling(Stomper entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(2.5, 2.5, 2.5);
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
        state.ridden = entity.isVehicle();
        state.grabPhase = entity.getGrabPhase();
        state.grabTime = entity.getGrabTime(partialTicks);
        state.roll = entity.getRoll(partialTicks);
        state.rollAngle = entity.getRollAngle(partialTicks);
    }
}
