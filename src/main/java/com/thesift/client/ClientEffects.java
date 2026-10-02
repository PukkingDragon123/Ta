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
 *   <li>Sculk Corruption: sculk taking your sight - a crust growing in from the corners of the
 *   screen, glowing veins pulsing in time with a heartbeat you can hear, and tentacles writhing
 *   in from the edges, more of them, longer and closer the deeper it goes</li>
 * </ul>
 */
public final class ClientEffects {
    private static final Identifier SCULK_VIGNETTE = TheSift.id("textures/misc/sculk_vignette.png");
    private static final Identifier SCULK_VEINS = TheSift.id("textures/misc/sculk_veins.png");
    /** The crust growing in from each corner: bottom-left, bottom-right, top-right, top-left. */
    private static final Identifier[] SCULK_CRUST = {TheSift.id("textures/misc/sculk_crust.png"), TheSift.id("textures/misc/sculk_crust_br.png"),
            TheSift.id("textures/misc/sculk_crust_tr.png"), TheSift.id("textures/misc/sculk_crust_tl.png")};
    /** Four 16 x 128 tentacles side by side, tip at the top: drawn as chains of eight 16 x 16 slices. */
    private static final Identifier SCULK_TENTACLE = TheSift.id("textures/misc/sculk_tentacle.png");
    private static final RandomSource RANDOM = RandomSource.create();

    private static int lastPerformanceTick = -1;
    private static int fightTick;
    private static int fightPhase;
    private static float darkness;
    private static float darknessO;
    private static int corruptionTicks;
    /** Corruption's level (amplifier + 1, 0 when clean), eased so the overlay never jumps. */
    private static float corruptionLevel;
    private static int heartbeatIn;
    private static int lastBeat = -100;
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
        var corruption = player.getEffect(ModEffects.SCULK_CORRUPTION);
        if (corruption != null) {
            corruptionTicks = Math.min(1200, corruptionTicks + 1);
            corruptionLevel = Mth.approach(corruptionLevel, corruption.getAmplifier() + 1.0F, 0.04F);
            // a heartbeat you can hear, quickening the deeper it goes
            if (--heartbeatIn <= 0) {
                int amp = corruption.getAmplifier();
                heartbeatIn = Math.max(12, 30 - amp * 4);
                lastBeat = player.tickCount;
                Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(net.minecraft.sounds.SoundEvents.WARDEN_HEARTBEAT, SoundSource.PLAYERS,
                        0.35F + amp * 0.1F, 0.9F + amp * 0.05F, RANDOM, player.getX(), player.getEyeY(), player.getZ()));
            }
            if (player.tickCount % 3 == 0 && mc.level != null) {
                // sculk motes cling about you
                mc.level.addParticle(net.minecraft.core.particles.ParticleTypes.SCULK_CHARGE_POP, player.getRandomX(0.8), player.getY() + RANDOM.nextDouble() * 1.8,
                        player.getRandomZ(0.8), 0.0, 0.02, 0.0);
            }
        } else {
            corruptionTicks = Math.max(0, corruptionTicks - 8);
            corruptionLevel = Mth.approach(corruptionLevel, 0.0F, 0.06F);
            heartbeatIn = 0;
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
        if (corruptionTicks <= 0 || corruptionLevel <= 0.01F) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float time = mc.player.tickCount + partial;
        // how deep it has its hold: its level, and how long it has had you
        float level = Mth.clamp(corruptionLevel / 5.0F, 0.0F, 1.0F);
        float held = Math.min(1.0F, corruptionTicks / 600.0F);
        float k = Mth.clamp(0.3F + level * 0.45F + held * 0.25F, 0.0F, 1.0F);
        // it creeps in over the first second or two, and pulls back as it fades
        float appear = Math.min(1.0F, corruptionTicks / 40.0F) * Math.min(1.0F, corruptionLevel);
        float since = time - lastBeat;
        float beat = since < 0.0F ? 0.0F : (float) Math.exp(-since * 0.25F) + 0.5F * (float) Math.exp(-Math.abs(since - 6.0F) * 0.5F);
        int w = g.guiWidth();
        int h = g.guiHeight();

        // the dark closing in
        float grow = 1.7F - k * 0.75F - beat * 0.04F;
        int vw = (int) (w * grow);
        int vh = (int) (h * grow);
        int va = (int) (Mth.clamp((0.45F + k * 0.55F) * appear, 0.0F, 1.0F) * 255.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, SCULK_VIGNETTE, (w - vw) / 2, (h - vh) / 2, 0.0F, 0.0F, vw, vh, 256, 256, 256, 256, va << 24 | 0xFFFFFF);
        if (vw < w || vh < h) {
            int fill = va << 24 | 0x01080A;
            g.fill(0, 0, w, Math.max(0, (h - vh) / 2), fill);
            g.fill(0, (h + vh) / 2, w, h, fill);
            g.fill(0, 0, Math.max(0, (w - vw) / 2), h, fill);
            g.fill((w + vw) / 2, 0, w, h, fill);
        }
        // glowing veins, throbbing with the heartbeat
        int veinA = (int) (Mth.clamp((0.15F + k * 0.45F) * (0.55F + 0.45F * beat) * appear, 0.0F, 1.0F) * 255.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, SCULK_VEINS, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256, veinA << 24 | 0xFFFFFF);

        // tentacles writhing in from the edges, more and longer the deeper it goes
        int count = Math.min(9, 2 + Math.round(corruptionLevel * 1.5F) + (held > 0.5F ? 1 : 0));
        float reach = Math.min(w, h) * (0.24F + 0.34F * k) * (0.3F + 0.7F * appear);
        int ta = (int) (Mth.clamp(appear * 1.2F, 0.0F, 1.0F) * 255.0F);
        for (int n = 0; n < count; n++) {
            drawTentacle(g, n, w, h, reach * (0.85F + 0.15F * Mth.sin(n * 2.3F)), time, beat, ta);
        }

        // and the crust growing over the corners of your sight
        float crust = Math.min(w, h) * (0.32F + 0.5F * k) * (0.6F + 0.4F * appear) * (1.0F + beat * 0.03F);
        int ca = (int) (Mth.clamp(appear * 1.1F, 0.0F, 1.0F) * 255.0F);
        int cs = (int) crust;
        for (int corner = 0; corner < 4; corner++) {
            boolean right = corner == 1 || corner == 2;
            boolean top = corner >= 2;
            g.blit(RenderPipelines.GUI_TEXTURED, SCULK_CRUST[corner], right ? w - cs : 0, top ? 0 : h - cs, 0.0F, 0.0F, cs, cs, 256, 256, 256, 256,
                    ca << 24 | 0xFFFFFF);
        }
        // the beat itself: a faint teal flush at the edges
        if (beat > 0.05F) {
            int fa = (int) (beat * 0.12F * k * appear * 255.0F);
            g.blit(RenderPipelines.GUI_TEXTURED, SCULK_VIGNETTE, -w / 4, -h / 4, 0.0F, 0.0F, w * 3 / 2, h * 3 / 2, 256, 256, 256, 256, fa << 24 | 0x29DFEB);
        }
    }

    /**
     * One tentacle: eight slices of the strip, chained root to tip, each turned a little further
     * than the last - a slow writhe, a curl at the tip, and a twitch on every heartbeat.
     */
    private static void drawTentacle(GuiGraphicsExtractor g, int n, int w, int h, float length, float time, float beat, int alpha) {
        // where along the screen's edge it reaches in from (spread round the edge, never two together)
        float f = (n * 0.618034F + 0.11F) % 1.0F;
        float per = 2.0F * (w + h);
        float d = f * per;
        float x;
        float y;
        if (d < w) {
            x = d;
            y = h;
        } else if (d < w + h) {
            x = w;
            y = h - (d - w);
        } else if (d < 2 * w + h) {
            x = w - (d - w - h);
            y = 0.0F;
        } else {
            x = 0.0F;
            y = d - 2 * w - h;
        }
        float cx = w * 0.5F - x;
        float cy = h * 0.5F - y;
        float sway = Mth.sin(time * (0.021F + n * 0.004F) + n * 1.7F) * 0.35F;
        float angle = (float) Math.atan2(cx, -cy) + sway + (n % 2 == 0 ? 0.25F : -0.25F);
        float curl = n % 2 == 0 ? 1.0F : -1.0F;
        float scale = length / 128.0F;
        var pose = g.pose();
        pose.pushMatrix();
        // the root sits just off the edge of the screen
        pose.translate(x - Mth.sin(angle) * length * 0.06F, y + Mth.cos(angle) * length * 0.06F);
        pose.rotate(angle);
        pose.scale(scale, scale);
        int u = (n % 4) * 16;
        for (int i = 0; i < 8; i++) {
            float along = i / 7.0F;
            float writhe = Mth.sin(time * (0.09F + n * 0.011F) - i * 0.75F + n) * (0.07F + 0.11F * along);
            float twitch = beat * 0.06F * Mth.sin(i * 1.9F + n);
            pose.rotate(curl * (0.02F + 0.075F * along * along) + writhe + twitch);
            boolean tip = i == 7;
            int v = 112 - i * 16;
            // each slice runs a texel into the next, so the bends show no gaps
            g.blit(RenderPipelines.GUI_TEXTURED, SCULK_TENTACLE, -8, tip ? -16 : -17, (float) u, (float) (tip ? v : v - 1), 16, tip ? 16 : 17, 16,
                    tip ? 16 : 17, 64, 128, alpha << 24 | 0xFFFFFF);
            pose.translate(0.0F, -16.0F);
        }
        pose.popMatrix();
    }
}
