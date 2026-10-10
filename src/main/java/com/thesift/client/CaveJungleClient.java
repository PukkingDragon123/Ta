package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ColossusPonderModel;
import com.thesift.client.model.CrocotodoModel;
import com.thesift.client.model.CruncherModel;
import com.thesift.client.model.GlowFlyModel;
import com.thesift.client.model.MantisModel;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.model.PonderTadpoleModel;
import com.thesift.client.renderer.JungleMobRenderer;
import com.thesift.entity.jungle.ColossusPonder;
import com.thesift.entity.jungle.Crocotodo;
import com.thesift.entity.jungle.Cruncher;
import com.thesift.entity.jungle.GlowFly;
import com.thesift.entity.jungle.Mantis;
import com.thesift.entity.jungle.PonderTadpole;
import com.thesift.registry.ModCaveJungle;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * P4 Cave Jungle, client side: the six creatures' model layers (geometry from tools/jungle_mobs.py) and their
 * renderers - one {@link JungleMobRenderer} each, told what its model animates.
 */
public final class CaveJungleClient {
    public static final ModelLayerLocation GLOW_FLY = layer("glow_fly");
    public static final ModelLayerLocation CROCOTODO = layer("crocotodo");
    public static final ModelLayerLocation MANTIS = layer("mantis");
    public static final ModelLayerLocation COLOSSUS_PONDER = layer("colossus_ponder");
    public static final ModelLayerLocation PONDER_TADPOLE = layer("ponder_tadpole");
    public static final ModelLayerLocation CRUNCHER = layer("cruncher");

    private CaveJungleClient() {
    }

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(TheSift.id(name), "main");
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CaveJungleClient::registerLayers);
        modBus.addListener(CaveJungleClient::registerRenderers);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(GLOW_FLY, ModelGeometry::glow_fly);
        event.registerLayerDefinition(CROCOTODO, ModelGeometry::crocotodo);
        event.registerLayerDefinition(MANTIS, ModelGeometry::mantis);
        event.registerLayerDefinition(COLOSSUS_PONDER, ModelGeometry::colossus_ponder);
        event.registerLayerDefinition(PONDER_TADPOLE, ModelGeometry::ponder_tadpole);
        event.registerLayerDefinition(CRUNCHER, ModelGeometry::cruncher);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        // the Glow Fly: its lantern shines with the light it holds; valueA = charge, flagA = fleeing
        event.registerEntityRenderer(ModCaveJungle.GLOW_FLY.get(), ctx -> new JungleMobRenderer<GlowFly>(ctx,
                new GlowFlyModel(ctx.bakeLayer(GLOW_FLY)), "glow_fly", true, 0.3F, 1.0F, 1.0F, (e, s, pt) -> {
                    s.glow = e.glow(pt);
                    s.valueA = e.getCharge() / (float) GlowFly.MAX_CHARGE;
                    s.flagA = e.isFleeing();
                    s.animA.copyFrom(e.absorbAnimation);
                    s.animB.copyFrom(e.giveAnimation);
                    s.animC.copyFrom(e.flashAnimation);
                }));
        event.registerEntityRenderer(ModCaveJungle.CROCOTODO.get(), ctx -> new JungleMobRenderer<Crocotodo>(ctx,
                new CrocotodoModel(ctx.bakeLayer(CROCOTODO)), "crocotodo", false, 0.4F, 1.0F, 0.8F, (e, s, pt) -> {
                    s.animA.copyFrom(e.peckAnimation);
                    s.animB.copyFrom(e.snapAnimation);
                    s.animC.copyFrom(e.flapAnimation);
                }));
        // the Mantis, a little larger than its model: flagA = diving, flagB = holding its prey; its crystals glow
        event.registerEntityRenderer(ModCaveJungle.MANTIS.get(), ctx -> new JungleMobRenderer<Mantis>(ctx,
                new MantisModel(ctx.bakeLayer(MANTIS)), "mantis", true, 0.5F, 1.15F, 0.35F, (e, s, pt) -> {
                    s.glow = 0.75F + 0.25F * net.minecraft.util.Mth.sin((e.tickCount + pt) * 0.1F);
                    s.flagA = e.isDiving();
                    s.flagB = e.isGrabbing();
                    s.animA.copyFrom(e.strikeAnimation);
                    s.animB.copyFrom(e.eatAnimation);
                    s.animC.copyFrom(e.sliceAnimation);
                }));
        // the Colossus Ponder, a giant: its model is drawn half as large again; flagA = rampaging; its moss glows
        event.registerEntityRenderer(ModCaveJungle.COLOSSUS_PONDER.get(), ctx -> new JungleMobRenderer<ColossusPonder>(ctx,
                new ColossusPonderModel(ctx.bakeLayer(COLOSSUS_PONDER)), "colossus_ponder", true, 1.0F, 1.5F, 0.25F, (e, s, pt) -> {
                    s.glow = 0.8F + 0.2F * net.minecraft.util.Mth.sin((e.tickCount + pt) * 0.05F);
                    s.flagA = e.isRampaging();
                    s.animA.copyFrom(e.croakAnimation);
                    s.animB.copyFrom(e.stompAnimation);
                    s.animC.copyFrom(e.layAnimation);
                    if (e.roarAnimation.isStarted()) {
                        s.animD.copyFrom(e.roarAnimation);
                    } else {
                        s.animD.copyFrom(e.slamAnimation);
                    }
                }));
        event.registerEntityRenderer(ModCaveJungle.PONDER_TADPOLE.get(), ctx -> new JungleMobRenderer<PonderTadpole>(ctx,
                new PonderTadpoleModel(ctx.bakeLayer(PONDER_TADPOLE)), "ponder_tadpole", true, 0.25F, 1.0F, 1.0F, (e, s, pt) -> {
                    s.animA.copyFrom(e.biteAnimation);
                    s.animB.copyFrom(e.burrowAnimation);
                }));
        // the Cruncher: its glittering Magnesium dust and its eyes glow faintly
        event.registerEntityRenderer(ModCaveJungle.CRUNCHER.get(), ctx -> new JungleMobRenderer<Cruncher>(ctx,
                new CruncherModel(ctx.bakeLayer(CRUNCHER)), "cruncher", true, 0.35F, 1.0F, 0.7F, (e, s, pt) -> {
                    s.glow = 0.6F;
                    s.animA.copyFrom(e.biteAnimation);
                    s.animB.copyFrom(e.crunchAnimation);
                    s.animC.copyFrom(e.wasteAnimation);
                }));
    }
}
