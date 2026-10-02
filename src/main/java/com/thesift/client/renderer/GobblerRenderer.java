package com.thesift.client.renderer;

import com.thesift.client.model.GobblerModel;
import com.thesift.client.renderer.state.SiftFishRenderState;
import com.thesift.entity.Gobbler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Gobbler: the music-fish renderer (glowing souls and lines pulse on their own layer), scaled up. */
public class GobblerRenderer extends SiftFishRenderer<Gobbler, GobblerModel> {
    public GobblerRenderer(EntityRendererProvider.Context context, GobblerModel model) {
        super(context, model, "gobbler", 1.0F, 1.55F);
    }

    @Override
    public void extractRenderState(Gobbler entity, SiftFishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.lunge.copyFrom(entity.lungeAnimation);
        state.gulp.copyFrom(entity.gulpAnimation);
        state.spit.copyFrom(entity.spitAnimation);
        state.calm = entity.isCalm();
        state.hunting = entity.isAggressive();
    }
}
