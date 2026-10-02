package com.thesift.client.renderer;

import com.thesift.client.Expression;
import com.thesift.client.SwifterClient;
import com.thesift.client.model.SwifterModel;
import com.thesift.client.renderer.state.SwifterRenderState;
import com.thesift.entity.Swifter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The Swifter and its cubs (one model, one texture per facial expression; a crying cub screws its eyes shut). */
public class SwifterRenderer extends SiftMobRenderer<Swifter, SwifterRenderState, SwifterModel> {
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("swifter", Expression.BLINK, Expression.ANGRY, Expression.SLEEP,
            Expression.HURT, Expression.HAPPY, Expression.DEAD);

    public SwifterRenderer(EntityRendererProvider.Context context) {
        super(context, new SwifterModel(context.bakeLayer(SwifterClient.SWIFTER)), 0.45F);
    }

    @Override
    protected float bounciness() {
        return 0.55F;
    }

    @Override
    public Identifier getTextureLocation(SwifterRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected Expression expression(Swifter entity, SwifterRenderState state) {
        if (entity.isCrying() && entity.deathTime == 0) {
            return Expression.HURT;
        }
        boolean happy = entity.calmAnimation.isStarted() && entity.calmAnimation.getTimeInMillis(entity.tickCount) < 2500;
        boolean fierce = entity.isAngry() || entity.isAggressive() || entity.getMode() == Swifter.CROUCH;
        return Expression.pick(entity, fierce, happy, entity.isNapping());
    }

    @Override
    public SwifterRenderState createRenderState() {
        return new SwifterRenderState();
    }

    @Override
    public void extractRenderState(Swifter entity, SwifterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.mode = entity.getMode();
        state.jet = Mth.lerp(partialTicks, entity.jetO, entity.jet);
        state.sleep = Mth.lerp(partialTicks, entity.sleepO, entity.sleep);
        state.crouch = Mth.lerp(partialTicks, entity.crouchO, entity.crouch);
        state.cry = Mth.lerp(partialTicks, entity.cryO, entity.cry);
        state.pitch = Mth.lerp(partialTicks, entity.pitchO, entity.pitch);
        state.angry = entity.isAngry();
        state.carrying = entity.isCarrying();
        state.grab.copyFrom(entity.grabAnimation);
        state.slam.copyFrom(entity.slamAnimation);
        state.calm.copyFrom(entity.calmAnimation);
        state.snarl.copyFrom(entity.snarlAnimation);
        state.seed = (entity.getId() * 17) % 101;
    }
}
