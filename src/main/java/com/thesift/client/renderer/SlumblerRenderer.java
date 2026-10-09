package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.SlumblerModel;
import com.thesift.client.renderer.state.SlumblerRenderState;
import com.thesift.entity.Slumbler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.Identifier;

/**
 * S2: the Slumbler, one hand-painted texture (tools/slumbler.py); its face lives in the model (the
 * heavy eyelids, the jaw, the cheeks), so there are no expression textures or glow layers.
 */
public class SlumblerRenderer extends SiftMobRenderer<Slumbler, SlumblerRenderState, SlumblerModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/slumbler/slumbler.png");

    public SlumblerRenderer(EntityRendererProvider.Context context) {
        super(context, new SlumblerModel(context.bakeLayer(ModModelLayers.SLUMBLER)), 0.95F);
    }

    @Override
    public Identifier getTextureLocation(SlumblerRenderState state) {
        return TEXTURE;
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
        state.sleep = entity.getSleepAmount(partialTicks);
        state.swim = entity.getSwimAmount(partialTicks);
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
