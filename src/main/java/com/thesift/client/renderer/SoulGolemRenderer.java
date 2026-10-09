package com.thesift.client.renderer;

import com.thesift.client.EchoerClient;
import com.thesift.client.Expression;
import com.thesift.client.model.SoulGolemModel;
import com.thesift.client.renderer.state.SoulGolemRenderState;
import com.thesift.entity.SoulGolem;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Soul Golem: its eyes, seams and lamp glow with its soul energy, flickering like a flame. */
public class SoulGolemRenderer extends SiftMobRenderer<SoulGolem, SoulGolemRenderState, SoulGolemModel> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.HAPPY, Expression.SLEEP, Expression.HURT, Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("soul_golem", PAINTED);
    private static final ExpressionTextures GLOW = new ExpressionTextures("soul_golem", new String[]{"soul_golem"}, "_glow", PAINTED);

    public SoulGolemRenderer(EntityRendererProvider.Context context) {
        super(context, new SoulGolemModel(context.bakeLayer(EchoerClient.SOUL_GOLEM)), 0.35F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression),
                (s, age) -> (0.15F + 0.85F * s.energy) * (0.88F + 0.12F * Mth.sin(age * 0.5F + s.seed) * Mth.sin(age * 0.23F)),
                this.model, RenderTypes::entityTranslucentEmissive, false));
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
    public SoulGolemRenderState createRenderState() {
        return new SoulGolemRenderState();
    }

    @Override
    public void extractRenderState(SoulGolem entity, SoulGolemRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.energy = entity.getEnergy() / (float) SoulGolem.MAX_ENERGY;
        state.slump = Mth.lerp(partialTicks, entity.slumpO, entity.slump);
        state.mode = entity.getMode();
        state.dig = Mth.lerp(partialTicks, entity.digO, entity.dig);   // CAVE
        state.peek = Mth.lerp(partialTicks, entity.peekO, entity.peek);
        state.happy.copyFrom(entity.happyAnimation);
        state.seed = (entity.getId() * 13) % 97;
    }
}
