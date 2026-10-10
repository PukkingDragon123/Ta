package com.thesift.client.model;

import com.thesift.client.renderer.state.KerkorerRenderState;
import com.thesift.entity.dunes.Kerkorer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Kerkorer. A chameleon's walk: diagonal pairs of legs stepping slowly while the body rocks back and forth, the
 * tail half uncoiled, the casque steady. Hiding, it sinks low with its elbows out, the jaw a crack open over the
 * bait on its tongue, only the turret eyes rolling - each on its own. Calling its name, the throat comb pumps and
 * the jaw opens on each syllable. The snap: a slow draw back of the head, then a strike - the jaws gape and slam,
 * or the tongue shoots out (the stalk stretches, the sticky pad flies with the bait on it) and snaps back.
 */
public class KerkorerModel extends EntityModel<KerkorerRenderState> {
    private final ModelPart body;
    private final ModelPart crest;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart comb;
    private final ModelPart leftEye;
    private final ModelPart rightEye;
    private final ModelPart tongue;
    private final ModelPart stalk;
    private final ModelPart tip;
    private final ModelPart[] legs = new ModelPart[4];
    private final ModelPart[] shins = new ModelPart[4];
    private final ModelPart[] feet = new ModelPart[4];
    private final ModelPart[] tail = new ModelPart[5];

    public KerkorerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.crest = this.body.getChild("crest");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.comb = this.jaw.getChild("comb");
        this.leftEye = this.head.getChild("left_eye");
        this.rightEye = this.head.getChild("right_eye");
        this.tongue = this.head.getChild("tongue");
        this.stalk = this.tongue.getChild("tongue_stalk");
        this.tip = this.tongue.getChild("tongue_tip");
        String[] names = {"front_left", "front_right", "back_left", "back_right"};
        for (int i = 0; i < 4; i++) {
            this.legs[i] = this.body.getChild(names[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(names[i] + "_shin");
            this.feet[i] = this.shins[i].getChild(names[i] + "_foot");
        }
        this.tail[0] = this.body.getChild("tail");
        for (int i = 1; i < 5; i++) {
            this.tail[i] = this.tail[i - 1].getChild("tail_" + (i + 1));
        }
    }

    /** The chain from the root to the tongue's tip, for drawing the bait on it. */
    public ModelPart[] tipChain() {
        return new ModelPart[]{this.body, this.head, this.tongue, this.tip};
    }

    @Override
    public void setupAnim(KerkorerRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.2F);
        float pos = s.walkAnimationPos * 1.1F;
        float hide = s.state == Kerkorer.HIDE ? Anim.smooth(s.camo * 1.6F) : 0.0F;
        float hunt = s.state == Kerkorer.CHASE || s.state == Kerkorer.PROWL ? 1.0F : 0.0F;

        // ---- breathing, and the low crouch of a hiding Kerkorer
        float breath = Mth.sin(age * 0.07F);
        this.body.yScale = 1.0F + breath * 0.012F;
        this.body.xScale = 1.0F - breath * 0.008F;
        this.body.y += 1.8F * hide;
        this.head.xRot += 0.1F * hide;
        this.head.y += 0.4F * hide;

        // ---- the walk: diagonal pairs (front left with back right), the body rocking fore and aft
        for (int i = 0; i < 4; i++) {
            float sx = (i % 2 == 0) ? 1.0F : -1.0F;
            boolean diag = i == 0 || i == 3;
            float ph = Mth.sin(pos + (diag ? 0.0F : Mth.PI));
            float lift = Math.max(0.0F, Mth.cos(pos + (diag ? 0.0F : Mth.PI)));
            this.legs[i].xRot += ph * 0.55F * walk;
            this.legs[i].zRot -= sx * lift * 0.25F * walk;
            this.shins[i].xRot -= lift * 0.45F * walk;
            this.feet[i].xRot -= this.legs[i].xRot * 0.7F - lift * 0.3F * walk;
            // hiding: elbows out, belly to the sand
            this.legs[i].zRot -= sx * 0.38F * hide;
            this.shins[i].zRot += sx * 0.38F * hide;
        }
        this.body.z += Mth.sin(pos * 2.0F) * 0.5F * walk;
        this.body.zRot += Mth.sin(pos) * 0.05F * walk;
        this.body.xRot += Mth.sin(pos * 2.0F + 0.5F) * 0.03F * walk;

        // ---- head: follows its look, steadied against the rocking
        this.head.yRot += s.yRot * Anim.DEG * 0.45F;
        this.head.xRot += s.xRot * Anim.DEG * 0.35F;
        this.head.zRot -= this.body.zRot * 0.8F;
        // the crest bristles while it hunts
        this.crest.yScale = 1.0F + 0.25F * hunt;

        // ---- turret eyes, each rolling on its own
        this.leftEye.yRot += s.eyeLYaw;
        this.leftEye.xRot += s.eyeLPitch;
        this.rightEye.yRot += s.eyeRYaw;
        this.rightEye.xRot += s.eyeRPitch;

        // ---- the tail: coiled at rest, half uncoiled walking or hunting, curling and uncurling a little
        for (int i = 0; i < 5; i++) {
            float uncoil = (i == 0 ? 0.1F : 0.28F) * Math.max(walk, hunt * 0.8F);
            this.tail[i].xRot += uncoil + Mth.sin(age * 0.035F - i * 0.7F) * 0.045F * (i + 1) * 0.5F;
            this.tail[i].yRot += Mth.sin(pos * 0.5F - i * 0.6F) * 0.08F * walk;
        }

        // ---- the bait: the jaw rests a crack open over it while it hides
        this.jaw.xRot += 0.14F * hide;

        // ---- calling its name: "ker" - "ko" - "rer", the throat comb pumping
        float c = Anim.seconds(s.call, s.ageInTicks);
        if (c >= 0.0F && c < 1.5F) {
            float syl = Anim.envelope(c, 0.0F, 0.06F, 0.1F, 0.12F) + Anim.envelope(c, 0.3F, 0.06F, 0.1F, 0.12F)
                    + Anim.envelope(c, 0.6F, 0.08F, 0.4F, 0.3F);
            this.jaw.xRot += 0.32F * syl;
            this.comb.xRot -= 0.35F * syl;
            this.comb.zScale = 1.0F + 0.25F * syl;
            this.head.xRot -= 0.18F * Anim.envelope(c, 0.0F, 0.2F, 0.9F, 0.3F);
            this.body.xScale += 0.05F * syl;
            if (c > 0.6F && c < 1.2F) {
                this.comb.zRot += Mth.sin(age * 3.0F) * 0.06F; // the rolling "rrr"
            }
        }

        // ---- the snap
        float t = Anim.seconds(s.snap, s.ageInTicks);
        if (t >= 0.0F && t < 1.0F) {
            float draw = Anim.envelope(t, 0.0F, 0.3F, 0.02F, 0.08F);
            float strike = Anim.envelope(t, 0.32F, 0.05F, 0.12F, 0.35F);
            this.head.xRot -= 0.3F * draw;
            this.head.z += 1.5F * draw - 3.5F * strike;
            this.body.z -= 1.2F * strike;
            this.body.y -= 0.8F * strike;
            this.crest.yScale += 0.3F * draw;
            if (s.tongue) {
                // the tongue: out like a shot, the pad (with the bait) at the end of a stretched stalk, then reeled in
                float ext = Anim.envelope(t, 0.32F, 0.06F, 0.14F, 0.25F);
                this.jaw.xRot += 0.45F * Anim.envelope(t, 0.28F, 0.04F, 0.3F, 0.15F);
                this.stalk.zScale = 1.0F + ext * 15.0F;
                this.tip.z -= ext * 2.5F * 15.0F;
                this.tongue.xRot += 0.08F * ext;
            } else {
                // the jaws: a wide gape that slams shut
                float gape = Anim.envelope(t, 0.3F, 0.05F, 0.03F, 0.07F);
                this.jaw.xRot += 0.95F * gape;
            }
        }
    }
}
