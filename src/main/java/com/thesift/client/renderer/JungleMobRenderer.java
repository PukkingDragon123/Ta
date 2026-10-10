package com.thesift.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/**
 * P4 Cave Jungle: one renderer for the Cave Jungle's creatures - their texture, an optional glow layer
 * (Lumen Moss, crystals, the Glow Fly's lantern) whose strength the creature sets, a size and how
 * bouncy its hurt squash and death pop are. What each model needs is copied in by an {@link Extractor}.
 */
public class JungleMobRenderer<T extends Mob> extends SiftMobRenderer<T, JungleRenderState, EntityModel<JungleRenderState>> {
    /** Copies what the model animates from the creature into the state (client side, every frame). */
    @FunctionalInterface
    public interface Extractor<T> {
        void fill(T entity, JungleRenderState state, float partialTicks);
    }

    private final Identifier texture;
    private final float size;
    private final float bounce;
    private final Extractor<T> extractor;

    public JungleMobRenderer(EntityRendererProvider.Context context, EntityModel<JungleRenderState> model, String name, boolean glows, float shadow,
            float size, float bounce, Extractor<T> extractor) {
        super(context, model, shadow * size);
        this.texture = TheSift.id("textures/entity/" + name + "/" + name + ".png");
        this.size = size;
        this.bounce = bounce;
        this.extractor = extractor;
        if (glows) {
            Identifier glow = TheSift.id("textures/entity/" + name + "/" + name + "_glow.png");
            this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> glow, (s, age) -> s.glow, this.model, RenderTypes::entityTranslucentEmissive, false));
        }
    }

    @Override
    public Identifier getTextureLocation(JungleRenderState state) {
        return this.texture;
    }

    @Override
    public JungleRenderState createRenderState() {
        return new JungleRenderState();
    }

    @Override
    protected float bounciness() {
        return this.bounce;
    }

    @Override
    public void extractRenderState(T entity, JungleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.seed = (entity.getId() * 37) % 101;
        this.extractor.fill(entity, state, partialTicks);
    }

    @Override
    protected void scale(JungleRenderState state, PoseStack poseStack) {
        float k = this.size * (state.isBaby ? 0.5F : 1.0F);
        if (k != 1.0F) {
            poseStack.scale(k, k, k);
        }
        super.scale(state, poseStack);
    }
}
