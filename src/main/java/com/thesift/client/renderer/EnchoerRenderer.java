package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.model.EnchoerModel;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.renderer.state.EnchoerRenderState;
import com.thesift.entity.Enchoer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** The Echoer, a furry god-deer: its runes, eyes, antler tips, glass chime and halo glow, brighter while it hums or dances. */
public class EnchoerRenderer extends SiftMobRenderer<Enchoer, EnchoerRenderState, EnchoerModel> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.HAPPY, Expression.SLEEP, Expression.HURT, Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("enchoer", PAINTED);
    private static final ExpressionTextures GLOW = new ExpressionTextures("enchoer", new String[]{"enchoer"}, "_glow", PAINTED);

    public EnchoerRenderer(EntityRendererProvider.Context context) {
        super(context, new EnchoerModel(context.bakeLayer(ModModelLayers.ENCHOER)), 0.7F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression),
                (s, age) -> Math.min(1.0F, 0.55F + 0.2F * Mth.sin(age * 0.06F + s.seed) + 0.35F * Math.max(s.sing, s.dance)),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(EnchoerRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected Expression expression(Enchoer entity, EnchoerRenderState state) {
        int st = entity.getState();
        boolean happy = entity.isSinging() || st == Enchoer.DANCING;
        Expression e = Expression.pick(entity, false, happy, st == Enchoer.SLEEPING);
        return st == Enchoer.DISAPPOINTED && e == Expression.NEUTRAL ? Expression.BLINK : e;
    }

    @Override
    protected float bounciness() {
        return 0.5F;
    }

    @Override
    protected void scale(EnchoerRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        poseStack.scale(0.92F, 0.92F, 0.92F);
    }

    /** The antlers, their chimes and the halo rise far above the hitbox, the muzzle reaches past it. */
    @Override
    protected AABB getBoundingBoxForCulling(Enchoer entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.5, 1.2, 1.5);
    }

    @Override
    public EnchoerRenderState createRenderState() {
        return new EnchoerRenderState();
    }

    @Override
    public void extractRenderState(Enchoer entity, EnchoerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.inspect = Mth.lerp(partialTicks, entity.inspectO, entity.inspect);
        state.wait = Mth.lerp(partialTicks, entity.waitO, entity.wait);
        state.dance = Mth.lerp(partialTicks, entity.danceO, entity.dance);
        state.sad = Mth.lerp(partialTicks, entity.sadO, entity.sad);
        state.sleep = Mth.lerp(partialTicks, entity.sleepO, entity.sleep);
        state.sing = Mth.lerp(partialTicks, entity.singO, entity.sing);
        state.bow.copyFrom(entity.bowAnimation);
        state.seed = (entity.getId() * 37) % 210;
    }
}
