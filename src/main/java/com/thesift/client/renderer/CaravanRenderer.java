package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.client.model.Anim;
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

/**
 * Caravans in their five caravan colours: glowing gems, eyes and pores, the ore chunk a worker carries
 * to its Queen in its claws, and the crab's sideways scuttle - as it gets going it turns side-on to its path
 * (each crab favours its own side) and turns back to face you when it stops.
 */
public class CaravanRenderer extends SiftMobRenderer<Caravan, CaravanRenderState, CaravanModel> {
    private static final String[] VARIANTS = {"caravan_amber", "caravan_rose", "caravan_teal", "caravan_violet", "caravan_gold"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("caravan", VARIANTS);
    private static final ExpressionTextures GLOW = new ExpressionTextures("caravan", VARIANTS, "_glow");
    private final ItemModelResolver items;

    public CaravanRenderer(EntityRendererProvider.Context context) {
        super(context, new CaravanModel(context.bakeLayer(ModModelLayers.CARAVAN)), 0.5F);
        this.items = context.getItemModelResolver();
        // the gems pulse softly, brighter while the Caravan is calm and singing
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.variant, s.expression),
                (s, age) -> (s.calm ? 0.85F : 0.65F) + 0.2F * Mth.sin(age * 0.11F + s.seed), this.model, RenderTypes::entityTranslucentEmissive, false));
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
        state.growth = entity.getGrowth() / 100.0F;
        state.seed = entity.getId();
        state.hand = (entity.getId() & 1) == 0 ? 1.0F : -1.0F;
        // side-on once it is really moving (a carrying worker walks claws-first, holding its load out in front);
        // CAVE: eased by the entity, so it no longer snaps round when it picks up or sets down its load
        state.scuttle = Anim.smooth(Mth.lerp(partialTicks, entity.scuttleO, entity.scuttle));
        this.items.updateForTopItem(state.carried, entity.getCarried(), ItemDisplayContext.GROUND, entity.level(), null, entity.getId());
        state.tap.copyFrom(entity.tapAnimation);
        state.mine.copyFrom(entity.mineAnimation);
        state.bite.copyFrom(entity.biteAnimation);
        state.offer.copyFrom(entity.offerAnimation);
        state.warn.copyFrom(entity.warnAnimation);
    }

    @Override
    protected void scale(CaravanRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.scuttle > 0.0F) {
            poseStack.rotateDegrees(Axis.YP, 90.0F * state.scuttle * state.hand);
        }
    }

    /** The ore chunk, held in front of the shell between the two claws. */
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
            poseStack.translate(0.0F, 0.6F / 16.0F, -7.2F / 16.0F);
            poseStack.rotateDegrees(Axis.XP, 90.0F);
            poseStack.scale(0.6F, 0.6F, 0.6F);
            state.carried.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
