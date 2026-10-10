package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.DunesClient;
import com.thesift.client.model.JaberoraModel;
import com.thesift.client.renderer.state.JaberoraRenderState;
import com.thesift.entity.dunes.Jaberora;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * The Jaberora, a jerboa of the dunes in five colours, with massive sparkly eyes and whiskers (drawn at 0.7 scale; babies smaller): hops, arias, its piercing note, naps and rides on
 * a Kerkorer's back - and its long tail shimmers, the sheen running along it and flaring as it flicks.
 */
public class JaberoraRenderer extends SiftMobRenderer<Jaberora, JaberoraRenderState, JaberoraModel> {
    /** Its colours, in the order of Jaberora#getVariant (pink, sand, peach, lilac, snow). */
    private static final String[] VARIANTS = {"jaberora", "jaberora_sand", "jaberora_peach", "jaberora_lilac", "jaberora_snow"};
    private static final Identifier[] TEXTURES = new Identifier[VARIANTS.length];
    private static final Identifier[] GLOWS = new Identifier[VARIANTS.length];

    static {
        for (int i = 0; i < VARIANTS.length; i++) {
            TEXTURES[i] = TheSift.id("textures/entity/jaberora/" + VARIANTS[i] + ".png");
            GLOWS[i] = TheSift.id("textures/entity/jaberora/" + VARIANTS[i] + "_glow.png");
        }
    }
    public static final float SCALE = 0.7F;

    public JaberoraRenderer(EntityRendererProvider.Context context) {
        super(context, new JaberoraModel(context.bakeLayer(DunesClient.JABERORA)), 0.25F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOWS[Mth.clamp(s.variant, 0, GLOWS.length - 1)], (s, age) -> 0.45F + 0.35F * Mth.sin((age + s.seed) * 0.21F)
                + 0.2F * Mth.sin((age + s.seed) * 0.53F), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(JaberoraRenderState state) {
        return TEXTURES[Mth.clamp(state.variant, 0, TEXTURES.length - 1)];
    }

    @Override
    public JaberoraRenderState createRenderState() {
        return new JaberoraRenderState();
    }

    @Override
    protected void scale(JaberoraRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        float k = state.isBaby ? SCALE * 0.6F : SCALE;
        poseStack.scale(k, k, k);
    }

    @Override
    public void extractRenderState(Jaberora entity, JaberoraRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.crouch = Mth.lerp(partialTicks, entity.crouchO, entity.crouch);
        state.air = Mth.lerp(partialTicks, entity.airO, entity.air);
        state.blink = entity.blinkTicks > 0 ? 1.0F : 0.0F;
        state.singing = entity.isSinging();
        state.napping = entity.isNapping();
        state.sitting = entity.isOrderedToSit();
        state.riding = entity.isPassenger();
        state.seed = (entity.getId() * 23) % 89;
        state.variant = entity.getVariant();
        state.pulse.copyFrom(entity.pulseAnimation);
        state.eat.copyFrom(entity.eatAnimation);
        state.sing.copyFrom(entity.singAnimation);
    }
}
