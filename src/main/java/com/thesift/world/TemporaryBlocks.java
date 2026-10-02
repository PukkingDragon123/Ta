package com.thesift.world;

import com.thesift.registry.ModBlocks;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

/**
 * Blocks that only last a while - the Weaver's musical cobwebs - and vanish by themselves (if
 * nothing else has replaced them in the meantime). Kept in memory only: after a restart any
 * leftovers are just ordinary musical cobwebs.
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
                web(level, p, ticks + level.getRandom().nextInt(40));
            }
        }
    }

    /** One musical cobweb at pos, for `ticks`. */
    public static boolean web(ServerLevel level, BlockPos pos, int ticks) {
        return place(level, pos, ModBlocks.MUSICAL_COBWEB.get().defaultBlockState(), ticks);
    }

    /** A ring of musical cobwebs around centre (on the ground where it can find it), for `ticks`. */
    public static int webRing(ServerLevel level, BlockPos centre, double radius, int ticks) {
        int n = Math.max(6, (int) (radius * Math.PI * 2.0 * 0.8));
        int made = 0;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            BlockPos at = BlockPos.containing(centre.getX() + 0.5 + Math.cos(a) * radius, centre.getY(), centre.getZ() + 0.5 + Math.sin(a) * radius);
            for (int dy = 1; dy >= -2; dy--) {
                BlockPos p = at.above(dy);
                if (level.getBlockState(p.below()).isFaceSturdy(level, p.below(), net.minecraft.core.Direction.UP) && web(level, p, ticks + level.getRandom().nextInt(30))) {
                    made++;
                    break;
                }
            }
        }
        return made;
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
