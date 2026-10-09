package com.thesift.client.renderer;

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

/**
 * The Echoer, a deer spirit (CAVE: modelled and painted in tools/echoer.py in the Sculk-mob pipeline, with
 * a face for every expression): its glow - the antler tips and crystal chimes, its eyes and tear-lines, the
 * star on its chest, the song-marks on its back and haunches and the soles of its hooves - flares with every
 * note it sings and dims in its sleep.
 */
public class EnchoerRenderer extends SiftMobRenderer<Enchoer, EnchoerRenderState, EnchoerModel> {
    private static final Expression[] PAINTED = {Expression.BLINK, Expression.HAPPY, Expression.SLEEP, Expression.HURT, Expression.DEAD};
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("enchoer", PAINTED);
    private static final ExpressionTextures GLOW = new ExpressionTextures("enchoer", new String[]{"enchoer"}, "_glow", PAINTED);

    public EnchoerRenderer(EntityRendererProvider.Context context) {
        super(context, new EnchoerModel(context.bakeLayer(ModModelLayers.ENCHOER)), 0.6F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression),
                (s, age) -> Mth.clamp(0.55F + 0.12F * Mth.sin(age * 0.05F + s.seed) + 0.5F * s.voice + 0.25F * Math.max(s.dance, s.present), 0.0F, 1.0F)
                        * (1.0F - 0.65F * s.sleep),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(EnchoerRenderState state) {
        return TEXTURES.get(state.expression);
    }

    /** Happy while it dances or holds up a gift, asleep when it roosts. */
    @Override
    protected Expression expression(Enchoer entity, EnchoerRenderState state) {
        int st = entity.getState();
        return Expression.pick(entity, false, st == Enchoer.DANCING || st == Enchoer.PRESENTING, st == Enchoer.SLEEPING);
    }

    @Override
    protected float bounciness() {
        return 0.5F;
    }

    /** Its antlers and nose reach well past the hitbox. */
    @Override
    protected AABB getBoundingBoxForCulling(Enchoer entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(0.8, 0.8, 0.8);
    }

    @Override
    public EnchoerRenderState createRenderState() {
        return new EnchoerRenderState();
    }

    @Override
    public void extractRenderState(Enchoer entity, EnchoerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.air = Mth.lerp(partialTicks, entity.airO, entity.air);
        state.dance = Mth.lerp(partialTicks, entity.danceO, entity.dance);
        state.listen = Mth.lerp(partialTicks, entity.listenO, entity.listen);
        state.sleep = Mth.lerp(partialTicks, entity.sleepO, entity.sleep);
        state.present = Mth.lerp(partialTicks, entity.presentO, entity.present);
        state.voice = entity.voice.get(partialTicks);
        state.bow.copyFrom(entity.bowAnimation);
        state.nod.copyFrom(entity.nodAnimation);
        state.seed = (entity.getId() * 37) % 210;
    }
}
