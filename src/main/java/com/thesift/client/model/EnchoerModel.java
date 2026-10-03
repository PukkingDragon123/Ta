package com.thesift.client.model;

import com.thesift.client.renderer.state.EnchoerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR1 Echoer, the speaker-bat (geometry in tools/echoer.py): a furry loudspeaker body with a woofer
 * in its chest, bat wings with a speaker cone in each, tweeter ears, a spiralling brass drill for a
 * snout and a coiled cable tail with a jack plug. The wingbeat, the drill's spin and the cones' throw
 * come from the entity, so the flap sounds and the rings it sends out land on the beat.
 *
 * <ul>
 *   <li>flight: a bat's stroke - a quick downstroke, the hand trailing the arm (follow-through), the
 *   wings sweeping forward on the way down and back on the way up; the body bobs against them and
 *   leans into its flight, the cable tail streams and swings</li>
 *   <li>idle hover: slower beats, ears twitching one at a time, the drill pointing where it looks</li>
 *   <li>inspect: nose down over the offering, ears forward, the drill ticking round as it pings</li>
 *   <li>waiting: upright and attentive, ears tall; humming: head up, jaw open, cones pumping on the
 *   beat and the drill whirring; dance: twirls and barrel-rolls with its wings flung wide</li>
 *   <li>disappointed: it sags in the air, ears and wings drooping; asleep: wings folded round it like
 *   a cloak, head tucked, ears laid back</li>
 *   <li>bow: a little rise (anticipation), then it tips forward with its wings swept ahead and
 *   settles back (follow-through)</li>
 * </ul>
 */
public class EnchoerModel extends EntityModel<EnchoerRenderState> {
    private final ModelPart body;
    private final ModelPart woofer;
    private final ModelPart wooferCap;
    private final ModelPart tuft;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart drill;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftTweeter;
    private final ModelPart rightTweeter;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftTip;
    private final ModelPart rightTip;
    private final ModelPart[] cones = new ModelPart[4];
    private final ModelPart leftFoot;
    private final ModelPart rightFoot;
    private final ModelPart[] tail = new ModelPart[4];

    public EnchoerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.woofer = this.body.getChild("woofer");
        this.wooferCap = this.woofer.getChild("woofer_cap");
        this.tuft = this.body.getChild("tuft");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.drill = this.head.getChild("drill");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftTweeter = this.leftEar.getChild("left_tweeter");
        this.rightTweeter = this.rightEar.getChild("right_tweeter");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftTip = this.leftWing.getChild("left_wing_tip");
        this.rightTip = this.rightWing.getChild("right_wing_tip");
        this.cones[0] = this.leftWing.getChild("left_wing_cone");
        this.cones[1] = this.rightWing.getChild("right_wing_cone");
        this.cones[2] = this.leftTip.getChild("left_tip_cone");
        this.cones[3] = this.rightTip.getChild("right_tip_cone");
        this.leftFoot = this.body.getChild("left_foot");
        this.rightFoot = this.body.getChild("right_foot");
        this.tail[0] = this.body.getChild("tail");
        this.tail[1] = this.tail[0].getChild("tail_1");
        this.tail[2] = this.tail[1].getChild("tail_2");
        this.tail[3] = this.tail[2].getChild("plug");
    }

    @Override
    public void setupAnim(EnchoerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float awake = 1.0F - s.sleep;
        float fly = Math.min(1.0F, s.speed * 6.0F) * awake;
        float beat = s.beat * awake;
        float f = s.flap;
        // a bat's stroke: a quick downstroke and a slower upstroke; the hand follows the arm a beat late
        float w = Mth.sin(f + 0.45F * Mth.sin(f));
        float wTip = Mth.sin(f - 0.8F + 0.45F * Mth.sin(f - 0.8F));

        // --- the body bobs against its wings and leans into its flight
        this.body.y += Mth.sin(f - 1.2F) * 1.1F * beat;
        this.body.xRot += 0.28F * fly;
        float amp = 0.72F * beat;
        this.leftWing.zRot += w * amp;
        this.rightWing.zRot -= w * amp;
        this.leftTip.zRot += wTip * amp * 0.75F;
        this.rightTip.zRot -= wTip * amp * 0.75F;
        float sweep = Mth.cos(f) * 0.22F * beat;
        this.leftWing.yRot += sweep;
        this.rightWing.yRot -= sweep;

        // --- looking about, ears twitching one at a time
        this.head.yRot += s.yRot * Anim.DEG * 0.8F * awake;
        this.head.xRot += s.xRot * Anim.DEG * 0.6F * awake;
        float cycle = Mth.positiveModulo(age + s.seed * 7.0F, 110.0F);
        this.leftEar.zRot -= Anim.envelope(cycle, 10.0F, 2.0F, 1.0F, 5.0F) * 0.35F * awake;
        this.rightEar.zRot += Anim.envelope(cycle, 62.0F, 2.0F, 1.0F, 5.0F) * 0.35F * awake;
        this.leftEar.xRot += Mth.sin(f - 1.6F) * 0.06F * beat;
        this.rightEar.xRot += Mth.sin(f - 1.6F) * 0.06F * beat;

        // --- the drill turns with its spin; the speakers pump
        this.drill.zRot += s.drill;
        float pump = Mth.clamp(s.pump, -0.3F, 1.2F);
        this.woofer.z -= pump * 0.9F;
        this.woofer.xScale = 1.0F + 0.06F * pump;
        this.woofer.yScale = 1.0F + 0.06F * pump;
        this.wooferCap.z -= pump * 0.6F;
        this.leftTweeter.z -= pump * 0.45F;
        this.rightTweeter.z -= pump * 0.45F;
        this.leftTweeter.xScale = this.leftTweeter.yScale = 1.0F + 0.12F * pump;
        this.rightTweeter.xScale = this.rightTweeter.yScale = 1.0F + 0.12F * pump;
        for (ModelPart cone : this.cones) {
            cone.y -= pump * 0.35F;
            cone.yScale = 1.0F + 2.0F * Math.max(0.0F, pump);
            cone.xScale = cone.zScale = 1.0F + 0.1F * pump;
        }

        // --- the cable tail streams behind and swings; the feet dangle
        float trail = 0.35F * fly;
        for (int i = 0; i < this.tail.length; i++) {
            this.tail[i].yRot += Mth.sin(age * 0.09F + s.seed - i * 0.7F) * (0.18F + 0.06F * i) * awake;
            this.tail[i].xRot -= trail * (i == 0 ? 1.0F : 0.4F);
        }
        float dangle = Mth.sin(f - 2.0F) * 0.12F * beat;
        this.leftFoot.xRot += (0.35F * fly + dangle) * awake;
        this.rightFoot.xRot += (0.35F * fly + dangle) * awake;
        this.tuft.xRot += Mth.sin(f - 2.4F) * 0.08F * beat;

        // --- inspecting an offering: nose down over it, ears forward
        float in = s.inspect;
        if (in > 0.0F) {
            this.body.xRot += 0.5F * in;
            this.head.xRot += 0.35F * in;
            this.leftEar.xRot -= 0.35F * in;
            this.rightEar.xRot -= 0.35F * in;
        }

        // --- waiting for the song: upright, ears tall, a slow attentive sway
        float wt = s.wait;
        this.body.xRot -= 0.1F * wt;
        this.leftEar.zRot -= 0.25F * wt;
        this.rightEar.zRot += 0.25F * wt;
        this.head.zRot += Mth.sin(age * 0.05F) * 0.08F * wt;

        // --- humming: head up, mouth open, swaying to its own beat
        float sing = s.sing * awake;
        if (sing > 0.0F) {
            this.head.xRot -= 0.25F * sing;
            this.jaw.xRot += (0.25F + 0.12F * Mth.sin(age * 0.6F)) * sing;
            this.body.zRot += Mth.sin(age * 0.25F) * 0.08F * sing;
            this.head.zRot += Mth.sin(age * 0.25F - 0.6F) * 0.1F * sing;
        }

        // --- the ceremony dance: twirls and barrel-rolls, wings flung wide, the tail whipping
        float d = s.dance;
        if (d > 0.0F) {
            float b = age * 0.4F;
            this.body.yRot += Mth.sin(b * 0.5F) * 1.1F * d;
            this.body.zRot += Mth.sin(b) * 0.3F * d;
            this.body.y -= Math.abs(Mth.sin(b)) * 2.0F * d;
            this.leftWing.zRot -= 0.25F * d;
            this.rightWing.zRot += 0.25F * d;
            for (int i = 0; i < this.tail.length; i++) {
                this.tail[i].yRot += Mth.sin(b * 2.0F - i) * 0.45F * d;
            }
        }

        // --- disappointed: it sags in the air, ears and wings drooping
        float sad = s.sad;
        if (sad > 0.0F) {
            this.body.y += 1.5F * sad;
            this.head.xRot += 0.45F * sad;
            this.head.yRot += Mth.sin(age * 0.3F) * 0.2F * sad;
            this.leftEar.zRot += 0.55F * sad;
            this.rightEar.zRot -= 0.55F * sad;
            this.leftWing.zRot += 0.3F * sad;
            this.rightWing.zRot -= 0.3F * sad;
        }

        // --- asleep: the wings folded round it like a cloak, head tucked, ears laid back
        float z = s.sleep;
        if (z > 0.0F) {
            this.leftWing.xRot += 0.2F * z;
            this.leftWing.yRot -= 1.1F * z;
            this.leftWing.zRot += 1.45F * z;
            this.rightWing.xRot += 0.2F * z;
            this.rightWing.yRot += 1.1F * z;
            this.rightWing.zRot -= 1.45F * z;
            this.leftTip.yRot += 2.6F * z;
            this.leftTip.zRot -= 0.75F * z;
            this.rightTip.yRot -= 2.6F * z;
            this.rightTip.zRot += 0.75F * z;
            this.head.xRot += 0.35F * z;
            this.leftEar.xRot += 0.3F * z;
            this.leftEar.zRot += 0.5F * z;
            this.rightEar.xRot += 0.3F * z;
            this.rightEar.zRot -= 0.5F * z;
            // and now and then an ear flicks in its sleep
            this.leftEar.zRot += Math.max(0.0F, Mth.sin(age * 0.03F + s.seed) - 0.92F) * 3.0F * Mth.sin(age * 1.3F) * z;
        }

        // --- the bow: a little rise, then it tips forward with its wings swept ahead, and settles back
        float bt = Anim.seconds(s.bow, age);
        if (bt >= 0.0F && bt < 2.2F) {
            float up = Anim.envelope(bt, 0.0F, 0.18F, 0.0F, 0.25F);
            float bow = Anim.envelope(bt, 0.25F, 0.5F, 0.6F, 0.8F);
            this.body.y -= 1.2F * up;
            this.body.xRot += 0.6F * bow - 0.1F * up;
            this.head.xRot += 0.3F * bow;
            this.leftWing.yRot += 0.6F * bow;
            this.rightWing.yRot -= 0.6F * bow;
            this.leftWing.zRot += 0.3F * bow;
            this.rightWing.zRot -= 0.3F * bow;
        }
    }
}
