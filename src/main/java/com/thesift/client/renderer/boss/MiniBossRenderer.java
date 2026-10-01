package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.renderer.ExpressionTextures;
import com.thesift.client.renderer.SiftMobRenderer;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.MiniBoss;
import java.util.function.BiFunction;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders a mini-boss at its giant scale, with its faces and its glowing parts. */
public class MiniBossRenderer<T extends MiniBoss, M extends EntityModel<MiniBossRenderState>> extends SiftMobRenderer<T, MiniBossRenderState, M> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.ANGRY, Expression.HURT, Expression.DEAD};
    private final ExpressionTextures textures;
    private final ExpressionTextures glow;
    private final float scale;
    private final BiFunction<T, MiniBossRenderState, Expression> face;

    public MiniBossRenderer(EntityRendererProvider.Context context, M model, String name, float scale, float shadow,
            BiFunction<T, MiniBossRenderState, Expression> face) {
        super(context, model, shadow * scale);
        this.scale = scale;
        this.face = face;
        this.textures = ExpressionTextures.single(name, PAINTED);
        this.glow = new ExpressionTextures(name, new String[]{name}, "_glow", PAINTED);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> this.glow.get(s.expression),
                (s, age) -> s.bossState != MiniBoss.IDLE ? 1.0F : 0.6F + 0.25F * Mth.sin(age * 0.08F), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(MiniBossRenderState state) {
        return this.textures.get(state.expression);
    }

    @Override
    public MiniBossRenderState createRenderState() {
        return new MiniBossRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.45F;
    }

    @Override
    protected Expression expression(T entity, MiniBossRenderState state) {
        Expression e = this.face.apply(entity, state);
        return e != null ? e : Expression.pick(entity, entity.getTarget() != null || entity.getState() != MiniBoss.IDLE, false, false);
    }

    @Override
    public void extractRenderState(T entity, MiniBossRenderState state, float partialTicks) {
        state.bossState = entity.getState();
        state.stateTime = entity.stateTime(partialTicks);
        state.grounded = entity.onGround();
        state.enraged = entity.getHealth() < entity.getMaxHealth() * 0.5F;
        super.extractRenderState(entity, state, partialTicks);
    }

    @Override
    protected void scale(MiniBossRenderState state, PoseStack poseStack) {
        poseStack.scale(this.scale, this.scale, this.scale);
        super.scale(state, poseStack);
    }
}
