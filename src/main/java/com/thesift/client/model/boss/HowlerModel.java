package com.thesift.client.model.boss;

import com.thesift.client.Expression;
import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Howler: a cheeky loudmouth of a pipe organ.
 *
 * <ul>
 *   <li>idle: the bellows body breathes in and out, the pipes bob in turn, ears swing, now and then
 *   it puffs its cheeks out like it is about to say something</li>
 *   <li>move: a bouncy trot, ears flapping</li>
 *   <li>angry: head down, ears pinned back</li>
 *   <li>attack: a huge breath in (body and cheeks swell, pipes rise and glow), then a blast - the
 *   bell flares, the head kicks back, ears fly</li>
 *   <li>hurt: deflates with a wheeze</li>
 * </ul>
 */
public class HowlerModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart bell;
    private final ModelPart tail;
    private final ModelPart leftCheek;
    private final ModelPart rightCheek;
    private final ModelPart leftEar;
    private final ModelPart rightEar;
    private final ModelPart[] pipes = new ModelPart[7];
    private final ModelPart[] legs = new ModelPart[4];

    public HowlerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.bell = this.head.getChild("bell");
        this.leftCheek = this.head.getChild("left_cheek");
        this.rightCheek = this.head.getChild("right_cheek");
        this.leftEar = this.head.getChild("left_ear");
        this.rightEar = this.head.getChild("right_ear");
        this.tail = this.body.getChild("tail");
        ModelPart pipeRoot = this.body.getChild("pipes");
        for (int i = 0; i < 7; i++) {
            this.pipes[i] = pipeRoot.getChild("pipe_" + i);
        }
        String[] names = {"front_left", "front_right", "hind_left", "hind_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = root.getChild(names[i] + "_leg");
        }
    }

    @Override
    public void setupAnim(MinionRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 0.8F;
        boolean angry = s.expression == Expression.ANGRY || s.windingUp;

        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : Mth.PI;
            this.legs[i].xRot = Mth.cos(pos + phase) * 0.7F * walk;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 1.0F * walk;
        // bellows breathing
        float breath = Mth.sin(age * 0.08F);
        this.body.zScale = 1.0F + breath * 0.04F;
        this.body.yScale = 1.0F + breath * 0.02F;
        this.head.yRot = s.yRot * Anim.DEG * 0.7F;
        this.head.xRot = s.xRot * Anim.DEG * 0.5F + Mth.sin(age * 0.05F) * 0.06F + (angry ? 0.25F : 0.0F);
        this.tail.yRot = Mth.sin(age * 0.1F) * 0.2F;
        for (int i = 0; i < 7; i++) {
            this.pipes[i].y -= Math.max(0.0F, Mth.sin(age * 0.09F + i * 0.9F)) * 0.6F;
        }
        // floppy ears: swing with the trot, pinned back when angry
        float flap = Mth.sin(pos * 2.0F) * 0.4F * walk + Mth.sin(age * 0.07F) * 0.08F;
        this.leftEar.zRot += flap + (angry ? -0.5F : 0.0F);
        this.rightEar.zRot -= flap + (angry ? -0.5F : 0.0F);
        this.leftEar.xRot = angry ? 0.6F : 0.0F;
        this.rightEar.xRot = angry ? 0.6F : 0.0F;
        // now and then it puffs its cheeks out, cheeky
        float puff = Anim.envelope(Mth.positiveModulo(age + 31.0F, 160.0F), 0.0F, 6.0F, 10.0F, 5.0F);
        float cheek = 1.0F + 0.5F * puff;

        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 2.2F) {
            float inhale = Anim.envelope(t, 0.0F, 1.4F, 0.1F, 0.08F);
            float blast = Anim.envelope(t, 1.5F, 0.06F, 0.35F, 0.3F);
            this.body.xScale = 1.0F + 0.14F * inhale - 0.06F * blast;
            this.body.yScale = 1.0F + 0.12F * inhale;
            this.body.zScale = 1.0F + 0.1F * inhale;
            cheek = Math.max(cheek, 1.0F + 0.9F * inhale);
            for (int i = 0; i < 7; i++) {
                this.pipes[i].y -= 1.5F * inhale;
                this.pipes[i].xRot = Mth.sin(age * 3.0F + i) * 0.08F * blast;
            }
            this.head.xRot += -0.3F * inhale + 0.25F * blast;
            this.head.z -= 2.0F * blast;
            this.bell.xScale = 1.0F + 0.3F * blast;
            this.bell.yScale = 1.0F + 0.3F * blast;
            this.leftEar.zRot -= 1.1F * blast;
            this.rightEar.zRot += 1.1F * blast;
            for (int i = 0; i < 4; i++) {
                this.legs[i].xRot *= 1.0F - inhale;
            }
        }
        this.leftCheek.xScale = cheek;
        this.leftCheek.yScale = cheek;
        this.leftCheek.zScale = cheek;
        this.rightCheek.xScale = cheek;
        this.rightCheek.yScale = cheek;
        this.rightCheek.zScale = cheek;
        if (s.hasRedOverlay) {
            this.head.xRot -= 0.3F;
            this.body.yScale *= 0.92F;
            this.leftEar.zRot += 0.5F;
            this.rightEar.zRot -= 0.5F;
        }
    }
}
