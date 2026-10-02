package com.thesift.client.model;

import com.thesift.client.renderer.state.CaravanRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Caravan (geometry in tools/caravans.py): a crystal-backed ant.
 *
 * <ul>
 *   <li>walk: an alternating tripod gait - three legs swing and lift while the other three push -
 *   with the gaster swaying against the step and the head nodding</li>
 *   <li>idle: antennae sweep and feel the air, the gaster breathes slightly</li>
 *   <li>tap (singing): the head dips with a little wind-up, both antennae flick down onto the
 *   crystal and spring back with an overshoot</li>
 *   <li>mine: the head rears, then hammers down while the mandibles scissor</li>
 *   <li>bite: jaws gape, the head lunges and snaps shut, then recoils</li>
 *   <li>carrying: mandibles clamp tight, head held high under the load</li>
 *   <li>calm: slow, dreamy antenna waves</li>
 * </ul>
 * Soldiers wear great crystal jaws instead of mandibles.
 */
public class CaravanModel extends EntityModel<CaravanRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart gaster;
    private final ModelPart[] antenna = new ModelPart[2];
    private final ModelPart[] antennaTip = new ModelPart[2];
    private final ModelPart[] mandible = new ModelPart[2];
    private final ModelPart[] jaw = new ModelPart[2];
    private final ModelPart[][] legs = new ModelPart[2][3];
    private final ModelPart[][] feet = new ModelPart[2][3];

    public CaravanModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.gaster = this.body.getChild("gaster");
        String[] sides = {"left", "right"};
        for (int s = 0; s < 2; s++) {
            this.antenna[s] = this.head.getChild(sides[s] + "_antenna");
            this.antennaTip[s] = this.antenna[s].getChild(sides[s] + "_antenna_tip");
            this.mandible[s] = this.head.getChild(sides[s] + "_mandible");
            this.jaw[s] = this.head.getChild(sides[s] + "_crystal_jaw");
            for (int i = 0; i < 3; i++) {
                this.legs[s][i] = this.body.getChild(sides[s] + "_leg_" + i);
                this.feet[s][i] = this.legs[s][i].getChild(sides[s] + "_foot_" + i);
            }
        }
    }

    public ModelPart body() {
        return this.body;
    }

    public ModelPart head() {
        return this.head;
    }

    @Override
    public void setupAnim(CaravanRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 1.6F;
        boolean soldier = s.soldier;
        for (int k = 0; k < 2; k++) {
            this.mandible[k].visible = !soldier;
            this.jaw[k].visible = soldier;
        }

        // --- the tripod gait: L0, R1, L2 move together, then R0, L1, R2
        for (int side = 0; side < 2; side++) {
            float sx = side == 0 ? 1.0F : -1.0F;
            for (int i = 0; i < 3; i++) {
                float phase = pos + (((i + side) % 2 == 0) ? 0.0F : Mth.PI);
                float swing = Mth.cos(phase) * 0.45F * walk;
                float lift = Math.max(0.0F, Mth.sin(phase)) * 0.4F * walk;
                this.legs[side][i].yRot += swing * sx;
                this.legs[side][i].zRot -= lift * sx;
                this.feet[side][i].zRot += lift * 0.6F * sx;
            }
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.4F * walk;
        this.body.zRot = Mth.sin(pos) * 0.04F * walk;
        // follow-through: the heavy gaster lags behind the body's roll and sways
        this.gaster.yRot = -Mth.sin(pos - 0.6F) * 0.12F * walk + Mth.sin(age * 0.07F) * 0.03F;
        this.gaster.xRot += Mth.sin(age * 0.09F) * 0.025F - Math.abs(Mth.cos(pos)) * 0.05F * walk;

        // --- the head follows the look, nodding with each step
        this.head.yRot = s.yRot * Anim.DEG * 0.6F;
        this.head.xRot = s.xRot * Anim.DEG * 0.4F + Mth.sin(pos * 2.0F) * 0.05F * walk;

        // --- antennae feel the air (calm Caravans wave them slowly and widely)
        float wave = s.calm ? 0.22F : 0.1F;
        float speed = s.calm ? 0.06F : 0.13F;
        for (int k = 0; k < 2; k++) {
            float off = k * 1.7F;
            this.antenna[k].xRot += Mth.sin(age * speed + off) * wave - walk * 0.25F;
            this.antenna[k].zRot += Mth.cos(age * speed * 0.8F + off) * wave * 0.6F * (k == 0 ? 1.0F : -1.0F);
            this.antennaTip[k].xRot += Mth.sin(age * speed + off - 0.7F) * wave * 0.8F;
        }

        // --- carrying: mandibles clamp, head carried high
        if (s.carrying) {
            this.head.xRot -= 0.25F;
            this.mandible[0].yRot += 0.3F;
            this.mandible[1].yRot -= 0.3F;
        }

        // --- tap: a small wind-up, the flick down onto the crystal, an overshooting spring back
        float tap = Anim.seconds(s.tap, age);
        if (tap >= 0.0F && tap < 0.4F) {
            float wind = Anim.envelope(tap, 0.0F, 0.06F, 0.0F, 0.06F);
            float flick = Anim.envelope(tap, 0.06F, 0.06F, 0.04F, 0.2F);
            float overshoot = Anim.envelope(tap, 0.2F, 0.06F, 0.0F, 0.14F);
            for (int k = 0; k < 2; k++) {
                this.antenna[k].xRot += -wind * 0.3F + flick * 1.25F - overshoot * 0.25F;
                this.antennaTip[k].xRot += flick * 0.5F;
            }
            this.head.xRot += flick * 0.3F - wind * 0.1F;
        }

        // --- mine: rear up, then hammer down, mandibles scissoring
        float mine = Anim.seconds(s.mine, age);
        if (mine >= 0.0F && mine < 0.35F) {
            float rear = Anim.envelope(mine, 0.0F, 0.08F, 0.0F, 0.05F);
            float hit = Anim.envelope(mine, 0.08F, 0.05F, 0.05F, 0.17F);
            this.head.xRot += -rear * 0.35F + hit * 0.6F;
            this.body.xRot += hit * 0.12F;
            float snip = Mth.sin(mine * 40.0F) * 0.35F * hit;
            this.mandible[0].yRot -= snip;
            this.mandible[1].yRot += snip;
            this.jaw[0].yRot -= snip;
            this.jaw[1].yRot += snip;
        }

        // --- bite: gape, lunge and snap, recoil
        float bite = Anim.seconds(s.bite, age);
        if (bite >= 0.0F && bite < 0.5F) {
            float gape = Anim.envelope(bite, 0.0F, 0.1F, 0.02F, 0.06F);
            float lunge = Anim.envelope(bite, 0.1F, 0.06F, 0.06F, 0.25F);
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.mandible[k].yRot -= gape * 0.7F * sx;
                this.jaw[k].yRot -= gape * 0.6F * sx;
            }
            this.head.z -= lunge * 1.6F;
            this.head.xRot += lunge * 0.2F - gape * 0.15F;
            this.body.z -= lunge * 0.8F;
        }

        // --- build: the gaster lifts and the head presses the crystal into place
        float build = Anim.seconds(s.build, age);
        if (build >= 0.0F && build < 0.8F) {
            float press = Anim.envelope(build, 0.0F, 0.15F, 0.3F, 0.3F);
            this.head.xRot += press * 0.5F;
            this.gaster.xRot -= press * 0.35F;
            this.body.y += press * 0.6F;
        }
    }
}
