package com.thesift.block;

import com.thesift.registry.ModMansion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * MANSION: the Bookshelf Door - looks exactly like a vanilla bookshelf. It swings open (all the door's shelves together:
 * every Bookshelf Door touching it) while it is powered, or when the Loose Bookshelves near it are pulled in the right
 * order ({@link LooseBookshelfBlock}); then it closes again by itself, once nobody stands in the doorway. The Woodland
 * Mansion's secret room hides behind two of them, with a lever inside to let you out.
 */
public class SecretBookshelfBlock extends Block {
    public static final BooleanProperty OPEN = BooleanProperty.create("open");
    /** How long the door stays open after the books opened it. */
    public static final int OPEN_TICKS = 160;

    public SecretBookshelfBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(OPEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OPEN);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(OPEN) ? Shapes.empty() : Shapes.block();
    }

    /** Opens the whole door that {@code pos} belongs to (up to 8 shelves), for {@link #OPEN_TICKS} at least. */
    public static void openDoor(ServerLevel level, BlockPos pos) {
        boolean any = false;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            BlockState s = level.getBlockState(p);
            if (s.getBlock() instanceof SecretBookshelfBlock door) {
                if (!s.getValue(OPEN)) {
                    level.setBlock(p, s.setValue(OPEN, true), Block.UPDATE_ALL);
                    level.sendParticles(ParticleTypes.POOF, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, 0.3, 0.3, 0.3, 0.01);
                    any = true;
                }
                level.scheduleTick(p.immutable(), door, OPEN_TICKS);
            }
        }
        if (any) {
            level.playSound(null, pos, ModMansion.SECRET_OPEN.get(), SoundSource.BLOCKS, 1.0F, 0.8F);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(OPEN)) {
            return;
        }
        boolean someoneInside = !level.getEntitiesOfClass(LivingEntity.class, new AABB(pos)).isEmpty();
        if (groupPowered(level, pos) || someoneInside) {
            level.scheduleTick(pos, this, 40);
            return;
        }
        level.setBlock(pos, state.setValue(OPEN, false), Block.UPDATE_ALL);
        level.playSound(null, pos, ModMansion.SECRET_OPEN.get(), SoundSource.BLOCKS, 0.8F, 0.6F);
        LooseBookshelfBlock.resetNear(level, pos);
    }

    /** True while any shelf of the door (the Bookshelf Doors within two blocks) is powered: the whole door stays open. */
    private static boolean groupPowered(ServerLevel level, BlockPos pos) {
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-2, -2, -2), pos.offset(2, 2, 2))) {
            if (level.getBlockState(p).getBlock() instanceof SecretBookshelfBlock && level.hasNeighborSignal(p)) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        if (level instanceof ServerLevel server && server.hasNeighborSignal(pos) && !state.getValue(OPEN)) {
            openDoor(server, pos);
        }
    }
}
