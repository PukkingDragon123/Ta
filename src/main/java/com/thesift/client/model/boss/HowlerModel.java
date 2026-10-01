package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MinionRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Howler.
 *
 * <ul>
 *   <li>idle: slow breathing through the pipes - each pipe bobs on its own phase - and the bell
 *   head sways</li>
 *   <li>move: a hunched four-legged prowl</li>
 *   <li>attack: plants its feet and swells as it inhales (pipes rise, body inflates), then the
 *   blast - head thrust forward, bell flared, everything shudders</li>
 * </ul>
 */
public class HowlerModel extends EntityModel<MinionRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart bell;
    private final ModelPart tail;
    private final ModelPart[] pipes = new ModelPart[7];
    private final ModelPart[] legs = new ModelPart[4];

    public HowlerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.bell = this.head.getChild("bell");
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

        for (int i = 0; i < 4; i++) {
            float phase = (i == 0 || i == 3) ? 0.0F : Mth.PI;
            this.legs[i].xRot = Mth.cos(pos + phase) * 0.7F * walk;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.6F * walk;
        this.head.yRot = s.yRot * Anim.DEG * 0.7F;
        this.head.xRot = s.xRot * Anim.DEG * 0.5F + Mth.sin(age * 0.05F) * 0.06F;
        this.tail.yRot = Mth.sin(age * 0.1F) * 0.2F;
        for (int i = 0; i < 7; i++) {
            this.pipes[i].y -= Math.max(0.0F, Mth.sin(age * 0.09F + i * 0.9F)) * 0.6F;
        }

        float t = Anim.seconds(s.attack, age);
        if (t >= 0.0F && t < 2.2F) {
            float inhale = Anim.envelope(t, 0.0F, 1.4F, 0.1F, 0.08F);
            float blast = Anim.envelope(t, 1.5F, 0.06F, 0.35F, 0.3F);
            this.body.xScale = 1.0F + 0.12F * inhale - 0.06F * blast;
            this.body.yScale = 1.0F + 0.1F * inhale;
            this.body.zScale = 1.0F + 0.08F * inhale;
            for (int i = 0; i < 7; i++) {
                this.pipes[i].y -= 1.5F * inhale;
                this.pipes[i].xRot = Mth.sin(age * 3.0F + i) * 0.06F * blast;
            }
            this.head.xRot += -0.3F * inhale + 0.25F * blast;
            this.head.z -= 2.0F * blast;
            this.bell.xScale = 1.0F + 0.25F * blast;
            this.bell.yScale = 1.0F + 0.25F * blast;
            for (int i = 0; i < 4; i++) {
                this.legs[i].xRot *= 1.0F - inhale;
            }
        }
        if (s.hasRedOverlay) {
            this.head.xRot -= 0.3F;
            this.body.xScale *= 1.05F;
        }
    }
}
