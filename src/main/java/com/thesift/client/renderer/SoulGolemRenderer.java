package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.client.EchoerClient;
import com.thesift.client.Expression;
import com.thesift.client.model.SoulGolemModel;
import com.thesift.client.renderer.state.SoulGolemRenderState;
import com.thesift.entity.SoulGolem;
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
 * The Soul Golem, a copper mole (CAVE v4, tools/echoer.py): its button eyes' glints, the soul lamp in its
 * lightning rod's cap and the vent on its back glow with its soul energy; whatever it dug up it holds up in
 * its forepaws.
 */
public class SoulGolemRenderer extends SiftMobRenderer<SoulGolem, SoulGolemRenderState, SoulGolemModel> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.HAPPY, Expression.SLEEP, Expression.HURT, Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("soul_golem", PAINTED);
    private static final ExpressionTextures GLOW = new ExpressionTextures("soul_golem", new String[]{"soul_golem"}, "_glow", PAINTED);
    private final ItemModelResolver items;

    public SoulGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulGolemModel(context.bakeLayer(EchoerClient.SOUL_GOLEM)), 0.45F);
        this.items = context.getItemModelResolver();
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression),
                (s, age) -> (0.15F + 0.85F * s.energy) * (0.88F + 0.12F * Mth.sin(age * 0.5F + s.seed) * Mth.sin(age * 0.23F)),
                this.model, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new FindLayer(this));
    }

    @Override
    public Identifier getTextureLocation(SoulGolemRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected Expression expression(SoulGolem entity, SoulGolemRenderState state) {
        boolean happy = entity.happyAnimation.isStarted() && entity.happyAnimation.getTimeInMillis(entity.tickCount) < 1200
                || entity.getMode() == SoulGolem.PEEKING;
        return Expression.pick(entity, false, happy, entity.isSlumped());
    }

    @Override
    protected float bounciness() {
        return 0.8F;
    }

    @Override
    public SoulGolemRenderState createRenderState() {
        return new SoulGolemRenderState();
    }

    @Override
    public void extractRenderState(SoulGolem entity, SoulGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.energy = entity.getEnergy() / (float) SoulGolem.MAX_ENERGY;
        state.slump = Mth.lerp(partialTicks, entity.slumpO, entity.slump);
        state.mode = entity.getMode();
        state.dig = Mth.lerp(partialTicks, entity.digO, entity.dig);
        state.peek = Mth.lerp(partialTicks, entity.peekO, entity.peek);
        state.burrow = Mth.lerp(partialTicks, entity.burrowO, entity.burrow);
        state.happy.copyFrom(entity.happyAnimation);
        state.emerge.copyFrom(entity.emergeAnimation);
        state.carrying = !entity.getStash().isEmpty();
        this.items.updateForTopItem(state.carried, entity.getStash(), ItemDisplayContext.GROUND, entity.level(), null, entity.getId());
        state.seed = (entity.getId() * 13) % 97;
    }

    /** Its find, held up between its forepaws in front of its drill (out of sight while it is under the ground). */
    private static class FindLayer extends RenderLayer<SoulGolemRenderState, SoulGolemModel> {
        FindLayer(RenderLayerParent<SoulGolemRenderState, SoulGolemModel> parent) {
            super(parent);
        }

        @Override
        public void submit(PoseStack poseStack, SubmitNodeCollector collector, int light, SoulGolemRenderState state, float yRot, float xRot) {
            if (!state.carrying || state.carried.isEmpty() || state.isInvisible || state.burrow > 0.5F) {
                return;
            }
            poseStack.pushPose();
            this.getParentModel().body().translateAndRotate(poseStack);
            poseStack.translate(0.0F, 0.5F / 16.0F, -10.5F / 16.0F);
            poseStack.rotateDegrees(Axis.XP, -15.0F);
            poseStack.scale(0.7F, 0.7F, 0.7F);
            state.carried.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, state.outlineColor);
            poseStack.popPose();
        }
    }
}
