package com.thesift.client.model;

import com.thesift.client.renderer.state.ReservoirRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * Reservoir. A living column that sways like a worm standing on its tail; its segments swell with the Chrome it
 * holds and a pulse runs up them, base to mouth, quicker the fuller it is. Creeping along, each segment squashes in
 * turn like an inchworm. Its six petals open by day and fold shut over the mouth at night or when it is hurt.
 * Bursting, the segments heave, then collapse to empty while the petals fling wide; drinking or drained, the
 * mouth bobs and the petals clap.
 */
public class ReservoirModel extends EntityModel<ReservoirRenderState> {
    private final ModelPart[] segments = new ModelPart[4];
    private final ModelPart mouth;
    private final ModelPart[] petals = new ModelPart[6];
    private final ModelPart[] roots = new ModelPart[6];

    public ReservoirModel(ModelPart root) {
        super(root);
        ModelPart base = root.getChild("base");
        ModelPart parent = base;
        for (int i = 0; i < 4; i++) {
            this.segments[i] = parent.getChild("segment_" + i);
            parent = this.segments[i];
        }
        this.mouth = parent.getChild("mouth");
        for (int i = 0; i < 6; i++) {
            this.petals[i] = this.mouth.getChild("petal_" + i);
            this.roots[i] = base.getChild("root_" + i);
        }
    }

    @Override
    public void setupAnim(ReservoirRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float fill = s.fill;
        float walk = Math.min(1.0F, s.walkAnimationSpeed * 3.0F);
        float pos = s.walkAnimationPos * 1.4F;

        // the burst: a heave, then everything collapses empty
        float b = Anim.seconds(s.burst, s.ageInTicks);
        float heave = b >= 0.0F && b < 1.4F ? Anim.envelope(b, 0.0F, 0.08F, 0.05F, 0.1F) : 0.0F;
        float collapse = b >= 0.0F && b < 1.4F ? Anim.envelope(b, 0.15F, 0.12F, 0.5F, 0.6F) : 0.0F;
        float g = Anim.seconds(s.gulp, s.ageInTicks);
        float gulp = g >= 0.0F && g < 0.8F ? Anim.envelope(g, 0.0F, 0.1F, 0.1F, 0.4F) : 0.0F;

        // each segment's absolute girth (it swells with Chrome; a pulse runs up it), applied relative to its parent
        float speed = 0.06F + 0.12F * fill;
        float prev = 1.0F;
        float prevY = 1.0F;
        for (int i = 0; i < 4; i++) {
            float pulse = Mth.sin(age * speed * 2.0F - i * 0.9F) * (0.02F + 0.05F * fill);
            float girth = 0.92F + 0.22F * fill + pulse + 0.18F * heave - 0.12F * collapse;
            float tall = 1.0F - 0.25F * (girth - 1.0F) - walk * 0.1F * Mth.sin(pos - i * 1.2F) + 0.04F * gulp * (i == 3 ? 1.0F : 0.0F);
            this.segments[i].xScale = girth / prev;
            this.segments[i].zScale = girth / prev;
            this.segments[i].yScale = tall / prevY;
            prev = girth;
            prevY = tall;
            // the sway of a worm standing on its tail
            this.segments[i].zRot += Mth.sin(age * 0.03F + i * 0.8F) * 0.025F + Mth.sin(pos * 0.5F - i) * 0.05F * walk
                    + Mth.sin(age * 1.7F) * 0.04F * heave;
            this.segments[i].xRot += Mth.cos(age * 0.027F + i * 0.7F) * 0.02F;
        }
        // the mouth keeps its own size
        this.mouth.xScale = 1.0F / prev;
        this.mouth.zScale = 1.0F / prev;
        this.mouth.yScale = 1.0F / prevY;
        this.mouth.y -= 0.6F * gulp;

        // petals: open by day, shut over the mouth at night; flung wide by a burst, clapping when it drinks
        float bloom = Math.max(s.bloom, collapse);
        for (int i = 0; i < 6; i++) {
            float flutter = Mth.sin(age * 0.09F + i * 1.1F) * 0.04F;
            this.petals[i].xRot += -0.7F * (1.0F - bloom) + 0.45F * bloom + flutter + 0.6F * collapse - 0.5F * gulp;
            this.petals[i].zRot += Mth.sin(age * 0.05F + i) * 0.03F * bloom;
        }
        // root-feet grip and shuffle as it creeps
        for (int i = 0; i < 6; i++) {
            this.roots[i].xRot += Mth.sin(pos + i) * 0.2F * walk;
        }
    }
}
