package com.thesift.client.model;

import com.thesift.client.renderer.state.SlumblerRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Slumbler: a heavy, sprawling salamander. Its legs splay out in a lizard gait while the body and
 * three-part tail undulate; yawns open the enormous jaw in a slow, satisfying stretch.
 */
public class SlumblerModel extends EntityModel<SlumblerRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftEyelid;
    private final ModelPart rightEyelid;
    private final ModelPart leftGills;
    private final ModelPart rightGills;
    private final ModelPart crest;
    private final ModelPart[] tail = new ModelPart[3];
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];

    public SlumblerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftEyelid = this.head.getChild("left_eye").getChild("left_eyelid");
        this.rightEyelid = this.head.getChild("right_eye").getChild("right_eyelid");
        this.leftGills = this.head.getChild("left_gills");
        this.rightGills = this.head.getChild("right_gills");
        this.crest = this.body.getChild("crest");
        this.tail[0] = this.body.getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail3");
        String[] names = {"left_front", "right_front", "left_hind", "right_hind"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(names[i] + "_leg");
            this.feet[i] = this.legs[i].getChild(names[i] + "_foot");
        }
    }

    @Override
    public void setupAnim(SlumblerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F);
        float pos = s.walkAnimationPos * 0.55F;

        // --- sprawling lizard gait: diagonal legs move together, body swings
        float sw = Mth.sin(pos);
        float cw = Mth.cos(pos);
        this.body.yRot = sw * 0.12F * walk;
        this.body.zRot = cw * 0.04F * walk;
        for (int i = 0; i < 4; i++) {
            boolean left = i % 2 == 0;
            float phase = (i == 0 || i == 3) ? 0.0F : (float) Math.PI;
            float sgn = left ? 1.0F : -1.0F;
            this.legs[i].yRot = Mth.sin(pos + phase) * 0.55F * walk * sgn;
            this.legs[i].zRot = -sgn * Math.max(0.0F, Mth.cos(pos + phase)) * 0.35F * walk;
            this.feet[i].zRot = sgn * Math.max(0.0F, Mth.cos(pos + phase)) * 0.3F * walk;
        }
        float tailSwing = 0.08F + walk * 0.25F;
        for (int i = 0; i < 3; i++) {
            this.tail[i].yRot = Mth.sin(pos - 0.9F * (i + 1)) * tailSwing * walk + Mth.sin(age * 0.05F - i * 0.7F) * 0.08F;
            this.tail[i].xRot = Mth.sin(age * 0.07F - i) * 0.02F;
        }
        if (s.isInWater) {
            for (int i = 0; i < 4; i++) {
                this.legs[i].yRot = (i < 2 ? 0.9F : 0.6F) * (i % 2 == 0 ? 1 : -1);
            }
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot = Mth.sin(age * 0.25F - i * 0.9F) * 0.35F;
            }
        }

        // --- head look + breathing
        float breath = Mth.sin(age * (s.sleeping ? 0.05F : 0.09F));
        this.head.yRot = s.yRot * Anim.DEG * 0.5F;
        this.head.xRot = s.xRot * Anim.DEG * 0.4F;
        this.body.yScale = 1.0F + breath * 0.025F;
        this.body.xScale = 1.0F + breath * 0.02F;
        this.leftGills.yRot += Mth.sin(age * 0.18F) * 0.18F;
        this.rightGills.yRot -= Mth.sin(age * 0.18F + 0.6F) * 0.18F;
        this.crest.zRot = Mth.sin(age * 0.1F) * 0.05F;

        // --- sleeping: head down, legs splayed flat, eyes shut, curled tail
        if (s.sleeping) {
            this.head.xRot = 0.12F;
            this.head.yRot = 0.0F;
            for (int i = 0; i < 4; i++) {
                float sgn = i % 2 == 0 ? 1.0F : -1.0F;
                this.legs[i].zRot = -sgn * 0.45F;
                this.feet[i].zRot = sgn * 0.9F;
            }
            this.body.y += 2.5F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot = 0.35F;
            }
        }

        // --- yawn: a slow, huge stretch (2.4s)
        float yawn = Anim.seconds(s.yawn, age);
        float yawnOpen = yawn >= 0 ? Anim.envelope(yawn, 0.1F, 0.7F, 0.6F, 0.8F) : 0.0F;
        // --- bite: a quick snap (0.45s)
        float bite = Anim.seconds(s.bite, age);
        float biteOpen = bite >= 0 ? Anim.envelope(bite, 0.0F, 0.12F, 0.05F, 0.15F) : 0.0F;
        float open = Math.max(yawnOpen * 0.95F, biteOpen * 0.75F);
        this.jaw.xRot += open;
        this.head.xRot -= yawnOpen * 0.35F - biteOpen * 0.2F;
        this.leftGills.zRot += yawnOpen * 0.4F;
        this.rightGills.zRot -= yawnOpen * 0.4F;
        this.body.zScale = 1.0F + yawnOpen * 0.04F;

        // --- hurt: the head jerks up, the body flinches and the tail whips
        if (s.hasRedOverlay) {
            this.head.xRot -= 0.3F;
            this.jaw.xRot += 0.35F;
            this.body.yScale *= 0.92F;
            this.body.xScale *= 1.05F;
            for (int i = 0; i < 3; i++) {
                this.tail[i].yRot += 0.35F * (i + 1) * Mth.sin(age * 1.4F);
            }
            this.leftGills.yRot += 0.4F;
            this.rightGills.yRot -= 0.4F;
        }

        // --- death: rolls belly-up, legs stiff in the air, jaw lolling open
        float roll = Anim.smooth(s.dying / 14.0F);
        if (roll > 0.0F) {
            this.body.zRot = roll * (float) Math.PI;
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

        boolean eyesShut = s.sleeping || yawnOpen > 0.55F || s.blink || roll > 0.6F;
        this.leftEyelid.visible = eyesShut;
        this.rightEyelid.visible = eyesShut;
    }
}
