package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.MiniBoss;
import com.thesift.entity.boss.Whistler;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Whistler. In the air its great wings beat in slow, deep strokes - the hand of each wing
 * lagging behind the arm, so the primaries whip through at the bottom of the stroke - its legs
 * trail behind and its long neck rides the swell. While it plays its song lock the neck stretches
 * out at you, the wings hold a trembling glide and the jaw quivers; in a dive the wings fold back
 * and it becomes a spear. Grounded with nothing to do, it folds its wings and stalks.
 */
public class WhistlerModel extends EntityModel<MiniBossRenderState> {
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart neckUpper;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftTip;
    private final ModelPart rightTip;
    private final ModelPart tail;
    private final ModelPart spines;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private final ModelPart leftShin;
    private final ModelPart rightShin;

    public WhistlerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.neckUpper = this.neck.getChild("neck_upper");
        this.head = this.neckUpper.getChild("head");
        this.jaw = this.head.getChild("beak").getChild("jaw");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftTip = this.leftWing.getChild("left_wing_tip");
        this.rightTip = this.rightWing.getChild("right_wing_tip");
        this.tail = this.body.getChild("tail");
        this.spines = this.body.getChild("spines");
        this.leftLeg = root.getChild("left_leg");
        this.rightLeg = root.getChild("right_leg");
        this.leftShin = this.leftLeg.getChild("left_shin");
        this.rightShin = this.rightLeg.getChild("right_shin");
    }

    @Override
    public void setupAnim(MiniBossRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        int st = s.bossState;
        float t = s.stateTime;
        boolean perched = s.grounded && (st == MiniBoss.IDLE || st == Whistler.STUNNED);

        this.head.yRot = s.yRot * Anim.DEG * 0.5F;
        this.head.xRot = s.xRot * Anim.DEG * 0.5F;
        for (int i = 0; i < 5; i++) {
            ModelPart sp = this.spines.getChild("spine_" + i);
            sp.xRot += Mth.sin(age * 0.15F + i * 0.7F) * 0.06F;
        }
        if (perched) {
            // folded and stalking: wings swept back along the body, a heron's careful walk
            float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
            float pos = s.walkAnimationPos * 0.6F;
            this.leftWing.yRot = -1.45F;
            this.rightWing.yRot = 1.45F;
            this.leftWing.zRot = 0.2F;
            this.rightWing.zRot = -0.2F;
            this.leftTip.yRot = 0.25F;
            this.rightTip.yRot = -0.25F;
            this.leftLeg.xRot = Mth.cos(pos) * 0.6F * walk;
            this.rightLeg.xRot = -Mth.cos(pos) * 0.6F * walk;
            this.leftShin.xRot = Math.max(0.0F, -Mth.sin(pos)) * 0.6F * walk;
            this.rightShin.xRot = Math.max(0.0F, Mth.sin(pos)) * 0.6F * walk;
            this.neck.xRot = Mth.sin(pos) * 0.12F * walk + Mth.sin(age * 0.05F) * 0.05F;
            this.body.y -= Math.abs(Mth.cos(pos)) * 0.4F * walk;
            if (st == Whistler.STUNNED) {
                this.neck.xRot = 0.6F;
                this.head.zRot = Mth.sin(age * 0.3F) * 0.35F;
                this.leftWing.zRot = 0.6F + Mth.sin(age * 0.5F) * 0.1F;
                this.rightWing.zRot = -0.6F - Mth.sin(age * 0.5F) * 0.1F;
            }
            return;
        }
        // --- the wing beat: arm leads, hand lags
        float a = Whistler.WhistlerFlap.angle(age, st);
        float lag = Whistler.WhistlerFlap.angle(age - 3.0F, st);
        this.leftWing.zRot = -a;
        this.rightWing.zRot = a;
        this.leftTip.zRot = -(lag - a) * 0.9F - 0.08F;
        this.rightTip.zRot = (lag - a) * 0.9F + 0.08F;
        // the body rides the stroke: up when the wings come down
        this.body.y += Mth.sin(a) * 1.4F;
        this.body.xRot = 0.1F;
        // legs trailing, gently swinging
        this.leftLeg.xRot = 1.25F + Mth.sin(age * 0.1F) * 0.06F;
        this.rightLeg.xRot = 1.3F + Mth.sin(age * 0.1F + 1.0F) * 0.06F;
        this.leftLeg.y += Mth.sin(a) * 1.4F;
        this.rightLeg.y += Mth.sin(a) * 1.4F;
        this.leftShin.xRot = 0.15F;
        this.rightShin.xRot = 0.15F;
        this.neck.xRot = -0.2F - Mth.sin(a) * 0.08F;
        this.neckUpper.xRot = 0.15F + Mth.sin(a) * 0.08F;
        this.tail.xRot = 0.15F + Mth.sin(age * 0.2F) * 0.05F;
        this.tail.xScale = 1.15F;

        switch (st) {
            case Whistler.BEAM_CHARGE, Whistler.BEAM_LOCK -> {
                float k = Anim.smooth(t / 10.0F);
                // neck out straight at the target, head level, beak trembling with the song
                this.neck.xRot = Mth.lerp(k, this.neck.xRot, -0.95F);
                this.neckUpper.xRot = Mth.lerp(k, this.neckUpper.xRot, 0.55F);
                this.head.xRot += 0.35F * k;
                boolean lock = st == Whistler.BEAM_LOCK;
                this.jaw.xRot = (lock ? 0.18F + Mth.sin(age * 3.1F) * 0.06F : 0.06F) * k;
                this.head.zRot = lock ? Mth.sin(age * 2.3F) * 0.02F : 0.0F;
                for (int i = 0; i < 5; i++) {
                    ModelPart sp = this.spines.getChild("spine_" + i);
                    sp.yScale = 1.0F + (lock ? Mth.sin(age * 1.5F + i) * 0.08F : 0.04F * k);
                }
                this.tail.xRot -= 0.2F * k;
            }
            case Whistler.DIVE_WINDUP -> {
                float k = Anim.smooth(t / 10.0F);
                this.neck.xRot -= 0.3F * k;
                this.body.xRot = Mth.lerp(k, 0.1F, -0.35F);
                this.tail.xRot += 0.3F * k;
            }
            case Whistler.DIVE -> {
                float k = Anim.smooth(t / 5.0F);
                // folded into a spear
                this.leftWing.yRot = -1.2F * k;
                this.rightWing.yRot = 1.2F * k;
                this.leftWing.zRot = 0.1F;
                this.rightWing.zRot = -0.1F;
                this.leftTip.yRot = 0.4F * k;
                this.rightTip.yRot = -0.4F * k;
                this.body.xRot = 0.85F * k;
                this.neck.xRot = -1.1F * k;
                this.neckUpper.xRot = 0.2F;
                this.jaw.xRot = 0.35F * k;
                this.leftLeg.xRot = 1.5F;
                this.rightLeg.xRot = 1.5F;
            }
            case Whistler.CALL -> {
                float k = Anim.envelope(t, 0.0F, 6.0F, 14.0F, 8.0F);
                this.neck.xRot = Mth.lerp(k, this.neck.xRot, -0.1F);
                this.neckUpper.xRot = Mth.lerp(k, this.neckUpper.xRot, -0.5F);
                this.head.xRot -= 0.5F * k;
                this.jaw.xRot = 0.5F * k;
                this.leftWing.zRot = Mth.lerp(k, this.leftWing.zRot, -1.1F);
                this.rightWing.zRot = Mth.lerp(k, this.rightWing.zRot, 1.1F);
            }
            case Whistler.STUNNED -> {
                this.head.zRot = Mth.sin(age * 0.35F) * 0.4F;
                this.neck.xRot = 0.5F;
            }
            default -> {
            }
        }
        if (s.hurtTicks >= 0.0F) {
            float h = 1.0F - Math.min(1.0F, s.hurtTicks / 10.0F);
            this.neck.xRot += 0.25F * h;
            this.jaw.xRot += 0.3F * h;
        }
    }
}
