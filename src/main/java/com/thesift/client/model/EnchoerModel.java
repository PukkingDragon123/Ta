package com.thesift.client.model;

import com.thesift.client.renderer.state.EnchoerRenderState;
import com.thesift.entity.Enchoer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Echoer, a deer spirit (CAVE: geometry and paint in tools/echoer.py, the Sculk-mob pipeline). Every pose
 * is a smoothed amount from the entity or an envelope, so nothing snaps.
 *
 * <ul>
 *   <li>skipping through the air (tied to limbSwing): a slow bounding gait - forelegs reach out together
 *   and fold, hind legs drive back, the body rises with each bound and rocks over it, head held level</li>
 *   <li>hovering: it treads the air, legs paddling slowly, rising and sinking a little</li>
 *   <li>on the ground: it stands and breathes, ears and tail flicking; walking, a light diagonal walk</li>
 *   <li>singing: every note opens its mouth and lifts its head (the renderer flares the antler tips)</li>
 *   <li>listening: head cocked, ears forward, swaying; dancing: it rears and paws the air, tossing its antlers</li>
 *   <li>the gift bow (3 s): a lift of the head, then down onto one knee, the other foreleg forward, neck and
 *   antlers lowered towards you; presenting: head held low, the gift floating between the antlers</li>
 *   <li>a nod for visitors, sleep (lying with legs folded, head turned to its flank), a hurt flinch and a
 *   death fall (the renderer then pops it)</li>
 *   <li>CAVE: the crystal chimes on its antlers swing behind every movement and keep hanging plumb as it lowers
 *   its head; its antlers spread a little and the star on its chest swells with every note it sings</li>
 * </ul>
 */
public class EnchoerModel extends EntityModel<EnchoerRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart star;
    private final ModelPart[] antlers = new ModelPart[2];
    private final ModelPart[] chimes = new ModelPart[2];
    /** [side][front 0 / hind 1]. */
    private final ModelPart[][] legs = new ModelPart[2][2];
    private final ModelPart[][] shins = new ModelPart[2][2];
    private final ModelPart[][] hooves = new ModelPart[2][2];

    public EnchoerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.neck = this.body.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.star = this.body.getChild("star");
        for (int k = 0; k < 2; k++) {
            String side = SIDES[k];
            this.ears[k] = this.head.getChild(side + "_ear");
            this.antlers[k] = this.head.getChild(side + "_antler");
            this.chimes[k] = this.antlers[k].getChild(side + "_antler_upper").getChild(side + "_chime");
            for (int f = 0; f < 2; f++) {
                String leg = side + (f == 0 ? "_front" : "_hind");
                this.legs[k][f] = this.body.getChild(leg + "_leg");
                this.shins[k][f] = this.legs[k][f].getChild(leg + "_shin");
                this.hooves[k][f] = this.shins[k][f].getChild(leg + "_hoof");
            }
        }
    }

    @Override
    public void setupAnim(EnchoerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float sleep = s.sleep;
        float awake = 1.0F - sleep;
        float moving = Math.min(1.0F, s.walkAnimationSpeed * 1.6F) * awake;
        float air = s.air * awake;
        float skip = air * moving;
        float hover = air * (1.0F - moving);
        float walk = (1.0F - s.air) * moving;
        float stand = (1.0F - s.air) * (1.0F - moving) * awake;
        float c = s.walkAnimationPos * Enchoer.SKIP_RATE;

        // --- breathing, and a little life in the ears and tail
        float br = Mth.sin(age * 0.07F);
        this.neck.xRot += br * 0.02F;
        float flick = Anim.envelope((age * 0.05F) % 5.0F, 4.2F, 0.12F, 0.1F, 0.25F);
        this.ears[0].zRot += flick * 0.45F;
        this.ears[1].zRot -= Anim.envelope((age * 0.05F + 2.3F) % 5.0F, 4.2F, 0.12F, 0.1F, 0.25F) * 0.45F;
        this.tail.xRot += Mth.sin(age * 0.6F) * 0.12F * Anim.envelope((age * 0.04F + 1.0F) % 4.0F, 3.0F, 0.15F, 0.4F, 0.3F) * awake;

        // --- the skip through the air: forelegs reach out and fold, hind legs drive back, the body rides each bound
        for (int k = 0; k < 2; k++) {
            float p = c + k * 0.35F;
            float reach = Mth.sin(p);
            this.legs[k][0].xRot += (-0.2F - 0.85F * reach) * skip;
            this.shins[k][0].xRot += (0.25F + 1.15F * Math.max(0.0F, -Mth.sin(p + 0.7F))) * skip;
            this.hooves[k][0].xRot += 0.35F * Math.max(0.0F, -Mth.sin(p + 0.7F)) * skip;
            float drive = Mth.sin(p - 0.45F);
            this.legs[k][1].xRot += (0.15F + 0.75F * drive) * skip;
            this.shins[k][1].xRot += (-0.2F - 0.8F * Math.max(0.0F, Mth.sin(p + 1.9F))) * skip;
            this.hooves[k][1].xRot += 0.3F * Math.max(0.0F, -drive) * skip;
        }
        float rise = 0.5F + 0.5F * Mth.sin(c - 0.6F);
        this.body.y -= (1.0F + 1.8F * rise) * skip;
        this.body.xRot -= Mth.cos(c) * 0.11F * skip;
        this.neck.xRot += (Mth.cos(c) * 0.08F - 0.08F) * skip;
        this.head.xRot += Mth.cos(c) * 0.05F * skip;
        this.tail.xRot -= (0.25F + 0.15F * Mth.sin(c)) * skip;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.ears[k].yRot += sx * 0.35F * skip;
        }

        // --- treading the air while it hovers
        float t = age * 0.09F;
        for (int k = 0; k < 2; k++) {
            float ph = k == 0 ? 0.0F : Mth.PI;
            this.legs[k][0].xRot += (-0.3F + Mth.sin(t + ph) * 0.3F) * hover;
            this.shins[k][0].xRot += (0.6F + Mth.sin(t + ph + 1.2F) * 0.35F) * hover;
            this.legs[k][1].xRot += (0.25F + Mth.sin(t + ph + Mth.HALF_PI) * 0.25F) * hover;
            this.shins[k][1].xRot += (-0.35F - Math.max(0.0F, Mth.sin(t + ph + 2.5F)) * 0.35F) * hover;
        }
        this.body.y += (Mth.sin(t * 0.8F) * 0.8F - 0.8F) * hover;
        this.body.xRot += Mth.sin(t * 0.8F + 1.0F) * 0.03F * hover;

        // --- walking on the ground: a light diagonal walk
        for (int k = 0; k < 2; k++) {
            float ph = k == 0 ? 0.0F : Mth.PI;
            float swing = Mth.cos(c * 1.6F + ph);
            this.legs[k][0].xRot += swing * 0.45F * walk;
            this.shins[k][0].xRot += Math.max(0.0F, -Mth.sin(c * 1.6F + ph)) * 0.6F * walk;
            this.legs[k][1].xRot -= swing * 0.4F * walk;
            this.shins[k][1].xRot -= Math.max(0.0F, Mth.sin(c * 1.6F + ph)) * 0.4F * walk;
        }
        this.body.y -= Math.abs(Mth.cos(c * 1.6F)) * 0.4F * walk;
        this.neck.xRot += Mth.sin(c * 3.2F) * 0.04F * walk;

        // --- standing: a weight shift now and then
        this.body.zRot += Mth.sin(age * 0.021F) * 0.015F * stand;

        // --- head look (clamped, shared between neck and head), never while asleep
        float yaw = Mth.clamp(s.yRot, -40.0F, 40.0F) * Anim.DEG * awake;
        float pitch = Mth.clamp(s.xRot, -25.0F, 25.0F) * Anim.DEG * awake;
        this.neck.yRot += yaw * 0.4F;
        this.head.yRot += yaw * 0.6F;
        this.head.xRot += pitch * 0.6F;

        // --- its song: every note opens the mouth and lifts the head
        float v = Mth.clamp(s.voice, 0.0F, 1.2F);
        float jawOpen = 0.45F * v;
        this.head.xRot -= 0.12F * v;
        this.neck.xRot -= 0.04F * v;

        // --- listening: head cocked, ears forward, swaying a little
        float ls = s.listen * awake;
        this.head.zRot += 0.22F * ls;
        this.neck.xRot -= 0.12F * ls;
        this.body.zRot += Mth.sin(age * 0.07F) * 0.025F * ls;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.ears[k].yRot -= sx * 0.4F * ls;
            this.ears[k].zRot -= sx * 0.25F * ls;
        }

        // --- dancing: it rears in the air and paws, tossing its antlers to the beat
        float d = s.dance * awake;
        if (d > 0.0F) {
            float b = age * 0.45F;
            this.body.xRot -= 0.3F * d;
            this.body.y -= 1.2F * d + Math.abs(Mth.sin(b)) * 0.8F * d;
            for (int k = 0; k < 2; k++) {
                float ph = k == 0 ? 0.0F : Mth.PI;
                this.legs[k][0].xRot -= (0.95F + 0.45F * Mth.sin(b + ph)) * d;
                this.shins[k][0].xRot += (0.9F + 0.4F * Mth.sin(b + ph + 0.8F)) * d;
                this.legs[k][1].xRot -= 0.15F * d;
            }
            this.neck.xRot += (0.2F + Mth.sin(b) * 0.14F) * d;
            this.head.zRot += Mth.sin(b * 0.5F) * 0.18F * d;
            this.tail.xRot -= 0.35F * d;
            jawOpen = Math.max(jawOpen, 0.2F * d);
        }

        // --- a nod for a visitor (1.2 s)
        float nod = Anim.seconds(s.nod, s.ageInTicks);
        if (nod >= 0.0F && nod < 1.2F) {
            float e = Anim.envelope(nod, 0.0F, 0.3F, 0.2F, 0.6F);
            this.neck.xRot += 0.3F * e;
            this.head.xRot += 0.25F * e;
        }

        // --- the gift bow (3 s): a lift of the head, then down onto one knee, neck and antlers lowered to you
        float bowT = Anim.seconds(s.bow, s.ageInTicks);
        if (bowT >= 0.0F && bowT < 3.0F) {
            float lift = Anim.envelope(bowT, 0.0F, 0.35F, 0.1F, 0.3F);
            float bow = Anim.envelope(bowT, 0.45F, 0.7F, 1.1F, 0.75F);
            this.neck.xRot -= 0.25F * lift;
            this.body.xRot -= 0.05F * lift;
            this.body.xRot += 0.2F * bow;
            this.body.y += 1.6F * bow;
            this.legs[0][0].xRot -= 0.7F * bow;
            this.shins[0][0].xRot += 0.15F * bow;
            this.legs[1][0].xRot -= 0.1F * bow;
            this.shins[1][0].xRot += 1.5F * bow;
            this.hooves[1][0].xRot += 0.5F * bow;
            for (int k = 0; k < 2; k++) {
                this.legs[k][1].xRot -= 0.3F * bow;
                this.shins[k][1].xRot += 0.08F * bow;
            }
            this.neck.xRot += 0.42F * bow;
            this.head.xRot += 0.3F * bow;
            this.tail.xRot += 0.3F * bow;
        }
        // --- presenting: head held low, the gift floating between its antlers
        float pr = s.present * awake;
        this.neck.xRot += 0.32F * pr;
        this.head.xRot += 0.22F * pr + Mth.sin(age * 0.06F) * 0.04F * pr;
        this.body.xRot += 0.08F * pr;
        for (int k = 0; k < 2; k++) {
            this.legs[k][1].xRot -= 0.1F * pr;
        }

        // --- sleep: lying in the grass, legs folded under, head turned back onto its flank
        if (sleep > 0.0F) {
            this.body.y += 8.2F * sleep;
            this.body.xRot *= awake;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.legs[k][0].xRot = Mth.lerp(sleep, this.legs[k][0].xRot, 1.25F);
                this.shins[k][0].xRot = Mth.lerp(sleep, this.shins[k][0].xRot, -2.5F);
                this.hooves[k][0].xRot = Mth.lerp(sleep, this.hooves[k][0].xRot, 0.6F);
                this.legs[k][1].xRot = Mth.lerp(sleep, this.legs[k][1].xRot, -1.05F);
                this.shins[k][1].xRot = Mth.lerp(sleep, this.shins[k][1].xRot, 2.0F);
                this.legs[k][1].zRot += sx * 0.12F * sleep;
            }
            this.neck.yRot += 1.0F * sleep;
            this.neck.xRot += 0.45F * sleep + br * 0.03F * sleep;
            this.head.xRot += 0.35F * sleep;
            this.head.zRot += 0.35F * sleep;
            this.ears[0].zRot += 0.3F * sleep;
            this.ears[1].zRot -= 0.3F * sleep;
        }

        // --- hurt: a flinch that eases out (no snapping)
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float h = Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F);
            float k = Mth.sin(h * Mth.PI) * (1.0F - 0.4F * h);
            this.neck.xRot -= 0.3F * k;
            this.head.xRot -= 0.15F * k;
            this.body.zRot += 0.08F * k;
            this.ears[0].zRot += 0.5F * k;
            this.ears[1].zRot -= 0.5F * k;
            jawOpen = Math.max(jawOpen, 0.3F * k);
        }
        // --- death: its legs give way and it sinks onto its side (the renderer then pops it)
        float roll = Anim.smooth(s.dying / 14.0F);
        if (roll > 0.0F) {
            this.body.y += 6.0F * roll;
            this.body.zRot += 1.1F * roll;
            this.neck.xRot += 0.5F * roll;
            this.head.xRot += 0.2F * roll;
            for (int k = 0; k < 2; k++) {
                this.shins[k][0].xRot += 0.9F * roll;
                this.legs[k][1].xRot -= 0.4F * roll;
            }
            jawOpen = Math.max(jawOpen, 0.25F * roll);
        }
        this.jaw.xRot += jawOpen;

        // --- CAVE: its song spreads the antlers and swells the star; the chimes swing behind every movement and
        // hang plumb however far it lowers its head (bow, presenting, sleep)
        float dip = this.neck.xRot - 0.42F + this.head.xRot + 0.17F + this.body.xRot;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.antlers[k].zRot += sx * 0.06F * v;
            float swing = Mth.sin(age * 0.13F + k * 2.1F) * 0.12F + Mth.sin(c - 1.2F + k) * 0.3F * skip + Mth.sin(age * 0.45F - 0.8F) * 0.25F * d;
            this.chimes[k].zRot += swing * sx;
            this.chimes[k].xRot += Mth.cos(age * 0.1F + k) * 0.08F - dip;
        }
        float swell = 1.0F + 0.3F * v + 0.05F * Mth.sin(age * 0.1F) * awake;
        this.star.xScale = swell;
        this.star.yScale = swell;
    }
}
