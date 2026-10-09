package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thesift.TheSift;
import com.thesift.block.entity.SculkGrasperBlockEntity;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-deep caves: the Sculk Grasper's moving parts. At rest, three short feelers sway and taste the air over the maw; when
 * it strikes, a thick ribbed tendril whips out to its prey (tapering, writhing, with a bony two-talon claw on the end),
 * stays wrapped round them while it reels them in, and snaps back when it lets go.
 */
public class SculkGrasperRenderer implements BlockEntityRenderer<SculkGrasperBlockEntity, SculkGrasperRenderer.State> {
    private static final Identifier SKIN = TheSift.id("textures/entity/sculk_grasper/tendril.png");
    private static final Identifier CLAW = TheSift.id("textures/entity/sculk_grasper/claw.png");
    private static final int SEGMENTS = 14;

    public SculkGrasperRenderer(BlockEntityRendererProvider.Context context) {
    }

    public static class State extends BlockEntityRenderState {
        public float time;
        public float seed;
        /** How far the tendril reaches out (0 = only the feelers), and where to (relative to the block's corner). */
        public float reach;
        public @Nullable Vec3 tip;
        public boolean holding;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SculkGrasperBlockEntity grasper, State state, float partialTicks, Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(grasper, state, partialTicks, cameraPosition, breakProgress);
        long now = grasper.getLevel() != null ? grasper.getLevel().getGameTime() : 0L;
        state.time = (now % 24000L) + partialTicks;
        state.seed = (grasper.getBlockPos().asLong() & 255L) * 0.37F;
        float age = (now - grasper.phaseStart()) + partialTicks;
        Entity prey = grasper.target();
        Vec3 corner = Vec3.atLowerCornerOf(grasper.getBlockPos());
        Vec3 at = prey != null ? prey.getPosition(partialTicks).add(0.0, prey.getBbHeight() * 0.45, 0.0) : grasper.lastTarget();
        state.tip = at != null ? at.subtract(corner) : null;
        state.holding = false;
        switch (grasper.phase()) {
            case SculkGrasperBlockEntity.LUNGE -> state.reach = Mth.clamp(age / SculkGrasperBlockEntity.LUNGE_TICKS, 0.0F, 1.0F);
            case SculkGrasperBlockEntity.HOLD -> {
                state.reach = 1.0F;
                state.holding = true;
            }
            case SculkGrasperBlockEntity.RECOIL -> state.reach = 1.0F - Mth.clamp(age / SculkGrasperBlockEntity.RECOIL_TICKS, 0.0F, 1.0F);
            default -> state.reach = 0.0F;
        }
        if (state.tip == null) {
            state.reach = 0.0F;
        }
    }

    @Override
    public AABB getRenderBoundingBox(SculkGrasperBlockEntity grasper) {
        return new AABB(grasper.getBlockPos()).inflate(SculkGrasperBlockEntity.RANGE + 3.0);
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        int light = LightCoordsUtil.withBlock(state.lightCoords, 11);
        Vec3 mouth = new Vec3(0.5, 0.5, 0.5);
        float t = state.time;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(SKIN), (pose, buffer) -> {
            // the feelers, always tasting the air (they shrink back while the big tendril is out)
            float feel = 1.0F - state.reach * 0.7F;
            for (int i = 0; i < 3; i++) {
                float a = state.seed + i * 2.094F;
                Vec3 base = mouth.add(Mth.cos(a) * 0.18, 0.05, Mth.sin(a) * 0.18);
                Vec3[] pts = new Vec3[6];
                for (int k = 0; k < pts.length; k++) {
                    float f = k / (float) (pts.length - 1);
                    float sway = Mth.sin(t * 0.09F + a * 3.0F + f * 2.2F) * 0.16F * f;
                    float sway2 = Mth.cos(t * 0.07F + a * 5.0F + f * 1.7F) * 0.16F * f;
                    pts[k] = base.add(Mth.cos(a) * 0.12 * f + sway, (0.25 + 0.3 * f) * f * feel * 1.6, Mth.sin(a) * 0.12 * f + sway2);
                }
                tube(buffer, pose, pts, 0.045F * feel + 0.01F, 0.012F, light);
            }
            if (state.reach <= 0.0F || state.tip == null) {
                return;
            }
            Vec3 end = mouth.add(state.tip.subtract(mouth).scale(state.reach));
            Vec3 d = end.subtract(mouth);
            double len = d.length();
            if (len < 0.05) {
                return;
            }
            Vec3 side = Math.abs(d.y) < 0.95 * len ? d.cross(new Vec3(0.0, 1.0, 0.0)).normalize() : new Vec3(1.0, 0.0, 0.0);
            Vec3 up = side.cross(d).normalize();
            Vec3[] spine = new Vec3[SEGMENTS + 1];
            for (int j = 0; j <= SEGMENTS; j++) {
                float g = j / (float) SEGMENTS;
                float env = Mth.sin(g * Mth.PI);
                // it arcs up out of the maw and writhes along its length; held taut it only trembles
                float writhe = (state.holding ? 0.05F : 0.22F) * env;
                double w1 = Mth.sin(t * 0.45F + g * 9.0F) * writhe;
                double w2 = Mth.cos(t * 0.38F + g * 7.0F) * writhe;
                double arc = env * Math.min(1.0, len / 4.0) * (state.holding ? 0.25 : 0.7);
                spine[j] = mouth.add(d.scale(g)).add(side.scale(w1)).add(up.scale(w2)).add(0.0, arc, 0.0);
            }
            tube(buffer, pose, spine, 0.13F, 0.05F, light);
        });
        if (state.reach > 0.2F && state.tip != null) {
            Vec3 clawAt = mouth.add(state.tip.subtract(mouth).scale(state.reach));
            Vec3 fwd = clawAt.subtract(mouth).normalize();
            Vec3 across = Math.abs(fwd.y) < 0.95 ? fwd.cross(new Vec3(0.0, 1.0, 0.0)).normalize() : new Vec3(1.0, 0.0, 0.0);
            Vec3 lift = across.cross(fwd).normalize();
            double open = state.holding ? 0.35 : 1.0;
            Vec3 spread = across.scale(open).add(lift.scale(1.0 - open)).normalize();
            collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(CLAW), (pose, buffer) -> {
                claw(buffer, pose, clawAt, fwd, spread, light);
                claw(buffer, pose, clawAt, fwd, lift, light);
            });
        }
    }

    /** A four-sided tube through the points, tapering from {@code r0} to {@code r1}; the skin texture runs along it. */
    private static void tube(VertexConsumer buffer, PoseStack.Pose pose, Vec3[] pts, float r0, float r1, int light) {
        int n = pts.length;
        for (int k = 0; k < n - 1; k++) {
            Vec3 a = pts[k];
            Vec3 b = pts[k + 1];
            Vec3 dir = b.subtract(a);
            if (dir.lengthSqr() < 1.0E-8) {
                continue;
            }
            dir = dir.normalize();
            Vec3 s = Math.abs(dir.y) < 0.95 ? dir.cross(new Vec3(0.0, 1.0, 0.0)).normalize() : dir.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
            Vec3 u = s.cross(dir).normalize();
            float ra = Mth.lerp(k / (float) (n - 1), r0, r1);
            float rb = Mth.lerp((k + 1) / (float) (n - 1), r0, r1);
            float v0 = k / (float) (n - 1);
            float v1 = (k + 1) / (float) (n - 1);
            Vec3[] around = {s, u, s.scale(-1.0), u.scale(-1.0)};
            for (int side = 0; side < 4; side++) {
                Vec3 n0 = around[side];
                Vec3 n1 = around[(side + 1) % 4];
                Vec3 normal = n0.add(n1).normalize();
                float u0 = side * 0.25F;
                float u1 = u0 + 0.25F;
                vertex(buffer, pose, a.add(n0.scale(ra)), u0, v0, normal, light);
                vertex(buffer, pose, b.add(n0.scale(rb)), u0, v1, normal, light);
                vertex(buffer, pose, b.add(n1.scale(rb)), u1, v1, normal, light);
                vertex(buffer, pose, a.add(n1.scale(ra)), u1, v0, normal, light);
            }
        }
    }

    /** One cut-out plane of the claw, 0.6 blocks long, its knuckle at the tendril's end: texture x runs along f. */
    private static void claw(VertexConsumer buffer, PoseStack.Pose pose, Vec3 at, Vec3 f, Vec3 across, int light) {
        float h = 0.3F;
        Vec3 c = at.add(f.scale(h * 0.8));
        Vec3[] p = {c.add(f.scale(-h)).add(across.scale(h)), c.add(f.scale(h)).add(across.scale(h)), c.add(f.scale(h)).add(across.scale(-h)),
                c.add(f.scale(-h)).add(across.scale(-h))};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        Vec3 n = f.cross(across).normalize();
        for (int k = 0; k < 4; k++) {
            vertex(buffer, pose, p[k], uv[k][0], uv[k][1], n, light);
        }
        // and the back face, so it shows from both sides
        for (int k = 3; k >= 0; k--) {
            vertex(buffer, pose, p[k], uv[k][0], uv[k][1], n.scale(-1.0), light);
        }
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, Vec3 p, float u, float v, Vec3 n, int light) {
        buffer.addVertex(pose, (float) p.x, (float) p.y, (float) p.z).setColor(-1).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                .setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
    }
}
