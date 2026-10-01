package com.thesift.client.renderer.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.model.BulbModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.BulbRenderState;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * The Bulb's see-through jelly, drawn after the opaque core like vanilla's slime outer layer:
 * translucent texels let the darker heart show through, the face stays crisp on top.
 */
public class BulbJellyLayer extends RenderLayer<BulbRenderState, BulbModel> {
    private final BulbModel jelly;
    private final java.util.function.Function<BulbRenderState, Identifier> texture;

    public BulbJellyLayer(RenderLayerParent<BulbRenderState, BulbModel> parent, EntityModelSet models,
            java.util.function.Function<BulbRenderState, Identifier> texture) {
        super(parent);
        this.jelly = new BulbModel(models.bakeLayer(ModModelLayers.BULB), BulbModel.Pass.JELLY);
        this.texture = texture;
    }

    @Override
    public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, BulbRenderState state, float yRot, float xRot) {
        boolean outlineOnly = state.appearsGlowing() && state.isInvisible;
        if (state.isInvisible && !outlineOnly) {
            return;
        }
        Identifier tex = this.texture.apply(state);
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        collector.order(1).submitModel(this.jelly, state, poseStack, outlineOnly ? RenderTypes.outline(tex) : RenderTypes.entityTranslucent(tex), light,
                overlay, state.outlineColor);
    }
}
