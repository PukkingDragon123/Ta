package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.MiniBossRenderState;
import com.thesift.entity.boss.Thumper;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Thumper. Idle, it plods like a tortoise, head swinging, tendrils twitching. Its attacks:
 *
 * <ul>
 *   <li>emerge: buried to the eyes, it claws its way up out of the floor, front first, and roars</li>
 *   <li>stomp: rears back on its hind legs, forefeet pawing the air, and crashes down</li>
 *   <li>charge: head low, hind feet scraping, then a thundering gallop</li>
 *   <li>tail sweep: cocks its tail to one side and whips round in a full circle</li>
 *   <li>beam: lifts its head, jaws gaping wide, shuddering as the sculk song pours out</li>
 *   <li>boulders: dips its head, rakes the ground, flings each boulder up with a toss</li>
 *   <li>burrow / erupt: nose down, digging furiously as it sinks; bursts out rearing up</li>
 *   <li>exposed: slumped and splayed, head lolling, its vents gaping and glowing</li>
 * </ul>
 * Its three vents have bone lids that flip open whenever they are exposed, and the sculk inside
 * lights up.
 */
public class ThumperModel extends EntityModel<MiniBossRenderState> {
    private static final String[] VENTS = {"vent_top", "vent_left", "vent_right"};
    private final ModelPart body;
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
    private final ModelPart[] lids = new ModelPart[3];
    private final ModelPart[] lit = new ModelPart[3];

    public ThumperModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
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
        for (int i = 0; i < 3; i++) {
            ModelPart vent = this.body.getChild(VENTS[i]);
            this.lids[i] = vent.getChild(VENTS[i] + "_lid");
            this.lit[i] = vent.getChild(VENTS[i] + "_lit");
        }
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
        this.neck.xRot = Mth.sin(age * 0.05F) * 0.05F + Mth.sin(pos) * 0.06F * walk;
        this.head.yRot = s.yRot * Anim.DEG * 0.7F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F;
        this.tail.yRot = Mth.sin(age * 0.12F + pos) * 0.25F;
        this.jaw.xRot = Math.max(0.0F, Mth.sin(age * 0.04F)) * 0.06F;
        boolean angry = st != Thumper.IDLE && st != Thumper.EXPOSED;
        this.leftBrow.zRot = angry || s.enraged ? -0.35F : 0.0F;
        this.rightBrow.zRot = angry || s.enraged ? 0.35F : 0.0F;

        // --- the Warden's tendrils: a restless twitch, a shiver when it is hurt or attacking
        float shiver = (s.hurtTicks >= 0.0F ? 1.0F : 0.0F) + (angry ? 0.5F : 0.0F);
        for (int i = 0; i < 2; i++) {
            float sgn = i == 0 ? 1.0F : -1.0F;
            float tw = Mth.sin(age * (0.11F + i * 0.02F) + i) * 0.18F + Mth.sin(age * 1.7F + i) * 0.08F * shiver;
            this.tendrils[i].zRot += sgn * tw;
            this.tendrils[i].xRot += Mth.sin(age * 0.07F + i * 2.0F) * 0.12F;
        }

        switch (st) {
            case Thumper.EMERGE -> this.emerge(t, age);
            case Thumper.STOMP -> this.slam(Anim.envelope(t, 0.0F, 18.0F, 2.0F, 2.5F), Anim.envelope(t, 20.0F, 1.5F, 2.0F, 10.0F), age);
            case Thumper.CHARGE_WINDUP -> {
                float k = Anim.smooth(t / 8.0F);
                this.neck.xRot += 0.35F * k;
                this.neck.z -= 1.0F * k;
                this.hindLeft.xRot = Mth.sin(age * 0.9F) * 0.6F * k;
                this.hindRight.xRot = Mth.sin(age * 0.9F + Mth.PI) * 0.6F * k;
                this.body.zRot = Mth.sin(age * 1.3F) * 0.03F * k;
                this.body.xRot = 0.06F * k;
                this.jaw.xRot += 0.2F * k;
            }
            case Thumper.CHARGE -> {
                float gallop = age * 1.1F;
                this.frontLeft.xRot = Mth.cos(gallop);
                this.hindRight.xRot = Mth.cos(gallop);
                this.frontRight.xRot = -Mth.cos(gallop);
                this.hindLeft.xRot = -Mth.cos(gallop);
                this.body.y -= Math.abs(Mth.sin(gallop)) * 1.5F;
                this.body.xRot = 0.08F;
                this.neck.xRot = 0.4F;
                this.neck.z -= 1.5F;
                this.jaw.xRot = 0.3F;
                this.tail.yRot = Mth.sin(gallop * 2.0F) * 0.4F;
            }
            case Thumper.TAIL_SWEEP -> {
                float coil = Anim.smooth(t / Thumper.SWEEP_START) * (1.0F - Anim.smooth((t - Thumper.SWEEP_END) / 10.0F));
                boolean whip = t >= Thumper.SWEEP_START && t <= Thumper.SWEEP_END + 2;
                // cocked to one side, then dragged out straight behind by the spin
                this.tail.yRot = whip ? -0.9F + Mth.sin(age * 2.0F) * 0.1F : 1.1F * coil;
                this.tail.xRot = -0.35F * coil;
                this.body.zRot += (whip ? 0.12F : -0.08F) * coil;
                this.neck.z += 2.0F * coil;
                this.neck.xRot += 0.3F * coil;
                this.frontLeft.zRot = -0.25F * coil;
                this.frontRight.zRot = 0.25F * coil;
                this.hindLeft.zRot = -0.25F * coil;
                this.hindRight.zRot = 0.25F * coil;
            }
            case Thumper.EXPOSED -> {
                float k = Anim.smooth(t / 6.0F) * (1.0F - Anim.smooth((t - Thumper.EXPOSED_TICKS + 10.0F) / 10.0F));
                this.body.y += 3.0F * k;
                this.frontLeft.zRot = -0.6F * k;
                this.hindLeft.zRot = -0.6F * k;
                this.frontRight.zRot = 0.6F * k;
                this.hindRight.zRot = 0.6F * k;
                this.neck.xRot = Mth.lerp(k, this.neck.xRot, 0.55F);
                this.head.zRot = Mth.sin(age * 0.25F) * 0.3F * k;
                this.head.yRot = Mth.lerp(k, this.head.yRot, Mth.sin(age * 0.15F) * 0.3F);
                // panting, jaw slack
                this.jaw.xRot = (0.4F + Math.max(0.0F, Mth.sin(age * 0.5F)) * 0.25F) * k;
                this.tail.yRot *= 1.0F - 0.8F * k;
            }
            case Thumper.BEAM -> {
                float charge = Anim.smooth(t / Thumper.BEAM_CHARGE);
                boolean firing = t >= Thumper.BEAM_CHARGE && t < Thumper.BEAM_END;
                float k = Math.min(charge, t < Thumper.BEAM_END ? 1.0F : 1.0F - Anim.smooth((t - Thumper.BEAM_END) / 12.0F));
                this.neck.xRot -= 0.3F * k;
                this.head.xRot -= 0.15F * k;
                this.jaw.xRot = (firing ? 1.0F : 0.5F * charge) * k;
                this.frontLeft.xRot = -0.2F * k;
                this.frontRight.xRot = -0.2F * k;
                this.hindLeft.xRot = 0.25F * k;
                this.hindRight.xRot = 0.25F * k;
                if (firing) {
                    this.neck.xRot += Mth.sin(age * 2.2F) * 0.03F;
                    this.body.zRot += Mth.sin(age * 1.9F) * 0.02F;
                }
            }
            case Thumper.BOULDERS -> {
                float rake = Anim.envelope(t, 0.0F, 4.0F, 30.0F, 8.0F);
                this.frontLeft.xRot = -0.5F * rake + Math.max(0.0F, Mth.sin(age * 0.9F)) * 0.6F * rake;
                this.frontRight.xRot = -0.5F * rake + Math.max(0.0F, Mth.sin(age * 0.9F + Mth.PI)) * 0.6F * rake;
                for (int throwAt = 16; throwAt <= 32; throwAt += 8) {
                    float dip = Anim.envelope(t, throwAt - 6.0F, 4.0F, 0.5F, 1.5F);
                    float toss = Anim.envelope(t, throwAt, 1.0F, 1.0F, 4.0F);
                    this.neck.xRot += 0.7F * dip - 0.6F * toss;
                    this.jaw.xRot += 0.5F * dip + 0.3F * toss;
                    this.body.xRot += 0.08F * dip - 0.1F * toss;
                }
            }
            case Thumper.BURROW -> {
                float k = Anim.smooth(t / Thumper.BURROW_TICKS);
                this.body.y += 22.0F * k * k;
                this.body.xRot = 0.35F * Anim.smooth(t / 6.0F);
                this.frontLeft.xRot = -0.8F + Mth.sin(age * 1.3F) * 0.9F;
                this.frontRight.xRot = -0.8F + Mth.sin(age * 1.3F + Mth.PI) * 0.9F;
                this.hindLeft.xRot = 0.4F + Mth.sin(age * 1.1F) * 0.5F;
                this.hindRight.xRot = 0.4F + Mth.sin(age * 1.1F + Mth.PI) * 0.5F;
                this.neck.xRot = 0.5F;
            }
            case Thumper.ERUPT -> {
                float out = Anim.backOut(Anim.clamp01(t / Thumper.ERUPT_IMPACT));
                float rear = 1.0F - Anim.smooth((t - 4.0F) / 12.0F);
                this.body.y += 22.0F * (1.0F - out);
                this.body.xRot = -0.55F * rear;
                this.frontLeft.xRot = Mth.lerp(rear, this.frontLeft.xRot, -1.2F);
                this.frontRight.xRot = Mth.lerp(rear, this.frontRight.xRot, -1.0F);
                this.neck.xRot -= 0.5F * rear;
                this.jaw.xRot = 0.9F * rear;
            }
            case Thumper.ROAR -> {
                float k = Anim.envelope(t, 0.0F, 6.0F, 24.0F, 10.0F);
                this.body.xRot = -0.18F * k;
                this.neck.xRot -= 0.55F * k;
                this.head.xRot -= 0.3F * k;
                this.jaw.xRot = 1.05F * k;
                this.head.zRot = Mth.sin(age * 1.6F) * 0.06F * k;
                this.frontLeft.xRot = -0.3F * k;
                this.frontRight.xRot = -0.3F * k;
            }
            default -> {
            }
        }
        this.vents(s, st, t, age);
    }

    /** Buried to the eyes, it claws its way up out of the floor and roars. */
    private void emerge(float t, float age) {
        float climb = Anim.smooth(t / (Thumper.EMERGE_TICKS - 22.0F));
        float under = 1.0F - climb;
        this.body.y += 30.0F * under;
        // front first: nose up while it hauls itself out
        this.body.xRot = -0.6F * Mth.sin(climb * Mth.PI);
        float claw = Mth.sin(climb * Mth.PI) + 0.2F * under;
        this.frontLeft.xRot = -1.0F * claw + Mth.sin(age * 0.45F) * 0.7F * claw;
        this.frontRight.xRot = -1.0F * claw + Mth.sin(age * 0.45F + Mth.PI) * 0.7F * claw;
        this.hindLeft.xRot = 0.5F * claw;
        this.hindRight.xRot = 0.5F * claw;
        this.neck.xRot -= 0.25F * claw;
        this.head.yRot = Mth.sin(age * 0.2F) * 0.3F * under;
        // the roar, then a last shake of the head to settle
        float roar = Anim.envelope(t, Thumper.EMERGE_TICKS - 22.0F, 3.0F, 12.0F, 6.0F);
        this.neck.xRot -= 0.6F * roar;
        this.head.xRot -= 0.35F * roar;
        this.jaw.xRot = Math.max(this.jaw.xRot, 1.1F * roar);
        this.head.zRot = Mth.sin(age * 1.8F) * 0.08F * roar;
    }

    /** Rears up (`up`) and crashes down (`hit`). */
    private void slam(float up, float hit, float age) {
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
    }

    /** The bone lids flip open over the vents and the sculk inside lights up. */
    private void vents(MiniBossRenderState s, int st, float t, float age) {
        float open = switch (st) {
            case Thumper.EXPOSED -> Anim.smooth(t / 6.0F) * (1.0F - Anim.smooth((t - Thumper.EXPOSED_TICKS + 8.0F) / 8.0F));
            case Thumper.STOMP -> Anim.smooth((t - Thumper.STOMP_IMPACT) / 4.0F);
            case Thumper.BEAM -> Anim.backOut(Anim.clamp01(t / 10.0F)) * (1.0F - Anim.smooth((t - Thumper.BEAM_END - 4.0F) / 8.0F));
            case Thumper.ERUPT -> Anim.smooth((t - Thumper.ERUPT_IMPACT) / 4.0F);
            default -> 0.0F;
        };
        // a rattle as it is struck, or as the beam builds in it
        float rattle = s.hurtTicks >= 0.0F ? Mth.sin(s.hurtTicks * 3.0F) * 0.12F * (1.0F - Math.min(1.0F, s.hurtTicks / 10.0F)) : 0.0F;
        if (st == Thumper.BEAM && t < Thumper.BEAM_CHARGE) {
            rattle += Mth.sin(age * 2.5F) * 0.05F;
        }
        for (int i = 0; i < 3; i++) {
            this.lids[i].xRot = -1.9F * open + rattle * (i == 0 ? 1.0F : -1.0F);
            boolean glow = open > 0.25F && (open > 0.7F || Mth.sin(age * 2.0F + i) > 0.0F);
            this.lit[i].visible = glow || (st == Thumper.BEAM && t < Thumper.BEAM_CHARGE && Mth.sin(age * 1.5F + i * 2.0F) > 0.4F);
        }
    }
}
