package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.TheSift;
import com.thesift.client.CaveCreaturesClient;
import com.thesift.client.model.SculklingModel;
import com.thesift.client.renderer.state.SculklingRenderState;
import com.thesift.entity.cave.Sculkling;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/** Sculklings: glowing ear veins and chest soul, and whatever shiny thing a thief is clutching. */
public class SculklingRenderer extends SiftMobRenderer<Sculkling, SculklingRenderState, SculklingModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/sculkling/sculkling.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/sculkling/sculkling_glow.png");
    private final ItemModelResolver items;

    public SculklingRenderer(EntityRendererProvider.Context context) {
        super(context, new SculklingModel(context.bakeLayer(CaveCreaturesClient.SCULKLING)), 0.35F);
        this.items = context.getItemModelResolver();
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> 0.7F + 0.3F * Mth.sin(age * 0.2F + s.seed),
                this.model, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LootLayer(this));
    }

    @Override
    public Identifier getTextureLocation(SculklingRenderState state) {
        return TEXTURE;
    }

    @Override
    public SculklingRenderState createRenderState() {
        return new SculklingRenderState();
    }

    @Override
    public void extractRenderState(Sculkling entity, SculklingRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scared = entity.isScared();
        state.hasLoot = !entity.getLoot().isEmpty();
        this.items.updateForTopItem(state.loot, entity.getLoot(), ItemDisplayContext.GROUND, entity.level(), null, entity.getId());
        state.leftEar = entity.leftEar.get(partialTicks);
        state.rightEar = entity.rightEar.get(partialTicks);
        state.seed = (entity.getId() * 13) % 97;
        state.snatch.copyFrom(entity.snatchAnimation);
        state.cover.copyFrom(entity.coverAnimation);
        state.giggle.copyFrom(entity.giggleAnimation);
        state.screech.copyFrom(entity.screechAnimation);
    }

    /** The stolen prize, held in the right claw against its chest. */
    private static class LootLayer extends RenderLayer<SculklingRenderState, SculklingModel> {
        LootLayer(RenderLayerParent<SculklingRenderState, SculklingModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SculklingRenderState state, float yRot, float xRot) {
            if (!state.hasLoot || state.loot.isEmpty() || state.isInvisible) {
                return;
            }
            SculklingModel model = this.getParentModel();
            poseStack.pushPose();
            model.body().translateAndRotate(poseStack);
            model.rightArm().translateAndRotate(poseStack);
            model.rightForearm().translateAndRotate(poseStack);
            model.rightHand().translateAndRotate(poseStack);
            poseStack.translate(0.0F, 2.0F / 16.0F, -0.5F / 16.0F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(0.45F, 0.45F, 0.45F);
            state.loot.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
