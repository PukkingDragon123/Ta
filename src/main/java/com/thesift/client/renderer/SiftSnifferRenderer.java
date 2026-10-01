package com.thesift.client.renderer;

import com.thesift.client.renderer.layers.SnifferSaddleLayer;
import com.thesift.client.renderer.state.SiftSnifferRenderState;
import com.thesift.entity.SiftSniffer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SnifferRenderer;
import net.minecraft.client.renderer.entity.state.SnifferRenderState;
import net.minecraft.world.entity.animal.sniffer.Sniffer;

/** The vanilla Sniffer, plus a saddle once it is tamed and saddled. */
public class SiftSnifferRenderer extends SnifferRenderer {
    public SiftSnifferRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new SnifferSaddleLayer(this));
    }

    @Override
    public SnifferRenderState createRenderState() {
        return new SiftSnifferRenderState();
    }

    @Override
    public void extractRenderState(Sniffer entity, SnifferRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof SiftSnifferRenderState s) {
            s.saddled = entity instanceof SiftSniffer sniffer && sniffer.isSaddled();
        }
    }
}
