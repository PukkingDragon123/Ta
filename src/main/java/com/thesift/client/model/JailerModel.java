package com.thesift.client.model;

import com.thesift.client.renderer.state.JailerRenderState;
import com.thesift.entity.cave.Jailer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Jailer (CAVE v4, tools/jailer.py): a hulking, eyeless sculk warden whose own ribcage is the cell. The ribcage
 * with its shoulders, arms (its front legs) and head is the 'cage' part, a child of the hips ('body'): every pose
 * moves the hips - lowering, lunging, pitching and rolling them - and the torso follows as one piece, while the
 * hind legs (from the root) are re-seated on the hips and bend or lean so their feet stay planted, and the front
 * legs swing to keep their claws on the ground. Nothing comes apart.
 *
 * <ul>
 *   <li>walking on four limbs: the hind legs stride, the front legs knuckle forward in turn, the hips roll and bob;
 *   the chains on its corner ribs and the bone keys swing after it; stalking, it sinks low, head thrust out,
 *   tendrils raised and quivering</li>
 *   <li>idle: the ribs rise and fall with its breath, the heart of soul light beats, the head tilts and the tendrils
 *   twitch; hearing something, the head snaps aside and the tendrils flare</li>
 *   <li>the slam: a crouch; it rears up on its hind legs with a roar, front legs lifting and the ribs flaring open like
 *   a cage door; then it lunges and crashes down over you, front legs slamming into the ground and the ribs snapping
 *   shut; the lock jolts as the cell closes</li>
 *   <li>holding a prisoner: its grip beats with its heart - a thump, then the ribs ease apart (the moment to strike);
 *   it squeezes, the ribs pressing in; struck, the ribs rattle; a good heave bows them out; a rescuer gets a backward
 *   kick; the ribs burst with a roar and a stagger and grow back</li>
 *   <li>stunned it sways; hurt it flinches; dying its legs give way and it sinks onto its keel, front legs splayed;
 *   emerging it claws its way up out of the rock and roars</li>
 * </ul>
 */
public class JailerModel extends EntityModel<JailerRenderState> {
    /** The order the ribs snap in as the cell is broken (and the reverse is the order they regrow). */
    private static final int[] BREAK_ORDER = {2, 9, 4, 7, 0, 11, 3, 8, 5, 6, 1, 10};
    /** The ribs carrying chain_0..3 (tools/jailer.py CHAIN_RIBS). */
    private static final int[] CHAIN_RIBS = {0, 6, 5, 11};
    private static final String[] SIDES = {"left", "right"};
    /** Hind leg length (hip to sole) and the front legs' reach, in model units; the shoulders' distance in front of the hips. */
    private static final float LEG = 22.0F;
    private static final float ARM = 34.0F;
    private static final float SHOULDER_FWD = 32.4F;
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
    // the pose being built: hip offsets and turns, rib flare, and each front leg's swing
    private float drop;
    private float lunge;
    private float pitch;
    private float roll;
    private float spread;
    private final float[] armX = new float[2];
    private final float[] armZ = new float[2];
    private final float[] foreX = new float[2];

    public JailerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.spine = this.body.getChild("spine");
        this.keys = this.body.getChild("keys");
        this.cage = this.body.getChild("cage");
        this.floor = this.cage.getChild("floor");
        this.heart = this.cage.getChild("heart");
        this.neck = this.cage.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        for (int i = 0; i < this.bars.length; i++) {
            this.bars[i] = this.cage.getChild("bar_" + i);
        }
        for (int i = 0; i < this.chains.length; i++) {
            this.chains[i] = this.bars[CHAIN_RIBS[i]].getChild("chain_" + i);
        }
        // the padlock hangs from the foot of the front left rib
        this.lock = this.bars[0].getChild("bar_0_mid").getChild("bar_0_low").getChild("lock");
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

    /** Swings front leg i: forward/back (negative x: forward), out (negative z: away from the body), forearm bend. */
    private void arm(int i, float ax, float az, float fx) {
        this.armX[i] += ax;
        this.armZ[i] += az;
        this.foreX[i] += fx;
    }

    @Override
    public void setupAnim(JailerRenderState s) {
        super.setupAnim(s);
        this.drop = 0.0F;
        this.lunge = 0.0F;
        this.pitch = 0.0F;
        this.roll = 0.0F;
        this.spread = 0.0F;
        for (int i = 0; i < 2; i++) {
            this.armX[i] = 0.0F;
            this.armZ[i] = 0.0F;
            this.foreX[i] = 0.0F;
        }
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

        // ---- walking on four limbs: hind legs stride, the front legs knuckle forward in turn, the hips roll and bob
        for (int i = 0; i < 2; i++) {
            float phase = i == 0 ? sw : -sw;
            this.legs[i].xRot += phase * 0.5F * walk;
            this.shins[i].xRot += Math.max(0.0F, -phase) * 0.6F * walk;
            this.feet[i].xRot -= phase * 0.3F * walk;
            this.arm(i, -phase * 0.4F * walk, 0.0F, Math.max(0.0F, phase) * 0.35F * walk);
            this.claws[i].xRot += Math.max(0.0F, -phase) * 0.3F * walk;
        }
        this.drop -= Math.abs(Mth.cos(pos)) * 0.9F * walk;
        this.roll += sw * 0.04F * walk;
        this.keys.zRot += Mth.sin(pos - 1.0F) * 0.3F * walk + Mth.sin(age * 0.05F) * 0.04F;
        this.keys.xRot += Mth.cos(pos - 1.2F) * 0.15F * walk;

        // ---- stalking: sunk low on bent legs, the head thrust forward and down, tendrils raised and quivering
        this.drop += 2.5F * k;
        this.neck.xRot += 0.15F * k;
        this.head.xRot += 0.1F * k;
        for (int i = 0; i < 2; i++) {
            this.arm(i, 0.0F, 0.0F, 0.2F * k);
        }

        // ---- idle life: the ribs rise and fall with its breath, its heart beats, the head tilts, the tendrils twitch
        float breath = Mth.sin(age * 0.06F);
        this.cage.xScale *= 1.0F + 0.012F * breath;
        this.spread += 0.03F * breath;
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

        // ---- stunned: swaying, head hanging, tendrils drooping
        if (s.mode == Jailer.STUNNED) {
            this.roll += Mth.sin(age * 0.15F) * 0.04F;
            this.drop += 1.0F;
            this.neck.xRot += 0.35F;
            this.head.xRot += 0.3F;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
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

        // ---- the slam (impact at 0.9 s): a crouch; it rears up on its hind legs roaring, front legs lifting and the
        // ribs flaring open; then it lunges and crashes down over its prey, front legs slamming down, ribs snapping shut
        float t = Anim.seconds(s.slam, s.ageInTicks);
        if (t >= 0.0F && t < 1.5F) {
            float crouch = Anim.envelope(t, 0.0F, 0.2F, 0.05F, 0.25F);
            float rear = Anim.smooth((t - 0.15F) / 0.5F);
            float down = Anim.smooth((t - 0.7F) / 0.2F);
            float back = Anim.smooth((t - 1.1F) / 0.4F);
            float up = rear * (1.0F - down);
            float slam = down * (1.0F - back);
            this.drop += 2.0F * crouch - 1.0F * up + 0.5F * slam;
            this.pitch += 0.08F * crouch - 0.42F * up + 0.22F * slam;
            this.lunge -= 8.0F * slam;
            this.spread += 0.28F * up - 0.06F * slam;
            if (t > 0.9F && t < 1.2F) {
                float shake = 1.0F - (t - 0.9F) / 0.3F;
                this.roll += Mth.sin((t - 0.9F) * 50.0F) * 0.025F * shake;
                this.drop += Mth.sin((t - 0.9F) * 60.0F) * 0.4F * shake;
            }
            for (int i = 0; i < 2; i++) {
                this.arm(i, -0.7F * up, -0.15F * up, -0.4F * up + 0.15F * slam);
                this.claws[i].xRot += 0.4F * slam;
            }
            this.neck.xRot -= 0.3F * up - 0.2F * slam;
            this.head.xRot -= 0.25F * up;
            jawOpen = Math.max(jawOpen, 0.9F * Anim.envelope(t, 0.35F, 0.15F, 0.35F, 0.3F));
            flare += 0.6F * up;
        }

        // ---- trapped: the ribs clench, the cell jolts and the bone lock snaps shut
        float tr = Anim.seconds(s.trap, s.ageInTicks);
        if (tr >= 0.0F && tr < 0.6F) {
            float j = Mth.sin(tr * 18.0F) * (1.0F - tr / 0.6F);
            this.cage.yScale *= 1.0F - 0.03F * j;
            this.spread -= 0.06F * Math.abs(j);
            this.lock.xRot += 0.5F * j;
        }

        // ---- squeezing: the ribs press in on the prisoner, the front legs hug the cage, the jaw works
        float sq = Anim.seconds(s.squeeze, s.ageInTicks);
        if (sq >= 0.0F && sq < 1.0F) {
            float e = Anim.envelope(sq, 0.0F, 0.2F, 0.3F, 0.4F);
            this.spread -= 0.12F * e;
            this.cage.xScale *= 1.0F - 0.05F * e;
            for (int i = 0; i < 2; i++) {
                this.arm(i, 0.0F, 0.12F * e, -0.15F * e);
                this.claws[i].xRot += 0.3F * e;
            }
            pulse += 0.15F * e;
            jawOpen = Math.max(jawOpen, 0.3F * e);
        }

        // ---- struck: the ribs rattle, the chains swing, the head turns down to the noise
        float r = Anim.seconds(s.rattle, s.ageInTicks);
        float rattle = r >= 0.0F && r < 0.5F ? 1.0F - r / 0.5F : 0.0F;
        if (rattle > 0.0F) {
            for (int i = 0; i < this.bars.length; i++) {
                this.bars[i].zRot += Mth.sin(r * 80.0F + i) * 0.05F * rattle;
            }
            this.roll += Mth.sin(r * 70.0F) * 0.015F * rattle;
            this.head.xRot += 0.35F * Anim.envelope(r, 0.0F, 0.08F, 0.15F, 0.27F);
        }

        // ---- CR4 its grip beats with its heart: a thump, then the ribs ease apart and the torso sags a little - they
        // glow (see JailerRenderer) and only now do blows count
        float lo = Anim.seconds(s.loosen, s.ageInTicks);
        if (s.carrying && lo >= 0.0F && lo < 1.0F) {
            float eased = Anim.envelope(lo, Jailer.CUE_TICKS / 20.0F * 0.5F, Jailer.CUE_TICKS / 20.0F * 0.5F, Jailer.LOOSE_TICKS / 20.0F, 0.2F);
            float cue = Anim.envelope(lo, 0.0F, 0.04F, 0.0F, 0.14F);
            pulse += 0.35F * cue;
            this.drop += 0.8F * eased;
            this.spread += 0.1F * eased;
            this.head.xRot -= 0.15F * eased;
            for (int i = 0; i < 2; i++) {
                this.claws[i].xRot -= 0.5F * eased;
            }
            flare += 0.2F * eased;
        }

        // ---- a good heave against the ribs: they bow out, the torso lurches back, the Jailer flinches
        float hv = Anim.seconds(s.heave, s.ageInTicks);
        if (hv >= 0.0F && hv < 0.7F) {
            float buckle = Anim.envelope(hv, 0.0F, 0.04F, 0.06F, 0.5F);
            float wob = Mth.sin(hv * 40.0F) * (1.0F - hv / 0.7F);
            this.spread += 0.14F * buckle;
            for (int i = 0; i < this.bars.length; i++) {
                this.bars[i].zRot += Mth.sin(hv * 50.0F + i * 1.7F) * 0.06F * buckle;
            }
            this.pitch -= 0.04F * buckle;
            this.roll += 0.02F * wob;
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
            this.pitch += 0.04F * strike;
            jawOpen = Math.max(jawOpen, 0.5F * strike);
        }

        // ---- the ribs burst: a roar, front legs flung wide, a stagger back onto its haunches
        float b = Anim.seconds(s.cageBreak, s.ageInTicks);
        if (b >= 0.0F && b < 1.2F) {
            float e = Anim.envelope(b, 0.0F, 0.1F, 0.3F, 0.8F);
            for (int i = 0; i < 2; i++) {
                this.arm(i, -0.3F * e, -0.45F * e, 0.2F * e);
            }
            this.lunge += 2.0F * e;
            this.pitch -= 0.1F * e;
            this.neck.xRot -= 0.4F * e;
            this.head.xRot -= 0.3F * e;
            jawOpen = Math.max(jawOpen, 0.9F * e);
            flare += 0.5F * e;
        }

        // ---- emerging: it claws its way up out of the rock, front legs first, then it rears and roars
        float em = Anim.seconds(s.emerge, s.ageInTicks);
        if (emerging && em >= 0.0F) {
            float rise = Anim.smooth(em / 4.2F);
            float jerk = Mth.sin(em * 9.0F) * 0.8F * (1.0F - rise);
            this.root().y += 58.0F * (1.0F - rise) + jerk;
            this.pitch -= 0.3F * (1.0F - rise);
            float claw = 1.0F - Anim.smooth((em - 3.2F) / 1.0F);
            for (int i = 0; i < 2; i++) {
                float alt = Mth.sin(em * 5.0F + i * Mth.PI) * 0.35F;
                this.arm(i, (-1.6F + alt) * claw, -0.1F * claw, -0.5F * claw);
            }
            float roar = Anim.envelope(em, 3.6F, 0.2F, 0.6F, 0.4F);
            this.pitch -= 0.15F * roar;
            this.neck.xRot -= 0.5F * roar + 0.3F * claw;
            this.head.xRot -= 0.3F * roar;
            jawOpen = Math.max(jawOpen, 1.0F * roar);
            flare += 0.7F * roar;
            this.spread += 0.15F * roar;
        }

        // ---- hurt: a flinch back from the blow
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float h = Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI);
            this.pitch -= 0.06F * h;
            this.neck.xRot -= 0.3F * h;
            this.head.zRot += 0.15F * h;
            jawOpen = Math.max(jawOpen, 0.5F * h);
            flare += 0.3F * h;
        }
        // ---- dying: the hind legs give way and it sinks onto its keel, front legs splaying out, head sagging
        float dead = Anim.smooth(s.dying / 16.0F);
        if (dead > 0.0F) {
            this.drop += 5.0F * dead;
            this.pitch += 0.05F * dead;
            this.roll += 0.1F * dead;
            for (int i = 0; i < 2; i++) {
                this.arm(i, 0.0F, -0.35F * dead, 0.3F * dead);
            }
            this.neck.xRot += 0.6F * dead;
            jawOpen = Math.max(jawOpen, 0.6F * dead);
            flare = flare * (1.0F - dead) - 0.4F * dead;
        }

        this.applyBody();
        this.applyLimbs();

        // ---- the ribs flare as one; the face, the heart; the chains hang plumb from their ribs and swing
        for (int i = 0; i < this.bars.length; i++) {
            this.bars[i].zRot -= this.spread * (i < 6 ? 1.0F : -1.0F);
        }
        this.jaw.xRot += jawOpen;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.tendrils[i].zRot -= sx * flare;
            this.tips[i].xRot -= 0.3F * flare;
        }
        // the padlock hangs plumb from its rib, however the rib flares or the hips pitch and roll
        this.lock.xRot -= this.pitch;
        this.lock.zRot -= this.bars[0].zRot + 1.15F + this.roll;
        this.heart.xScale = pulse;
        this.heart.yScale = pulse;
        this.heart.zScale = pulse;
        for (int i = 0; i < this.chains.length; i++) {
            float side = CHAIN_RIBS[i] < 6 ? 1.0F : -1.0F;
            this.chains[i].xRot += -this.pitch + Mth.sin(pos - 0.8F + i) * 0.25F * walk + Mth.sin(age * 0.05F + i * 1.3F) * 0.04F
                    + Mth.sin(r * 30.0F + i) * 0.3F * rattle;
            // cancel whatever turned its rib away from rest (the flare, a rattle), and the hips' roll
            this.chains[i].zRot += -(this.bars[CHAIN_RIBS[i]].zRot + 1.15F * side) - this.roll + Mth.cos(pos - 0.8F + i) * 0.12F * walk;
        }
    }

    /** The hips carry the torso: lowered, lunged, pitched and rolled as one. */
    private void applyBody() {
        this.body.y += this.drop;
        this.body.z += this.lunge;
        this.body.xRot += this.pitch;
        this.body.zRot += this.roll;
    }

    /**
     * The hind legs stay seated on the hips and planted on the ground: moved with the hips, leaning back by the
     * lunge and bending at the knee as the hips sink. The front legs (on the shoulders) swing to keep their claws on
     * the ground however the torso pitches or sinks, on top of their own movement.
     */
    private void applyLimbs() {
        float bend = this.drop / LEG;
        float lean = -this.lunge / LEG;
        for (int i = 0; i < 2; i++) {
            this.legs[i].y += this.drop;
            this.legs[i].z += this.lunge;
            this.legs[i].xRot += lean - bend;
            this.shins[i].xRot += 2.0F * bend;
            this.feet[i].xRot -= bend;
        }
        float shoulderDrop = this.drop + SHOULDER_FWD * Mth.sin(this.pitch);
        float reach = shoulderDrop > 0.0F ? (float) Math.acos(Math.max(0.3F, 1.0F - shoulderDrop / ARM)) : 0.0F;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.arms[i].xRot += this.armX[i] - this.pitch - reach;
            this.arms[i].zRot += this.armZ[i] * sx - this.roll;
            this.forearms[i].xRot += this.foreX[i];
        }
    }
}
