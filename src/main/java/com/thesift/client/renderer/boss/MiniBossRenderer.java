package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.renderer.ExpressionTextures;
import com.thesift.client.renderer.SiftMobRenderer;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.MiniBoss;
import com.thesift.entity.boss.Thumper;
import java.util.function.BiFunction;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

/** Renders a mini-boss at its giant scale, with its faces and its glowing parts. */
public class MiniBossRenderer<T extends MiniBoss, M extends EntityModel<MiniBossRenderState>> extends SiftMobRenderer<T, MiniBossRenderState, M> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.ANGRY, Expression.HURT, Expression.DEAD};
    private final ExpressionTextures textures;
    private final ExpressionTextures glow;
    private final float scale;
    private final BiFunction<T, MiniBossRenderState, Expression> face;
    /** The Thumper's titan skin (same layout, swallowed by sculk), faded in as it grows. */
    private final @Nullable ExpressionTextures titanTextures;
    private final @Nullable ExpressionTextures titanGlow;

    public MiniBossRenderer(EntityRendererProvider.Context context, M model, String name, float scale, float shadow,
            BiFunction<T, MiniBossRenderState, Expression> face) {
        this(context, model, name, scale, shadow, face, null);
    }

    public MiniBossRenderer(EntityRendererProvider.Context context, M model, String name, float scale, float shadow,
            BiFunction<T, MiniBossRenderState, Expression> face, @Nullable String titanSkin) {
        super(context, model, shadow * scale);
        this.scale = scale;
        this.face = face;
        this.textures = ExpressionTextures.single(name, PAINTED);
        this.glow = new ExpressionTextures(name, new String[]{name}, "_glow", PAINTED);
        this.titanTextures = titanSkin == null ? null : new ExpressionTextures(name, new String[]{titanSkin}, PAINTED);
        this.titanGlow = titanSkin == null ? null : new ExpressionTextures(name, new String[]{titanSkin}, "_glow", PAINTED);
        if (this.titanTextures != null) {
            this.addLayer(new TitanSkinLayer<>(this, s -> this.titanTextures.get(s.expression)));
        }
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> (s.titan > 0.5F && this.titanGlow != null ? this.titanGlow : this.glow).get(s.expression),
                (s, age) -> s.bossState != MiniBoss.IDLE ? 1.0F : 0.6F + 0.25F * Mth.sin(age * 0.08F), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(MiniBossRenderState state) {
        if (state.titan >= 0.999F && this.titanTextures != null) {
            return this.titanTextures.get(state.expression);
        }
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
        if (entity instanceof Thumper th) {
            state.titan = th.growth(partialTicks);
            state.ridden = state.titan > 0.0F && th.level().players().stream().anyMatch(th::isOnBack);
        } else {
            state.titan = 0.0F;
            state.ridden = false;
        }
        super.extractRenderState(entity, state, partialTicks);
    }

    /** Wings, necks and tails reach past the box (the titan's head and tail far past it). */
    @Override
    protected AABB getBoundingBoxForCulling(T entity, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
        if (entity instanceof Thumper th && th.isTitan()) {
            return box.inflate(entity.getBbWidth() * 1.4, entity.getBbHeight() * 0.6, entity.getBbWidth() * 1.4);
        }
        return box.inflate(1.5, 1.0, 1.5);
    }

    private static float titanSize(MiniBossRenderState state) {
        return 1.0F + (Thumper.TITAN_SCALE - 1.0F) * state.titan;
    }

    @Override
    protected void scale(MiniBossRenderState state, PoseStack poseStack) {
        float k = this.scale * titanSize(state);
        poseStack.scale(k, k, k);
        super.scale(state, poseStack);
    }

    @Override
    protected float getShadowRadius(MiniBossRenderState state) {
        return super.getShadowRadius(state) * titanSize(state);
    }
}
