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
 * </ul>
 *
 * <p>As the titan the same bones carry the new parts it grows: the sculk crust swells up over its
 * shell, the drum sinks into the middle of it, plates push up along its edges, its neck and a long
 * new tail. The plates are dark until it means to breathe - then they light one by one, tail to
 * head, and stay lit and flickering while the beam pours out of its jaws.
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

    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart mantle;
    private final ModelPart plates;
    private final ModelPart tailTitan;
    private final ModelPart[] neckPlates = new ModelPart[2];
    /** The plates' lit twins, tail tip first: one step of the light's climb per row. */
    private final ModelPart[][] lit = new ModelPart[10][];

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
        this.tendrils[0] = this.head.getChild("left_tendril");
        this.tendrils[1] = this.head.getChild("right_tendril");
        this.mantle = this.body.getChild("mantle");
        this.plates = this.body.getChild("plates");
        this.tailTitan = this.tail.getChild("tail_titan");
        int k = 0;
        for (int i = 2; i >= 0; i--) {
            this.lit[k++] = new ModelPart[]{this.tailTitan.getChild("tail_plate_" + i).getChild("tail_plate_" + i + "_lit")};
        }
        for (int i = 0; i < 5; i++) {
            this.lit[k++] = new ModelPart[]{this.plates.getChild("left_plate_" + i).getChild("left_plate_" + i + "_lit"),
                    this.plates.getChild("right_plate_" + i).getChild("right_plate_" + i + "_lit")};
        }
        for (int i = 0; i < 2; i++) {
            this.neckPlates[i] = this.neck.getChild("neck_plate_" + i);
            this.lit[k++] = new ModelPart[]{this.neckPlates[i].getChild("neck_plate_" + i + "_lit")};
        }
    }

    @Override
    public void setupAnim(MiniBossRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float g = s.titan;
        float size = 1.0F + (Thumper.TITAN_SCALE - 1.0F) * g;
        // a titan's stride is longer: slower legs for the same ground covered
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.4F * (1.0F + g));
        float pos = s.walkAnimationPos * 0.55F / size;
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

        // the Warden's tendrils: a restless twitch, a shiver when it is hurt or attacking
        float shiver = (s.hurtTicks >= 0.0F ? 1.0F : 0.0F) + (s.bossState != com.thesift.entity.boss.MiniBoss.IDLE ? 0.5F : 0.0F);
        for (int i = 0; i < 2; i++) {
            float sgn = i == 0 ? 1.0F : -1.0F;
            float tw = Mth.sin(s.ageInTicks * (0.11F + i * 0.02F) + i) * 0.18F + Mth.sin(s.ageInTicks * 1.7F + i) * 0.08F * shiver;
            this.tendrils[i].zRot += sgn * tw;
            this.tendrils[i].xRot += Mth.sin(s.ageInTicks * 0.07F + i * 2.0F) * 0.12F;
        }
        switch (st) {
            case Thumper.SLAM -> this.slam(Anim.envelope(t, 0.0F, 18.0F, 2.0F, 2.5F), Anim.envelope(t, 20.0F, 1.5F, 2.0F, 10.0F), age, 1.0F);
            case Thumper.T_STOMP -> {
                // the slam, slowed to a titan's pace; with riders aboard it only lifts its forefeet
                float tt = t * 0.8F;
                this.slam(Anim.envelope(tt, 0.0F, 18.0F, 2.0F, 2.5F), Anim.envelope(tt, 20.0F, 1.5F, 2.0F, 10.0F), age, s.ridden ? 0.15F : 1.0F);
            }
            case Thumper.T_BREATH -> {
                float charge = Anim.smooth(t / Thumper.BREATH_CHARGE);
                boolean firing = t >= Thumper.BREATH_CHARGE && t < Thumper.BREATH_END;
                float k = Math.min(charge, t < Thumper.BREATH_END ? 1.0F : 1.0F - Anim.smooth((t - Thumper.BREATH_END) / 12.0F));
                // neck up, head down at its target, braced on its forelegs, jaws wide
                this.neck.xRot = Mth.lerp(k, this.neck.xRot, -0.55F);
                this.head.xRot += 0.35F * k;
                this.jaw.xRot = Mth.lerp(k, this.jaw.xRot, firing ? 0.85F + Mth.sin(age * 2.9F) * 0.06F : 0.25F * charge);
                this.frontLeft.xRot = Mth.lerp(k, this.frontLeft.xRot, -0.25F);
                this.frontRight.xRot = Mth.lerp(k, this.frontRight.xRot, -0.25F);
                this.body.xRot = -0.08F * k;
                this.tail.yRot = Mth.sin(age * 0.4F) * 0.25F * k;
                if (firing) {
                    this.head.zRot = Mth.sin(age * 1.9F) * 0.03F;
                    this.body.zRot += Mth.sin(age * 2.3F) * 0.01F;
                }
            }
            case Thumper.T_SHAKE -> {
                float k = Anim.envelope(t, 0.0F, 8.0F, 52.0F, 10.0F);
                // shaking itself like a wet dog
                this.body.zRot += Mth.sin(age * 2.2F) * 0.07F * k;
                this.body.xRot += Mth.sin(age * 1.7F + 1.0F) * 0.04F * k;
                this.neck.xRot = Mth.lerp(k, this.neck.xRot, -0.4F);
                this.head.yRot += Mth.sin(age * 1.3F) * 0.5F * k;
                this.jaw.xRot = 0.7F * k;
                this.frontLeft.xRot = Mth.sin(age * 1.1F) * 0.4F * k;
                this.frontRight.xRot = Mth.sin(age * 1.1F + Mth.PI) * 0.4F * k;
                this.tail.yRot = Mth.sin(age * 1.5F) * 0.6F * k;
            }
            case Thumper.T_BRACE -> this.neck.xRot += 0.12F;
            case Thumper.T_HOLD -> {
                // glaring up at the tower top, growling
                float k = Anim.smooth(t / 10.0F);
                this.neck.xRot = Mth.lerp(k, this.neck.xRot, -0.35F);
                this.jaw.xRot = (0.15F + Math.max(0.0F, Mth.sin(age * 0.3F)) * 0.15F) * k;
                this.tail.yRot = Mth.sin(age * 0.2F) * 0.3F;
            }
            case Thumper.AWAKEN -> {
                if (t < Thumper.GROW_START) {
                    // it doubles up, shuddering, as the sculk in its shell wakes
                    float k = Anim.smooth(t / 8.0F);
                    this.body.zRot += Mth.sin(age * 2.5F) * 0.05F * k;
                    this.head.zRot = Mth.sin(age * 1.9F) * 0.3F * k;
                    this.jaw.xRot = 0.5F * k;
                    this.neck.xRot = 0.4F * k;
                } else if (t < Thumper.GROW_END) {
                    // swelling: legs splayed and braced, head thrown back
                    float k = Anim.smooth((t - Thumper.GROW_START) / 20.0F);
                    this.neck.xRot = Mth.lerp(k, 0.4F, -0.6F);
                    this.jaw.xRot = 0.4F + Mth.sin(age * 0.9F) * 0.2F;
                    this.body.zRot += Mth.sin(age * 3.1F) * 0.02F;
                    this.frontLeft.zRot = -0.15F * k;
                    this.hindLeft.zRot = -0.15F * k;
                    this.frontRight.zRot = 0.15F * k;
                    this.hindRight.zRot = 0.15F * k;
                } else {
                    // and the roar
                    float k = 1.0F - Anim.smooth((t - Thumper.GROW_END - 20.0F) / 10.0F);
                    this.neck.xRot = -0.75F * k;
                    this.head.xRot -= 0.4F * k;
                    this.jaw.xRot = 1.0F * k;
                }
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
                float gallop = age * 1.1F;
                this.frontLeft.xRot = Mth.cos(gallop) * 1.0F;
                this.hindRight.xRot = Mth.cos(gallop) * 1.0F;
                this.frontRight.xRot = -Mth.cos(gallop) * 1.0F;
                this.hindLeft.xRot = -Mth.cos(gallop) * 1.0F;
                this.body.y -= Math.abs(Mth.sin(gallop)) * 1.5F;
                this.body.xRot = 0.08F;
                this.neck.xRot = 0.4F;
                this.neck.z -= 1.5F;
                this.jaw.xRot = 0.3F;
                this.tail.yRot = Mth.sin(gallop * 2.0F) * 0.4F;
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
            default -> {
            }
        }
        this.titan(s, g, age, t, st);
        if (s.hurtTicks >= 0.0F) {
            // the drum skin shudders when struck
            float h = 1.0F - Math.min(1.0F, s.hurtTicks / 10.0F);
            this.drum.xScale = 1.0F + Mth.sin(s.hurtTicks * 2.5F) * 0.06F * h;
            this.drum.zScale = this.drum.xScale;
            this.leftBrow.y -= 0.8F * h;
            this.rightBrow.y -= 0.8F * h;
        }
    }

    /** Rears up (`up`) and crashes down (`hit`); `rear` scales how far it tips back. */
    private void slam(float up, float hit, float age, float rear) {
        this.body.xRot = (-0.5F * up + 0.06F * hit) * rear;
        this.body.y -= 3.0F * up * rear;
        this.frontLeft.xRot = Mth.lerp(up, this.frontLeft.xRot, -1.1F + Mth.sin(age * 0.6F) * 0.3F);
        this.frontRight.xRot = Mth.lerp(up, this.frontRight.xRot, -1.1F - Mth.sin(age * 0.6F) * 0.3F);
        this.hindLeft.xRot = Mth.lerp(up, this.hindLeft.xRot, 0.5F);
        this.hindRight.xRot = Mth.lerp(up, this.hindRight.xRot, 0.5F);
        this.neck.xRot -= 0.4F * up;
        this.jaw.xRot += 0.35F * up + 0.6F * hit;
        this.body.yScale *= 1.0F - 0.15F * hit * rear;
        this.body.xScale = 1.0F + 0.08F * hit;
        this.body.zScale = 1.0F + 0.08F * hit;
        this.leftStick.xRot -= 0.8F * hit;
        this.rightStick.xRot -= 0.8F * hit;
    }

    /** The parts it grows as the titan, and the light climbing its plates. */
    private void titan(MiniBossRenderState s, float g, float age, float t, int st) {
        boolean titan = g > 0.0F;
        this.mantle.visible = titan;
        this.plates.visible = titan;
        this.tailTitan.visible = titan;
        this.neckPlates[0].visible = titan;
        this.neckPlates[1].visible = titan;
        this.leftStick.visible = g < 0.4F;
        this.rightStick.visible = g < 0.4F;
        if (!titan) {
            for (ModelPart[] row : this.lit) {
                for (ModelPart p : row) {
                    p.visible = false;
                }
            }
            return;
        }
        // the crust swells up over the shell; the plates push up out of it, the tail grows out
        float grow = Math.max(0.02F, g);
        this.mantle.yScale = grow;
        this.plates.yScale = grow;
        this.tailTitan.zScale = grow;
        this.neckPlates[0].yScale = grow;
        this.neckPlates[1].yScale = grow;
        // the drum sinks into the middle of the deck, its skin flush with it
        this.drum.y += 8.9F * g;
        if (st == Thumper.IDLE) {
            // a titan carries its head high
            this.neck.xRot -= 0.2F * g;
        }
        int n = this.lit.length;
        for (int k = 0; k < n; k++) {
            boolean on;
            if (st == Thumper.T_BREATH) {
                // tail to head, a row at a time, then all of them flickering while it breathes
                on = t < Thumper.BREATH_CHARGE ? t >= k * (Thumper.BREATH_CHARGE / (float) n) : t < Thumper.BREATH_END || Mth.sin(age * 3.0F + k) > 0.0F;
            } else if (st == Thumper.AWAKEN) {
                on = Mth.sin(age * 1.7F + k * 2.1F) > 0.3F;
            } else {
                // now and then a ripple of light runs up its spine
                float wave = (age * 0.12F) % 18.0F;
                on = Math.abs(wave - k) < 0.8F;
            }
            for (ModelPart p : this.lit[k]) {
                p.visible = on;
            }
        }
    }
}
