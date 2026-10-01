package com.thesift.world;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Blocks that only last a while - the Strummer's webs - and vanish by themselves (if nothing
 * else has replaced them in the meantime). Kept in memory only: after a restart any leftovers are
 * just ordinary cobwebs.
 */
public final class TemporaryBlocks {
    private static final Map<ResourceKey<Level>, Map<BlockPos, Entry>> PLACED = new HashMap<>();

    private record Entry(BlockState state, long expires) {
    }

    private TemporaryBlocks() {
    }

    /** Places `state` at pos if it is air (or replaceable), to be removed after `ticks`. */
    public static boolean place(ServerLevel level, BlockPos pos, BlockState state, int ticks) {
        BlockState here = level.getBlockState(pos);
        if (!here.isAir() && !here.canBeReplaced()) {
            return false;
        }
        if (!here.getFluidState().isEmpty()) {
            return false;
        }
        level.setBlock(pos, state, Block.UPDATE_ALL);
        PLACED.computeIfAbsent(level.dimension(), k -> new HashMap<>()).put(pos.immutable(), new Entry(state, level.getGameTime() + ticks));
        return true;
    }

    public static void webs(ServerLevel level, BlockPos centre, int radius, float chance, int ticks) {
        for (BlockPos p : BlockPos.betweenClosed(centre.offset(-radius, 0, -radius), centre.offset(radius, 1, radius))) {
            if (level.getRandom().nextFloat() < chance) {
                place(level, p, Blocks.COBWEB.defaultBlockState(), ticks + level.getRandom().nextInt(40));
            }
        }
    }

    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.getGameTime() % 10 != 0) {
            return;
        }
        Map<BlockPos, Entry> map = PLACED.get(level.dimension());
        if (map == null || map.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Map.Entry<BlockPos, Entry>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<BlockPos, Entry> e = it.next();
            if (now >= e.getValue().expires() || now < e.getValue().expires() - 20 * 60 * 10) {
                if (level.isLoaded(e.getKey()) && level.getBlockState(e.getKey()).is(e.getValue().state().getBlock())) {
                    level.removeBlock(e.getKey(), false);
                }
                it.remove();
            }
        }
    }
}
