package com.thesift.client.renderer.boss;

import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.boss.DictatorModel;
import com.thesift.client.renderer.SiftMobRenderer;
import com.thesift.client.renderer.state.DictatorRenderState;
import com.thesift.entity.boss.Dictator;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

public class DictatorRenderer extends SiftMobRenderer<Dictator, DictatorRenderState, DictatorModel> {
    private static final com.thesift.client.Expression[] PAINTED = {com.thesift.client.Expression.ANGRY, com.thesift.client.Expression.HURT, com.thesift.client.Expression.DEAD};
    private static final com.thesift.client.renderer.ExpressionTextures TEXTURES = com.thesift.client.renderer.ExpressionTextures.single("dictator", PAINTED);
    private static final com.thesift.client.renderer.ExpressionTextures GLOW = new com.thesift.client.renderer.ExpressionTextures("dictator",
            new String[]{"dictator"}, "_glow", PAINTED);

    public DictatorRenderer(EntityRendererProvider.Context context) {
        super(context, new DictatorModel(context.bakeLayer(ModModelLayers.DICTATOR)), 0.7F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression),
                (s, age) -> Math.min(1.0F, 0.55F + s.phase * 0.12F + 0.25F * Mth.sin(age * (0.06F + s.phase * 0.04F))), this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(DictatorRenderState state) {
        return TEXTURES.get(state.expression);
    }

    /** He grows with every movement, more godlike each time; the change comes at the height of his transformation. */
    @Override
    protected void scale(DictatorRenderState state, com.mojang.blaze3d.vertex.PoseStack poseStack) {
        float s = 1.0F + (state.phase - 1) * 0.12F;
        if (state.transform >= 0.0F && state.phase > 1) {
            s = Mth.lerp(com.thesift.client.model.Anim.smooth((state.transform - 0.45F) / 0.2F), s - 0.12F, s);
        }
        if (state.transform >= 0.0F) {
            // a pulse of power at the moment of change
            s *= 1.0F + 0.12F * Math.max(0.0F, 1.0F - Math.abs(state.transform - 0.57F) * 8.0F);
        }
        poseStack.scale(s, s, s);
        super.scale(state, poseStack);
    }

    @Override
    protected float bounciness() {
        return 0.5F;
    }

    @Override
    public DictatorRenderState createRenderState() {
        return new DictatorRenderState();
    }

    @Override
    public void extractRenderState(Dictator entity, DictatorRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.phase = entity.getPhase();
        state.action = entity.getAction();
        state.actionTime = entity.actionTime(partialTicks);
        int tt = entity.transformTicks();
        state.transform = tt > 0 ? 1.0F - (tt - partialTicks) / Dictator.TRANSFORM_TICKS : -1.0F;
        int at = entity.assembleTicks();
        state.assemble = at > 0 ? Math.min(1.0F, 1.0F - (at - partialTicks) / Dictator.ASSEMBLE_TICKS) : -1.0F;
        state.blink.copyFrom(entity.blinkAnimation);
        state.summon.copyFrom(entity.summonAnimation);
        state.crescendo.copyFrom(entity.crescendoAnimation);
        state.slash.copyFrom(entity.slashAnimation);
        state.roar.copyFrom(entity.roarAnimation);
    }
}
