package com.thesift.client.model;

import com.thesift.client.renderer.state.CoralOrganRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR3 Fish &amp; Coral Organs, WATER remake - the Sculk Coral Organ (geometry in tools/waterfolk.py): a living
 * organism. Its body breathes, the lid of its giant mouth ('head') lifting a crack with every breath; a dozen eyes
 * each wander, blink and track on their own clock - all of them swing round to whatever swims near, and fix wide
 * open on prey; on every chord its pipes swell one after another. The attack reads clearly: the mouth creaks open,
 * trembling, for a second (the telegraph), then hangs wide while the throat works and the tongue lolls (the pull),
 * then slams shut in a lunging bite. Drawing its hooked line it half-opens and the tongue draws back; firing, the
 * tongue kicks out; reeling a catch in, it chomps.
 */
public class CoralOrganModel extends EntityModel<CoralOrganRenderState> {
    /** = tools/waterfolk.py ORGAN_EYES (sizes in model units). */
    private static final float[] EYE_SIZE = {4, 3, 3, 2, 2, 3, 3, 2, 2, 2, 3, 2};
    private final ModelPart base;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart tongue;
    private final ModelPart[] pipes = new ModelPart[5];
    private final ModelPart[] eyes = new ModelPart[EYE_SIZE.length];
    private final ModelPart[] pupils = new ModelPart[EYE_SIZE.length];
    private final ModelPart[] lids = new ModelPart[EYE_SIZE.length];
    private final ModelPart[] tendrils = new ModelPart[4];
    private final ModelPart[] corals = new ModelPart[6];

    public CoralOrganModel(ModelPart root) {
        super(root);
        this.base = root.getChild("base");
        this.body = this.base.getChild("body");
        this.head = this.body.getChild("head");
        this.tongue = this.body.getChild("tongue");
        for (int i = 0; i < this.pipes.length; i++) {
            this.pipes[i] = this.head.getChild("pipe_" + i);
        }
        for (int i = 0; i < this.eyes.length; i++) {
            this.eyes[i] = this.head.getChild("eye_" + i);
            this.pupils[i] = this.eyes[i].getChild("pupil_" + i);
            this.lids[i] = this.eyes[i].getChild("lid_" + i);
        }
        for (int i = 0; i < this.tendrils.length; i++) {
            this.tendrils[i] = this.base.getChild("tendril_" + i);
        }
        for (int i = 0; i < this.corals.length; i++) {
            this.corals[i] = this.base.getChild("coral_" + i);
        }
    }

    @Override
    public void setupAnim(CoralOrganRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        // breathing: the body swells and sinks, the mouth lifts a crack, the tongue stirs, the pipes sway in the current
        float breath = Mth.sin(age * 0.07F);
        this.body.xScale = 1.0F + 0.025F * breath;
        this.body.zScale = 1.0F + 0.025F * breath;
        this.body.yScale = 1.0F + 0.035F * breath;
        this.head.xRot -= 0.035F * (0.5F + 0.5F * breath);
        this.tongue.xRot += Mth.sin(age * 0.09F) * 0.05F;
        for (int i = 0; i < this.pipes.length; i++) {
            this.pipes[i].zRot += Mth.sin(age * 0.05F + i * 1.3F) * 0.015F;
            this.pipes[i].xRot += Mth.sin(age * 0.04F + i * 0.7F) * 0.012F;
        }
        // a chord: it sings through its pipes, each swelling in turn, the mouth lifting as it sounds
        float chord = Anim.seconds(s.chord, s.ageInTicks);
        float open = 0.0F;
        if (chord >= 0.0F && chord < 2.2F) {
            open = 0.12F * Anim.envelope(chord, 0.0F, 0.15F, 0.6F, 0.8F);
            for (int i = 0; i < this.pipes.length; i++) {
                int order = (i * 2 + 1) % this.pipes.length;
                float swell = Anim.envelope(chord, order * 0.15F, 0.08F, 0.2F, 0.5F);
                this.pipes[i].yScale = 1.0F + 0.1F * swell;
                this.pipes[i].xScale = 1.0F + 0.12F * swell;
                this.pipes[i].zScale = 1.0F + 0.12F * swell;
            }
        }
        // the telegraph: the mouth creaks open, trembling; then it hangs wide and sucks, the throat working
        float gape = Anim.seconds(s.gape, s.ageInTicks);
        if (gape >= 0.0F) {
            open = Math.max(open, 0.8F * Anim.smooth(gape) + 0.2F * Anim.smooth((gape - 1.0F) / 0.3F));
            this.head.xRot += Mth.sin(s.ageInTicks * 2.3F) * 0.03F * Anim.clamp01(gape);
            if (gape > 1.0F) {
                this.body.yScale += 0.04F * Mth.sin(s.ageInTicks * 0.9F);
                this.tongue.xRot += 0.2F + 0.1F * Mth.sin(s.ageInTicks * 0.7F);
            }
        }
        // drawing its hooked line: half open, the tongue drawn back and trembling; firing, the tongue kicks out
        float charge = Anim.seconds(s.charge, s.ageInTicks);
        if (charge >= 0.0F && charge < 1.5F) {
            float pull = Anim.smooth(charge / 1.3F);
            open = Math.max(open, 0.45F * Anim.smooth(charge / 0.6F));
            this.tongue.z += 2.0F * pull;
            this.tongue.yRot += Mth.sin(s.ageInTicks * 3.1F) * 0.04F * pull;
        }
        float fire = Anim.seconds(s.fire, s.ageInTicks);
        if (fire >= 0.0F && fire < 0.8F) {
            float kick = Anim.envelope(fire, 0.0F, 0.04F, 0.06F, 0.6F);
            open = Math.max(open, 0.5F * (1.0F - Anim.smooth(fire / 0.6F)));
            this.tongue.z -= 4.0F * kick;
            this.tongue.xScale = 1.0F + 0.2F * kick;
            this.body.yScale -= 0.05F * kick;
        }
        this.head.xRot -= open;
        // the bite: it slams shut from wide open, lunging forward, squashing, and chews once
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.8F) {
            this.head.xRot -= 1.0F * (1.0F - Anim.smooth(bite / 0.1F));
            float hit = Anim.envelope(bite, 0.06F, 0.05F, 0.08F, 0.45F);
            this.body.z -= 3.0F * hit;
            this.body.xRot += 0.14F * hit;
            this.head.xRot += 0.05F * hit;
            this.body.xScale += 0.06F * hit;
            this.body.zScale += 0.06F * hit;
            this.body.yScale -= 0.08F * hit;
        }
        // reeling its catch in: a chomp
        float clamp = Anim.seconds(s.clamp, s.ageInTicks);
        if (clamp >= 0.0F && clamp < 0.6F) {
            float chomp = Anim.envelope(clamp, 0.0F, 0.08F, 0.05F, 0.3F);
            this.head.xRot -= 0.5F * chomp;
            this.body.yScale -= 0.05F * chomp;
        }
        // the eyes: each wanders, blinks and tracks on its own; all of them fix on prey
        boolean hunting = s.aiming || gape >= 0.0F;
        for (int i = 0; i < this.eyes.length; i++) {
            ModelPart eye = this.eyes[i];
            float restYaw = eye.yRot;
            float restPitch = -eye.xRot;
            float yaw;
            float pitch;
            if (s.watching) {
                // a little late and a little off, each in its own way
                float lag = Mth.sin(age * (0.05F + 0.01F * i) + i * 1.7F) * (hunting ? 0.03F : 0.1F);
                yaw = s.aimYaw + lag;
                pitch = s.aimPitch + lag * 0.5F;
            } else {
                yaw = restYaw + Mth.sin(age * (0.031F + 0.007F * i) + i * 2.1F) * 0.7F;
                pitch = restPitch + Mth.sin(age * (0.027F + 0.005F * i) + i) * 0.35F;
            }
            float relYaw = Mth.clamp(Mth.wrapDegrees((yaw - restYaw) * Mth.RAD_TO_DEG) * Mth.DEG_TO_RAD, -1.3F, 1.3F);
            float relPitch = Mth.clamp(pitch - restPitch, -1.0F, 1.0F);
            float reach = EYE_SIZE[i] * 0.16F;
            this.pupils[i].x -= Mth.sin(relYaw) * reach;
            this.pupils[i].y -= Mth.sin(relPitch) * reach;
            eye.yRot += relYaw * 0.25F;
            eye.xRot -= relPitch * 0.25F;
            float dilate = hunting ? 1.1F : 1.0F;
            this.pupils[i].xScale = dilate;
            this.pupils[i].yScale = dilate;
            // lids: heavy when calm, wide open on prey, a blink now and then, squeezed shut when hurt
            float lid = hunting ? 0.05F : 0.28F;
            int period = 70 + (i * 37) % 60;
            float phase = (age + i * 23.0F) % period;
            if (phase < 5.0F) {
                lid = Math.max(lid, 1.0F - Math.abs(phase - 2.5F) / 2.5F);
            }
            if (s.hurtTicks >= 0.0F) {
                lid = Math.max(lid, 1.0F - s.hurtTicks / 10.0F);
            }
            this.lids[i].yScale = Math.max(0.02F, lid);
        }
        // sensor tendrils twitch at every sound, the coral fans sway
        for (int i = 0; i < this.tendrils.length; i++) {
            float t = Mth.sin(age * (0.7F + i * 0.13F)) * Mth.sin(age * 0.17F + i);
            this.tendrils[i].zRot += t * 0.25F;
            this.tendrils[i].xRot += (hunting ? -0.3F : 0.0F) + Mth.sin(age * 0.11F + i) * 0.08F;
        }
        for (int i = 0; i < this.corals.length; i++) {
            this.corals[i].zRot += Mth.sin(age * 0.06F + i * 2.1F) * 0.05F;
        }
    }
}
