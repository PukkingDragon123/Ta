package com.thesift.client.model;

import com.thesift.client.renderer.state.SiftFishRenderState;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;

/**
 * S2: the Gobbler (geometry and hand-painted texture in tools/waterfolk.py), a huge blind catfish. It
 * swims with a slow, heavy wave down the body, tail and fluke; its jaw breathes a little open (wider
 * when it hunts), the gill flaps pump, the barbels stream back as it speeds up and drift when it
 * idles, and the sensor spines on its back sweep back with speed and shiver like the Warden's while
 * it listens. Lulled, it drifts with everything folded and slow. The lunge: a 0.6 s wind-up (jaw wide,
 * head back, drawing back) then a snap forward and a slow settle; the gulp bulges its head with the
 * jaw clamped while the gills pump; the spit throws the jaw open. Stranded, it lies on its side
 * gasping; dying, it rolls belly-up.
 */
public class GobblerModel extends EntityModel<SiftFishRenderState> {
    private static final int SPINES = 4;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart tail;
    private final ModelPart tail2;
    private final ModelPart fluke;
    private final ModelPart dorsal;
    private final ModelPart[] spines = new ModelPart[SPINES];
    private final ModelPart[] barbels = new ModelPart[2];
    private final ModelPart[] barbelTips = new ModelPart[2];
    private final ModelPart[] chinBarbels = new ModelPart[2];
    private final ModelPart[] tendrils = new ModelPart[2];
    private final ModelPart[] gills = new ModelPart[2];
    private final ModelPart[] fins = new ModelPart[2];

    public GobblerModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = this.body.getChild("head");
        this.jaw = this.head.getChild("jaw");
        this.tail = this.body.getChild("tail");
        this.tail2 = this.tail.getChild("tail2");
        this.fluke = this.tail2.getChild("fluke");
        this.dorsal = this.tail.getChild("dorsal");
        for (int i = 0; i < SPINES; i++) {
            this.spines[i] = this.body.getChild("spine_" + i);
        }
        String[] sides = {"left", "right"};
        for (int k = 0; k < 2; k++) {
            String side = sides[k];
            this.barbels[k] = this.head.getChild(side + "_barbel");
            this.barbelTips[k] = this.barbels[k].getChild(side + "_barbel_tip");
            this.chinBarbels[k] = this.jaw.getChild(side + "_chin_barbel");
            this.tendrils[k] = this.head.getChild(side + "_tendril");
            this.gills[k] = this.body.getChild(side + "_gill");
            this.fins[k] = this.body.getChild(side + "_fin");
        }
    }

    @Override
    public void setupAnim(SiftFishRenderState s) {
        super.setupAnim(s);
        float age = s.ageInTicks + s.seed;
        float e = s.inLiquid ? (s.calm ? s.effort * 0.4F : s.effort) : 1.0F;
        float hunt = s.hunting && !s.calm ? 1.0F : 0.0F;
        float calm = s.calm ? 1.0F : 0.0F;
        float t = age * (0.12F + 0.18F * e);
        float amp = 0.1F + 0.04F * (1.0F - calm) + 0.22F * e;
        // the heavy wave down the body, the tail and the fluke
        this.body.xRot = s.xRot * Anim.DEG;
        this.body.yRot = -Mth.sin(t) * 0.04F * (1.0F + e);
        this.body.zRot = Mth.sin(age * 0.05F) * 0.05F * calm;
        this.tail.yRot = Mth.sin(t - 0.8F) * amp * 0.6F;
        this.tail2.yRot = Mth.sin(t - 1.6F) * amp * 0.9F;
        this.fluke.yRot = Mth.sin(t - 2.4F) * amp * 1.2F;
        this.dorsal.zRot = Mth.sin(t - 1.2F) * 0.1F;
        // breathing: the jaw a little open (wider on the hunt), the gill flaps pump
        float breath = Mth.sin(age * 0.08F);
        this.jaw.xRot = 0.06F + breath * 0.05F + 0.15F * hunt - 0.04F * calm;
        float flare = Math.max(0.0F, breath) * 0.2F + 0.2F * hunt;
        for (int k = 0; k < 2; k++) {
            float sx = k == 0 ? 1.0F : -1.0F;
            this.gills[k].yRot += sx * flare;
            this.fins[k].zRot += sx * Mth.sin(age * 0.15F + k) * 0.2F;
            this.fins[k].yRot += sx * 0.3F * e;
            // the barbels stream back with speed and drift when it idles; the tips lag behind
            this.barbels[k].xRot += 0.35F * e + Mth.sin(age * 0.06F + k) * 0.08F;
            this.barbels[k].yRot += sx * (Mth.sin(age * 0.07F + k * 1.3F) * 0.15F - 0.25F * e);
            this.barbelTips[k].xRot += Mth.sin(age * 0.07F + k - 0.8F) * 0.2F + 0.2F * e;
            this.chinBarbels[k].xRot += Mth.sin(age * 0.1F + k) * 0.15F + 0.3F * e;
            // the sensor tendrils: they shiver while it listens on the hunt, and droop when it is lulled
            this.tendrils[k].zRot += sx * (Mth.sin(age * 0.09F + k) * 0.08F + Mth.sin(s.ageInTicks * 1.7F + k) * 0.15F * hunt + 0.45F * calm);
        }
        for (int i = 0; i < SPINES; i++) {
            this.spines[i].xRot += Mth.sin(age * 0.09F - i * 0.7F) * 0.1F + 0.4F * e + 0.3F * calm;
            this.spines[i].zRot += Mth.sin(s.ageInTicks * 1.9F + i) * 0.08F * hunt;
        }
        // the lunge (1.4 s): a 0.6 s wind-up, a snap forward, a slow settle
        float lunge = Anim.seconds(s.lunge, s.ageInTicks);
        if (lunge >= 0.0F && lunge < 1.4F) {
            float wind = Anim.smooth(lunge / 0.6F) * (1.0F - Anim.smooth((lunge - 0.6F) / 0.1F));
            float strike = Anim.envelope(lunge, 0.6F, 0.08F, 0.25F, 0.45F);
            this.jaw.xRot = Math.max(this.jaw.xRot, 1.0F * wind + 0.85F * strike * (1.0F - Anim.smooth((lunge - 0.85F) / 0.15F)));
            this.head.xRot -= 0.25F * wind;
            this.body.z += 2.5F * wind - 5.0F * strike;
            this.tail.yRot *= 1.0F - wind;
            for (int k = 0; k < 2; k++) {
                this.gills[k].yRot += (k == 0 ? 1.0F : -1.0F) * 0.4F * wind;
            }
        }
        // the gulp (2.5 s): the head bulges, the jaw clamps shut, the gills pump the water out
        float gulp = Anim.seconds(s.gulp, s.ageInTicks);
        if (gulp >= 0.0F && gulp < 2.5F) {
            float bulge = Anim.envelope(gulp, 0.0F, 0.15F, 1.9F, 0.4F);
            this.head.xScale = 1.0F + 0.12F * bulge;
            this.head.yScale = 1.0F + 0.06F * bulge;
            this.jaw.xRot *= 1.0F - bulge;
            for (int k = 0; k < 2; k++) {
                this.gills[k].yRot += (k == 0 ? 1.0F : -1.0F) * Mth.sin(gulp * 12.0F) * 0.25F * bulge;
            }
        }
        // the spit (0.8 s): the jaw flies open and the head jerks back
        float spit = Anim.seconds(s.spit, s.ageInTicks);
        if (spit >= 0.0F && spit < 0.8F) {
            float open = Anim.envelope(spit, 0.0F, 0.06F, 0.12F, 0.5F);
            this.jaw.xRot = Math.max(this.jaw.xRot, 1.1F * open);
            this.head.xRot -= 0.3F * open;
            this.body.z += 2.0F * open;
        }
        // stranded: on its side, gasping, the tail thrashing
        if (!s.inLiquid) {
            this.body.zRot = Mth.HALF_PI;
            this.body.y += 3.0F;
            this.jaw.xRot = 0.15F + Math.max(0.0F, Mth.sin(s.ageInTicks * 0.3F)) * 0.4F;
            this.tail.yRot = Mth.sin(s.ageInTicks * 0.5F) * 0.3F;
            this.tail2.yRot = Mth.sin(s.ageInTicks * 0.5F - 0.8F) * 0.4F;
        }
        if (s.hurtTicks >= 0.0F && s.dying <= 0.0F) {
            float k = Mth.sin(Mth.clamp(s.hurtTicks / 10.0F, 0.0F, 1.0F) * Mth.PI);
            this.head.xRot -= 0.15F * k;
            this.jaw.xRot += 0.3F * k;
        }
        float die = Anim.smooth(s.dying / 12.0F);
        if (die > 0.0F) {
            this.body.zRot = Mth.PI * die;
        }
    }
}
