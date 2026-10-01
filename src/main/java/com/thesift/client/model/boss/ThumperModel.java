package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.Thumper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Thumper. Idle, it plods like a tortoise, head swinging, the drumsticks on its back tapping
 * out a lazy rhythm on their own. Every attack is acted out:
 *
 * <ul>
 *   <li>slam: rears up on its hind legs, front legs pawing the air, then crashes down - the shell
 *   squashes, the jaw flies open</li>
 *   <li>charge wind-up: head down, hind legs pawing, the sticks drum furiously; charge: a full
 *   gallop</li>
 *   <li>spin: head, legs and tail pull into the shell and it whirls</li>
 *   <li>dazed: sprawled, head lolling, jaw hanging</li>
 *   <li>drum roll: head up, proud, sticks a blur</li>
 * </ul>
 */
public class ThumperModel extends EntityModel<MiniBossRenderState> {
    private final ModelPart body;
    private final ModelPart drum;
    private final ModelPart leftStick;
    private final ModelPart rightStick;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart leftBrow;
    private final ModelPart rightBrow;
    private final ModelPart frontLeft;
    private final ModelPart frontRight;
    private final ModelPart hindLeft;
    private final ModelPart hindRight;
    private final ModelPart tail;

    public ThumperModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.drum = this.body.getChild("drum");
        this.leftStick = this.drum.getChild("left_stick");
        this.rightStick = this.drum.getChild("right_stick");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.leftBrow = this.head.getChild("left_brow");
        this.rightBrow = this.head.getChild("right_brow");
        this.frontLeft = this.body.getChild("front_left_leg");
        this.frontRight = this.body.getChild("front_right_leg");
        this.hindLeft = this.body.getChild("hind_left_leg");
        this.hindRight = this.body.getChild("hind_right_leg");
        this.tail = this.body.getChild("tail");
    }

    @Override
    public void setupAnim(MiniBossRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
        float pos = s.walkAnimationPos * 0.55F;
        float t = s.stateTime;
        int st = s.bossState;

        // --- a heavy plod: diagonal pairs of legs, the shell rocking with each step
        float c = Mth.cos(pos);
        this.frontLeft.xRot = c * 0.55F * walk;
        this.hindRight.xRot = c * 0.55F * walk;
        this.frontRight.xRot = -c * 0.55F * walk;
        this.hindLeft.xRot = -c * 0.55F * walk;
        this.body.zRot = Mth.sin(pos) * 0.05F * walk;
        this.body.y -= Math.abs(c) * 0.6F * walk;
        this.body.yScale = 1.0F + Mth.sin(age * 0.07F) * 0.012F;
        // the head swings and looks around
        this.neck.xRot = Mth.sin(age * 0.05F) * 0.05F + Mth.sin(pos) * 0.06F * walk;
        this.head.yRot = s.yRot * Anim.DEG * 0.7F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F;
        this.tail.yRot = Mth.sin(age * 0.12F + pos) * 0.25F;
        this.jaw.xRot = Math.max(0.0F, Mth.sin(age * 0.04F)) * 0.06F;
        // the sticks tap a lazy beat on their own
        float beat = Math.max(0.0F, Mth.sin(age * 0.25F));
        float beat2 = Math.max(0.0F, Mth.sin(age * 0.25F + Mth.PI));
        this.leftStick.xRot = 0.5F + beat * 0.35F;
        this.rightStick.xRot = 0.5F + beat2 * 0.35F;
        this.drum.yScale = 1.0F - (beat + beat2) * 0.012F;
        boolean angry = st != Thumper.IDLE && st != Thumper.DAZED;
        this.leftBrow.zRot = angry ? -0.35F : 0.0F;
        this.rightBrow.zRot = angry ? 0.35F : 0.0F;
        this.leftBrow.y += angry ? 0.5F : 0.0F;
        this.rightBrow.y += angry ? 0.5F : 0.0F;

        switch (st) {
            case Thumper.SLAM -> {
                float up = Anim.envelope(t, 0.0F, 18.0F, 2.0F, 2.5F);
                float hit = Anim.envelope(t, 20.0F, 1.5F, 2.0F, 10.0F);
                this.body.xRot = -0.5F * up + 0.06F * hit;
                this.body.y -= 3.0F * up;
                this.frontLeft.xRot = Mth.lerp(up, this.frontLeft.xRot, -1.1F + Mth.sin(age * 0.6F) * 0.3F);
                this.frontRight.xRot = Mth.lerp(up, this.frontRight.xRot, -1.1F - Mth.sin(age * 0.6F) * 0.3F);
                this.hindLeft.xRot = Mth.lerp(up, this.hindLeft.xRot, 0.5F);
                this.hindRight.xRot = Mth.lerp(up, this.hindRight.xRot, 0.5F);
                this.neck.xRot -= 0.4F * up;
                this.jaw.xRot += 0.35F * up + 0.6F * hit;
                this.body.yScale *= 1.0F - 0.15F * hit;
                this.body.xScale = 1.0F + 0.08F * hit;
                this.body.zScale = 1.0F + 0.08F * hit;
                this.leftStick.xRot -= 0.8F * hit;
                this.rightStick.xRot -= 0.8F * hit;
            }
            case Thumper.CHARGE_WINDUP -> {
                float k = Anim.smooth(t / 8.0F);
                this.neck.xRot += 0.35F * k;
                this.neck.z -= 1.0F * k;
                this.hindLeft.xRot = Mth.sin(age * 0.9F) * 0.6F * k;
                this.hindRight.xRot = Mth.sin(age * 0.9F + Mth.PI) * 0.6F * k;
                this.body.zRot = Mth.sin(age * 1.3F) * 0.03F * k;
                this.leftStick.xRot = 0.5F + Math.max(0.0F, Mth.sin(age * 1.6F)) * 0.6F;
                this.rightStick.xRot = 0.5F + Math.max(0.0F, Mth.sin(age * 1.6F + Mth.PI)) * 0.6F;
                this.jaw.xRot += 0.2F * k;
            }
            case Thumper.CHARGE -> {
                float g = age * 1.1F;
                this.frontLeft.xRot = Mth.cos(g) * 1.0F;
                this.hindRight.xRot = Mth.cos(g) * 1.0F;
                this.frontRight.xRot = -Mth.cos(g) * 1.0F;
                this.hindLeft.xRot = -Mth.cos(g) * 1.0F;
                this.body.y -= Math.abs(Mth.sin(g)) * 1.5F;
                this.body.xRot = 0.08F;
                this.neck.xRot = 0.4F;
                this.neck.z -= 1.5F;
                this.jaw.xRot = 0.3F;
                this.tail.yRot = Mth.sin(g * 2.0F) * 0.4F;
            }
            case Thumper.SPIN -> {
                float tuck = Anim.envelope(t, 0.0F, 8.0F, 66.0F, 10.0F);
                // spin up over the first ticks, hold, wind down
                float spin = t < 10.0F ? 0.0F : t < 74.0F ? (t - 10.0F) * 0.9F : 57.6F + (1.0F - (float) Math.pow(1.0F - Math.min(1.0F, (t - 74.0F) / 12.0F), 2)) * 6.0F;
                this.body.yRot = spin;
                this.body.y += 3.5F * tuck;
                float hide = 1.0F - tuck;
                for (ModelPart leg : new ModelPart[]{this.frontLeft, this.frontRight, this.hindLeft, this.hindRight}) {
                    leg.yScale = Math.max(0.05F, hide);
                    leg.y -= 4.0F * tuck;
                }
                this.neck.z += 6.0F * tuck;
                this.neck.yScale = Math.max(0.05F, hide);
                this.neck.xScale = Math.max(0.05F, hide);
                this.tail.visible = tuck < 0.6F;
                this.body.zRot = Mth.sin(age * 0.8F) * 0.05F * tuck;
            }
            case Thumper.DAZED -> {
                float k = Anim.smooth(t / 6.0F) * (1.0F - Anim.smooth((t - 70.0F) / 10.0F));
                this.body.y += 3.0F * k;
                this.frontLeft.zRot = -0.6F * k;
                this.hindLeft.zRot = -0.6F * k;
                this.frontRight.zRot = 0.6F * k;
                this.hindRight.zRot = 0.6F * k;
                this.neck.xRot = 0.55F * k;
                this.head.zRot = Mth.sin(age * 0.25F) * 0.3F * k;
                this.head.yRot = Mth.sin(age * 0.15F) * 0.3F * k;
                this.jaw.xRot = 0.5F * k;
                this.drum.zRot = Mth.sin(age * 0.2F) * 0.08F * k;
                this.leftStick.xRot = 1.4F * k + this.leftStick.xRot * (1 - k);
                this.rightStick.xRot = 1.5F * k + this.rightStick.xRot * (1 - k);
            }
            case Thumper.DRUMROLL -> {
                float k = Anim.smooth(t / 6.0F);
                this.neck.xRot = -0.35F * k;
                this.jaw.xRot = 0.4F * k + Mth.sin(age * 0.8F) * 0.1F;
                this.leftStick.xRot = 0.4F + Math.abs(Mth.sin(age * 2.2F)) * 0.5F;
                this.rightStick.xRot = 0.4F + Math.abs(Mth.cos(age * 2.2F)) * 0.5F;
                this.drum.yScale = 1.0F + Mth.sin(age * 4.4F) * 0.03F;
                this.body.y -= Math.abs(Mth.sin(age * 0.5F)) * 0.8F;
            }
            default -> {
            }
        }
        if (s.hurtTicks >= 0.0F) {
            // the drum skin shudders when struck
            float h = 1.0F - Math.min(1.0F, s.hurtTicks / 10.0F);
            this.drum.xScale = 1.0F + Mth.sin(s.hurtTicks * 2.5F) * 0.06F * h;
            this.drum.zScale = this.drum.xScale;
            this.leftBrow.y -= 0.8F * h;
            this.rightBrow.y -= 0.8F * h;
        }
    }
}
