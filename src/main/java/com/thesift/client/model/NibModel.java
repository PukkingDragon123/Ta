package com.thesift.client.model;

import com.thesift.client.renderer.state.NibRenderState;
import com.thesift.entity.Nib;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.util.Mth;

/**
 * Nib: wings beat fast in flight (the lower pair a beat behind the upper), the body bobbing on
 * every stroke and tilting into turns; at rest on a flower the wings fold up over the back and
 * open again, slowly, now and then; swirling into treasure they blur.
 */
public class NibModel extends EntityModel<NibRenderState> {
    private final ModelPart body;
    private final ModelPart leftWing;
    private final ModelPart rightWing;
    private final ModelPart leftWingLow;
    private final ModelPart rightWingLow;
    private final ModelPart leftAntenna;
    private final ModelPart rightAntenna;

    public NibModel(ModelPart root) {
        super(root, RenderTypes::entityTranslucent);
        this.body = root.getChild("body");
        this.leftWing = this.body.getChild("left_wing");
        this.rightWing = this.body.getChild("right_wing");
        this.leftWingLow = this.body.getChild("left_wing_low");
        this.rightWingLow = this.body.getChild("right_wing_low");
        this.leftAntenna = this.body.getChild("left_antenna");
        this.rightAntenna = this.body.getChild("right_antenna");
    }

    @Override
    public void setupAnim(NibRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.phase * 10.0F;
        float flap;
        if (s.state == Nib.RESTING) {
            // folded up over the back, opening slowly every few seconds
            float open = Anim.envelope((age % 90.0F) / 20.0F, 2.5F, 0.6F, 0.4F, 0.8F);
            flap = -1.25F + open * 1.0F;
            this.body.y += 0.6F;
        } else {
            float speed = s.state == Nib.SWIRLING ? 2.6F : 1.7F;
            flap = Mth.sin(age * speed) * 0.95F - 0.2F;
            this.body.y += Mth.cos(age * speed) * 0.5F;
            this.body.xRot -= 0.25F;
        }
        float low = s.state == Nib.RESTING ? flap : Mth.sin(age * 1.7F - 0.7F) * 0.8F - 0.1F;
        this.leftWing.zRot += flap;
        this.rightWing.zRot -= flap;
        this.leftWingLow.zRot += low;
        this.rightWingLow.zRot -= low;
        float twitch = Mth.sin(age * 0.3F) * 0.12F;
        this.leftAntenna.xRot += twitch;
        this.rightAntenna.xRot -= twitch;
    }
}
