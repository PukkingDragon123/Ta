package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR3 Fish &amp; Coral Organs - the Sculk Fish (a hand-drawn sprite extruded into a deep body, built at twice
 * vanilla scale - see tools/fish_art.py): a quick, twitchy tail beat, the underbitten jaw that chatters while
 * the school hunts and gapes then snaps shut on every bite, sensor tendrils on its brow that flick toward
 * every sound, and glowing fins that flare when it attacks.
 */
public class SculkFishModel extends EntityModel<SiftFishRenderState> {
    private final ModelPart body;
    private final ModelPart jaw;
    private final ModelPart leftTendril;
    private final ModelPart rightTendril;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart dorsal;
    private final ModelPart analFin;
    private final ModelPart tailStem;
    private final ModelPart tail;

    public SculkFishModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.jaw = this.body.getChild("jaw");
        this.leftTendril = this.body.getChild("left_tendril");
        this.rightTendril = this.body.getChild("right_tendril");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.dorsal = this.body.getChild("dorsal");
        this.analFin = this.body.getChild("anal_fin");
        this.tailStem = this.body.getChild("tail_stem");
        this.tail = this.tailStem.getChild("tail");
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.effort;
        float hunt = s.aggressive ? 1.0F : 0.0F;
        // a quick, slightly jerky stroke: a sharp wave with a little extra snap at the tail
        float beat = age * (0.55F + e * 1.0F + hunt * 0.25F);
        float amp = 0.28F + e * 0.4F;
        this.body.yRot = -Mth.sin(beat) * 0.08F * (1.0F + e);
        this.tailStem.yRot = Mth.sin(beat - 0.7F) * amp * 0.6F;
        this.tail.yRot = Mth.sin(beat - 1.4F) * amp + Mth.sin(beat * 2.0F) * 0.05F * hunt;
        this.body.xRot = s.xRot * Anim.DEG;
        this.body.zRot = Mth.sin(beat * 0.5F) * 0.06F * e;
        // fins: sculling while it drifts, flared stiff and wide when it attacks
        float scull = Mth.sin(age * (0.6F + e * 0.5F));
        this.leftFin.yRot += scull * 0.3F * (1.0F - hunt * 0.5F) + hunt * 0.35F;
        this.rightFin.yRot -= scull * 0.3F * (1.0F - hunt * 0.5F) + hunt * 0.35F;
        this.dorsal.xRot = -0.1F * hunt + Mth.sin(beat * 0.5F) * 0.06F;
        this.dorsal.yScale = 1.0F + 0.25F * hunt;
        this.analFin.zRot = Mth.sin(beat - 1.0F) * 0.12F;
        // the jaw: hanging a little open, chattering while the school hunts
        float jawOpen = 0.12F + hunt * (0.2F + 0.15F * Math.max(0.0F, Mth.sin(age * 1.7F)));
        float bite = Anim.seconds(s.snap, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.45F) {
            // gape, then snap shut and shudder
            float gape = Anim.envelope(bite, 0.0F, 0.08F, 0.04F, 0.08F);
            jawOpen = 0.9F * gape;
            this.body.z -= 2.0F * gape;
            this.body.yRot += Mth.sin(bite * 60.0F) * 0.12F * (1.0F - Anim.smooth(bite / 0.45F));
        }
        this.jaw.xRot = jawOpen;
        // sensor tendrils flick toward sounds, twitching like a sculk sensor's
        float twitch = Mth.sin(age * 0.9F) * Mth.sin(age * 0.23F);
        this.leftTendril.zRot += twitch * 0.25F + hunt * 0.3F;
        this.rightTendril.zRot -= Mth.sin(age * 0.8F + 1.3F) * Mth.sin(age * 0.19F) * 0.25F + hunt * 0.3F;
        this.leftTendril.xRot += hunt * -0.35F;
        this.rightTendril.xRot += hunt * -0.35F;
        if (!s.inLiquid) {
            // stranded: on its side, snapping and flopping
            this.body.zRot = (float) Math.PI * 0.5F;
            this.body.y += 3.0F;
            this.tailStem.yRot = Mth.sin(s.ageInTicks * 1.3F) * 0.5F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 1.3F - 0.8F) * 0.8F;
            this.jaw.xRot = 0.3F + Math.max(0.0F, Mth.sin(s.ageInTicks * 0.9F)) * 0.5F;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = (float) Math.PI * die;
            this.jaw.xRot = 0.6F * die;
        }
    }
}
