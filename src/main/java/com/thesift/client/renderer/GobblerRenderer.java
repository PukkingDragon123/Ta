package com.thesift.client.renderer;

import com.thesift.client.model.Anim;
import com.thesift.client.model.GobblerModel;
import com.thesift.client.renderer.state.SiftFishRenderState;
import com.thesift.entity.Gobbler;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.util.Mth;

/**
 * The Gobbler: the music-fish renderer, scaled up. Its veins, photophores and lantern lures glow on
 * their own layer with a slow double heartbeat - quicker and brighter while it hunts, a dim drowse
 * when the Lullaby lulls it, and a flare of light through the whole body as it lunges.
 */
public class GobblerRenderer extends SiftFishRenderer<Gobbler, GobblerModel> {
    private static final float TWO_PI = (float) (Math.PI * 2.0);

    public GobblerRenderer(EntityRendererProvider.Context context, GobblerModel model) {
        super(context, model, "gobbler", 1.0F, 1.0F); // S2: built at its real size (tools/waterfolk.py)
    }

    @Override
    public void extractRenderState(Gobbler entity, SiftFishRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.lunge.copyFrom(entity.lungeAnimation);
        state.gulp.copyFrom(entity.gulpAnimation);
        state.spit.copyFrom(entity.spitAnimation);
        state.calm = entity.isCalm();
        state.hunting = entity.isAggressive();
        state.glowPulse = pulse(state);
    }

    /** The heartbeat: two quick thumps then a rest, each thump lighting the veins up and fading. */
    private static float pulse(SiftFishRenderState s) {
        float age = s.ageInTicks + s.seed * 3.0F;
        float rate = s.calm ? 0.05F : (s.hunting ? 0.2F : 0.11F);
        float phase = age * rate;
        float t = phase - (float) Math.floor(phase / TWO_PI) * TWO_PI;
        float first = (float) Math.exp(-t * 4.0F);
        float second = t > 0.9F ? 0.6F * (float) Math.exp(-(t - 0.9F) * 5.0F) : 0.0F;
        float glow = (s.calm ? 0.3F : 0.5F) + (s.calm ? 0.25F : 0.5F) * Math.min(1.0F, first + second);
        float lunge = Anim.seconds(s.lunge, s.ageInTicks);
        if (lunge >= 0.0F && lunge < 1.4F) {
            glow = Math.max(glow, 0.6F + 0.4F * Anim.envelope(lunge, 0.45F, 0.15F, 0.3F, 0.5F));
        }
        return Mth.clamp(glow, 0.0F, 1.0F);
    }
}
