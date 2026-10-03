package com.thesift.client.renderer;

import com.thesift.client.Expression;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SifterModel;
import com.thesift.client.renderer.state.SifterRenderState;
import com.thesift.entity.Sifter;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** CR1: the Sifter, a living bell. The gold runes round its waist flare up with every strike of its clapper. */
public class SifterRenderer extends SiftMobRenderer<Sifter, SifterRenderState, SifterModel> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.HAPPY, Expression.ANGRY, Expression.HURT, Expression.SLEEP,
            Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("sifter", PAINTED);
    private static final ExpressionTextures GLOW = new ExpressionTextures("sifter", new String[]{"sifter"}, "_glow", PAINTED);

    public SifterRenderer(EntityRendererProvider.Context context) {
        super(context, new SifterModel(context.bakeLayer(ModModelLayers.SIFTER)), 0.6F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression), (s, age) -> s.ring, this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(SifterRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected float bounciness() {
        return 0.7F;
    }

    /** Napping in the sand it shuts its eyes - and peeks when someone comes close; a good ring makes it beam. */
    @Override
    protected Expression expression(Sifter entity, SifterRenderState state) {
        boolean asleep = entity.isBurrowed() && entity.level().getNearestPlayer(entity, 6.0) == null;
        boolean happy = !entity.isAggressive() && entity.bell.ring > 0.7F;
        return Expression.pick(entity, entity.isAggressive(), happy, asleep);
    }

    @Override
    public SifterRenderState createRenderState() {
        return new SifterRenderState();
    }

    @Override
    protected float getShadowRadius(SifterRenderState state) {
        return state.burrowed ? 0.0F : super.getShadowRadius(state);
    }

    @Override
    public void extractRenderState(Sifter entity, SifterRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.burrowed = entity.isBurrowed();
        state.squash = entity.squash.get(partialTicks);
        state.rockX = entity.bell.rockX(partialTicks);
        state.rockZ = entity.bell.rockZ(partialTicks);
        state.swingX = entity.bell.swingX(partialTicks);
        state.swingZ = entity.bell.swingZ(partialTicks);
        state.ring = entity.bell.ring(partialTicks);
        state.bonk.copyFrom(entity.bonkAnimation);
        state.emerge.copyFrom(entity.emergeAnimation);
        state.burrow.copyFrom(entity.burrowAnimation);
        state.chasing = entity.isAggressive();
    }
}
