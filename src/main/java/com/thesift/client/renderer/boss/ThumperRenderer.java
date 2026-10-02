package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.client.Expression;
import com.thesift.client.model.boss.ThumperModel;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.client.renderer.state.ThumperRenderState;
import com.thesift.entity.boss.Thumper;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/**
 * The Thumper Titan (agent B2): the mini-boss renderer, plus where its last vent hit landed (the
 * model staggers from it), and no cartoon death pop - the model plays its own long collapse.
 */
public class ThumperRenderer extends MiniBossRenderer<Thumper, ThumperModel> {
    public ThumperRenderer(EntityRendererProvider.Context context, ThumperModel model) {
        super(context, model, "thumper", Thumper.SCALE, 0.8F,
                (e, s) -> s.bossState == Thumper.EXPOSED && e.deathTime == 0 ? Expression.HURT : null);
    }

    @Override
    public MiniBossRenderState createRenderState() {
        return new ThumperRenderState();
    }

    @Override
    public void extractRenderState(Thumper entity, MiniBossRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof ThumperRenderState ts) {
            ts.staggerVent = entity.staggerVent;
            ts.staggerTime = entity.staggerVent < 0 ? 99.0F : entity.tickCount - entity.staggerTick + partialTicks;
        }
    }

    @Override
    protected void scale(MiniBossRenderState state, PoseStack poseStack) {
        if (state.dying > 0.0F) {
            // the collapse is animated by the model; keep only the giant scale
            poseStack.scale(Thumper.SCALE, Thumper.SCALE, Thumper.SCALE);
            return;
        }
        super.scale(state, poseStack);
    }
}
