package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.block.entity.EuphoryAltarBlockEntity;
import com.thesift.client.renderer.state.EuphoryAltarRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** Floats the offered item above the altar; it spins faster and rises as the ritual builds. */
public class EuphoryAltarRenderer implements BlockEntityRenderer<EuphoryAltarBlockEntity, EuphoryAltarRenderState> {
    private final ItemModelResolver itemModelResolver;

    public EuphoryAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public EuphoryAltarRenderState createRenderState() {
        return new EuphoryAltarRenderState();
    }

    @Override
    public void extractRenderState(EuphoryAltarBlockEntity altar, EuphoryAltarRenderState state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTicks, cameraPosition, breakProgress);
        state.spin = Mth.lerp(partialTicks, altar.spinO, altar.spin);
        state.time = altar.age + partialTicks;
        state.ritual = altar.isRitualActive();
        state.progress = altar.ritualProgress();
        this.itemModelResolver.updateForTopItem(state.item, altar.getItem(), ItemDisplayContext.GROUND, altar.getLevel(), null,
                (int) altar.getBlockPos().asLong());
    }

    @Override
    public void submit(EuphoryAltarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        float lift = state.ritual ? state.progress * 0.45F : 0.0F;
        poseStack.translate(0.5F, 1.15F + lift + Mth.sin(state.time * 0.08F) * 0.05F, 0.5F);
        poseStack.rotate(Axis.YP, state.spin);
        float scale = 0.9F + (state.ritual ? Mth.sin(state.time * 0.6F) * 0.05F : 0.0F);
        poseStack.scale(scale, scale, scale);
        int light = state.ritual ? LightCoordsUtil.withBlock(state.lightCoords, 15) : state.lightCoords;
        state.item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
