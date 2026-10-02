package com.thesift.world;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.EventHooks;
import net.neoforged.neoforge.fluids.FluidInteractionRegistry;
import org.jspecify.annotations.Nullable;

/**
 * How Chrome answers the world (server side).
 *
 * <ul>
 *   <li>Where Chrome meets moving water it settles into Chime Sand, the way lava meeting water turns
 *   to stone. The check runs for every Chrome block that water touches, so it works both ways round:
 *   Chrome poured onto water, and water poured onto or into Chrome. Still Chrome lying against still
 *   water (the shore of a real-water sea, as the world was made) is left alone, so a passing fish or
 *   a growing kelp never sets a whole coastline off.</li>
 *   <li>Every note played near Chrome bursts its surface into colour: one {@code chrome_chord}
 *   particle tells the clients, which light up the Chrome around it themselves.</li>
 * </ul>
 */
public final class ChromeReactions {
    private ChromeReactions() {
    }

    public static void registerFluidInteractions() {
        FluidInteractionRegistry.addInteraction(ModFluids.CHROME_TYPE.get(),
                new FluidInteractionRegistry.InteractionInformation(ChromeReactions::meetsWater, ChromeReactions::settle));
    }

    private static boolean meetsWater(Level level, BlockPos chromePos, BlockPos otherPos, FluidState chrome) {
        FluidState other = level.getFluidState(otherPos);
        return other.getFluidType() == NeoForgeMod.WATER_TYPE.value() && !(chrome.isSource() && other.isSource());
    }

    private static void settle(Level level, BlockPos pos, BlockPos waterPos, FluidState chrome) {
        level.setBlockAndUpdate(pos, EventHooks.fireFluidPlaceBlockEvent(level, pos, pos, ModBlocks.CHIME_SAND.get().defaultBlockState()));
        if (level instanceof ServerLevel server) {
            RandomSource random = server.getRandom();
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 1.0F, 0.7F + random.nextFloat() * 0.6F);
            server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.35F, 1.4F + random.nextFloat() * 0.3F);
            for (int i = 0; i < 5; i++) {
                server.sendParticles(ModChrome.CHROME_SPARK.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.9, pos.getZ() + random.nextDouble(),
                        0, random.nextDouble(), 0.05 + random.nextDouble() * 0.05, 0.0, 1.0);
            }
            server.sendParticles(ModParticles.SIFT_MIST.get(), pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, 2, 0.3, 0.1, 0.3, 0.005);
        }
    }

    /** A note (pitch 0-24) rang out at {@code at}: if there is Chrome about, its surface bursts into colour. */
    public static void onNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        if (chromeNear(level, BlockPos.containing(at))) {
            level.sendParticles(ModChrome.CHROME_CHORD.get(), at.x, at.y, at.z, 0, Mth.clamp(pitch, 0, 24) / 24.0, 0.0, 0.0, 1.0);
        }
    }

    private static boolean chromeNear(ServerLevel level, BlockPos centre) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dy = -3; dy <= 2; dy++) {
            for (int dx = -6; dx <= 6; dx += 2) {
                for (int dz = -6; dz <= 6; dz += 2) {
                    p.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    if (level.isLoaded(p) && level.getFluidState(p).getType().isSame(ModFluids.CHROME.get())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
