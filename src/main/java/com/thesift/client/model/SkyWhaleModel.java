package com.thesift.client.model;

import com.thesift.client.renderer.state.SkyWhaleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR2: the Sky Whale, a humpback of the sky (geometry in tools/sky_whale.py).
 *
 * <ul>
 *   <li>swimming: the spine drives a slow vertical wave from the chest down the tail stock to the
 *   flukes - each tail segment a beat behind the one before and swinging further - and the flukes
 *   give a powerful down-stroke and a lazy recovery, flexing at their tips; the head rides the wave
 *   a little against it</li>
 *   <li>the long white flippers sweep in slow strokes, the tips trailing a beat behind; climbing it
 *   lifts them, diving it tucks them back</li>
 *   <li>turning: it banks into the turn, the inner flipper dipping, and the tail swings out like a
 *   rudder with the dorsal fin leaning</li>
 *   <li>now and then the blowhole flares open and closes again</li>
 *   <li>singing: the jaw drops open on its baleen and the throat pleats balloon out, swelling with
 *   each phrase; the head lifts and the flukes keep time</li>
 *   <li>spitting a gem: a gulp that swells the throat, then a big open-mouthed heave</li>
 *   <li>hurt: a flinch, flippers thrown up; dying: it rolls slowly belly-up</li>
 * </ul>
 */
public class SkyWhaleModel extends EntityModel<SkyWhaleRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart blowhole;
    private final ModelPart dorsal;
    private final ModelPart[] fins = new ModelPart[2];
    private final ModelPart[] finTips = new ModelPart[2];
    private final ModelPart[] tail = new ModelPart[4];
    private final ModelPart[] flukes = new ModelPart[2];

    public SkyWhaleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.blowhole = this.head.getChild("blowhole");
        this.throat = this.body.getChild("throat");
        this.tail[0] = this.body.getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail3");
        this.tail[3] = this.tail[2].getChild("tail4");
        this.dorsal = this.tail[0].getChild("dorsal_fin");
        for (int s = 0; s < 2; s++) {
            this.fins[s] = this.body.getChild(SIDES[s] + "_fin");
            this.finTips[s] = this.fins[s].getChild(SIDES[s] + "_fin_tip");
            this.flukes[s] = this.tail[3].getChild(SIDES[s] + "_fluke");
        }
    }

    @Override
    public void setupAnim(SkyWhaleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float swim = age * 0.055F;
        float bank = s.bank * Anim.DEG * 2.2F;

        // --- the spine's travelling wave: the chest rises and falls, the tail follows a beat later and swings further
        this.body.xRot = Mth.sin(swim) * 0.03F - s.climb * 0.5F;
        this.body.zRot = -bank;
        this.body.y += Mth.sin(swim) * 1.4F;
        for (int i = 0; i < 4; i++) {
            float amp = 0.05F + 0.045F * i;
            this.tail[i].xRot = Mth.sin(swim - 0.8F * (i + 1)) * amp + s.climb * 0.2F;
            // the tail swings out of the turn like a rudder
            this.tail[i].yRot = bank * 0.18F * (i + 1) + Mth.sin(age * 0.02F - i * 0.5F) * 0.025F;
        }
        // the fluke stroke: a quick, powerful down-beat and a slow recovery, the blades flexing behind it
        float beat = swim - 4.0F;
        float power = Mth.sin(beat) + 0.3F * Mth.sin(beat * 2.0F);
        this.tail[3].xRot += power * 0.12F;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.flukes[k].xRot = Mth.sin(beat - 0.9F) * 0.16F;
            this.flukes[k].zRot = -bank * 0.5F * sx + Mth.sin(beat - 0.6F) * 0.05F * sx;
        }
        this.dorsal.zRot = -bank * 0.6F + Mth.sin(swim - 1.4F) * 0.03F;
        this.head.xRot = s.xRot * Anim.DEG * 0.3F - Mth.sin(swim + 0.4F) * 0.025F;
        this.head.yRot = s.yRot * Anim.DEG * 0.25F;

        // --- the long flippers: slow sweeping strokes, the tips trailing; lifted in a climb, tucked in a dive; the inner one dips in a turn
        float stroke = age * 0.045F;
        float sweep = Mth.sin(stroke) + 0.2F * Mth.sin(stroke * 2.0F);
        float lift = Mth.clamp(s.climb * 2.5F, -0.6F, 0.6F);
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.fins[k].zRot += (sweep * 0.22F - lift * 0.3F) * sx - bank * 0.35F;
            this.fins[k].xRot += Mth.cos(stroke) * 0.08F + Math.min(0.0F, lift) * -0.4F;
            this.fins[k].yRot += Mth.sin(stroke - 0.8F) * 0.06F * sx;
            this.finTips[k].zRot += Mth.sin(stroke - 0.9F) * 0.18F * sx;
            this.finTips[k].yRot += Mth.sin(stroke - 1.3F) * 0.05F * sx;
        }

        // --- the blowhole flares now and then
        float blow = (age + s.seed * 11.0F) % 260.0F / 20.0F;
        float flare = Anim.envelope(blow, 0.0F, 0.15F, 0.35F, 0.5F);
        this.blowhole.xScale = 1.0F + 0.25F * flare;
        this.blowhole.zScale = 1.0F + 0.2F * flare;
        this.blowhole.y -= 0.4F * flare;

        // --- singing: the jaw falls open, the throat pleats balloon with each phrase, the head lifts, the flukes keep time
        float sing = Anim.seconds(s.sing, s.ageInTicks);
        if (sing >= 0.0F && sing < 4.0F) {
            float e = Anim.envelope(sing, 0.0F, 0.6F, 2.2F, 1.0F);
            float warble = Mth.sin(sing * 9.0F) * 0.04F * e;
            this.jaw.xRot += 0.35F * e + warble;
            this.head.xRot -= 0.15F * e;
            float swell = e * (0.6F + 0.4F * Mth.sin(sing * 2.6F));
            this.throat.yScale = 1.0F + 1.6F * swell;
            this.throat.xScale = 1.0F + 0.06F * swell;
            this.tail[3].xRot += Mth.sin(sing * 3.0F) * 0.15F * e;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.fins[k].zRot -= 0.25F * e * sx;
            }
        }

        // --- spitting the gem: a gulp (the throat swells), then a big open-mouthed heave
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        if (spit >= 0.0F && spit < 1.6F) {
            float gulp = Anim.envelope(spit, 0.0F, 0.3F, 0.2F, 0.1F);
            float heave = Anim.envelope(spit, 0.55F, 0.08F, 0.3F, 0.5F);
            this.throat.yScale = Math.max(this.throat.yScale, 1.0F + 1.2F * gulp);
            this.body.xScale = 1.0F + 0.04F * gulp - 0.02F * heave;
            this.jaw.xRot = Math.max(this.jaw.xRot, 0.6F * heave);
            this.head.xRot -= 0.12F * heave;
            // a recoil of the whole whale, rather than pushing the head off its neck
            this.body.z += 1.5F * heave;
        }

        // --- hurt: a flinch, the flippers thrown up
        if (s.hasRedOverlay) {
            this.head.xRot += 0.12F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.fins[k].zRot -= 0.3F * sx;
            }
            this.tail[0].xRot -= 0.1F;
        }
        // --- dying: it rolls slowly belly-up, the jaw slack
        float die = Anim.smooth(s.dying / 16.0F);
        if (die > 0.0F) {
            this.body.zRot = Mth.PI * die;
            this.jaw.xRot = Math.max(this.jaw.xRot, die * 0.35F);
        }
    }
}
