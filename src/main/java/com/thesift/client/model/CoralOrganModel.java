package com.thesift.client.model;

import com.thesift.client.renderer.state.CoralOrganRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR3 Fish &amp; Coral Organs - the Sculk Coral Organ (tools/fish_art.py): the sac at the back breathes; on every
 * chord it squeezes like a bellows and the pipes swell one after another as their notes sound; sensor tendrils
 * twitch; the bone harpoon horn turns to aim, draws back trembling while it charges, and kicks on firing; and
 * the whole reef clenches when it clamps down on its catch.
 */
public class CoralOrganModel extends EntityModel<CoralOrganRenderState> {
    private final ModelPart base;
    private final ModelPart[] pipes = new ModelPart[5];
    private final ModelPart sac;
    private final ModelPart launcher;
    private final ModelPart[] tendrils = new ModelPart[4];
    private final ModelPart[] corals = new ModelPart[6];

    public CoralOrganModel(ModelPart root) {
        super(root);
        this.base = root.getChild("base");
        for (int i = 0; i < this.pipes.length; i++) {
            this.pipes[i] = this.base.getChild("pipe_" + i);
        }
        this.sac = this.base.getChild("sac");
        this.launcher = this.base.getChild("launcher");
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
        // breathing: the sac swells and sinks, the pipes sway a hair in the current
        float breath = Mth.sin(age * 0.08F);
        this.sac.xScale = 1.0F + 0.06F * breath;
        this.sac.yScale = 1.0F + 0.08F * breath;
        this.sac.zScale = 1.0F + 0.06F * breath;
        for (int i = 0; i < this.pipes.length; i++) {
            this.pipes[i].zRot += Mth.sin(age * 0.05F + i * 1.3F) * 0.015F;
            this.pipes[i].xRot += Mth.sin(age * 0.04F + i * 0.7F) * 0.012F;
        }
        // a chord: the sac squeezes like a bellows and the pipes swell in turn as they sound
        float chord = Anim.seconds(s.chord, s.ageInTicks);
        if (chord >= 0.0F && chord < 2.2F) {
            float squeeze = Anim.envelope(chord, 0.0F, 0.15F, 0.6F, 0.8F);
            this.sac.xScale -= 0.14F * squeeze;
            this.sac.zScale -= 0.14F * squeeze;
            this.sac.yScale -= 0.1F * squeeze;
            for (int i = 0; i < this.pipes.length; i++) {
                int order = (i * 2 + 1) % this.pipes.length;
                float swell = Anim.envelope(chord, order * 0.15F, 0.08F, 0.2F, 0.5F);
                this.pipes[i].yScale = 1.0F + 0.1F * swell;
                this.pipes[i].xScale = 1.0F + 0.12F * swell;
                this.pipes[i].zScale = 1.0F + 0.12F * swell;
            }
        }
        // sensor tendrils twitch at every sound
        for (int i = 0; i < this.tendrils.length; i++) {
            float t = Mth.sin(age * (0.7F + i * 0.13F)) * Mth.sin(age * 0.17F + i);
            this.tendrils[i].zRot += t * 0.25F;
            this.tendrils[i].xRot += (s.aiming ? -0.3F : 0.0F) + Mth.sin(age * 0.11F + i) * 0.08F;
        }
        for (int i = 0; i < this.corals.length; i++) {
            this.corals[i].zRot += Mth.sin(age * 0.06F + i * 2.1F) * 0.05F;
        }
        // the harpoon horn turns to aim (it cannot look behind itself: the reef turns for that)
        float yaw = Mth.clamp(s.aimYaw, -1.0F, 1.0F);
        float pitch = Mth.clamp(s.aimPitch, -0.35F, 1.2F);
        this.launcher.yRot = s.aiming ? yaw : Mth.sin(age * 0.03F) * 0.25F;
        this.launcher.xRot = s.aiming ? -pitch : Mth.sin(age * 0.045F) * 0.08F;
        float charge = Anim.seconds(s.charge, s.ageInTicks);
        if (charge >= 0.0F && charge < 1.5F) {
            // drawing back, trembling harder and harder
            float pull = Anim.smooth(charge / 1.3F);
            this.launcher.z += 2.5F * pull;
            this.launcher.yRot += Mth.sin(s.ageInTicks * 3.1F) * 0.04F * pull;
            this.launcher.xScale = 1.0F + 0.12F * pull;
            this.launcher.yScale = 1.0F + 0.12F * pull;
        }
        float fire = Anim.seconds(s.fire, s.ageInTicks);
        if (fire >= 0.0F && fire < 0.8F) {
            // the kick: it lunges, then settles back
            float kick = Anim.envelope(fire, 0.0F, 0.04F, 0.06F, 0.6F);
            this.launcher.z -= 3.0F * kick;
            this.launcher.xScale = 1.0F + 0.25F * kick;
            this.launcher.yScale = 1.0F + 0.25F * kick;
            this.base.yScale = 1.0F - 0.05F * kick;
        }
        float clamp = Anim.seconds(s.clamp, s.ageInTicks);
        if (clamp >= 0.0F && clamp < 0.6F) {
            float squeeze = Anim.envelope(clamp, 0.0F, 0.05F, 0.1F, 0.4F);
            this.base.xScale = 1.0F + 0.06F * squeeze;
            this.base.zScale = 1.0F + 0.06F * squeeze;
            this.base.yScale = 1.0F - 0.08F * squeeze;
            for (int i = 0; i < this.pipes.length; i++) {
                this.pipes[i].zRot += (i < 2 ? 0.12F : i > 2 ? -0.12F : 0.0F) * squeeze;
            }
        }
    }
}
