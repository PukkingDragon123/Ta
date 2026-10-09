package com.thesift.client.model;

import com.thesift.client.renderer.state.CaravanRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Caravan (geometry in tools/caravans.py): an alien crab with a gem-crusted shell.
 *
 * <ul>
 *   <li>walk: it scuttles sideways (CaravanRenderer turns it side-on as it gets going) on an
 *   alternating four-and-four gait - each leg reaches, plants and pulls - the shell rocking from
 *   side to side; the eyes on their stalks keep looking where it is going</li>
 *   <li>idle: feelers sweep, mouthparts flutter, the pincers flex, an eye stalk now and then
 *   twitches down and pops back up</li>
 *   <li>tap (singing): a wind-up, both feelers flick their glowing beads down onto the crystal and
 *   spring back with an overshoot</li>
 *   <li>mine: the claws rise one after the other and hammer down, pincers snipping</li>
 *   <li>bite: the claws gape wide, lunge and snap shut, then recoil</li>
 *   <li>carrying: both claws held out in front, pincers closed round the ore</li>
 *   <li>offer: it bows before its Queen, claws lowered and opening to lay the ore down</li>
 *   <li>warn: it rears up on its hind legs, claws spread high, and clacks them at the intruder</li>
 *   <li>calm: slow, dreamy feeler waves, swaying like a hum</li>
 *   <li>the gem crust grows with the Caravan: every gem has its own threshold and swells into place</li>
 * </ul>
 * Soldiers carry crystal knuckles on their claws.
 */
public class CaravanModel extends EntityModel<CaravanRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart[] gems = new ModelPart[7];
    private final ModelPart[] stalks = new ModelPart[2];
    private final ModelPart[] feelers = new ModelPart[2];
    private final ModelPart[] feelerTips = new ModelPart[2];
    private final ModelPart[] mouth = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] claws = new ModelPart[2];
    private final ModelPart[] pincers = new ModelPart[2];
    private final ModelPart[] knuckles = new ModelPart[2];
    private final ModelPart[][] legs = new ModelPart[2][4];
    private final ModelPart[][] shins = new ModelPart[2][4];

    public CaravanModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        for (int i = 0; i < this.gems.length; i++) {
            this.gems[i] = this.body.getChild("gem_" + i);
        }
        for (int s = 0; s < 2; s++) {
            String side = SIDES[s];
            this.stalks[s] = this.body.getChild(side + "_eye_stalk");
            this.feelers[s] = this.body.getChild(side + "_feeler");
            this.feelerTips[s] = this.feelers[s].getChild(side + "_feeler_tip");
            this.mouth[s] = this.body.getChild(side + "_mouthpart");
            this.arms[s] = this.body.getChild(side + "_arm");
            this.claws[s] = this.arms[s].getChild(side + "_claw");
            this.pincers[s] = this.claws[s].getChild(side + "_pincer");
            this.knuckles[s] = this.claws[s].getChild(side + "_claw_crystal");
            for (int i = 0; i < 4; i++) {
                this.legs[s][i] = this.body.getChild(side + "_leg_" + i);
                this.shins[s][i] = this.legs[s][i].getChild(side + "_shin_" + i);
            }
        }
    }

    public ModelPart body() {
        return this.body;
    }

    @Override
    public void setupAnim(CaravanRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 1.4F;

        // --- the gem crust grows with the crab: each gem swells in at its own point of growth
        for (int i = 0; i < this.gems.length; i++) {
            float g = Anim.clamp01((s.growth - i * 0.09F) / 0.35F);
            float k = 0.2F + 0.8F * Anim.smooth(g);
            this.gems[i].xScale = k;
            this.gems[i].yScale = k;
            this.gems[i].zScale = k;
        }
        for (int c = 0; c < 2; c++) {
            this.knuckles[c].visible = s.soldier;
        }

        // --- the gait: legs 0 and 2 of one side move with 1 and 3 of the other; each reaches out
        // (lifting at the knee), plants, and pulls the body along
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 1.0F : -1.0F;
            for (int i = 0; i < 4; i++) {
                float phase = pos + (((i + side) % 2 == 0) ? 0.0F : Mth.PI) + i * 0.35F;
                float lift = Math.max(0.0F, Mth.sin(phase));
                float reach = Mth.cos(phase);
                this.legs[side][i].zRot -= lift * 0.45F * walk * sx;
                this.legs[side][i].yRot += reach * 0.22F * walk * sx * (1.0F - 0.6F * s.scuttle);
                this.shins[side][i].zRot += (lift * 0.35F - reach * 0.18F * s.scuttle) * walk * sx;
            }
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.5F * walk;
        this.body.zRot = Mth.sin(pos) * 0.07F * walk;
        this.body.xRot = Mth.cos(pos * 2.0F) * 0.02F * walk;

        // --- eyes: they look where it is going, even scuttling side-on; now and then one twitches
        float look = s.yRot * Anim.DEG * 0.6F - s.scuttle * s.hand * Mth.HALF_PI;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.stalks[k].yRot = look;
            this.stalks[k].xRot += s.xRot * Anim.DEG * 0.3F + Mth.sin(age * 0.11F + k * 2.0F) * 0.05F;
            float twitch = (age + s.seed * 13 + k * 37) % 97.0F;
            if (twitch < 6.0F) {
                this.stalks[k].xRot += Anim.envelope(twitch / 20.0F, 0.0F, 0.05F, 0.05F, 0.2F) * 0.9F;
            }
            this.stalks[k].zRot += Mth.sin(age * 0.07F + k) * 0.06F * sx;
        }

        // --- feelers sweep the air (slow and wide when calm); mouthparts flutter
        float wave = s.calm ? 0.24F : 0.12F;
        float speed = s.calm ? 0.06F : 0.14F;
        for (int k = 0; k < 2; k++) {
            float off = k * 1.7F;
            float sx = k == 0 ? 1.0F : -1.0F;
            this.feelers[k].xRot += Mth.sin(age * speed + off) * wave - walk * 0.2F;
            this.feelers[k].yRot += Mth.cos(age * speed * 0.8F + off) * wave * 0.6F * sx;
            this.feelerTips[k].xRot += Mth.sin(age * speed + off - 0.7F) * wave * 0.8F;
            this.mouth[k].yRot = Mth.sin(age * 0.5F + k * Mth.PI) * 0.18F * sx;
            this.mouth[k].xRot += Mth.sin(age * 0.33F + k) * 0.06F;
        }
        if (s.calm) {
            this.body.zRot += Mth.sin(age * 0.08F) * 0.05F;
        }

        // --- claws: they flex at rest and swing a little with the gait
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.pincers[k].xRot += Mth.sin(age * 0.09F + k * 1.3F) * 0.08F - 0.04F;
            this.arms[k].xRot += Mth.sin(pos + k * Mth.PI) * 0.12F * walk;
            this.arms[k].zRot += Mth.sin(age * 0.05F + k) * 0.03F * sx;
        }

        // --- carrying: both claws out in front, pincers clamped on the ore
        if (s.carrying) {
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot -= 0.25F;
                this.arms[k].yRot += 0.18F * sx;
                this.pincers[k].xRot = 0.12F;
            }
        }

        // --- tap: a small wind-up, the flick down onto the crystal, an overshooting spring back
        float tap = Anim.seconds(s.tap, age);
        if (tap >= 0.0F && tap < 0.4F) {
            float wind = Anim.envelope(tap, 0.0F, 0.06F, 0.0F, 0.06F);
            float flick = Anim.envelope(tap, 0.06F, 0.06F, 0.04F, 0.2F);
            float overshoot = Anim.envelope(tap, 0.2F, 0.06F, 0.0F, 0.14F);
            for (int k = 0; k < 2; k++) {
                this.feelers[k].xRot += -wind * 0.3F + flick * 1.1F - overshoot * 0.25F;
                this.feelerTips[k].xRot += flick * 0.5F;
            }
            this.body.xRot += flick * 0.08F - wind * 0.04F;
        }

        // --- mine: one claw then the other rears up and hammers down, pincers snipping
        float mine = Anim.seconds(s.mine, age);
        if (mine >= 0.0F && mine < 0.4F) {
            for (int k = 0; k < 2; k++) {
                float t = mine - k * 0.08F;
                float rear = Anim.envelope(t, 0.0F, 0.08F, 0.0F, 0.05F);
                float hit = Anim.envelope(t, 0.08F, 0.05F, 0.05F, 0.17F);
                this.arms[k].xRot += -rear * 0.9F + hit * 0.5F;
                this.pincers[k].xRot -= Mth.sin(mine * 40.0F) * 0.4F * hit;
            }
            this.body.xRot += Anim.envelope(mine, 0.1F, 0.05F, 0.05F, 0.15F) * 0.12F;
        }

        // --- bite: claws gape wide, lunge and snap shut, then recoil
        float bite = Anim.seconds(s.bite, age);
        if (bite >= 0.0F && bite < 0.5F) {
            float gape = Anim.envelope(bite, 0.0F, 0.1F, 0.02F, 0.06F);
            float lunge = Anim.envelope(bite, 0.1F, 0.06F, 0.06F, 0.25F);
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].yRot -= gape * 0.45F * sx;
                this.arms[k].xRot -= gape * 0.3F;
                this.pincers[k].xRot -= gape * 0.7F;
                this.arms[k].yRot += lunge * 0.3F * sx;
            }
            this.body.z -= lunge * 1.2F;
            this.body.xRot += lunge * 0.1F;
        }

        // --- offer: a bow before the Queen - claws lowered, pincers opening to lay the ore down
        float offer = Anim.seconds(s.offer, age);
        if (offer >= 0.0F && offer < 0.9F) {
            float bow = Anim.envelope(offer, 0.0F, 0.2F, 0.35F, 0.35F);
            float open = Anim.envelope(offer, 0.2F, 0.1F, 0.25F, 0.2F);
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot += bow * 0.55F;
                this.arms[k].yRot -= open * 0.25F * sx;
                this.pincers[k].xRot -= open * 0.6F;
                this.stalks[k].xRot += bow * 0.35F;
            }
            this.body.xRot += bow * 0.18F;
            this.body.y += bow * 0.6F;
        }

        // --- warn: reared up on the hind legs, claws high and wide, clacking (two quick snaps)
        float warn = Anim.seconds(s.warn, age);
        if (warn >= 0.0F && warn < 1.1F) {
            float rear = Anim.envelope(warn, 0.0F, 0.15F, 0.65F, 0.3F);
            float clack = Mth.abs(Mth.sin(warn * 22.0F)) * Anim.envelope(warn, 0.15F, 0.05F, 0.5F, 0.1F);
            this.body.xRot -= rear * 0.42F;
            this.body.y -= rear * 1.2F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot -= rear * 0.9F;
                this.arms[k].yRot -= rear * 0.5F * sx;
                this.pincers[k].xRot -= (1.0F - clack) * rear * 0.7F;
                this.stalks[k].xRot -= rear * 0.3F;
                this.feelers[k].xRot -= rear * 0.5F;
            }
            for (int side = 0; side < 2; side++) {
                float sx = side == 0 ? 1.0F : -1.0F;
                // the front legs lift off the ground, the hind ones brace
                this.legs[side][0].zRot -= rear * 0.5F * sx;
                this.legs[side][1].zRot -= rear * 0.25F * sx;
                this.legs[side][3].zRot += rear * 0.15F * sx;
            }
        }

        // --- hurt: claws snap up to guard, eyes duck
        if (s.hasRedOverlay) {
            for (int k = 0; k < 2; k++) {
                this.arms[k].xRot -= 0.5F;
                this.stalks[k].xRot += 0.8F;
            }
        }

        // --- dying: the legs curl in
        if (s.dying > 0.0F) {
            float curl = Anim.smooth(s.dying / 10.0F);
            for (int side = 0; side < 2; side++) {
                float sx = side == 0 ? 1.0F : -1.0F;
                for (int i = 0; i < 4; i++) {
                    this.legs[side][i].zRot -= curl * 0.6F * sx;
                    this.shins[side][i].zRot += curl * 0.9F * sx;
                }
            }
        }
    }
}
