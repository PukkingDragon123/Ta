package com.thesift.client.gate;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.thesift.TheSift;
import com.thesift.client.sky.SiftSkyFx;
import com.thesift.portal.GateAwakening;
import com.thesift.registry.ModGateFx;
import com.thesift.registry.ModParticles;
import com.thesift.world.Rumble;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * B1 Portal & sky FX: the gate's opening, staged on the client in step with the drum's timeline
 * ({@link GateAwakening}). Every player near a waking gate sees it:
 *
 * <ol>
 *   <li><b>the glow</b> - from the first note cyan light swells over the place: the fog and the
 *   screen turn cyan, glowing motes drift in towards the gate, mist pools on the ground;</li>
 *   <li><b>the distortion</b> - from {@link GateAwakening#WARP_START} a real post effect
 *   (post_effect/gate_warp_1..6, shader thesift:post/gate_warp) twists the world round the gate,
 *   draws it in and splits its colours, harder and harder;</li>
 *   <li><b>the implosion</b> - just before the chord everything rushes into the gate and the view
 *   narrows;</li>
 *   <li><b>the flash</b> - on the chord a white-cyan flash fills the screen, the camera kicks and a
 *   shockwave of bent light (post_effect/gate_shock_1..8) and sparks rolls outwards while the
 *   portal appears, cell by cell from its heart (see {@link SiftPortalRenderer}).</li>
 * </ol>
 *
 * <p>The shader is test-compiled first, like the Rainbow Daze; if it is refused the rest still
 * plays. Distortion scales with the "Distortion Effects" accessibility slider; the flash never
 * drops below half strength but never reaches full white either.</p>
 */
public final class GateAwakeningFx {
    private static final Identifier VIGNETTE = TheSift.id("textures/misc/rainbow_daze_vignette.png");
    private static final List<Identifier> WARP = List.of(TheSift.id("gate_warp_1"), TheSift.id("gate_warp_2"), TheSift.id("gate_warp_3"),
            TheSift.id("gate_warp_4"), TheSift.id("gate_warp_5"), TheSift.id("gate_warp_6"));
    private static final List<Identifier> SHOCK = List.of(TheSift.id("gate_shock_1"), TheSift.id("gate_shock_2"), TheSift.id("gate_shock_3"),
            TheSift.id("gate_shock_4"), TheSift.id("gate_shock_5"), TheSift.id("gate_shock_6"), TheSift.id("gate_shock_7"),
            TheSift.id("gate_shock_8"));
    /** Beyond this the opening is only heard. */
    private static final double FELT = 48.0;
    private static final RandomSource RANDOM = RandomSource.create();

    private static float glow;
    private static float glowO;
    private static float warp;
    private static float warpO;
    private static float flash;
    private static float flashO;
    private static int shockStep;
    private static int lastTick = -1;
    private static @Nullable Boolean postWorks;
    private static @Nullable RenderPipeline postCheck;

    private GateAwakeningFx() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(GateAwakeningFx::registerOverlay);
        modBus.addListener(GateAwakeningFx::registerRenderers);
        NeoForge.EVENT_BUS.addListener(GateAwakeningFx::onClientTick);
        NeoForge.EVENT_BUS.addListener(GateAwakeningFx::onFogColor);
        NeoForge.EVENT_BUS.addListener(GateAwakeningFx::onFov);
        NeoForge.EVENT_BUS.addListener(GateAwakeningFx::onCameraAngles);
        // the Sift sky's rainbows, ribbons, clouds, shooting stars and ambient motes tick here too
        NeoForge.EVENT_BUS.addListener(SiftSkyFx::onClientTick);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(ModGateFx.SIFT_PORTAL.get(), SiftPortalRenderer::new);
    }

    private static void registerOverlay(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TheSift.id("gate_awakening"), GateAwakeningFx::draw);
    }

    // ------------------------------------------------------------------ the timeline

    private static float smooth(float x) {
        float t = Mth.clamp(x, 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** The waking gate's centre, if one is waking and reporting right now. */
    static @Nullable Vec3 liveGate(ClientLevel level) {
        Vec3 gate = GateAwakening.clientCentre;
        if (gate == null || level.getGameTime() - GateAwakening.clientSeen > 5L || GateAwakening.clientTick > GateAwakening.LENGTH) {
            return null;
        }
        return gate;
    }

    private static float distortion(Minecraft mc) {
        return mc.options.screenEffectScale().get().floatValue();
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        glowO = glow;
        warpO = warp;
        flashO = flash;
        LocalPlayer player = mc.player;
        ClientLevel level = mc.level;
        if (level == null || player == null) {
            glow = 0.0F;
            warp = 0.0F;
            flash = 0.0F;
            shockStep = 0;
            lastTick = -1;
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        Vec3 gate = liveGate(level);
        float tGlow = 0.0F;
        float tWarp = 0.0F;
        float tFlash = 0.0F;
        shockStep = 0;
        if (gate != null) {
            int t = GateAwakening.clientTick;
            double dist = Math.sqrt(player.distanceToSqr(gate));
            float near = Mth.clamp((float) (1.0 - (dist - 8.0) / (FELT - 8.0)), 0.0F, 1.0F);
            int climax = GateAwakening.CLIMAX;
            tGlow = (t < climax ? smooth(t / (float) (climax - 6)) : 1.0F - smooth((t - climax) / 40.0F)) * near;
            if (t >= GateAwakening.WARP_START && t < climax) {
                tWarp = smooth((t - GateAwakening.WARP_START) / (float) (climax - GateAwakening.WARP_START - 4)) * near;
            }
            if (t >= climax - 3 && t < climax) {
                tFlash = (t - (climax - 3) + 1) / 4.0F * near;
            } else if (t >= climax) {
                float fade = 1.0F - (t - climax) / 22.0F;
                tFlash = fade > 0.0F ? fade * fade * near : 0.0F;
            }
            if (t >= climax && t < climax + GateAwakening.SHOCK_TICKS && near > 0.0F) {
                shockStep = Mth.clamp(1 + (t - climax) * SHOCK.size() / GateAwakening.SHOCK_TICKS, 1, SHOCK.size());
            }
            // the moments of the opening, each played once (catching up if a tick was missed)
            int from = lastTick < 0 || t < lastTick || t - lastTick > 40 ? t : lastTick + 1;
            for (int tick = from; tick <= t; tick++) {
                moment(level, gate, tick, near);
            }
            lastTick = t;
            if (near > 0.0F) {
                particles(level, gate, t, tGlow);
            }
        } else {
            lastTick = -1;
        }
        // ease, so nothing snaps when a player walks in or out of range
        glow += (tGlow - glow) * 0.35F;
        warp += (tWarp - warp) * 0.35F;
        flash = tFlash > flash ? tFlash : flash + (tFlash - flash) * 0.5F;
        postTick(mc, player);
    }

    private static void sound(ClientLevel level, Vec3 at, SoundEvent sound, float volume, float pitch) {
        level.playLocalSound(at.x, at.y, at.z, sound, SoundSource.BLOCKS, volume, pitch, false);
    }

    /** Sounds and bursts tied to one tick of the opening. */
    private static void moment(ClientLevel level, Vec3 c, int t, float near) {
        int climax = GateAwakening.CLIMAX;
        if (t == 1) {
            sound(level, c, SoundEvents.BEACON_ACTIVATE, 2.0F, 0.7F);
        } else if (t == GateAwakening.WARP_START) {
            sound(level, c, SoundEvents.CONDUIT_ACTIVATE, 1.8F, 0.8F);
        } else if (t > GateAwakening.WARP_START && t < GateAwakening.IMPLODE_START && (t - GateAwakening.WARP_START) % 22 == 0) {
            // the hum climbs as the light gathers
            float k = (t - GateAwakening.WARP_START) / (float) (GateAwakening.IMPLODE_START - GateAwakening.WARP_START);
            sound(level, c, SoundEvents.BEACON_AMBIENT, 1.6F, 0.8F + 0.7F * k);
        } else if (t == GateAwakening.IMPLODE_START) {
            sound(level, c, SoundEvents.RESPAWN_ANCHOR_CHARGE, 2.0F, 0.55F);
            sound(level, c, SoundEvents.BEACON_POWER_SELECT, 1.6F, 0.6F);
        } else if (t == climax) {
            sound(level, c, SoundEvents.FIREWORK_ROCKET_LARGE_BLAST, 2.5F, 0.55F);
            sound(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 2.0F, 1.5F);
            if (near > 0.0F) {
                shockwave(level, c);
                Rumble.at(c, 0.9F, 36.0F, 26);
            }
        } else if (t == climax + 5) {
            sound(level, c, SoundEvents.FIREWORK_ROCKET_TWINKLE, 1.6F, 1.2F);
        } else if (t == climax + 12) {
            sound(level, c, SoundEvents.FIREWORK_ROCKET_TWINKLE_FAR, 1.4F, 0.9F);
        }
    }

    /** The shockwave: a flat ring of sparks racing out along the ground and a ball of light. */
    private static void shockwave(ClientLevel level, Vec3 c) {
        double floor = c.y - GateAwakening.clientSpan * 0.45 + 0.3;
        for (int i = 0; i < 72; i++) {
            double a = i * Math.PI * 2.0 / 72.0;
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            level.addParticle(ParticleTypes.END_ROD, c.x + cos * 0.8, floor, c.z + sin * 0.8, cos * 0.85, 0.02, sin * 0.85);
            if (i % 3 == 0) {
                level.addParticle(ParticleTypes.GLOW, c.x + cos, floor + 0.4, c.z + sin, cos * 0.6, 0.05, sin * 0.6);
            }
        }
        for (int j = 0; j < 40; j++) {
            double vx = RANDOM.nextGaussian() * 0.4;
            double vy = RANDOM.nextGaussian() * 0.4;
            double vz = RANDOM.nextGaussian() * 0.4;
            level.addParticle(ModParticles.STAR_SPARKLE.get(), c.x, c.y, c.z, vx, vy, vz);
        }
        level.addParticle(ModParticles.RESONANCE_RING.get(), c.x, floor, c.z, 6.0, 0.0, 0.0);
    }

    /** Cyan motes gathering on the gate, mist on the ground, and the rush inward before the flash. */
    private static void particles(ClientLevel level, Vec3 c, int t, float k) {
        int climax = GateAwakening.CLIMAX;
        if (t >= climax) {
            return;
        }
        double reach = 3.0 + GateAwakening.clientSpan * 0.6;
        int n = 1 + (int) (k * 5.0F);
        for (int i = 0; i < n; i++) {
            Vec3 d = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian() * 0.6, RANDOM.nextGaussian()).normalize();
            double r = reach + RANDOM.nextDouble() * 7.0;
            Vec3 p = c.add(d.scale(r));
            Vec3 v = d.scale(-(0.03 + 0.04 * k));
            int pick = RANDOM.nextInt(4);
            if (pick == 0) {
                level.addParticle(ModParticles.GLOW_DUST.get(), p.x, p.y, p.z, v.x, v.y, v.z);
            } else if (pick == 1) {
                level.addParticle(ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, v.x, v.y, v.z);
            } else {
                level.addParticle(ParticleTypes.GLOW, p.x, p.y, p.z, v.x, v.y, v.z);
            }
        }
        if (t % 3 == 0) {
            double ang = RANDOM.nextDouble() * Math.PI * 2.0;
            double rr = 2.0 + RANDOM.nextDouble() * (reach + 4.0);
            level.addParticle(ModParticles.SIFT_MIST.get(), c.x + Math.cos(ang) * rr, c.y - GateAwakening.clientSpan * 0.45, c.z + Math.sin(ang) * rr,
                    0.0, 0.004, 0.0);
        }
        if (t >= GateAwakening.IMPLODE_START) {
            // everything rushes into the gate
            for (int j = 0; j < 10; j++) {
                Vec3 dir = new Vec3(RANDOM.nextGaussian(), RANDOM.nextGaussian(), RANDOM.nextGaussian()).normalize();
                Vec3 from = c.add(dir.scale(6.0 + RANDOM.nextDouble() * 3.0));
                Vec3 vel = dir.scale(-0.32);
                level.addParticle(ParticleTypes.END_ROD, from.x, from.y, from.z, vel.x, vel.y, vel.z);
            }
        }
    }

    // ------------------------------------------------------------------ the post effect

    private static void postTick(Minecraft mc, LocalPlayer player) {
        float scale = distortion(mc);
        Identifier want = null;
        if (scale > 0.05F && postWorks()) {
            if (shockStep > 0) {
                want = SHOCK.get(shockStep - 1);
            } else {
                float w = warp * scale;
                if (w > 0.03F) {
                    want = WARP.get(Mth.clamp(Mth.ceil(w * WARP.size()), 1, WARP.size()) - 1);
                }
            }
        }
        // keep whatever post effects are already running (the server's, the Rainbow Daze) and swap ours
        List<Identifier> current = player.getActivePostEffects();
        List<Identifier> next = new ArrayList<>(current.size() + 1);
        for (Identifier id : current) {
            if (!WARP.contains(id) && !SHOCK.contains(id)) {
                next.add(id);
            }
        }
        if (want != null) {
            next.add(want);
        }
        if (!next.equals(current)) {
            player.setActivePostEffects(next);
        }
    }

    /** Compiles the warp shader once, the way a post chain would, without letting a failure reach the game. */
    private static boolean postWorks() {
        if (postWorks == null) {
            boolean ok;
            try {
                if (postCheck == null) {
                    postCheck = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                            .withVertexShader(TheSift.id("post/gate_warp"))
                            .withFragmentShader(TheSift.id("post/gate_warp"))
                            .withLocation(TheSift.id("pipeline/gate_warp_check"))
                            .withBindGroupLayout(BindGroupLayout.builder()
                                    .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                                    .withUniform("WarpConfig", UniformType.UNIFORM_BUFFER)
                                    .build())
                            .withColorTargetState(ColorTargetState.DEFAULT)
                            .build();
                }
                ok = RenderSystem.getCompiledPipelineNullable(postCheck) != null;
            } catch (RuntimeException e) {
                ok = false;
            }
            if (!ok) {
                TheSift.LOGGER.warn("Gate awakening: the warp shader is not available here, showing the glow and flash only");
            }
            postWorks = ok;
        }
        return postWorks;
    }

    // ------------------------------------------------------------------ the screen, the fog, the camera

    /** How bright the flash is right now (0-1); the portal renderer flares with it. */
    public static float flash(float partial) {
        return Mth.lerp(partial, flashO, flash);
    }

    private static void draw(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float k = Mth.lerp(partial, glowO, glow);
        float w0 = Mth.lerp(partial, warpO, warp);
        float f = flash(partial);
        if (k < 0.01F && f < 0.01F) {
            return;
        }
        int w = g.guiWidth();
        int h = g.guiHeight();
        float pulse = 0.85F + 0.15F * Mth.sin((mc.player.tickCount + partial) * 0.21F);
        // cyan light over everything, deepest at the edges of sight
        int tint = (int) (Mth.clamp(k * 0.16F + w0 * 0.06F, 0.0F, 0.3F) * 255.0F);
        if (tint > 0) {
            g.fill(0, 0, w, h, tint << 24 | 0x5CF5EE);
        }
        int va = (int) (Mth.clamp(k * 0.75F * pulse, 0.0F, 1.0F) * 255.0F);
        if (va > 0) {
            g.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256, va << 24 | 0x66FFF2);
        }
        // the flash: white at the heart of it, never quite opaque
        float strength = 0.5F + 0.4F * distortion(mc);
        int fa = (int) (Mth.clamp(f * strength, 0.0F, 0.92F) * 255.0F);
        if (fa > 0) {
            g.fill(0, 0, w, h, fa << 24 | 0xEFFFFF);
        }
    }

    private static void onFogColor(ViewportEvent.ComputeFogColor event) {
        float partial = (float) event.getPartialTick();
        float k = Mth.lerp(partial, glowO, glow) * 0.65F;
        float f = flash(partial);
        if (k <= 0.0F && f <= 0.0F) {
            return;
        }
        event.setRed(Mth.lerp(f, Mth.lerp(k, event.getRed(), 0.45F), 0.95F));
        event.setGreen(Mth.lerp(f, Mth.lerp(k, event.getGreen(), 1.0F), 1.0F));
        event.setBlue(Mth.lerp(f, Mth.lerp(k, event.getBlue(), 0.95F), 1.0F));
    }

    /** The view narrows as the world is drawn in, and kicks wide with the flash. */
    private static void onFov(ViewportEvent.ComputeFov event) {
        float partial = (float) event.getPartialTick();
        float w0 = Mth.lerp(partial, warpO, warp);
        float f = flash(partial);
        if (w0 < 0.01F && f < 0.01F) {
            return;
        }
        float scale = distortion(Minecraft.getInstance());
        event.setFOV(event.getFOV() * (1.0F + (f * 0.12F - w0 * 0.08F) * scale));
    }

    /** A slow, uneasy roll while the world bends. */
    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft mc = Minecraft.getInstance();
        float partial = (float) event.getPartialTick();
        float w0 = Mth.lerp(partial, warpO, warp) * distortion(mc);
        if (w0 < 0.01F || mc.player == null) {
            return;
        }
        float time = mc.player.tickCount + partial;
        event.setRoll(event.getRoll() + Mth.sin(time * 0.09F) * 1.6F * w0);
    }
}
