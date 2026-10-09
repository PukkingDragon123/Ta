package com.thesift.client.model;

import com.thesift.client.renderer.state.SlumblerEggsRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * CR2: a clutch of Slumbler eggs (geometry in tools/slumbler.py), drawn translucent so the rainbow
 * tadpoles show through the jelly. Afloat, the raft bobs and the eggs jostle; the jelly breathes;
 * the little ones inside turn and twitch, more and more as they near hatching.
 */
public class SlumblerEggsModel extends EntityModel<SlumblerEggsRenderState> {
    private static final int EGGS = 7;
    private final ModelPart raft;
    private final ModelPart[] eggs = new ModelPart[EGGS];
    private final ModelPart[] jelly = new ModelPart[EGGS];

    public SlumblerEggsModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.raft = root.getChild("raft");
        for (int i = 0; i < EGGS; i++) {
            this.eggs[i] = this.raft.getChild("egg_" + i);
            this.jelly[i] = this.eggs[i].getChild("jelly_" + i);
        }
    }

    @Override
    public void setupAnim(SlumblerEggsRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float p = s.progress;
        if (s.afloat) {
            this.raft.y += Mth.sin(age * 0.08F) * 0.4F;
            this.raft.zRot = Mth.sin(age * 0.05F) * 0.04F;
            this.raft.xRot = Mth.sin(age * 0.06F + 1.0F) * 0.03F;
        }
        for (int i = 0; i < EGGS; i++) {
            float ph = i * 1.37F;
            // the jelly breathes, each egg on its own
            float k = 1.0F + Mth.sin(age * 0.07F + ph) * 0.03F;
            this.jelly[i].xScale = k;
            this.jelly[i].yScale = 2.0F - k;
            this.jelly[i].zScale = k;
            // the little one inside turns slowly, and twitches as it grows
            float twitch = p > 0.4F ? Anim.envelope(((age + i * 23.0F) % 50.0F) / 20.0F, 0.0F, 0.05F, 0.05F, 0.2F) * (p - 0.4F) * 2.0F : 0.0F;
            this.eggs[i].yRot = age * 0.01F * (i % 2 == 0 ? 1.0F : -1.0F) + twitch * 0.8F;
            this.eggs[i].x += Mth.sin(age * 0.04F + ph) * 0.15F;
            this.eggs[i].z += Mth.cos(age * 0.035F + ph) * 0.15F;
        }
        if (s.hasRedOverlay) {
            this.raft.yScale = 0.85F;
        }
    }
}
