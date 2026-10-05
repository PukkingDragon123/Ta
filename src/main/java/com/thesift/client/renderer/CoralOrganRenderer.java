package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.Expression;
import com.thesift.client.model.Anim;
import com.thesift.client.model.CoralOrganModel;
import com.thesift.client.renderer.state.CoralOrganRenderState;
import com.thesift.entity.CoralOrgan;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * CR3 Fish &amp; Coral Organs: the Sculk Coral Organ. Its glowing rims, polyps and veins shine on their own
 * layer, brightening with every chord and blazing while it charges a shot; the horn's aim comes from the
 * target the organ syncs ({@link CoralOrgan#aimTarget()}).
 */
public class CoralOrganRenderer extends SiftMobRenderer<CoralOrgan, CoralOrganRenderState, CoralOrganModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/coral_organ/coral_organ.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/coral_organ/coral_organ_glow.png");

    public CoralOrganRenderer(EntityRendererProvider.Context context, CoralOrganModel model) {
        super(context, model, 0.9F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> glow(s), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    private static float glow(CoralOrganRenderState s) {
        float g = 0.55F + 0.15F * Mth.sin(s.ageInTicks * 0.08F + s.seed);
        float chord = Anim.seconds(s.chord, s.ageInTicks);
        if (chord >= 0.0F && chord < 2.2F) {
            g = Math.max(g, 0.55F + 0.45F * Anim.envelope(chord, 0.0F, 0.1F, 0.8F, 1.0F));
        }
        float charge = Anim.seconds(s.charge, s.ageInTicks);
        if (charge >= 0.0F && charge < 1.5F) {
            g = Math.max(g, 0.6F + 0.4F * Anim.smooth(charge / 1.2F) * (0.75F + 0.25F * Mth.sin(s.ageInTicks * 2.0F)));
        }
        return Mth.clamp(g, 0.0F, 1.0F);
    }

    @Override
    protected float bounciness() {
        return 0.35F;
    }

    @Override
    protected Expression expression(CoralOrgan entity, CoralOrganRenderState state) {
        return Expression.NEUTRAL;
    }

    @Override
    public Identifier getTextureLocation(CoralOrganRenderState state) {
        return TEXTURE;
    }

    @Override
    public CoralOrganRenderState createRenderState() {
        return new CoralOrganRenderState();
    }

    @Override
    public void extractRenderState(CoralOrgan entity, CoralOrganRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.chord.copyFrom(entity.chordAnimation);
        state.charge.copyFrom(entity.chargeAnimation);
        state.fire.copyFrom(entity.fireAnimation);
        state.clamp.copyFrom(entity.clampAnimation);
        state.seed = (entity.getId() * 37) % 101;
        Entity target = entity.aimTarget();
        state.aiming = target != null;
        if (target != null) {
            Vec3 mouth = entity.getPosition(partialTicks).add(0.0, 0.62, 0.0);
            Vec3 to = target.getPosition(partialTicks).add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(mouth);
            float want = (float) (Mth.atan2(to.z, to.x) * Mth.RAD_TO_DEG) - 90.0F;
            float body = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
            state.aimYaw = Mth.wrapDegrees(want - body) * Mth.DEG_TO_RAD;
            state.aimPitch = (float) Mth.atan2(to.y, Math.sqrt(to.x * to.x + to.z * to.z));
        }
    }
}
