package com.thesift.client.model.boss;

import com.thesift.client.Expression;
import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Resonator: a haughty diva of a violin on spider legs.
 *
 * <ul>
 *   <li>idle: sways like a dancer, tilts its scroll of a head, idly bows a slow note, the strings hum</li>
 *   <li>move: tiptoes on its four long legs, neck held high</li>
 *   <li>angry: the head lowers into a glare, the tuning pegs wind tight</li>
 *   <li>attack: draws the bow back across its strings (the wind-up), then snaps it out</li>
 *   <li>hurt: recoils, head thrown back, pegs spin</li>
 * </ul>
 */
public class ResonatorModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart frame;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart rightForearm;
    private final ModelPart bow;
    private final ModelPart leftPegs;
    private final ModelPart rightPegs;
    private final ModelPart[] legs = new ModelPart[4];

    public ResonatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.frame = this.body.getChild("frame");
        this.neck = this.frame.getChild("neck");
        this.head = this.neck.getChild("head");
        this.leftArm = this.frame.getChild("left_arm");
        this.rightArm = this.frame.getChild("right_arm");
        this.rightForearm = this.rightArm.getChild("right_forearm");
        this.bow = this.rightForearm.getChild("bow");
        this.leftPegs = this.head.getChild("left_pegs");
        this.rightPegs = this.head.getChild("right_pegs");
        String[] names = {"front_left", "front_right", "hind_left", "hind_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild(names[i] + "_leg");
        }
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.8F);
        float pos = s.walkAnimationPos * 1.4F;
        float rest = 1.0F - walk;
        boolean angry = s.expression == Expression.ANGRY || s.windingUp;

        // --- the dancer's sway and the humming strings
        float sway = Mth.sin(age * 0.05F);
        this.frame.zRot = sway * 0.08F * rest;
        this.frame.xScale = 1.0F + Mth.sin(age * 1.7F) * 0.012F;
        this.neck.zRot = -sway * 0.12F * rest;
        this.head.zRot = Mth.sin(age * 0.06F + 0.8F) * 0.22F * rest;
        this.head.yRot = s.yRot * Anim.DEG * 0.6F;
        this.head.xRot = s.xRot * Anim.DEG * 0.4F + (angry ? 0.35F : -0.12F);
        this.body.yRot = s.yRot * Anim.DEG * 0.2F;
        this.leftPegs.xRot = angry ? age * 0.4F : 0.0F;
        this.rightPegs.xRot = angry ? -age * 0.4F : 0.0F;

        // --- tiptoe gait
        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : Mth.PI;
            this.legs[i].xRot += Mth.sin(pos + phase) * 0.5F * walk;
            this.legs[i].yRot = Mth.cos(pos + phase) * 0.3F * walk;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.8F * walk;

        // --- idly bowing a long slow note across its own strings
        float bowing = Mth.sin(age * 0.07F);
        this.rightArm.xRot = -0.9F * rest + bowing * 0.25F * rest;
        this.rightArm.zRot += 0.25F * rest;
        this.rightForearm.zRot += -0.6F * rest + bowing * 0.3F * rest;
        this.leftArm.zRot += Mth.sin(age * 0.08F) * 0.06F;
        this.leftArm.xRot = -0.4F * rest;

        // --- attack: draw the bow back... and snap it out
        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 1.1F) {
            float draw = Anim.envelope(t, 0.0F, 0.6F, 0.1F, 0.06F);
            float snap = Anim.envelope(t, 0.7F, 0.05F, 0.1F, 0.25F);
            this.rightArm.xRot += 1.2F * draw - 1.4F * snap;
            this.rightArm.zRot -= 0.6F * draw;
            this.rightForearm.zRot += 0.9F * draw - 1.2F * snap;
            this.bow.zRot = 1.25F + 0.4F * draw - 0.5F * snap;
            this.frame.xRot += -0.25F * draw + 0.2F * snap;
            this.neck.xRot += -0.3F * draw + 0.3F * snap;
            float buzz = Mth.sin(age * 4.0F) * 0.08F * (snap + draw * 0.3F);
            this.frame.xScale *= 1.0F + buzz;
            this.leftArm.zRot += buzz * 3.0F;
        }
        if (s.hasRedOverlay) {
            this.head.xRot -= 0.6F;
            this.neck.xRot -= 0.3F;
            this.frame.xScale *= 1.06F;
            this.leftPegs.xRot += age * 0.8F;
            this.rightPegs.xRot -= age * 0.8F;
        }
    }
}
