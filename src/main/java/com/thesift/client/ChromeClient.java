package com.thesift.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.ColorTargetState;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.thesift.TheSift;
import com.thesift.client.particle.ChromeChord;
import com.thesift.client.particle.ChromeParticle;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModFluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * A3 Chrome on the client: the rainbow that rolls across every Chrome lake, the fog inside it, the
 * ripples behind anything that swims or wades, Chrome's particles and the Rainbow Daze.
 *
 * <p>The Daze is a real post effect (assets/thesift/post_effect/rainbow_daze_1..4, shader
 * thesift:post/rainbow_daze) stepped through four strengths as the effect fades in and out, added
 * to the player's active post effects next to any the server set. The shader is test-compiled
 * first; if a graphics card refuses it the daze still shows as the screen overlay, sway and FOV
 * wobble, which always play. Everything scales with the "Distortion Effects" accessibility slider.
 */
public final class ChromeClient {
    /** One loop of Chrome's colour in ticks: the fluid textures' 40 frames x 3 ticks. */
    public static final float HUE_LOOP = 120.0F;
    private static final Identifier VIGNETTE = TheSift.id("textures/misc/rainbow_daze_vignette.png");
    private static final Identifier SWIRL = TheSift.id("textures/misc/rainbow_daze_swirl.png");
    private static final List<Identifier> POST = List.of(TheSift.id("rainbow_daze_1"), TheSift.id("rainbow_daze_2"), TheSift.id("rainbow_daze_3"),
            TheSift.id("rainbow_daze_4"));
    private static final RandomSource RANDOM = RandomSource.create();
    private static @Nullable Boolean postWorks;
    private static @Nullable RenderPipeline postCheck;

    /**
     * Chrome's tint: a slow rainbow gradient across the world, baked per block when a chunk is meshed.
     * The fluid textures' own colour turns round the wheel every few seconds; where it lines up with
     * this tint the surface shines in vivid colour, so bands of colour sweep across connected Chrome.
     */
    public static final FluidTintSource TINT = new FluidTintSource() {
        @Override
        public int color(FluidState state) {
            return 0xFFFFFFFF;
        }

        @Override
        public int colorInWorld(FluidState fluidState, BlockState blockState, BlockAndTintGetter level, BlockPos pos) {
            return 0xFF000000 | hsv(tintHue(pos.getX(), pos.getY(), pos.getZ()), 0.45F, 1.0F);
        }
    };

    private ChromeClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(ChromeClient::registerParticles);
        modBus.addListener(ChromeClient::registerOverlays);
        NeoForge.EVENT_BUS.addListener(ChromeClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(ChromeClient::onCameraAngles);
        NeoForge.EVENT_BUS.addListener(ChromeClient::onFov);
    }

    private static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ModChrome.CHROME_RIPPLE.get(), sprites -> new ChromeParticle.Provider(ChromeParticle.Kind.RIPPLE, sprites));
        event.registerSpriteSet(ModChrome.CHROME_SPARK.get(), sprites -> new ChromeParticle.Provider(ChromeParticle.Kind.SPARK, sprites));
        event.registerSpriteSet(ModChrome.RAINBOW_MOTE.get(), sprites -> new ChromeParticle.Provider(ChromeParticle.Kind.MOTE, sprites));
        event.registerSpriteSet(ModChrome.CHROME_CHORD.get(), ChromeChord.Provider::new);
    }

    private static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TheSift.id("rainbow_daze"), ChromeClient::drawDaze);
    }

    // ------------------------------------------------------------------ colour

    static float tintHue(double x, double y, double z) {
        return (float) (x / 43.0 + z / 61.0 + y / 37.0 + 0.07 * Math.sin(x * 0.09 + z * 0.03) + 0.07 * Math.cos(z * 0.11 - x * 0.02));
    }

    /** 0xRRGGBB for a hue (wraps round), saturation and value, all 0-1. */
    public static int hsv(float hue, float sat, float val) {
        float h = (hue - Mth.floor(hue)) * 6.0F;
        int sector = Math.min(5, (int) h);
        float f = h - sector;
        float p = val * (1.0F - sat);
        float q = val * (1.0F - f * sat);
        float t = val * (1.0F - (1.0F - f) * sat);
        float r;
        float g;
        float b;
        switch (sector) {
            case 0 -> {
                r = val;
                g = t;
                b = p;
            }
            case 1 -> {
                r = q;
                g = val;
                b = p;
            }
            case 2 -> {
                r = p;
                g = val;
                b = t;
            }
            case 3 -> {
                r = p;
                g = q;
                b = val;
            }
            case 4 -> {
                r = t;
                g = p;
                b = val;
            }
            default -> {
                r = val;
                g = p;
                b = q;
            }
        }
        return (int) (r * 255.0F) << 16 | (int) (g * 255.0F) << 8 | (int) (b * 255.0F);
    }

    /** The fog when your eyes are in Chrome: its colour turns in step with the surface's. */
    public static void fogColor(Camera camera, ClientLevel level, float partialTick, Vector4f color) {
        Vec3 cam = camera.position();
        float hue = tintHue(cam.x, cam.y, cam.z) + ((level.getGameTime() % 24000L) + partialTick) / HUE_LOOP;
        int rgb = hsv(hue, 0.42F, 0.92F);
        color.set(((rgb >> 16) & 255) / 255.0F, ((rgb >> 8) & 255) / 255.0F, (rgb & 255) / 255.0F, 1.0F);
    }

    // ------------------------------------------------------------------ ripples

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.isPaused()) {
            return;
        }
        ripples(mc.level, mc.player);
        dazeTick(mc, mc.player);
    }

    private static boolean isChrome(ClientLevel level, BlockPos pos) {
        return level.getFluidState(pos).getType().isSame(ModFluids.CHROME.get());
    }

    /** Anything moving in or on Chrome leaves rainbow rings spreading over its surface. */
    private static void ripples(ClientLevel level, LocalPlayer player) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (Entity e : level.entitiesForRendering()) {
            if (e.isSpectator() || e.distanceToSqr(player) > 32.0 * 32.0) {
                continue;
            }
            double dx = e.getX() - e.xo;
            double dz = e.getZ() - e.zo;
            double speed = dx * dx + dz * dz;
            if (speed < 4.0E-4) {
                continue;
            }
            p.set(e.getX(), e.getY() + 0.1, e.getZ());
            if (!isChrome(level, p)) {
                continue;
            }
            int every = speed > 0.04 ? 2 : speed > 0.01 ? 3 : 5;
            if ((e.tickCount + e.getId()) % every != 0) {
                continue;
            }
            int up = 0;
            while (up < 4 && isChrome(level, p.above())) {
                p.move(0, 1, 0);
                up++;
            }
            double top = p.getY() + level.getFluidState(p).getHeight(level, p);
            float hue = tintHue(e.getX(), top, e.getZ()) + (level.getGameTime() % 24000L) / HUE_LOOP;
            if (isChrome(level, p.above()) || top > e.getY() + e.getBbHeight() + 1.5) {
                // deep down: a trail of rising twinkles instead
                level.addParticle(ModChrome.CHROME_SPARK.get(), e.getRandomX(0.5), e.getRandomY(), e.getRandomZ(0.5), hue, 0.03, 0.0);
                continue;
            }
            level.addParticle(ModChrome.CHROME_RIPPLE.get(), e.getX(), top + 0.02, e.getZ(), hue, e.getBbWidth(), 0.0);
            if (speed > 0.02 && RANDOM.nextInt(3) == 0) {
                level.addParticle(ModChrome.CHROME_SPARK.get(), e.getX() + (RANDOM.nextDouble() - 0.5) * e.getBbWidth(), top + 0.05,
                        e.getZ() + (RANDOM.nextDouble() - 0.5) * e.getBbWidth(), hue + 0.1F, 0.08, 0.0);
            }
        }
    }

    // ------------------------------------------------------------------ the Rainbow Daze

    private static float distortion(Minecraft mc) {
        return mc.options.screenEffectScale().get().floatValue();
    }

    private static float daze(float partial) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return 0.0F;
        }
        return mc.player.getEffectBlendFactor(ModChrome.RAINBOW_DAZE, partial) * distortion(mc);
    }

    private static void dazeTick(Minecraft mc, LocalPlayer player) {
        float k = player.getEffectBlendFactor(ModChrome.RAINBOW_DAZE, 1.0F) * distortion(mc);
        int step = k < 0.02F ? 0 : Mth.clamp(Mth.ceil(k * POST.size()), 1, POST.size());
        if (step > 0 && !postWorks()) {
            step = 0;
        }
        // keep whatever post effects the server set, and swap ours in or out
        List<Identifier> current = player.getActivePostEffects();
        List<Identifier> next = new ArrayList<>(current.size() + 1);
        for (Identifier id : current) {
            if (!POST.contains(id)) {
                next.add(id);
            }
        }
        if (step > 0) {
            next.add(POST.get(step - 1));
        }
        if (!next.equals(current)) {
            player.setActivePostEffects(next);
        }
        // rainbow motes drifting through your sight
        if (k > 0.2F && RANDOM.nextFloat() < k * 0.6F) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            double d = 1.5 + RANDOM.nextDouble() * 3.5;
            mc.level.addParticle(ModChrome.RAINBOW_MOTE.get(), eye.x + look.x * d + (RANDOM.nextDouble() - 0.5) * 4.0,
                    eye.y + look.y * d + (RANDOM.nextDouble() - 0.5) * 2.5, eye.z + look.z * d + (RANDOM.nextDouble() - 0.5) * 4.0, 0.0, 0.0, 0.0);
        }
    }

    /** Compiles the daze shader once, the way a post chain would, without letting a failure reach the game. */
    private static boolean postWorks() {
        if (postWorks == null) {
            boolean ok;
            try {
                if (postCheck == null) {
                    postCheck = RenderPipeline.builder(RenderPipelines.POST_PROCESSING_SNIPPET)
                            .withVertexShader(TheSift.id("post/rainbow_daze"))
                            .withFragmentShader(TheSift.id("post/rainbow_daze"))
                            .withLocation(TheSift.id("pipeline/rainbow_daze_check"))
                            .withBindGroupLayout(BindGroupLayout.builder()
                                    .withUniform("InSampler", UniformType.COMBINED_IMAGE_SAMPLER)
                                    .withUniform("SamplerInfo", UniformType.UNIFORM_BUFFER)
                                    .withUniform("DazeConfig", UniformType.UNIFORM_BUFFER)
                                    .build())
                            .withColorTargetState(ColorTargetState.DEFAULT)
                            .build();
                }
                ok = RenderSystem.getCompiledPipelineNullable(postCheck) != null;
            } catch (RuntimeException e) {
                ok = false;
            }
            if (!ok) {
                TheSift.LOGGER.warn("Rainbow Daze: the post effect shader is not available here, showing the screen overlay only");
            }
            postWorks = ok;
        }
        return postWorks;
    }

    private static void drawDaze(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }
        float partial = delta.getGameTimeDeltaPartialTick(false);
        float k = daze(partial);
        if (k < 0.01F) {
            return;
        }
        float time = mc.player.tickCount + partial;
        int w = g.guiWidth();
        int h = g.guiHeight();
        // a soft rainbow vignette whose colour slowly turns
        int va = (int) (k * 120.0F * (0.8F + 0.2F * Mth.sin(time * 0.11F)));
        g.blit(RenderPipelines.GUI_TEXTURED, VIGNETTE, 0, 0, 0.0F, 0.0F, w, h, 256, 256, 256, 256, va << 24 | hsv(time / HUE_LOOP, 0.65F, 1.0F));
        // and a rainbow swirl turning round the edge of your sight
        float size = Mth.sqrt((float) (w * w + h * h)) * 1.1F;
        var pose = g.pose();
        pose.pushMatrix();
        pose.translate(w * 0.5F, h * 0.5F);
        pose.rotate(time * 0.006F);
        pose.scale(size / 256.0F, size / 256.0F);
        g.blit(RenderPipelines.GUI_TEXTURED, SWIRL, -128, -128, 0.0F, 0.0F, 256, 256, 256, 256, 256, 256, (int) (k * 95.0F) << 24 | 0xFFFFFF);
        pose.popMatrix();
    }

    /** A gentle, seasick sway. */
    private static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        float k = daze(event.getPartialTick());
        if (k < 0.01F || Minecraft.getInstance().player == null) {
            return;
        }
        float t = Minecraft.getInstance().player.tickCount + event.getPartialTick();
        event.setRoll(event.getRoll() + (Mth.sin(t * 0.045F) * 3.5F + Mth.sin(t * 0.13F) * 0.8F) * k);
        event.setPitch(event.getPitch() + Mth.sin(t * 0.06F + 1.0F) * 1.2F * k);
        event.setYaw(event.getYaw() + Mth.cos(t * 0.04F) * 1.5F * k);
    }

    /** The world breathes in and out. */
    private static void onFov(ViewportEvent.ComputeFov event) {
        float k = daze(event.getPartialTick());
        if (k < 0.01F || Minecraft.getInstance().player == null) {
            return;
        }
        float t = Minecraft.getInstance().player.tickCount + event.getPartialTick();
        event.setFOV(event.getFOV() * (1.0F + (0.035F * Mth.sin(t * 0.07F) + 0.015F * Mth.sin(t * 0.19F)) * k));
    }
}
