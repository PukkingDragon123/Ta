package com.thesift.client.model;

import com.thesift.client.renderer.EchoerDrillRenderer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * RR: the Echoer Drill's gun (geometry from tools/echoer_drill.py). The bit spins (slowly while it listens, hard while
 * it drills), the whole gun kicks back on every bite and eases forward again, the listening horn twitches at each
 * beat it hears, and the music drum on the side turns a notch per beat.
 */
public class EchoerDrillModel extends Model<EchoerDrillRenderer.State> {
    private final ModelPart gun;
    private final ModelPart drill;
    private final ModelPart ear;
    private final ModelPart dial;

    public EchoerDrillModel(ModelPart root) {
        super(root, RenderTypes::entityCutout);
        this.gun = root.getChild("gun");
        this.drill = this.gun.getChild("drill");
        this.ear = root.getChild("ear");
        this.dial = root.getChild("dial");
    }

    @Override
    public void setupAnim(EchoerDrillRenderer.State s) {
        super.setupAnim(s);
        // the kick: sharp back, then it settles forward (z+ is back, towards the breech)
        float kick = s.recoil * s.recoil;
        this.gun.z += kick * 2.4F;
        this.drill.zRot = s.spin;
        // a working drill shivers
        if (s.drilling) {
            this.gun.x += Mth.sin(s.time * 2.7F) * 0.08F;
            this.gun.y += Mth.cos(s.time * 3.1F) * 0.08F;
        }
        this.ear.xRot += -s.ear * 0.45F + Mth.sin(s.time * 0.05F) * 0.03F;
        this.ear.zRot += s.ear * 0.18F * Mth.sin(s.time * 1.3F);
        this.dial.xRot = s.dial;
    }
}
