package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.TheSift;
import com.thesift.block.entity.EurophyRecipes;
import com.thesift.block.entity.EurophyTableBlockEntity;
import com.thesift.client.model.EurophyTableModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
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
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Draws the Europhy Table's clockwork (ring gear, pinions, the crown of arms, the floating Prism lens), the
 * ingredients riding on the arms' pans, the finished output resting on the hub, and - while it forms - the
 * output growing and spinning in the centre under the lens.
 */
public class EurophyTableRenderer implements BlockEntityRenderer<EurophyTableBlockEntity, EurophyTableRenderer.State> {
    public static final ModelLayerLocation LAYER = new ModelLayerLocation(TheSift.id("europhy_table"), "main");
    private static final Identifier TEXTURE = TheSift.id("textures/entity/europhy_table/europhy_table.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/europhy_table/europhy_table_glow.png");

    private final ItemModelResolver itemModelResolver;
    private final EurophyTableModel model;

    public EurophyTableRenderer(BlockEntityRendererProvider.Context context) {
        this.itemModelResolver = context.itemModelResolver();
        this.model = new EurophyTableModel(context.bakeLayer(LAYER));
    }

    public static class State extends BlockEntityRenderState {
        public float time;
        public float spin;
        public float crown;
        public float lift;
        public float pulse;
        public boolean forming;
        public float form;
        public final ItemStackRenderState[] inputs = {new ItemStackRenderState(), new ItemStackRenderState(), new ItemStackRenderState(),
                new ItemStackRenderState()};
        public final Vec3[] pans = new Vec3[EurophyTableBlockEntity.INPUTS];
        public final ItemStackRenderState output = new ItemStackRenderState();
        public final ItemStackRenderState forms = new ItemStackRenderState();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EurophyTableBlockEntity table, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(table, state, partialTicks, cameraPosition, breakProgress);
        state.time = table.age + partialTicks;
        state.spin = Mth.lerp(partialTicks, table.spinO, table.spin);
        state.crown = Mth.lerp(partialTicks, table.crownO, table.crown);
        state.lift = Mth.lerp(partialTicks, table.liftO, table.lift);
        state.pulse = table.pulse;
        state.forming = table.isForming();
        state.form = table.formProgress(partialTicks);
        BlockPos pos = table.getBlockPos();
        int seed = (int) pos.asLong();
        for (int k = 0; k < EurophyTableBlockEntity.INPUTS; k++) {
            this.itemModelResolver.updateForTopItem(state.inputs[k], table.getItems().getItem(k), ItemDisplayContext.GROUND, table.getLevel(), null,
                    seed + k);
            state.pans[k] = table.panPos(k, partialTicks).subtract(pos.getX(), pos.getY(), pos.getZ());
        }
        this.itemModelResolver.updateForTopItem(state.output, table.getItems().getItem(EurophyTableBlockEntity.OUTPUT), ItemDisplayContext.GROUND,
                table.getLevel(), null, seed + 7);
        ItemStack forming = ItemStack.EMPTY;
        if (state.forming) {
            EurophyRecipes.Match m = table.match();
            if (m != null) {
                forming = m.preview(table.inputs());
            }
        }
        this.itemModelResolver.updateForTopItem(state.forms, forming, ItemDisplayContext.GROUND, table.getLevel(), null, seed + 9);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        // the clockwork, drawn like a mob model standing on the block's floor
        poseStack.pushPose();
        poseStack.translate(0.5F, 1.5F, 0.5F);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        this.model.setupAnim(state);
        collector.submitModel(this.model, state, poseStack, TEXTURE, state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        collector.submitModel(this.model, state, poseStack, RenderTypes.eyes(GLOW), state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
        int lit = LightCoordsUtil.withBlock(state.lightCoords, 15);
        // the ingredients on the pans, turning slowly
        for (int k = 0; k < EurophyTableBlockEntity.INPUTS; k++) {
            if (state.inputs[k].isEmpty() || state.pans[k] == null) {
                continue;
            }
            Vec3 p = state.pans[k];
            poseStack.pushPose();
            poseStack.translate(p.x, p.y + Mth.sin(state.time * 0.1F + k) * 0.015F, p.z);
            poseStack.rotate(Axis.YP, state.time * 0.04F + k * 1.3F);
            poseStack.scale(0.55F, 0.55F, 0.55F);
            state.inputs[k].submit(poseStack, collector, state.forming ? lit : state.lightCoords, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (state.forming && !state.forms.isEmpty()) {
            // the output forms in the centre: it grows from a speck, spinning faster and faster, lit by the lens
            float f = state.form;
            float scale = 0.1F + 0.8F * f * f * (3.0F - 2.0F * f);
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.98F + Mth.sin(state.time * 0.3F) * 0.02F, 0.5F);
            poseStack.rotate(Axis.YP, state.time * (0.05F + f * 0.4F));
            poseStack.scale(scale, scale, scale);
            state.forms.submit(poseStack, collector, lit, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        } else if (!state.output.isEmpty()) {
            // what it made waits on the hub
            poseStack.pushPose();
            poseStack.translate(0.5F, 0.98F + Mth.sin(state.time * 0.08F) * 0.03F, 0.5F);
            poseStack.rotate(Axis.YP, state.time * 0.03F);
            poseStack.scale(0.6F, 0.6F, 0.6F);
            state.output.submit(poseStack, collector, lit, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
    }

    /** The lens floats above the block: draw while any of the two blocks it reaches into is in view (NeoForge hook). */
    public AABB getRenderBoundingBox(EurophyTableBlockEntity table) {
        BlockPos p = table.getBlockPos();
        return new AABB(p.getX(), p.getY(), p.getZ(), p.getX() + 1, p.getY() + 2, p.getZ() + 1);
    }
}
