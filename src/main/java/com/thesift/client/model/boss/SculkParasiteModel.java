package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * A Sculk Parasite: seven plated segments that ripple from side to side as it scuttles, legs rowing
 * in a wave down each flank, bone mandibles that open slowly and snap shut, Warden tendrils that
 * twitch and a glowing sting held up over its back. To lunge it rears up and folds its body into a
 * tight zigzag, like a spring, then shoots forward with its jaws flung wide.
 */
public class SculkParasiteModel extends EntityModel<MinionRenderState> {
    private static final int SEGMENTS = 7;
    private final ModelPart head;
    private final ModelPart leftMandible;
    private final ModelPart rightMandible;
    private final ModelPart leftTip;
    private final ModelPart rightTip;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;
    private final ModelPart[] segments = new ModelPart[SEGMENTS];
    private final ModelPart[] spines = new ModelPart[SEGMENTS];
    private final ModelPart[] leftLegs = new ModelPart[SEGMENTS];
    private final ModelPart[] rightLegs = new ModelPart[SEGMENTS];
    private final ModelPart[] leftFeet = new ModelPart[SEGMENTS];
    private final ModelPart[] rightFeet = new ModelPart[SEGMENTS];
    private final ModelPart leftCercus;
    private final ModelPart rightCercus;
    private final ModelPart tail;
    private final ModelPart stinger;

    public SculkParasiteModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.leftMandible = this.head.getChild("left_mandible");
        this.rightMandible = this.head.getChild("right_mandible");
        this.leftTip = this.leftMandible.getChild("left_mandible_tip");
        this.rightTip = this.rightMandible.getChild("right_mandible_tip");
        this.leftTendril = this.head.getChild("left_tendril");
        this.rightTendril = this.head.getChild("right_tendril");
        // the body hangs off the back of the head, each segment off the one before it
        ModelPart parent = this.head;
        for (int i = 0; i < SEGMENTS; i++) {
            ModelPart seg = parent.getChild("segment_" + i);
            this.segments[i] = seg;
            this.spines[i] = seg.getChild("spine_" + i);
            this.leftLegs[i] = seg.getChild("left_leg_" + i);
            this.rightLegs[i] = seg.getChild("right_leg_" + i);
            this.leftFeet[i] = this.leftLegs[i].getChild("left_foot_" + i);
            this.rightFeet[i] = this.rightLegs[i].getChild("right_foot_" + i);
            parent = seg;
        }
        this.leftCercus = parent.getChild("left_cercus");
        this.rightCercus = parent.getChild("right_cercus");
        this.tail = parent.getChild("tail");
        this.stinger = this.tail.getChild("stinger");
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F);
        float pos = s.walkAnimationPos;
        // the lunge (the attack animation starts with the wind-up): half a second rearing up and
        // coiling, then the strike
        float t = Anim.seconds(s.attack, age);
        float coil = Anim.envelope(t, 0.0F, 0.3F, 0.2F, 0.06F);
        float strike = Anim.envelope(t, 0.5F, 0.06F, 0.12F, 0.35F);
        float tense = Math.max(coil, s.windingUp ? 1.0F : 0.0F);

        // the head turns to look; the body keeps its course behind it
        float look = s.yRot * Anim.DEG * 0.5F;
        float pitch = s.xRot * Anim.DEG * 0.4F;
        this.head.yRot += look;
        this.head.xRot += pitch;
        this.segments[0].yRot -= look;
        this.segments[0].xRot -= pitch;

        // a wave rippling down the body from side to side: barely there at rest, strong at a run
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i].yRot += Mth.sin(pos * 0.45F - i * 0.75F) * (0.04F + 0.12F * walk) + Mth.sin(age * 0.07F - i * 0.6F) * 0.035F;
            this.segments[i].xRot += Mth.sin(age * 0.1F - i * 0.5F) * 0.012F;
            // the bone spines rise and fall like breathing, and bristle when it coils
            this.spines[i].xRot += Mth.sin(age * 0.12F - i * 0.7F) * 0.08F + 0.35F * tense;
        }

        // the legs row in a wave down each flank, left and right out of step; a foot lifts and tucks
        // as its leg swings forward
        for (int i = 0; i < SEGMENTS; i++) {
            float ph = pos * 1.3F - i * 0.9F;
            float swing = Mth.cos(ph) * 0.4F * walk;
            float liftL = Math.max(0.0F, -Mth.sin(ph)) * 0.5F * walk;
            float liftR = Math.max(0.0F, Mth.sin(ph)) * 0.5F * walk;
            // standing still, the feet fidget one after another
            float fidget = Mth.sin(age * 0.21F + i * 1.7F) * 0.05F * (1.0F - walk);
            this.leftLegs[i].yRot += swing + fidget;
            this.rightLegs[i].yRot += swing - fidget;
            this.leftLegs[i].zRot -= liftL + 0.15F * tense;
            this.rightLegs[i].zRot += liftR + 0.15F * tense;
            this.leftFeet[i].zRot += liftL * 0.6F + 0.3F * tense;
            this.rightFeet[i].zRot -= liftR * 0.6F + 0.3F * tense;
        }

        // the mandibles open slowly and snap shut - chattering fast when it is about to lunge - and
        // gnaw as it runs; on the strike they are flung wide, then slam shut on the bite
        float cyc = (age * (tense > 0.0F ? 0.25F : 0.05F)) % 1.0F;
        float open = cyc < 0.7F ? Anim.smooth(cyc / 0.7F) : Math.max(0.0F, 1.0F - (cyc - 0.7F) / 0.06F);
        float jaw = open * (0.25F + 0.2F * tense) + Mth.sin(pos * 0.7F) * 0.06F * walk;
        float wide = Anim.envelope(t, 0.47F, 0.05F, 0.08F, 0.05F);
        jaw = jaw * (1.0F - wide) + 0.75F * wide;
        this.leftMandible.yRot -= jaw;
        this.rightMandible.yRot += jaw;
        this.leftTip.yRot -= jaw * 0.35F;
        this.rightTip.yRot += jaw * 0.35F;

        // Warden tendrils: a slow sway and, every so often, a twitch - a flurry of them while it coils,
        // splayed wide in alarm
        float flick = Math.min(1.0F, (float) Math.pow(Math.max(0.0F, Mth.sin(age * 0.11F)), 16.0) + tense);
        float twitch = Mth.cos(age * 2.25F) * Mth.PI * 0.08F * flick;
        this.leftTendril.xRot += twitch + Mth.sin(age * 0.09F) * 0.06F + 0.3F * tense;
        this.rightTendril.xRot += -twitch + Mth.sin(age * 0.09F + 1.3F) * 0.06F + 0.3F * tense;
        this.leftTendril.zRot += Mth.sin(age * 0.13F) * 0.05F + 0.3F * tense;
        this.rightTendril.zRot -= Mth.sin(age * 0.13F + 0.8F) * 0.05F + 0.3F * tense;

        // the sting, held up over the back and swaying; raised high and quivering while it coils,
        // and the prongs behind it twitching as it runs
        this.tail.yRot += Mth.sin(age * 0.09F) * 0.15F + Mth.sin(pos * 0.45F - SEGMENTS * 0.75F) * 0.1F * walk;
        this.tail.xRot += Mth.sin(age * 0.11F) * 0.06F + 0.45F * tense - 0.3F * strike;
        this.stinger.xRot += Mth.sin(age * 0.15F + 1.0F) * 0.08F + (0.35F + Mth.sin(age * 3.1F) * 0.06F) * tense;
        this.leftCercus.yRot += Mth.sin(age * 0.17F) * 0.08F + Mth.sin(pos * 1.3F) * 0.1F * walk;
        this.rightCercus.yRot -= Mth.sin(age * 0.17F + 0.8F) * 0.08F + Mth.sin(pos * 1.3F + 1.0F) * 0.1F * walk;

        if (coil > 0.0F || strike > 0.0F) {
            // rearing: the head lifts, tilts back and draws in, the front of the body slopes back down
            // to the ground...
            this.head.y -= 3.0F * coil;
            this.head.z += 1.5F * coil;
            this.head.xRot -= 0.45F * coil;
            this.segments[0].xRot -= 0.3F * coil;
            this.segments[2].xRot += 0.75F * coil;
            // ...and the rest folds into a tight zigzag
            for (int i = 2; i < SEGMENTS; i++) {
                this.segments[i].yRot += (i == 2 ? 0.25F : i % 2 == 0 ? 0.5F : -0.5F) * coil;
            }
            // the strike: the head shoots forward and dips to bite, the body springs out straight
            // behind it
            this.head.z -= 3.0F * strike;
            this.head.xRot += 0.25F * strike;
            this.segments[0].xRot -= 0.25F * strike;
            for (int i = 1; i < SEGMENTS; i++) {
                this.segments[i].z += 0.3F * strike;
            }
        }

        // hit: it recoils and writhes
        if (s.hurtTicks >= 0.0F) {
            float h = 1.0F - Math.min(1.0F, s.hurtTicks / 10.0F);
            this.head.xRot -= 0.2F * h;
            this.segments[0].xRot += 0.2F * h;
            for (int i = 0; i < SEGMENTS; i++) {
                this.segments[i].yRot += Mth.sin(s.hurtTicks * 1.3F + i * 0.9F) * 0.12F * h;
            }
        }

        // dying: it curls up into a ring, legs folding and twitching, jaws gaping, the sting drooping
        if (s.dying > 0.0F) {
            float d = Anim.smooth(s.dying / 12.0F);
            float kick = d * (1.0F - Anim.smooth(s.dying / 20.0F));
            for (int i = 0; i < SEGMENTS; i++) {
                this.segments[i].yRot += 0.32F * d;
                float k = Mth.sin(age * 2.1F + i * 1.3F) * 0.3F * kick;
                this.leftLegs[i].yRot += k;
                this.rightLegs[i].yRot -= k;
                this.leftLegs[i].zRot += 0.4F * d;
                this.rightLegs[i].zRot -= 0.4F * d;
                this.leftFeet[i].zRot += 0.6F * d;
                this.rightFeet[i].zRot -= 0.6F * d;
            }
            this.tail.xRot -= 0.7F * d;
            this.stinger.xRot -= 0.5F * d;
            this.leftMandible.yRot -= 0.5F * d;
            this.rightMandible.yRot += 0.5F * d;
            this.leftTendril.xRot -= 0.4F * d;
            this.rightTendril.xRot -= 0.4F * d;
        }
    }
}
