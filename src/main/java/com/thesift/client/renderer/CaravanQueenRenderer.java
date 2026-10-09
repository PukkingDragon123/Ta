package com.thesift.client.renderer;

import com.thesift.client.CaravansClient;
import com.thesift.client.model.Anim;
import com.thesift.client.model.CaravanQueenModel;
import com.thesift.client.renderer.state.CaravanQueenRenderState;
import com.thesift.entity.caravan.CaravanQueen;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * CR2: the Caravan Queen in her caravans' colour. The gems on her shell and in her body, her eyes
 * and the runes on her face glow - a slow pulse at rest, a flare as she rings her gems in a jam or
 * spits, and a blaze as she swells to burst.
 */
public class CaravanQueenRenderer extends SiftMobRenderer<CaravanQueen, CaravanQueenRenderState, CaravanQueenModel> {
    private static final String[] VARIANTS = {"caravan_queen_amber", "caravan_queen_rose", "caravan_queen_teal", "caravan_queen_violet",
            "caravan_queen_gold"};
    private static final ExpressionTextures TEXTURES = new ExpressionTextures("caravan_queen", VARIANTS);
    private static final ExpressionTextures GLOW = new ExpressionTextures("caravan_queen", VARIANTS, "_glow");

    public CaravanQueenRenderer(EntityRendererProvider.Context context) {
        super(context, new CaravanQueenModel(context.bakeLayer(CaravansClient.CARAVAN_QUEEN)), 1.6F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.variant, s.expression), CaravanQueenRenderer::glow, this.model,
                RenderTypes::entityTranslucentEmissive, false));
    }

    private static float glow(CaravanQueenRenderState s, float age) {
        float rest = (s.calm ? 0.85F : 0.65F) + 0.15F * Mth.sin(age * 0.06F + s.seed);
        float tap = Anim.seconds(s.tap, age);
        float spit = Anim.seconds(s.spit, age);
        float flare = Math.max(tap >= 0.0F ? Anim.envelope(tap, 0.0F, 0.05F, 0.05F, 0.35F) : 0.0F,
                spit >= 0.0F ? Anim.envelope(spit, 0.3F, 0.3F, 0.1F, 0.4F) : 0.0F);
        float burst = s.dying > 0.0F ? Anim.clamp01(s.dying / CaravanQueen.BURST_AT) : 0.0F;
        return Math.min(1.0F, rest + 0.35F * flare + burst);
    }

    @Override
    public Identifier getTextureLocation(CaravanQueenRenderState state) {
        return TEXTURES.get(state.variant, state.expression);
    }

    @Override
    public CaravanQueenRenderState createRenderState() {
        return new CaravanQueenRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.2F;
    }

    /** Her claws and feelers reach well past her shell: keep drawing her while any of her is on screen. */
    @Override
    protected AABB getBoundingBoxForCulling(CaravanQueen entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(1.5, 1.0, 1.5);
    }

    @Override
    public void extractRenderState(CaravanQueen entity, CaravanQueenRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.variant = Mth.clamp(entity.getVariant(), 0, VARIANTS.length - 1);
        state.calm = entity.isCalm();
        state.seed = entity.getId() % 97;
        state.tap.copyFrom(entity.tapAnimation);
        state.spit.copyFrom(entity.spitAnimation);
        state.swat.copyFrom(entity.swatAnimation);
        state.feed.copyFrom(entity.feedAnimation);
        state.warn.copyFrom(entity.warnAnimation);
        state.roar.copyFrom(entity.roarAnimation);
        state.settle.copyFrom(entity.settleAnimation);
    }
}
