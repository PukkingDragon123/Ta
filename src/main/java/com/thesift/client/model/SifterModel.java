package com.thesift.client.model;

import com.thesift.client.renderer.state.SifterRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Sifter: a box-headed dune lurker. The head is a tray-like lower jaw with a heavy lid hinged at
 * the back; it waddles on two stubby legs with little fins flapping at its sides.
 *
 * <ul>
 *   <li>idle: the lid slowly "breathes" open and shut, fins sway</li>
 *   <li>move: waddling legs, bobbing head, the lid clacks with every step</li>
 *   <li>attack: the lid flies open wide and slams shut while the body lunges</li>
 *   <li>hurt: the lid pops open and the head jerks back</li>
 *   <li>burrowed: sunk into the sand so only the lid peeks out; bursts up on emerging</li>
 * </ul>
 */
public class SifterModel extends EntityModel<SifterRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart lid;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftFin;
    private final ModelPart rightFin;

    public SifterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.lid = this.head.getChild("lid");
        this.leftLeg = this.body.getChild("left_leg");
        this.rightLeg = this.body.getChild("right_leg");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
    }

    @Override
    public void setupAnim(SifterRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
        float pos = s.walkAnimationPos * 1.1F;

        float sq = Mth.clamp(s.squash, -0.4F, 0.5F);
        this.body.yScale = 1.0F + sq * 0.6F;
        this.body.xScale = 1.0F - sq * 0.3F;
        this.body.zScale = 1.0F - sq * 0.3F;

        // --- waddle
        float swing = Mth.cos(pos) * 0.75F * walk;
        this.leftLeg.xRot = swing;
        this.rightLeg.xRot = -swing;
        this.body.zRot = Mth.sin(pos) * 0.07F * walk;
        this.body.y -= Math.abs(Mth.sin(pos)) * 1.0F * walk;
        this.head.yRot = s.yRot * Anim.DEG * 0.5F;
        this.head.xRot = s.xRot * Anim.DEG * 0.3F + Mth.sin(pos * 2.0F) * 0.05F * walk;

        // --- lid: slow breathing at rest, clacking with each step
        float breathe = Mth.sin(age * 0.05F);
        float open = (float) Math.pow(Math.max(0.0F, breathe), 3) * 0.22F * (1.0F - walk);
        open += Math.abs(Mth.sin(pos)) * 0.16F * walk;

        // --- fins flap
        this.leftFin.zRot += -Mth.sin(age * 0.12F) * 0.08F - Math.abs(Mth.sin(pos)) * 0.35F * walk;
        this.rightFin.zRot += Mth.sin(age * 0.12F + 1.0F) * 0.08F + Math.abs(Mth.sin(pos)) * 0.35F * walk;

        // --- bite: fly open, slam shut, lunge
        float chomp = Anim.seconds(s.chomp, age);
        if (chomp >= 0) {
            float wide = Anim.envelope(chomp, 0.0F, 0.12F, 0.08F, 0.12F);
            open += wide * 1.15F;
            this.body.xRot += wide * 0.25F;
            this.body.z -= wide * 1.5F;
        }
        // --- hurt: lid pops, head jerks back
        if (s.hasRedOverlay) {
            open += 0.5F;
            this.head.xRot -= 0.25F;
        }
        this.lid.xRot = -open;

        // --- burrowed: only the lid peeks out of the sand
        float emerge = Anim.seconds(s.emerge, age);
        if (s.burrowed) {
            this.body.y += 16.5F;
            this.lid.xRot = -Math.max(0.0F, Mth.sin(age * 0.03F)) * 0.12F;
        } else if (emerge >= 0 && emerge < 0.6F) {
            float t = Anim.backOut(emerge / 0.45F);
            this.body.y += 16.5F * (1.0F - t);
            this.lid.xRot -= (1.0F - t) * 0.6F;
        }
    }
}
