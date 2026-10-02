package com.thesift.client.model;

import com.thesift.client.renderer.state.SifterRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Sifter: a trap with legs. A heavy sandstone tray of a lower jaw under a layered carapace lid that
 * is hinged at the back, a glowing lure dangling from the brow on a jointed stalk, and four bony crab
 * legs.
 *
 * <ul>
 *   <li>idle: the lid rests a crack open, the lure bobs and sways on its stalk (springs), legs shift</li>
 *   <li>move: a skittering crab gait (diagonal pairs), the lid clacks with the steps</li>
 *   <li>chase: low and fast, the lid chattering, the lure streaming back</li>
 *   <li>snap: the lid creaks open wide (anticipation), slams shut like a trap and the whole head shakes</li>
 *   <li>burrow: it shimmies down into the sand, legs digging, until only the lid and the lure show;
 *   emerge: bursts up with an overshoot, the lid flung open</li>
 *   <li>hurt: lid pops, head jerks back; death: the cartoon pop of SiftMobRenderer</li>
 * </ul>
 */
public class SifterModel extends EntityModel<SifterRenderState> {
    /** How far it sinks into the sand: the lid's rim ends up level with the ground. */
    private static final float SUNK = 14.5F;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart lid;
    private final ModelPart stalk;
    private final ModelPart stalkTip;
    private final ModelPart lure;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];

    public SifterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.lid = this.head.getChild("lid");
        this.stalk = this.lid.getChild("lure_stalk");
        this.stalkTip = this.stalk.getChild("lure_tip");
        this.lure = this.stalkTip.getChild("lure");
        String[] names = {"front_left", "front_right", "back_left", "back_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(names[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(names[i] + "_shin");
        }
    }

    @Override
    public void setupAnim(SifterRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.4F);
        float chase = s.chasing ? walk : 0.0F;
        float pos = s.walkAnimationPos * (1.3F + 0.5F * chase);

        float sq = Mth.clamp(s.squash, -0.4F, 0.5F);
        this.body.yScale = 1.0F + sq * 0.5F;
        this.body.xScale = 1.0F - sq * 0.25F;
        this.body.zScale = 1.0F - sq * 0.25F;

        // --- skittering crab gait: diagonal pairs lift and swing together
        for (int i = 0; i < 4; i++) {
            float sgn = i % 2 == 0 ? 1.0F : -1.0F;
            float phase = (i == 0 || i == 3) ? 0.0F : (float) Math.PI;
            float lift = Math.max(0.0F, Mth.sin(pos + phase));
            this.legs[i].yRot = Mth.cos(pos + phase) * 0.45F * walk * sgn;
            this.legs[i].zRot += -sgn * lift * 0.35F * walk;
            this.shins[i].zRot += sgn * lift * 0.25F * walk;
            // idle: now and then a leg shifts its footing
            float shift = Anim.envelope(Mth.positiveModulo(age + i * 23.0F, 90.0F), 0.0F, 3.0F, 1.0F, 4.0F) * (1.0F - walk);
            this.legs[i].zRot -= sgn * shift * 0.2F;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.6F * walk;
        // chasing it runs low and leans in
        this.body.y += 1.5F * chase;
        this.body.xRot = 0.12F * chase;
        this.body.zRot = Mth.sin(pos) * 0.05F * walk;
        this.head.yRot = s.yRot * Anim.DEG * 0.5F;
        this.head.xRot = s.xRot * Anim.DEG * 0.3F + Mth.sin(pos * 2.0F) * 0.04F * walk;

        // --- lid: a crack open at rest, clacking with the steps, chattering in a chase
        float open = 0.06F + Math.abs(Mth.sin(pos)) * 0.12F * walk * (1.0F - chase);
        open += chase * (0.12F + 0.1F * Math.abs(Mth.sin(age * 1.1F)));

        // --- the lure: bobs on its stalk, swings with the springs, streams back when running
        float swayL = s.antennaLeft;
        float swayR = s.antennaRight;
        this.stalk.xRot += Mth.sin(age * 0.07F) * 0.08F + swayL * 0.5F - walk * 0.35F;
        this.stalk.zRot = Mth.sin(age * 0.05F) * 0.12F + (swayL - swayR) * 0.4F;
        this.stalkTip.xRot += Mth.sin(age * 0.07F - 0.9F) * 0.15F + swayR * 0.6F - walk * 0.4F;
        float glowPulse = 1.0F + Mth.sin(age * 0.2F) * 0.06F;
        this.lure.xScale = this.lure.yScale = this.lure.zScale = glowPulse;

        // --- snap: the lid creaks open (anticipation), slams shut like a trap, the head shakes
        float chomp = Anim.seconds(s.chomp, age);
        if (chomp >= 0.0F && chomp < 0.75F) {
            float wind = chomp < 0.22F ? Anim.smooth(chomp / 0.22F) : 0.0F;
            float shut = chomp >= 0.22F ? Anim.envelope(chomp, 0.22F, 0.0F, 0.0F, 0.5F) : 0.0F;
            open = open * (1.0F - wind) + 1.2F * wind;
            this.body.xRot -= 0.15F * wind;
            this.body.z += 1.0F * wind;
            // the slam: closed hard, the body lunges forward and shudders
            float shudder = Mth.sin(chomp * 70.0F) * shut;
            if (chomp >= 0.22F) {
                open = Math.max(0.0F, open - 0.06F * (1.0F - shut));
                this.body.z -= 2.0F * shut;
                this.body.xRot += 0.2F * shut;
                this.head.zRot = shudder * 0.06F;
                this.head.yScale = 1.0F - 0.08F * shut;
            }
            this.stalk.xRot += 0.6F * shut;
        }
        // --- hurt: lid pops, head jerks back
        if (s.hasRedOverlay) {
            open += 0.5F;
            this.head.xRot -= 0.25F;
        }

        // --- burrowing: it shimmies into the sand, the legs dig, until only the lid and lure show
        float burrowT = Anim.seconds(s.burrow, age);
        float emerge = Anim.seconds(s.emerge, age);
        if (s.burrowed) {
            float sink = burrowT >= 0.0F && burrowT < 1.0F ? Anim.smooth(burrowT) : 1.0F;
            float dig = burrowT >= 0.0F && burrowT < 1.0F ? 1.0F - burrowT : 0.0F;
            this.body.y += SUNK * sink;
            this.body.zRot += Mth.sin(age * 1.4F) * 0.1F * dig;
            for (int i = 0; i < 4; i++) {
                float sgn = i % 2 == 0 ? 1.0F : -1.0F;
                this.legs[i].yRot += Mth.sin(age * 1.8F + i * 1.5F) * 0.6F * dig * sgn;
                this.legs[i].zRot -= sgn * 0.5F * sink;
            }
            // lurking: the lid opens a slow crack and the lure dances to tempt you closer
            open = Math.max(0.0F, Mth.sin(age * 0.03F)) * 0.15F * sink + open * (1.0F - sink);
            this.stalk.xRot -= 0.35F * sink;
            this.stalkTip.xRot += Mth.sin(age * 0.18F) * 0.25F * sink;
            this.stalk.zRot += Mth.sin(age * 0.11F) * 0.2F * sink;
        } else if (emerge >= 0.0F && emerge < 0.7F) {
            float t = Anim.backOut(emerge / 0.45F);
            this.body.y += SUNK * (1.0F - t);
            open += Anim.envelope(emerge, 0.0F, 0.08F, 0.12F, 0.4F) * 0.9F;
            this.stalkTip.xRot -= (1.0F - Anim.smooth(emerge / 0.6F)) * 0.6F;
        }
        this.lid.xRot = -open;
    }
}
