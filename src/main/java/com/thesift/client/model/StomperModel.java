package com.thesift.client.model;

import com.thesift.client.renderer.state.StomperRenderState;
import com.thesift.entity.Stomper;
import com.thesift.entity.StomperRig;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * S1 the Stomper, a tall Sift elephant (geometry: tools/stomper.py).
 *
 * <ul>
 *   <li>Walk: a lateral-sequence elephant walk (left hind, left fore, right hind, right fore), the
 *   stride tied to limb swing so planted feet never slide; knees fold as each foot lifts, feet stay
 *   flat, the body rolls and the trunk, ears, tail and garden swing with every step.</li>
 *   <li>Idle: breathing (the torso swells, the legs stay put), ear flaps, a swaying, curling trunk.</li>
 *   <li>Grab: the trunk is posed by {@link StomperRig#pose} from the synced grab phase, the same
 *   pose the entity uses to hold the victim at the tip.</li>
 *   <li>Stomp: rears up on the hind legs (they stay planted and upright), forelegs folded, trunk
 *   raised, then slams down with a squash.</li>
 *   <li>Spray, drink, trunk slap, garden shake, sniff, dance, hurt flinch and a collapsing death.</li>
 *   <li>Babies (Stomplings): short legs, a big head and ears, and they curl up and roll.</li>
 * </ul>
 * Every part only rotates or scales about its own pivot (the body is also moved), so nothing comes
 * loose however many animations stack up.
 */
public class StomperModel extends EntityModel<StomperRenderState> {
    private static final float PI = (float) Math.PI;
    private static final float TWO_PI = PI * 2.0F;
    private static final String[] LEGS = {"front_left", "front_right", "back_left", "back_right"};
    /** Lateral-sequence walk, as fractions of a stride: left hind, then left fore, right hind, right fore. */
    private static final float[] GAIT = {0.25F, 0.75F, 0.0F, 0.5F};
    /** Stride frequency against limb swing and swing amplitude, chosen together so the feet do not slide. */
    private static final float STRIDE = 0.76F;
    private static final float SWING = 0.32F;
    /** A baby's legs are this long (it sits lower by the difference) and the curled ball's centre. */
    private static final float BABY_LEGS = 0.6F;
    private static final float BABY_DROP = 28.0F * (1.0F - BABY_LEGS);

    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart garden;
    private final ModelPart tail;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftTusk;
    private final ModelPart rightTusk;
    private final ModelPart crown;
    private final ModelPart[] trunk = new ModelPart[StomperRig.SEGMENTS];
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];
    private final ModelPart[] plants;
    private final float[] grab = new float[2 + StomperRig.SEGMENTS];

    public StomperModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.torso = this.body.getChild("torso");
        this.garden = this.torso.getChild("garden");
        this.tail = this.torso.getChild("tail");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftTusk = this.head.getChild("left_tusk");
        this.rightTusk = this.head.getChild("right_tusk");
        this.crown = this.head.getChild("crown_tuft");
        ModelPart t = this.head;
        for (int i = 0; i < StomperRig.SEGMENTS; i++) {
            t = t.getChild("trunk_" + i);
            this.trunk[i] = t;
        }
        java.util.List<ModelPart> list = new java.util.ArrayList<>();
        for (int i = 0; this.garden.hasChild("plant_" + i); i++) {
            list.add(this.garden.getChild("plant_" + i));
        }
        this.plants = list.toArray(new ModelPart[0]);
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(LEGS[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(LEGS[i] + "_shin");
            this.feet[i] = this.shins[i].getChild(LEGS[i] + "_foot");
        }
    }

    @Override
    public void setupAnim(StomperRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float roll = s.isBaby ? s.roll : 0.0F;
        // once it moves at all the stride is full size: a shrinking stride is what makes feet slide
        float walk = Anim.smooth(s.walkAnimationSpeed / 0.25F) * (1.0F - roll);
        float pos = s.walkAnimationPos * STRIDE;
        boolean grabbing = StomperRig.pose(s.grabPhase, s.grabTime, this.grab);
        float earL = 0.0F;

        // ---- walk
        for (int i = 0; i < 4; i++) {
            float p = pos + GAIT[i] * TWO_PI;
            float lift = Math.max(0.0F, -Mth.cos(p)) * walk;
            this.legs[i].xRot = Mth.sin(p) * SWING * walk;
            this.shins[i].xRot = lift * 0.6F;
        }
        this.body.y += (1.0F - Math.abs(Mth.cos(pos * 2.0F))) * 0.45F * walk;
        this.body.zRot = Mth.sin(pos) * 0.035F * walk;
        this.body.yRot = Mth.sin(pos) * 0.02F * walk;
        this.head.xRot += Mth.sin(pos * 2.0F - 0.8F) * 0.04F * walk;
        for (int i = 0; i < StomperRig.SEGMENTS; i++) {
            this.trunk[i].xRot += Mth.sin(pos - i * 0.55F) * 0.07F * walk * (0.6F + i * 0.2F);
            this.trunk[i].zRot += Mth.sin(pos + i * 0.5F) * 0.05F * walk;
        }
        earL += Mth.sin(pos * 2.0F) * 0.07F * walk;
        this.tail.yRot = Mth.sin(pos) * 0.35F * walk;

        // ---- idle: breathing, a lazy ear fan with a double flap now and then, a swaying trunk
        float breath = Mth.sin(age * 0.08F);
        this.torso.yScale = 1.0F + 0.015F * breath;
        this.torso.xScale = 1.0F + 0.008F * breath;
        this.torso.zScale = 1.0F + 0.006F * breath;
        float cyc = (age * 1.0F) % 110.0F;
        float flap = cyc < 18.0F ? Mth.sin(cyc / 18.0F * TWO_PI * 2.0F) * (1.0F - cyc / 18.0F) : 0.0F;
        earL += flap * 0.45F + Mth.sin(age * 0.05F) * 0.06F;
        for (int i = 0; i < StomperRig.SEGMENTS; i++) {
            this.trunk[i].xRot += Mth.sin(age * 0.045F - i * 0.6F) * 0.05F * (1.0F + i * 0.3F);
            this.trunk[i].zRot += Mth.sin(age * 0.035F - i * 0.7F) * 0.06F;
        }
        // now and then the tip curls up to sniff the air
        float curl = Anim.envelope(age % 220.0F, 150.0F, 12.0F, 20.0F, 14.0F);
        this.trunk[3].xRot -= 0.35F * curl;
        this.trunk[4].xRot -= 0.6F * curl;
        this.tail.zRot = Mth.sin(age * 0.08F) * 0.15F;
        this.tail.xRot += Mth.sin(age * 0.05F) * 0.06F;
        this.head.yRot = Mth.clamp(s.yRot * Anim.DEG, -0.6F, 0.6F) * 0.8F;
        this.head.xRot += Mth.clamp(s.xRot * Anim.DEG, -0.4F, 0.4F) * 0.5F;

        // ---- sitting: folds its legs and lies down on its belly
        if (s.sitting) {
            this.body.y += 9.0F;
            for (int i = 0; i < 4; i++) {
                boolean front = i < 2;
                this.legs[i].xRot = front ? -1.25F : 1.25F;
                this.shins[i].xRot = front ? 1.6F : -1.6F;
            }
            this.head.xRot += 0.15F;
        }

        // ---- the stomp (1.3 s): rears up on the hind legs, holds, slams down, squashes
        float st = Anim.seconds(s.stomp, s.ageInTicks);
        float rise = 0.0F;
        float impact = 0.0F;
        if (st >= 0.0F && st < 1.3F) {
            rise = st < 0.55F ? Anim.smooth(st / 0.55F) : st < 0.65F ? 1.0F : st < 0.8F ? 1.0F - sq((st - 0.65F) / 0.15F) : 0.0F;
            impact = Anim.envelope(st, 0.8F, 0.03F, 0.05F, 0.42F);
            float a = 0.55F * rise;
            this.body.xRot -= a;
            this.body.y += 1.2F * impact;
            this.torso.yScale *= 1.0F - 0.05F * impact;
            this.torso.xScale *= 1.0F + 0.03F * impact;
            for (int i = 0; i < 4; i++) {
                if (i < 2) {
                    // forelegs fold up and paw, then plant straight
                    this.legs[i].xRot += -0.45F * rise + 0.15F * impact;
                    this.shins[i].xRot += 1.25F * rise + 0.2F * impact;
                } else {
                    // hind legs stay upright under it, crouching a little at the top
                    this.legs[i].xRot += a + 0.12F * rise;
                    this.shins[i].xRot += -0.2F * rise;
                }
            }
            this.head.xRot += -0.35F * rise + 0.25F * impact;
            this.trunk[0].xRot -= 1.9F * rise;
            this.trunk[1].xRot -= 0.3F * rise;
            this.trunk[3].xRot += 0.5F * rise;
            this.trunk[4].xRot += 0.6F * rise;
            earL += 0.5F * rise + Mth.sin(st * 40.0F) * 0.3F * impact;
            this.jaw.xRot += 0.45F * rise;
        }

        // ---- spray: rears a little, raises the trunk high, then blasts (jitter)
        float spray = Anim.seconds(s.spray, s.ageInTicks);
        float windup = Stomper.SPRAY_WINDUP / 20.0F;
        if (spray >= 0.0F && spray < windup + 1.7F) {
            float k = Math.min(Anim.backOut(Math.min(1.0F, spray / (windup * 0.8F))), Anim.envelope(spray, 0.0F, windup * 0.8F, 1.5F + windup * 0.2F, 0.4F));
            float shoot = spray > windup ? Anim.envelope(spray, windup, 0.08F, 1.3F, 0.3F) : 0.0F;
            this.body.xRot -= 0.12F * k;
            this.legs[2].xRot += 0.12F * k;
            this.legs[3].xRot += 0.12F * k;
            this.legs[0].xRot -= 0.2F * k;
            this.legs[1].xRot -= 0.2F * k;
            this.head.xRot -= 0.3F * k;
            float[] aim = {-2.2F, -0.2F, -0.1F, 0.25F, -0.3F};
            for (int i = 0; i < StomperRig.SEGMENTS; i++) {
                this.trunk[i].xRot = Mth.lerp(k, this.trunk[i].xRot, aim[i]);
                if (shoot > 0.0F) {
                    this.trunk[i].zRot += Mth.sin(s.ageInTicks * 2.3F) * 0.04F * shoot * (i + 1);
                }
            }
            this.jaw.xRot += 0.3F * k;
            earL += 0.35F * k;
        }

        // ---- drinking (3 s): head down, the trunk reaches into the pool, then sips back to the mouth
        float drink = Anim.seconds(s.drink, s.ageInTicks);
        if (drink >= 0.0F && drink < 3.2F) {
            float e = Anim.envelope(drink, 0.0F, 0.4F, 2.3F, 0.5F);
            float sip = drink > 0.6F ? Math.max(0.0F, Mth.sin((drink - 0.6F) * 5.0F)) * e : 0.0F;
            this.head.xRot += 0.4F * e;
            this.trunk[0].xRot += -0.5F * e;
            this.trunk[1].xRot += 0.1F * e + 0.4F * sip;
            this.trunk[2].xRot += 0.1F * e + 0.7F * sip;
            this.trunk[3].xRot += 0.3F * e + 0.9F * sip;
            this.trunk[4].xRot += 0.6F * e + 0.9F * sip;
            this.jaw.xRot += 0.25F * sip;
        }

        // ---- melee: a trunk swat (0.5 s)
        float slap = Anim.seconds(s.slap, s.ageInTicks);
        if (slap >= 0.0F && slap < 0.6F) {
            float wind = Anim.envelope(slap, 0.0F, 0.12F, 0.0F, 0.0F);
            float hit = Anim.envelope(slap, 0.12F, 0.08F, 0.05F, 0.3F);
            this.trunk[0].xRot -= 1.1F * Math.max(wind, hit);
            this.trunk[0].yRot += 0.8F * wind - 1.2F * hit;
            this.trunk[2].xRot += 0.4F * hit;
            this.head.yRot += 0.15F * wind - 0.3F * hit;
        }

        // ---- a baby drums the ground with its trunk, one tap a beat
        float drum = Anim.seconds(s.drum, s.ageInTicks);
        if (drum >= 0.0F && drum < 0.25F) {
            float up = Anim.envelope(drum, 0.0F, 0.06F, 0.0F, 0.14F);
            this.trunk[0].xRot -= 0.7F * up;
            this.trunk[4].xRot -= 0.4F * up;
            this.head.xRot -= 0.12F * up;
            earL += 0.3F * up;
        }

        // ---- a puff of air blown out of the trunk
        float puff = Anim.seconds(s.puff, s.ageInTicks);
        if (puff >= 0.0F && puff < 0.8F) {
            float up = Anim.envelope(puff, 0.0F, 0.15F, 0.1F, 0.5F);
            this.trunk[0].xRot -= 0.5F * up;
            this.trunk[3].xRot -= 0.4F * up;
            this.trunk[4].xRot -= 0.7F * up;
            this.head.xRot -= 0.1F * up;
        }

        // ---- the garden shake (1.2 s): hunch, then shake itself out like a wet dog, head to tail
        float shake = Anim.seconds(s.shake, s.ageInTicks);
        float shook = 0.0F;
        if (shake >= 0.0F && shake < 1.25F) {
            float hunch = Anim.envelope(shake, 0.0F, 0.12F, 0.05F, 0.15F);
            shook = Anim.envelope(shake, 0.15F, 0.08F, 0.55F, 0.4F);
            float w = shake * 30.0F;
            this.head.xRot += 0.15F * hunch;
            this.torso.zRot = Mth.sin(w - 0.6F) * 0.08F * shook;
            this.head.zRot = Mth.sin(w) * 0.2F * shook;
            this.tail.yRot += Mth.sin(w - 1.8F) * 0.9F * shook;
            earL += Mth.sin(w + 0.4F) * 0.7F * shook;
            for (int i = 0; i < StomperRig.SEGMENTS; i++) {
                this.trunk[i].zRot += Mth.sin(w - 0.5F * (i + 1)) * 0.3F * shook;
            }
        }

        // ---- the snuffle (2.2 s): head down, trunk tip at the flowers by its feet, twitching
        float sniff = Anim.seconds(s.sniff, s.ageInTicks);
        if (sniff >= 0.0F && sniff < 2.3F) {
            float e = Anim.envelope(sniff, 0.0F, 0.4F, 1.3F, 0.5F);
            float twitch = Math.max(0.0F, Mth.sin(sniff * 26.0F)) * Anim.envelope(sniff, 0.4F, 0.1F, 1.1F, 0.2F);
            this.head.xRot += 0.35F * e;
            this.trunk[0].xRot += 0.15F * e;
            this.trunk[3].xRot -= 0.25F * e + 0.2F * twitch;
            this.trunk[4].xRot -= 0.3F * e + 0.35F * twitch;
            earL -= 0.2F * e;
        }

        // ---- dancing: bob, sway, wave the trunk, flap the ears, lift the feet in turn
        if (s.dancing) {
            float beat = s.ageInTicks * 0.55F;
            float bob = Math.abs(Mth.sin(beat));
            float d = Math.min(1.0F, (Stomper.DANCE_LENGTH - s.danceTicks) / 8.0F);
            this.body.y += bob * 1.2F * d;
            this.body.zRot += Mth.sin(beat * 0.5F) * 0.07F * d;
            this.head.zRot += Mth.sin(beat * 0.5F + 0.6F) * 0.15F * d;
            this.trunk[0].xRot += (-1.2F + Mth.sin(beat * 0.5F) * 0.4F) * d;
            for (int i = 0; i < StomperRig.SEGMENTS; i++) {
                this.trunk[i].zRot += Mth.sin(beat * 0.5F - i * 0.9F) * 0.3F * d;
            }
            earL += Mth.sin(beat) * 0.45F * d;
            for (int i = 0; i < 4; i++) {
                float ph = (i == 0 || i == 3) ? 0.0F : PI;
                float up = Math.max(0.0F, Mth.sin(beat * 0.5F + ph)) * d;
                this.legs[i].xRot -= 0.25F * up;
                this.shins[i].xRot += 0.5F * up;
            }
            this.tail.yRot += Mth.sin(beat) * 0.6F * d;
        }

        // ---- the grab: the trunk follows the rig's pose exactly (the victim hangs at its tip)
        if (grabbing) {
            this.head.yRot = 0.0F;
            this.head.xRot = this.grab[0];
            this.head.zRot = 0.0F;
            this.trunk[0].yRot = this.grab[1];
            for (int i = 0; i < StomperRig.SEGMENTS; i++) {
                this.trunk[i].xRot = this.grab[2 + i];
                this.trunk[i].zRot = 0.0F;
            }
            boolean held = s.grabPhase == StomperRig.LIFT || s.grabPhase == StomperRig.HOLD;
            earL += held ? 0.45F : 0.2F;
            this.jaw.xRot += held ? 0.35F : 0.15F;
            // braces its legs while it heaves
            for (int i = 0; i < 4; i++) {
                this.legs[i].zRot += (i % 2 == 0 ? -0.05F : 0.05F) * (held ? 1.0F : 0.4F);
            }
        }

        // ---- hurt: a flinch - head up, ears out, trunk curls
        if (s.hurtTicks >= 0.0F) {
            float h = Mth.sin(Math.min(1.0F, s.hurtTicks / 10.0F) * PI);
            this.head.xRot -= 0.22F * h;
            earL += 0.45F * h;
            for (int i = 1; i < StomperRig.SEGMENTS; i++) {
                if (!grabbing) {
                    this.trunk[i].xRot += 0.3F * h;
                }
            }
            this.body.zRot += 0.04F * h;
        }

        // ---- death: the forelegs buckle, it sinks and keels over onto its side, trunk limp
        if (s.dying > 0.0F) {
            float buckle = Anim.smooth(s.dying / 8.0F);
            float fall = Anim.smooth((s.dying - 5.0F) / 9.0F);
            this.body.xRot += 0.22F * buckle * (1.0F - fall);
            this.body.y += 3.0F * buckle + 11.0F * fall;
            this.body.zRot = Mth.lerp(fall, this.body.zRot, 1.35F);
            for (int i = 0; i < 4; i++) {
                this.legs[i].xRot = Mth.lerp(buckle, this.legs[i].xRot, i < 2 ? -0.5F : 0.2F);
                this.shins[i].xRot = Mth.lerp(buckle, this.shins[i].xRot, i < 2 ? 1.4F : 0.3F);
                this.legs[i].zRot += (i % 2 == 0 ? -0.25F : 0.25F) * fall;
            }
            for (int i = 0; i < StomperRig.SEGMENTS; i++) {
                this.trunk[i].xRot = Mth.lerp(fall, this.trunk[i].xRot, 0.15F);
            }
            this.head.xRot += 0.3F * buckle;
            earL = Mth.lerp(fall, earL, -0.45F);
            this.jaw.xRot += 0.3F * buckle;
        }

        // feet stay flat on the ground whatever the leg above is doing
        for (int i = 0; i < 4; i++) {
            this.feet[i].xRot = -(this.body.xRot + this.legs[i].xRot + this.shins[i].xRot);
        }
        this.leftEar.yRot += earL;
        this.rightEar.yRot -= earL;
        this.jaw.xRot = Mth.clamp(this.jaw.xRot, 0.0F, 0.6F);

        // ---- the garden sways with each heavy step, jiggles on stomps, wiggles when it dances
        for (int i = 0; i < this.plants.length; i++) {
            ModelPart p = this.plants[i];
            float ph = i * 1.37F;
            p.zRot = Mth.sin(age * 0.07F + ph) * 0.07F + Mth.sin(pos * 2.0F + ph) * 0.15F * walk + impact * Mth.sin(s.ageInTicks * 1.9F + ph) * 0.35F
                    + shook * Mth.sin(shake * 30.0F - 1.5F - ph * 0.2F) * 0.5F;
            p.xRot = Mth.cos(age * 0.06F + ph) * 0.05F - walk * 0.1F + rise * 0.15F;
            if (s.dancing) {
                p.zRot += Mth.sin(s.ageInTicks * 0.55F + ph) * 0.3F;
            }
        }

        if (s.isBaby) {
            this.baby(s, roll);
        }
    }

    /**
     * A Stompling: stubby legs, a big head with big ears, a short trunk, no tusks, a little garden.
     * Rolling, it tucks everything in and tumbles along like a ball (the angle comes from the
     * distance it has rolled, so it never skids).
     */
    private void baby(StomperRenderState s, float roll) {
        this.head.xScale = this.head.yScale = this.head.zScale = 1.35F;
        // (the ears grow with the head; a little less, or they would swamp it)
        this.leftEar.xScale = this.leftEar.yScale = this.leftEar.zScale = 0.9F;
        this.rightEar.xScale = this.rightEar.yScale = this.rightEar.zScale = 0.9F;
        this.leftTusk.visible = false;
        this.rightTusk.visible = false;
        this.trunk[0].xScale = this.trunk[0].yScale = this.trunk[0].zScale = 0.65F;
        this.torso.zScale *= 0.82F;
        this.head.z += 3.5F;
        // the little garden tucks away as it curls up
        this.garden.xScale = this.garden.yScale = this.garden.zScale = 0.6F * (1.0F - roll);
        this.garden.visible = roll < 0.98F;
        for (int i = 0; i < this.plants.length; i++) {
            this.plants[i].visible = i % 2 == 0;
        }
        for (int i = 0; i < 4; i++) {
            this.legs[i].yScale = BABY_LEGS;
        }
        this.tail.yScale = 0.7F;
        if (s.dying <= 0.0F && !s.sitting) {
            this.body.y += BABY_DROP;
        }
        if (roll <= 0.0F) {
            return;
        }
        // curl up into a ball: legs drawn up into the body, chin tucked, trunk wrapped under, ears hugging the head
        for (int i = 0; i < 4; i++) {
            this.legs[i].xRot = Mth.lerp(roll, this.legs[i].xRot, 0.0F);
            this.legs[i].yScale = Mth.lerp(roll, BABY_LEGS, 0.25F);
            this.shins[i].xRot = Mth.lerp(roll, this.shins[i].xRot, 0.0F);
            this.feet[i].xRot = Mth.lerp(roll, this.feet[i].xRot, 0.0F);
        }
        this.head.xRot = Mth.lerp(roll, this.head.xRot, 1.1F);
        float hs = Mth.lerp(roll, 1.35F, 0.95F);
        // the tuft on its crown tucks away too
        this.crown.xScale = this.crown.yScale = this.crown.zScale = 1.0F - roll;
        this.crown.visible = roll < 0.98F;
        this.head.xScale = this.head.yScale = this.head.zScale = hs;
        for (int i = 0; i < StomperRig.SEGMENTS; i++) {
            this.trunk[i].xRot = Mth.lerp(roll, this.trunk[i].xRot, 0.55F);
        }
        this.leftEar.yRot = Mth.lerp(roll, this.leftEar.yRot, 0.0F);
        this.rightEar.yRot = Mth.lerp(roll, this.rightEar.yRot, 0.0F);
        // tumble about the ball's centre: rotate the body, then move its pivot so the centre stays put
        float a = s.rollAngle;
        float py = this.body.y;
        float pz = this.body.z;
        float cy = py - 6.0F;
        float cz = pz - 10.0F;
        // the curled body tumbles like a box on its edges: its lowest point stays on the ground
        float ty = Mth.lerp(roll, cy, 24.0F - (13.0F * Math.abs(Mth.cos(s.rollAngle)) + 19.0F * Math.abs(Mth.sin(s.rollAngle))) * 0.85F);
        float dy = cy - py;
        float dz = cz - pz;
        float c = Mth.cos(a);
        float sn = Mth.sin(a);
        this.body.xRot += a;
        this.body.y = ty - (dy * c - dz * sn);
        this.body.z = cz - (dy * sn + dz * c);
    }

    private static float sq(float t) {
        t = Mth.clamp(t, 0.0F, 1.0F);
        return t * t;
    }
}
