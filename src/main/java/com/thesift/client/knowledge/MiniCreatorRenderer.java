package com.thesift.client.knowledge;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.model.Anim;
import com.thesift.client.renderer.ExpressionTextures;
import com.thesift.client.renderer.SiftMobRenderer;
import com.thesift.entity.MiniCreator;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** F3: the Mini Creator - he blinks, his gems and Prism block glow, and he pops in and spins away to nothing. */
public class MiniCreatorRenderer extends SiftMobRenderer<MiniCreator, MiniCreatorRenderState, MiniCreatorModel> {
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("mini_creator", Expression.BLINK);
    /** S1 land: his eyes glow (and close with the blink), with his halo, runes and gold. */
    private static final ExpressionTextures GLOW = new ExpressionTextures("mini_creator", new String[]{"mini_creator"}, "_glow", Expression.BLINK);

    public MiniCreatorRenderer(EntityRendererProvider.Context context) {
        super(context, new MiniCreatorModel(context.bakeLayer(KnowledgeClient.MINI_CREATOR)), 0.4F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(0, s.expression), (s, age) -> 0.8F + 0.2F * Mth.sin(age * 0.15F), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(MiniCreatorRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected Expression expression(MiniCreator entity, MiniCreatorRenderState state) {
        return Expression.pick(entity, false, false, false);
    }

    @Override
    public MiniCreatorRenderState createRenderState() {
        return new MiniCreatorRenderState();
    }

    @Override
    public void extractRenderState(MiniCreator entity, MiniCreatorRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.talk.copyFrom(entity.talkAnimation);
        state.wave.copyFrom(entity.waveAnimation);
        state.celebrate.copyFrom(entity.celebrateAnimation);
        state.appear.copyFrom(entity.appearAnimation);
        state.poof.copyFrom(entity.poofAnimation);
        state.rise.copyFrom(entity.riseAnimation);
        state.seed = (entity.getId() * 37) % 100 / 10.0F;
    }

    @Override
    protected void scale(MiniCreatorRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        float k = 0.75F; // a mini platypus: three quarters of the model's full size
        float ap = Anim.seconds(state.appear, state.ageInTicks);
        if (ap >= 0.0F && ap < 0.6F) {
            k = Anim.backOut(Anim.clamp01(ap / 0.6F));
        }
        float pf = Anim.seconds(state.poof, state.ageInTicks);
        if (pf >= 0.0F) {
            k *= Math.max(0.0F, 1.0F - pf / 0.7F);
        }
        poseStack.scale(k, k, k);
    }
}
