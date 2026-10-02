package com.thesift.client.renderer.boss;

import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.boss.StrumlingModel;
import com.thesift.client.renderer.state.MinionRenderState;
import com.thesift.client.renderer.state.SculkSpiderRenderState;
import com.thesift.entity.boss.Strumling;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

/** The Sculk Spider: the orchestra's minion renderer, plus whether it is on a wall or mid-pounce. */
public class StrumlingRenderer extends MinionRenderer<Strumling, StrumlingModel> {
    public StrumlingRenderer(EntityRendererProvider.Context context) {
        super(context, new StrumlingModel(context.bakeLayer(ModModelLayers.STRUMLING)), "strumling", 0.7F);
    }

    @Override
    public MinionRenderState createRenderState() {
        return new SculkSpiderRenderState();
    }

    @Override
    public void extractRenderState(Strumling entity, MinionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        if (state instanceof SculkSpiderRenderState s) {
            s.climbing = entity.isClimbing();
            s.airborne = !entity.onGround() && !entity.isInWater();
        }
    }
}
