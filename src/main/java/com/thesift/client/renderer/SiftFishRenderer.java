package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.renderer.state.SiftFishRenderState;
import com.thesift.entity.FanfareEel;
import com.thesift.entity.SculkFish;
import com.thesift.entity.SiftFish;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders the music fish: their faces, their colour variants (CR3), a squishy hit, and an overall size. */
public class SiftFishRenderer<T extends SiftFish, M extends EntityModel<SiftFishRenderState>> extends SiftMobRenderer<T, SiftFishRenderState, M> {
    private final ExpressionTextures textures;
    private final ExpressionTextures glow;
    private final float size;

    public SiftFishRenderer(EntityRendererProvider.Context context, M model, String name, float shadow, float size, Expression... painted) {
        this(context, model, name, new String[]{name}, shadow, size, painted);
    }

    /** CR3: variants are the texture names of the fish's colour variants, in the order of SiftFish.getVariant(). */
    public SiftFishRenderer(EntityRendererProvider.Context context, M model, String name, String[] variants, float shadow, float size,
            Expression... painted) {
        super(context, model, shadow);
        this.textures = new ExpressionTextures(name, variants, painted);
        this.glow = new ExpressionTextures(name, variants, "_glow", painted);
        this.size = size;
        // glowing sculk lines, spots, sprouts and eyes, softly pulsing
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> this.glow.get(s.variant, s.expression), (s, age) -> s.glowPulse >= 0.0F ? s.glowPulse : 0.7F + 0.3F * Mth.sin(age * 0.1F + s.seed),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SiftFishRenderState state) {
        return this.textures.get(state.variant, state.expression);
    }

    @Override
    protected Expression expression(T entity, SiftFishRenderState state) {
        boolean angry = entity.isAggressive();
        if (entity instanceof SculkFish) {
            return Expression.pick(entity, angry, false, false);
        }
        boolean happy = entity instanceof FanfareEel eel && eel.biteAnimation.isStarted() && eel.biteAnimation.getTimeInMillis(entity.tickCount) < 400;
        return Expression.pick(entity, angry || happy, false, false);
    }

    @Override
    protected void scale(SiftFishRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (this.size != 1.0F) {
            poseStack.scale(this.size, this.size, this.size);
        }
    }

    @Override
    public SiftFishRenderState createRenderState() {
        return new SiftFishRenderState();
    }

    @Override
    public void extractRenderState(T entity, SiftFishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.effort = Mth.lerp(partialTicks, entity.effortO, entity.effort);
        state.inLiquid = entity.inLiquid();
        state.seed = (entity.getId() * 37) % 101;
        state.variant = entity.getVariant();
        state.aggressive = entity.isAggressive();
        if (entity instanceof SculkFish sculk) {
            state.snap.copyFrom(sculk.biteAnimation);
        } else {
            state.snap.stop();
        }
        if (entity instanceof FanfareEel eel) {
            state.bite.copyFrom(eel.biteAnimation);
        } else {
            state.bite.stop();
        }
    }
}
