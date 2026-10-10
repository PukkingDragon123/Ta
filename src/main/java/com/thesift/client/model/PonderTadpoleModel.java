package com.thesift.client.model;

import com.thesift.client.renderer.state.JungleRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * P4 Ponder Tadpole (geometry: tools/jungle_mobs.py). Its tail never stops: a wave runs down it (faster when it
 * moves) and the body wriggles against it; the little legs kick, the toothy jaw gapes now and then. animA bites
 * (jaw wide, a lunge); animB bores into wood (head-first, wriggling hard).
 */
public class PonderTadpoleModel extends EntityModel<JungleRenderState> {
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tip;
    private final ModelPart[] legs = new ModelPart[2];
    private final ModelPart[] feet = new ModelPart[2];

    public PonderTadpoleModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tip = this.tail.getChild("tail_tip");
        this.legs[0] = this.body.getChild("left_leg");
        this.legs[1] = this.body.getChild("right_leg");
        this.feet[0] = this.legs[0].getChild("left_foot");
        this.feet[1] = this.legs[1].getChild("right_foot");
    }

    @Override
    public void setupAnim(JungleRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 2.5F);
        float rate = 0.25F + 0.55F * walk;
        float wave = age * rate;
        this.tail.yRot += Mth.sin(wave) * (0.25F + 0.3F * walk);
        this.tip.yRot += Mth.sin(wave - 1.2F) * (0.45F + 0.4F * walk);
        this.body.yRot += Mth.sin(wave + 1.4F) * 0.12F * (0.4F + walk);
        this.body.y -= Math.abs(Mth.sin(wave)) * 0.6F * walk;
        for (int i = 0; i < 2; i++) {
            float sx = i == 0 ? 1.0F : -1.0F;
            float k = Mth.sin(wave * 1.5F + i * Mth.PI);
            this.legs[i].yRot += sx * k * 0.5F * walk;
            this.feet[i].zRot += sx * k * 0.4F * walk;
        }
        this.jaw.xRot += Math.max(0.0F, Mth.sin(age * 0.05F)) * 0.15F;
        this.body.xRot += s.xRot * Anim.DEG * 0.3F;

        float bt = Anim.seconds(s.animA, s.ageInTicks);
        if (bt >= 0.0F && bt < 0.45F) {
            float open = Anim.envelope(bt, 0.0F, 0.08F, 0.05F, 0.1F);
            float snap = Anim.envelope(bt, 0.12F, 0.04F, 0.05F, 0.2F);
            this.jaw.xRot += 0.9F * open;
            this.body.z -= 2.0F * snap;
            this.body.xRot -= 0.2F * open;
        }
        float bu = Anim.seconds(s.animB, s.ageInTicks);
        if (bu >= 0.0F && bu < 1.0F) {
            float e = Anim.envelope(bu, 0.0F, 0.1F, 0.6F, 0.3F);
            this.body.xRot += 0.6F * e;
            this.tail.yRot += Mth.sin(bu * 40.0F) * 0.6F * e;
            this.jaw.xRot += 0.5F * e;
        }
    }
}
