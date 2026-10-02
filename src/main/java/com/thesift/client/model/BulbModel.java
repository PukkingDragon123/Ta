package com.thesift.client.model;

import com.thesift.client.renderer.state.BulbRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Bulb: a jelly cube on four stubby feet with two tall ears and a pom-pom tail. Everything hangs
 * off "body", pivoted at the feet, so squash and stretch deforms the whole jelly from the ground
 * up. Each ear is a two-segment spring that lags behind the body.
 *
 * <p>The jelly is see-through, so the model is drawn twice, like vanilla's slime: once opaque with
 * only the darker {@code core} showing ({@link Pass#CORE}), then translucent with only the jelly
 * ({@link Pass#JELLY}, see {@code BulbJellyLayer}). The core copies the body's squash so it wobbles
 * along inside.</p>
 *
 * <ul>
 *   <li>hop: squats down first (anticipation), stretches tall on take-off with the ears trailing,
 *   splats flat on landing and the ears whip on through (follow-through, from the springs)</li>
 *   <li>sniff: leans in, the front of the jelly twitches, ears pricked forward</li>
 *   <li>groom: sits back and pulls one ear down, rubbing it with both front paws</li>
 *   <li>sleep: curled into a low dome, ears laid flat along its back, paws and tail tucked in</li>
 *   <li>wiggle (fed): a happy shimmy from side to side with a wagging tail</li>
 *   <li>music: every note gives a squash-and-hop, and while the music lasts it sways to the beat</li>
 *   <li>hurt: squishes flat, ears flop; death: the cartoon pop of SiftMobRenderer</li>
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
    private final ModelPart tail;
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
        this.tail = this.body.getChild("tail");
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
        float sleep = s.sleepy ? 1.0F : 0.0F;
        float hurt = s.hasRedOverlay && s.dying <= 0.0F ? 1.0F : 0.0F;

        // --- squash & stretch (the springs carry the crouch, the take-off stretch and the landing splat)
        float sq = Mth.clamp(s.squash, -0.45F, 0.6F);
        float y = 1.0F + sq - hurt * 0.3F - sleep * 0.2F;
        float wide = 1.0F - sq * 0.55F + hurt * 0.18F + sleep * 0.1F;
        this.body.yScale = y;
        this.body.xScale = wide;
        this.body.zScale = wide;
        // a little roll from side to side while hopping along, and it turns a touch towards what it watches
        this.body.zRot = Mth.sin(pos * 0.6F) * 0.06F * walk;
        this.body.yRot = s.yRot * Anim.DEG * 0.25F * (1.0F - sleep);

        // --- ears: springy two-segment wobble, perk up near players; now and then one flicks
        float perk = s.earPerk * (1.0F - sleep);
        float flop = Math.max(hurt, 0.0F);
        float flick = Anim.envelope(Mth.positiveModulo(age + s.variant * 17.0F, 130.0F), 0.0F, 2.0F, 1.0F, 4.0F) * (1.0F - sleep);
        // the ears lag behind the body's roll, then swing past it (follow-through)
        float lag = -Mth.sin(pos * 0.6F - 0.9F) * 0.1F * walk;
        this.leftEar.xRot = s.earLeft * 1.1F - perk * 0.15F + flop * 0.9F;
        this.leftEar.zRot = s.earLeft * 0.2F + 0.06F + flop * 0.6F + lag;
        this.leftEarTip.xRot = s.earLeft * 0.9F + (1.0F - perk) * 0.3F + flop * 0.6F;
        this.rightEar.xRot = s.earRight * 1.1F - perk * 0.15F + flop * 0.8F;
        this.rightEar.zRot = -s.earRight * 0.2F - 0.06F - flop * 0.6F + lag;
        this.rightEarTip.xRot = s.earRight * 0.9F + (1.0F - perk) * 0.3F + flop * 0.6F;
        this.leftEarTip.zRot = flick * 0.6F;
        this.tail.yRot = Mth.sin(pos * 0.6F) * 0.2F * walk;

        // --- feet: tucked in the air, pattering on the ground
        float air = s.airborne ? 1.0F : 0.0F;
        float step = Mth.cos(pos * 0.9F) * 0.6F * walk * (1.0F - air);
        this.frontLeftLeg.xRot = -air * 0.6F + step;
        this.frontRightLeg.xRot = -air * 0.6F - step;
        this.backLeftLeg.xRot = air * 0.7F - step;
        this.backRightLeg.xRot = air * 0.7F + step;

        // --- sniffing (1.5 s): leans in, the jelly twitches at the front, ears pricked forward
        float sniffT = Anim.seconds(s.sniff, age);
        if (sniffT >= 0.0F && sniffT < 1.6F) {
            float e = Anim.envelope(sniffT, 0.0F, 0.2F, 1.0F, 0.3F);
            float twitch = Math.max(0.0F, Mth.sin(sniffT * 38.0F)) * e;
            this.body.xRot += 0.14F * e + twitch * 0.03F;
            this.body.zScale *= 1.0F + twitch * 0.04F;
            this.body.yRot += Mth.sin(sniffT * 3.0F) * 0.25F * e;
            this.leftEar.xRot -= 0.3F * e;
            this.rightEar.xRot -= 0.3F * e;
            this.leftEarTip.xRot -= twitch * 0.2F;
            this.rightEarTip.xRot -= twitch * 0.2F;
        }

        // --- grooming (2 s): sits back, pulls an ear down and rubs it with both front paws
        float groomT = Anim.seconds(s.groom, age);
        if (groomT >= 0.0F && groomT < 2.1F) {
            float e = Anim.envelope(groomT, 0.0F, 0.3F, 1.4F, 0.35F);
            float rub = Mth.sin(groomT * 16.0F) * e;
            this.body.xRot -= 0.18F * e;
            this.leftEar.xRot += 1.45F * e;
            this.leftEar.zRot -= 0.25F * e;
            this.leftEarTip.xRot += 0.5F * e + rub * 0.15F;
            this.frontLeftLeg.xRot -= 1.3F * e + rub * 0.3F;
            this.frontRightLeg.xRot -= 1.3F * e - rub * 0.3F;
            this.frontLeftLeg.zRot = -0.3F * e;
            this.frontRightLeg.zRot = 0.3F * e;
        }

        // --- the happy wiggle when it is fed (1.3 s)
        float wiggleT = Anim.seconds(s.wiggle, age);
        if (wiggleT >= 0.0F && wiggleT < 1.4F) {
            float e = Anim.envelope(wiggleT, 0.0F, 0.08F, 0.8F, 0.45F);
            float shimmy = Mth.sin(wiggleT * 22.0F) * e;
            this.body.zRot += shimmy * 0.2F;
            this.body.yRot += Mth.sin(wiggleT * 22.0F + 1.2F) * 0.15F * e;
            this.tail.yRot += shimmy * 0.9F;
            this.leftEar.zRot += shimmy * 0.4F;
            this.rightEar.zRot += shimmy * 0.4F;
            this.leftEarTip.zRot -= shimmy * 0.3F;
        }

        // --- music: sways to the beat while the music lasts, every note a squash-and-hop
        if (s.dancing) {
            float b = Math.min(1.0F, s.beat / 10.0F);
            float sway = Mth.sin(age * 0.45F);
            this.body.zRot += sway * 0.15F;
            this.leftEar.zRot += sway * 0.35F;
            this.rightEar.zRot += sway * 0.35F;
            this.leftEarTip.xRot += Mth.cos(age * 0.45F) * 0.3F;
            this.rightEarTip.xRot -= Mth.cos(age * 0.45F) * 0.3F;
            this.tail.yRot += sway * 0.5F;
            // the paws pump up on each beat
            this.frontLeftLeg.xRot -= (1.0F - b) * 0.8F;
            this.frontRightLeg.xRot -= (1.0F - b) * 0.8F;
        }

        // --- asleep: curled into a low dome, ears laid flat back along its body, paws and tail tucked
        if (sleep > 0.0F) {
            float breath = Mth.sin(age * 0.06F);
            this.body.yScale *= 1.0F + breath * 0.015F;
            this.body.zRot = 0.0F;
            this.leftEar.xRot = -1.35F;
            this.rightEar.xRot = -1.3F;
            this.leftEar.zRot = 0.25F;
            this.rightEar.zRot = -0.25F;
            this.leftEarTip.xRot = -0.25F + breath * 0.04F;
            this.rightEarTip.xRot = -0.2F - breath * 0.04F;
            this.leftEarTip.zRot = 0.0F;
            this.frontLeftLeg.xRot = -1.2F;
            this.frontRightLeg.xRot = -1.2F;
            this.backLeftLeg.xRot = 1.2F;
            this.backRightLeg.xRot = 1.2F;
            this.tail.xScale = this.tail.yScale = this.tail.zScale = 0.8F;
            // the tucked paws no longer hold it up: it sinks onto its belly
            this.body.y += 2.4F;
        }

        // --- the core inside follows the jelly around
        this.core.xScale = this.body.xScale;
        this.core.yScale = this.body.yScale;
        this.core.zScale = this.body.zScale;
        this.core.xRot = this.body.xRot;
        this.core.yRot = this.body.yRot;
        this.core.zRot = this.body.zRot;
        this.core.y = this.body.y;
    }
}
