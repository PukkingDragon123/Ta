package com.thesift.client.model;

import com.thesift.client.renderer.state.SlumblerTadpoleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR2: a Slumbler tadpole, a little stingray (geometry in tools/slumbler.py).
 *
 * <ul>
 *   <li>gliding: a wave ripples out along each wing-fin - the outer half a beat behind the inner -
 *   in a slow, rolling flight; the harder it swims the deeper and quicker the strokes, and the
 *   whip of a tail streams behind, swinging from side to side</li>
 *   <li>the snout's lobes curl and uncurl as it noses about; the little pelvic fins paddle</li>
 *   <li>bite: it rears its snout up, lunges and snaps down, the wings flaring</li>
 *   <li>crash (its note in a band): both wing-fins clap up together, then quiver</li>
 *   <li>out of the water it flaps helplessly; hurt, it curls; dying, it flips over</li>
 * </ul>
 */
public class SlumblerTadpoleModel extends EntityModel<SlumblerTadpoleRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart snout;
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[] wingTips = new ModelPart[2];
    private final ModelPart[] pelvics = new ModelPart[2];
    private final ModelPart[] lobes = new ModelPart[2];
    private final ModelPart[] tail = new ModelPart[3];

    public SlumblerTadpoleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.snout = this.body.getChild("snout");
        for (int s = 0; s < 2; s++) {
            this.wings[s] = this.body.getChild(SIDES[s] + "_wing");
            this.wingTips[s] = this.wings[s].getChild(SIDES[s] + "_wing_tip");
            this.pelvics[s] = this.body.getChild(SIDES[s] + "_pelvic");
            this.lobes[s] = this.snout.getChild(SIDES[s] + "_lobe");
        }
        this.tail[0] = this.body.getChild("tail1");
        this.tail[1] = this.tail[0].getChild("tail2");
        this.tail[2] = this.tail[1].getChild("tail3");
    }

    @Override
    public void setupAnim(SlumblerTadpoleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float effort = s.inLiquid ? s.effort : 1.0F;
        float speed = 0.16F + 0.22F * effort;
        float depth = 0.22F + 0.3F * effort;
        float t = age * speed;

        // --- the wing-fins ripple: inner half then outer half, a slow rolling flight
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.wings[k].zRot = -Mth.sin(t) * depth * sx;
            this.wingTips[k].zRot = -Mth.sin(t - 1.0F) * depth * 1.2F * sx;
            this.wings[k].xRot = Mth.sin(t - 0.5F) * 0.05F;
            this.pelvics[k].zRot = -Mth.sin(t * 1.5F + k) * 0.25F * sx;
            this.lobes[k].yRot += Mth.sin(age * 0.11F + k * 1.7F) * 0.2F * sx;
            this.lobes[k].xRot = Mth.sin(age * 0.09F + k) * 0.15F;
        }
        // the body rises and falls with the strokes; the tail streams and swings
        this.body.y += Mth.sin(t + 0.6F) * 0.35F * depth;
        this.body.xRot = s.xRot * Anim.DEG * 0.6F + Mth.cos(t) * 0.03F;
        for (int i = 0; i < 3; i++) {
            this.tail[i].yRot = Mth.sin(age * 0.12F - i * 0.9F) * (0.12F + 0.08F * i) * (0.5F + effort);
            this.tail[i].xRot = Mth.sin(t - (i + 1) * 0.8F) * 0.08F;
        }
        this.snout.xRot = Mth.sin(age * 0.07F) * 0.05F;

        // --- out of the water: it flaps helplessly
        if (!s.inLiquid) {
            float flap = Mth.sin(age * 0.9F);
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.wings[k].zRot = -flap * 0.6F * sx;
                this.wingTips[k].zRot = -Mth.sin(age * 0.9F - 0.8F) * 0.5F * sx;
            }
            this.body.zRot = Mth.sin(age * 0.45F) * 0.25F;
        }

        // --- bite: snout up, a lunge, a snap down, the wings flaring
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.5F) {
            float rear = Anim.envelope(bite, 0.0F, 0.1F, 0.03F, 0.06F);
            float snap = Anim.envelope(bite, 0.1F, 0.06F, 0.05F, 0.25F);
            this.snout.xRot += -rear * 0.5F + snap * 0.35F;
            this.body.xRot += -rear * 0.15F + snap * 0.1F;
            this.body.z -= snap * 1.0F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.wings[k].zRot -= rear * 0.4F * sx;
                this.lobes[k].yRot -= rear * 0.5F * sx;
            }
        }

        // --- crash: both wing-fins clap up together, then quiver
        float crash = Anim.seconds(s.crash, s.ageInTicks);
        if (crash >= 0.0F && crash < 0.7F) {
            float clap = Anim.envelope(crash, 0.0F, 0.06F, 0.04F, 0.2F);
            float quiver = Mth.sin(crash * 60.0F) * Anim.envelope(crash, 0.1F, 0.05F, 0.2F, 0.3F);
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.wings[k].zRot -= (clap * 0.9F + quiver * 0.12F) * sx;
                this.wingTips[k].zRot -= (clap * 0.7F + quiver * 0.2F) * sx;
            }
            this.tail[0].xRot += clap * 0.3F;
        }

        // --- hurt: it curls; dying: it flips over
        if (s.hasRedOverlay) {
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.wings[k].zRot -= 0.5F * sx;
            }
            this.tail[0].xRot += 0.3F;
        }
        float flip = Anim.smooth(s.dying / 12.0F);
        if (flip > 0.0F) {
            this.body.zRot = Mth.PI * flip;
            this.body.y -= 1.5F * flip;
        }
    }
}
