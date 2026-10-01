package com.thesift.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.renderer.state.SiftSnifferRenderState;
import net.minecraft.client.model.animal.sniffer.SnifferModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.resources.Identifier;

/** Draws the saddle and its coral blanket over a tamed Sniffer's back, following every animation. */
public class SnifferSaddleLayer extends RenderLayer<SnifferRenderState, SnifferModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/sift_sniffer/saddle.png");

    public SnifferSaddleLayer(RenderLayerParent<SnifferRenderState, SnifferModel> parent) {
        super(parent);
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SnifferRenderState state, float yRot, float xRot) {
        if (state instanceof SiftSnifferRenderState s && s.saddled && !state.isBaby) {
            coloredCutoutModelCopyLayerRender(this.getParentModel(), TEXTURE, poseStack, collector, light, state, -1, 1);
        }
    }
}
