package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.model.Anim;
import com.thesift.client.model.BulbModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.layers.BulbFlowerLayer;
import com.thesift.client.renderer.layers.BulbJellyLayer;
import com.thesift.client.renderer.state.BulbRenderState;
import com.thesift.entity.Bulb;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MushroomCowRenderer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

public class BulbRenderer extends SiftMobRenderer<Bulb, BulbRenderState, BulbModel> {
    private static final String[] VARIANTS = {"bulb_blue", "bulb_white"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("bulb", VARIANTS, Expression.BLINK, Expression.HAPPY,
            Expression.HURT, Expression.DEAD, Expression.SLEEP);
    /** The model is drawn at this size: a little cube about a third of a block across. */
    private static final float SIZE = 0.6F;
    private static final float BABY = 0.55F;
    private final BlockModelResolver blockModelResolver;

    public BulbRenderer(EntityRendererProvider.Context context) {
        super(context, new BulbModel(context.bakeLayer(ModModelLayers.BULB), BulbModel.Pass.CORE), 0.25F);
        this.blockModelResolver = context.getBlockModelResolver();
        this.addLayer(new BulbFlowerLayer(this));
        this.addLayer(new BulbJellyLayer(this, context.getModelSet(), this::getTextureLocation));
    }

    @Override
    public Identifier getTextureLocation(BulbRenderState state) {
        return TEXTURES.get(state.variant, state.expression);
    }

    @Override
    public BulbRenderState createRenderState() {
        return new BulbRenderState();
    }

    @Override
    protected void scale(BulbRenderState state, PoseStack poseStack) {
        float s = state.isBaby ? SIZE * BABY : SIZE;
        poseStack.scale(s, s, s);
        super.scale(state, poseStack);
    }

    @Override
    protected Expression expression(Bulb entity, BulbRenderState state) {
        boolean wiggling = entity.wiggleAnimation.isStarted() && entity.wiggleAnimation.getTimeInMillis(entity.tickCount) < 1400;
        boolean swapping = (entity.placeAnimation.isStarted() && entity.placeAnimation.getTimeInMillis(entity.tickCount) < 1600)
                || (entity.pluckAnimation.isStarted() && entity.pluckAnimation.getTimeInMillis(entity.tickCount) < 1200);
        return Expression.pick(entity, false, entity.isDancing() || wiggling || swapping, state.sleepy);
    }

    @Override
    public void extractRenderState(Bulb entity, BulbRenderState state, float partialTicks) {
        state.variant = entity.getVariant();
        state.squash = entity.squash.get(partialTicks);
        state.earLeft = entity.earLeft.get(partialTicks);
        state.earRight = entity.earRight.get(partialTicks);
        state.earPerk = entity.earPerk.get(partialTicks);
        state.flowerSway = entity.flowerSway.get(partialTicks);
        state.dancing = entity.isDancing();
        state.airborne = !entity.onGround();
        state.fallSpeed = (float) entity.getDeltaMovement().y;
        state.sleepy = entity.isSleepingBulb();
        state.beat = entity.tickCount - entity.lastBeat + partialTicks;
        state.sniff.copyFrom(entity.sniffAnimation);
        state.groom.copyFrom(entity.groomAnimation);
        state.wiggle.copyFrom(entity.wiggleAnimation);
        state.nibble.copyFrom(entity.nibbleAnimation);
        state.place.copyFrom(entity.placeAnimation);
        state.pluck.copyFrom(entity.pluckAnimation);
        // the back flowers are block models, resolved like a Mooshroom's mushrooms
        for (int i = 0; i < Bulb.FLOWER_SLOTS; i++) {
            BlockState flower = entity.getBackFlower(i);
            if (flower.isAir()) {
                state.flowers[i].clear();
            } else {
                this.blockModelResolver.update(state.flowers[i], flower, MushroomCowRenderer.BLOCK_DISPLAY_CONTEXT);
            }
            // a flower that just landed pops in with a little overshoot
            float since = entity.tickCount - entity.flowerChanged[i] + partialTicks;
            state.flowerPop[i] = since >= 7.0F ? 1.0F : Anim.backOut(Math.max(0.0F, since) / 7.0F);
        }
        BlockState held = entity.getHeldFlower();
        if (held.isAir()) {
            state.held.clear();
        } else {
            this.blockModelResolver.update(state.held, held, MushroomCowRenderer.BLOCK_DISPLAY_CONTEXT);
        }
        super.extractRenderState(entity, state, partialTicks);
    }
}
