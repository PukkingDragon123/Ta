package com.thesift.client.renderer;

import com.thesift.TheSift;
import com.thesift.client.CaveCreaturesClient;
import com.thesift.client.model.Anim;
import com.thesift.client.model.CypoleModel;
import com.thesift.client.renderer.state.CypoleRenderState;
import com.thesift.entity.swamp.Cypole;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Cypole: its golden eye, sculk warts and the knots in its plates glow (brighter when it is
 * angry, flaring as the plates crash), and its tongue is aimed here at whatever it stuck to.
 */
public class CypoleRenderer extends SiftMobRenderer<Cypole, CypoleRenderState, CypoleModel> {
    private static final Identifier TEXTURE = TheSift.id("textures/entity/cypole/cypole.png");
    private static final Identifier GLOW = TheSift.id("textures/entity/cypole/cypole_glow.png");

    public CypoleRenderer(EntityRendererProvider.Context context) {
        super(context, new CypoleModel(context.bakeLayer(CaveCreaturesClient.CYPOLE)), 0.5F);
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW, (s, age) -> {
            float c = Anim.seconds(s.crash, age);
            float flash = c >= 0.0F && c < 0.6F ? 1.0F - c / 0.6F : 0.0F;
            float base = s.angry ? 0.85F + 0.15F * Mth.sin(age * 0.35F) : 0.6F + 0.25F * Math.max(0.0F, Mth.sin(age * 0.05F + s.seed));
            return Math.min(1.0F, base + 0.5F * flash);
        }, this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(CypoleRenderState state) {
        return TEXTURE;
    }

    @Override
    public CypoleRenderState createRenderState() {
        return new CypoleRenderState();
    }

    @Override
    protected float bounciness() {
        return 0.6F;
    }

    @Override
    public void extractRenderState(Cypole entity, CypoleRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.action = entity.getAction();
        state.angry = entity.isAngry();
        state.onGround = entity.onGround();
        state.afloat = entity.isInFluidType() && !entity.onGround();
        state.crouch = Mth.lerp(partialTicks, entity.crouchO, entity.crouch);
        state.air = Mth.lerp(partialTicks, entity.airO, entity.air);
        state.squash = entity.squash.get(partialTicks);
        state.ring = entity.ring.get(partialTicks);
        state.blink = entity.blinkTicks > 0 ? Mth.sin(Mth.clamp((entity.blinkTicks - partialTicks) / 6.0F, 0.0F, 1.0F) * Mth.PI) : 0.0F;
        state.seed = (entity.getId() * 29) % 113;
        state.croak.copyFrom(entity.croakAnimation);
        state.clash.copyFrom(entity.clashAnimation);
        state.crash.copyFrom(entity.crashAnimation);
        state.tongue.copyFrom(entity.tongueAnimation);
        state.warn.copyFrom(entity.warnAnimation);
        this.extractTongue(entity, state, partialTicks);
    }

    /** How far out the tongue is, and where its tip goes - in the model's own space (units, y down, facing -z). */
    private void extractTongue(Cypole entity, CypoleRenderState state, float partialTicks) {
        state.tongueOut = 0.0F;
        if (!entity.tongueAnimation.isStarted()) {
            return;
        }
        float ticks = Anim.seconds(entity.tongueAnimation, state.ageInTicks) * 20.0F;
        float out = Mth.clamp((ticks - Cypole.TONGUE_FIRE) / Cypole.TONGUE_FLY, 0.0F, 1.0F);
        if (entity.tongueBack >= 0) {
            out *= 1.0F - Mth.clamp((entity.tickCount + partialTicks - entity.tongueBack) / 4.0F, 0.0F, 1.0F);
        }
        if (out <= 0.0F) {
            return;
        }
        Vec3 self = entity.getPosition(partialTicks);
        Vec3 to;
        Entity stuck = entity.tongueTarget();
        Vec3 aim = entity.tongueAim();
        if (stuck != null) {
            to = stuck.getPosition(partialTicks).add(0.0, stuck.getBbHeight() * 0.55, 0.0);
        } else if (aim != null) {
            to = aim;
        } else {
            to = self.add(Vec3.directionFromRotation(0.0F, state.bodyRot).scale(4.5)).add(0.0, 0.45, 0.0); // the Codex: straight ahead
        }
        Vec3 d = to.subtract(self);
        float yaw = state.bodyRot * Mth.DEG_TO_RAD;
        float cos = Mth.cos(yaw);
        float sin = Mth.sin(yaw);
        state.tongueX = (float) (d.x * cos + d.z * sin) * 16.0F;
        state.tongueY = 24.0F - (float) d.y * 16.0F;
        state.tongueZ = (float) (d.x * sin - d.z * cos) * 16.0F;
        state.tongueOut = out;
    }

    /** The tongue reaches far beyond its own box. */
    @Override
    protected AABB getBoundingBoxForCulling(Cypole entity, float partialTicks) {
        AABB box = super.getBoundingBoxForCulling(entity, partialTicks);
        return entity.getAction() == Cypole.TONGUE ? box.inflate(Cypole.TONGUE_RANGE, 2.0, Cypole.TONGUE_RANGE) : box.inflate(0.3);
    }
}
