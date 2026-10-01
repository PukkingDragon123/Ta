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
        state.blink.copyFrom(entity.blinkAnimation);
        state.summon.copyFrom(entity.summonAnimation);
        state.crescendo.copyFrom(entity.crescendoAnimation);
        state.slash.copyFrom(entity.slashAnimation);
        state.roar.copyFrom(entity.roarAnimation);
    }
}
