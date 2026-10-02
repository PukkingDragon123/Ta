package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.renderer.state.MiniBossRenderState;
import java.util.function.Function;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * While the Thumper grows into the titan, its sculk-swallowed skin is drawn over its old one,
 * fading in with the growth (once it is fully grown the renderer uses that skin outright).
 */
public class TitanSkinLayer<M extends EntityModel<MiniBossRenderState>> extends RenderLayer<MiniBossRenderState, M> {
    private final Function<MiniBossRenderState, Identifier> texture;

    public TitanSkinLayer(RenderLayerParent<MiniBossRenderState, M> parent, Function<MiniBossRenderState, Identifier> texture) {
        super(parent);
        this.texture = texture;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, MiniBossRenderState state, float yRot, float xRot) {
        if (state.titan <= 0.0F || state.titan >= 0.999F || state.isInvisible) {
            return;
        }
        int alpha = Math.max(1, Math.min(255, (int) (state.titan * 255.0F)));
        int color = alpha << 24 | 0xFFFFFF;
        collector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.entityTranslucent(this.texture.apply(state)), light,
                LivingEntityRenderer.getOverlayCoords(state, 0.0F), color, null, state.outlineColor);
    }
}
