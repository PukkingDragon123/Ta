package com.thesift.client.model;

import com.thesift.client.renderer.state.CypoleRenderState;
import com.thesift.entity.swamp.Cypole;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Cypole. It squats before every hop, flings its legs back in the air and splats down on
 * landing, the brass plates jangling on. Idle, its throat breathes, its one great eye swivels after
 * whatever it watches and blinks now and then. Croaking, the vocal sac balloons and the plates either
 * side of it swing out and tick back together. Warning you off, it rises on its front legs, eye
 * narrowed, and the plates fizz against each other. The clash: it rears right up, swings the plates
 * out wide (the sac swelling), then slams down as they crash together under its chin and ring on
 * long after. The tongue: a low squat and a stare, the jaw drops and the tongue whips out to its
 * target (stretched between the mouth and the tip) and reels back in. Floating, its legs paddle.
 */
public class CypoleModel extends EntityModel<CypoleRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart eye;
    private final ModelPart iris;
    private final ModelPart upperLid;
    private final ModelPart lowerLid;
    private final ModelPart jaw;
    private final ModelPart throat;
    private final ModelPart tongue;
    private final ModelPart tongueTip;
    private final ModelPart[] plates = new ModelPart[2];
    private final ModelPart[] tympana = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];

    public CypoleModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.eye = this.head.getChild("eye");
        this.iris = this.eye.getChild("iris");
        this.upperLid = this.eye.getChild("upper_lid");
        this.lowerLid = this.eye.getChild("lower_lid");
        this.jaw = this.head.getChild("jaw");
        this.throat = this.jaw.getChild("throat");
        this.tongue = root.getChild("tongue");
        this.tongueTip = root.getChild("tongue_tip");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.plates[i] = this.jaw.getChild(s + "_plate");
            this.tympana[i] = this.head.getChild(s + "_tympanum");
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
            this.arms[i] = root.getChild(s + "_arm");
            this.forearms[i] = this.arms[i].getChild(s + "_forearm");
            this.hands[i] = this.forearms[i].getChild(s + "_hand");
        }
    }

    @Override
    public void setupAnim(CypoleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float air = s.air;
        float crouch = s.crouch;
        float lidUp = 0.12F;
        float lidDown = 0.07F;
        float bodyY = 0.0F;

        // ---- breathing, and the squash and stretch of the hops
        float breathe = Mth.sin(age * 0.11F);
        this.throat.xScale *= 1.0F + 0.05F * breathe;
        this.throat.yScale *= 1.0F + 0.08F * breathe;
        float sq = Mth.clamp(s.squash, -0.45F, 0.45F);
        this.body.yScale *= 1.0F + sq * 0.55F;
        this.body.xScale *= 1.0F - sq * 0.3F;
        this.body.zScale *= 1.0F - sq * 0.2F;

        // ---- the squat before a hop: down on its haunches, head low
        bodyY += 1.4F * crouch;
        this.body.xRot += 0.12F * crouch;
        for (int i = 0; i < 2; i++) {
            this.legs[i].xRot -= 0.22F * crouch;
            this.arms[i].xRot += 0.25F * crouch;
        }

        // ---- in mid-hop: hind legs flung out behind, front legs reaching ahead, nose up
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.legs[i].xRot += 0.9F * air;
            this.legs[i].y -= 1.5F * air;
            this.shins[i].xRot -= 0.5F * air;
            this.feet[i].xRot += 0.9F * air;
            this.arms[i].xRot -= 0.85F * air;
            this.arms[i].zRot -= 0.2F * sx * air;
        }
        this.body.xRot -= 0.22F * air;
        bodyY -= 0.8F * air;

        // ---- floating: a slow frog kick, the hands sculling
        if (s.afloat) {
            for (int i = 0; i < 2; i++) {
                float phase = Mth.sin(age * 0.18F + i * Mth.PI);
                this.legs[i].xRot += 0.55F + 0.35F * phase;
                this.shins[i].xRot -= 0.3F * Math.max(0.0F, phase);
                this.feet[i].xRot += 0.4F * Math.max(0.0F, -phase);
                this.arms[i].xRot -= 0.35F + 0.15F * phase;
            }
            this.body.xRot += 0.05F * Mth.sin(age * 0.09F);
        }

        // ---- the one great eye: it swivels after what it watches, and blinks
        this.eye.yRot += Mth.clamp(s.yRot, -60.0F, 60.0F) * Anim.DEG * 0.65F;
        this.eye.xRot += Mth.clamp(s.xRot, -30.0F, 30.0F) * Anim.DEG * 0.4F;
        this.iris.x += Mth.clamp(-s.yRot / 60.0F, -1.0F, 1.0F) * 0.35F;
        this.iris.y += Mth.clamp(s.xRot / 40.0F, -1.0F, 1.0F) * 0.3F;
        float pupil = 1.0F + 0.06F * Mth.sin(age * 0.07F);
        this.iris.xScale *= pupil;
        this.iris.yScale *= pupil;
        if (s.angry) {
            lidUp = 0.36F;
            lidDown = 0.18F;
        }

        // ---- the brass ringing on after a crash (or a hard landing): plates and tympana shiver
        float ring = Mth.clamp(s.ring, -1.2F, 1.2F);
        float shiver = ring * Mth.sin(age * 2.7F);
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            this.plates[i].zRot -= sx * (0.12F * shiver + 0.06F * Math.abs(ring));
            this.plates[i].xRot += 0.08F * ring * Mth.cos(age * 2.1F);
            this.tympana[i].zRot += sx * 0.18F * shiver;
            this.tympana[i].xScale *= 1.0F + 0.08F * Math.abs(shiver);
        }

        // ---- a croak: the sac balloons, the plates swing out on it and tick back together
        float c = Anim.seconds(s.croak, s.ageInTicks);
        if (c >= 0.0F && c < 0.55F) {
            float puff = Anim.envelope(c, 0.0F, 0.12F, 0.08F, 0.25F);
            this.throat.xScale *= 1.0F + 0.75F * puff;
            this.throat.yScale *= 1.0F + 0.6F * puff;
            this.throat.zScale *= 1.0F + 0.45F * puff;
            this.jaw.xRot -= 0.05F * puff;
            this.head.xRot -= 0.08F * puff;
            for (int i = 0; i < 2; i++) {
                this.plates[i].zRot -= (i == 0 ? 1.0F : -1.0F) * 0.42F * puff;
            }
            lidUp = Math.max(lidUp, 0.3F * puff);
            lidDown = Math.max(lidDown, 0.22F * puff);
        }

        // ---- warning you off: up on its front legs, eye narrowed, the plates fizzing together
        if (s.action == Cypole.WARN) {
            float w = Anim.smooth(Anim.seconds(s.warn, s.ageInTicks) / 0.3F);
            this.body.xRot -= 0.18F * w;
            bodyY -= 0.8F * w;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.arms[i].xRot -= 0.15F * w;
                this.plates[i].zRot -= sx * (0.2F + 0.14F * Mth.sin(age * 2.4F + i * 1.3F)) * w;
            }
            this.throat.yScale *= 1.0F + 0.15F * w * Math.abs(Mth.sin(age * 0.7F));
            this.jaw.xRot += 0.08F * w;
            lidUp = Math.max(lidUp, 0.34F * w);
            lidDown = Math.max(lidDown, 0.2F * w);
        }

        // ---- the clash: rear up and swing the plates wide - crash them together and slam down
        float t = Anim.seconds(s.clash, s.ageInTicks);
        float crashAt = Cypole.CLASH_CRASH / 20.0F;
        if (t >= 0.0F && t < Cypole.CLASH_END / 20.0F + 0.2F) {
            float rise = Anim.smooth(t / crashAt) * (1.0F - Anim.smooth((t - crashAt) / 0.07F));
            float slam = Anim.envelope(t, crashAt, 0.05F, 0.15F, 0.6F);
            this.body.xRot += -0.5F * rise + 0.22F * slam;
            bodyY += -2.2F * rise + 1.0F * slam;
            this.head.xRot += -0.25F * rise + 0.12F * slam;
            this.jaw.xRot += 0.25F * rise;
            this.throat.xScale *= 1.0F + 0.7F * rise;
            this.throat.yScale *= 1.0F + 0.5F * rise;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                // out wide and forward on the wind-up; snapped together under the chin on the crash
                this.plates[i].zRot -= sx * (1.15F * rise - 0.85F * slam);
                this.plates[i].xRot -= 0.35F * rise + 0.75F * slam;
                this.arms[i].xRot += -0.7F * rise + 0.35F * slam;
                this.arms[i].zRot -= sx * 0.25F * rise;
                this.legs[i].xRot -= 0.25F * rise;
            }
            lidUp = rise > 0.05F ? 0.02F : Math.max(lidUp, 0.32F * slam);
            lidDown = rise > 0.05F ? 0.02F : Math.max(lidDown, 0.22F * slam);
        }

        // ---- the tongue: a squat and a stare, the jaw drops, out it whips
        float g = Anim.seconds(s.tongue, s.ageInTicks);
        boolean tongueOut = s.tongueOut > 0.01F;
        if (g >= 0.0F && (g < Cypole.TONGUE_FIRE / 20.0F + 0.3F || tongueOut)) {
            float aim = Anim.smooth(g / (Cypole.TONGUE_FIRE / 20.0F));
            float open = tongueOut ? 1.0F : Anim.envelope(g, Cypole.TONGUE_FIRE / 20.0F - 0.1F, 0.08F, 0.1F, 0.2F);
            bodyY += 1.2F * aim * (1.0F - 0.5F * open);
            this.body.xRot += 0.08F * aim;
            this.jaw.xRot += 0.15F * aim + 0.55F * open;
            this.head.xRot -= 0.12F * open;
            lidUp = Math.min(lidUp, 0.03F);
            lidDown = Math.min(lidDown, 0.03F);
        }
        this.body.y += bodyY;
        this.tongue.visible = tongueOut;
        this.tongueTip.visible = tongueOut;
        if (tongueOut) {
            this.aimTongue(s, bodyY * 0.85F);
        }

        // ---- the eyelids, last: a blink shuts it whatever else is going on
        float blink = Mth.clamp(s.blink, 0.0F, 1.0F);
        lidUp = Mth.lerp(blink, lidUp, 0.62F);
        lidDown = Mth.lerp(blink, lidDown, 0.45F);
        this.upperLid.yScale = Math.max(0.02F, lidUp);
        this.lowerLid.yScale = Math.max(0.02F, lidDown);
    }

    /** Points the tongue from the mouth to its target and stretches it there; the sticky tip sits on the end. */
    private void aimTongue(CypoleRenderState s, float mouthDrop) {
        float mx = this.tongue.x;
        float my = this.tongue.y + mouthDrop;
        float mz = this.tongue.z;
        float vx = s.tongueX - mx;
        float vy = s.tongueY - my;
        float vz = s.tongueZ - mz;
        float len = Mth.sqrt(vx * vx + vy * vy + vz * vz);
        if (len < 0.5F) {
            this.tongue.visible = false;
            this.tongueTip.visible = false;
            return;
        }
        float yaw = (float) Mth.atan2(-vx, -vz);
        float pitch = (float) Math.asin(Mth.clamp(vy / len, -1.0F, 1.0F));
        float out = Mth.clamp(s.tongueOut, 0.0F, 1.0F);
        this.tongue.y = my;
        this.tongue.yRot = yaw;
        this.tongue.xRot = pitch;
        this.tongue.zScale = Math.max(0.05F, len * out);
        // a little wobble along its length as it flies
        this.tongue.xScale *= 1.0F - 0.25F * out;
        this.tongueTip.x = mx + vx * out;
        this.tongueTip.y = my + vy * out;
        this.tongueTip.z = mz + vz * out;
        this.tongueTip.yRot = yaw;
        this.tongueTip.xRot = pitch;
    }
}
