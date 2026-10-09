package com.thesift.client.knowledge;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.item.MusicSheetItem;
import com.thesift.knowledge.Knowledge;
import com.thesift.knowledge.LoreReading;
import com.thesift.registry.ModKnowledge;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientTooltipComponentFactoriesEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * F3 Knowledge &amp; lore, client side: the Mini Creator's model and renderer, the Music Sheet artwork in tooltips, the
 * reading screen for Lore Books and Scrolls, and the "Knowledge Book: ..." line when an item you pick up has a page.
 */
public final class KnowledgeClient {
    public static final ModelLayerLocation MINI_CREATOR = new ModelLayerLocation(TheSift.id("mini_creator"), "main");
    private static List<String> lastKnown = List.of();
    private static int lastSize = -1;

    private KnowledgeClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(KnowledgeClient::layers);
        modBus.addListener(KnowledgeClient::renderers);
        modBus.addListener(KnowledgeClient::tooltips);
        NeoForge.EVENT_BUS.addListener(KnowledgeClient::onClientTick);
        LoreReading.clientOpen = lore -> Minecraft.getInstance().gui.setScreen(new LoreScreen(lore));
        LoreReading.clientKnows = key -> {
            LocalPlayer p = Minecraft.getInstance().player;
            return p != null && Knowledge.has(p, key);
        };
    }

    private static void layers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(MINI_CREATOR, ModelGeometry::mini_creator);
    }

    private static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModKnowledge.MINI_CREATOR.get(), MiniCreatorRenderer::new);
    }

    private static void tooltips(RegisterClientTooltipComponentFactoriesEvent event) {
        event.register(MusicSheetItem.SheetArt.class, SheetTooltip::new);
    }

    /** Items are recorded quietly by the server; the ones with a page of their own are announced here. */
    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            lastSize = -1;
            return;
        }
        List<String> now = Knowledge.all(p);
        if (now == lastKnown) {
            return;
        }
        // (a big jump is the login sync, not something just picked up)
        if (lastSize >= 0 && now.size() > lastSize && now.size() - lastSize <= 3) {
            for (int i = lastSize; i < now.size(); i++) {
                String key = now.get(i);
                if (key.startsWith("item:")) {
                    Component page = com.thesift.client.codex.CodexOpener.pageFor(key);
                    if (page != null) {
                        p.sendOverlayMessage(Component.translatable("knowledge.thesift.unlocked", page));
                        p.playSound(ModKnowledge.UNLOCK.get(), 0.5F, 1.1F);
                    }
                }
            }
        }
        lastKnown = now;
        lastSize = now.size();
    }
}
