package com.thesift.client.renderer.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.TheSift;
import com.thesift.client.model.Anim;
import com.thesift.client.model.ModModelLayers;
import com.thesift.client.model.boss.DictatorKaijuModel;
import com.thesift.client.model.boss.DictatorModel;
import com.thesift.client.renderer.SiftMobRenderer;
import com.thesift.client.renderer.state.DictatorRenderState;
import com.thesift.entity.boss.Dictator;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

/**
 * The Conductor, and (C3) the colossus he swells into for his last movement: two models, the
 * renderer drawing whichever he is this frame - the man while he swells, the colossus from the
 * moment his mask splits.
 */
public class DictatorRenderer extends SiftMobRenderer<Dictator, DictatorRenderState, EntityModel<DictatorRenderState>> {
    private static final com.thesift.client.Expression[] PAINTED = {com.thesift.client.Expression.ANGRY, com.thesift.client.Expression.HURT, com.thesift.client.Expression.DEAD};
    private static final com.thesift.client.renderer.ExpressionTextures TEXTURES = com.thesift.client.renderer.ExpressionTextures.single("dictator", PAINTED);
    private static final com.thesift.client.renderer.ExpressionTextures GLOW = new com.thesift.client.renderer.ExpressionTextures("dictator",
            new String[]{"dictator"}, "_glow", PAINTED);
    private static final Identifier KAIJU = TheSift.id("textures/entity/dictator_kaiju/dictator_kaiju.png");
    private static final Identifier KAIJU_GLOW = TheSift.id("textures/entity/dictator_kaiju/dictator_kaiju_glow.png");
    /** He stands half as tall again as his model (about six blocks to the tips of his horn-ears). */
    private static final float MAN = 1.5F;
    /** The colossus model drawn to fill his colossus hitbox (about eleven blocks to the pipes). */
    private static final float COLOSSUS = 2.15F;
    private static final float BURST = Dictator.KAIJU_BURST / (float) Dictator.KAIJU_TICKS;

    private final DictatorModel man;
    private final DictatorKaijuModel colossus;

    public DictatorRenderer(EntityRendererProvider.Context context) {
        super(context, new DictatorModel(context.bakeLayer(ModModelLayers.DICTATOR)), 1.0F);
        this.man = (DictatorModel) this.model;
        this.colossus = new DictatorKaijuModel(context.bakeLayer(ModModelLayers.DICTATOR_KAIJU));
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> GLOW.get(s.expression),
                (s, age) -> s.kaiju ? 0.0F : Math.min(1.0F, 0.55F + s.phase * 0.12F + 0.25F * Mth.sin(age * (0.06F + s.phase * 0.04F))), this.man,
                RenderTypes::entityTranslucentEmissive, false));
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> KAIJU_GLOW,
                (s, age) -> s.kaiju ? Math.min(1.0F, 0.8F + 0.2F * Mth.sin(age * 0.11F)) : 0.0F, this.colossus,
                RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public void submit(DictatorRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        this.model = state.kaiju ? this.colossus : this.man;
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(DictatorRenderState state) {
        return state.kaiju ? KAIJU : TEXTURES.get(state.expression);
    }

    /**
     * He grows with every movement; at the last he swells (trembling, ever faster) until the mask
     * splits, and the colossus unfolds out of him.
     */
    @Override
    protected void scale(DictatorRenderState state, PoseStack poseStack) {
        float s;
        if (state.kaiju) {
            s = COLOSSUS;
            if (state.transform >= 0.0F) {
                float e = Anim.clamp01((state.transform - BURST) / 0.3F);
                s *= Mth.lerp(Anim.backOut(e), 0.55F, 1.0F);
            }
        } else {
            s = MAN * (1.0F + Math.min(1, state.phase - 1) * 0.06F);
            if (state.transform >= 0.0F && state.phase == 2) {
                s = Mth.lerp(Anim.smooth((state.transform - 0.45F) / 0.2F), MAN, s);
            }
            if (state.transform >= 0.0F && state.phase == 3) {
                float k = Anim.clamp01(state.transform / BURST);
                float tremble = Mth.sin(state.ageInTicks * (1.5F + k * 2.0F)) * 0.04F * k;
                s *= 1.0F + 0.75F * k * k + tremble;
            } else if (state.transform >= 0.0F) {
                // a pulse of power at the moment of change
                s *= 1.0F + 0.12F * Math.max(0.0F, 1.0F - Math.abs(state.transform - 0.57F) * 8.0F);
            }
        }
        poseStack.scale(s, s, s);
        super.scale(state, poseStack);
    }

    @Override
    protected float bounciness() {
        return 0.35F;
    }

    @Override
    protected float getShadowRadius(DictatorRenderState state) {
        return state.kaiju ? 3.4F : 1.0F;
    }

    @Override
    protected AABB getBoundingBoxForCulling(Dictator entity, float partialTicks) {
        return super.getBoundingBoxForCulling(entity, partialTicks).inflate(entity.isKaiju() ? 3.0 : 1.0, 1.0, entity.isKaiju() ? 3.0 : 1.0);
    }

    @Override
    public DictatorRenderState createRenderState() {
        return new DictatorRenderState();
    }

    @Override
    public void extractRenderState(Dictator entity, DictatorRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.phase = entity.getPhase();
        state.action = entity.getAction();
        state.actionTime = entity.actionTime(partialTicks);
        int tt = entity.transformTicks();
        state.transform = tt > 0 ? 1.0F - (tt - partialTicks) / entity.transformLength() : -1.0F;
        state.kaiju = entity.isKaiju() && (state.transform < 0.0F || state.transform >= BURST);
        int at = entity.assembleTicks();
        state.assemble = at > 0 ? Math.min(1.0F, 1.0F - (at - partialTicks) / Dictator.ASSEMBLE_TICKS) : -1.0F;
        state.blink.copyFrom(entity.blinkAnimation);
        state.summon.copyFrom(entity.summonAnimation);
        state.crescendo.copyFrom(entity.crescendoAnimation);
        state.slash.copyFrom(entity.slashAnimation);
        state.roar.copyFrom(entity.roarAnimation);
        state.slam.copyFrom(entity.slamAnimation);
    }
}
