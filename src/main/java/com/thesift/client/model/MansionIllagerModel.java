package com.thesift.client.model;

import com.thesift.client.renderer.state.MansionIllagerRenderState;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.illager.IllagerModel;
import net.minecraft.util.Mth;

/**
 * MANSION: the Hornblower's and the Bard's model - the vanilla illager model and its animations exactly (it is baked from
 * the vanilla pillager layer), with their hood/cap layer shown and two poses added for their instruments:
 *
 * <ul>
 *   <li>Hornblower, winding up: the horn raised to his lips like a player tooting a goat horn (the right arm follows his
 *   head), the left hand under the bell, his chin lifting as he draws breath; on the blast the arms and head jolt with
 *   the recoil and settle.</li>
 *   <li>Bard, playing: the guitar raised across his body like a player playing one (the holding arm and the fretting hand's
 *   reach are the free-play stance's, client/music/InstrumentPoses), the right hand strumming, his head bobbing; the last,
 *   healing chord is swept with a flourish.</li>
 * </ul>
 */
public class MansionIllagerModel extends IllagerModel<MansionIllagerRenderState> {
    /** The guitar stance's holding arm (x, y rotation) and the fretting hand's two grip points (low, high), model space. */
    private static final float[] GUITAR_HOLD = {-0.5852F, -0.6435F};
    private static final float[] GUITAR_GRIPS = {6.91F, 3.82F, -11.02F, 4.14F, 5.66F, -9.43F};
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;

    public MansionIllagerModel(ModelPart root) {
        super(root);
        this.head = root.getChild("head");
        this.rightArm = root.getChild("right_arm");
        this.leftArm = root.getChild("left_arm");
    }

    @Override
    public void setupAnim(MansionIllagerRenderState s) {
        super.setupAnim(s);
        this.getHat().visible = true; // the Hornblower's fleece hood, the Bard's cap
        if (s.isRiding) {
            return;
        }
        float age = s.ageInTicks;
        if (s.bard) {
            if (s.playing) {
                this.playGuitar(s, age);
            }
        } else if (s.playing) {
            this.blowHorn(s, age);
        }
        float t = s.sinceFlourish;
        if (t >= 0.0F && t < 12.0F) {
            // the blast's recoil / the last chord's sweep: a jolt that settles
            float k = (1.0F - t / 12.0F);
            float shake = Mth.sin(t * 2.4F) * 0.25F * k * k;
            if (s.bard) {
                this.rightArm.xRot -= 0.6F * k;
                this.head.xRot -= 0.25F * k;
            } else {
                this.head.xRot += shake + 0.2F * k;
                this.rightArm.xRot += shake;
                this.leftArm.xRot += shake * 0.7F;
            }
        }
    }

    /** The vanilla goat-horn pose (HumanoidModel's TOOT_HORN), the left hand under the bell, breath drawn in. */
    private void blowHorn(MansionIllagerRenderState s, float age) {
        float breath = Math.min(1.0F, s.ticksUsingItem / 30.0F);
        this.head.xRot -= 0.22F * breath;
        this.rightArm.xRot = Mth.clamp(this.head.xRot - 1.4835298F, -2.4F, 3.3F);
        this.rightArm.yRot = this.head.yRot - (float) (Math.PI / 6);
        this.rightArm.zRot = 0.0F;
        this.leftArm.xRot = Mth.clamp(this.head.xRot - 1.25F, -2.4F, 3.3F);
        this.leftArm.yRot = this.head.yRot + 0.45F;
        this.leftArm.zRot = 0.0F;
        // the bell trembles as he fills it
        float tremble = Mth.sin(age * 3.1F) * 0.035F * breath;
        this.rightArm.xRot += tremble;
        this.leftArm.xRot += tremble;
    }

    /** The free-play guitar stance: the guitar across the body, the right hand strumming, the left fretting up and down the neck. */
    private void playGuitar(MansionIllagerRenderState s, float age) {
        float strum = Math.max(0.0F, Mth.sin(age * 0.63F)) * 0.11F;
        this.rightArm.xRot = GUITAR_HOLD[0] + strum;
        this.rightArm.yRot = GUITAR_HOLD[1];
        this.rightArm.zRot = 0.0F;
        float place = 0.5F + 0.5F * Mth.sin(age * 0.11F);
        reach(this.leftArm, Mth.lerp(place, GUITAR_GRIPS[0], GUITAR_GRIPS[3]), Mth.lerp(place, GUITAR_GRIPS[1], GUITAR_GRIPS[4]),
                Mth.lerp(place, GUITAR_GRIPS[2], GUITAR_GRIPS[5]));
        this.head.zRot = Mth.sin(age * 0.31F) * 0.08F;
        this.head.xRot += 0.1F + Mth.sin(age * 0.63F) * 0.04F;
    }

    /** Points an arm from its shoulder at a model-space point (as InstrumentPoses does for players). */
    private static void reach(ModelPart arm, float tx, float ty, float tz) {
        float dx = tx - arm.x;
        float dy = ty - arm.y;
        float dz = tz - arm.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        float a = -(float) Math.acos(Mth.clamp(dy, -1.0F, 1.0F));
        float sa = Mth.sin(a);
        arm.xRot = a;
        arm.yRot = Math.abs(sa) > 1.0E-4F ? (float) Math.atan2(dx / sa, dz / sa) : 0.0F;
        arm.zRot = 0.0F;
    }
}
