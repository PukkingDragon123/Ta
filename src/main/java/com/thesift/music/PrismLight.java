package com.thesift.music;

import com.thesift.registry.ModParticles;
import com.thesift.world.TemporaryBlocks;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * M1 instrument play: the four colours of light a Prism instrument plays its notes in - Rose,
 * Amber, Cyan and Violet. A Prism song ({@link Song#prism()}) asks for a colour with every note; the
 * play screens pick the colour with the keys 1-4 or the mouse wheel.
 *
 * <p>In the world every Prism note is a little light show: a ring and a shaft of coloured light
 * round the player, sparkles - and real light, for a moment, where the player stands.
 */
public final class PrismLight {
    public static final int COUNT = 4;
    private static final int[] RGB = {0xFF5FA2, 0xFFC341, 0x3FE6E0, 0xA67BFF};
    private static final String[] IDS = {"rose", "amber", "cyan", "violet"};
    /** How long a note's light lingers on the spot (ticks). */
    private static final int GLOW_TICKS = 30;
    private static final Map<UUID, Long> LIT = new HashMap<>();

    private PrismLight() {
    }

    /** RGB of a colour of light (white for none). */
    public static int rgb(int colour) {
        return colour >= 0 && colour < COUNT ? RGB[colour] : 0xFFFFFF;
    }

    public static String id(int colour) {
        return colour >= 0 && colour < COUNT ? IDS[colour] : "none";
    }

    /** "Rose", "Amber", ... */
    public static Component name(int colour) {
        return Component.translatable("music.thesift.light." + id(colour));
    }

    /** Server: one Prism note in {@code colour}, played by {@code player} from {@code at}. */
    public static void flash(ServerLevel level, Player player, Vec3 at, int colour) {
        int rgb = rgb(colour);
        DustParticleOptions dust = new DustParticleOptions(rgb, 1.4F);
        DustParticleOptions soft = new DustParticleOptions(rgb, 0.8F);
        Vec3 c = player.position();
        // a ring of light opens round the player's feet...
        for (int i = 0; i < 16; i++) {
            double a = i / 16.0 * Math.PI * 2.0;
            level.sendParticles(soft, c.x + Math.cos(a) * 1.1, c.y + 0.15, c.z + Math.sin(a) * 1.1, 0, Math.cos(a), 0.0, Math.sin(a), 0.06);
        }
        // ...and a shaft of it rises from the instrument
        for (int i = 0; i < 7; i++) {
            level.sendParticles(dust, at.x, at.y + 0.2 + i * 0.32, at.z, 1, 0.04, 0.05, 0.04, 0.0);
        }
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y + 0.5, at.z, 3, 0.35, 0.35, 0.35, 0.02);
        glow(level, player);
    }

    /** Server: a burst in all four colours (a Prism song completed). */
    public static void burst(ServerLevel level, Vec3 c) {
        for (int i = 0; i < 32; i++) {
            double a = i / 32.0 * Math.PI * 2.0;
            DustParticleOptions dust = new DustParticleOptions(RGB[i % COUNT], 1.8F);
            for (int h = 0; h < 3; h++) {
                level.sendParticles(dust, c.x + Math.cos(a) * (1.6 + h * 0.5), c.y + 0.6 + h * 0.9 + Mth.sin((float) (a * 3.0)) * 0.3,
                        c.z + Math.sin(a) * (1.6 + h * 0.5), 1, 0.0, 0.05, 0.0, 0.0);
            }
        }
    }

    /** Real light at the player's head for a moment (at most every few ticks, and only into air). */
    private static void glow(ServerLevel level, Player player) {
        long now = level.getGameTime();
        Long last = LIT.get(player.getUUID());
        if (last != null && now - last < 8 && now >= last) {
            return;
        }
        LIT.put(player.getUUID(), now);
        if (LIT.size() > 256) {
            LIT.clear();
        }
        light(level, BlockPos.containing(player.getEyePosition()), 13, GLOW_TICKS);
    }

    /** Server: a temporary light block of {@code brightness} at pos (or just above it), if there is room. */
    public static boolean light(ServerLevel level, BlockPos pos, int brightness, int ticks) {
        BlockState light = Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, Mth.clamp(brightness, 0, 15));
        for (BlockPos p : new BlockPos[]{pos, pos.above()}) {
            BlockState here = level.getBlockState(p);
            if ((here.isAir() || here.is(Blocks.LIGHT)) && here.getFluidState().isEmpty()) {
                return TemporaryBlocks.place(level, p, light, ticks);
            }
        }
        return false;
    }
}
