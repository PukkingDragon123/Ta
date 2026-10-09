package com.thesift.client.renderer;

import com.thesift.client.Expression;
import com.thesift.client.SiftSnifferClient;
import com.thesift.client.model.SiftSnifferModel;
import com.thesift.client.renderer.state.SiftSnifferRenderState;
import com.thesift.entity.SiftSniffer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/** E1 the Sift Sniffer (one texture per facial expression; grazing and trumpeting it shuts its eyes happily). */
public class SiftSnifferRenderer extends SiftMobRenderer<SiftSniffer, SiftSnifferRenderState, SiftSnifferModel> {
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("sift_sniffer", Expression.BLINK, Expression.HAPPY,
            Expression.ANGRY, Expression.HURT, Expression.DEAD);

    public SiftSnifferRenderer(EntityRendererProvider.Context context) {
        super(context, new SiftSnifferModel(context.bakeLayer(SiftSnifferClient.SIFT_SNIFFER)), 1.0F);
    }

    @Override
    protected float bounciness() {
        return 0.35F;
    }

    @Override
    public Identifier getTextureLocation(SiftSnifferRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected Expression expression(SiftSniffer entity, SiftSnifferRenderState state) {
        byte mode = entity.getState();
        boolean fierce = entity.isAngry() || mode == SiftSniffer.WINDUP || mode == SiftSniffer.CHARGING;
        boolean happy = mode == SiftSniffer.GRAZING
                || entity.trumpetAnimation.isStarted() && entity.trumpetAnimation.getTimeInMillis(entity.tickCount) < 1300;
        return Expression.pick(entity, fierce, happy, false);
    }

    @Override
    public SiftSnifferRenderState createRenderState() {
        return new SiftSnifferRenderState();
    }

    @Override
    public void extractRenderState(SiftSniffer entity, SiftSnifferRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.graze.copyFrom(entity.grazeAnimation);
        state.sniff.copyFrom(entity.sniffAnimation);
        state.dig.copyFrom(entity.digAnimation);
        state.trumpet.copyFrom(entity.trumpetAnimation);
        state.windup.copyFrom(entity.windupAnimation);
        state.lay.copyFrom(entity.layAnimation);
        state.charging = entity.getState() == SiftSniffer.CHARGING;
        state.garden = entity.getGarden();
        state.seed = (entity.getId() * 37) % 200;
    }
}
