package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.thesift.TheSift;
import com.thesift.entity.CoralHook;
import com.thesift.entity.CoralOrgan;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR3 Fish &amp; Coral Organs: a Sculk Coral Organ's hooked line - two twisted strands (dark sinew and a glowing
 * sculk thread) from the organ's horn to the barb, or to the chest of whatever it has caught, trembling with
 * strain; and while it flies, the bone barb itself, as two crossed cut-out planes along its flight.
 */
public class CoralHookRenderer extends EntityRenderer<CoralHook, CoralHookRenderer.State> {
    private static final Identifier BARB = TheSift.id("textures/entity/coral_organ/hook.png");

    public static class State extends EntityRenderState {
        /** Line start (the barb or the catch's chest) and end (the organ's horn), relative to the hook. */
        public @Nullable Vec3 from;
        public @Nullable Vec3 to;
        public boolean flying;
        public boolean taut;
    }

    public CoralHookRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    private static Vec3 mouth(CoralOrgan organ, float partialTicks) {
        float yaw = Mth.rotLerp(partialTicks, organ.yBodyRotO, organ.yBodyRot);
        Vec3 f = Vec3.directionFromRotation(0.0F, yaw);
        return organ.getPosition(partialTicks).add(f.x * 0.85, 0.62, f.z * 0.85);
    }

    @Override
    public void extractRenderState(CoralHook entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        Vec3 here = entity.getPosition(partialTicks);
        CoralOrgan organ = entity.owner();
        LivingEntity caught = entity.hooked();
        state.flying = caught == null;
        state.taut = caught != null;
        if (organ == null) {
            state.from = null;
            state.to = null;
            return;
        }
        Vec3 start = caught != null ? caught.getPosition(partialTicks).add(0.0, caught.getBbHeight() * 0.55, 0.0)
                : here.add(0.0, entity.getBbHeight() * 0.5, 0.0);
        state.from = start.subtract(here);
        state.to = mouth(organ, partialTicks).subtract(here);
    }

    @Override
    protected AABB getBoundingBoxForCulling(CoralHook entity, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
        CoralOrgan organ = entity.owner();
        if (organ == null) {
            return box;
        }
        Vec3 m = mouth(organ, partialTicks);
        return box.minmax(new AABB(m, m).inflate(0.5));
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (state.from == null || state.to == null) {
            return;
        }
        Vec3 a = state.from;
        Vec3 b = state.to;
        float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        float age = state.ageInTicks;
        boolean taut = state.taut;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            float dx = (float) (b.x - a.x);
            float dy = (float) (b.y - a.y);
            float dz = (float) (b.z - a.z);
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.01F) {
                return;
            }
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;
            int segments = Math.max(8, Math.min(32, (int) (len * 2.0F)));
            for (int strand = 0; strand < 2; strand++) {
                int colour = strand == 0 ? 0xFF0E3A44 : 0xFF3FF5E6;
                float phase = strand * Mth.PI;
                for (int i = 0; i < segments; i++) {
                    float t0 = (float) i / segments;
                    float t1 = (float) (i + 1) / segments;
                    // the two strands twist round each other; a taut line trembles, a slack one sags
                    float tw0 = Mth.sin(t0 * len * 3.0F + phase) * 0.035F;
                    float tw1 = Mth.sin(t1 * len * 3.0F + phase) * 0.035F;
                    float sh0 = Mth.sin(t0 * Mth.PI) * (taut ? Mth.sin(age * 2.3F + t0 * 11.0F) * 0.05F : -0.25F * Math.min(1.0F, len / 12.0F));
                    float sh1 = Mth.sin(t1 * Mth.PI) * (taut ? Mth.sin(age * 2.3F + t1 * 11.0F) * 0.05F : -0.25F * Math.min(1.0F, len / 12.0F));
                    buffer.addVertex(pose, (float) a.x + dx * t0 + tw0, (float) a.y + dy * t0 + sh0, (float) a.z + dz * t0 - tw0).setColor(colour)
                            .setNormal(pose, nx, ny, nz).setLineWidth(width);
                    buffer.addVertex(pose, (float) a.x + dx * t1 + tw1, (float) a.y + dy * t1 + sh1, (float) a.z + dz * t1 - tw1).setColor(colour)
                            .setNormal(pose, nx, ny, nz).setLineWidth(width);
                }
            }
        });
        if (!state.flying) {
            return;
        }
        // the barb, pointing along its flight (away from the horn)
        Vec3 f = a.subtract(b).normalize();
        Vec3 side = Math.abs(f.y) < 0.9 ? f.cross(new Vec3(0.0, 1.0, 0.0)).normalize() : f.cross(new Vec3(1.0, 0.0, 0.0)).normalize();
        Vec3 up = side.cross(f).normalize();
        int light = state.lightCoords;
        collector.submitCustomGeometry(poseStack, RenderTypes.entityCutout(BARB), (pose, buffer) -> {
            quad(buffer, pose, a, f, side, light);
            quad(buffer, pose, a, f, up, light);
        });
    }

    /** One cut-out plane of the barb, 0.7 blocks long, centred on c: texture x runs along f (the tip at u = 1), y along across. */
    private static void quad(VertexConsumer buffer, PoseStack.Pose pose, Vec3 c, Vec3 f, Vec3 across, int light) {
        float h = 0.35F;
        Vec3[] p = {c.add(f.scale(-h)).add(across.scale(h)), c.add(f.scale(h)).add(across.scale(h)), c.add(f.scale(h)).add(across.scale(-h)),
                c.add(f.scale(-h)).add(across.scale(-h))};
        float[][] uv = {{0, 0}, {1, 0}, {1, 1}, {0, 1}};
        Vec3 n = f.cross(across).normalize();
        for (int k = 0; k < 4; k++) {
            buffer.addVertex(pose, (float) p[k].x, (float) p[k].y, (float) p[k].z).setColor(-1).setUv(uv[k][0], uv[k][1])
                    .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose, (float) n.x, (float) n.y, (float) n.z);
        }
    }
}
