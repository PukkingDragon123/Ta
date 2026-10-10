package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.CaveCreaturesClient;
import com.thesift.client.model.Anim;
import com.thesift.client.model.JailerModel;
import com.thesift.client.renderer.state.JailerRenderState;
import com.thesift.entity.cave.Jailer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/** The Jailer (CAVE v4): its heart of soul light, its veins, tendril tips and Sculkite crystals glow, pulsing faster while it hunts. */
public class JailerRenderer extends SiftMobRenderer<Jailer, JailerRenderState, JailerModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/jailer/jailer.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/jailer/jailer_glow.png");

    public JailerRenderer(EntityRendererProvider.Context context) {
        super(context, new JailerModel(context.bakeLayer(CaveCreaturesClient.JAILER)), 0.8F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, JailerRenderer::glow, this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    /**
     * How bright its soul, tendrils and the sculk on its cell glow. Carrying someone, the glow beats
     * with its grip: dim while it holds tight, flaring as it loosens - the prisoner's cue to strike.
     */
    private static float glow(JailerRenderState s, float age) {
        if (s.carrying) {
            float t = Anim.seconds(s.loosen, age);
            float cue = Jailer.CUE_TICKS / 20.0F;
            float loose = t < 0.0F ? 0.0F : Anim.envelope(t, 0.0F, cue, Jailer.LOOSE_TICKS / 20.0F, 0.2F);
            return 0.22F + 0.78F * loose;
        }
        return s.mode == Jailer.HUNTING || s.mode == Jailer.SLAMMING
                ? 0.8F + 0.2F * Mth.sin(age * 0.45F)
                : 0.55F + 0.3F * Math.max(0.0F, Mth.sin(age * 0.09F + s.seed));
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
        state.loosen.copyFrom(entity.loosenAnimation);
        state.heave.copyFrom(entity.heaveAnimation);
        state.kick.copyFrom(entity.kickAnimation);
    }

    /** The cell reaches well outside the Jailer's own box - and a prisoner looks out from inside it. */
    @Override
    protected AABB getBoundingBoxForCulling(Jailer entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(2.2, 0.6, 2.2);
    }
}
