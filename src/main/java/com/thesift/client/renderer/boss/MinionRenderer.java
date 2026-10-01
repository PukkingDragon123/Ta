package com.thesift.client.renderer.boss;

import com.thesift.client.Expression;
import com.thesift.client.renderer.ExpressionTextures;
import com.thesift.client.renderer.SiftMobRenderer;
import com.thesift.client.renderer.state.MinionRenderState;
import com.thesift.entity.boss.OrchestraMinion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Shared renderer of the Dictator's orchestra: faces that scowl while winding up, glowing eyes and throats. */
public class MinionRenderer<T extends OrchestraMinion, M extends EntityModel<MinionRenderState>> extends SiftMobRenderer<T, MinionRenderState, M> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.ANGRY, Expression.HURT, Expression.DEAD};
    private final ExpressionTextures textures;
    private final ExpressionTextures glow;

    public MinionRenderer(EntityRendererProvider.Context context, M model, String name, float shadow) {
        super(context, model, shadow);
        this.textures = ExpressionTextures.single(name, PAINTED);
        this.glow = new ExpressionTextures(name, new String[]{name}, "_glow", PAINTED);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> this.glow.get(s.expression),
                (s, age) -> s.windingUp ? 1.0F : 0.5F + 0.2F * Mth.sin(age * 0.1F), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(MinionRenderState state) {
        return this.textures.get(state.expression);
    }

    @Override
    public MinionRenderState createRenderState() {
        return new MinionRenderState();
    }

    @Override
    protected Expression expression(T entity, MinionRenderState state) {
        return Expression.pick(entity, entity.isWindingUp() || entity.getTarget() != null, false, false);
    }

    @Override
    public void extractRenderState(T entity, MinionRenderState state, float partialTicks) {
        state.windingUp = entity.isWindingUp();
        state.attack.copyFrom(entity.attackAnimation);
        super.extractRenderState(entity, state, partialTicks);
    }
}
