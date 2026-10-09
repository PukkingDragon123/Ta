package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.EchoerClient;
import com.thesift.client.model.NibModel;
import com.thesift.client.renderer.state.NibRenderState;
import com.thesift.entity.Nib;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** M3: a Nib - see-through wings, its tail and antenna tips glowing like glow-dust and twinkling. */
public class NibRenderer extends SiftMobRenderer<Nib, NibRenderState, NibModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/nib/nib.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/nib/nib_glow.png");

    public NibRenderer(EntityRendererProvider.Context context) {
        super(context, new NibModel(context.bakeLayer(EchoerClient.NIB)), 0.1F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW,
                (s, age) -> Mth.clamp(0.7F + 0.25F * Mth.sin(age * 0.4F + s.phase * 6.0F) + 0.3F * s.swirl, 0.0F, 1.0F),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(NibRenderState state) {
        return TEXTURE;
    }

    @Override
    public NibRenderState createRenderState() {
        return new NibRenderState();
    }

    @Override
    public void extractRenderState(Nib entity, NibRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.state = entity.getState();
        state.phase = entity.phase();
        state.rest = Mth.lerp(partialTicks, entity.restO, entity.rest);
        state.swirl = Mth.lerp(partialTicks, entity.swirlO, entity.swirl);
    }
}
