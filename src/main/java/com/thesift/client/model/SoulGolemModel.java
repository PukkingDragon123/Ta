package com.thesift.client.model;

import com.thesift.client.renderer.state.SoulGolemRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Soul Golem (CAVE: remodelled in tools/echoer.py with more moving parts): walks with a rocking waddle-hop -
 * the whole body tips from foot to foot, squashes as each foot plants and stretches as it rises, arms swing
 * out for balance with the hands flapping behind them, the feet stay flat on the ground - and its hanging
 * lantern swings a beat behind the stalk, which lags behind the body. Digging, it leans in and scoops with
 * both hands in turn; peeking, it crouches low, brows down, and leans forward, the lantern lighting the block.
 * Run down, it slumps forward, arms hanging, jaw dropped, brows drooping. When happy (a find, or the Golem
 * Hymn) it does a little spinning hop with arms up, brows raised and mouth open. Digging and peeking blend in
 * and out (they used to snap).
 */
public class SoulGolemModel extends EntityModel<SoulGolemRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart stalk;
    private final ModelPart stalkTip;
    private final ModelPart lamp;
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[] eyes = new ModelPart[2];
    private final ModelPart[] brows = new ModelPart[2];

    public SoulGolemModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.stalk = this.body.getChild("stalk");
        this.stalkTip = this.stalk.getChild("stalk_tip");
        this.lamp = this.stalkTip.getChild("lamp");
        for (int k = 0; k < 2; k++) {
            String side = SIDES[k];
            this.legs[k] = root.getChild(side + "_leg");
            this.feet[k] = this.legs[k].getChild(side + "_foot");
            this.arms[k] = this.body.getChild(side + "_arm");
            this.hands[k] = this.arms[k].getChild(side + "_hand");
            this.eyes[k] = this.body.getChild(side + "_eye");
            this.brows[k] = this.eyes[k].getChild(side + "_brow");
        }
    }

    @Override
    public void setupAnim(SoulGolemRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float up = 1.0F - s.slump;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.0F) * up;
        float pos = s.walkAnimationPos * 0.9F;
        float sw = Mth.sin(pos);
        float hop = Math.abs(Mth.cos(pos));
        float jawOpen = 0.0F;
        float brow = 0.0F;

        // --- the waddle-hop: tip onto each foot, bounce, squash as it plants and stretch as it rises
        this.body.zRot += sw * 0.16F * walk;
        this.body.y -= hop * 1.4F * walk;
        float squash = (0.45F - hop) * 0.14F * walk + Mth.sin(age * 0.08F + s.seed) * 0.012F * up;
        this.body.yScale = 1.0F - squash;
        this.body.xScale = 1.0F + squash * 0.6F;
        this.body.zScale = 1.0F + squash * 0.6F;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            float step = sw * sx;
            this.legs[k].xRot += step * 0.7F * walk;
            this.legs[k].y -= Math.max(0.0F, step) * 0.8F * walk;
            // the foot stays flat, its toes lifting as it leaves the ground
            this.feet[k].xRot += -step * 0.7F * walk + Math.max(0.0F, step) * 0.25F * walk;
            // arms out for balance, the hands a beat behind them
            this.arms[k].zRot -= (0.25F + step * 0.2F) * walk * sx;
            this.arms[k].xRot -= step * 0.3F * walk;
            this.hands[k].xRot -= Mth.sin(pos - 0.9F) * 0.35F * walk * sx;
            this.hands[k].zRot += Mth.sin(age * 0.07F + k) * 0.05F * up;
        }

        // --- looking around: the whole body turns a little, the eye domes a little more
        float yaw = s.yRot * Anim.DEG;
        this.body.yRot += yaw * 0.35F * up;
        for (int k = 0; k < 2; k++) {
            this.eyes[k].yRot += yaw * 0.25F * up;
        }
        this.body.xRot += s.xRot * Anim.DEG * 0.2F * up;

        // --- digging (blended): lean in, both hands scooping in turn
        float dig = s.dig * up;
        if (dig > 0.0F) {
            float d = age * 0.9F;
            this.body.xRot += 0.45F * dig;
            this.body.y += 0.8F * dig;
            for (int k = 0; k < 2; k++) {
                this.arms[k].xRot += (-1.2F + Mth.sin(d + k * Mth.PI) * 0.6F) * dig;
                this.hands[k].xRot += (-0.4F + Mth.sin(d + k * Mth.PI - 0.8F) * 0.5F) * dig;
            }
            this.stalk.xRot += (0.3F + Mth.sin(d * 2.0F) * 0.08F) * dig;
            jawOpen += 0.15F * dig;
        }
        // --- peeking (blended): crouched, leaning far over, brows down, the lantern brought over the block
        float peek = s.peek * up;
        if (peek > 0.0F) {
            float wobble = Mth.sin(age * 0.15F) * 0.05F;
            this.body.xRot += (0.6F + wobble) * peek;
            this.body.y += 1.2F * peek;
            this.stalk.xRot += 0.5F * peek;
            this.stalkTip.xRot += 0.3F * peek;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].zRot -= 0.3F * peek * sx;
                this.arms[k].xRot -= 0.6F * peek;
            }
            brow -= 0.3F * peek;
        }

        // --- run down: slumped forward, arms hanging, jaw dropped, the lantern drooping
        if (s.slump > 0.0F) {
            float k = s.slump;
            this.body.xRot += 0.5F * k;
            this.body.y += 1.2F * k;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.arms[i].xRot -= 0.35F * k;
                this.arms[i].zRot += 0.1F * k * sx;
                this.hands[i].xRot += 0.3F * k;
            }
            this.stalk.xRot += 0.6F * k;
            this.stalkTip.xRot += 0.4F * k;
            jawOpen += 0.35F * k;
            brow -= 0.35F * k;
        }

        // --- happy: a crouch, a spinning hop with both arms flung up, mouth open, a wobbly landing
        float h = Anim.seconds(s.happy, age);
        if (h >= 0.0F && h < 1.2F) {
            float crouch = Anim.envelope(h, 0.0F, 0.12F, 0.0F, 0.12F);
            float air = Anim.envelope(h, 0.12F, 0.12F, 0.25F, 0.3F);
            float land = Anim.envelope(h, 0.65F, 0.08F, 0.0F, 0.45F);
            this.body.y += 1.2F * crouch - 3.0F * air + 0.8F * land;
            this.body.yRot += Anim.smooth((h - 0.12F) / 0.55F) * Mth.TWO_PI;
            float st = 0.18F * crouch - 0.12F * air + 0.2F * land;
            this.body.yScale *= 1.0F - st;
            this.body.xScale *= 1.0F + st * 0.6F;
            this.body.zScale *= 1.0F + st * 0.6F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].zRot -= 2.2F * air * sx;
                this.hands[k].zRot -= 0.5F * air * sx;
            }
            this.body.zRot += Mth.sin(h * 30.0F) * 0.12F * land;
            this.stalk.xRot -= 0.6F * air - 0.4F * land;
            jawOpen += 0.45F * air;
            brow += 0.35F * (air + crouch);
        }

        // --- the face: brows and jaw
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.brows[k].xRot -= brow;
            this.brows[k].y -= Math.max(0.0F, brow) * 0.6F;
            this.brows[k].zRot += Mth.sin(age * 0.05F + k * 2.0F) * 0.03F * sx;
        }
        this.jaw.xRot -= jawOpen + Math.max(0.0F, Mth.sin(age * 0.06F + s.seed)) * 0.04F * up;

        // --- the stalk lags behind the body, its tip behind that; the lantern swings on its hook and keeps
        // hanging down however the golem leans
        this.stalk.zRot -= Mth.sin(pos - 0.8F) * 0.25F * walk + Mth.sin(age * 0.08F + s.seed) * 0.06F * up;
        this.stalk.xRot += Mth.cos(age * 0.06F + s.seed) * 0.05F * up;
        this.stalkTip.zRot -= Mth.sin(pos - 1.4F) * 0.2F * walk;
        this.stalkTip.xRot += Mth.cos(age * 0.06F + s.seed - 0.7F) * 0.06F * up;
        float lean = this.body.xRot + this.stalk.xRot + 0.15F + this.stalkTip.xRot - 0.9F;
        this.lamp.xRot -= lean + Mth.sin(pos - 2.0F) * 0.25F * walk;
        this.lamp.zRot -= this.body.zRot + this.stalk.zRot + this.stalkTip.zRot - Mth.sin(age * 0.11F + s.seed) * 0.08F;
    }
}
