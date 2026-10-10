package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.DunesClient;
import com.thesift.client.model.ReservoirModel;
import com.thesift.client.renderer.state.ReservoirRenderState;
import com.thesift.entity.dunes.Reservoir;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;

/**
 * The Reservoir and the Monarch Reservoir: the fuller of Chrome, the more vivid its stripes (an empty one is dull and
 * greyed) and the brighter the Chrome glows through its veins and in its mouth, pulsing faster. The Monarch is the
 * same creature drawn 4.4 times as big, in its own rose-gold and sapphire.
 */
public class ReservoirRenderer extends SiftMobRenderer<Reservoir, ReservoirRenderState, ReservoirModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/reservoir/reservoir.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/reservoir/reservoir_glow.png");
    private static final Identifier MONARCH = TheSift.id("textures/entity/reservoir/reservoir_monarch.png");
    private static final Identifier MONARCH_GLOW = TheSift.id("textures/entity/reservoir/reservoir_monarch_glow.png");
    public static final float MONARCH_SCALE = 4.4F;

    public ReservoirRenderer(EntityRendererProvider.Context context, boolean monarch) {
        super(context, new ReservoirModel(context.bakeLayer(DunesClient.RESERVOIR)), monarch ? 1.9F : 0.5F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> s.monarch ? MONARCH_GLOW : GLOW, (s, age) -> {
            float beat = Mth.sin((age + s.seed) * (0.08F + 0.14F * s.fill)) * 0.5F + 0.5F;
            return Mth.clamp(0.12F + 0.7F * s.fill + 0.18F * beat * s.fill, 0.0F, 1.0F);
        }, this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(ReservoirRenderState state) {
        return state.monarch ? MONARCH : TEXTURE;
    }

    @Override
    public ReservoirRenderState createRenderState() {
        return new ReservoirRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.3F;
    }

    @Override
    protected void scale(ReservoirRenderState state, PoseStack poseStack) {
        super.scale(state, poseStack);
        if (state.monarch) {
            poseStack.scale(MONARCH_SCALE, MONARCH_SCALE, MONARCH_SCALE);
        }
    }

    /** Empty, its colours fade to a dull lilac-grey; full, they are vivid. */
    @Override
    protected int getModelTint(ReservoirRenderState state) {
        float k = 0.55F + 0.45F * state.fill;
        int c = ARGB.color(255, (int) (255 * (0.78F + 0.22F * k)), (int) (255 * (0.72F + 0.28F * k)), (int) (255 * (0.8F + 0.2F * k)));
        return ARGB.multiply(super.getModelTint(state), c);
    }

    @Override
    public void extractRenderState(Reservoir entity, ReservoirRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.fill = entity.fill();
        state.bloom = Mth.lerp(partialTicks, entity.bloomO, entity.bloom);
        state.monarch = entity.isMonarch();
        state.seed = (entity.getId() * 31) % 97;
        state.burst.copyFrom(entity.burstAnimation);
        state.gulp.copyFrom(entity.gulpAnimation);
    }
}
