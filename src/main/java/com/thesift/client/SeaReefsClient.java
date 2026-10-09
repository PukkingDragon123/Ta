package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.registry.ModSeaReefs;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector4f;

/**
 * W-sea on the client: how Chrome looks in each sea.
 *
 * <ul>
 *   <li>Chrome's rainbow tint is turned towards each sea's own look: a clear, sparkling green-copper in the Chrome Coral
 *   Ocean, and the water's own colour in and next to the water seas, so where Chrome sinks under a water sea it takes the
 *   water's colour and the two meet without a seam. Two biome colour resolvers carry the look and its strength; the game
 *   blends them across biome edges like water colour.</li>
 *   <li>Chrome's fog is far thinner than it was (56 blocks; 128 in the Chrome Coral Ocean, where it starts a few blocks
 *   out) and its colour eases towards the same look as you move between seas.</li>
 * </ul>
 */
public final class SeaReefsClient {
    /** Each sea's look: RGB and how strongly it replaces Chrome's rainbow (0-255). Everywhere else Chrome keeps its rainbow. */
    private static final Map<Identifier, int[]> LOOKS = Map.of(
            TheSift.id("chrome_coral_ocean"), new int[]{0x4FE0A2, 180},
            TheSift.id("magic_kelp_forest"), new int[]{0x3FE6D6, 255},
            TheSift.id("brass_coral_reef"), new int[]{0x3FDCC4, 255},
            TheSift.id("deep_dark_ocean"), new int[]{0x0D5A63, 255});
    private static final int[] NONE = {0xFFFFFF, 0};
    private static final Map<Biome, int[]> CACHE = new ConcurrentHashMap<>();
    private static ClientLevel cachedFor;

    /** The look premultiplied by its strength (blends correctly across biome edges). */
    public static final ColorResolver LOOK = (biome, x, z) -> {
        int[] l = look(biome);
        int w = l[1];
        return (((l[0] >> 16) & 255) * w / 255) << 16 | (((l[0] >> 8) & 255) * w / 255) << 8 | ((l[0] & 255) * w / 255);
    };
    /** The look's strength, as a grey. */
    public static final ColorResolver WEIGHT = (biome, x, z) -> {
        int w = look(biome)[1];
        return w << 16 | w << 8 | w;
    };

    /** The Chrome fog's look, eased tick by tick towards the sea the camera is in: r, g, b (0-1) and strength. */
    private static final float[] FOG = {1.0F, 1.0F, 1.0F, 0.0F};
    /** 0-1: how far into the Chrome Coral Ocean's clear water the camera is. */
    private static float clarity;

    private SeaReefsClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SeaReefsClient::registerResolvers);
        NeoForge.EVENT_BUS.addListener(SeaReefsClient::onClientTick);
    }

    private static void registerResolvers(RegisterColorHandlersEvent.ColorResolvers event) {
        event.register(LOOK);
        event.register(WEIGHT);
    }

    private static int[] look(Biome biome) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) {
            return NONE;
        }
        if (cachedFor != level) {
            CACHE.clear();
            cachedFor = level;
        }
        return CACHE.computeIfAbsent(biome, b -> {
            Identifier id = level.registryAccess().lookupOrThrow(Registries.BIOME).getKey(b);
            int[] l = id == null ? null : LOOKS.get(id);
            return l == null ? NONE : l;
        });
    }

    private static int[] look(Holder<Biome> biome) {
        return biome.unwrapKey().map(k -> LOOKS.getOrDefault(k.identifier(), NONE)).orElse(NONE);
    }

    /** Chrome's colour at a block: its rainbow ({@code rainbow}, 0xRRGGBB) turned towards the sea's look. */
    public static int chromeTint(BlockAndTintGetter level, BlockPos pos, int rainbow) {
        int w = level.getBlockTint(pos, WEIGHT) & 255;
        if (w <= 2) {
            return rainbow & 0xFFFFFF;
        }
        int look = level.getBlockTint(pos, LOOK);
        float keep = 1.0F - w / 255.0F;
        int r = Math.min(255, Math.round(((rainbow >> 16) & 255) * keep) + ((look >> 16) & 255));
        int g = Math.min(255, Math.round(((rainbow >> 8) & 255) * keep) + ((look >> 8) & 255));
        int b = Math.min(255, Math.round((rainbow & 255) * keep) + (look & 255));
        return r << 16 | g << 8 | b;
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (mc.level == null || player == null) {
            return;
        }
        Holder<Biome> here = mc.level.getBiome(player.blockPosition());
        int[] l = look(here);
        float k = 0.04F;
        FOG[0] = Mth.lerp(k, FOG[0], ((l[0] >> 16) & 255) / 255.0F);
        FOG[1] = Mth.lerp(k, FOG[1], ((l[0] >> 8) & 255) / 255.0F);
        FOG[2] = Mth.lerp(k, FOG[2], (l[0] & 255) / 255.0F);
        FOG[3] = Mth.lerp(k, FOG[3], l[1] / 255.0F);
        clarity = Mth.clamp(clarity + (here.is(ModSeaReefs.CHROME_CORAL_OCEAN) ? 0.01F : -0.01F), 0.0F, 1.0F);
    }

    /** The fog colour inside Chrome (its turning rainbow, {@code color}) turned towards the sea's look. */
    public static void chromeFogColor(Vector4f color) {
        float w = FOG[3];
        if (w <= 0.01F) {
            return;
        }
        color.set(Mth.lerp(w, color.x(), FOG[0]), Mth.lerp(w, color.y(), FOG[1]), Mth.lerp(w, color.z(), FOG[2]), color.w());
    }

    /** How far you see inside Chrome: much further than before, and far further still in the Chrome Coral Ocean. */
    public static void chromeFog(FogData fog) {
        fog.environmentalStart = Mth.lerp(clarity, 0.0F, 6.0F);
        fog.environmentalEnd = Mth.lerp(clarity, 56.0F, 128.0F);
    }
}
