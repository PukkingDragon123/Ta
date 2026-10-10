package com.thesift.client.model;

import com.thesift.client.renderer.state.GrubRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Grub. Shelled, it skitters on six stubby legs in a tripod gait under its crust of rock, mandibles working and
 * antennae feeling ahead; each note of music makes the shell shudder, and its pieces fall away one by one (the loose
 * rock, the head cap, the sides, the top). Free, the gem creature walks lighter, its crystal wings quivering and
 * now and then buzzing open; drinking from a Reservoir it dips its head and pumps.
 */
public class GrubModel extends EntityModel<GrubRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] mandibles = new ModelPart[2];
    private final ModelPart[] antennae = new ModelPart[2];
    private final ModelPart[] wings = new ModelPart[2];
    private final ModelPart[][] legs = new ModelPart[2][3];
    private final ModelPart shellTop;
    private final ModelPart shellLeft;
    private final ModelPart shellRight;
    private final ModelPart shellHead;
    private final ModelPart shellRock;

    public GrubModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        String[] sides = {"left", "right"};
        for (int i = 0; i < 2; i++) {
            this.mandibles[i] = this.head.getChild(sides[i] + "_mandible");
            this.antennae[i] = this.head.getChild(sides[i] + "_antenna");
            this.wings[i] = this.body.getChild(sides[i] + "_wing");
            for (int k = 0; k < 3; k++) {
                this.legs[i][k] = this.body.getChild(sides[i] + "_leg_" + k);
            }
        }
        this.shellTop = this.body.getChild("shell_top");
        this.shellLeft = this.body.getChild("shell_left");
        this.shellRight = this.body.getChild("shell_right");
        this.shellHead = this.head.getChild("shell_head");
        this.shellRock = this.body.getChild("shell_rock");
    }

    @Override
    public void setupAnim(GrubRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.5F);
        float pos = s.walkAnimationPos * 2.2F;

        // ---- the shell, falling away piece by piece as music cracks it
        this.shellRock.visible = s.crackStage < 1;
        this.shellHead.visible = s.crackStage < 2;
        this.shellLeft.visible = s.crackStage < 3;
        this.shellRight.visible = s.crackStage < 3;
        this.shellTop.visible = s.crackStage < 4;

        // ---- the tripod skitter: legs 0 and 2 of one side with 1 of the other
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            for (int k = 0; k < 3; k++) {
                float ph = pos + ((i + k) % 2 == 0 ? 0.0F : Mth.PI);
                this.legs[i][k].xRot += Mth.sin(ph) * 0.6F * walk;
                this.legs[i][k].zRot -= sx * Math.max(0.0F, Mth.cos(ph)) * 0.35F * walk;
                this.legs[i][k].zRot += sx * Mth.sin(age * 0.1F + k) * 0.03F;
            }
            // mandibles work, antennae feel the air
            this.mandibles[i].yRot += sx * (Mth.sin(age * 0.25F + i) * 0.12F + 0.1F * walk);
            this.antennae[i].xRot += Mth.sin(age * 0.11F + i * 1.3F) * 0.12F + 0.25F * walk;
            this.antennae[i].zRot += sx * Mth.sin(age * 0.07F + i) * 0.1F;
        }
        this.body.y += Math.abs(Mth.sin(pos)) * -0.3F * walk;
        this.body.zRot += Mth.sin(pos) * 0.05F * walk;
        this.head.yRot += s.yRot * Anim.DEG * 0.5F;
        this.head.xRot += s.xRot * Anim.DEG * 0.4F;

        // ---- free: the crystal wings quiver, and buzz open now and then
        if (s.cracked) {
            float buzz = Math.max(0.0F, Mth.sin(age * 0.05F + s.seed) - 0.6F) * 2.5F;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.wings[i].zRot -= sx * (0.25F * buzz + Mth.sin(age * 2.4F) * 0.25F * buzz + Mth.sin(age * 0.3F) * 0.03F);
                this.wings[i].yRot -= sx * 0.35F * buzz;
            }
            this.body.y -= 0.5F * buzz;
        }

        // ---- a note lands: the shell shudders
        float c = Anim.seconds(s.creak, s.ageInTicks);
        if (c >= 0.0F && c < 0.5F) {
            float shake = (1.0F - c / 0.5F) * Mth.sin(c * 70.0F) * 0.12F;
            this.body.zRot += shake;
            this.shellTop.y -= Math.abs(shake) * 4.0F;
        }
        // ---- bursting free: a squash, then wings flung wide
        float b = Anim.seconds(s.burst, s.ageInTicks);
        if (b >= 0.0F && b < 1.2F) {
            float squash = Anim.envelope(b, 0.0F, 0.05F, 0.05F, 0.2F);
            float spread = Anim.envelope(b, 0.1F, 0.1F, 0.4F, 0.5F);
            this.body.yScale = 1.0F - 0.25F * squash;
            this.body.xScale = 1.0F + 0.2F * squash;
            for (int i = 0; i < 2; i++) {
                float sx = i == 0 ? 1.0F : -1.0F;
                this.wings[i].zRot -= sx * 1.1F * spread;
            }
        }
        // ---- drinking: head down, pumping
        float d = Anim.seconds(s.drink, s.ageInTicks);
        if (d >= 0.0F && d < 1.0F) {
            float dk = Anim.envelope(d, 0.0F, 0.15F, 0.5F, 0.3F);
            this.head.xRot += (0.45F + Mth.sin(d * 25.0F) * 0.1F) * dk;
            this.body.xRot += 0.15F * dk;
        }
    }
}
