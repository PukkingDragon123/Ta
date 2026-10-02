package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.block.MusicCrystalBlock;
import com.thesift.block.entity.MusicCrystalBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/** The treasure inside a frozen music crystal: held in the middle of the clear crystal, turning slowly and bobbing, lit by the crystal's glow. */
public class MusicCrystalRenderer implements BlockEntityRenderer<MusicCrystalBlockEntity, MusicCrystalRenderer.State> {
    private final ItemModelResolver itemModelResolver;

    public MusicCrystalRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
    }

    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public float time;
        public Direction facing = Direction.UP;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MusicCrystalBlockEntity crystal, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(crystal, state, partialTicks, cameraPosition, breakProgress);
        state.time = (crystal.getLevel() != null ? crystal.getLevel().getGameTime() % 24000L : 0L) + partialTicks;
        BlockState bs = crystal.getBlockState();
        state.facing = bs.hasProperty(MusicCrystalBlock.FACING) ? bs.getValue(MusicCrystalBlock.FACING) : Direction.UP;
        this.itemModelResolver.updateForTopItem(state.item, crystal.getItem(), ItemDisplayContext.GROUND, crystal.getLevel(), null,
                (int) crystal.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.item.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // the middle of the crystal body, which sits a little towards the face it grows from
        float in = -1.0F / 16.0F;
        poseStack.translate(0.5F + state.facing.getStepX() * in, 0.5F + state.facing.getStepY() * in - 0.12F, 0.5F + state.facing.getStepZ() * in);
        poseStack.translate(0.0F, Mth.sin(state.time * 0.05F) * 0.025F, 0.0F);
        poseStack.rotate(Axis.YP, state.time * 0.03F);
        poseStack.scale(1.2F, 1.2F, 1.2F);
        state.item.submit(poseStack, collector, LightCoordsUtil.withBlock(state.lightCoords, 15), OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }
}
