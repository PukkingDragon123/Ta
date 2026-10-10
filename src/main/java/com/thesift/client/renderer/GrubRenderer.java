package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.DunesClient;
import com.thesift.client.model.GrubModel;
import com.thesift.client.renderer.state.GrubRenderState;
import com.thesift.entity.dunes.Grub;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** The Grub, in its gem colour: the gem glints faintly through the cracks of its shell, and shines once it is free. */
public class GrubRenderer extends SiftMobRenderer<Grub, GrubRenderState, GrubModel> {
    private static final String[] GEMS = {"amethyst", "emerald", "diamond", "prism"};
    private static final Identifier[] TEXTURES = new Identifier[4];
    private static final Identifier[] GLOWS = new Identifier[4];

    static {
        for (int i = 0; i < 4; i++) {
            TEXTURES[i] = TheSift.id("textures/entity/grub/grub_" + GEMS[i] + ".png");
            GLOWS[i] = TheSift.id("textures/entity/grub/grub_" + GEMS[i] + "_glow.png");
        }
    }

    public GrubRenderer(EntityRendererProvider.Context context) {
        super(context, new GrubModel(context.bakeLayer(DunesClient.GRUB)), 0.35F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOWS[s.gem], (s, age) -> s.cracked
                ? 0.7F + 0.3F * Mth.sin((age + s.seed) * 0.15F)
                : 0.1F + 0.1F * s.crackStage + 0.05F * Mth.sin((age + s.seed) * 0.2F), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(GrubRenderState state) {
        return TEXTURES[Mth.clamp(state.gem, 0, 3)];
    }

    @Override
    public GrubRenderState createRenderState() {
        return new GrubRenderState();
    }

    @Override
    public void extractRenderState(Grub entity, GrubRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.gem = entity.getGem();
        state.crackStage = entity.crackStage();
        state.cracked = entity.isCracked();
        state.seed = (entity.getId() * 13) % 71;
        state.creak.copyFrom(entity.creakAnimation);
        state.burst.copyFrom(entity.burstAnimation);
        state.drink.copyFrom(entity.drinkAnimation);
    }
}
