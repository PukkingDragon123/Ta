package com.thesift.client.model;

import com.thesift.block.entity.EurophyTableBlockEntity;
import com.thesift.client.renderer.EurophyTableRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * The Europhy Table's clockwork (geometry from tools/materials.py): the ring gear turns, the pinions spin the
 * other way, the crown of arms revolves and lifts its pans as the table charges, and the Prism lens bobs,
 * spins and swells on every note.
 */
public class EurophyTableModel extends Model<EurophyTableRenderer.State> {
    private final ModelPart ring;
    private final ModelPart pinion0;
    private final ModelPart pinion1;
    private final ModelPart crown;
    private final ModelPart lens;
    private final ModelPart lensCore;
    private final ModelPart[] lifts = new ModelPart[EurophyTableBlockEntity.INPUTS];

    public EurophyTableModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.ring = root.getChild("ring");
        this.pinion0 = root.getChild("pinion_0");
        this.pinion1 = root.getChild("pinion_1");
        this.crown = root.getChild("crown");
        this.lens = root.getChild("lens");
        this.lensCore = this.lens.getChild("lens_core");
        for (int k = 0; k < this.lifts.length; k++) {
            this.lifts[k] = this.crown.getChild("arm_" + k).getChild("lift_" + k);
        }
    }

    @Override
    public void setupAnim(EurophyTableRenderer.State s) {
        super.setupAnim(s);
        this.ring.yRot = s.spin;
        this.pinion0.yRot = -s.spin * 4.4F;
        this.pinion1.yRot = -s.spin * 4.4F;
        this.crown.yRot = s.crown;
        for (int k = 0; k < this.lifts.length; k++) {
            // each arm lifts its pan toward the centre; a note makes them all twitch
            float twitch = s.pulse * 0.12F * Mth.sin(s.time * 0.9F + k * 1.7F);
            this.lifts[k].xRot = -s.lift * EurophyTableBlockEntity.ARM_LIFT - twitch;
        }
        // the lens rises out of the way while the output forms, bobs, turns against the crown and swells on each note
        this.lens.y += -s.lift * 4.0F + Mth.sin(s.time * 0.08F) * 0.6F;
        this.lens.yRot = -s.crown * 1.6F + s.time * 0.01F;
        float swell = 1.0F + s.pulse * 0.1F + (s.forming ? 0.06F * Mth.sin(s.time * 0.5F) : 0.0F);
        this.lensCore.xScale = swell;
        this.lensCore.yScale = swell;
        this.lensCore.zScale = swell;
    }
}
