package com.thesift.portal;

import com.thesift.block.SiftPortalBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModDimensions;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Works out where a Sift portal leads. On first use it builds an arrival gate of reinforced
 * deepslate - an Ancient City gate in miniature - so the way back can never be mined for its
 * (very expensive) Sift Gate Frames.
 */
public final class SiftTeleporter {
    private SiftTeleporter() {}

    public static @Nullable TeleportTransition destination(ServerLevel current, Entity entity, BlockPos portalPos) {
        ResourceKey<Level> targetKey = current.dimension() == ModDimensions.THE_SIFT ? Level.OVERWORLD : ModDimensions.THE_SIFT;
        ServerLevel target = current.getServer().getLevel(targetKey);
        if (target == null) {
            return null;
        }
        BlockPos anchor = PortalFrames.anchorOf(current, portalPos);
        GlobalPos from = GlobalPos.of(current.dimension(), anchor);
        SiftPortalData data = SiftPortalData.get(current.getServer());
        GlobalPos linked = data.linked(from);
        // the far gate may have been rebuilt or shrunk so its anchor moved: follow it rather than build another
        BlockPos arrival = linked != null && linked.dimension() == targetKey ? relink(target, linked.pos()) : null;
        if (arrival != null) {
            if (!arrival.equals(linked.pos())) {
                data.link(from, GlobalPos.of(targetKey, arrival));
            }
        } else {
            Direction.Axis axis = current.getBlockState(portalPos).getOptionalValue(SiftPortalBlock.AXIS).orElse(Direction.Axis.X);
            BlockPos approx = target.getWorldBorder().clampToBounds(entity.getX(), entity.getY(), entity.getZ());
            arrival = buildArrivalPortal(target, approx, axis);
            data.link(from, GlobalPos.of(targetKey, arrival));
        }
        Vec3 pos = PortalFrames.standingSpot(target, arrival);
        final BlockPos ticketPos = arrival;
        return new TeleportTransition(target, pos, Vec3.ZERO, entity.getYRot(), entity.getXRot(), Set.<Relative>of(), e -> {
            e.placePortalTicket(ticketPos);
            if (e.level() instanceof ServerLevel sl) {
                sl.playSound(null, ticketPos, ModSounds.PORTAL_TRAVEL.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
                sl.sendParticles(ModParticles.PORTAL_SOUL.get(), e.getX(), e.getY() + 1.0, e.getZ(), 30, 0.6, 1.0, 0.6, 0.05);
            }
        });
    }

    /** A portal block close to where a linked gate used to be anchored, or null. */
    private static @Nullable BlockPos relink(ServerLevel level, BlockPos old) {
        if (level.getBlockState(old).is(ModBlocks.SIFT_PORTAL.get())) {
            return old;
        }
        for (BlockPos p : BlockPos.betweenClosed(old.offset(-3, -3, -3), old.offset(3, 3, 3))) {
            if (level.getBlockState(p).is(ModBlocks.SIFT_PORTAL.get())) {
                return PortalFrames.anchorOf(level, p.immutable());
            }
        }
        return null;
    }

    /** Builds a 4x5 reinforced deepslate gate with a small Dreamstone landing and returns its anchor. */
    public static BlockPos buildArrivalPortal(ServerLevel level, BlockPos approx, Direction.Axis axis) {
        int x = approx.getX(), z = approx.getZ();
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        if (y <= level.getMinY() + 2) {
            y = Math.max(level.getSeaLevel() + 16, level.getMinY() + 40);
        }
        y = Math.min(y, level.getMaxY() - 8);
        Direction along = axis == Direction.Axis.X ? Direction.EAST : Direction.SOUTH;
        Direction across = along.getClockWise();
        BlockPos base = new BlockPos(x, y, z);
        BlockState floor = ModBlocks.POLISHED_DREAMSTONE.get().defaultBlockState();
        BlockState frame = net.minecraft.world.level.block.Blocks.REINFORCED_DEEPSLATE.defaultBlockState();
        BlockState air = net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        // Landing platform and head room.
        for (int a = -2; a <= 3; a++) {
            for (int c = -2; c <= 2; c++) {
                BlockPos f = base.relative(along, a).relative(across, c).below();
                if (!level.getBlockState(f).isSolidRender() || !level.getFluidState(f).isEmpty()) {
                    level.setBlock(f, floor, Block.UPDATE_CLIENTS);
                }
                for (int h = 0; h < 6; h++) {
                    BlockPos b = base.relative(along, a).relative(across, c).above(h);
                    if (!level.getBlockState(b).isAir() || !level.getFluidState(b).isEmpty()) {
                        level.setBlock(b, air, Block.UPDATE_CLIENTS);
                    }
                }
            }
        }
        // Frame ring: 4 wide, 5 tall. Interior is 2x3.
        BlockState portal = ModBlocks.SIFT_PORTAL.get().defaultBlockState().setValue(SiftPortalBlock.AXIS, axis);
        for (int a = -1; a <= 2; a++) {
            for (int h = 0; h <= 4; h++) {
                BlockPos b = base.relative(along, a).above(h);
                boolean edge = a == -1 || a == 2 || h == 0 || h == 4;
                level.setBlock(b, edge ? frame : portal, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            }
        }
        return base.above(1).immutable();
    }
}
