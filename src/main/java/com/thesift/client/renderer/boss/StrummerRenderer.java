package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.boss.StrummerModel;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.Strummer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Weaver: the mini-boss renderer, plus the glowing silk thread it swings on - two strands from
 * its spinnerets up to the anchor, shot out over the first few ticks and trembling while it hangs.
 */
public class StrummerRenderer extends MiniBossRenderer<Strummer, StrummerModel> {
    public StrummerRenderer(EntityRendererProvider.Context context) {
        super(context, new StrummerModel(context.bakeLayer(ModModelLayers.STRUMMER)), "strummer", Strummer.SCALE, 0.9F, (e, s) -> null);
    }

    /** What the Weaver's model and thread need beyond the shared mini-boss state. */
    public static class WeaverState extends MiniBossRenderState {
        public boolean climbing;
        /** From the entity's feet to the thread's anchor, or null with no thread out. */
        public @Nullable Vec3 thread;
    }

    @Override
    public MiniBossRenderState createRenderState() {
        return new WeaverState();
    }

    @Override
    public void extractRenderState(Strummer entity, MiniBossRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof WeaverState w) {
            w.climbing = entity.isClimbing();
            Vec3 anchor = entity.threadAnchor();
            w.thread = anchor == null ? null : anchor.subtract(entity.getPosition(partialTicks));
        }
    }

    @Override
    protected AABB getBoundingBoxForCulling(Strummer entity, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
        Vec3 anchor = entity.threadAnchor();
        return anchor == null ? box : box.minmax(new AABB(anchor, anchor).inflate(0.5));
    }

    @Override
    public void submit(MiniBossRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        super.submit(state, poseStack, collector, camera);
        if (!(state instanceof WeaverState w) || w.thread == null) {
            return;
        }
        Vec3 to = w.thread;
        float shot = Mth.clamp(state.stateTime / 7.0F, 0.0F, 1.0F);
        float width = Minecraft.getInstance().gameRenderer.gameRenderState().windowRenderState.appropriateLineWidth;
        float age = state.ageInTicks;
        collector.submitCustomGeometry(poseStack, RenderTypes.lines(), (pose, buffer) -> {
            float ox = 0.0F;
            float oy = 1.7F;
            float oz = 0.0F;
            float dx = (float) to.x - ox;
            float dy = (float) to.y - oy;
            float dz = (float) to.z - oz;
            float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 0.01F) {
                return;
            }
            float nx = dx / len;
            float ny = dy / len;
            float nz = dz / len;
            int segments = 16;
            for (int strand = 0; strand < 2; strand++) {
                float off = strand == 0 ? -0.04F : 0.04F;
                int colour = strand == 0 ? 0xFFD6FFFF : 0xFF29DFEB;
                for (int i = 0; i < segments; i++) {
                    float a = shot * i / segments;
                    float b = shot * (i + 1) / segments;
                    // a little tremble along the strand, still at both ends
                    float ta = Mth.sin(a * Mth.PI) * Mth.sin(age * 1.7F + a * 9.0F + strand) * 0.06F;
                    float tb = Mth.sin(b * Mth.PI) * Mth.sin(age * 1.7F + b * 9.0F + strand) * 0.06F;
                    buffer.addVertex(pose, ox + dx * a + off + ta, oy + dy * a, oz + dz * a + off - ta).setColor(colour).setNormal(pose, nx, ny, nz)
                            .setLineWidth(width);
                    buffer.addVertex(pose, ox + dx * b + off + tb, oy + dy * b, oz + dz * b + off - tb).setColor(colour).setNormal(pose, nx, ny, nz)
                            .setLineWidth(width);
                }
            }
        });
    }
}
