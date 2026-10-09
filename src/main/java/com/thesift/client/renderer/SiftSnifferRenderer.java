package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.SiftSnifferClient;
import com.thesift.client.model.SiftSnifferModel;
import com.thesift.client.renderer.state.SiftSnifferRenderState;
import com.thesift.entity.SiftSniffer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;

/**
 * E1 the Sift Sniffer: Mojang's Sniffer and Snifflet models re-themed (two models, like vanilla's
 * SnifferRenderer), one texture per facial expression; grazing and trumpeting it shuts its eyes happily.
 */
public class SiftSnifferRenderer extends SiftMobRenderer<SiftSniffer, SiftSnifferRenderState, SiftSnifferModel> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.HAPPY, Expression.ANGRY, Expression.HURT, Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("sift_sniffer", PAINTED);
    private static final ExpressionTextures BABY_TEXTURES = ExpressionTextures.single("sift_sniffer_baby", PAINTED);

    private final SiftSnifferModel adult;
    private final SiftSnifferModel baby;

    public SiftSnifferRenderer(EntityRendererProvider.Context context) {
        super(context, new SiftSnifferModel(context.bakeLayer(SiftSnifferClient.SIFT_SNIFFER), false), 1.1F);
        this.adult = this.model;
        this.baby = new SiftSnifferModel(context.bakeLayer(SiftSnifferClient.SIFT_SNIFFER_BABY), true);
    }

    @Override
    public void submit(SiftSnifferRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = state.isBaby ? this.baby : this.adult;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    protected float bounciness() {
        return 0.35F;
    }

    @Override
    public Identifier getTextureLocation(SiftSnifferRenderState state) {
        return (state.isBaby ? BABY_TEXTURES : TEXTURES).get(state.expression);
    }

    @Override
    protected Expression expression(SiftSniffer entity, SiftSnifferRenderState state) {
        byte mode = entity.getState();
        boolean fierce = entity.isAngry() || mode == SiftSniffer.WINDUP || mode == SiftSniffer.CHARGING;
        boolean happy = mode == SiftSniffer.GRAZING
                || entity.trumpetAnimation.isStarted() && entity.trumpetAnimation.getTimeInMillis(entity.tickCount) < 1300;
        return Expression.pick(entity, fierce, happy, false);
    }

    /** Like vanilla's: the long nose reaches well past the hitbox. */
    @Override
    protected AABB getBoundingBoxForCulling(SiftSniffer entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.0F);
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
