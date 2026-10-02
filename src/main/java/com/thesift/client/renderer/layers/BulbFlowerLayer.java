package com.thesift.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.client.model.BulbModel;
import com.thesift.client.renderer.state.BulbRenderState;
import com.thesift.entity.Bulb;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelRenderState;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/**
 * The flowers on a Bulb's back, drawn the way vanilla draws a Mooshroom's mushrooms - each one the
 * flower's own block model, turned a different way - but riding on the jelly: they squash with it,
 * nod after every hop (a spring) and pop in with a little overshoot when they land. The flower it is
 * nibbling sticks out of its mouth.
 */
public class BulbFlowerLayer extends RenderLayer<BulbRenderState, BulbModel> {
    /** Block model size on the back (in model space, before the renderer's own 0.6). */
    private static final float SCALE = 0.62F;
    private static final float HELD_SCALE = 0.45F;

    public BulbFlowerLayer(RenderLayerParent<BulbRenderState, BulbModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, BulbRenderState state, float yRot, float xRot) {
        boolean outlineOnly = state.appearsGlowing() && state.isInvisible;
        if (state.isInvisible && !outlineOnly) {
            return;
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        for (int i = 0; i < Bulb.FLOWER_SLOTS; i++) {
            BlockModelRenderState flower = state.flowers[i];
            if (flower.isEmpty()) {
                continue;
            }
            float[] spot = Bulb.FLOWER_SPOTS[i];
            float k = SCALE * state.flowerPop[i];
            float nod = state.flowerSway * (0.8F + 0.25F * i);
            poseStack.pushPose();
            this.getParentModel().body().translateAndRotate(poseStack);
            poseStack.translate(spot[0] / 16.0F, -9.0F / 16.0F, spot[1] / 16.0F);
            poseStack.rotateDegrees(Axis.XP, nod * 22.0F);
            poseStack.rotateDegrees(Axis.ZP, nod * (i == 1 ? -14.0F : 14.0F));
            poseStack.rotateDegrees(Axis.YP, spot[2]);
            poseStack.scale(-k, -k, k);
            // stand the block model on its base, sunk a little into the jelly
            poseStack.translate(-0.5F, -0.08F, -0.5F);
            submitFlower(poseStack, collector, light, outlineOnly, state.outlineColor, flower, overlay);
            poseStack.popPose();
        }
        if (!state.held.isEmpty()) {
            // held by the stem in its mouth, the bloom sticking out to one side
            poseStack.pushPose();
            this.getParentModel().body().translateAndRotate(poseStack);
            poseStack.translate(0.0F, -3.4F / 16.0F, -5.4F / 16.0F);
            poseStack.rotateDegrees(Axis.YP, 90.0F);
            poseStack.rotateDegrees(Axis.ZP, -70.0F + Mth.sin(state.ageInTicks * 1.6F) * 8.0F);
            poseStack.scale(-HELD_SCALE, -HELD_SCALE, HELD_SCALE);
            poseStack.translate(-0.5F, -0.1F, -0.5F);
            submitFlower(poseStack, collector, light, outlineOnly, state.outlineColor, state.held, overlay);
            poseStack.popPose();
        }
    }

    private static void submitFlower(PoseStack poseStack, SubmitNodeCollector collector, int light, boolean outlineOnly, int outlineColor,
            BlockModelRenderState flower, int overlay) {
        if (outlineOnly) {
            flower.submitOnlyOutline(poseStack, collector, light, overlay, outlineColor);
        } else {
            flower.submit(poseStack, collector, light, overlay, outlineColor);
        }
    }
}
