package com.thesift.client.music;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.music.Instrument;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;

/**
 * INS free play: the instrument in first person while you play it. It is raised from below into
 * its place in front of you - the guitar across the lower right with its neck reaching left, the
 * harp upright at the right, the flute from your lips out to the right, the drums low before you,
 * the chimes held up at the right - and it answers every note: the guitar bucks with the strum, the
 * drums dip under each hit and spring back, the chimes sway, the flute breathes. The points are the
 * first-person bases of tools/instrument_models.py FAMILY (the play models' first-person display
 * transforms are solved for exactly this translation).
 */
public final class FreePlayHand {
    private FreePlayHand() {
    }

    /** Camera-space base point of each stance (x for the right hand; mirrored for the left). */
    static float[] base(Instrument ins) {
        if (ins == Instrument.HARP) {
            return new float[]{0.4F, -0.3F, -0.85F};
        }
        return switch (ins.family()) {
            case STRINGS -> new float[]{0.44F, -0.46F, -0.8F};
            case FLUTE -> new float[]{0.4F, -0.22F, -0.55F};
            case DRUM -> new float[]{0.12F, -0.33F, -0.68F};
            case CHIMES -> new float[]{0.36F, 0.08F, -0.85F};
        };
    }

    /** IClientItemExtensions#applyForgeHandTransform: true (and the pose set) while the instrument is raised. */
    static boolean transform(PoseStack pose, HumanoidArm arm, ItemStack stack, float partial, float equip) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (p == null || !p.isUsingItem() || !(stack.getItem() instanceof SiftInstrumentItem item) || p.getUseItem().getItem() != stack.getItem()) {
            return false;
        }
        Instrument ins = item.instrument();
        float k = arm == HumanoidArm.RIGHT ? 1.0F : -1.0F;
        float[] b = base(ins);
        // raised from below over a few ticks, with a soft landing
        float r = Mth.clamp((p.getTicksUsingItem() + partial) / 6.0F, 0.0F, 1.0F);
        float raise = 1.0F - (1.0F - r) * (1.0F - r) * (1.0F - r);
        pose.translate(k * b[0], b[1] - (1.0F - raise) * 0.5F - equip * 0.6F, b[2]);
        PlayAnim.State s = PlayAnim.get(p.getId());
        float time = (float) (PlayAnim.now() % 1000.0);
        if (ins == Instrument.HARP) {
            float h = s == null ? 0.0F : PlayAnim.hit(s.noteAt);
            pose.rotateDegrees(Axis.YP, k * 2.0F * h);
        } else {
            switch (ins.family()) {
                case STRINGS -> {
                    float h = s == null ? 0.0F : PlayAnim.hit(s.noteAt);
                    pose.translate(0.0F, -0.014F * h, 0.0F);
                    pose.rotateDegrees(Axis.ZP, k * 2.5F * h);
                }
                case DRUM -> {
                    float hr = s == null ? 0.0F : PlayAnim.hit(s.rightAt);
                    float hl = s == null ? 0.0F : PlayAnim.hit(s.leftAt);
                    pose.translate(0.0F, -0.022F * (hr + hl), 0.0F);
                    pose.rotateDegrees(Axis.XP, 3.5F * (hr + hl));
                    pose.rotateDegrees(Axis.ZP, k * 2.5F * (hl - hr));
                }
                case FLUTE -> {
                    float h = s == null ? 0.0F : PlayAnim.hit(s.noteAt);
                    pose.translate(0.0F, 0.004F * Mth.sin(time * 2.2F), 0.008F * h);
                }
                case CHIMES -> pose.rotateDegrees(Axis.ZP, k * 3.0F * PlayAnim.swing(s));
            }
        }
        // the hands are never quite still
        pose.rotateDegrees(Axis.ZP, k * 0.5F * Mth.sin(time * 1.1F));
        return true;
    }
}
