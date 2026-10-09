package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.Anim;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SlumblerModel;
import com.thesift.client.renderer.state.SlumblerRenderState;
import com.thesift.entity.Slumbler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Slumbler: its painted face per mood, a warm light in the f-holes of its wooden back (brighter
 * asleep, brightest while it hums along to music), and the shimmer of its rainbow scales - every
 * few seconds a glint of light sweeps along its body from snout to tail (frames painted by
 * tools/slumbler.py), more often while it hums.
 */
public class SlumblerRenderer extends SiftMobRenderer<Slumbler, SlumblerRenderState, SlumblerModel> {
    private static final ExpressionTextures TEXTURES = ExpressionTextures.single("slumbler", com.thesift.client.Expression.ANGRY,
            com.thesift.client.Expression.HURT, com.thesift.client.Expression.DEAD);
    private static final Identifier GLOW = TheSift.id("textures/entity/slumbler/slumbler_glow.png");
    /** The sweep of light along the scales: SHIMMER_FRAMES frames, two ticks each, once every SHIMMER_PERIOD ticks. */
    private static final int SHIMMER_FRAMES = 12;
    private static final int SHIMMER_PERIOD = 110;
    private static final Identifier[] SHIMMER = new Identifier[SHIMMER_FRAMES];

    static {
        for (int i = 0; i < SHIMMER_FRAMES; i++) {
            SHIMMER[i] = TheSift.id("textures/entity/slumbler/slumbler_shimmer_" + i + ".png");
        }
    }

    public SlumblerRenderer(EntityRendererProvider.Context context) {
        super(context, new SlumblerModel(context.bakeLayer(ModModelLayers.SLUMBLER)), 1.0F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> {
            float hum = Anim.seconds(s.hum, age);
            float singing = hum >= 0.0F && hum < 1.6F ? Anim.envelope(hum, 0.0F, 0.15F, 0.9F, 0.5F) : 0.0F;
            float rest = (s.sleeping ? 0.45F : 0.25F) + 0.25F * Mth.sin(age * 0.05F);
            return Math.min(1.0F, rest + 0.6F * singing);
        }, this.model, RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> SHIMMER[Math.max(0, shimmerFrame(s))],
                (s, age) -> shimmerFrame(s) < 0 ? 0.0F : 0.75F, this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    /** The shimmer frame to show now, or -1 between sweeps (sweeps come twice as often while it hums). */
    private static int shimmerFrame(SlumblerRenderState s) {
        if (s.sleeping && s.inChrome) {
            return -1;
        }
        float hum = Anim.seconds(s.hum, s.ageInTicks);
        int period = hum >= 0.0F && hum < 1.6F ? SHIMMER_PERIOD / 2 : SHIMMER_PERIOD;
        int t = Math.floorMod((int) s.ageInTicks + s.seed * 7, period);
        return t < SHIMMER_FRAMES * 2 ? t / 2 : -1;
    }

    @Override
    public Identifier getTextureLocation(SlumblerRenderState state) {
        return TEXTURES.get(state.expression);
    }

    @Override
    protected float bounciness() {
        return 0.6F;
    }

    @Override
    public SlumblerRenderState createRenderState() {
        return new SlumblerRenderState();
    }

    @Override
    public void extractRenderState(Slumbler entity, SlumblerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.sleeping = entity.isSlumbering();
        state.yawn.copyFrom(entity.yawnAnimation);
        state.bite.copyFrom(entity.biteAnimation);
        state.gulp.copyFrom(entity.gulpAnimation);
        state.nuzzle.copyFrom(entity.nuzzleAnimation);
        state.hum.copyFrom(entity.humAnimation);
        state.shake.copyFrom(entity.shakeAnimation);
        state.spit.copyFrom(entity.spitAnimation);
        state.lay.copyFrom(entity.layAnimation);
        state.inChrome = entity.isInFluidType();
        state.seed = entity.getId() * 37 % 101;
    }
}
