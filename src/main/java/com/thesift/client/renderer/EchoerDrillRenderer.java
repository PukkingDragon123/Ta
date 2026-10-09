package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.TheSift;
import com.thesift.block.EchoerDeviceBlock;
import com.thesift.block.entity.EchoerDeviceBlockEntity;
import com.thesift.client.model.EchoerDrillModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * RR: draws the Echoer Drill's gun on its housing (the housing is the block model): turned to the block's facing,
 * its coils, rune channel, horn throat and pink tip glowing while it listens or drills.
 */
public class EchoerDrillRenderer implements BlockEntityRenderer<EchoerDeviceBlockEntity, EchoerDrillRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TheSift.id("echoer_drill"), "main");
    private static final Identifier TEXTURE = TheSift.id("textures/entity/echoer_drill/echoer_drill.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/echoer_drill/echoer_drill_glow.png");

    private final EchoerDrillModel model;

    public EchoerDrillRenderer(BlockEntityRendererProvider.Context context) {
        this.model = new EchoerDrillModel(context.bakeLayer(LAYER));
    }

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public float time;
        public float spin;
        public float recoil;
        public float ear;
        public float dial;
        public boolean active;
        public boolean drilling;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EchoerDeviceBlockEntity drill, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(drill, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = drill.getBlockState();
        boolean ours = bs.getBlock() instanceof EchoerDeviceBlock;
        state.facing = ours ? bs.getValue(EchoerDeviceBlock.FACING) : Direction.NORTH;
        state.active = ours && bs.getValue(EchoerDeviceBlock.CHARGING);
        state.drilling = drill.drilling;
        state.time = drill.age + partialTicks;
        state.spin = Mth.lerp(partialTicks, drill.spinO, drill.spin);
        state.recoil = Mth.lerp(partialTicks, drill.recoilO, drill.recoil);
        state.ear = Mth.lerp(partialTicks, drill.earO, drill.ear);
        state.dial = Mth.lerp(partialTicks, drill.dialO, drill.dial);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.5F, 0.5F);
        // the model fires towards -Z (north); turn it like the blockstate turns the housing
        switch (state.facing) {
            case SOUTH -> poseStack.rotateDegrees(Axis.YP, 180.0F);
            case EAST -> poseStack.rotateDegrees(Axis.YP, -90.0F);
            case WEST -> poseStack.rotateDegrees(Axis.YP, 90.0F);
            case UP -> poseStack.rotateDegrees(Axis.XP, 90.0F);
            case DOWN -> poseStack.rotateDegrees(Axis.XP, -90.0F);
            default -> {
            }
        }
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.setupAnim(state);
        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        if (state.active) {
            collector.submitModel(this.model, state, poseStack, RenderTypes.eyes(GLOW), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        }
        poseStack.popPose();
    }

    /** The bit and the horn reach out of the block: draw while any block round it is in view (NeoForge hook). */
    @Override
    public AABB getRenderBoundingBox(EchoerDeviceBlockEntity drill) {
        BlockPos p = drill.getBlockPos();
        return new AABB(p).inflate(1.0);
    }
}
