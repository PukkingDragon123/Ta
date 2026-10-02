package com.thesift.client.model;

import com.thesift.client.renderer.state.BulbRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Bulb: a jelly cube on four stubby feet with two tall ears. Everything hangs off "body", pivoted
 * at the feet, so squash and stretch deforms the whole jelly from the ground up. Each ear is a
 * two-segment spring that lags behind the body.
 *
 * <p>The jelly is see-through, so the model is drawn twice, like vanilla's slime: once opaque with
 * only the darker {@code core} showing ({@link Pass#CORE}), then translucent with only the jelly
 * ({@link Pass#JELLY}, see {@code BulbJellyLayer}). The core copies the body's squash so it wobbles
 * along inside.</p>
 *
 * <ul>
 *   <li>idle: slow jelly breathing, ears sway and perk up when a player is near, the odd ear flick</li>
 *   <li>move: stretch on take-off, splat on landing, feet tuck in the air and patter on the ground</li>
 *   <li>happy (dancing): bops to the beat, ears swing</li>
 *   <li>hurt: squishes flat, ears flop; death: the cartoon pop of SiftMobRenderer</li>
 *   <li>sleepy: sinks a little lower, ears droop, breathes slowly</li>
 * </ul>
 */
public class BulbModel extends EntityModel<BulbRenderState> {
    public enum Pass { CORE, JELLY }

    private final ModelPart core;
    private final ModelPart body;
    private final ModelPart leftEar;
    private final ModelPart leftEarTip;
    private final ModelPart rightEar;
    private final ModelPart rightEarTip;
    private final ModelPart frontLeftLeg;
    private final ModelPart frontRightLeg;
    private final ModelPart backLeftLeg;
    private final ModelPart backRightLeg;

    public BulbModel(ModelPart root, Pass pass) {
        super(root, pass == Pass.JELLY ? net.minecraft.client.renderer.rendertype.RenderTypes::entityTranslucent
                : net.minecraft.client.renderer.rendertype.RenderTypes::entityCutout);
        this.body = root.getChild("body");
        this.core = root.getChild("core");
        this.body.visible = pass == Pass.JELLY;
        this.core.visible = pass == Pass.CORE;
        this.leftEar = this.body.getChild("left_ear");
        this.leftEarTip = this.leftEar.getChild("left_ear_tip");
        this.rightEar = this.body.getChild("right_ear");
        this.rightEarTip = this.rightEar.getChild("right_ear_tip");
        this.frontLeftLeg = this.body.getChild("front_left_leg");
        this.frontRightLeg = this.body.getChild("front_right_leg");
        this.backLeftLeg = this.body.getChild("back_left_leg");
        this.backRightLeg = this.body.getChild("back_right_leg");
    }

    @Override
    public void setupAnim(BulbRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos;

        // --- squash & stretch
        float sq = Mth.clamp(s.squash, -0.45F, 0.6F);
        float sleepy = s.sleepy ? 1.0F : 0.0F;
        float hurt = s.hasRedOverlay && s.dying <= 0.0F ? 1.0F : 0.0F;
        float y = 1.0F + sq - hurt * 0.3F - sleepy * 0.08F;
        float wide = 1.0F - sq * 0.55F + hurt * 0.18F + sleepy * 0.05F;
        this.body.yScale = y;
        this.body.xScale = wide;
        this.body.zScale = wide;
        // a little wobble from side to side while hopping along
        this.body.zRot = Mth.sin(pos * 0.6F) * 0.06F * walk;
        // the whole jelly turns a touch towards what it looks at
        this.body.yRot = s.yRot * Anim.DEG * 0.25F;
        float melt = 0.0F;

        // --- ears: springy two-segment wobble, perk up near players; now and then one flicks
        float perk = s.earPerk * (1.0F - sleepy);
        float flop = Math.max(hurt, Math.max(melt, sleepy * 0.6F));
        float flick = Anim.envelope(Mth.positiveModulo(age + s.variant * 17.0F, 130.0F), 0.0F, 2.0F, 1.0F, 4.0F);
        this.leftEar.xRot = s.earLeft * 1.1F - perk * 0.15F + Mth.sin(age * 0.08F) * 0.05F + flop * 0.9F;
        this.leftEar.zRot = s.earLeft * 0.2F + 0.06F + flop * 0.6F;
        this.leftEarTip.xRot = s.earLeft * 0.9F + Mth.sin(age * 0.08F - 0.8F) * 0.07F + (1.0F - perk) * 0.3F + flop * 0.6F;
        this.rightEar.xRot = s.earRight * 1.1F - perk * 0.15F + Mth.sin(age * 0.08F + 1.7F) * 0.05F + flop * 0.8F;
        this.rightEar.zRot = -s.earRight * 0.2F - 0.06F - flop * 0.6F;
        this.rightEarTip.xRot = s.earRight * 0.9F + Mth.sin(age * 0.08F + 0.9F) * 0.07F + (1.0F - perk) * 0.3F + flop * 0.6F;
        this.leftEarTip.zRot = flick * 0.6F;

        // --- dancing: the whole bunny bops to the beat
        if (s.dancing) {
            float beat = Mth.sin(age * 0.55F);
            this.body.zRot += beat * 0.18F;
            this.body.yScale *= 1.0F + Math.abs(beat) * 0.12F;
            this.leftEar.zRot += beat * 0.4F;
            this.rightEar.zRot += beat * 0.4F;
            this.leftEarTip.xRot += Mth.cos(age * 0.55F) * 0.3F;
            this.rightEarTip.xRot -= Mth.cos(age * 0.55F) * 0.3F;
        }

        // --- feet: tucked in the air, pattering on the ground
        float air = s.airborne ? 1.0F : 0.0F;
        float step = Mth.cos(pos * 0.9F) * 0.6F * walk * (1.0F - air);
        this.frontLeftLeg.xRot = -air * 0.6F + step;
        this.frontRightLeg.xRot = -air * 0.6F - step;
        this.backLeftLeg.xRot = air * 0.7F - step;
        this.backRightLeg.xRot = air * 0.7F + step;

        // --- the core inside follows the jelly around
        this.core.xScale = this.body.xScale;
        this.core.yScale = this.body.yScale;
        this.core.zScale = this.body.zScale;
        this.core.xRot = this.body.xRot;
        this.core.yRot = this.body.yRot;
        this.core.zRot = this.body.zRot;
    }
}
