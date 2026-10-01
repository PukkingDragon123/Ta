package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.music.Score;
import com.thesift.entity.boss.Dictator;
import com.thesift.music.Performance;
import com.thesift.registry.ModEffects;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;

/**
 * Everything the client adds to the finale and to the curses:
 *
 * <ul>
 *   <li>the Grand Stage's performance: plays the score in time with the stage, darkens the sky
 *   and the fog bar by bar, and silences every other sound in the world - only the music is left</li>
 *   <li>the Conductor's fight: a music loop for each of his movements</li>
 *   <li>Sculk Corruption: a darkness that closes in from the edges of your sight the longer it
 *   lasts</li>
 *   <li>Feather Shield: a whirl of feathers in front of your eyes</li>
 * </ul>
 */
public final class ClientEffects {
    private static final Identifier SCULK_VIGNETTE = TheSift.id("textures/misc/sculk_vignette.png");
    private static final Identifier FEATHERS = TheSift.id("textures/misc/feather_shield.png");
    private static final RandomSource RANDOM = RandomSource.create();

    private static int lastPerformanceTick = -1;
    private static int fightTick;
    private static int fightPhase;
    private static float darkness;
    private static float darknessO;
    private static int corruptionTicks;
    private static boolean performing;

    private ClientEffects() {
    }

    // ------------------------------------------------------------------ state

    private static @Nullable BlockPos activeStage(Minecraft mc) {
        if (mc.level == null || mc.player == null || Performance.clientStage == null) {
            return null;
        }
        if (mc.level.getGameTime() - Performance.clientSeen > 5) {
            return null;
        }
        BlockPos s = Performance.clientStage;
        return mc.player.distanceToSqr(s.getX() + 0.5, s.getY(), s.getZ() + 0.5) < 80 * 80 ? s : null;
    }

    private static @Nullable Dictator nearbyConductor(LocalPlayer player) {
        for (Dictator d : player.level().getEntitiesOfClass(Dictator.class, new AABB(player.blockPosition()).inflate(64.0))) {
            if (d.isAlive()) {
                return d;
            }
        }
        return null;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        darknessO = darkness;
        if (mc.level == null || player == null || mc.isPaused()) {
            return;
        }
        BlockPos stage = activeStage(mc);
        float targetDark = 0.0F;
        if (stage != null) {
            int t = Performance.clientTick;
            if (!performing) {
                performing = true;
                // the world falls silent
                mc.getSoundManager().stop(null, SoundSource.AMBIENT);
                mc.getSoundManager().stop(null, SoundSource.WEATHER);
                mc.getSoundManager().stop(null, SoundSource.MUSIC);
            }
            if (t % 20 == 0) {
                mc.getMusicManager().stopPlaying();
            }
            int from = lastPerformanceTick < 0 || t < lastPerformanceTick || t - lastPerformanceTick > 5 ? t : lastPerformanceTick + 1;
            for (int k = from; k <= t; k++) {
                for (Score.Note n : Score.performance(k)) {
                    play(n, stage.getX() + 0.5, stage.getY() + 2.0, stage.getZ() + 0.5, 2.5F);
                }
            }
            lastPerformanceTick = t;
            targetDark = Performance.darkness(t);
            fightTick = 0;
        } else {
            performing = false;
            lastPerformanceTick = -1;
            Dictator d = nearbyConductor(player);
            if (d != null) {
                int phase = d.getPhase();
                if (phase != fightPhase) {
                    fightPhase = phase;
                    fightTick = 0;
                }
                if (fightTick % 20 == 0) {
                    mc.getMusicManager().stopPlaying();
                }
                // the loop holds its breath while he transforms
                if (d.transformTicks() == 0) {
                    for (Score.Note n : Score.fight(phase, fightTick)) {
                        play(n, player.getX(), player.getEyeY(), player.getZ(), 1.0F);
                    }
                    fightTick++;
                }
                targetDark = 0.45F + phase * 0.1F;
            } else {
                fightPhase = 0;
            }
        }
        darkness = Mth.approach(darkness, targetDark, 0.01F);
        if (player.hasEffect(ModEffects.SCULK_CORRUPTION)) {
            corruptionTicks = Math.min(1200, corruptionTicks + 1);
        } else {
            corruptionTicks = Math.max(0, corruptionTicks - 8);
        }
    }

    private static void play(Score.Note n, double x, double y, double z, float volume) {
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(n.sound().value(), SoundSource.RECORDS, n.volume() * volume, n.pitch(),
                RANDOM, x, y, z));
    }

    /** During the performance everything but the music falls silent. */
    public static void onPlaySound(PlaySoundEvent event) {
        if (!performing || event.getSound() == null) {
            return;
        }
        SoundInstance s = event.getSound();
        String path = event.getName();
        if (s.getSource() == SoundSource.RECORDS || s.getSource() == SoundSource.MASTER || s.getSource() == SoundSource.VOICE
                || path.startsWith("entity.conductor_mask") || path.startsWith("ui.") || path.startsWith("block.note_block")) {
            return;
        }
        event.setSound(null);
    }

    // ------------------------------------------------------------------ the dark sky

    private static float dark(float partial) {
        return Mth.lerp(partial, darknessO, darkness);
    }

    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float k = dark((float) event.getPartialTick());
        if (k <= 0.0F) {
            return;
        }
        // towards a deep night blue with a sculk tint
        event.setRed(Mth.lerp(k, event.getRed(), 0.02F));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.05F));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.09F));
    }

    public static void onRenderFog(ViewportEvent.RenderFog event) {
        float k = dark((float) event.getPartialTick());
        float c = Math.min(1.0F, corruptionTicks / 600.0F);
        float squeeze = Math.max(k * 0.6F, c * 0.8F);
        if (squeeze <= 0.0F) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(squeeze, event.getFarPlaneDistance(), 20.0F));
        event.setNearPlaneDistance(Mth.lerp(squeeze, event.getNearPlaneDistance(), 2.0F));
    }

    // ------------------------------------------------------------------ overlays

    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TheSift.id("stage_darkness"), ClientEffects::drawDarkness);
        event.registerAbove(TheSift.id("stage_darkness"), TheSift.id("sculk_corruption"), ClientEffects::drawCorruption);
        event.registerAbove(TheSift.id("sculk_corruption"), TheSift.id("feather_shield"), ClientEffects::drawFeathers);
    }

    private static void drawDarkness(GuiGraphicsExtractor g, DeltaTracker delta) {
        float k = dark(delta.getGameTimeDeltaPartialTick(false));
        if (k <= 0.01F) {
            return;
        }
        int a = (int) (k * 0.5F * 255.0F);
        g.fill(0, 0, g.guiWidth(), g.guiHeight(), a << 24 | 0x02040A);
    }

    private static void drawCorruption(GuiGraphicsExtractor g, DeltaTracker delta) {
        if (corruptionTicks <= 0) {
            return;
        }
        // the longer it lasts, the more of your sight it takes
        float c = Math.min(1.0F, corruptionTicks / 600.0F);
        Minecraft mc = Minecraft.getInstance();
        float pulse = mc.level == null ? 0.0F : Mth.sin((mc.level.getGameTime() + delta.getGameTimeDeltaPartialTick(false)) * 0.12F) * 0.06F;
        int a = (int) (Mth.clamp(0.35F + c * 0.65F + pulse, 0.0F, 1.0F) * 255.0F);
        int w = g.guiWidth();
        int h = g.guiHeight();
        // the vignette closes in: drawn larger than the screen early, and pulled tighter as it grows
        float grow = 1.6F - c * 0.6F;
        int vw = (int) (w * grow);
        int vh = (int) (h * grow);
        g.blit(RenderPipelines.GUI_TEXTURED, SCULK_VIGNETTE, (w - vw) / 2, (h - vh) / 2, 0.0F, 0.0F, vw, vh, 256, 256, 256, 256, a << 24 | 0xFFFFFF);
        if (vw > w || vh > h) {
            return;
        }
        int fillA = a << 24 | 0x01080A;
        g.fill(0, 0, w, (h - vh) / 2, fillA);
        g.fill(0, (h + vh) / 2, w, h, fillA);
    }

    private static void drawFeathers(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || !mc.player.hasEffect(ModEffects.FEATHER_SHIELD) || !mc.options.getCameraType().isFirstPerson()) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        float t = mc.player.tickCount + delta.getGameTimeDeltaPartialTick(false);
        // two layers of feathers turning in opposite directions
        for (int layer = 0; layer < 2; layer++) {
            float sway = Mth.sin(t * (0.07F + layer * 0.03F)) * 12.0F;
            int size = (int) (Math.max(w, h) * (1.25F + layer * 0.2F));
            int x = (w - size) / 2 + (int) (sway * (layer == 0 ? 1 : -1));
            int y = (h - size) / 2 + (int) (Mth.cos(t * 0.05F) * 6.0F);
            int alpha = layer == 0 ? 235 : 150;
            g.blit(RenderPipelines.GUI_TEXTURED, FEATHERS, x, y, layer * 128.0F, 0.0F, size, size, 128, 256, 256, 256, alpha << 24 | 0xFFFFFF);
        }
    }
}
