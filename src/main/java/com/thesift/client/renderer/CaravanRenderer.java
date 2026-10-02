package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.client.model.CaravanModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.CaravanRenderState;
import com.thesift.entity.caravan.Caravan;
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

/** Caravans in their five colours, glowing crystal shards and jaws, and the ore chunk a worker carries home. */
public class CaravanRenderer extends SiftMobRenderer<Caravan, CaravanRenderState, CaravanModel> {
    private static final String[] VARIANTS = {"caravan_amber", "caravan_rose", "caravan_teal", "caravan_violet", "caravan_gold"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("caravan", VARIANTS);
    private static final ExpressionTextures GLOW = new ExpressionTextures("caravan", VARIANTS, "_glow");
    private final ItemModelResolver items;

    public CaravanRenderer(EntityRendererProvider.Context context) {
        super(context, new CaravanModel(context.bakeLayer(ModModelLayers.CARAVAN)), 0.4F);
        this.items = context.getItemModelResolver();
        // the crystals pulse softly, brighter while the Caravan is calm and singing
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.variant, s.expression),
                (s, age) -> (s.calm ? 0.85F : 0.65F) + 0.2F * Mth.sin(age * 0.11F), this.model, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new CarriedOreLayer(this));
    }

    @Override
    public Identifier getTextureLocation(CaravanRenderState state) {
        return TEXTURES.get(state.variant, state.expression);
    }

    @Override
    public CaravanRenderState createRenderState() {
        return new CaravanRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.6F;
    }

    @Override
    public void extractRenderState(Caravan entity, CaravanRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Mth.clamp(entity.getVariant(), 0, VARIANTS.length - 1);
        state.soldier = entity.isSoldier();
        state.calm = entity.isCalm();
        state.carrying = !entity.getCarried().isEmpty();
        this.items.updateForTopItem(state.carried, entity.getCarried(), ItemDisplayContext.GROUND, entity.level(), null, entity.getId());
        state.tap.copyFrom(entity.tapAnimation);
        state.mine.copyFrom(entity.mineAnimation);
        state.bite.copyFrom(entity.biteAnimation);
        state.build.copyFrom(entity.buildAnimation);
    }

    /** The ore chunk, held out in front of the mandibles and bobbing with the head. */
    private static class CarriedOreLayer extends RenderLayer<CaravanRenderState, CaravanModel> {
        CarriedOreLayer(RenderLayerParent<CaravanRenderState, CaravanModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, CaravanRenderState state, float yRot, float xRot) {
            if (!state.carrying || state.carried.isEmpty() || state.isInvisible) {
                return;
            }
            CaravanModel model = this.getParentModel();
            poseStack.pushPose();
            model.body().translateAndRotate(poseStack);
            model.head().translateAndRotate(poseStack);
            poseStack.translate(0.0F, 1.5F / 16.0F, -5.5F / 16.0F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(0.75F, 0.75F, 0.75F);
            state.carried.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
