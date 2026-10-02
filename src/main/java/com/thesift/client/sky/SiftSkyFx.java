package com.thesift.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.thesift.TheSift;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModDimensions;
import com.thesift.registry.ModParticles;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * B1 Portal & sky FX: what moves across the Sift's sky, drawn by {@link SiftSkyRenderer} after the
 * nebula, in the same pass and with the same textured, translucent sky pipeline.
 *
 * <ul>
 *   <li><b>rainbows</b> - a soft bow (with a faint, reversed second bow outside it) stands over the
 *   horizon as each dream cycle dawns and fades, and a bright one follows every rain;</li>
 *   <li><b>aurora ribbons</b> - in the cycle's blue hour three curtains of violet and emerald light
 *   sway slowly across the north;</li>
 *   <li><b>colour clouds</b> - seven pastel clouds circle the sky, very slowly;</li>
 *   <li><b>shooting stars</b> - now and then, far more often in the blue hour.</li>
 * </ul>
 *
 * <p>All geometry is built once; everything that moves does so through the transform and colour
 * of each draw. The ambient motes under the sky (rainbow motes beneath a bow, glow-dust fireflies in
 * the blue hour, drifting pollen and high sparkles) are spawned from the client tick.</p>
 */
public final class SiftSkyFx {
    private static final Identifier RAINBOW = TheSift.id("textures/environment/sift_rainbow.png");
    private static final Identifier RAINBOW_OUTER = TheSift.id("textures/environment/sift_rainbow_outer.png");
    private static final Identifier AURORA = TheSift.id("textures/environment/sift_aurora.png");
    private static final Identifier CLOUDS = TheSift.id("textures/environment/sift_colour_clouds.png");
    private static final Identifier STAR = TheSift.id("textures/environment/sift_shooting_star.png");
    /** The dream cycle (data/thesift/world_clock + timeline dream_cycle): 12000 ticks. */
    private static final float CYCLE = 12000.0F;
    private static final int ARC_SEGMENTS = 32;
    private static final int AURORA_SEGMENTS = 40;
    /** The most indices one draw here needs (a quad is 6); the sky renderer sizes its index buffer to fit. */
    public static final int MAX_INDICES = Math.max(ARC_SEGMENTS, AURORA_SEGMENTS) * 2 * 6;
    private static final float[][] CLOUD_TINTS = {{1.0F, 0.82F, 0.9F}, {0.86F, 0.8F, 1.0F}, {1.0F, 0.9F, 0.78F}, {0.8F, 1.0F, 0.9F},
            {1.0F, 0.86F, 0.96F}, {0.84F, 0.92F, 1.0F}, {1.0F, 0.95F, 0.86F}};
    private static final RandomSource RANDOM = RandomSource.create();

    private static @Nullable GpuBuffer arc;
    private static @Nullable GpuBuffer outerArc;
    private static @Nullable GpuBuffer aurora;
    private static @Nullable GpuBuffer star;
    private static final GpuBuffer[] CLOUD_QUADS = new GpuBuffer[4];

    private static float rainbow;
    private static float rainbowO;
    private static float ribbons;
    private static float ribbonsO;
    private static int afterRain;
    private static boolean wasRaining;
    private static final List<ShootingStar> SHOOTING = new ArrayList<>();

    private record ShootingStar(float yaw, float pitch, float heading, int life, int born) {}

    private SiftSkyFx() {
    }

    // ------------------------------------------------------------------ the clock

    /** 1 at {@code centre} of the dream cycle, easing to 0 at {@code width} ticks either side (wraps round). */
    private static float bell(float cycle, float centre, float width) {
        float d = Math.abs(cycle - centre);
        d = Math.min(d, CYCLE - d) / width;
        if (d >= 1.0F) {
            return 0.0F;
        }
        float k = 1.0F - d * d;
        return k * k;
    }

    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        LocalPlayer player = mc.player;
        rainbowO = rainbow;
        ribbonsO = ribbons;
        if (level == null || player == null || !ModDimensions.isSift(level)) {
            SHOOTING.clear();
            return;
        }
        if (mc.isPaused()) {
            return;
        }
        long time = level.getGameTime();
        float cycle = time % (long) CYCLE;
        float rain = level.getRainLevel(1.0F);
        // a bright bow follows every rain
        if (rain > 0.4F) {
            wasRaining = true;
        } else if (wasRaining && rain < 0.15F) {
            wasRaining = false;
            afterRain = 2400;
        }
        if (afterRain > 0) {
            afterRain--;
        }
        float dawnDusk = Math.max(bell(cycle, 1500.0F, 1500.0F), bell(cycle, 6600.0F, 1500.0F)) * 0.5F;
        float fresh = Math.min(1.0F, afterRain / 500.0F) * 0.9F;
        float targetBow = Math.max(dawnDusk, fresh) * (1.0F - rain);
        float targetRibbons = bell(cycle, 10000.0F, 1700.0F) * 0.85F * (1.0F - rain);
        rainbow += (targetBow - rainbow) * 0.02F;
        ribbons += (targetRibbons - ribbons) * 0.02F;
        // shooting stars
        int now = (int) (time % 1000000L);
        SHOOTING.removeIf(s -> now - s.born() > s.life() || now < s.born());
        if (SHOOTING.size() < 3 && RANDOM.nextFloat() < (0.002F + 0.03F * ribbons) * (1.0F - rain)) {
            SHOOTING.add(new ShootingStar(RANDOM.nextFloat() * Mth.TWO_PI, 0.35F + RANDOM.nextFloat() * 0.6F,
                    -0.5F - RANDOM.nextFloat() * 0.9F, 14 + RANDOM.nextInt(14), now));
        }
        ambient(level, player, rain);
    }

    /** A few motes under the open sky; never many. */
    private static void ambient(ClientLevel level, LocalPlayer player, float rain) {
        if (!level.canSeeSky(player.blockPosition()) || rain > 0.5F) {
            return;
        }
        double px = player.getX();
        double py = player.getY();
        double pz = player.getZ();
        if (rainbow > 0.2F && RANDOM.nextFloat() < rainbow * 0.35F) {
            level.addParticle(ModChrome.RAINBOW_MOTE.get(), px + (RANDOM.nextDouble() - 0.5) * 24.0, py + 1.0 + RANDOM.nextDouble() * 6.0,
                    pz + (RANDOM.nextDouble() - 0.5) * 24.0, 0.0, 0.0, 0.0);
        }
        if (ribbons > 0.25F && RANDOM.nextFloat() < ribbons * 0.5F) {
            level.addParticle(ModParticles.GLOW_DUST.get(), px + (RANDOM.nextDouble() - 0.5) * 28.0, py - 1.0 + RANDOM.nextDouble() * 4.0,
                    pz + (RANDOM.nextDouble() - 0.5) * 28.0, 0.0, 0.01, 0.0);
        }
        if (RANDOM.nextFloat() < 0.1F) {
            level.addParticle(ModParticles.DREAM_POLLEN.get(), px + (RANDOM.nextDouble() - 0.5) * 20.0, py + RANDOM.nextDouble() * 5.0,
                    pz + (RANDOM.nextDouble() - 0.5) * 20.0, 0.01, 0.0, 0.005);
        }
        if (RANDOM.nextFloat() < 0.04F) {
            level.addParticle(ModParticles.STAR_SPARKLE.get(), px + (RANDOM.nextDouble() - 0.5) * 30.0, py + 8.0 + RANDOM.nextDouble() * 10.0,
                    pz + (RANDOM.nextDouble() - 0.5) * 30.0, 0.0, 0.0, 0.0);
        }
    }

    // ------------------------------------------------------------------ geometry (built once)

    private static GpuBuffer upload(String name, BufferBuilder b) {
        try (MeshData mesh = b.buildOrThrow()) {
            return RenderSystem.getDevice().createBuffer(() -> name, 40, mesh.vertexBuffer());
        }
    }

    /** Both windings of a quad, so culling can never hide it. */
    private static void quad(BufferBuilder b, float[][] p, float[][] uv) {
        for (int winding = 0; winding < 2; winding++) {
            for (int k = 0; k < 4; k++) {
                int i = winding == 0 ? k : 3 - k;
                b.addVertex(p[i][0], p[i][1], p[i][2]).setUv(uv[i][0], uv[i][1]).setColor(-1);
            }
        }
    }

    /** A rainbow band from radius r0 (inside) to r1 (outside), standing at distance 1.15 in front. */
    private static GpuBuffer arc(String name, float r0, float r1) {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(ARC_SEGMENTS * 8 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            for (int s = 0; s < ARC_SEGMENTS; s++) {
                float a0 = Mth.PI * s / ARC_SEGMENTS;
                float a1 = Mth.PI * (s + 1) / ARC_SEGMENTS;
                float u0 = s / (float) ARC_SEGMENTS;
                float u1 = (s + 1) / (float) ARC_SEGMENTS;
                quad(b, new float[][] {{Mth.cos(a0) * r1, Mth.sin(a0) * r1, -1.15F}, {Mth.cos(a1) * r1, Mth.sin(a1) * r1, -1.15F},
                        {Mth.cos(a1) * r0, Mth.sin(a1) * r0, -1.15F}, {Mth.cos(a0) * r0, Mth.sin(a0) * r0, -1.15F}},
                        new float[][] {{u0, 0.0F}, {u1, 0.0F}, {u1, 1.0F}, {u0, 1.0F}});
            }
            return upload(name, b);
        }
    }

    /** A wavy curtain hanging across the northern sky, from 18 to 50 degrees up. */
    private static GpuBuffer curtain() {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(AURORA_SEGMENTS * 8 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            float span = 70.0F * Mth.DEG_TO_RAD;
            float low = 18.0F * Mth.DEG_TO_RAD;
            float high = 50.0F * Mth.DEG_TO_RAD;
            for (int s = 0; s < AURORA_SEGMENTS; s++) {
                float f0 = -span + 2.0F * span * s / AURORA_SEGMENTS;
                float f1 = -span + 2.0F * span * (s + 1) / AURORA_SEGMENTS;
                // the hem waves, the top drifts the other way: the curtain folds
                float b0 = f0 + 0.08F * Mth.sin(f0 * 6.0F);
                float b1 = f1 + 0.08F * Mth.sin(f1 * 6.0F);
                float t0 = f0 - 0.05F * Mth.sin(f0 * 4.0F + 1.0F);
                float t1 = f1 - 0.05F * Mth.sin(f1 * 4.0F + 1.0F);
                float u0 = 3.0F * s / AURORA_SEGMENTS;
                float u1 = 3.0F * (s + 1) / AURORA_SEGMENTS;
                quad(b, new float[][] {dir(t0, high), dir(t1, high), dir(b1, low), dir(b0, low)},
                        new float[][] {{u0, 0.0F}, {u1, 0.0F}, {u1, 1.0F}, {u0, 1.0F}});
            }
            return upload("Sift aurora", b);
        }
    }

    private static float[] dir(float heading, float elevation) {
        float c = Mth.cos(elevation);
        return new float[] {Mth.sin(heading) * c, Mth.sin(elevation), -Mth.cos(heading) * c};
    }

    private static GpuBuffer flatQuad(String name, float halfHeight, float u0, float v0, float u1, float v1) {
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(8 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            quad(b, new float[][] {{-1.0F, halfHeight, -1.0F}, {1.0F, halfHeight, -1.0F}, {1.0F, -halfHeight, -1.0F}, {-1.0F, -halfHeight, -1.0F}},
                    new float[][] {{u0, v0}, {u1, v0}, {u1, v1}, {u0, v1}});
            return upload(name, b);
        }
    }

    /** Builds the geometry the first time; called before the sky's render pass opens. */
    public static void prepare() {
        if (arc == null) {
            arc = arc("Sift rainbow", 0.86F, 1.0F);
            outerArc = arc("Sift outer rainbow", 1.1F, 1.2F);
            aurora = curtain();
            star = flatQuad("Sift shooting star", 0.012F, 0.0F, 0.0F, 1.0F, 1.0F);
            for (int i = 0; i < 4; i++) {
                float u = (i % 2) * 0.5F;
                float v = (i / 2) * 0.5F;
                CLOUD_QUADS[i] = flatQuad("Sift colour cloud", 0.5F, u, v, u + 0.5F, v + 0.5F);
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    private static void draw(RenderPass pass, @Nullable GpuBuffer mesh, int quads, Identifier texture, Matrix4f transform, float r, float g, float b, float a) {
        if (mesh == null || a <= 0.003F) {
            return;
        }
        AbstractTexture tex = Minecraft.getInstance().getTextureManager().getTexture(texture);
        GpuBufferSlice slice = RenderSystem.getDynamicUniforms().writeTransform(transform, new Vector4f(r, g, b, a));
        pass.setUniform("DynamicTransforms", slice);
        pass.setUniform("Sampler0", tex.getTextureView(), tex.getSampler());
        pass.setVertexBuffer(0, mesh.slice());
        pass.drawIndexed(quads * 2 * 6, 1, 0, 0, 0);
    }

    /** Draws everything above the nebula. The pass already has the sky pipeline and an index buffer of {@link #MAX_INDICES}. */
    public static void render(RenderPass pass, Matrix4f view) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return;
        }
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        float time = (level.getGameTime() % 1000000L) + partial;
        float rain = level.getRainLevel(partial);
        // colour clouds, circling very slowly
        for (int i = 0; i < 7; i++) {
            float yaw = i * Mth.TWO_PI / 7.0F + time * 0.00002F * (1 + i % 3);
            float pitch = 0.16F + 0.06F * ((i * 37) % 5);
            float size = 0.2F + 0.035F * ((i * 13) % 4);
            float[] tint = CLOUD_TINTS[i];
            Matrix4f cloud = new Matrix4f(view).rotateY(yaw).rotateX(pitch).rotateZ(0.08F * Mth.sin(i * 1.7F)).scale(100.0F).scale(size * 1.6F, size, 1.0F);
            draw(pass, CLOUD_QUADS[i % 4], 1, CLOUDS, cloud, tint[0], tint[1], tint[2], 0.5F * (1.0F - rain * 0.7F));
        }
        // the rainbow (and its fainter, reversed twin) over the horizon, a new heading each cycle
        float bow = Mth.lerp(partial, rainbowO, rainbow);
        if (bow > 0.003F) {
            float heading = (level.getGameTime() / (long) CYCLE % 7L) * 0.9F;
            Matrix4f bowAt = new Matrix4f(view).rotateY(heading).rotateX(-0.1F).scale(100.0F);
            draw(pass, arc, ARC_SEGMENTS, RAINBOW, bowAt, 1.0F, 1.0F, 1.0F, bow * 0.8F);
            draw(pass, outerArc, ARC_SEGMENTS, RAINBOW_OUTER, bowAt, 1.0F, 1.0F, 1.0F, bow * 0.3F);
        }
        // aurora ribbons in the blue hour
        float k = Mth.lerp(partial, ribbonsO, ribbons);
        if (k > 0.003F) {
            for (int i = 0; i < 3; i++) {
                float sway = Mth.sin(time * 0.004F + i * 2.1F) * 0.22F + i * 0.55F - 0.55F;
                float lift = 1.0F + 0.08F * Mth.sin(time * 0.006F + i);
                float pulse = 0.7F + 0.3F * Mth.sin(time * 0.02F + i * 1.3F);
                Matrix4f ribbon = new Matrix4f(view).rotateY(sway).scale(100.0F).scale(1.0F, lift, 1.0F);
                float red = i == 1 ? 1.0F : 0.85F;
                float blue = i == 2 ? 1.0F : 0.9F;
                draw(pass, aurora, AURORA_SEGMENTS, AURORA, ribbon, red, 1.0F, blue, k * pulse * (i == 0 ? 0.9F : 0.65F));
            }
        }
        // shooting stars
        int now = (int) (level.getGameTime() % 1000000L);
        for (ShootingStar s : SHOOTING) {
            float age = (now - s.born() + partial) / s.life();
            if (age < 0.0F || age > 1.0F) {
                continue;
            }
            float alpha = Mth.sin(age * Mth.PI);
            Matrix4f streak = new Matrix4f(view).rotateY(s.yaw()).rotateX(s.pitch()).rotateZ(s.heading()).scale(100.0F)
                    .translate(age * 0.5F - 0.25F, 0.0F, 0.0F).scale(0.08F, 1.0F, 1.0F);
            draw(pass, star, 1, STAR, streak, 1.0F, 1.0F, 1.0F, alpha);
        }
    }
}
