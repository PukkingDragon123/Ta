package com.thesift.world.sky;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thesift.TheSift;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: the Sky Vine a player swings on, drawn from where it hangs to the player's hands as two crossed strips of
 * liana (the texture of vanilla's twisting vines, re-themed by tools/sky_art.py). Taut, it runs straight; slack (when
 * the player swings up past the pivot) it sags.
 */
public class SkyRopeRenderer extends EntityRenderer<SkyRope, SkyRopeRenderer.State> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/sky_rope.png");
    private static final float HALF_WIDTH = 0.22F;

    public static class State extends EntityRenderState {
        /** The hands, relative to the rope's pivot (null when nobody holds it). */
        public @Nullable Vec3 to;
        public float slack;
    }

    public SkyRopeRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static Vec3 hands(Player player, float partialTicks) {
        return player.getPosition(partialTicks).add(0.0, player.getBbHeight() + 0.05, 0.0);
    }

    @Override
    public void extractRenderState(SkyRope rope, State state, float partialTicks) {
        super.extractRenderState(rope, state, partialTicks);
        Player holder = rope.holder();
        if (holder == null) {
            state.to = null;
            return;
        }
        Vec3 to = hands(holder, partialTicks).subtract(rope.getPosition(partialTicks));
        state.to = to;
        // a slack rope (the hands above the line a hanging rope would follow) sags a little
        state.slack = (float) Mth.clamp((to.y + to.length() * 0.85) * 0.25, 0.0, 0.8);
    }

    @Override
    protected AABB getBoundingBoxForCulling(SkyRope rope, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(rope, partialTicks);
        Player holder = rope.holder();
        return holder == null ? box : box.minmax(holder.getBoundingBox().inflate(0.5));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        Vec3 to = state.to;
        if (to == null || to.lengthSqr() < 0.01) {
            return;
        }
        int light = state.lightCoords;
        float slack = state.slack;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(TEXTURE), (pose, buffer) -> {
            double len = to.length();
            Vec3 dir = to.scale(1.0 / len);
            Vec3 side = Math.abs(dir.y) > 0.98 ? new Vec3(1.0, 0.0, 0.0) : dir.cross(new Vec3(0.0, 1.0, 0.0)).normalize();
            Vec3 other = dir.cross(side).normalize();
            int segments = Math.max(2, Mth.ceil(len));
            for (int i = 0; i < segments; i++) {
                float t0 = (float) i / segments;
                float t1 = (float) (i + 1) / segments;
                Vec3 a = to.scale(t0).add(0.0, -slack * 4.0F * t0 * (1.0F - t0), 0.0);
                Vec3 b = to.scale(t1).add(0.0, -slack * 4.0F * t1 * (1.0F - t1), 0.0);
                ribbon(buffer, pose, a, b, side, light);
                ribbon(buffer, pose, a, b, other, light);
            }
        });
    }

    /** One strip of liana from a to b, facing both ways (one texture repeat per block of rope). */
    private static void ribbon(VertexConsumer buffer, PoseStack.Pose pose, Vec3 a, Vec3 b, Vec3 across, int light) {
        Vec3 w = across.scale(HALF_WIDTH);
        Vec3[] p = {a.subtract(w), a.add(w), b.add(w), b.subtract(w)};
        float[][] uv = {{0.0F, 0.0F}, {1.0F, 0.0F}, {1.0F, 1.0F}, {0.0F, 1.0F}};
        Vec3 n = b.subtract(a).cross(across);
        n = n.lengthSqr() > 1.0E-8 ? n.normalize() : new Vec3(0.0, 1.0, 0.0);
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                int j = pass == 0 ? k : 3 - k;
                float nx = (float) (pass == 0 ? n.x : -n.x);
                float ny = (float) (pass == 0 ? n.y : -n.y);
                float nz = (float) (pass == 0 ? n.z : -n.z);
                buffer.addVertex(pose, (float) p[j].x, (float) p[j].y, (float) p[j].z).setColor(-1).setUv(uv[j][0], uv[j][1])
                        .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, nx, ny, nz);
            }
        }
    }
}
