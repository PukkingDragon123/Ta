package com.thesift.client.model;

import com.thesift.client.renderer.state.SifterRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR1 Sifter: a living bronze bell on four stubby legs (geometry in tools/sifter.py). The bell's rock
 * and its clapper's swing come from the entity's little simulation ({@code entity/BellClapper}), so
 * when the clapper is seen to meet the lip, that is exactly when it tinks.
 *
 * <ul>
 *   <li>walk: a waddle - the legs pace left pair, right pair, the bell rocks from foot to foot and
 *   leans into its stride; charging it tips well forward and the clapper jangles</li>
 *   <li>idle: still, but for fidgets that rock the bell (and tink), blinking eyes that glance about</li>
 *   <li>ring: after every strike the bell shivers and the loop on its crown buzzes as the runes flare</li>
 *   <li>bonk: it rears back on its hind legs (anticipation), throws its whole bell forward into you
 *   and rocks back to rest</li>
 *   <li>burrow: it shuffles down into the sand until only its shoulder and eyes show; emerge: it
 *   bursts up with an overshoot</li>
 * </ul>
 */
public class SifterModel extends EntityModel<SifterRenderState> {
    /** How far it settles into the sand: the waist's lower edge ends up level with the ground. */
    private static final float SUNK = 11.0F;
    private final ModelPart body;
    private final ModelPart bell;
    private final ModelPart canon;
    private final ModelPart clapper;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];

    public SifterModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.bell = this.body.getChild("bell");
        this.canon = this.bell.getChild("canon");
        this.clapper = this.bell.getChild("clapper");
        this.leftEye = this.bell.getChild("left_eye");
        this.rightEye = this.bell.getChild("right_eye");
        String[] names = {"front_left", "front_right", "back_left", "back_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(names[i] + "_leg");
            this.feet[i] = this.legs[i].getChild(names[i] + "_foot");
        }
    }

    @Override
    public void setupAnim(SifterRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = s.burrowed ? 0.0F : Math.min(1.0F, s.walkAnimationSpeed * 1.5F);
        float pos = s.walkAnimationPos * 0.6662F;

        // --- squash and stretch about the lip
        float sq = Mth.clamp(s.squash, -0.4F, 0.5F);
        this.body.yScale = 1.0F + sq * 0.45F;
        this.body.xScale = 1.0F - sq * 0.22F;
        this.body.zScale = 1.0F - sq * 0.22F;

        // --- the bell rocks on its lip and the clapper hangs plumb (so in the bell's frame it swings by the difference)
        this.bell.xRot = s.rockX;
        this.bell.zRot = s.rockZ;
        this.clapper.xRot = s.swingX - s.rockX;
        this.clapper.zRot = s.swingZ - s.rockZ;
        // the ring: the bell shivers and the loop on its crown buzzes
        float ring = s.ring;
        if (ring > 0.0F) {
            float shiver = Mth.sin(age * 4.7F) * ring;
            this.bell.xScale = 1.0F + 0.025F * shiver;
            this.bell.zScale = 1.0F - 0.025F * shiver;
            this.canon.zRot = Mth.sin(age * 3.1F) * 0.12F * ring;
        }

        // --- the waddle: left legs, then right legs; a lifted foot swings forward, the planted one pushes back
        for (int i = 0; i < 4; i++) {
            boolean left = i % 2 == 0;
            float phase = left ? Mth.PI : 0.0F;
            float lift = Math.max(0.0F, Mth.sin(pos + phase));
            this.legs[i].xRot += Mth.cos(pos + phase) * 0.5F * walk;
            this.legs[i].zRot += (left ? -1.0F : 1.0F) * lift * 0.22F * walk;
            this.legs[i].y -= lift * 1.2F * walk;
            this.feet[i].xRot = -this.legs[i].xRot * 0.7F + lift * 0.3F * walk;
        }
        this.body.y -= Math.abs(Mth.sin(pos)) * 0.5F * walk;

        // --- eyes: they glance where it looks, and roam slowly while it lies in the sand
        float look = Mth.clamp(s.yRot * Anim.DEG * 0.35F, -0.3F, 0.3F) + (s.burrowed ? Mth.sin(age * 0.05F) * 0.25F : 0.0F);
        float lookUp = Mth.clamp(s.xRot * Anim.DEG * 0.3F, -0.25F, 0.25F);
        this.leftEye.yRot += look;
        this.rightEye.yRot += look;
        this.leftEye.xRot += lookUp;
        this.rightEye.xRot += lookUp;

        // --- the bonk: rear back on the hind legs, then throw the whole bell forward and rock back
        float bonk = Anim.seconds(s.bonk, age);
        if (bonk >= 0.0F && bonk < 0.75F) {
            float rear = Anim.envelope(bonk, 0.0F, 0.2F, 0.02F, 0.08F);
            float slam = Anim.envelope(bonk, 0.22F, 0.06F, 0.06F, 0.4F);
            this.bell.xRot += -0.35F * rear + 0.45F * slam;
            this.body.z += 1.5F * rear - 3.0F * slam;
            this.body.y -= 1.2F * rear;
            this.legs[0].xRot -= 0.5F * rear;
            this.legs[1].xRot -= 0.5F * rear;
            this.legs[2].xRot += 0.3F * slam;
            this.legs[3].xRot += 0.3F * slam;
        }

        // --- burrowing: it shuffles down into the sand until only its shoulder and eyes show
        float burrowT = Anim.seconds(s.burrow, age);
        float emerge = Anim.seconds(s.emerge, age);
        if (s.burrowed) {
            float sink = burrowT >= 0.0F && burrowT < 1.2F ? Anim.smooth(burrowT / 1.2F) : 1.0F;
            float dig = burrowT >= 0.0F && burrowT < 1.2F ? 1.0F - burrowT / 1.2F : 0.0F;
            this.body.y += SUNK * sink;
            this.bell.zRot += Mth.sin(age * 1.6F) * 0.12F * dig;
            this.bell.xRot *= 1.0F - sink;
            this.bell.zRot *= 1.0F - 0.8F * sink;
            for (int i = 0; i < 4; i++) {
                this.legs[i].xRot += Mth.sin(age * 1.9F + i * 1.5F) * 0.7F * dig;
                this.legs[i].zRot += (i % 2 == 0 ? -0.5F : 0.5F) * sink;
            }
        } else if (emerge >= 0.0F && emerge < 0.7F) {
            float t = Anim.backOut(Math.min(1.0F, emerge / 0.45F));
            this.body.y += SUNK * (1.0F - t);
            for (int i = 0; i < 4; i++) {
                this.legs[i].zRot += (i % 2 == 0 ? -0.6F : 0.6F) * (1.0F - Anim.smooth(emerge / 0.6F));
            }
        }

        // --- hurt: it rings, so it shudders
        if (s.hasRedOverlay) {
            this.bell.zRot += Mth.sin(age * 5.0F) * 0.05F;
        }
    }
}
