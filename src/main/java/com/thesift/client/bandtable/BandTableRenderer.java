package com.thesift.client.bandtable;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.TheSift;
import com.thesift.block.BandTableBlock;
import com.thesift.block.entity.BandTableBlockEntity;
import com.thesift.enchant.SiftEnchant;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * F2 Band Table: draws the Music Band Table's moving parts ({@link BandTableModel}) over its block model, the item
 * being enchanted turning above the songbook, and the glowing notes of the song being played.
 */
public class BandTableRenderer implements BlockEntityRenderer<BandTableBlockEntity, BandTableRenderer.State> {
    public static final Identifier TEXTURE = TheSift.id("textures/entity/band_table/band_table.png");
    public static final Identifier GLOW = TheSift.id("textures/entity/band_table/band_table_glow.png");
    private static final int[] NONE = new int[0];
    private final ItemModelResolver itemModelResolver;
    private final BandTableModel model;

    public BandTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.model = new BandTableModel(context.bakeLayer(BandTableClient.LAYER));
    }

    /** What one frame of the table looks like. */
    public static class State extends BlockEntityRenderState {
        public final ItemStackRenderState item = new ItemStackRenderState();
        public Direction facing = Direction.NORTH;
        public float time;
        public float open;
        public float flip;
        public float swing;
        public boolean performing;
        /** The song's notes while one is played (else empty). */
        public int[] notes = NONE;
        public int progress;
        /** 1 the moment a note lands (or misses), fading to 0 over half a second. */
        public float hit;
        public float miss;
        /** 1 as a performance ends, fading to 0 over two seconds. */
        public float done;
        public int doneLevel;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(BandTableBlockEntity table, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = table.getBlockState();
        state.facing = bs.hasProperty(BandTableBlock.FACING) ? bs.getValue(BandTableBlock.FACING) : Direction.NORTH;
        state.time = table.age + partialTicks;
        state.open = Mth.lerp(partialTicks, table.openO, table.open);
        state.flip = Mth.lerp(partialTicks, table.flipO, table.flip);
        state.swing = Mth.lerp(partialTicks, table.swingO, table.swing);
        SiftEnchant e = table.performing();
        state.performing = e != null;
        long now = table.getLevel() == null ? 0L : table.getLevel().getGameTime();
        float clock = now + partialTicks;
        state.progress = table.progress();
        state.hit = Mth.clamp(1.0F - (clock - table.hitAt()) / 10.0F, 0.0F, 1.0F);
        state.miss = Mth.clamp(1.0F - (clock - table.missAt()) / 12.0F, 0.0F, 1.0F);
        state.done = Mth.clamp(1.0F - (clock - table.doneAt()) / 40.0F, 0.0F, 1.0F);
        state.doneLevel = table.doneLevel();
        SiftEnchant last = table.lastPlayed();
        if (e != null) {
            state.notes = e.song().notes();
        } else if (state.done > 0.0F && last != null) {
            // the song just ended: its notes rise and fade
            state.notes = last.song().notes();
            state.progress = state.notes.length;
        } else {
            state.notes = NONE;
        }
        this.itemModelResolver.updateForTopItem(state.item, table.getItem(), ItemDisplayContext.GROUND, table.getLevel(), null,
                (int) table.getBlockPos().asLong());
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5F, 0.0F, 0.5F);
        float turn = switch (state.facing) {
            case EAST -> 90.0F;
            case SOUTH -> 180.0F;
            case WEST -> 270.0F;
            default -> 0.0F;
        };
        poseStack.rotateDegrees(Axis.YP, -turn);

        // the item being enchanted, turning over the songbook (it rises and glows while the song is played)
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            float lift = state.performing ? 0.12F + state.progress * 0.02F : 0.0F;
            poseStack.translate(0.0F, 1.2F + lift + Mth.sin(state.time * 0.08F) * 0.04F, -0.06F);
            poseStack.rotate(Axis.YP, state.time * (state.performing ? 0.09F : 0.03F));
            float scale = 0.85F + state.hit * 0.12F;
            poseStack.scale(scale, scale, scale);
            int light = state.performing || state.done > 0.0F ? LightCoordsUtil.withBlock(state.lightCoords, 15) : state.lightCoords;
            state.item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }

        // the moving parts, then the glowing notes
        poseStack.translate(0.0F, 1.5F, 0.0F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        collector.submitModel(this.model, new BandTableModel.Pass(state, false), poseStack, RenderTypes.entityCutout(TEXTURE), state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0);
        collector.order(1).submitModel(this.model, new BandTableModel.Pass(state, true), poseStack, RenderTypes.eyes(GLOW), state.lightCoords,
                OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(BandTableBlockEntity table) {
        BlockPos p = table.getBlockPos();
        return new AABB(p.getX() - 0.25, p.getY(), p.getZ() - 0.25, p.getX() + 1.25, p.getY() + 2.0, p.getZ() + 1.25);
    }
}
