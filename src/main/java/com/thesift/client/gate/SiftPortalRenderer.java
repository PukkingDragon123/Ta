package com.thesift.client.gate;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thesift.TheSift;
import com.thesift.block.SiftPortalBlock;
import com.thesift.block.entity.SiftPortalBlockEntity;
import com.thesift.portal.GateAwakening;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * B1 Portal & sky FX: each portal cell drawn as a window onto the Sift's sky, with real depth.
 *
 * <p>Nothing is built behind the frame. Instead every layer is a quad on the portal's surface whose
 * texture coordinates are where the ray from the eye through that point would meet a plane
 * {@code depth} blocks behind it. The eye's distance to the portal's plane is the same for every
 * point on it, so that mapping is exactly affine and the GPU's ordinary interpolation gets it right:
 * as you move, near layers slide and far ones barely move, just like looking through a window.</p>
 *
 * <ul>
 *   <li>the sky itself - turquoise above, a rose-and-mint horizon, violet below - at infinite
 *   depth, so it only follows the direction you look in (opaque, unlit);</li>
 *   <li>two star fields and two drifting cloud layers at decreasing depth, and caustic ripples on
 *   the surface (all added on top as light).</li>
 * </ul>
 *
 * <p>The layers sit just in front of the block model's face on the viewer's side, so the old
 * animated membrane is only seen from far away (or for portals made before this block entity).
 * While a gate awakens the cells appear from its heart outwards on the flash, white-hot first.</p>
 */
public class SiftPortalRenderer implements BlockEntityRenderer<SiftPortalBlockEntity, SiftPortalRenderer.State> {
    private static final Identifier SKY = TheSift.id("textures/entity/sift_gate/sky.png");
    private static final Identifier STARS = TheSift.id("textures/entity/sift_gate/stars.png");
    private static final Identifier CLOUDS = TheSift.id("textures/entity/sift_gate/clouds.png");
    private static final Identifier RIPPLE = TheSift.id("textures/entity/sift_gate/ripple.png");
    private static final int FULL_BRIGHT = 0xF000F0;
    /** The block model's faces are 2/16 either side of the middle; draw just in front of them. */
    private static final float FACE = 0.125F + 0.004F;

    private static @Nullable RenderType skyType;
    private static @Nullable RenderType starsType;
    private static @Nullable RenderType cloudsType;
    private static @Nullable RenderType rippleType;

    public SiftPortalRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public Direction.Axis axis = Direction.Axis.X;
        public float time;
        /** 0 = not there yet, 1 = fully open. */
        public float appear = 1.0F;
        /** White heat on the surface just after it appears, and while the flash lasts. */
        public float heat;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SiftPortalBlockEntity portal, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(portal, state, partialTicks, cameraPosition, breakProgress);
        BlockState bs = portal.getBlockState();
        state.axis = bs.hasProperty(SiftPortalBlock.AXIS) ? bs.getValue(SiftPortalBlock.AXIS) : Direction.Axis.X;
        long now = portal.getLevel() != null ? portal.getLevel().getGameTime() : 0L;
        state.time = (now % 240000L) + partialTicks;
        state.appear = 1.0F;
        state.heat = 0.0F;
        Minecraft mc = Minecraft.getInstance();
        Vec3 gate = mc.level != null ? GateAwakeningFx.liveGate(mc.level) : null;
        if (gate != null) {
            // the surface appears from the gate's heart outwards, riding the shockwave
            BlockPos p = portal.getBlockPos();
            double d = Math.sqrt(gate.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
            if (d < GateAwakening.clientSpan + 4.0) {
                float since = GateAwakening.clientTick + partialTicks - GateAwakening.CLIMAX - (float) d * 0.9F;
                state.appear = Mth.clamp(since / 5.0F, 0.0F, 1.0F);
                state.heat = Mth.clamp(1.0F - since / 14.0F, 0.0F, 1.0F) * state.appear;
            }
        }
        state.heat = Math.max(state.heat, GateAwakeningFx.flash(partialTicks) * 0.6F);
    }

    @Override
    public int getViewDistance() {
        return 96;
    }

    private static void types() {
        if (skyType == null) {
            skyType = RenderTypes.beaconBeam(SKY, false);
            starsType = RenderTypes.energySwirl(STARS, 0.0F, 0.0F);
            cloudsType = RenderTypes.energySwirl(CLOUDS, 0.0F, 0.0F);
            rippleType = RenderTypes.energySwirl(RIPPLE, 0.0F, 0.0F);
        }
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        if (state.appear <= 0.0F) {
            return;
        }
        types();
        boolean xAxis = state.axis == Direction.Axis.X;
        BlockPos pos = state.blockPos;
        // the eye, in this block's own coordinates
        double ex = camera.pos.x - pos.getX();
        double ey = camera.pos.y - pos.getY();
        double ez = camera.pos.z - pos.getZ();
        // u runs along the portal, n is across it
        float eyeU = (float) (xAxis ? ex : ez);
        float eyeY = (float) ey;
        float eyeN = (float) (xAxis ? ez : ex) - 0.5F;
        float side = eyeN >= 0.0F ? 1.0F : -1.0F;
        float dn = Math.max(0.3F, Math.abs(eyeN));
        // world position of this cell (wrapped so the numbers stay small; every texture tiles)
        int cellU = Math.floorMod(xAxis ? pos.getX() : pos.getZ(), 240);
        int cellY = Math.floorMod(pos.getY(), 240);
        float t = state.time;
        float a = state.appear;
        Layer base = new Layer(Float.POSITIVE_INFINITY, 1.0F, 0.0F, 0.0F);
        // the sky: infinitely far, so it only depends on the direction through each corner
        submitLayer(poseStack, collector, skyType, state, xAxis, side, 0.0F, eyeU, eyeY, dn, cellU, cellY, base,
                argb(1.0F, a, a, a), true);
        submitLayer(poseStack, collector, starsType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                new Layer(60.0F, 7.0F, t * 0.00012F, 0.0F), argb(1.0F, 0.95F * a, 0.95F * a, 0.95F * a), false);
        submitLayer(poseStack, collector, starsType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                new Layer(26.0F, 4.3F, 0.37F - t * 0.0002F, 0.21F),
                argb(1.0F, 0.55F * a, 0.75F * a, 0.8F * a), false);
        submitLayer(poseStack, collector, cloudsType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                new Layer(11.0F, 7.0F, t * 0.0009F, 0.13F), argb(1.0F, 0.42F * a, 0.26F * a, 0.46F * a), false);
        submitLayer(poseStack, collector, cloudsType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                new Layer(3.5F, 4.5F, 0.5F - t * 0.0017F, t * 0.0004F), argb(1.0F, 0.12F * a, 0.4F * a, 0.42F * a), false);
        float shimmer = 0.22F + 0.06F * Mth.sin(t * 0.07F);
        submitLayer(poseStack, collector, rippleType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                new Layer(0.0F, 2.5F, t * 0.0021F, -t * 0.0013F), argb(1.0F, shimmer * 0.7F, shimmer, shimmer), false);
        if (state.heat > 0.01F) {
            float h = state.heat;
            // white heat: the cloud texture blown out, flat on the surface
            submitLayer(poseStack, collector, cloudsType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                    new Layer(0.0F, 1.5F, t * 0.01F, 0.0F), argb(1.0F, h, h, h), false);
            submitLayer(poseStack, collector, rippleType, state, xAxis, side, 0.002F, eyeU, eyeY, dn, cellU, cellY,
                    new Layer(0.0F, 0.8F, 0.0F, t * 0.02F), argb(1.0F, h, h, h), false);
        }
    }

    /** One layer at {@code depth} blocks behind the surface: tile size {@code scale}, drifting by (du, dv). */
    private record Layer(float depth, float scale, float du, float dv) {}

    private static int argb(float alpha, float r, float g, float b) {
        return (int) (Mth.clamp(alpha, 0.0F, 1.0F) * 255.0F) << 24 | (int) (Mth.clamp(r, 0.0F, 1.0F) * 255.0F) << 16
                | (int) (Mth.clamp(g, 0.0F, 1.0F) * 255.0F) << 8 | (int) (Mth.clamp(b, 0.0F, 1.0F) * 255.0F);
    }

    private static void submitLayer(PoseStack poseStack, SubmitNodeCollector collector, @Nullable RenderType type, State state, boolean xAxis,
            float side, float lift, float eyeU, float eyeY, float dn, int cellU, int cellY, Layer layer, int colour, boolean bothSides) {
        if (type == null) {
            return;
        }
        // every light layer shares one plane: they are added together, so the order they are drawn in
        // never matters and the equal depth always passes the depth test
        float n = 0.5F + side * (FACE + lift);
        // the corners, in (u, y); this order faces +n for an X portal and -n for a Z portal
        float[][] corners = {{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}, {0.0F, 1.0F}};
        float[] us = new float[4];
        float[] vs = new float[4];
        for (int i = 0; i < 4; i++) {
            float cu = corners[i][0];
            float cy = corners[i][1];
            float du = cu - eyeU;
            float dy = cy - eyeY;
            if (Float.isInfinite(layer.depth())) {
                // only the direction matters: across the gradient by height, gently along it by heading
                us[i] = 0.5F + du / dn * 0.22F;
                vs[i] = Mth.clamp(0.5F - dy / dn * 0.3F, 0.01F, 0.99F);
            } else {
                float k = layer.depth() / dn;
                us[i] = (cellU + cu + du * k) / layer.scale() + layer.du();
                vs[i] = -(cellY + cy + dy * k) / layer.scale() + layer.dv();
            }
        }
        float frontSign = xAxis ? 1.0F : -1.0F;
        boolean forward = side == frontSign;
        float nx = xAxis ? 0.0F : side;
        float nz = xAxis ? side : 0.0F;
        collector.submitCustomGeometry(poseStack, type, (pose, buffer) -> {
            for (int pass = 0; pass < (bothSides ? 2 : 1); pass++) {
                boolean order = pass == 0 ? forward : !forward;
                for (int j = 0; j < 4; j++) {
                    int idx = order ? j : 3 - j;
                    float qu = corners[idx][0];
                    float qy = corners[idx][1];
                    vertex(buffer, pose, xAxis ? qu : n, qy, xAxis ? n : qu, colour, us[idx], vs[idx], nx, nz);
                }
            }
        });
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, float x, float y, float z, int colour, float u, float v, float nx, float nz) {
        buffer.addVertex(pose, x, y, z).setColor(colour).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL_BRIGHT)
                .setNormal(pose, nx, 0.0F, nz);
    }
}
