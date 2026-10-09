package com.thesift.client.model;

import com.thesift.client.renderer.state.NibRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * A Nib, the tiny glowing wisp-butterfly (CAVE: its old design, now in the Sculk-mob pipeline in tools/echoer.py
 * with more moving parts). Resting and swirling are smoothed amounts from the entity, so it never snaps.
 *
 * <ul>
 *   <li>flight: a fast wingbeat, the lower pair a beat behind and each forewing's tip flexing after its root;
 *   the soft body squashes on the upstroke and stretches on the downstroke as it rides each beat, the glowing
 *   tail swings a beat behind that, the antennae and their beads trail after the head, the little legs dangle</li>
 *   <li>at rest on a flower: it settles with a squash, folds its wings up over its back and opens them again,
 *   slowly, now and then, breathing; its legs grip</li>
 *   <li>swirling into treasure: it spins and rolls, its wings a blur; dying, its wings fold and it drops</li>
 * </ul>
 */
public class NibModel extends EntityModel<NibRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart frontLegs;
    private final ModelPart hindLegs;
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[] wingTips = new ModelPart[2];
    private final ModelPart[] lowWings = new ModelPart[2];
    private final ModelPart[] antennae = new ModelPart[2];
    private final ModelPart[] beads = new ModelPart[2];

    public NibModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
        this.frontLegs = this.body.getChild("front_legs");
        this.hindLegs = this.body.getChild("hind_legs");
        for (int k = 0; k < 2; k++) {
            String side = SIDES[k];
            this.wings[k] = this.body.getChild(side + "_wing");
            this.wingTips[k] = this.wings[k].getChild(side + "_wing_tip");
            this.lowWings[k] = this.body.getChild(side + "_wing_low");
            this.antennae[k] = this.head.getChild(side + "_antenna");
            this.beads[k] = this.antennae[k].getChild(side + "_antenna_tip");
        }
    }

    @Override
    public void setupAnim(NibRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.phase * 10.0F;
        float rest = s.rest;
        float swirl = s.swirl * (1.0F - rest);
        float fly = 1.0F - rest;
        float speed = Mth.lerp(swirl, 1.7F, 2.6F);
        float beat = age * speed;

        // --- flight: the beat, the lower pair a beat behind, the tips flexing after the roots
        float flap = (Mth.sin(beat) * 0.95F - 0.2F) * fly;
        float low = (Mth.sin(beat - 0.7F) * 0.8F - 0.1F) * fly;
        float flex = Mth.sin(beat - 1.1F) * 0.45F * fly;
        // the body rides each stroke: up and squashed on the upstroke, down and stretched on the downstroke
        float ride = Mth.cos(beat);
        this.body.y += ride * 0.55F * fly;
        this.body.xRot -= 0.2F * fly;
        float squash = ride * 0.12F * fly;
        // --- settling on a flower: a squash as it lands, then a slow breath
        float land = rest * (1.0F - rest) * 4.0F;
        float breath = Mth.sin(age * 0.09F) * 0.04F * rest;
        squash += 0.22F * land + breath;
        this.body.yScale = 1.0F - squash;
        this.body.xScale = 1.0F + squash * 0.7F;
        this.body.zScale = 1.0F + squash * 0.4F;
        this.body.y += 0.6F * rest + 0.4F * land;
        // the wings fold up over its back and open again, slowly, every few seconds
        float open = Anim.envelope((age % 90.0F) / 20.0F, 2.5F, 0.6F, 0.4F, 0.8F);
        flap += (-1.25F + open * 1.0F) * rest;
        low += (-1.1F + open * 0.9F) * rest;
        flex += -0.15F * open * rest;

        // --- the tail swings a beat behind the body, its tip a beat behind that; it curls when resting
        this.tail.xRot += Mth.sin(beat - 1.4F) * 0.16F * fly + (0.15F + Mth.sin(age * 0.08F) * 0.06F) * rest;
        this.tailTip.xRot += Mth.sin(beat - 2.2F) * 0.22F * fly + 0.2F * rest;
        this.tail.yRot += Mth.sin(age * 0.11F) * 0.1F;
        // --- the legs dangle and trail in flight, and grip the flower at rest
        this.frontLegs.xRot += (0.35F + Mth.sin(beat - 0.9F) * 0.2F) * fly - 0.3F * rest;
        this.hindLegs.xRot += (0.45F + Mth.sin(beat - 1.3F) * 0.2F) * fly + 0.25F * rest;
        // --- the head bobs against the body, the antennae and their beads trail behind
        this.head.xRot += Mth.sin(age * 0.05F) * 0.08F - ride * 0.08F * fly;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.antennae[k].xRot += Mth.sin(age * 0.3F + k) * 0.12F + Mth.sin(beat - 1.6F) * 0.15F * fly;
            this.antennae[k].zRot += Mth.sin(age * 0.17F + k * 1.7F) * 0.08F * sx;
            this.beads[k].xRot += Mth.sin(beat - 2.4F) * 0.3F * fly + Mth.sin(age * 0.3F - 0.8F + k) * 0.15F;
        }

        // --- swirling into treasure: it spins and rolls in the spiral
        this.body.zRot += Mth.sin(age * 0.6F) * 0.4F * swirl;
        this.body.yRot += Mth.sin(age * 0.3F) * 0.5F * swirl;
        // --- dying: the wings fold and it drops (the renderer then pops it)
        float roll = Anim.smooth(s.dying / 10.0F);
        flap = Mth.lerp(roll, flap, -1.2F);
        low = Mth.lerp(roll, low, -1.0F);
        flex = Mth.lerp(roll, flex, -0.3F);
        this.body.zRot += 0.9F * roll;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.wings[k].zRot += sx * flap;
            this.wingTips[k].zRot += sx * flex;
            this.lowWings[k].zRot += sx * low;
        }
    }
}
