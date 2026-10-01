package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.block.entity.InstrumentAltarBlockEntity;
import com.thesift.client.renderer.state.InstrumentAltarRenderState;
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

/** Floats the instrument over its altar, turning slowly; while the stage plays it bobs to the beat. */
public class InstrumentAltarRenderer implements BlockEntityRenderer<InstrumentAltarBlockEntity, InstrumentAltarRenderState> {
    private final ItemModelResolver itemModelResolver;

    public InstrumentAltarRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    @Override
    public InstrumentAltarRenderState createRenderState() {
        return new InstrumentAltarRenderState();
    }

    @Override
    public void extractRenderState(InstrumentAltarBlockEntity altar, InstrumentAltarRenderState state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(altar, state, partialTicks, cameraPosition, breakProgress);
        state.time = altar.age + partialTicks;
        state.playing = altar.isLocked();
        this.itemModelResolver.updateForTopItem(state.item, altar.getItem(), ItemDisplayContext.GROUND, altar.getLevel(), null,
                (int) altar.getBlockPos().asLong());
    }

    @Override
    public void submit(InstrumentAltarRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        float bob = state.playing ? Math.abs(Mth.sin(state.time * Mth.PI / 10.0F)) * 0.12F : Mth.sin(state.time * 0.07F) * 0.05F;
        poseStack.translate(0.5F, 1.25F + bob, 0.5F);
        poseStack.rotate(Axis.YP, state.time * (state.playing ? 0.06F : 0.025F));
        float scale = 1.3F + (state.playing ? Math.abs(Mth.sin(state.time * Mth.PI / 10.0F)) * 0.08F : 0.0F);
        poseStack.scale(scale, scale, scale);
        state.item.submit(poseStack, collector, LightCoordsUtil.withBlock(state.lightCoords, 15), OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
