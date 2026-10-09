package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * S2: the Sculk Fish (geometry and hand-painted variants in tools/waterfolk.py). It swims like vanilla's
 * fish - a travelling wave down the body into the peduncle and the tail fan - with its underbite jaw a
 * little agape. Its two brow tendrils sway; when the school hunts they stand forward and shiver like the
 * Warden's, the jaw hangs wider and the spiny dorsal rises. The snap: the jaw flies open and the fish
 * lunges. Stranded, it lies on its side and flaps; hurt, it curls; dying, it rolls belly-up.
 */
public class SculkFishModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart dorsal;
    private final ModelPart analFin;
    private final ModelPart tailStem;
    private final ModelPart tail;
    private final ModelPart[] fins = new ModelPart[2];
    private final ModelPart[] tendrils = new ModelPart[2];

    public SculkFishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.dorsal = this.body.getChild("dorsal");
        this.analFin = this.body.getChild("anal_fin");
        this.tailStem = this.body.getChild("tail_stem");
        this.tail = this.tailStem.getChild("tail");
        this.fins[0] = this.body.getChild("left_fin");
        this.fins[1] = this.body.getChild("right_fin");
        this.tendrils[0] = this.body.getChild("left_tendril");
        this.tendrils[1] = this.body.getChild("right_tendril");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.inLiquid ? s.effort : 1.0F;
        float hunt = s.aggressive ? 1.0F : 0.0F;
        float beat = age * (0.32F + 0.6F * e);
        float amp = 0.22F + 0.36F * e;
        this.body.yRot = -Mth.sin(beat) * 0.06F * (1.0F + e);
        this.tailStem.yRot = Mth.sin(beat - 0.8F) * amp * 0.7F;
        this.tail.yRot = Mth.sin(beat - 1.6F) * amp;
        this.analFin.yRot = Mth.sin(beat - 1.0F) * amp * 0.35F;
        this.body.xRot = s.xRot * Anim.DEG;
        this.body.zRot = Mth.sin(beat * 0.5F) * 0.04F * e;
        // the jaw hangs a little open, wider on the hunt; the spiny dorsal rises
        this.jaw.xRot = 0.08F + Mth.sin(age * 0.1F) * 0.04F + 0.15F * hunt;
        this.dorsal.xRot = -0.25F * e * (1.0F - hunt) + 0.1F * hunt;
        this.dorsal.yScale = 1.0F + 0.2F * hunt;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.fins[k].zRot += sx * Mth.sin(age * (0.5F + 0.3F * (1.0F - e)) + k) * 0.3F;
            this.fins[k].yRot += sx * 0.3F * e;
            // the brow tendrils sway; hunting, they stand forward and shiver
            this.tendrils[k].xRot += Mth.sin(age * 0.09F + k) * 0.12F + 0.3F * e * (1.0F - hunt) - 0.45F * hunt
                    + Mth.sin(s.ageInTicks * 1.7F + k) * 0.1F * hunt;
            this.tendrils[k].yRot += sx * Mth.sin(age * 0.07F + k * 0.6F) * 0.1F;
        }
        // the snap (0.35 s): jaw wide, a lunge
        float snap = Anim.seconds(s.snap, s.ageInTicks);
        if (snap >= 0.0F && snap < 0.4F) {
            float open = Anim.envelope(snap, 0.0F, 0.06F, 0.04F, 0.2F);
            this.jaw.xRot += 0.75F * open;
            this.body.z -= 1.5F * open;
        }
        if (!s.inLiquid) {
            this.body.zRot = Mth.HALF_PI;
            this.body.y += 2.5F;
            this.tailStem.yRot = Mth.sin(s.ageInTicks * 1.1F) * 0.6F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 1.1F - 0.7F) * 0.8F;
            this.jaw.xRot = 0.2F + Mth.sin(s.ageInTicks * 0.4F) * 0.15F;
        }
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float k = Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI);
            this.tailStem.yRot += 0.5F * k;
            this.tail.yRot += 0.4F * k;
            this.jaw.xRot += 0.3F * k;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = Mth.PI * die;
        }
    }
}
