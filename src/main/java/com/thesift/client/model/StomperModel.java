package com.thesift.client.model;

import com.thesift.client.Expression;
import com.thesift.client.renderer.state.StomperRenderState;
import com.thesift.entity.Stomper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Stomper: a heavy, rolling elephant gait with squash on every footfall, a breathing hump whose
 * spiracles pop when they puff, a throat sac that croaks and swells, and a four-part trunk that
 * sways, sips, rears up to spray, slaps and waves about when it dances.
 *
 * <p>Babies get their own proportions - a huge head, big eyes and ears, a short trunk, a round
 * little body on stubby legs - and carry a drum on their rump that they tap with their tail.
 *
 * <p>Every part only ever rotates or scales about its own pivot (only the root body is moved), and
 * the jaw's swing is capped, so nothing comes loose however many animations stack up.
 */
public class StomperModel extends EntityModel<StomperRenderState> {
    private static final float PI = (float) Math.PI;
    private final ModelPart body;
    private final ModelPart hump;
    private final ModelPart[] spiracles = new ModelPart[3];
    private final ModelPart tail;
    private final ModelPart drum;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart leftEyelid;
    private final ModelPart rightEyelid;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart leftSmallEye;
    private final ModelPart rightSmallEye;
    private final ModelPart[] trunk = new ModelPart[4];
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];
    /** The garden on its back, shoulders and brow: every plant sways on its own. */
    private final ModelPart[] plants;

    public StomperModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.hump = this.body.getChild("hump");
        for (int i = 0; i < 3; i++) {
            this.spiracles[i] = this.hump.getChild("spiracle_" + i);
        }
        this.tail = this.body.getChild("tail");
        this.drum = this.body.getChild("baby_drum");
        this.leftEye = this.head().getChild("left_eye");
        this.rightEye = this.head().getChild("right_eye");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.throat = this.jaw.getChild("throat");
        this.leftEyelid = this.head.getChild("left_eye").getChild("left_eyelid");
        this.rightEyelid = this.head.getChild("right_eye").getChild("right_eyelid");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.leftSmallEye = this.head.getChild("left_small_eye");
        this.rightSmallEye = this.head.getChild("right_small_eye");
        ModelPart t = this.head;
        for (int i = 0; i < 4; i++) {
            t = t.getChild("trunk_" + i);
            this.trunk[i] = t;
        }
        // however many plants tools/mobs_wild.py grew on its back, shoulders and brow
        java.util.List<ModelPart> garden = new java.util.ArrayList<>();
        for (int i = 0; this.hump.hasChild("plant_" + i); i++) {
            garden.add(this.hump.getChild("plant_" + i));
        }
        for (int i = 0; this.body.hasChild("body_plant_" + i); i++) {
            garden.add(this.body.getChild("body_plant_" + i));
        }
        for (int i = 0; this.head.hasChild("head_plant_" + i); i++) {
            garden.add(this.head.getChild("head_plant_" + i));
        }
        this.plants = garden.toArray(new ModelPart[0]);
        String[] names = {"front_left", "front_right", "back_left", "back_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(names[i] + "_leg");
            this.feet[i] = this.legs[i].getChild(names[i] + "_foot");
        }
    }

    private ModelPart head() {
        return this.body.getChild("head");
    }

    @Override
    public void setupAnim(StomperRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos * 0.45F;

        // --- the heavy gait: diagonal legs together, a rolling sway and a bounce on each footfall
        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : PI;
            float sw = Mth.sin(pos + phase);
            this.legs[i].xRot = sw * 0.45F * walk;
            this.feet[i].xRot = -this.legs[i].xRot * 0.6F - Math.max(0.0F, Mth.cos(pos + phase)) * 0.25F * walk;
        }
        float fall = Math.abs(Mth.cos(pos));
        this.body.y -= (1.0F - fall) * 1.3F * walk;
        this.body.zRot = Mth.sin(pos) * 0.05F * walk;
        this.body.yRot = Mth.sin(pos) * 0.03F * walk;
        float squash = (fall > 0.92F ? (fall - 0.92F) / 0.08F : 0.0F) * walk;
        this.body.yScale = 1.0F - 0.05F * squash;
        this.body.xScale = 1.0F + 0.03F * squash;
        this.body.zScale = 1.0F + 0.02F * squash;
        // the front dips as the forefeet land and the head follows a beat behind (follow-through)
        this.body.xRot = Mth.sin(pos * 2.0F) * 0.025F * walk;
        this.head.xRot = Mth.sin(pos * 2.0F - 0.7F) * 0.05F * walk;
        this.hump.yScale = 1.0F - 0.04F * squash;
        this.hump.xScale = 1.0F + 0.02F * squash;
        this.tail.yRot = Mth.sin(pos) * 0.3F * walk + Mth.sin(age * 0.07F) * 0.15F;
        this.tail.xRot += Mth.sin(age * 0.05F) * 0.05F;

        // --- croaking, looking around
        float croak = Math.max(0.0F, Mth.sin(age * 0.045F)) ;
        croak = croak > 0.85F ? (croak - 0.85F) / 0.15F : 0.0F;
        float sac = croak * 0.4F;
        this.head.yRot = s.yRot * Anim.DEG * 0.4F;
        this.head.xRot += s.xRot * Anim.DEG * 0.3F;
        this.leftEar.zRot += Mth.sin(age * 0.11F) * 0.07F + walk * Mth.sin(pos * 2.0F) * 0.12F;
        this.rightEar.zRot -= Mth.sin(age * 0.11F + 0.8F) * 0.07F + walk * Mth.sin(pos * 2.0F) * 0.12F;
        this.leftSmallEye.yRot = Mth.sin(age * 0.03F) * 0.15F;
        float glance = Mth.clamp(s.yRot * Anim.DEG * 0.2F, -0.15F, 0.15F);
        this.leftEye.yRot = glance;
        this.rightEye.yRot = glance;
        this.rightSmallEye.yRot = Mth.sin(age * 0.03F + 1.3F) * 0.15F;
        // the trunk sways and curls on its own, like a curious hand
        for (int i = 0; i < 4; i++) {
            this.trunk[i].xRot += Mth.sin(age * 0.05F - i * 0.6F) * 0.07F * (i + 1) * 0.6F + walk * Mth.sin(pos - i * 0.7F) * 0.08F;
            this.trunk[i].zRot = Mth.sin(age * 0.04F - i * 0.7F) * 0.08F;
        }

        // --- spiracle puffs: each hole pops up, the hump heaves
        float puffT = Anim.seconds(s.puff, s.ageInTicks);
        if (puffT >= 0.0F) {
            for (int i = 0; i < 3; i++) {
                float e = Anim.envelope(puffT, i * 0.05F, 0.06F, 0.06F, 0.35F);
                this.spiracles[i].yScale = 1.0F + 1.6F * e;
                this.spiracles[i].xScale = 1.0F + 0.5F * e;
                this.spiracles[i].zScale = 1.0F + 0.5F * e;
            }
            float h = Anim.envelope(puffT, 0.0F, 0.08F, 0.0F, 0.4F);
            this.hump.yScale *= 1.0F + 0.08F * h;
            this.hump.xScale = 1.0F - 0.03F * h;
        }

        // --- sitting / resting: belly down, legs splayed
        if (s.sitting) {
            this.body.y += 7.0F;
            for (int i = 0; i < 4; i++) {
                float sgn = (i % 2 == 0) ? 1.0F : -1.0F;
                this.legs[i].xRot = i < 2 ? -0.9F : 0.9F;
                this.legs[i].zRot = -sgn * 0.25F;
                this.feet[i].xRot = -this.legs[i].xRot;
            }
            for (int i = 0; i < 4; i++) {
                this.trunk[i].xRot -= 0.12F;
            }
        }

        // --- drinking (3 s): head down, trunk reaches into the Chrome, then sips back up to the mouth
        float drink = Anim.seconds(s.drink, s.ageInTicks);
        if (drink >= 0.0F && drink < 3.2F) {
            float e = Anim.envelope(drink, 0.0F, 0.4F, 2.3F, 0.5F);
            this.head.xRot += 0.45F * e;
            this.body.xRot += 0.08F * e;
            this.trunk[0].xRot += -0.55F * e;
            float sip = drink > 0.5F ? Mth.sin((drink - 0.5F) * 6.0F) : -1.0F;
            float curl = Math.max(0.0F, sip) * e;
            this.trunk[1].xRot += 0.2F * e + 1.0F * curl;
            this.trunk[2].xRot += 0.35F * e + 1.1F * curl;
            this.trunk[3].xRot += 0.6F * e + 0.9F * curl;
            sac = Math.max(sac, curl * 0.6F);
            this.jaw.xRot += curl * 0.15F;
        }

        // --- spray: the telegraph (rear up, trunk raised high, gurgle) then the stream
        float spray = Anim.seconds(s.spray, s.ageInTicks);
        float windup = Stomper.SPRAY_WINDUP / 20.0F;
        if (spray >= 0.0F && spray < windup + 1.7F) {
            float up = Anim.envelope(spray, 0.0F, windup * 0.8F, 1.5F + windup * 0.2F, 0.4F);
            float shoot = spray > windup ? Anim.envelope(spray, windup, 0.08F, 1.3F, 0.3F) : 0.0F;
            float raise = Anim.backOut(Math.min(1.0F, spray / (windup * 0.8F)));
            float k = Math.min(raise, up);
            this.body.xRot -= 0.14F * k;
            this.body.y -= 1.5F * k;
            this.legs[0].xRot -= 0.35F * k;
            this.legs[1].xRot -= 0.35F * k;
            this.head.xRot -= 0.3F * k;
            this.trunk[0].xRot = Mth.lerp(k, this.trunk[0].xRot, -2.0F + 0.35F * shoot);
            this.trunk[1].xRot = Mth.lerp(k, this.trunk[1].xRot, 0.35F - 0.3F * shoot);
            this.trunk[2].xRot = Mth.lerp(k, this.trunk[2].xRot, 0.3F - 0.3F * shoot);
            this.trunk[3].xRot = Mth.lerp(k, this.trunk[3].xRot, 0.25F - 0.25F * shoot);
            if (shoot > 0.0F) {
                float jitter = Mth.sin(s.ageInTicks * 2.3F) * 0.05F * shoot;
                for (int i = 0; i < 4; i++) {
                    this.trunk[i].zRot += jitter * (i + 1);
                    this.trunk[i].xScale = 1.0F + 0.12F * shoot * Math.max(0.0F, Mth.sin(s.ageInTicks * 1.6F - i * 1.2F));
                    this.trunk[i].zScale = this.trunk[i].xScale;
                }
                this.body.zRot += Mth.sin(s.ageInTicks * 1.7F) * 0.02F * shoot;
                sac = Math.max(sac, 0.5F + 0.3F * Mth.sin(s.ageInTicks * 0.9F));
            } else {
                // the gulp before the blast
                sac = Math.max(sac, 0.9F * k);
                this.hump.yScale *= 1.0F + 0.06F * k;
            }
            this.leftEar.zRot += 0.4F * k;
            this.rightEar.zRot -= 0.4F * k;
        }

        // --- melee: a trunk slap (0.5 s)
        float slap = Anim.seconds(s.slap, s.ageInTicks);
        if (slap >= 0.0F && slap < 0.6F) {
            float wind = Anim.envelope(slap, 0.0F, 0.12F, 0.0F, 0.0F);
            float hit = Anim.envelope(slap, 0.12F, 0.08F, 0.05F, 0.3F);
            this.trunk[0].xRot -= 1.2F * Math.max(wind, hit);
            this.trunk[0].yRot += 0.9F * wind - 1.4F * hit;
            this.trunk[2].xRot += 0.5F * hit;
            this.head.yRot += 0.15F * wind - 0.3F * hit;
        }

        // --- the garden shake (1.2 s): it hunches (anticipation), then shakes itself out like a wet dog,
        // the roll running from the head back to the rump with every part whipping a beat late
        float shakeT = Anim.seconds(s.shake, s.ageInTicks);
        if (shakeT >= 0.0F && shakeT < 1.25F) {
            float hunch = Anim.envelope(shakeT, 0.0F, 0.12F, 0.05F, 0.15F);
            float e = Anim.envelope(shakeT, 0.15F, 0.08F, 0.55F, 0.4F);
            float w = shakeT * 30.0F;
            this.body.y += 1.2F * hunch;
            this.head.xRot += 0.2F * hunch;
            this.body.zRot += Mth.sin(w - 0.6F) * 0.1F * e;
            this.head.zRot = Mth.sin(w) * 0.22F * e;
            this.hump.zRot = Mth.sin(w - 1.2F) * 0.08F * e;
            this.tail.yRot += Mth.sin(w - 1.8F) * 0.9F * e;
            this.leftEar.zRot += Mth.sin(w + 0.4F) * 0.7F * e;
            this.rightEar.zRot += Mth.sin(w + 0.4F) * 0.7F * e;
            for (int i = 0; i < 4; i++) {
                this.trunk[i].zRot += Mth.sin(w - 0.5F * (i + 1)) * 0.3F * e;
                this.legs[i].zRot += (i % 2 == 0 ? -0.08F : 0.08F) * e;
            }
            this.throat.xScale = 1.0F + 0.1F * e;
        }

        // --- the snuffle (2.2 s): head down, the trunk reaches to the flowers at its feet and its
        // tip twitches as it sniffs, ears pricked forward, then a pleased lift of the head
        float sniffT = Anim.seconds(s.sniff, s.ageInTicks);
        if (sniffT >= 0.0F && sniffT < 2.3F) {
            float e = Anim.envelope(sniffT, 0.0F, 0.4F, 1.3F, 0.5F);
            float twitch = Math.max(0.0F, Mth.sin(sniffT * 26.0F)) * Anim.envelope(sniffT, 0.4F, 0.1F, 1.1F, 0.2F);
            float lift = Anim.envelope(sniffT, 1.7F, 0.15F, 0.1F, 0.35F);
            this.head.xRot += 0.4F * e - 0.12F * lift;
            this.body.xRot += 0.06F * e;
            this.trunk[0].xRot += 0.45F * e;
            this.trunk[1].xRot -= 0.25F * e;
            this.trunk[2].xRot -= 0.2F * e + 0.2F * twitch;
            this.trunk[3].xRot -= 0.15F * e + 0.35F * twitch;
            this.trunk[3].xScale = this.trunk[3].zScale = 1.0F + 0.08F * twitch;
            this.leftEar.zRot -= 0.3F * e;
            this.rightEar.zRot += 0.3F * e;
        }

        // --- dancing: bob, sway, wave the trunk, flap the ears, croak to the beat
        if (s.dancing) {
            float beat = s.ageInTicks * 0.55F;
            float bob = Math.abs(Mth.sin(beat));
            float d = Math.min(1.0F, (Stomper.DANCE_LENGTH - s.danceTicks) / 8.0F);
            this.body.y -= bob * 2.2F * d;
            this.body.yScale *= 1.0F + (bob - 0.5F) * 0.08F * d;
            this.body.xScale *= 1.0F - (bob - 0.5F) * 0.05F * d;
            this.body.zRot += Mth.sin(beat * 0.5F) * 0.12F * d;
            this.head.zRot = Mth.sin(beat * 0.5F + 0.6F) * 0.18F * d;
            this.head.xRot -= 0.15F * d;
            this.trunk[0].xRot += (-1.1F + Mth.sin(beat * 0.5F) * 0.4F) * d;
            for (int i = 0; i < 4; i++) {
                this.trunk[i].zRot += Mth.sin(beat * 0.5F - i * 0.9F) * 0.35F * d;
                if (i > 0) {
                    this.trunk[i].xRot += Mth.sin(beat - i) * 0.25F * d;
                }
            }
            this.leftEar.zRot += Mth.sin(beat) * 0.45F * d;
            this.rightEar.zRot -= Mth.sin(beat) * 0.45F * d;
            for (int i = 0; i < 4; i++) {
                float phase = (i == 0 || i == 3) ? 0.0F : PI;
                this.legs[i].zRot += Mth.sin(beat * 0.5F + phase) * 0.12F * d;
                this.legs[i].xRot += Math.max(0.0F, Mth.sin(beat * 0.5F + phase)) * -0.25F * d;
            }
            this.tail.yRot += Mth.sin(beat) * 0.6F * d;
            sac = Math.max(sac, Math.max(0.0F, Mth.sin(beat)) * 0.8F * d);
            this.jaw.xRot += Math.max(0.0F, Mth.sin(beat * 0.5F)) * 0.2F * d;
        }

        // --- the stomp: rear up on the hind legs, then slam down
        float stomp = Anim.seconds(s.stomp, s.ageInTicks);
        if (stomp >= 0.0F && stomp < 1.1F) {
            float rise = stomp < 0.3F ? Anim.smooth(stomp / 0.3F) : 1.0F - Anim.smooth((stomp - 0.3F) / 0.06F);
            float impact = Anim.envelope(stomp, 0.3F, 0.03F, 0.05F, 0.5F);
            this.body.xRot -= 0.4F * rise;
            this.body.y -= 3.5F * rise;
            this.legs[0].xRot -= 0.9F * rise;
            this.legs[1].xRot -= 0.9F * rise;
            this.feet[0].xRot += 0.4F * rise;
            this.feet[1].xRot += 0.4F * rise;
            this.trunk[0].xRot -= 1.6F * rise;
            this.head.xRot -= 0.3F * rise;
            this.jaw.xRot += 0.55F * rise + 0.3F * impact;
            this.leftEar.zRot += 0.6F * rise - 0.4F * impact;
            this.rightEar.zRot -= 0.6F * rise - 0.4F * impact;
            this.body.yScale *= 1.0F - 0.16F * impact;
            this.body.xScale *= 1.0F + 0.1F * impact;
            this.body.zScale *= 1.0F + 0.06F * impact;
            this.hump.yScale *= 1.0F - 0.12F * impact;
            sac = Math.max(sac, impact);
        }

        this.throat.yScale = 1.0F + sac * 0.9F;
        this.throat.xScale = 1.0F + sac * 0.35F;
        this.throat.zScale = 1.0F + sac * 0.45F;

        // --- hurt: a flinch; the trunk curls up and the ears pin back
        if (s.hasRedOverlay) {
            this.head.xRot -= 0.25F;
            for (int i = 1; i < 4; i++) {
                this.trunk[i].xRot -= 0.35F;
            }
            this.leftEar.zRot -= 0.5F;
            this.rightEar.zRot += 0.5F;
            this.jaw.xRot += 0.3F;
        }

        // stacked animations (dance + stomp + hurt) used to swing the jaw far past its hinge, so it
        // looked like it fell off: keep it on the hinge, between shut and a wide gape
        this.jaw.xRot = Mth.clamp(this.jaw.xRot, 0.0F, 0.7F);

        // --- death: keels over onto its side, legs stiff
        float roll = Anim.smooth(s.dying / 14.0F);
        if (roll > 0.0F) {
            this.body.zRot = roll * PI * 0.5F;
            this.body.y += roll * 4.0F;
            this.jaw.xRot = Math.max(this.jaw.xRot, roll * 0.5F);
            for (int i = 0; i < 4; i++) {
                this.legs[i].xRot *= 1.0F - roll;
                this.legs[i].zRot = (i % 2 == 0 ? -0.15F : 0.15F) * roll;
            }
            for (int i = 0; i < 4; i++) {
                this.trunk[i].xRot = Mth.lerp(roll, this.trunk[i].xRot, -0.1F);
            }
        }

        // --- the garden: sways with every heavy step, jiggles on stomps, wiggles when it dances
        float jolt = stomp >= 0.0F ? Anim.envelope(stomp, 0.3F, 0.03F, 0.05F, 0.6F) : 0.0F;
        float puffJolt = puffT >= 0.0F ? Anim.envelope(puffT, 0.0F, 0.06F, 0.0F, 0.5F) : 0.0F;
        float shook = shakeT >= 0.0F && shakeT < 1.25F ? Anim.envelope(shakeT, 0.15F, 0.08F, 0.55F, 0.5F) : 0.0F;
        for (int i = 0; i < this.plants.length; i++) {
            ModelPart p = this.plants[i];
            float ph = i * 1.37F;
            p.zRot = Mth.sin(age * 0.07F + ph) * 0.07F + Mth.sin(pos * 2.0F + ph) * 0.18F * walk + jolt * Mth.sin(s.ageInTicks * 1.9F + ph) * 0.35F
                    + shook * Mth.sin(shakeT * 30.0F - 1.5F - ph * 0.2F) * 0.55F;
            p.xRot = Mth.cos(age * 0.06F + ph) * 0.05F - walk * 0.12F + puffJolt * 0.2F * Mth.sin(ph);
            float bounce = 1.0F + Mth.sin(age * 0.11F + ph) * 0.04F - jolt * 0.3F + puffJolt * 0.12F;
            p.yScale = bounce;
            p.xScale = 1.0F + (1.0F - bounce) * 0.6F;
            p.zScale = p.xScale;
            if (s.dancing) {
                p.zRot += Mth.sin(s.ageInTicks * 0.55F + ph) * 0.3F;
                p.xRot += Mth.cos(s.ageInTicks * 0.55F + ph) * 0.15F;
            }
        }

        // --- babies: a huge head with big eyes and ears, a short trunk, a round body on stubby legs,
        // a tiny garden and a drum on the rump
        this.drum.visible = s.isBaby;
        if (s.isBaby) {
            this.head.xScale = this.head.yScale = this.head.zScale = 1.55F;
            this.leftEye.xScale = this.leftEye.yScale = this.leftEye.zScale = 1.3F;
            this.rightEye.xScale = this.rightEye.yScale = this.rightEye.zScale = 1.3F;
            this.leftEar.xScale = this.leftEar.yScale = this.leftEar.zScale = 1.35F;
            this.rightEar.xScale = this.rightEar.yScale = this.rightEar.zScale = 1.35F;
            this.trunk[0].xScale = this.trunk[0].yScale = this.trunk[0].zScale = 0.7F;
            this.hump.yScale *= 0.6F;
            this.body.zScale *= 0.85F;
            this.body.xScale *= 1.05F;
            for (int i = 0; i < 4; i++) {
                this.legs[i].yScale = 0.62F;
            }
            for (int i = 0; i < this.plants.length; i++) {
                this.plants[i].xScale *= 0.7F;
                this.plants[i].yScale *= 0.7F;
                this.plants[i].zScale *= 0.7F;
            }
            // the shorter legs would leave it floating: it sits that much lower
            if (s.dying <= 0.0F && !s.sitting) {
                this.body.y += 4.9F;
            }
            // the tail swings up over its back and taps the drum on every beat
            float drumT = Anim.seconds(s.drum, s.ageInTicks);
            float rest = 0.9F;
            if (drumT >= 0.0F && drumT < 2.0F) {
                float hold = Anim.envelope(drumT, 0.0F, 0.05F, 0.3F, 1.2F);
                float tap = Anim.envelope(drumT, 0.0F, 0.04F, 0.0F, 0.18F);
                this.tail.xRot = Mth.lerp(hold, this.tail.xRot, 2.1F - 0.25F * tap);
                this.tail.yRot = Mth.lerp(hold, this.tail.yRot, (s.drumBeats % 2 == 0 ? 0.12F : -0.12F));
                this.drum.yScale = 1.0F - 0.12F * tap;
                this.drum.xScale = this.drum.zScale = 1.0F + 0.06F * tap;
                this.head.zRot += (s.drumBeats % 2 == 0 ? 0.08F : -0.08F) * hold;
                this.head.xRot -= 0.1F * tap;
                this.leftEar.zRot += 0.3F * tap;
                this.rightEar.zRot -= 0.3F * tap;
                rest = 0.0F;
            }
            this.drum.xRot = Mth.sin(s.walkAnimationPos * 0.9F) * 0.06F * walk * rest;
        }
        boolean blink = s.expression == Expression.BLINK || s.expression == Expression.SLEEP;
        this.leftEyelid.visible = blink;
        this.rightEyelid.visible = blink;
    }
}
