package com.thesift.client.model;

import com.thesift.client.renderer.state.SlumblerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Slumbler (geometry in tools/slumbler.py): a big, sleepy salamander whose back is a carved wooden
 * instrument, in rainbow scales.
 *
 * <ul>
 *   <li>always: the frilled gills flutter (a quick shiver over a slow breath), the fins along the
 *   tail ripple in a wave from root to tip, the leg fins fan, the string whiskers sway a beat
 *   behind the head</li>
 *   <li>walk: a sprawling lizard gait, diagonal legs together, the body and tail swinging</li>
 *   <li>swim (in Chrome): legs folded back, the whole body and tail undulating, gills and whiskers
 *   streaming back</li>
 *   <li>nap: head down, eyes shut, gills rising and falling with slow breaths, whiskers draped on the
 *   ground, tail curled; afloat in the shallows, legs drift out</li>
 *   <li>spit: it rears and draws a breath, gills puffed, then whips its head forward and spits a gob
 *   of Chrome, jaw wide</li>
 *   <li>laying eggs: it settles low and still, the tail lifts and quivers, and it rises again</li>
 *   <li>yawn, bite, gulp, nuzzle and hum, a wet shake after leaving the Chrome, a flinch when hurt and
 *   a slow roll belly-up when it dies</li>
 * </ul>
 */
public class SlumblerModel extends EntityModel<SlumblerRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEyelid;
    private final ModelPart rightEyelid;
    private final ModelPart[][] gills = new ModelPart[2][3];
    private final ModelPart[][] whiskers = new ModelPart[2][2];
    private final ModelPart[][] whiskerTips = new ModelPart[2][2];
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];
    private final ModelPart[] legFins = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[3];
    private final ModelPart[] tailFins = new ModelPart[3];
    private final ModelPart[] keels = new ModelPart[3];
    private final ModelPart scroll;

    public SlumblerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftEyelid = this.head.getChild("left_eye").getChild("left_eyelid");
        this.rightEyelid = this.head.getChild("right_eye").getChild("right_eyelid");
        for (int s = 0; s < 2; s++) {
            for (int i = 0; i < 3; i++) {
                this.gills[s][i] = this.head.getChild(SIDES[s] + "_gill_" + i);
            }
            for (int i = 0; i < 2; i++) {
                this.whiskers[s][i] = this.head.getChild(SIDES[s] + "_whisker_" + i);
                this.whiskerTips[s][i] = this.whiskers[s][i].getChild(SIDES[s] + "_whisker_tip_" + i);
            }
        }
        String[] legNames = {"left_front", "right_front", "left_hind", "right_hind"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(legNames[i] + "_leg");
            this.feet[i] = this.legs[i].getChild(legNames[i] + "_foot");
            this.legFins[i] = this.legs[i].getChild(legNames[i] + "_fin");
        }
        this.tail[0] = this.body.getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail3");
        for (int i = 0; i < 3; i++) {
            this.tailFins[i] = this.tail[i].getChild("tail_fin_" + i);
            this.keels[i] = this.tail[i].getChild("tail_keel_" + i);
        }
        this.scroll = this.tail[2].getChild("scroll");
    }

    @Override
    public void setupAnim(SlumblerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 0.55F;
        boolean swimming = s.inChrome && !s.sleeping;

        // --- always alive: gills shiver over a slow breath, fins ripple root to tip, whiskers sway behind the head
        float breath = Mth.sin(age * (s.sleeping ? 0.05F : 0.08F));
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 1.0F : -1.0F;
            for (int i = 0; i < 3; i++) {
                float flutter = Mth.sin(age * (s.sleeping ? 0.12F : 0.42F) + i * 1.3F + side * 0.7F);
                this.gills[side][i].xRot += flutter * (s.sleeping ? 0.04F : 0.1F) + breath * 0.06F;
                this.gills[side][i].zRot += (Mth.sin(age * 0.23F + i * 0.9F) * 0.07F + breath * 0.08F) * sx;
                this.gills[side][i].yRot += Mth.sin(age * 0.11F + i) * 0.05F * sx;
            }
            for (int i = 0; i < 2; i++) {
                float ph = age * 0.06F + i * 1.1F + side * 0.4F;
                this.whiskers[side][i].yRot += Mth.sin(ph) * 0.1F * sx;
                this.whiskers[side][i].xRot += Mth.sin(ph * 0.8F + 0.5F) * 0.06F;
                this.whiskerTips[side][i].yRot += Mth.sin(ph - 0.9F) * 0.16F * sx;
                this.whiskerTips[side][i].xRot += Mth.sin(ph * 0.8F - 0.4F) * 0.1F;
            }
        }
        float ripple = swimming ? 0.32F : 0.16F;
        for (int i = 0; i < 3; i++) {
            this.tailFins[i].zRot = Mth.sin(age * ripple - i * 0.9F) * (0.1F + 0.04F * i);
            this.keels[i].zRot = Mth.sin(age * ripple - i * 0.9F + Mth.PI) * (0.08F + 0.03F * i);
        }
        for (int i = 0; i < 4; i++) {
            this.legFins[i].xRot += Mth.sin(age * 0.25F + i * 1.7F) * 0.14F;
        }

        // --- the sprawling lizard gait: diagonal legs move together, body and tail swing
        float sw = Mth.sin(pos);
        float cw = Mth.cos(pos);
        this.body.yRot = sw * 0.12F * walk;
        this.body.zRot = cw * 0.04F * walk;
        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : Mth.PI;
            float sgn = i % 2 == 0 ? 1.0F : -1.0F;
            this.legs[i].yRot = Mth.sin(pos + phase) * 0.55F * walk * sgn;
            this.legs[i].zRot = -sgn * Math.max(0.0F, Mth.cos(pos + phase)) * 0.35F * walk;
            this.feet[i].zRot = sgn * Math.max(0.0F, Mth.cos(pos + phase)) * 0.3F * walk;
        }
        float tailSwing = 0.08F + walk * 0.25F;
        for (int i = 0; i < 3; i++) {
            this.tail[i].yRot = Mth.sin(pos - 0.9F * (i + 1)) * tailSwing * walk + Mth.sin(age * 0.05F - i * 0.7F) * 0.08F;
            this.tail[i].xRot = Mth.sin(age * 0.07F - i) * 0.02F;
        }
        this.scroll.yRot = this.tail[2].yRot * 0.5F;

        // --- swimming in Chrome: legs folded back, the body and tail undulate, gills and whiskers stream back
        if (swimming) {
            float t = age * 0.22F + s.walkAnimationPos * 0.4F;
            float amp = 0.5F + 0.5F * walk;
            this.body.yRot = Mth.sin(t) * 0.1F * amp;
            this.body.zRot = Mth.sin(t + 0.6F) * 0.05F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot = Mth.sin(t - (i + 1) * 0.9F) * (0.22F + 0.1F * i) * amp;
            }
            for (int i = 0; i < 4; i++) {
                float sgn = i % 2 == 0 ? 1.0F : -1.0F;
                boolean front = i < 2;
                this.legs[i].yRot = (front ? -0.9F : 0.8F) * sgn + Mth.sin(t + i) * 0.08F;
                this.legs[i].zRot = -sgn * 0.12F;
                this.feet[i].zRot = sgn * 1.1F;
            }
            for (int side = 0; side < 2; side++) {
                float sx = side == 0 ? 1.0F : -1.0F;
                for (int i = 0; i < 3; i++) {
                    this.gills[side][i].yRot -= 0.45F * sx;
                }
                for (int i = 0; i < 2; i++) {
                    this.whiskers[side][i].xRot -= 0.45F;
                    this.whiskers[side][i].yRot -= 0.25F * sx;
                }
            }
            this.head.yRot -= this.body.yRot * 0.6F;
        }

        // --- head look
        this.head.yRot += s.yRot * Anim.DEG * 0.5F;
        this.head.xRot = s.xRot * Anim.DEG * 0.4F;

        // --- napping: head down, eyes shut, slow breaths, whiskers draped, tail curled
        if (s.sleeping) {
            this.head.xRot = 0.14F;
            this.head.yRot = 0.0F;
            for (int i = 0; i < 4; i++) {
                float sgn = i % 2 == 0 ? 1.0F : -1.0F;
                this.legs[i].zRot = -sgn * 0.45F;
                this.feet[i].zRot = sgn * 0.9F;
            }
            this.body.y += 2.5F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot = 0.35F + Mth.sin(age * 0.03F - i) * 0.03F;
            }
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < 2; i++) {
                    this.whiskers[side][i].xRot += 0.35F;
                    this.whiskerTips[side][i].xRot += 0.5F;
                }
            }
            this.body.yScale = 1.0F + breath * 0.02F;
            if (s.inChrome) {
                // half-submerged in the shallows: legs float out, the tail drifts, chin on the surface
                this.body.y += 3.0F;
                this.head.xRot = -0.1F + breath * 0.03F;
                for (int i = 0; i < 4; i++) {
                    float sgn = i % 2 == 0 ? 1.0F : -1.0F;
                    this.legs[i].zRot = -sgn * 0.15F;
                    this.legs[i].yRot = sgn * (i < 2 ? 0.7F : -0.5F);
                }
                for (int i = 0; i < 3; i++) {
                    this.tail[i].yRot = Mth.sin(age * 0.03F - i * 0.8F) * 0.2F;
                }
            }
        }

        // --- gulping plankton (2.2 s): the head dips into the Chrome, the jaw scoops, a big swallow
        float gulp = Anim.seconds(s.gulp, s.ageInTicks);
        float gulpOpen = 0.0F;
        if (gulp >= 0.0F && gulp < 2.3F) {
            float dip = Anim.envelope(gulp, 0.0F, 0.4F, 0.9F, 0.6F);
            gulpOpen = Anim.envelope(gulp, 0.3F, 0.2F, 0.3F, 0.15F);
            float swallow = Anim.envelope(gulp, 1.15F, 0.12F, 0.1F, 0.4F);
            this.head.xRot += 0.5F * dip;
            this.body.xRot = 0.08F * dip;
            this.flareGills(0.35F * swallow);
            this.body.xScale = 1.0F + 0.05F * swallow;
        }

        // --- nuzzle: a slow sideways rub of the snout
        float nuzzle = Anim.seconds(s.nuzzle, s.ageInTicks);
        if (nuzzle >= 0.0F && nuzzle < 2.0F) {
            float e = Anim.envelope(nuzzle, 0.0F, 0.3F, 1.2F, 0.4F);
            this.head.yRot += Mth.sin(nuzzle * 4.0F) * 0.3F * e;
            this.head.zRot = Mth.sin(nuzzle * 4.0F) * 0.15F * e;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot += Mth.sin(nuzzle * 5.0F - i) * 0.3F * e;
            }
        }

        // --- humming along to music: the head sways, the jaw half-opens, the gills fan and the fins quiver with the note
        float hum = Anim.seconds(s.hum, s.ageInTicks);
        float humOpen = 0.0F;
        if (hum >= 0.0F && hum < 1.6F) {
            float e = Anim.envelope(hum, 0.0F, 0.2F, 0.8F, 0.5F);
            humOpen = 0.3F * e + Mth.sin(hum * 12.0F) * 0.04F * e;
            this.head.zRot += Mth.sin(hum * 3.5F) * 0.12F * e;
            this.head.xRot -= 0.15F * e;
            this.flareGills(0.4F * e);
            for (int i = 0; i < 3; i++) {
                this.tailFins[i].zRot += Mth.sin(hum * 18.0F - i) * 0.08F * e;
            }
        }

        // --- the wet shake after climbing out of the Chrome: a shudder from head to tail, fast and dying away
        float shake = Anim.seconds(s.shake, s.ageInTicks);
        if (shake >= 0.0F && shake < 1.2F) {
            float e = Anim.envelope(shake, 0.0F, 0.08F, 0.5F, 0.6F);
            float w = Mth.sin(shake * 34.0F) * e;
            this.body.zRot += w * 0.22F;
            this.head.zRot -= w * 0.3F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot += Mth.sin(shake * 34.0F - (i + 1) * 0.8F) * 0.3F * e;
            }
            for (int side = 0; side < 2; side++) {
                for (int i = 0; i < 2; i++) {
                    this.whiskerTips[side][i].yRot += w * 0.6F;
                }
                for (int i = 0; i < 3; i++) {
                    this.gills[side][i].xRot += w * 0.35F;
                }
            }
        }

        // --- spit: rear back and draw a breath (gills puffed) ... whip the head forward, jaw wide, and spit
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        float spitOpen = 0.0F;
        if (spit >= 0.0F && spit < 1.1F) {
            float draw = Anim.envelope(spit, 0.0F, 0.45F, 0.1F, 0.08F);
            float whip = Anim.envelope(spit, 0.55F, 0.07F, 0.1F, 0.35F);
            spitOpen = Anim.envelope(spit, 0.55F, 0.05F, 0.12F, 0.25F);
            this.head.xRot += -draw * 0.35F + whip * 0.25F;
            this.body.xRot -= draw * 0.06F;
            this.body.z += draw * 0.8F - whip * 1.2F;
            this.flareGills(0.6F * draw);
            this.body.xScale *= 1.0F + 0.03F * draw;
        }

        // --- laying eggs: it settles low and still, the tail lifts and quivers, then it rises again
        float lay = Anim.seconds(s.lay, s.ageInTicks);
        if (lay >= 0.0F && lay < 2.5F) {
            float low = Anim.envelope(lay, 0.0F, 0.4F, 1.4F, 0.6F);
            float quiver = Mth.sin(lay * 30.0F) * Anim.envelope(lay, 0.5F, 0.2F, 0.9F, 0.3F);
            this.body.y += low * 2.0F;
            this.head.xRot += low * 0.2F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].xRot += low * 0.12F;
                this.tail[i].yRot += quiver * 0.06F * (i + 1);
            }
            this.flareGills(0.3F * low);
        }

        // --- yawn: a slow, huge stretch (2.4 s); bite: a quick snap (0.45 s)
        float yawn = Anim.seconds(s.yawn, s.ageInTicks);
        float yawnOpen = yawn >= 0 ? Anim.envelope(yawn, 0.1F, 0.7F, 0.6F, 0.8F) : 0.0F;
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        float biteOpen = bite >= 0 ? Anim.envelope(bite, 0.0F, 0.12F, 0.05F, 0.15F) : 0.0F;
        float open = Math.max(Math.max(yawnOpen * 0.95F, biteOpen * 0.75F), Math.max(Math.max(gulpOpen * 0.7F, humOpen), spitOpen * 0.8F));
        this.jaw.xRot += open;
        this.head.xRot -= yawnOpen * 0.35F - biteOpen * 0.2F;
        this.flareGills(0.45F * yawnOpen);
        this.body.zScale = 1.0F + yawnOpen * 0.04F;
        for (int side = 0; side < 2; side++) {
            for (int i = 0; i < 2; i++) {
                this.whiskers[side][i].xRot -= yawnOpen * 0.3F;
            }
        }

        // --- hurt: the head jerks up, the body flinches and the tail whips
        if (s.hasRedOverlay) {
            this.head.xRot -= 0.3F;
            this.jaw.xRot += 0.35F;
            this.body.yScale *= 0.92F;
            this.body.xScale *= 1.05F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot += 0.35F * (i + 1) * Mth.sin(age * 1.4F);
            }
            this.flareGills(0.5F);
        }

        // --- death: rolls belly-up, legs stiff in the air, jaw lolling open
        float roll = Anim.smooth(s.dying / 14.0F);
        if (roll > 0.0F) {
            this.body.zRot = roll * Mth.PI;
            this.body.y += roll * 5.0F;
            this.jaw.xRot = Math.max(this.jaw.xRot, roll * 0.5F);
            for (int i = 0; i < 4; i++) {
                float sgn = i % 2 == 0 ? 1.0F : -1.0F;
                this.legs[i].yRot *= 1.0F - roll;
                this.legs[i].zRot = -sgn * 0.2F * roll + Mth.sin(age * 1.8F + i) * 0.08F * (1.0F - roll);
                this.feet[i].zRot = sgn * 0.15F * roll;
            }
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot *= 1.0F - roll;
                this.tail[i].xRot = -0.15F * roll;
            }
        }

        boolean eyesShut = s.sleeping || yawnOpen > 0.55F || s.expression == com.thesift.client.Expression.BLINK || roll > 0.6F;
        this.leftEyelid.visible = eyesShut;
        this.rightEyelid.visible = eyesShut;
    }

    /** Fans the frilled gills out from the head. */
    private void flareGills(float amount) {
        if (amount == 0.0F) {
            return;
        }
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 1.0F : -1.0F;
            for (int i = 0; i < 3; i++) {
                this.gills[side][i].yRot += amount * 0.5F * sx;
                this.gills[side][i].zRot += amount * (i - 1) * 0.5F * sx;
            }
        }
    }
}
