package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.CaveCreaturesClient;
import com.thesift.client.model.JailerModel;
import com.thesift.client.renderer.state.JailerRenderState;
import com.thesift.entity.cave.Jailer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** The Jailer: its soul, tendril tips and the sculk on its cell glow, pulsing faster while it hunts. */
public class JailerRenderer extends SiftMobRenderer<Jailer, JailerRenderState, JailerModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/jailer/jailer.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/jailer/jailer_glow.png");

    public JailerRenderer(EntityRendererProvider.Context context) {
        super(context, new JailerModel(context.bakeLayer(CaveCreaturesClient.JAILER)), 0.8F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW,
                (s, age) -> s.mode == Jailer.HUNTING || s.mode == Jailer.SLAMMING
                        ? 0.8F + 0.2F * Mth.sin(age * 0.45F)
                        : 0.55F + 0.3F * Math.max(0.0F, Mth.sin(age * 0.09F + s.seed)),
                this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(JailerRenderState state) {
        return TEXTURE;
    }

    @Override
    public JailerRenderState createRenderState() {
        return new JailerRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.3F;
    }

    @Override
    public void extractRenderState(Jailer entity, JailerRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.mode = entity.getMode();
        state.cage = entity.getCage();
        state.carrying = entity.isCarrying();
        state.stalk = Mth.lerp(partialTicks, entity.stalkO, entity.stalk);
        state.seed = (entity.getId() * 17) % 89;
        state.emerge.copyFrom(entity.emergeAnimation);
        state.slam.copyFrom(entity.slamAnimation);
        state.trap.copyFrom(entity.trapAnimation);
        state.squeeze.copyFrom(entity.squeezeAnimation);
        state.rattle.copyFrom(entity.rattleAnimation);
        state.cageBreak.copyFrom(entity.breakAnimation);
        state.listen.copyFrom(entity.listenAnimation);
    }

    /** The cell reaches well outside the Jailer's own box - and a prisoner looks out from inside it. */
    @Override
    protected AABB getBoundingBoxForCulling(Jailer entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(2.2, 0.6, 2.2);
    }
}
