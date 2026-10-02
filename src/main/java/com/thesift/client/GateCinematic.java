package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.portal.GateAwakening;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.CalculateDetachedCameraDistanceEvent;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;

/**
 * The gate's awakening, seen like a film: for players near a gate that wakes (see
 * {@link GateAwakening}), the camera swings out behind them into third person and slowly pulls
 * back, the view turns towards the gate, black bars close in from top and bottom while the HUD
 * steps aside, and "The Sift Awakens" fades in as the last ring closes. Afterwards the camera comes
 * home and the player's own camera mode is put back.
 */
public final class GateCinematic {
    private static final Identifier LAYER = TheSift.id("gate_cinematic");
    /** The HUD that steps aside for the film (chat, titles and subtitles stay). */
    private static final Set<Identifier> HUD = Set.of(VanillaGuiLayers.CROSSHAIR, VanillaGuiLayers.HOTBAR, VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP, VanillaGuiLayers.EFFECTS, VanillaGuiLayers.SCOREBOARD_SIDEBAR);
    /** Vanilla's third-person distance. */
    private static final float NEAR = 4.0F;
    /** Turning towards the gate stops a little after the chord; then the view is the player's again. */
    private static final int LOOK_UNTIL = GateAwakening.CLIMAX + 14;

    private static boolean active;
    private static @Nullable CameraType previousCamera;
    private static @Nullable ClientLevel filmedIn;
    private static @Nullable Vec3 centre;
    private static int tick;
    private static float far = 14.0F;
    private static float zoom = NEAR;
    private static float zoomO = NEAR;
    private static float bars;
    private static float barsO;
    private static float title;
    private static float titleO;

    private GateCinematic() {
    }

    // ------------------------------------------------------------------ ticking

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        zoomO = zoom;
        barsO = bars;
        titleO = title;
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            if (active) {
                stop(mc);
            }
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        Vec3 gate = GateAwakening.clientCentre;
        boolean live = gate != null && mc.level.getGameTime() - GateAwakening.clientSeen <= 5L && GateAwakening.clientTick <= GateAwakening.LENGTH;
        if (!active) {
            // join a gate that is waking nearby, unless it is nearly done
            if (gate == null || !live || GateAwakening.clientTick > GateAwakening.CLIMAX
                    || player.distanceToSqr(gate) > GateAwakening.CINEMATIC_RANGE * GateAwakening.CINEMATIC_RANGE) {
                return;
            }
            start(mc, gate);
        }
        if (live && gate != null) {
            tick = GateAwakening.clientTick;
            centre = gate;
        } else {
            // the drum has stopped reporting (it finished, or went out of range): finish on our own clock
            tick++;
        }
        Vec3 c = centre;
        double leave = GateAwakening.CINEMATIC_RANGE * 1.6;
        if (c == null || mc.level != filmedIn || tick >= GateAwakening.LENGTH + 4 || player.distanceToSqr(c) > leave * leave) {
            stop(mc);
            return;
        }
        int t = tick;
        // the camera pulls back as the gate wakes, holds, and comes home after the chord
        float out = smooth(t / 50.0F) * (1.0F - smooth((t - (GateAwakening.CLIMAX + 12)) / 32.0F));
        float target = NEAR + (far - NEAR) * out;
        zoom += (target - zoom) * 0.3F;
        bars = Mth.approach(bars, t < GateAwakening.LENGTH - 16 ? 1.0F : 0.0F, 0.07F);
        boolean showTitle = t >= GateAwakening.CLIMAX && t < GateAwakening.CLIMAX + 32;
        title = Mth.approach(title, showTitle ? 1.0F : 0.0F, showTitle ? 0.12F : 0.06F);
        if (t < LOOK_UNTIL) {
            lookAt(player, c);
        }
    }

    private static void start(Minecraft mc, Vec3 gate) {
        active = true;
        centre = gate;
        filmedIn = mc.level;
        // further back for a bigger gate
        far = Mth.clamp(8.0F + GateAwakening.clientSpan * 0.4F, 12.0F, 16.0F);
        zoom = NEAR;
        zoomO = NEAR;
        previousCamera = mc.options.getCameraType();
        if (previousCamera != CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        }
    }

    private static void stop(Minecraft mc) {
        active = false;
        // put the player's own camera back - unless they changed it themselves meanwhile
        CameraType previous = previousCamera;
        if (previous != null && mc.options.getCameraType() == CameraType.THIRD_PERSON_BACK) {
            mc.options.setCameraType(previous);
        }
        previousCamera = null;
        filmedIn = null;
        centre = null;
        zoom = NEAR;
        zoomO = NEAR;
        bars = 0.0F;
        barsO = 0.0F;
        title = 0.0F;
        titleO = 0.0F;
    }

    /** Eases the player's view towards the gate, a little from above so the shot frames both. */
    private static void lookAt(LocalPlayer player, Vec3 c) {
        Vec3 eye = player.getEyePosition();
        double dx = c.x - eye.x;
        double dy = c.y - eye.y;
        double dz = c.z - eye.z;
        double horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 0.5) {
            return;
        }
        float yaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float pitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, horizontal)) + 6.0F, -25.0F, 30.0F);
        float k = 0.14F;
        player.setYRot(player.getYRot() + Mth.wrapDegrees(yaw - player.getYRot()) * k);
        player.setXRot(Mth.clamp(player.getXRot() + (pitch - player.getXRot()) * k, -90.0F, 90.0F));
    }

    private static float smooth(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    // ------------------------------------------------------------------ the camera

    public static void onCameraDistance(CalculateDetachedCameraDistanceEvent event) {
        if (!active) {
            return;
        }
        float partial = Minecraft.getInstance().getDeltaTracker().getGameTimeDeltaPartialTick(false);
        event.setDistance(Math.max(event.getDistance(), Mth.lerp(partial, zoomO, zoom)));
    }

    /** While the view turns towards the gate the mouse rests. */
    public static void onPlayerTurn(CalculatePlayerTurnEvent event) {
        if (active && tick < LOOK_UNTIL) {
            // vanilla turns by (s * 0.6 + 0.2)^3 per pixel: at s = -1/3 that is zero
            event.setMouseSensitivity(-1.0 / 3.0);
            event.setCinematicCameraEnabled(false);
        }
    }

    // ------------------------------------------------------------------ the screen

    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, LAYER, GateCinematic::draw);
    }

    public static void onRenderLayer(RenderGuiLayerEvent.Pre event) {
        if (active && bars > 0.05F && HUD.contains(event.getName())) {
            event.setCanceled(true);
        }
    }

    private static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (!active) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth();
        int h = g.guiHeight();
        int bar = Math.round(h * 0.11F * smooth(Mth.lerp(partial, barsO, bars)));
        if (bar > 0) {
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
        float a = Mth.lerp(partial, titleO, title);
        int alpha = Math.round(a * 255.0F);
        if (alpha <= 8) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Component main = Component.translatable("title.thesift.awakening");
        Component sub = Component.translatable("title.thesift.awakening.sub");
        float y = h * 0.34F;
        // the title grows a touch as it fades in
        float scale = 2.4F + 0.3F * a;
        g.pose().pushMatrix();
        g.pose().translate(w / 2.0F, y);
        g.pose().scale(scale, scale);
        g.text(mc.font, main, -mc.font.width(main) / 2, -4, alpha << 24 | 0xE8FFFC, true);
        g.pose().popMatrix();
        g.text(mc.font, sub, (w - mc.font.width(sub)) / 2, Math.round(y) + 18, alpha << 24 | 0xFFC8EC, true);
    }
}
