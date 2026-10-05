package com.thesift.client.model;

import com.thesift.client.renderer.state.JailerRenderState;
import com.thesift.entity.cave.Jailer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Jailer. Long, slow strides with the hips rolling and the cell bobbing a beat behind; idle,
 * it breathes, cocks its faceless head and its antler tendrils twitch. Hunting, it hunches low and
 * creeps, tendrils raised and quivering. Hearing something, the head snaps aside and the tendrils
 * flare. The slam: a crouch, the cell heaved high over its head, a hard slam down in front with a
 * jaw-open roar and a shuddering impact, then the cell hauled back up. Carrying, the cell has a
 * floor; it squeezes (bars pressed in), rattles when struck, bursts open (arms flung wide, a
 * stagger) and slowly regrows its bars. Emerging, it claws its way up out of the rock, the cell last.
 */
public class JailerModel extends EntityModel<JailerRenderState> {
    /** The order bars snap in as the cell is broken (and the reverse is the order they regrow). */
    private static final int[] BREAK_ORDER = {4, 9, 1, 7, 11, 2, 6, 10, 0, 5, 8, 3};
    private final ModelPart body;
    private final ModelPart chest;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart cage;
    private final ModelPart floor;
    private final ModelPart[] bars = new ModelPart[Jailer.CAGE_WHOLE];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] claws = new ModelPart[2];
    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart[] tinesLow = new ModelPart[2];
    private final ModelPart[] tinesHigh = new ModelPart[2];

    public JailerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.chest = this.body.getChild("chest");
        this.neck = this.chest.getChild("neck");
        this.head = this.neck.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.cage = root.getChild("cage");
        this.floor = this.cage.getChild("floor");
        for (int i = 0; i < this.bars.length; i++) {
            this.bars[i] = this.cage.getChild("bar_" + i);
        }
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.arms[i] = this.chest.getChild(s + "_arm");
            this.forearms[i] = this.arms[i].getChild(s + "_forearm");
            this.claws[i] = this.forearms[i].getChild(s + "_hand").getChild(s + "_claw");
            this.tendrils[i] = this.head.getChild(s + "_tendril");
            this.tinesLow[i] = this.tendrils[i].getChild(s + "_tine_low");
            this.tinesHigh[i] = this.tendrils[i].getChild(s + "_tine_high");
        }
    }

    private void arm(int i, float ax, float az, float fx, float fz) {
        float sx = i == 0 ? 1.0F : -1.0F;
        this.arms[i].xRot += ax;
        this.arms[i].zRot += az * sx;
        this.forearms[i].xRot += fx;
        this.forearms[i].zRot += fz * sx;
    }

    @Override
    public void setupAnim(JailerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float k = s.stalk;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.6F) * (s.mode == Jailer.EMERGING ? 0.0F : 1.0F);
        float pos = s.walkAnimationPos * (0.55F - 0.12F * k);
        float sw = Mth.sin(pos);

        // ---- the bars that still stand, and a floor when someone is inside
        int missing = Jailer.CAGE_WHOLE - Mth.clamp(s.cage, 0, Jailer.CAGE_WHOLE);
        for (int i = 0; i < BREAK_ORDER.length; i++) {
            this.bars[BREAK_ORDER[i]].visible = i >= missing;
        }
        this.floor.visible = s.carrying;

        // ---- walking: long strides, knees lifting, the hips rolling; the cell bobs a beat behind
        for (int i = 0; i < 2; i++) {
            float phase = i == 0 ? sw : -sw;
            this.legs[i].xRot += phase * (0.5F + 0.15F * k) * walk - 0.15F * k;
            this.shins[i].xRot += Math.max(0.0F, -phase) * 0.7F * walk + 0.3F * k;
        }
        this.body.y -= Math.abs(Mth.cos(pos)) * 1.1F * walk;
        this.body.y += 2.0F * k;
        this.body.zRot += sw * 0.06F * walk;
        this.chest.zRot -= sw * 0.05F * walk;
        this.cage.y -= Math.abs(Mth.cos(pos - 0.5F)) * 0.8F * walk;
        this.cage.zRot += Mth.sin(pos - 0.6F) * 0.035F * walk;
        this.cage.xRot += Mth.cos(pos * 2.0F - 0.6F) * 0.015F * walk;

        // ---- idle life: breathing, a slow head tilt, twitching tendrils
        this.chest.xRot += Mth.sin(age * 0.06F) * 0.025F + 0.22F * k;
        this.neck.xRot += 0.15F * k;
        this.head.xRot -= 0.3F * k;
        this.head.zRot += Mth.sin(age * 0.021F) * 0.07F * (1.0F - k);
        this.head.yRot += s.yRot * Anim.DEG * 0.5F;
        this.head.xRot += s.xRot * Anim.DEG * 0.3F;
        float twitch = Mth.sin(age * 0.37F) * Mth.sin(age * 0.11F);
        twitch = twitch > 0.8F ? (twitch - 0.8F) * 2.5F : 0.0F;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.tendrils[i].zRot += sx * (Mth.sin(age * 0.05F + i * 1.7F) * 0.04F + 0.25F * k + Mth.sin(age * 1.7F + i) * 0.05F * k
                    + twitch * 0.15F);
            this.tinesLow[i].zRot += sx * Mth.sin(age * 0.07F + i) * 0.05F;
            this.tinesHigh[i].zRot -= sx * Mth.sin(age * 0.06F + i * 2.0F) * 0.05F;
        }

        // ---- carrying someone: leaning back against the weight, claws locked tight
        if (s.carrying) {
            this.chest.xRot -= 0.06F;
            for (int i = 0; i < 2; i++) {
                this.claws[i].xRot += 0.25F;
            }
        }

        // ---- stunned: swaying, head hanging, arms gone slack
        if (s.mode == Jailer.STUNNED) {
            this.chest.zRot += Mth.sin(age * 0.15F) * 0.1F;
            this.head.xRot += 0.5F;
            for (int i = 0; i < 2; i++) {
                this.arm(i, 0.35F, 0.1F, 0.3F, 0.0F);
            }
        }

        // ---- listening: the head snaps aside and the tendrils flare and quiver
        float l = Anim.seconds(s.listen, s.ageInTicks);
        if (l >= 0.0F && l < 1.6F) {
            float e = Anim.envelope(l, 0.0F, 0.12F, 0.6F, 0.7F);
            this.head.zRot += 0.35F * e;
            this.head.xRot -= 0.2F * e;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.tendrils[i].zRot += sx * (0.45F + Mth.sin(l * 45.0F) * 0.12F) * e;
                this.tinesLow[i].zRot += sx * 0.3F * e;
            }
        }

        // ---- the slam
        float t = Anim.seconds(s.slam, s.ageInTicks);
        if (t >= 0.0F && t < 1.5F) {
            float crouch = Anim.envelope(t, 0.0F, 0.2F, 0.05F, 0.25F);
            float lift = Anim.smooth((t - 0.2F) / 0.5F);
            float down = Anim.smooth((t - 0.7F) / 0.2F);
            float back = Anim.smooth((t - 1.1F) / 0.4F);
            float up = lift * (1.0F - down);
            float slam = down * (1.0F - back);
            // the cell: up and back over its head, then down hard in front (1.75 blocks out, on the ground)
            this.cage.y += -29.2F * up + 4.8F * slam;
            this.cage.z += 16.4F * up - 9.6F * slam;
            this.cage.xRot += -0.5F * up;
            if (t > 0.9F && t < 1.2F) {
                float shake = 1.0F - (t - 0.9F) / 0.3F;
                this.cage.y += Mth.sin((t - 0.9F) * 60.0F) * 0.8F * shake;
                this.chest.zRot += Mth.sin((t - 0.9F) * 50.0F) * 0.04F * shake;
            }
            this.body.y += 2.5F * crouch;
            for (int i = 0; i < 2; i++) {
                this.legs[i].xRot -= 0.3F * crouch;
                this.shins[i].xRot += 0.6F * crouch;
                this.arm(i, -0.6F * slam, -0.7F * (up + slam), -1.7F * up + 0.2F * slam, -0.2F * up - 0.8F * slam);
                this.tendrils[i].zRot += (i == 0 ? 1.0F : -1.0F) * 0.4F * slam;
            }
            this.chest.xRot += -0.35F * up + 0.5F * slam;
            this.body.z -= 2.0F * slam;
            this.head.xRot += -0.3F * up + 0.3F * slam;
            this.jaw.xRot += 0.7F * Anim.envelope(t, 0.65F, 0.1F, 0.25F, 0.3F);
        }

        // ---- trapped: the cell jolts as it closes
        float tr = Anim.seconds(s.trap, s.ageInTicks);
        if (tr >= 0.0F && tr < 0.6F) {
            float j = Mth.sin(tr * 18.0F) * (1.0F - tr / 0.6F);
            this.cage.yScale *= 1.0F - 0.06F * j;
            this.cage.xScale *= 1.0F + 0.03F * j;
            this.cage.zScale *= 1.0F + 0.03F * j;
        }

        // ---- squeezing: the long hands press in and the bars bow
        float sq = Anim.seconds(s.squeeze, s.ageInTicks);
        if (sq >= 0.0F && sq < 1.0F) {
            float e = Anim.envelope(sq, 0.0F, 0.2F, 0.3F, 0.4F);
            this.cage.xScale *= 1.0F - 0.14F * e;
            this.cage.zScale *= 1.0F - 0.1F * e;
            this.cage.yScale *= 1.0F + 0.04F * e;
            for (int i = 0; i < 2; i++) {
                this.arm(i, 0.0F, 0.2F * e, -0.2F * e, -0.1F * e);
                this.claws[i].xRot += 0.3F * e;
            }
            this.chest.xRot += 0.1F * e;
            this.jaw.xRot += 0.3F * e;
        }

        // ---- struck: the cell rattles, the bars jiggle, the head turns down to the noise
        float r = Anim.seconds(s.rattle, s.ageInTicks);
        if (r >= 0.0F && r < 0.5F) {
            float fade = 1.0F - r / 0.5F;
            this.cage.zRot += Mth.sin(r * 70.0F) * 0.07F * fade;
            this.cage.xRot += Mth.sin(r * 55.0F + 1.0F) * 0.04F * fade;
            for (int i = 0; i < this.bars.length; i++) {
                this.bars[i].zRot += Mth.sin(r * 80.0F + i) * 0.05F * fade;
            }
            this.head.xRot += 0.35F * Anim.envelope(r, 0.0F, 0.08F, 0.15F, 0.27F);
        }

        // ---- CR4 its grip beats with its heart: a thump, then the claws ease open and the cell sags
        // a little - the bars glow (see JailerRenderer) and only now do blows count
        float lo = Anim.seconds(s.loosen, s.ageInTicks);
        if (s.carrying && lo >= 0.0F && lo < 1.0F) {
            float eased = Anim.envelope(lo, Jailer.CUE_TICKS / 20.0F * 0.5F, Jailer.CUE_TICKS / 20.0F * 0.5F, Jailer.LOOSE_TICKS / 20.0F, 0.2F);
            float thump = Anim.envelope(lo, 0.0F, 0.04F, 0.0F, 0.14F);
            this.chest.xScale *= 1.0F + 0.05F * thump;
            this.chest.zScale *= 1.0F + 0.05F * thump;
            this.cage.y += 1.2F * eased;
            this.cage.xScale *= 1.0F + 0.05F * eased;
            this.cage.zScale *= 1.0F + 0.05F * eased;
            this.head.xRot -= 0.15F * eased;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.claws[i].xRot -= 0.5F * eased;
                this.arm(i, 0.0F, -0.12F * eased, 0.0F, 0.08F * eased);
                this.tendrils[i].zRot += sx * 0.2F * eased;
            }
        }

        // ---- a good heave against the bars: they buckle outwards, the cell lurches, the Jailer flinches
        float hv = Anim.seconds(s.heave, s.ageInTicks);
        if (hv >= 0.0F && hv < 0.7F) {
            float buckle = Anim.envelope(hv, 0.0F, 0.04F, 0.06F, 0.5F);
            float wob = Mth.sin(hv * 40.0F) * (1.0F - hv / 0.7F);
            this.cage.xScale *= 1.0F + 0.12F * buckle;
            this.cage.zScale *= 1.0F + 0.08F * buckle;
            this.cage.y -= 1.5F * buckle;
            this.cage.zRot += 0.08F * wob;
            for (int i = 0; i < this.bars.length; i++) {
                this.bars[i].zRot += Mth.sin(hv * 50.0F + i * 1.7F) * 0.12F * buckle;
            }
            this.chest.xRot -= 0.2F * buckle;
            this.head.xRot -= 0.2F * buckle;
            this.jaw.xRot += 0.4F * buckle;
        }

        // ---- on guard: one leg drawn back, then a hard stamping kick at whoever came too close
        float kicked = Anim.seconds(s.kick, s.ageInTicks);
        if (kicked >= 0.0F && kicked < 1.0F) {
            float wind = Anim.envelope(kicked, 0.0F, 0.35F, 0.05F, 0.1F);
            float strike = Anim.envelope(kicked, 0.4F, 0.06F, 0.12F, 0.4F);
            this.legs[1].xRot += 0.7F * wind - 1.3F * strike;
            this.shins[1].xRot += 0.5F * wind - 0.3F * strike;
            this.chest.xRot -= 0.15F * wind - 0.1F * strike;
            this.body.z += 1.5F * strike;
            this.jaw.xRot += 0.5F * strike;
        }

        // ---- the cell bursts: arms flung wide, a stagger backwards, the frame tipping
        float b = Anim.seconds(s.cageBreak, s.ageInTicks);
        if (b >= 0.0F && b < 1.2F) {
            float e = Anim.envelope(b, 0.0F, 0.1F, 0.3F, 0.8F);
            for (int i = 0; i < 2; i++) {
                this.arm(i, 0.4F * e, -0.9F * e, 0.3F * e, 0.0F);
            }
            this.chest.xRot -= 0.5F * e;
            this.body.z += 2.0F * e;
            this.head.xRot -= 0.4F * e;
            this.jaw.xRot += 0.7F * e;
            this.cage.y += 5.0F * e;
            this.cage.xRot += 0.4F * e;
        }

        // ---- emerging: claws first, then the body hauls itself up out of the rock, the cell last
        float em = Anim.seconds(s.emerge, s.ageInTicks);
        if (s.mode == Jailer.EMERGING && em >= 0.0F) {
            float rise = Anim.smooth(em / 4.2F);
            float jerk = Mth.sin(em * 9.0F) * 0.8F * (1.0F - rise);
            this.root().y += 58.0F * (1.0F - rise) + jerk;
            this.cage.y += 34.0F * (1.0F - Anim.smooth((em - 2.6F) / 2.0F));
            float claw = 1.0F - Anim.smooth((em - 3.2F) / 1.0F);
            for (int i = 0; i < 2; i++) {
                float alt = Mth.sin(em * 5.0F + i * Mth.PI) * 0.35F;
                this.arm(i, (-2.4F + alt) * claw, 0.0F, -0.6F * claw, 0.0F);
            }
            this.head.xRot -= 0.5F * claw;
            this.chest.xRot += 0.3F * claw;
        }
    }
}
