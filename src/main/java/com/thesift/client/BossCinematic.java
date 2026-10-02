package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.entity.boss.BossStages;
import java.util.Set;
import net.minecraft.client.CameraType;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.CalculatePlayerTurnEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * The boss stage cutscene (agent B2), played when the server sends {@link BossStages.StageCleared}:
 * the view leaves the player's eyes for a free camera (an unspawned armour stand set as
 * {@link Minecraft#setCameraEntity}), which swings out and orbits the boss while pushing in on
 * it. Letterbox bars close in while the HUD steps aside, slow motes drift round the boss, and a
 * stylised title card fades in - the boss's name, "Stage II" / "Stage III" / "Vanquished" and the
 * stage's epithet. Then the camera glides home into the player's eyes. About four seconds;
 * sneaking skips it (and tells the server, which stops protecting the player).
 */
public final class BossCinematic {
    private static final Identifier LAYER = TheSift.id("boss_cinematic");
    private static final Set<Identifier> HUD = Set.of(VanillaGuiLayers.CROSSHAIR, VanillaGuiLayers.HOTBAR, VanillaGuiLayers.PLAYER_HEALTH,
            VanillaGuiLayers.ARMOR_LEVEL, VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.VEHICLE_HEALTH, VanillaGuiLayers.AIR_LEVEL,
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR,
            VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP, VanillaGuiLayers.EFFECTS, VanillaGuiLayers.SCOREBOARD_SIDEBAR,
            VanillaGuiLayers.BOSS_OVERLAY);
    private static final int LENGTH = BossStages.LENGTH;
    /** Ticks to glide out of the player's eyes, and back in. */
    private static final int INTRO = 12;
    private static final int OUTRO = 16;
    /** Title colours per boss: the Thumper's sculk teal, the Weaver's violet, the Conductor's gold. */
    private static final int[] TINT = {0x6FF6FF, 0xD69BFF, 0xF2D88A};
    private static final String[] NAMES = {"thumper", "strummer", "dictator"};
    private static final String[] NUMERALS = {"", "I", "II", "III"};

    private static boolean active;
    private static @Nullable ArmorStand camera;
    private static @Nullable CameraType previousCamera;
    private static @Nullable ClientLevel filmedIn;
    private static Vec3 centre = Vec3.ZERO;
    private static float width;
    private static float height;
    private static int kind;
    private static int stage;
    private static int tick;
    private static int end;
    private static boolean skipped;
    private static float baseAngle;
    private static float dir;
    private static float yaw;
    private static float bars;
    private static float barsO;
    private static float title;
    private static float titleO;
    private static float flash;
    private static float flashO;

    private BossCinematic() {
    }

    public static void register(IEventBus modBus) {
        BossStages.clientHandler = BossCinematic::start;
        modBus.addListener(BossCinematic::registerOverlay);
        NeoForge.EVENT_BUS.addListener(BossCinematic::onClientTick);
        NeoForge.EVENT_BUS.addListener(BossCinematic::onPlayerTurn);
        NeoForge.EVENT_BUS.addListener(BossCinematic::onRenderLayer);
        NeoForge.EVENT_BUS.addListener(BossCinematic::onRenderHand);
        NeoForge.EVENT_BUS.addListener(BossCinematic::onInteractionKey);
    }

    // ------------------------------------------------------------------ starting and stopping

    private static void start(BossStages.StageCleared p) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (player == null || level == null || player.isSpectator()) {
            return;
        }
        centre = new Vec3(p.x(), p.y(), p.z());
        width = Math.max(1.5F, p.width());
        height = Math.max(1.5F, p.height());
        kind = Mth.clamp(p.kind(), 0, 2);
        stage = p.stage();
        tick = 0;
        end = LENGTH;
        skipped = false;
        // the first shot looks past the player at the boss; the orbit turns a different way each stage
        baseAngle = (float) Math.atan2(player.getZ() - centre.z, player.getX() - centre.x);
        dir = stage == 2 ? 1.0F : -1.0F;
        if (!active) {
            ArmorStand cam = new ArmorStand(EntityTypes.ARMOR_STAND, level);
            Vec3 eye = player.getEyePosition();
            yaw = player.getYRot();
            place(cam, eye, yaw, player.getXRot());
            place(cam, eye, yaw, player.getXRot());
            camera = cam;
            filmedIn = level;
            previousCamera = mc.options.getCameraType();
            if (previousCamera != CameraType.FIRST_PERSON) {
                mc.options.setCameraType(CameraType.FIRST_PERSON);
            }
            mc.setCameraEntity(cam);
            active = true;
        }
        flash = 1.0F;
        flashO = 1.0F;
        // the beat that opens the film: a deep chime, a darker one for the defeat
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), stage == BossStages.DEFEATED ? SoundEvents.BEACON_DEACTIVATE : SoundEvents.BEACON_ACTIVATE,
                SoundSource.HOSTILE, 1.4F, stage == BossStages.DEFEATED ? 0.5F : 0.65F, false);
        level.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE, 2.0F, 0.5F, false);
    }

    private static void stop(Minecraft mc) {
        active = false;
        if (mc.player != null && mc.getCameraEntity() == camera) {
            mc.setCameraEntity(mc.player);
        }
        CameraType previous = previousCamera;
        if (previous != null && mc.options.getCameraType() == CameraType.FIRST_PERSON && previous != CameraType.FIRST_PERSON) {
            mc.options.setCameraType(previous);
        }
        previousCamera = null;
        camera = null;
        filmedIn = null;
        bars = 0.0F;
        barsO = 0.0F;
        title = 0.0F;
        titleO = 0.0F;
        flash = 0.0F;
        flashO = 0.0F;
    }

    // ------------------------------------------------------------------ the camera

    public static void onClientTick(ClientTickEvent.Post event) {
        if (!active) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        ArmorStand cam = camera;
        if (player == null || mc.level == null || mc.level != filmedIn || cam == null || !player.isAlive()) {
            stop(mc);
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        barsO = bars;
        titleO = title;
        flashO = flash;
        tick++;
        if (!skipped && tick > 4 && mc.options.keyShift.isDown()) {
            skipped = true;
            end = Math.min(end, tick + OUTRO);
            ClientPacketDistributor.sendToServer(BossStages.Skip.INSTANCE);
        }
        if (tick >= end) {
            stop(mc);
            return;
        }
        if (mc.getCameraEntity() != cam) {
            mc.setCameraEntity(cam);
        }
        // orbit and push in: a wide, high shot sweeping round to a low, close one
        float u = tick / (float) LENGTH;
        float e = smooth(u);
        double r = Math.max(width, height * 0.8F);
        double dist = Mth.lerp(e, r * 2.6 + 5.0, r * 1.5 + 2.5);
        double a = baseAngle + dir * Math.toRadians(85.0) * e;
        double up = Mth.lerp(e, r * 0.9 + 2.0, r * 0.25 + 0.6);
        Vec3 orbit = new Vec3(centre.x + Math.cos(a) * dist, centre.y + height * 0.5 + up, centre.z + Math.sin(a) * dist);
        Vec3 focus = centre.add(0.0, height * 0.55, 0.0);
        // glide out of the player's eyes, and back into them at the end
        Vec3 eye = player.getEyePosition();
        float k = smooth(tick / (float) INTRO) * (1.0F - smooth((tick - (end - OUTRO)) / (float) OUTRO));
        Vec3 at = eye.lerp(orbit, k);
        double dx = focus.x - at.x;
        double dy = focus.y - at.y;
        double dz = focus.z - at.z;
        float lookYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0F;
        float lookPitch = (float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)));
        float wantYaw = player.getYRot() + Mth.wrapDegrees(lookYaw - player.getYRot()) * k;
        float wantPitch = Mth.lerp(k, player.getXRot(), lookPitch);
        // keep the yaw continuous so the interpolation between ticks never spins the long way round
        yaw = yaw + Mth.wrapDegrees(wantYaw - yaw);
        place(cam, at, yaw, wantPitch);

        bars = Mth.approach(bars, tick < end - 12 ? 1.0F : 0.0F, 0.09F);
        boolean showTitle = tick >= 14 && tick < end - 14;
        title = Mth.approach(title, showTitle ? 1.0F : 0.0F, showTitle ? 0.08F : 0.12F);
        flash = Math.max(0.0F, flash - 0.12F);
        motes(mc.level, tick);
    }

    /** Moves the camera stand so its eyes are at {@code at}, remembering where it was for interpolation. */
    private static void place(ArmorStand cam, Vec3 at, float yRot, float xRot) {
        cam.xo = cam.getX();
        cam.yo = cam.getY();
        cam.zo = cam.getZ();
        cam.yRotO = cam.getYRot();
        cam.xRotO = cam.getXRot();
        cam.setPos(at.x, at.y - cam.getEyeHeight(), at.z);
        cam.setYRot(yRot);
        cam.setXRot(Mth.clamp(xRot, -89.0F, 89.0F));
    }

    /** The slow-motion feel: motes and souls hanging in the air round the boss, barely drifting. */
    private static void motes(ClientLevel level, int t) {
        RandomSource rnd = level.getRandom();
        double r = width * 1.6 + 2.0;
        int n = t < 6 ? 14 : 3;
        for (int i = 0; i < n; i++) {
            double a = rnd.nextDouble() * Math.PI * 2.0;
            double d = r * (0.4 + rnd.nextDouble() * 0.8);
            double x = centre.x + Math.cos(a) * d;
            double y = centre.y + rnd.nextDouble() * (height + 2.0);
            double z = centre.z + Math.sin(a) * d;
            switch (kind) {
                case BossStages.WEAVER -> level.addParticle(i % 2 == 0 ? ParticleTypes.WHITE_ASH : ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0, 0.004, 0.0);
                case BossStages.CONDUCTOR -> level.addParticle(i % 2 == 0 ? ParticleTypes.END_ROD : ParticleTypes.WHITE_ASH, x, y, z, 0.0, 0.003, 0.0);
                default -> level.addParticle(i % 2 == 0 ? ParticleTypes.SCULK_SOUL : ParticleTypes.ASH, x, y, z, 0.0, 0.004, 0.0);
            }
        }
    }

    private static float smooth(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    // ------------------------------------------------------------------ input while the film plays

    /** The mouse rests while the film plays. */
    public static void onPlayerTurn(CalculatePlayerTurnEvent event) {
        if (active) {
            event.setMouseSensitivity(-1.0 / 3.0);
            event.setCinematicCameraEnabled(false);
        }
    }

    /** No swinging or using things from the film camera. */
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (active) {
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    public static void onRenderHand(RenderHandEvent event) {
        if (active) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ the screen

    public static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerBelow(VanillaGuiLayers.CHAT, LAYER, BossCinematic::draw);
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
        Minecraft mc = Minecraft.getInstance();
        float partial = delta.getGameTimeDeltaPartialTick(false);
        int w = g.guiWidth();
        int h = g.guiHeight();
        // the white flash of the blow that ended the stage
        int fa = Math.round(Mth.lerp(partial, flashO, flash) * 150.0F);
        if (fa > 4) {
            g.fill(0, 0, w, h, fa << 24 | 0xFFFFFF);
        }
        int bar = Math.round(h * 0.12F * smooth(Mth.lerp(partial, barsO, bars)));
        if (bar > 0) {
            g.fill(0, 0, w, bar, 0xFF000000);
            g.fill(0, h - bar, w, h, 0xFF000000);
        }
        if (bar > 10 && !skipped) {
            Component skip = Component.translatable("cutscene.thesift.skip");
            g.text(mc.font, skip, w - mc.font.width(skip) - 8, h - bar / 2 - 4, 0x90A8B0B8, false);
        }
        float a = Mth.lerp(partial, titleO, title);
        int alpha = Math.round(a * 255.0F);
        if (alpha <= 8) {
            return;
        }
        int tint = TINT[kind];
        String boss = NAMES[kind];
        Component name = Component.translatable("entity.thesift." + boss);
        Component main = stage == BossStages.DEFEATED ? Component.translatable("cutscene.thesift.defeated")
                : Component.translatable("cutscene.thesift.stage", NUMERALS[Mth.clamp(stage, 0, 3)]);
        Component epithet = Component.translatable("cutscene.thesift." + boss + "." + stage);
        float cy = h * 0.40F;
        // the boss's name, small and spaced, over a hairline rule that grows out from the middle
        int nw = mc.font.width(name);
        g.text(mc.font, name, (w - nw) / 2, Math.round(cy) - 30, alpha << 24 | 0xD8D8D8, true);
        int rule = Math.round((nw / 2.0F + 40.0F) * smooth(a * 1.4F));
        g.fill(w / 2 - rule, Math.round(cy) - 19, w / 2 + rule, Math.round(cy) - 18, alpha << 24 | tint);
        // the stage, big, sliding up as it fades in
        float scale = 3.0F + 0.25F * a;
        g.pose().pushMatrix();
        g.pose().translate(w / 2.0F, cy + 6.0F * (1.0F - a));
        g.pose().scale(scale, scale);
        g.text(mc.font, main, -mc.font.width(main) / 2, -4, alpha << 24 | tint, true);
        g.pose().popMatrix();
        int ew = mc.font.width(epithet);
        g.fill(w / 2 - rule, Math.round(cy) + 17, w / 2 + rule, Math.round(cy) + 18, alpha << 24 | tint);
        g.text(mc.font, epithet, (w - ew) / 2, Math.round(cy) + 23, alpha << 24 | 0xF4F0E6, true);
    }
}
