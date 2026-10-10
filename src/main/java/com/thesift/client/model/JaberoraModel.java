package com.thesift.client.model;

import com.thesift.client.renderer.state.JaberoraRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Jaberora, a jerboa of the dunes. It bounds like a kangaroo-rat: a deep crouch on its springy legs, then legs flung out behind, tail up
 * for balance, hands tucked; each landing squashes. Sitting up it twitches its round ears and sways its long tail.
 * Its aria: head thrown back, the vast jaw wide and trembling with vibrato, little arms spread like a diva's. Its
 * piercing note: ears flat, jaw snapped wide, the whole body shuddering. Napping it curls up with its tail round it.
 */
public class JaberoraModel extends EntityModel<JaberoraRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tailMid;
    private final ModelPart tailTip;
    private final ModelPart[] ears = new ModelPart[2];
    private final ModelPart[] lids = new ModelPart[2];
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] thighs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public JaberoraModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = root.getChild("tail");
        this.tailMid = this.tail.getChild("tail_mid");
        this.tailTip = this.tailMid.getChild("tail_tip");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            String s = sides[i];
            this.ears[i] = this.head.getChild(s + "_ear");
            this.lids[i] = this.head.getChild(s + "_eye").getChild(s + "_eyelid");
            this.arms[i] = this.body.getChild(s + "_arm");
            this.thighs[i] = root.getChild(s + "_thigh");
            this.shins[i] = this.thighs[i].getChild(s + "_shin");
            this.feet[i] = this.shins[i].getChild(s + "_foot");
        }
    }

    @Override
    public void setupAnim(JaberoraRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float crouch = s.crouch;
        float air = s.air;
        boolean rest = s.sitting || s.riding;

        // ---- idle life: breathing, ear twitches, a swaying tail, a cocked head
        float breath = Mth.sin(age * 0.12F);
        this.body.yScale = 1.0F + breath * 0.02F;
        this.head.yRot += s.yRot * Anim.DEG * 0.8F;
        this.head.xRot += s.xRot * Anim.DEG * 0.6F;
        this.head.zRot += Mth.sin(age * 0.04F) * 0.08F;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            float twitch = Math.max(0.0F, Mth.sin(age * 0.21F + i * 1.7F) - 0.92F) * 4.0F;
            this.ears[i].zRot += sx * (twitch * 0.35F + Mth.sin(age * 0.06F + i) * 0.04F);
            this.ears[i].xRot -= 0.25F * air;
            this.lids[i].visible = s.blink > 0.5F || s.napping;
            this.arms[i].xRot += Mth.sin(age * 0.09F + i) * 0.06F;
        }
        this.tail.yRot += Mth.sin(age * 0.05F) * 0.25F;
        this.tailMid.yRot += Mth.sin(age * 0.05F - 0.8F) * 0.3F;
        this.tailTip.yRot += Mth.sin(age * 0.05F - 1.6F) * 0.3F;
        // a jerboa's tail flick: now and then a quick whip up and back, the tuft lagging behind
        float flick = Math.max(0.0F, Mth.sin(age * 0.11F + s.seed) - 0.88F) * 8.0F;
        this.tail.xRot += 0.15F * flick;
        this.tailMid.xRot += 0.35F * flick;
        this.tailTip.xRot += 0.55F * flick - 0.2F * Math.max(0.0F, Mth.sin(age * 0.11F + s.seed - 0.6F) - 0.88F) * 8.0F;

        // ---- the bound: crouch, then legs flung out behind and the tail up; it lands on its feet
        for (int i = 0; i < 2; i++) {
            this.thighs[i].xRot += 0.45F * crouch - 0.8F * air;
            this.shins[i].xRot += -0.35F * crouch + 0.55F * air;
            this.feet[i].xRot += -0.1F * crouch + 0.6F * air;
            this.arms[i].xRot -= 0.6F * air;
        }
        this.body.y += 1.4F * crouch - 0.6F * air;
        this.body.xRot += 0.15F * crouch - 0.25F * air;
        this.head.xRot -= 0.15F * crouch - 0.2F * air;
        // the tail balances the bound: down as it crouches, swung up behind as it flies, the tuft trailing
        this.tail.xRot += 0.5F * air - 0.15F * crouch;
        this.tailMid.xRot += 0.25F * air - 0.1F * crouch;
        this.tailTip.xRot -= 0.3F * air;
        for (int i = 0; i < 2; i++) {
            this.thighs[i].y += 1.4F * crouch;
        }

        // ---- sitting (told to, or riding a Kerkorer): settled on its haunches
        if (rest) {
            for (int i = 0; i < 2; i++) {
                this.thighs[i].xRot -= 0.5F;
                this.thighs[i].y += 1.0F;
                this.shins[i].xRot += 0.6F;
                this.feet[i].xRot -= 0.1F;
            }
            this.body.y += 1.0F;
            this.tail.y += 1.0F;
        }

        // ---- napping: curled up, nose to tail
        if (s.napping) {
            this.body.xRot += 0.6F;
            this.body.y += 3.2F;
            this.head.xRot += 0.5F;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.thighs[i].xRot -= 0.9F;
                this.thighs[i].y += 2.5F;
                this.shins[i].xRot += 1.1F;
                this.ears[i].zRot += sx * 0.35F;
                this.arms[i].xRot -= 0.8F;
            }
            this.tail.y += 2.5F;
            this.tail.yRot += 1.1F;
            this.tailMid.yRot += 0.9F;
            this.tailTip.yRot += 0.8F;
            this.body.yScale += breath * 0.03F;
        }

        // ---- the aria: head back, the jaw wide with vibrato, arms spread
        float sg = Anim.seconds(s.sing, s.ageInTicks);
        float sing = s.singing ? Anim.smooth(sg < 0.0F ? 1.0F : sg / 0.4F) : 0.0F;
        if (sing > 0.0F) {
            float vib = Mth.sin(age * 1.6F) * 0.07F;
            float phrase = 0.55F + 0.25F * Mth.sin(age * 0.3F);
            this.head.xRot -= 0.4F * sing;
            this.jaw.xRot += (phrase + vib) * sing;
            this.body.y -= 0.6F * sing;
            this.body.xRot -= 0.12F * sing;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.arms[i].xRot -= 0.9F * sing;
                this.arms[i].zRot -= sx * 0.7F * sing;
                this.ears[i].xRot += 0.3F * sing;
            }
        }

        // ---- the piercing note
        float p = Anim.seconds(s.pulse, s.ageInTicks);
        if (p >= 0.0F && p < 0.9F) {
            float pk = Anim.envelope(p, 0.0F, 0.06F, 0.35F, 0.4F);
            float shudder = Mth.sin(age * 4.0F) * 0.05F * pk;
            this.jaw.xRot += 1.0F * pk;
            this.head.xRot += 0.15F * pk + shudder;
            this.head.zRot += shudder;
            this.body.zRot += shudder;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.ears[i].xRot += 0.9F * pk;
                this.ears[i].zRot += sx * 0.5F * pk;
                this.arms[i].zRot -= sx * 0.4F * pk;
            }
        }

        // ---- eating: chewing, the fruit held up in its little hands
        float e = Anim.seconds(s.eat, s.ageInTicks);
        if (e >= 0.0F && e < 1.2F) {
            float ek = Anim.envelope(e, 0.0F, 0.15F, 0.8F, 0.25F);
            this.jaw.xRot += Math.abs(Mth.sin(e * 18.0F)) * 0.25F * ek;
            this.head.xRot += 0.2F * ek;
            for (int i = 0; i < 2; i++) {
                this.arms[i].xRot -= 1.1F * ek;
            }
        }
    }
}
