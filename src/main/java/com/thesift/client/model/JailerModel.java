package com.thesift.client.model;

import com.thesift.client.renderer.state.JailerRenderState;
import com.thesift.entity.cave.Jailer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Jailer (CAVE v4, designed afresh in tools/jailer.py): a hulking, eyeless sculk warden whose own ribcage is
 * the cell. The ribcage with its shoulders, arms and head is the 'cage' part (it stays where the prisoner is held);
 * the hips, spine and hind legs are the body and the root.
 *
 * <ul>
 *   <li>walking on four limbs: the hind legs stride, the long arms knuckle forward in turn, the ribcage rolls and
 *   bobs a beat behind the hips, the chains and the bone keys swing after it; stalking, it sinks low, head
 *   thrust out, tendrils raised and quivering</li>
 *   <li>idle: the ribs rise and fall with its breath, the heart of soul light beats, the head tilts and the
 *   tendrils twitch; hearing something, the head snaps aside and the tendrils flare</li>
 *   <li>the slam: a crouch, then it rears with a roar and its ribs spread wide open, and crashes down over you,
 *   the ribs snapping shut; the lock jolts as the cell closes</li>
 *   <li>holding a prisoner: the claws dig in; its grip beats with its heart - a thump, then the ribs ease apart
 *   (the moment to strike); it squeezes, the ribs pressing in; struck, the ribs rattle; a good heave bows them
 *   out; a rescuer gets a backward kick; the ribs burst with a roar and a stagger and slowly grow back</li>
 *   <li>stunned it sways; hurt it flinches; dying its legs give way and it sinks; emerging it claws its way up
 *   out of the rock and roars</li>
 * </ul>
 */
public class JailerModel extends EntityModel<JailerRenderState> {
    /** The order the ribs snap in as the cell is broken (and the reverse is the order they regrow). */
    private static final int[] BREAK_ORDER = {2, 9, 4, 7, 0, 11, 3, 8, 5, 6, 1, 10};
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart spine;
    private final ModelPart keys;
    private final ModelPart cage;
    private final ModelPart floor;
    private final ModelPart heart;
    private final ModelPart lock;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart[] bars = new ModelPart[Jailer.CAGE_WHOLE];
    private final ModelPart[] chains = new ModelPart[4];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] claws = new ModelPart[2];
    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart[] tips = new ModelPart[2];

    public JailerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.spine = this.body.getChild("spine");
        this.keys = this.body.getChild("keys");
        this.cage = root.getChild("cage");
        this.floor = this.cage.getChild("floor");
        this.heart = this.cage.getChild("heart");
        this.lock = this.cage.getChild("lock");
        this.neck = this.cage.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        for (int i = 0; i < this.bars.length; i++) {
            this.bars[i] = this.cage.getChild("bar_" + i);
        }
        for (int i = 0; i < this.chains.length; i++) {
            this.chains[i] = this.cage.getChild("chain_" + i);
        }
        for (int i = 0; i < 2; i++) {
            String s = SIDES[i];
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
            this.arms[i] = this.cage.getChild(s + "_arm");
            this.forearms[i] = this.arms[i].getChild(s + "_forearm");
            this.claws[i] = this.forearms[i].getChild(s + "_hand").getChild(s + "_claw");
            this.tendrils[i] = this.head.getChild(s + "_tendril");
            this.tips[i] = this.tendrils[i].getChild(s + "_tendril_tip");
        }
    }

    /** Bars 0..5 are the left ribs, 6..11 the right; spreads them (positive: out, negative: in). */
    private void ribs(float spread) {
        for (int i = 0; i < this.bars.length; i++) {
            this.bars[i].zRot -= spread * (i < 6 ? 1.0F : -1.0F);
        }
    }

    private void arm(int i, float ax, float az, float fx) {
        float sx = i == 0 ? 1.0F : -1.0F;
        this.arms[i].xRot += ax;
        this.arms[i].zRot += az * sx;
        this.forearms[i].xRot += fx;
    }

    @Override
    public void setupAnim(JailerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float k = s.stalk;
        boolean emerging = s.mode == Jailer.EMERGING;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F) * (emerging ? 0.0F : 1.0F);
        float pos = s.walkAnimationPos * (0.6F - 0.15F * k);
        float sw = Mth.sin(pos);
        float jawOpen = 0.0F;
        float flare = 0.0F;

        // ---- the ribs that still stand, and a floor across the cell when someone is inside
        int missing = Jailer.CAGE_WHOLE - Mth.clamp(s.cage, 0, Jailer.CAGE_WHOLE);
        for (int i = 0; i < BREAK_ORDER.length; i++) {
            this.bars[BREAK_ORDER[i]].visible = i >= missing;
        }
        this.floor.visible = s.carrying;

        // ---- walking on four limbs: hind legs stride, the long arms knuckle forward in turn, the ribcage rolls
        for (int i = 0; i < 2; i++) {
            float phase = i == 0 ? sw : -sw;
            this.legs[i].xRot += phase * 0.5F * walk - 0.2F * k;
            this.shins[i].xRot += Math.max(0.0F, -phase) * 0.6F * walk + 0.25F * k;
            this.feet[i].xRot -= phase * 0.3F * walk;
            // the arm on the same side swings against the leg
            this.arm(i, -phase * 0.4F * walk, 0.0F, Math.max(0.0F, phase) * 0.35F * walk);
            this.claws[i].xRot += Math.max(0.0F, -phase) * 0.3F * walk;
        }
        this.body.y -= Math.abs(Mth.cos(pos)) * 0.9F * walk;
        this.body.zRot += sw * 0.05F * walk;
        this.spine.xRot += Mth.cos(pos * 2.0F) * 0.02F * walk;
        this.cage.y -= Math.abs(Mth.cos(pos - 0.5F)) * 0.7F * walk;
        this.cage.zRot += Mth.sin(pos - 0.6F) * 0.03F * walk;
        this.keys.zRot += Mth.sin(pos - 1.0F) * 0.3F * walk + Mth.sin(age * 0.05F) * 0.04F;
        this.keys.xRot += Mth.cos(pos - 1.2F) * 0.15F * walk;

        // ---- stalking: sunk low, the head thrust forward and down, tendrils raised and quivering
        this.body.y += 2.0F * k;
        this.cage.y += 2.5F * k;
        this.neck.xRot += 0.15F * k;
        this.head.xRot += 0.1F * k;
        for (int i = 0; i < 2; i++) {
            this.arm(i, 0.0F, 0.08F * k, 0.25F * k);
        }

        // ---- idle life: the ribs rise and fall with its breath, its heart beats, the head tilts, the tendrils twitch
        float breath = Mth.sin(age * 0.06F);
        this.cage.xScale *= 1.0F + 0.012F * breath;
        this.ribs(0.03F * breath);
        float beat = (age % 24.0F) / 20.0F;
        float thump = Anim.envelope(beat, 0.0F, 0.05F, 0.0F, 0.15F) + 0.6F * Anim.envelope(beat, 0.25F, 0.05F, 0.0F, 0.15F);
        float pulse = 1.0F + 0.12F * thump;
        this.head.zRot += Mth.sin(age * 0.021F) * 0.07F * (1.0F - k);
        float yaw = Mth.clamp(s.yRot, -45.0F, 45.0F) * Anim.DEG;
        this.neck.yRot += yaw * 0.35F;
        this.head.yRot += yaw * 0.45F;
        this.head.xRot += Mth.clamp(s.xRot, -30.0F, 30.0F) * Anim.DEG * 0.3F;
        float twitch = Mth.sin(age * 0.37F) * Mth.sin(age * 0.11F);
        twitch = twitch > 0.8F ? (twitch - 0.8F) * 2.5F : 0.0F;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.tendrils[i].zRot -= sx * (Mth.sin(age * 0.05F + i * 1.7F) * 0.04F + 0.2F * k + Mth.sin(age * 1.7F + i) * 0.05F * k + twitch * 0.15F);
            this.tips[i].xRot += Mth.sin(age * 0.07F + i) * 0.08F;
        }

        // ---- holding someone: claws dug into the ground, the heart beating harder
        if (s.carrying) {
            for (int i = 0; i < 2; i++) {
                this.claws[i].xRot += 0.25F;
            }
            pulse += 0.05F * thump;
        }

        // ---- stunned: swaying, head hanging, arms gone slack, tendrils drooping
        if (s.mode == Jailer.STUNNED) {
            this.cage.zRot += Mth.sin(age * 0.15F) * 0.06F;
            this.neck.xRot += 0.35F;
            this.head.xRot += 0.3F;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.arm(i, 0.25F, 0.1F, 0.2F);
                this.tendrils[i].zRot += sx * 0.4F;
            }
        }

        // ---- listening: the head snaps aside and the tendrils flare and quiver
        float l = Anim.seconds(s.listen, s.ageInTicks);
        if (l >= 0.0F && l < 1.6F) {
            float e = Anim.envelope(l, 0.0F, 0.12F, 0.6F, 0.7F);
            this.head.zRot += 0.35F * e;
            this.head.xRot -= 0.2F * e;
            this.neck.xRot -= 0.15F * e;
            flare += (0.45F + Mth.sin(l * 45.0F) * 0.12F) * e;
        }

        // ---- the slam: crouch, rear up roaring with the ribs flung open, crash down over its prey, ribs snapping shut
        float t = Anim.seconds(s.slam, s.ageInTicks);
        if (t >= 0.0F && t < 1.5F) {
            float crouch = Anim.envelope(t, 0.0F, 0.2F, 0.05F, 0.25F);
            float rear = Anim.smooth((t - 0.15F) / 0.5F);
            float down = Anim.smooth((t - 0.7F) / 0.2F);
            float back = Anim.smooth((t - 1.1F) / 0.4F);
            float up = rear * (1.0F - down);
            float slam = down * (1.0F - back);
            // the ribcage: up and back as it rears, then down hard in front (1.75 blocks out, on the ground)
            this.cage.y += 2.0F * crouch - 10.0F * up + 4.8F * slam;
            this.cage.z += 3.0F * up - 9.6F * slam;
            this.cage.xRot += -0.35F * up + 0.08F * slam;
            this.ribs(0.65F * up - 0.12F * slam * (1.0F - down * 0.5F));
            if (t > 0.9F && t < 1.2F) {
                float shake = 1.0F - (t - 0.9F) / 0.3F;
                this.cage.y += Mth.sin((t - 0.9F) * 60.0F) * 0.8F * shake;
                this.cage.zRot += Mth.sin((t - 0.9F) * 50.0F) * 0.03F * shake;
            }
            this.body.y += 2.0F * crouch - 3.0F * up;
            this.body.z -= 4.0F * slam;
            this.spine.xRot -= 0.25F * up - 0.15F * slam;
            for (int i = 0; i < 2; i++) {
                this.legs[i].xRot -= 0.3F * crouch - 0.35F * slam;
                this.shins[i].xRot += 0.6F * crouch;
                // arms flung up and wide as it rears, then slammed to the ground either side of the cell
                this.arm(i, -1.6F * up - 0.3F * slam, -0.6F * up + 0.15F * slam, -0.6F * up);
            }
            this.neck.xRot -= 0.4F * up - 0.25F * slam;
            this.head.xRot -= 0.35F * up;
            jawOpen = Math.max(jawOpen, 0.9F * Anim.envelope(t, 0.35F, 0.15F, 0.35F, 0.3F));
            flare += 0.6F * up;
        }

        // ---- trapped: the ribs clench, the cell jolts and the bone lock snaps shut
        float tr = Anim.seconds(s.trap, s.ageInTicks);
        if (tr >= 0.0F && tr < 0.6F) {
            float j = Mth.sin(tr * 18.0F) * (1.0F - tr / 0.6F);
            this.cage.yScale *= 1.0F - 0.04F * j;
            this.ribs(-0.06F * Math.abs(j));
            this.lock.xRot += 0.5F * j;
        }

        // ---- squeezing: the ribs press in on the prisoner, the arms hug the cage, the jaw works
        float sq = Anim.seconds(s.squeeze, s.ageInTicks);
        if (sq >= 0.0F && sq < 1.0F) {
            float e = Anim.envelope(sq, 0.0F, 0.2F, 0.3F, 0.4F);
            this.ribs(-0.14F * e);
            this.cage.xScale *= 1.0F - 0.06F * e;
            for (int i = 0; i < 2; i++) {
                this.arm(i, 0.0F, 0.25F * e, -0.15F * e);
                this.claws[i].xRot += 0.3F * e;
            }
            pulse += 0.15F * e;
            jawOpen = Math.max(jawOpen, 0.3F * e);
        }

        // ---- struck: the ribs rattle, the chains swing, the head turns down to the noise
        float r = Anim.seconds(s.rattle, s.ageInTicks);
        float rattle = 0.0F;
        if (r >= 0.0F && r < 0.5F) {
            rattle = 1.0F - r / 0.5F;
            for (int i = 0; i < this.bars.length; i++) {
                this.bars[i].zRot += Mth.sin(r * 80.0F + i) * 0.06F * rattle;
            }
            this.cage.zRot += Mth.sin(r * 70.0F) * 0.03F * rattle;
            this.head.xRot += 0.35F * Anim.envelope(r, 0.0F, 0.08F, 0.15F, 0.27F);
        }

        // ---- CR4 its grip beats with its heart: a thump, then the ribs ease apart and the cell sags a little -
        // they glow (see JailerRenderer) and only now do blows count
        float lo = Anim.seconds(s.loosen, s.ageInTicks);
        if (s.carrying && lo >= 0.0F && lo < 1.0F) {
            float eased = Anim.envelope(lo, Jailer.CUE_TICKS / 20.0F * 0.5F, Jailer.CUE_TICKS / 20.0F * 0.5F, Jailer.LOOSE_TICKS / 20.0F, 0.2F);
            float cue = Anim.envelope(lo, 0.0F, 0.04F, 0.0F, 0.14F);
            pulse += 0.35F * cue;
            this.cage.y += 1.2F * eased;
            this.ribs(0.1F * eased);
            this.head.xRot -= 0.15F * eased;
            for (int i = 0; i < 2; i++) {
                this.claws[i].xRot -= 0.5F * eased;
            }
            flare += 0.2F * eased;
        }

        // ---- a good heave against the ribs: they bow out, the cage lurches, the Jailer flinches
        float hv = Anim.seconds(s.heave, s.ageInTicks);
        if (hv >= 0.0F && hv < 0.7F) {
            float buckle = Anim.envelope(hv, 0.0F, 0.04F, 0.06F, 0.5F);
            float wob = Mth.sin(hv * 40.0F) * (1.0F - hv / 0.7F);
            this.ribs(0.16F * buckle);
            for (int i = 0; i < this.bars.length; i++) {
                this.bars[i].zRot += Mth.sin(hv * 50.0F + i * 1.7F) * 0.08F * buckle;
            }
            this.cage.y -= 1.5F * buckle;
            this.cage.zRot += 0.04F * wob;
            this.head.xRot -= 0.2F * buckle;
            jawOpen = Math.max(jawOpen, 0.4F * buckle);
        }

        // ---- on guard: a hind leg drawn forward, then a hard backward kick at whoever came too close
        float kicked = Anim.seconds(s.kick, s.ageInTicks);
        if (kicked >= 0.0F && kicked < 1.0F) {
            float wind = Anim.envelope(kicked, 0.0F, 0.35F, 0.05F, 0.1F);
            float strike = Anim.envelope(kicked, 0.4F, 0.06F, 0.12F, 0.4F);
            this.legs[1].xRot -= 0.6F * wind - 1.4F * strike;
            this.shins[1].xRot += 0.4F * wind - 0.5F * strike;
            this.body.z -= 1.0F * wind - 1.5F * strike;
            jawOpen = Math.max(jawOpen, 0.5F * strike);
        }

        // ---- the ribs burst: a roar, arms flung wide, a stagger backwards, the cage sagging
        float b = Anim.seconds(s.cageBreak, s.ageInTicks);
        if (b >= 0.0F && b < 1.2F) {
            float e = Anim.envelope(b, 0.0F, 0.1F, 0.3F, 0.8F);
            for (int i = 0; i < 2; i++) {
                this.arm(i, -0.5F * e, -0.8F * e, 0.3F * e);
            }
            this.body.z += 2.0F * e;
            this.cage.y += 3.0F * e;
            this.cage.xRot += 0.15F * e;
            this.neck.xRot -= 0.4F * e;
            this.head.xRot -= 0.3F * e;
            jawOpen = Math.max(jawOpen, 0.9F * e);
            flare += 0.5F * e;
        }

        // ---- emerging: it claws its way up out of the rock, the arms first, then it rears and roars
        float em = Anim.seconds(s.emerge, s.ageInTicks);
        if (emerging && em >= 0.0F) {
            float rise = Anim.smooth(em / 4.2F);
            float jerk = Mth.sin(em * 9.0F) * 0.8F * (1.0F - rise);
            this.root().y += 58.0F * (1.0F - rise) + jerk;
            this.cage.y -= 6.0F * (1.0F - rise);
            float claw = 1.0F - Anim.smooth((em - 3.2F) / 1.0F);
            for (int i = 0; i < 2; i++) {
                float alt = Mth.sin(em * 5.0F + i * Mth.PI) * 0.35F;
                this.arm(i, (-2.2F + alt) * claw, 0.1F * claw, -0.6F * claw);
            }
            float roar = Anim.envelope(em, 3.6F, 0.2F, 0.6F, 0.4F);
            this.neck.xRot -= 0.5F * roar + 0.3F * claw;
            this.head.xRot -= 0.3F * roar;
            jawOpen = Math.max(jawOpen, 1.0F * roar);
            flare += 0.7F * roar;
            this.ribs(0.3F * roar);
        }

        // ---- hurt: a flinch back from the blow; dying: the legs give way and it sinks onto its ribs
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float h = Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI);
            this.neck.xRot -= 0.3F * h;
            this.head.zRot += 0.15F * h;
            this.cage.y -= 1.0F * h;
            jawOpen = Math.max(jawOpen, 0.5F * h);
            flare += 0.3F * h;
        }
        float dead = Anim.smooth(s.dying / 16.0F);
        if (dead > 0.0F) {
            this.cage.y += 4.5F * dead;
            this.cage.zRot += 0.15F * dead;
            this.body.y += 6.0F * dead;
            for (int i = 0; i < 2; i++) {
                this.legs[i].xRot -= 0.8F * dead;
                this.shins[i].xRot += 1.4F * dead;
                this.arm(i, 0.6F * dead, 0.4F * dead, 0.5F * dead);
            }
            this.neck.xRot += 0.6F * dead;
            jawOpen = Math.max(jawOpen, 0.6F * dead);
            flare = flare * (1.0F - dead) - 0.4F * dead;
        }

        // ---- the face, the heart, the chains: the jaw, the tendrils' flare, the beat, the chains hanging plumb
        this.jaw.xRot += jawOpen;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.tendrils[i].zRot -= sx * flare;
            this.tips[i].xRot -= 0.3F * flare;
        }
        this.heart.xScale = pulse;
        this.heart.yScale = pulse;
        this.heart.zScale = pulse;
        for (int i = 0; i < this.chains.length; i++) {
            this.chains[i].xRot += -this.cage.xRot + Mth.sin(pos - 0.8F + i) * 0.25F * walk + Mth.sin(age * 0.05F + i * 1.3F) * 0.04F
                    + Mth.sin(r * 30.0F + i) * 0.3F * rattle;
            this.chains[i].zRot += -this.cage.zRot + Mth.cos(pos - 0.8F + i) * 0.15F * walk;
        }
    }
}
