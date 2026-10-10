package com.thesift.client.model;

import com.thesift.client.renderer.state.SoulGolemRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Soul Golem, the copper mole (CAVE v4: geometry and paint in tools/echoer.py). Every pose is a smoothed amount
 * from the entity or an envelope, so nothing snaps.
 *
 * <ul>
 *   <li>walking: a rolling mole's waddle - the body sways from side to side and squashes a little at each
 *   step, the great forepaws paddle in turn as if swimming through the grass, the stubby hind legs patter, the
 *   head sways against the body, the antenna lags behind every move and its cap a beat behind that</li>
 *   <li>the drill nose turns slowly all the time and whirrs round while it digs</li>
 *   <li>digging in: nose down, forepaws scrabbling hard in turn; burrowing: it sinks nose-first into the ground
 *   until only its antenna shows, wiggling as it digs about down there; emerging: it pops up with a stretch,
 *   lands with a squash and shakes the soil off its head</li>
 *   <li>carrying a find: forepaws held up together in front of its drill; peeking: low over the block, the
 *   antenna's lamp leaned over it; run down: slumped, paws limp, antenna drooping; happy: a spinning hop</li>
 * </ul>
 */
public class SoulGolemModel extends EntityModel<SoulGolemRenderState> {
    private static final String[] SIDES = {"left", "right"};
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart drill;
    private final ModelPart antenna;
    private final ModelPart antennaTip;
    private final ModelPart tail;
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] paws = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public SoulGolemModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.drill = this.head.getChild("drill");
        this.antenna = this.head.getChild("antenna");
        this.antennaTip = this.antenna.getChild("antenna_tip");
        this.tail = this.body.getChild("tail");
        for (int k = 0; k < 2; k++) {
            String side = SIDES[k];
            this.arms[k] = this.body.getChild(side + "_arm");
            this.paws[k] = this.arms[k].getChild(side + "_paw");
            this.legs[k] = root.getChild(side + "_leg");
            this.feet[k] = this.legs[k].getChild(side + "_foot");
        }
    }

    /** For the renderer's held find. */
    public ModelPart body() {
        return this.body;
    }

    @Override
    public void setupAnim(SoulGolemRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float up = 1.0F - s.slump;
        float sink = Anim.smooth(s.burrow);
        float above = 1.0F - sink;
        float carry = s.carrying ? 1.0F : 0.0F;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.2F) * up * above;
        float pos = s.walkAnimationPos * 1.1F;
        float sw = Mth.sin(pos);
        float hop = Math.abs(Mth.cos(pos));
        float breath = Mth.sin(age * 0.08F) * up;

        // --- the waddle: sway, a little squash at each step, paws paddling in turn (held up together while carrying)
        this.body.zRot += sw * 0.12F * walk;
        this.body.y -= hop * 0.6F * walk;
        float squash = (0.5F - hop) * 0.08F * walk + breath * 0.012F;
        this.body.yScale = 1.0F - squash;
        this.body.xScale = 1.0F + squash * 0.6F;
        this.body.zScale = 1.0F + squash * 0.6F;
        float paddle = walk * (1.0F - 0.7F * carry);
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            float step = sw * sx;
            this.arms[k].xRot -= step * 0.6F * paddle;
            this.arms[k].zRot -= Math.max(0.0F, step) * 0.25F * paddle * sx;
            this.paws[k].xRot += Mth.sin(pos - 0.8F + k * Mth.PI) * 0.35F * paddle;
            this.legs[k].xRot -= step * 0.7F * walk;
            this.feet[k].xRot += step * 0.7F * walk + Math.max(0.0F, -step) * 0.2F * walk;
            // carrying: both paws raised together in front of the drill, cupping the find
            this.arms[k].xRot -= 0.75F * carry;
            this.arms[k].zRot += 0.3F * carry * sx;
            this.paws[k].xRot -= 0.35F * carry;
            this.paws[k].yRot += 0.4F * carry * sx;
            // idle: the claws flex now and then
            this.paws[k].zRot += Mth.sin(age * 0.05F + k * 1.7F) * 0.04F * up * sx;
        }

        // --- the head sways against the body and looks about; the drill turns slowly all the time
        float yaw = Mth.clamp(s.yRot, -50.0F, 50.0F) * Anim.DEG;
        this.head.yRot += yaw * 0.6F * up - sw * 0.08F * walk;
        this.head.xRot += s.xRot * Anim.DEG * 0.5F * up + Mth.sin(pos * 2.0F) * 0.03F * walk + breath * 0.02F;
        float spin = age * 0.06F;
        this.tail.yRot += Mth.sin(age * 0.2F) * 0.15F + sw * 0.3F * walk;

        // --- digging in: nose down, the forepaws scrabbling hard in turn, the drill whirring
        float dig = s.dig * up;
        if (dig > 0.0F) {
            float d = age * 1.1F;
            this.body.xRot += 0.25F * dig;
            this.head.xRot += 0.35F * dig;
            this.body.zRot += Mth.sin(d * 0.5F) * 0.05F * dig;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot += (-0.6F + Mth.sin(d + k * Mth.PI) * 0.9F) * dig;
                this.arms[k].zRot += Mth.cos(d + k * Mth.PI) * 0.2F * dig * sx;
                this.paws[k].xRot += (0.3F + Mth.sin(d + k * Mth.PI - 0.9F) * 0.6F) * dig;
            }
            spin += age * 0.9F * dig;
        }

        // --- burrowing: it sinks nose-first until only its antenna shows, and wiggles it as it digs about below
        if (sink > 0.0F) {
            float through = Mth.sin(sink * Mth.PI);
            this.body.y += 11.0F * sink;
            this.body.xRot += 0.4F * through;
            for (int k = 0; k < 2; k++) {
                this.legs[k].y += 11.0F * sink;
            }
            this.antenna.zRot += Mth.sin(age * 0.3F) * 0.2F * sink;
            this.antenna.xRot -= 0.4F * through + Mth.sin(age * 0.21F) * 0.1F * sink;
            spin += age * 0.6F * sink;
        }
        this.drill.zRot += spin;

        // --- emerging: a pop up out of the ground with a stretch, paws flung up, a squash on landing, a shake of the head
        float e = Anim.seconds(s.emerge, s.ageInTicks);
        if (e >= 0.0F && e < 1.1F) {
            float pop = Anim.envelope(e, 0.0F, 0.15F, 0.08F, 0.35F);
            float land = Anim.envelope(e, 0.35F, 0.07F, 0.05F, 0.3F);
            float shake = Anim.envelope(e, 0.55F, 0.05F, 0.25F, 0.2F);
            this.body.y -= 2.5F * pop;
            this.body.yScale *= 1.0F + 0.16F * pop - 0.16F * land;
            this.body.xScale *= 1.0F - 0.07F * pop + 0.1F * land;
            this.body.zScale *= 1.0F - 0.07F * pop + 0.1F * land;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot -= 1.0F * pop;
                this.arms[k].zRot -= 0.4F * pop * sx;
            }
            this.head.zRot += Mth.sin(e * 45.0F) * 0.18F * shake;
            this.antenna.zRot -= Mth.sin(e * 45.0F - 1.0F) * 0.3F * shake;
        }

        // --- peeking: crouched low over the block, the antenna's lamp leaned out over it
        float peek = s.peek * up;
        if (peek > 0.0F) {
            this.body.y += 0.8F * peek;
            this.body.xRot += 0.2F * peek;
            this.head.xRot += 0.4F * peek + Mth.sin(age * 0.15F) * 0.04F * peek;
            this.antenna.xRot -= 0.6F * peek;
        }

        // --- run down: slumped, paws limp out to the sides, the antenna drooping
        if (s.slump > 0.0F) {
            float k = s.slump;
            this.body.y += 1.0F * k;
            this.head.xRot += 0.5F * k;
            this.antenna.xRot += 0.8F * k;
            this.antennaTip.xRot += 0.4F * k;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.arms[i].zRot -= 0.3F * k * sx;
                this.paws[i].xRot += 0.3F * k;
            }
        }

        // --- happy: a crouch, a spinning hop with the paws up, a wobbly landing
        float h = Anim.seconds(s.happy, s.ageInTicks);
        if (h >= 0.0F && h < 1.2F) {
            float crouch = Anim.envelope(h, 0.0F, 0.12F, 0.0F, 0.12F);
            float air = Anim.envelope(h, 0.12F, 0.12F, 0.25F, 0.3F);
            float landing = Anim.envelope(h, 0.65F, 0.08F, 0.0F, 0.45F);
            this.body.y += 1.0F * crouch - 3.0F * air + 0.6F * landing;
            this.body.yRot += Anim.smooth((h - 0.12F) / 0.55F) * Mth.TWO_PI;
            float st = 0.16F * crouch - 0.12F * air + 0.18F * landing;
            this.body.yScale *= 1.0F - st;
            this.body.xScale *= 1.0F + st * 0.6F;
            this.body.zScale *= 1.0F + st * 0.6F;
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.arms[k].xRot -= 1.2F * air;
                this.arms[k].zRot -= 0.5F * air * sx;
            }
            this.body.zRot += Mth.sin(h * 30.0F) * 0.12F * landing;
        }

        // --- the antenna lags behind the body, its cap a beat behind that, and keeps roughly upright
        this.antenna.zRot -= Mth.sin(pos - 1.0F) * 0.18F * walk + Mth.sin(age * 0.07F) * 0.04F * up + this.body.zRot * 0.5F;
        this.antenna.xRot += Mth.cos(age * 0.06F) * 0.04F * up - (this.body.xRot + this.head.xRot) * 0.5F;
        this.antennaTip.zRot -= Mth.sin(pos - 1.8F) * 0.15F * walk + Mth.sin(age * 0.07F - 0.8F) * 0.05F * up;
    }
}
