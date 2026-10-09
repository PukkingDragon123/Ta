package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.client.Expression;
import com.thesift.client.renderer.state.SiftRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;

/**
 * Base renderer for the Sift's mobs: picks the facial expression each frame, squishes the body
 * when it is hit, and replaces vanilla's tip-over death with a cartoon pop - a squash, a hop with
 * a stretch, a splat on landing, a swell, and then it is gone in a burst of particles (the burst
 * itself is spawned by the entity, see {@link com.thesift.entity.KillBurst}).
 */
public abstract class SiftMobRenderer<T extends Mob, S extends SiftRenderState, M extends EntityModel<? super S>> extends MobRenderer<T, S, M> {
    // death keyframes: tick, vertical scale, hop height (blocks)
    private static final float[][] DEATH = {
            {0.0F, 1.00F, 0.00F}, {2.0F, 0.62F, 0.00F}, {6.0F, 1.38F, 0.30F}, {9.0F, 1.12F, 0.40F}, {12.0F, 0.60F, 0.00F},
            {15.0F, 1.16F, 0.06F}, {17.0F, 0.92F, 0.00F}, {20.0F, 1.00F, 0.00F}
    };

    protected SiftMobRenderer(EntityRendererProvider.Context context, M model, float shadow) {
        super(context, model, shadow);
        this.addLayer(new RotLayer()); // E1: the rot outside the Sift
    }

    /** How much the body squashes and hops: 1 for small springy mobs, less for heavy ones. */
    protected float bounciness() {
        return 1.0F;
    }

    /** The face to wear this frame. */
    protected Expression expression(T entity, S state) {
        return Expression.pick(entity, entity.isAggressive(), false, false);
    }

    @Override
    public void extractRenderState(T entity, S state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.dying = state.deathTime;
        state.hurtTicks = entity.hurtTime > 0 ? 10.0F - entity.hurtTime + partialTicks : -1.0F;
        state.expression = this.expression(entity, state);
        state.rot = com.thesift.entity.SiftRot.amount(entity); // E1
    }

    /** E1: a rotting creature's colours sour towards a dull, sickly olive. */
    @Override
    protected int getModelTint(S state) {
        int tint = super.getModelTint(state);
        if (state.rot <= 0.0F) {
            return tint;
        }
        float k = state.rot * 0.6F;
        return ARGB.multiply(tint, ARGB.color(255, (int) (255 - 95 * k), (int) (255 - 80 * k), (int) (255 - 135 * k)));
    }

    /**
     * E1: the rot itself - blotches of raw red and dark green (the texture's {@code _rot} twin, see
     * {@link RotTextures}) fading in over the creature as it rots.
     */
    private final class RotLayer extends RenderLayer<S, M> {
        RotLayer() {
            super(SiftMobRenderer.this);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
            if (state.rot < 0.04F || state.isInvisible) {
                return;
            }
            Identifier tex = RotTextures.of(SiftMobRenderer.this.getTextureLocation(state));
            if (tex == null) {
                return;
            }
            int alpha = (int) (Math.min(1.0F, state.rot * 1.3F) * 255.0F);
            collector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.entityTranslucent(tex), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), ARGB.color(alpha, 255, 255, 255), null, state.outlineColor);
        }
    }

    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    @Override
    protected void scale(S state, PoseStack poseStack) {
        super.scale(state, poseStack);
        squash(state, poseStack, this.bounciness());
    }

    /** Applies the hurt squish or the death pop to the pose (model space, y pointing down). */
    public static void squash(SiftRenderState s, PoseStack poseStack, float k) {
        float sy = 1.0F;
        float hop = 0.0F;
        float tilt = 0.0F;
        float inflate = 1.0F;
        if (s.dying > 0.0F) {
            float t = Math.min(20.0F, s.dying);
            int i = 0;
            while (i < DEATH.length - 2 && t > DEATH[i + 1][0]) {
                i++;
            }
            float[] a = DEATH[i];
            float[] b = DEATH[i + 1];
            float f = smooth((t - a[0]) / (b[0] - a[0]));
            sy = Mth.lerp(f, a[1], b[1]);
            hop = Mth.lerp(f, a[2], b[2]) * k;
            tilt = Mth.sin(t * 1.1F) * 9.0F * (1.0F - t / 20.0F) * k;
            // the last moment: it swells up before it pops
            inflate = 1.0F + 0.32F * smooth((t - 17.0F) / 3.0F);
        } else if (s.hurtTicks >= 0.0F) {
            float h = Math.min(1.0F, s.hurtTicks / 10.0F);
            sy = 1.0F - 0.2F * Mth.sin(h * Mth.TWO_PI) * (1.0F - h);
        }
        sy = 1.0F + (sy - 1.0F) * k;
        if (sy == 1.0F && hop == 0.0F && tilt == 0.0F && inflate == 1.0F) {
            return;
        }
        float wide = (float) (1.0 / Math.sqrt(sy)) * inflate;
        if (hop != 0.0F) {
            poseStack.translate(0.0F, -hop, 0.0F);
        }
        if (tilt != 0.0F) {
            poseStack.rotateDegrees(Axis.ZP, tilt);
        }
        poseStack.scale(wide, sy * inflate, wide);
    }

    private static float smooth(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }
}
