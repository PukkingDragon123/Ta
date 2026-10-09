package com.thesift.client.model;

import com.thesift.client.renderer.state.CaravanLarvaRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * CR2: a Caravan larva (geometry in tools/caravans.py): a grub of five shelled segments.
 *
 * <ul>
 *   <li>crawl: a wave runs back along its body - each segment lifts, reaches and sets down after
 *   the one before it, the body wriggling side to side - and its little legs paddle</li>
 *   <li>idle: it breathes in slow ripples, the head sways and the mandibles twitch</li>
 *   <li>bite: the head rears up with the mandibles spread, then strikes down and snaps them shut</li>
 *   <li>emerge: it bursts out of the rock with a stretch and a frantic wriggle</li>
 *   <li>hurt and dying: it curls up (CAVE: eased in and out, it used to snap shut with the red flash)</li>
 * </ul>
 * CAVE: the head and the segments hang from a 'body' root, so looking about and rearing to bite turn the
 * head alone (the whole grub used to swing round with every glance), and the crawl squashes and stretches
 * each segment as the wave passes through it.
 */
public class CaravanLarvaModel extends EntityModel<CaravanLarvaRenderState> {
    private static final int SEGMENTS = 4;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart[] mandibles = new ModelPart[2];
    private final ModelPart[] segments = new ModelPart[SEGMENTS];

    public CaravanLarvaModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.mandibles[0] = this.head.getChild("left_mandible");
        this.mandibles[1] = this.head.getChild("right_mandible");
        ModelPart prev = this.body;
        for (int i = 0; i < SEGMENTS; i++) {
            this.segments[i] = prev.getChild("segment_" + (i + 1));
            prev = this.segments[i];
        }
    }

    @Override
    public void setupAnim(CaravanLarvaRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float crawl = Math.min(1.0F, s.walkAnimationSpeed * 2.5F);
        float pos = s.walkAnimationPos * 1.8F;

        // --- the crawl wave and the idle ripple run back along the body, each segment a beat behind
        for (int i = 0; i < SEGMENTS; i++) {
            float lag = (i + 1) * 0.9F;
            float wave = Mth.sin(pos - lag);
            float ripple = Mth.sin(age * 0.12F - lag);
            this.segments[i].yRot = wave * 0.22F * crawl + ripple * 0.05F * (1.0F - crawl);
            this.segments[i].xRot = Mth.cos(pos - lag) * 0.16F * crawl + ripple * 0.03F;
            this.segments[i].y -= Math.max(0.0F, wave) * 0.35F * crawl;
            // the wave squashes each segment as it lifts and stretches it as it sets down
            float squash = Math.max(0.0F, wave) * 0.12F * crawl + ripple * 0.03F;
            this.segments[i].yScale = 1.0F - squash;
            this.segments[i].xScale = 1.0F + squash * 0.6F;
            this.segments[i].zScale = 1.0F + Math.max(0.0F, -wave) * 0.1F * crawl;
        }
        this.body.y -= Math.max(0.0F, Mth.sin(pos)) * 0.2F * crawl;
        this.head.yRot = s.yRot * Anim.DEG * 0.5F + Mth.sin(pos) * 0.15F * crawl + Mth.sin(age * 0.07F) * 0.1F * (1.0F - crawl);
        this.head.xRot = s.xRot * Anim.DEG * 0.4F + Mth.cos(pos) * 0.08F * crawl;
        this.head.y -= Math.max(0.0F, Mth.sin(pos)) * 0.3F * crawl;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.mandibles[k].yRot -= (Mth.sin(age * 0.3F + k) * 0.15F + 0.05F) * sx;
        }

        // --- bite: rear up with the mandibles spread, strike down, snap
        float bite = Anim.seconds(s.bite, s.ageInTicks);
        if (bite >= 0.0F && bite < 0.5F) {
            float rear = Anim.envelope(bite, 0.0F, 0.1F, 0.02F, 0.06F);
            float strike = Anim.envelope(bite, 0.1F, 0.06F, 0.06F, 0.25F);
            this.head.xRot += -rear * 0.7F + strike * 0.35F;
            this.head.y -= rear * 1.0F;
            this.segments[0].xRot -= rear * 0.12F - strike * 0.06F; // CAVE: the head rears alone now
            for (int k = 0; k < 2; k++) {
                float sx = k == 0 ? 1.0F : -1.0F;
                this.mandibles[k].yRot -= (rear * 0.6F - strike * 0.5F) * sx;
            }
        }

        // --- emerge: out of the rock with a stretch and a frantic wriggle
        float emerge = Anim.seconds(s.emerge, s.ageInTicks);
        if (emerge >= 0.0F && emerge < 1.0F) {
            float pop = Anim.envelope(emerge, 0.0F, 0.12F, 0.15F, 0.4F);
            float wriggle = Mth.sin(emerge * 32.0F) * Anim.envelope(emerge, 0.05F, 0.1F, 0.4F, 0.3F);
            this.head.xRot -= pop * 0.6F;
            for (int i = 0; i < SEGMENTS; i++) {
                this.segments[i].yRot += wriggle * 0.35F * (i % 2 == 0 ? 1.0F : -1.0F);
                this.segments[i].xRot += pop * 0.25F;
            }
        }

        // --- hurt and dying: it curls up, easing in and out of it
        float curl = s.hurtTicks >= 0.0F && s.dying <= 0.0F ? Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI) * 0.6F : 0.0F;
        if (s.dying > 0.0F) {
            curl = Math.max(curl, Anim.smooth(s.dying / 8.0F));
        }
        if (curl > 0.0F) {
            this.head.xRot += curl * 0.5F;
            for (int i = 0; i < SEGMENTS; i++) {
                this.segments[i].xRot -= curl * 0.55F;
            }
        }
    }
}
