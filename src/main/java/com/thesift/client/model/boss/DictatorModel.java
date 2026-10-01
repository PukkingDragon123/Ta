package com.thesift.client.model.boss;

import com.thesift.client.model.Anim;
import com.thesift.client.renderer.state.DictatorRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * The Dictator.
 *
 * <ul>
 *   <li>idle: conducts - the baton traces a slow figure of eight, the other hand keeps time, the
 *   head tilts to the music, coat tails sway</li>
 *   <li>move: long, fast, loping strides, arms trailing</li>
 *   <li>attack: a vicious backhand slash with the baton</li>
 *   <li>blink: folds down into himself and vanishes</li>
 *   <li>summon: both arms sweep outward, calling up the orchestra</li>
 *   <li>crescendo: arms and baton thrown high, head back</li>
 *   <li>roar (new phase): doubles over then rears up, crown flaring</li>
 *   <li>hurt: head snaps aside, shoulders hunch</li>
 *   <li>death: drops to his knees, slumps forward and crumbles away</li>
 * </ul>
 */
public class DictatorModel extends EntityModel<DictatorRenderState> {
    private final ModelPart body;
    private final ModelPart torso;
    private final ModelPart neck;
    private final ModelPart head;
    private final ModelPart crown;
    private final ModelPart coatTail;
    private final ModelPart[] arms = new ModelPart[2];
    private final ModelPart[] forearms = new ModelPart[2];
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] shins = new ModelPart[2];

    public DictatorModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.torso = this.body.getChild("torso");
        this.neck = this.torso.getChild("neck");
        this.head = this.neck.getChild("head");
        this.crown = this.head.getChild("crown");
        this.coatTail = this.torso.getChild("coat_tail");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            this.arms[i] = this.torso.getChild(sides[i] + "_arm");
            this.forearms[i] = this.arms[i].getChild(sides[i] + "_forearm");
            this.legs[i] = root.getChild(sides[i] + "_leg");
            this.shins[i] = this.legs[i].getChild(sides[i] + "_shin");
        }
    }

    @Override
    public void setupAnim(DictatorRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 1.2F);
        float pos = s.walkAnimationPos * 0.5F;
        float cw = Mth.cos(pos);
        float sw = Mth.sin(pos);
        float rest = 1.0F - walk;

        // --- long loping strides
        this.legs[0].xRot += cw * 0.75F * walk;
        this.legs[1].xRot -= cw * 0.75F * walk;
        this.shins[0].xRot += Math.max(0.0F, -sw) * 0.7F * walk;
        this.shins[1].xRot += Math.max(0.0F, sw) * 0.7F * walk;
        this.body.y -= Math.abs(cw) * 1.4F * walk;
        this.torso.xRot += 0.18F * walk + Mth.sin(age * 0.05F) * 0.03F;
        this.coatTail.xRot += 0.25F * walk + Mth.sin(age * 0.09F) * 0.06F + Math.abs(cw) * 0.2F * walk;

        // --- head follows you, tilting to the music
        this.head.yRot = s.yRot * Anim.DEG * 0.8F;
        this.head.xRot = s.xRot * Anim.DEG * 0.6F;
        this.head.zRot = Mth.sin(age * 0.045F) * 0.12F * rest;
        this.crown.yRot = Mth.sin(age * 0.03F) * 0.05F;

        // --- conducting: the baton traces a figure of eight, the left hand keeps the beat
        float beat = age * 0.12F;
        this.arms[1].xRot = -0.9F * rest + Mth.sin(beat) * 0.35F * rest + cw * 0.5F * walk;
        this.arms[1].zRot += Mth.sin(beat * 2.0F) * 0.25F * rest;
        this.forearms[1].xRot += -0.5F * rest + Mth.cos(beat) * 0.2F * rest;
        this.arms[0].xRot = -0.35F * rest + Math.max(0.0F, Mth.sin(beat * 2.0F)) * 0.3F * rest - cw * 0.5F * walk;
        this.forearms[0].xRot += -0.4F * rest;

        // --- hurt: head snaps aside, shoulders hunch
        if (s.hasRedOverlay) {
            this.head.zRot += 0.45F;
            this.head.xRot += 0.2F;
            this.torso.xRot += 0.15F;
        }

        float t;
        // --- slash
        if ((t = Anim.seconds(s.slash, age)) >= 0 && t < 0.5F) {
            float wind = Anim.envelope(t, 0.0F, 0.08F, 0.02F, 0.1F);
            float cut = Anim.envelope(t, 0.1F, 0.08F, 0.05F, 0.25F);
            this.arms[1].xRot += -1.6F * wind + 1.2F * cut;
            this.arms[1].zRot += 0.9F * wind - 1.0F * cut;
            this.torso.yRot += -0.4F * wind + 0.5F * cut;
        }
        // --- summon: both arms sweep outward
        if ((t = Anim.seconds(s.summon, age)) >= 0 && t < 1.4F) {
            float e = Anim.envelope(t, 0.0F, 0.35F, 0.6F, 0.4F);
            this.arms[0].zRot += -1.5F * e;
            this.arms[1].zRot += 1.5F * e;
            this.arms[0].xRot -= 0.5F * e;
            this.arms[1].xRot -= 0.5F * e;
            this.head.xRot -= 0.3F * e;
        }
        // --- crescendo: arms and baton high, head thrown back
        if ((t = Anim.seconds(s.crescendo, age)) >= 0 && t < 3.6F) {
            float e = Anim.envelope(t, 0.0F, 0.5F, 2.4F, 0.6F);
            float shake = Mth.sin(age * 2.5F) * 0.05F * e;
            this.arms[0].xRot = Mth.lerp(e, this.arms[0].xRot, -2.8F) + shake;
            this.arms[1].xRot = Mth.lerp(e, this.arms[1].xRot, -2.9F) - shake;
            this.arms[0].zRot += -0.35F * e;
            this.arms[1].zRot += 0.35F * e;
            this.forearms[0].xRot *= 1.0F - e;
            this.forearms[1].xRot *= 1.0F - e;
            this.head.xRot -= 0.6F * e;
            this.torso.xRot -= 0.2F * e;
        }
        // --- roar on entering a new phase
        if ((t = Anim.seconds(s.roar, age)) >= 0 && t < 2.0F) {
            float bow = Anim.envelope(t, 0.0F, 0.25F, 0.2F, 0.3F);
            float rear = Anim.envelope(t, 0.5F, 0.2F, 0.7F, 0.5F);
            this.torso.xRot += 0.7F * bow - 0.35F * rear;
            this.head.xRot += 0.4F * bow - 0.7F * rear;
            this.arms[0].zRot += -0.9F * rear;
            this.arms[1].zRot += 0.9F * rear;
            this.crown.xScale = 1.0F + 0.3F * rear;
            this.crown.zScale = 1.0F + 0.3F * rear;
            this.crown.yScale = 1.0F + 0.4F * rear;
        }
        // --- blink: folds down into himself
        if ((t = Anim.seconds(s.blink, age)) >= 0 && t < 0.35F) {
            float e = 1.0F - Anim.smooth(t / 0.35F);
            this.body.yScale *= 0.4F + 0.6F * (1.0F - e);
        }

        // --- death: knees, slump, crumble
        if (s.dying > 0.0F) {
            float kneel = Anim.smooth(s.dying / 8.0F);
            float slump = Anim.smooth((s.dying - 6.0F) / 10.0F);
            this.body.y += 9.0F * kneel;
            this.legs[0].xRot = -1.4F * kneel;
            this.legs[1].xRot = -1.4F * kneel;
            this.shins[0].xRot = 1.9F * kneel;
            this.shins[1].xRot = 1.9F * kneel;
            this.torso.xRot += 0.9F * slump;
            this.head.xRot += 0.6F * slump;
            this.arms[0].xRot = Mth.lerp(slump, this.arms[0].xRot, 0.2F);
            this.arms[1].xRot = Mth.lerp(slump, this.arms[1].xRot, 0.2F);
            float crumble = Anim.smooth((s.dying - 12.0F) / 8.0F);
            this.body.xScale = 1.0F - crumble * 0.3F;
            this.body.zScale = 1.0F - crumble * 0.3F;
            this.body.yScale *= 1.0F - crumble * 0.45F;
        }
    }
}
