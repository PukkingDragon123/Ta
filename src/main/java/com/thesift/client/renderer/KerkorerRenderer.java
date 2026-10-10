package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.TheSift;
import com.thesift.client.DunesClient;
import com.thesift.client.model.Anim;
import com.thesift.client.model.KerkorerModel;
import com.thesift.client.renderer.state.KerkorerRenderState;
import com.thesift.entity.dunes.Kerkorer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;

/**
 * The Kerkorer: its own rose-and-plum skin, with its sand skin (tinted the colour of the ground it lies on) faded in
 * over it as it melts into the dunes - and the bait on the pad of its tongue, which never fades.
 */
public class KerkorerRenderer extends SiftMobRenderer<Kerkorer, KerkorerRenderState, KerkorerModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/kerkorer/kerkorer.png");
    private static final Identifier CAMO = TheSift.id("textures/entity/kerkorer/kerkorer_camo.png");
    private final ItemModelResolver items;

    public KerkorerRenderer(EntityRendererProvider.Context context) {
        super(context, new KerkorerModel(context.bakeLayer(DunesClient.KERKORER)), 0.9F);
        this.items = context.getItemModelResolver();
        this.addLayer(new CamoLayer(this));
        this.addLayer(new LureLayer(this));
    }

    @Override
    public Identifier getTextureLocation(KerkorerRenderState state) {
        return TEXTURE;
    }

    @Override
    public KerkorerRenderState createRenderState() {
        return new KerkorerRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.35F;
    }

    @Override
    public void extractRenderState(Kerkorer entity, KerkorerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.state = entity.getState();
        state.camo = Mth.lerp(partialTicks, entity.camoO, entity.camo);
        BlockPos on = entity.getOnPos();
        state.ground = entity.level().getBlockState(on).getMapColor(entity.level(), on).col;
        state.eyeLYaw = Mth.lerp(partialTicks, entity.eyesO[0], entity.eyes[0]);
        state.eyeLPitch = Mth.lerp(partialTicks, entity.eyesO[1], entity.eyes[1]);
        state.eyeRYaw = Mth.lerp(partialTicks, entity.eyesO[2], entity.eyes[2]);
        state.eyeRPitch = Mth.lerp(partialTicks, entity.eyesO[3], entity.eyes[3]);
        state.tongue = entity.tongueOut();
        state.hasLure = !entity.getLure().isEmpty();
        this.items.updateForTopItem(state.lure, entity.getLure(), ItemDisplayContext.GROUND, entity.level(), null, entity.getId());
        state.seed = (entity.getId() * 17) % 101;
        state.snap.copyFrom(entity.snapAnimation);
        state.call.copyFrom(entity.callAnimation);
        state.glint.copyFrom(entity.glintAnimation);
    }

    /** Its sand skin, faded in over its own as it lies still, in the colour of the ground under it. */
    private static class CamoLayer extends RenderLayer<KerkorerRenderState, KerkorerModel> {
        CamoLayer(RenderLayerParent<KerkorerRenderState, KerkorerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, KerkorerRenderState state, float yRot, float xRot) {
            if (state.camo < 0.02F || state.isInvisible) {
                return;
            }
            // the sand skin is pale: tint it towards the ground's own colour (pale sand stays as it is)
            int g = state.ground;
            int r = (int) Mth.lerp(0.55F, 255.0F, (float) ARGB.red(g));
            int gg = (int) Mth.lerp(0.55F, 255.0F, (float) ARGB.green(g));
            int b = (int) Mth.lerp(0.55F, 255.0F, (float) ARGB.blue(g));
            int alpha = (int) (Anim.smooth(state.camo) * 255.0F);
            collector.order(1).submitModel(this.getParentModel(), state, poseStack, RenderTypes.entityTranslucent(CAMO), light,
                    LivingEntityRenderer.getOverlayCoords(state, 0.0F), ARGB.color(alpha, r, gg, b), null, state.outlineColor);
        }
    }

    /** The bait, stuck upright on the pad of its tongue (it goes with the tongue when it lashes out), bobbing a little. */
    private static class LureLayer extends RenderLayer<KerkorerRenderState, KerkorerModel> {
        LureLayer(RenderLayerParent<KerkorerRenderState, KerkorerModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, KerkorerRenderState state, float yRot, float xRot) {
            if (!state.hasLure || state.lure.isEmpty() || state.isInvisible) {
                return;
            }
            poseStack.pushPose();
            for (ModelPart p : this.getParentModel().tipChain()) {
                p.translateAndRotate(poseStack);
            }
            float age = state.ageInTicks + state.seed;
            poseStack.translate(0.0F, -1.3F / 16.0F + Mth.sin(age * 0.12F) * 0.01F, -1.0F / 16.0F);
            poseStack.rotateDegrees(Axis.ZP, 180.0F);
            poseStack.rotateDegrees(Axis.YP, Mth.sin(age * 0.05F) * 20.0F);
            poseStack.scale(0.6F, 0.6F, 0.6F);
            state.lure.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
