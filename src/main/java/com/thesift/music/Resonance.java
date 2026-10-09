package com.thesift.music;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

/**
 * Music physically affects the Sift. Every drum hit, note block, jukebox beat, instrument note and
 * Europhy Table calls {@link #pulse}: a visible ring rolls outwards, plants bloom and grow,
 * Chrome ripples and sparkles, leaves shake loose and music-loving mobs react.
 */
public final class Resonance {
    private Resonance() {}

    public static void pulse(ServerLevel level, BlockPos origin, float strength, int radius) {
        RandomSource random = level.getRandom();
        double cx = origin.getX() + 0.5, cy = origin.getY() + 0.1, cz = origin.getZ() + 0.5;

        // The visible pulse: a flat ring (xSpeed carries the target radius) plus a few notes.
        level.sendParticles(ModParticles.RESONANCE_RING.get(), cx, cy + 0.02, cz, 0, radius * 0.5, 0.0, 0.0, 1.0);
        int notes = 2 + (int) (strength * 4);
        for (int i = 0; i < notes; i++) {
            level.sendParticles(ModParticles.SIFT_NOTE.get(), cx + (random.nextDouble() - 0.5) * 1.6, cy + 0.9 + random.nextDouble() * 0.6,
                    cz + (random.nextDouble() - 0.5) * 1.6, 0, random.nextDouble(), 0.0, 0.0, 1.0);
        }

        // Blocks: flowers bloom, crops grow, Chrome sparkles, leaves shiver.
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int r2 = radius * radius;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx * dx + dz * dz > r2) continue;
                for (int dy = -3; dy <= 4; dy++) {
                    p.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    BlockState state = level.getBlockState(p);
                    if (state.isAir()) continue;
                    float falloff = strength * (1.0F - (float) Math.sqrt(dx * dx + dz * dz) / (radius + 1.0F));
                    if (state.getBlock() instanceof Resonant resonant) {
                        resonant.onResonate(level, p.immutable(), state, falloff);
                    } else if (state.is(ModBlocks.CHROME.get()) && random.nextInt(6) == 0) {
                        level.sendParticles(ModParticles.CHROME_DROPLET.get(), p.getX() + random.nextDouble(), p.getY() + 1.0, p.getZ() + random.nextDouble(),
                                1, 0.0, 0.0, 0.0, 0.08);
                    } else if ((state.is(ModBlocks.LULLWOOD_LEAVES.get()) || state.is(ModBlocks.WISHWOOD_LEAVES.get())) && random.nextInt(10) == 0) {
                        level.sendParticles(state.is(ModBlocks.LULLWOOD_LEAVES.get()) ? ModParticles.LULLWOOD_LEAF.get() : ModParticles.WISHWOOD_LEAF.get(),
                                p.getX() + random.nextDouble(), p.getY() - 0.05, p.getZ() + random.nextDouble(), 1, 0.0, 0.0, 0.0, 0.0);
                    } else if (state.hasProperty(BlockStateProperties.AGE_7) && state.getBlock() instanceof BonemealableBlock bm
                            && random.nextFloat() < 0.04F * falloff && bm.isValidBonemealTarget(level, p, state, BonemealSource.INTERACTION)) {
                        bm.performBonemeal(level, random, p.immutable(), state, BonemealSource.INTERACTION);
                        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.0);
                    }
                }
            }
        }

        // Mobs.
        AABB box = new AABB(origin).inflate(radius, 4, radius);
        List<Entity> entities = level.getEntities((Entity) null, box, e -> e.isAlive());
        for (Entity e : entities) {
            if (e instanceof MusicListener listener) {
                listener.hearMusic(origin, strength);
            } else if (e instanceof Allay allay && random.nextInt(4) == 0) {
                level.sendParticles(ParticleTypes.NOTE, allay.getX(), allay.getY() + 0.8, allay.getZ(), 0, random.nextDouble(), 0, 0, 1);
            }
        }
    }

    /** A little celebratory burst when an ancient plant blooms. */
    public static void bloomBurst(ServerLevel level, BlockPos pos) {
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 12, 0.4, 0.5, 0.4, 0.05);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 1.4, pos.getZ() + 0.5, 0, 0.8, 0, 0, 1);
    }

    /** True if the position is in a Chrome fluid. */
    public static boolean isChrome(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).getType().isSame(ModFluids.CHROME.get());
    }
}
