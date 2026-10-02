package com.thesift.client.model;

import com.thesift.client.renderer.state.SculklingRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Sculkling. A fast, low scurry - bent legs pattering, long arms swinging, ears streaming back with
 * speed and the tail whipping behind. Its giant ears ride on springs: they twitch towards every
 * sound, perk up and flop over. Covering its ears (music!), both claws clamp over the ear roots,
 * the ears fold down and it trembles, crouched. Snatching, it darts a claw out and hugs the prize
 * to its chest; a thief runs with it clutched there. Giggling, it chatters and its shoulders
 * bounce; screeching, the head goes back, jaw wide, ears flared.
 */
public class SculklingModel extends EntityModel<SculklingRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailTip;
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] hands = new ModelPart[2];
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] earTips = new ModelPart[2];

    public SculklingModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tailTip = this.tail.getChild("tail_tip");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.legs[i] = root.getChild(s + "_leg");
            this.shins[i] = this.legs[i].getChild(s + "_shin");
            this.arms[i] = this.body.getChild(s + "_arm");
            this.forearms[i] = this.arms[i].getChild(s + "_forearm");
            this.hands[i] = this.forearms[i].getChild(s + "_hand");
            this.ears[i] = this.head.getChild(s + "_ear");
            this.earTips[i] = this.ears[i].getChild(s + "_ear_tip");
        }
    }

    public ModelPart body() {
        return this.body;
    }

    public ModelPart rightArm() {
        return this.arms[1];
    }

    public ModelPart rightForearm() {
        return this.forearms[1];
    }

    public ModelPart rightHand() {
        return this.hands[1];
    }

    @Override
    public void setupAnim(SculklingRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.8F);
        float pos = s.walkAnimationPos * 1.6F;
        float sw = Mth.sin(pos);

        // ---- the scurry
        for (int i = 0; i < 2; i++) {
            float phase = i == 0 ? sw : -sw;
            float sx = i == 0 ? 1.0F : -1.0F;
            this.legs[i].xRot += phase * 0.9F * walk;
            this.shins[i].xRot += Math.max(0.0F, -phase) * 0.6F * walk;
            this.arms[i].xRot -= phase * 0.8F * walk;
            this.arms[i].zRot -= sx * 0.15F * walk;
            // ears stream back with speed, and flop on their springs
            float spring = i == 0 ? s.leftEar : s.rightEar;
            this.ears[i].xRot += 0.5F * walk + spring;
            this.ears[i].zRot += sx * (Mth.sin(age * 0.09F + i * 2.1F) * 0.04F - spring * 0.3F);
            this.earTips[i].zRot += sx * (Mth.sin(pos - 0.8F + i) * 0.15F * walk - spring * 0.6F + Mth.sin(age * 0.13F + i) * 0.05F);
            this.earTips[i].xRot += spring * 0.5F + 0.3F * walk;
        }
        this.body.xRot += 0.25F * walk;
        this.body.y += 0.8F * walk - Math.abs(Mth.cos(pos)) * 0.6F * walk;
        this.body.zRot += sw * 0.08F * walk;
        this.head.xRot -= 0.2F * walk;
        this.head.yRot += s.yRot * Anim.DEG * 0.7F;
        this.head.xRot += s.xRot * Anim.DEG * 0.5F;
        this.head.zRot += Mth.sin(age * 0.05F) * 0.06F * (1.0F - walk);
        this.tail.yRot += Mth.sin(pos * 0.5F) * 0.5F * walk + Mth.sin(age * 0.08F) * 0.15F;
        this.tailTip.yRot += Mth.sin(pos * 0.5F - 1.0F) * 0.6F * walk + Mth.sin(age * 0.08F - 1.0F) * 0.2F;
        this.body.xRot += Mth.sin(age * 0.12F) * 0.02F;

        // ---- running off with loot: the prize hugged to its chest in the right claw
        if (s.hasLoot) {
            this.arms[1].xRot = -1.0F;
            this.arms[1].zRot = 0.35F;
            this.forearms[1].xRot = -1.3F;
        }

        // ---- covering its ears: claws clamped over the ear roots, ears folded down, trembling
        float coverIn = Anim.seconds(s.cover, s.ageInTicks);
        float cover = s.scared ? (coverIn >= 0.0F ? Anim.backOut(coverIn / 0.25F) : 1.0F) : 0.0F;
        if (cover > 0.0F) {
            float tremble = Mth.sin(age * 2.6F) * 0.04F;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.arms[i].xRot = Mth.lerp(cover, this.arms[i].xRot, -2.6F);
                this.arms[i].zRot = Mth.lerp(cover, this.arms[i].zRot, -0.55F * sx);
                this.forearms[i].xRot = Mth.lerp(cover, this.forearms[i].xRot, -0.9F);
                this.forearms[i].zRot = Mth.lerp(cover, this.forearms[i].zRot, 0.6F * sx);
                this.ears[i].xRot += 0.9F * cover;
                this.ears[i].zRot += sx * 0.7F * cover;
                this.earTips[i].xRot += 0.8F * cover;
            }
            this.head.xRot += (0.4F + tremble) * cover;
            this.head.zRot += tremble * cover;
            this.body.y += 1.0F * cover;
            this.jaw.xRot += 0.15F * cover;
        }

        // ---- the snatch: a dart of the claw, then the prize yanked in
        float sn = Anim.seconds(s.snatch, s.ageInTicks);
        if (sn >= 0.0F && sn < 0.6F) {
            float reach = Anim.envelope(sn, 0.0F, 0.08F, 0.05F, 0.3F);
            this.arms[1].xRot -= 1.4F * reach;
            this.forearms[1].xRot += 1.1F * reach;
            this.body.xRot += 0.35F * reach;
            this.body.z -= 1.5F * reach;
            this.jaw.xRot += 0.4F * Anim.envelope(sn, 0.2F, 0.1F, 0.1F, 0.2F);
        }

        // ---- giggling: chattering jaw, bouncing shoulders, wiggling ears
        float g = Anim.seconds(s.giggle, s.ageInTicks);
        if (g >= 0.0F && g < 1.0F) {
            float e = Anim.envelope(g, 0.0F, 0.1F, 0.6F, 0.3F);
            this.jaw.xRot += (0.2F + Mth.sin(g * 40.0F) * 0.2F) * e;
            this.body.y -= Math.abs(Mth.sin(g * 20.0F)) * 0.5F * e;
            this.head.zRot += Mth.sin(g * 12.0F) * 0.12F * e;
            for (int i = 0; i < 2; i++) {
                this.earTips[i].zRot += Mth.sin(g * 25.0F + i * Mth.PI) * 0.2F * e;
            }
        }

        // ---- screeching: head thrown back, jaw wide, ears flared, claws spread
        float sc = Anim.seconds(s.screech, s.ageInTicks);
        if (sc >= 0.0F && sc < 0.9F) {
            float e = Anim.envelope(sc, 0.0F, 0.1F, 0.4F, 0.4F);
            this.head.xRot -= 0.5F * e;
            this.jaw.xRot += 0.9F * e;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.ears[i].zRot -= sx * 0.35F * e;
                this.ears[i].xRot -= 0.3F * e;
                this.arms[i].zRot -= sx * 0.7F * e;
                this.arms[i].xRot -= 0.5F * e;
            }
        }
    }
}
