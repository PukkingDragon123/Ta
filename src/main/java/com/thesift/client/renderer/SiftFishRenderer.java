package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.renderer.state.SiftFishRenderState;
import com.thesift.entity.FanfareEel;
import com.thesift.entity.SiftFish;
import com.thesift.entity.Tubafish;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders the music fish: their faces, a squishy hit, and an overall size. */
public class SiftFishRenderer<T extends SiftFish, M extends EntityModel<SiftFishRenderState>> extends SiftMobRenderer<T, SiftFishRenderState, M> {
    private final ExpressionTextures textures;
    private final ExpressionTextures glow;
    private final float size;

    public SiftFishRenderer(EntityRendererProvider.Context context, M model, String name, float shadow, float size, Expression... painted) {
        super(context, model, shadow);
        this.textures = ExpressionTextures.single(name, painted);
        this.glow = new ExpressionTextures(name, new String[]{name}, "_glow", painted);
        this.size = size;
        // glowing sculk lines, spots, sprouts and eyes, softly pulsing
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> this.glow.get(s.expression), (s, age) -> 0.7F + 0.3F * Mth.sin(age * 0.1F + s.seed),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SiftFishRenderState state) {
        return this.textures.get(state.expression);
    }

    @Override
    protected Expression expression(T entity, SiftFishRenderState state) {
        boolean angry = entity.isAggressive() || (entity instanceof Tubafish t && t.isPuffed());
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
        state.puff = entity instanceof Tubafish t ? Mth.lerp(partialTicks, t.puffO, t.puff) : 0.0F;
        if (entity instanceof FanfareEel eel) {
            state.bite.copyFrom(eel.biteAnimation);
        } else {
            state.bite.stop();
        }
    }
}
