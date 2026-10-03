package com.thesift.client.music;

import com.google.common.reflect.TypeToken;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.thesift.TheSift;
import com.thesift.music.band.BandPayloads;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.renderstate.RegisterRenderStateModifiersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * The band, as the player sees it (agent M2):
 * <ul>
 *   <li>every band member grooves - a slow sway shared by the whole band, and on every note it plays a
 *   little crouch, a hop and a landing squash, scaled to its size (any creature, any renderer: the
 *   transform wraps the whole renderer);</li>
 *   <li>the band leader sees a small band panel in the top-right corner: each member with its
 *   instrument and a note that jumps when it plays, how long it will stay, and how much stronger the
 *   band makes their songs.</li>
 * </ul>
 */
public final class BandClient {
    private static final ContextKey<Groove> GROOVE = new ContextKey<>(TheSift.id("band_groove"));
    private static final ContextKey<Boolean> PUSHED = new ContextKey<>(TheSift.id("band_groove_pushed"));
    /** Ticks a member or the panel lingers without news from the server. */
    private static final int FORGET = 60;
    private static final int PANEL_TOP = 56;
    private static final int ROW = 12;
    private static final Map<Integer, Groove> MEMBERS = new HashMap<>();
    private static BandPayloads.@Nullable Sync hud;
    private static long hudSeen;
    private static @Nullable ClientLevel seenLevel;

    /** One band member this client knows about. */
    private static final class Groove {
        final int id;
        final long since;
        int leader;
        int colour;
        int index;
        float stay;
        long seen;
        long pulseAt = Long.MIN_VALUE / 2;

        Groove(int id, long since) {
            this.id = id;
            this.since = since;
        }
    }

    private BandClient() {
    }

    public static void register(IEventBus modBus) {
        BandPayloads.clientSync = BandClient::onSync;
        BandPayloads.clientPulse = BandClient::onPulse;
        modBus.addListener(BandClient::registerOverlay);
        modBus.addListener(BandClient::registerRenderState);
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, BandClient::onRenderPre);
        NeoForge.EVENT_BUS.addListener(BandClient::onRenderPost);
        NeoForge.EVENT_BUS.addListener(BandClient::onClientTick);
    }

    private static long now() {
        ClientLevel level = Minecraft.getInstance().level;
        return level == null ? 0L : level.getGameTime();
    }

    // ------------------------------------------------------------------ news from the server

    private static void onSync(BandPayloads.Sync p) {
        long now = now();
        int[] ids = p.members();
        MEMBERS.values().removeIf(g -> g.leader == p.leader() && !contains(ids, g.id));
        for (int i = 0; i < ids.length; i++) {
            Groove g = MEMBERS.computeIfAbsent(ids[i], id -> new Groove(id, now));
            g.leader = p.leader();
            g.colour = i < p.colours().length ? p.colours()[i] : 0xFFFFFF;
            g.stay = i < p.stay().length ? p.stay()[i] / 255.0F : 1.0F;
            g.index = i;
            g.seen = now;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player != null && p.leader() == mc.player.getId()) {
            hud = ids.length == 0 ? null : p;
            hudSeen = now;
        }
    }

    private static void onPulse(BandPayloads.Pulse p) {
        long now = now();
        for (int id : p.members()) {
            Groove g = MEMBERS.get(id);
            if (g != null) {
                g.pulseAt = now;
            }
        }
    }

    private static boolean contains(int[] ids, int id) {
        for (int i : ids) {
            if (i == id) {
                return true;
            }
        }
        return false;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        ClientLevel level = Minecraft.getInstance().level;
        if (level != seenLevel) {
            seenLevel = level;
            MEMBERS.clear();
            hud = null;
        }
        if (level == null || MEMBERS.isEmpty() && hud == null) {
            return;
        }
        long now = level.getGameTime();
        MEMBERS.values().removeIf(g -> now - g.seen > FORGET || now < g.seen);
        if (hud != null && (now - hudSeen > FORGET || now < hudSeen)) {
            hud = null;
        }
    }

    // ------------------------------------------------------------------ the groove

    private static void registerRenderState(RegisterRenderStateModifiersEvent event) {
        event.registerEntityModifier(new TypeToken<LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>>() {
        }, (LivingEntity entity, LivingEntityRenderState state) -> state.setRenderData(GROOVE, MEMBERS.isEmpty() ? null : MEMBERS.get(entity.getId())));
    }

    private static void onRenderPre(RenderLivingEvent.Pre<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        Groove g = state.getRenderData(GROOVE);
        ClientLevel level = Minecraft.getInstance().level;
        if (g == null || level == null) {
            return;
        }
        long time = level.getGameTime();
        float partial = event.getPartialTick();
        float size = Mth.clamp(state.boundingBoxHeight, 0.3F, 4.0F);
        // the hop on the beat: a crouch, a hop with a stretch, a landing squash
        float t = Math.min(1000.0F, (float) (time - g.pulseAt)) + partial;
        float hop = 0.0F;
        float squash = 0.0F;
        float amp = Mth.clamp(0.05F + size * 0.06F, 0.06F, 0.3F);
        if (t >= 0.0F && t < 1.5F) {
            float k = t / 1.5F;
            hop = -0.03F * k;
            squash = -0.1F * k;
        } else if (t >= 1.5F && t < 8.5F) {
            float k = (t - 1.5F) / 7.0F;
            hop = Mth.sin(k * Mth.PI) * amp - 0.03F * (1.0F - k);
            squash = k < 0.5F ? 0.08F * (1.0F - k * 2.0F) : 0.0F;
        } else if (t >= 8.5F && t < 12.0F) {
            float k = (t - 8.5F) / 3.5F;
            squash = -0.07F * Mth.sin(k * Mth.PI);
        }
        // the band's shared sway, two seconds a cycle, and a nod on the hop
        float swayDeg = 7.0F / (1.0F + size * 0.45F);
        float sway = Mth.sin(((time % 40L) + partial) * Mth.TWO_PI / 40.0F) * swayDeg;
        float nod = amp > 0.0F ? hop / amp * 6.0F : 0.0F;
        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        state.setRenderData(PUSHED, Boolean.TRUE);
        pose.translate(0.0F, hop, 0.0F);
        float facing = 180.0F - state.bodyRot;
        pose.rotateDegrees(Axis.YP, facing);
        pose.rotateDegrees(Axis.ZP, sway);
        pose.rotateDegrees(Axis.XP, nod);
        pose.rotateDegrees(Axis.YP, -facing);
        float sy = 1.0F + squash;
        float sxz = (float) (1.0 / Math.sqrt(sy));
        pose.scale(sxz, sy, sxz);
    }

    private static void onRenderPost(RenderLivingEvent.Post<?, ?, ?> event) {
        LivingEntityRenderState state = event.getRenderState();
        if (state.getRenderData(PUSHED) != null) {
            state.setRenderData(PUSHED, null);
            event.getPoseStack().popPose();
        }
    }

    // ------------------------------------------------------------------ the band panel

    private static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.EFFECTS, TheSift.id("band"), BandClient::drawHud);
    }

    private static void drawHud(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        BandPayloads.Sync band = hud;
        if (band == null || mc.player == null || mc.level == null || band.leader() != mc.player.getId()) {
            return;
        }
        Font font = mc.font;
        long time = mc.level.getGameTime();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int[] ids = band.members();
        String[] names = new String[ids.length];
        Component[] instruments = new Component[ids.length];
        int nameW = 0;
        int instW = 0;
        for (int i = 0; i < ids.length; i++) {
            Entity e = mc.level.getEntity(ids[i]);
            String name = e == null ? "?" : e.getDisplayName().getString();
            while (name.length() > 1 && font.width(name) > 84) {
                name = name.substring(0, name.length() - 1);
            }
            names[i] = name;
            BandVoice voice = e == null ? null : BandRegistry.voiceOf(e.getType());
            instruments[i] = voice == null ? Component.empty()
                    : Component.translatableWithFallback(voice.instrumentKey(), voice.instrumentId().replace('_', ' '));
            nameW = Math.max(nameW, font.width(name));
            instW = Math.max(instW, font.width(instruments[i]));
        }
        Component title = Component.translatable("band.thesift.hud.title");
        String count = ids.length + "/" + com.thesift.music.band.Bands.MAX_MEMBERS;
        String power = "×" + String.format(java.util.Locale.ROOT, "%.2f", band.power());
        int w = Math.max(14 + nameW + 8 + instW + 4, 10 + font.width(title) + 6 + font.width(count) + 10 + font.width(power) + 4);
        int h = 14 + ids.length * ROW + 2;
        int x0 = g.guiWidth() - w - 4;
        int y0 = PANEL_TOP;
        // a dusky panel with a brass rim
        g.fill(x0, y0, x0 + w, y0 + h, 0xA8140F1C);
        g.fill(x0, y0, x0 + w, y0 + 1, 0xFFE8C890);
        g.fill(x0, y0 + h - 1, x0 + w, y0 + h, 0x80E8C890);
        g.fill(x0, y0 + 1, x0 + 1, y0 + h - 1, 0x60E8C890);
        g.text(font, "♫", x0 + 3, y0 + 3, 0xFFE8C890, true);
        g.text(font, title, x0 + 12, y0 + 3, 0xFFF4E4C1, true);
        g.text(font, count, x0 + 12 + font.width(title) + 5, y0 + 3, 0xFF9C8C78, true);
        g.text(font, power, x0 + w - 3 - font.width(power), y0 + 3, 0xFFFFD27A, true);
        for (int i = 0; i < ids.length; i++) {
            Groove gr = MEMBERS.get(ids[i]);
            int colour = gr != null ? gr.colour : 0xFFFFFF;
            float age = gr == null ? 99.0F : Math.min(1000.0F, (float) (time - gr.since)) + partial;
            int slide = Math.round((1.0F - Mth.clamp(age / 8.0F, 0.0F, 1.0F)) * 24.0F);
            int y = y0 + 14 + i * ROW;
            int x = x0 + 4 + slide;
            // the note jumps when the member plays
            float t = gr == null ? 99.0F : Math.min(1000.0F, (float) (time - gr.pulseAt)) + partial;
            int jump = t >= 0.0F && t < 6.0F ? Math.round(Mth.sin(t / 6.0F * Mth.PI) * 3.0F) : 0;
            drawNote(g, x, y + 1 - jump, 0xFF000000 | colour);
            g.text(font, names[i], x + 10, y, 0xFFFFFFFF, true);
            g.text(font, instruments[i], x0 + w - 4 - font.width(instruments[i]) + slide, y, 0xFFB8AFA2, true);
            // how long it will stay
            float stay = gr == null ? 1.0F : gr.stay;
            int barW = Math.round((w - 16) * stay);
            g.fill(x + 10, y + 9, x + 10 + barW, y + 10, 0x70000000 | colour);
        }
    }

    /** A little eighth note in the member's colour. */
    private static void drawNote(GuiGraphicsExtractor g, int x, int y, int argb) {
        g.fill(x, y + 5, x + 3, y + 7, argb);
        g.fill(x + 1, y + 4, x + 3, y + 5, argb);
        g.fill(x + 2, y, x + 3, y + 5, argb);
        g.fill(x + 3, y, x + 5, y + 1, argb);
        g.fill(x + 4, y + 1, x + 5, y + 3, argb);
    }
}
