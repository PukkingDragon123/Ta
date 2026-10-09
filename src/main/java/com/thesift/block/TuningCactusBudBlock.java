package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * W-land: the growing tip of a Tuning Cactus. It grows like a chorus flower - up, or splitting into side shoots - and when
 * it is done (age 5) it opens into a tuning-fork crown that bears Tuning Fruit. Use a fruiting crown to pick the fruit; tap
 * a bare one and it rings. Plant a bud on sand to start a new cactus.
 */
public class TuningCactusBudBlock extends Block {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_5;
    public static final BooleanProperty FRUIT = BooleanProperty.create("fruit");
    private static final Direction[] REACH = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST, Direction.DOWN};
    private static final VoxelShape BUD = Block.box(4.0, 4.0, 4.0, 12.0, 15.0, 12.0);

    public TuningCactusBudBlock(BlockBehaviour.Properties properties) {
        super(properties);
        BlockState s = this.stateDefinition.any().setValue(AGE, 0).setValue(FRUIT, false);
        for (Direction d : REACH) {
            s = s.setValue(TuningCactusBlock.SIDES.get(d), false);
        }
        this.registerDefaultState(s);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, FRUIT, BlockStateProperties.NORTH, BlockStateProperties.EAST, BlockStateProperties.SOUTH, BlockStateProperties.WEST,
                BlockStateProperties.DOWN);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        return TuningCactusBlock.connected(context.getLevel(), context.getClickedPos(), this.defaultBlockState());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        VoxelShape s = BUD;
        for (Direction d : REACH) {
            if (state.getValue(TuningCactusBlock.SIDES.get(d))) {
                s = Shapes.or(s, TuningCactusBlock.arm(d));
            }
        }
        return s;
    }

    /** Like a chorus flower: on a stem or the ground, or hanging off exactly one stem beside it over empty air. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.getBlock() instanceof TuningCactusBlock || RattlethornBlock.desertGround(below)) {
            return true;
        }
        if (!below.isAir()) {
            return false;
        }
        boolean found = false;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState side = level.getBlockState(pos.relative(d));
            if (side.getBlock() instanceof TuningCactusBlock) {
                if (found) {
                    return false;
                }
                found = true;
            } else if (!side.isAir()) {
                return false;
            }
        }
        return found;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighbourPos, BlockState neighbourState, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            ticks.scheduleTick(pos, this, 1);
        }
        if (direction == Direction.UP) {
            return state;
        }
        return state.setValue(TuningCactusBlock.SIDES.get(direction), TuningCactusBlock.reaches(neighbourState, direction));
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) {
            level.destroyBlock(pos, true);
        }
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(AGE) < 5 || !state.getValue(FRUIT);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int age = state.getValue(AGE);
        if (age >= 5) {
            if (!state.getValue(FRUIT) && random.nextInt(5) == 0) {
                level.setBlock(pos, state.setValue(FRUIT, true), Block.UPDATE_CLIENTS);
            }
            return;
        }
        BlockPos above = pos.above();
        if (!level.isEmptyBlock(above) || above.getY() >= level.getMaxY()) {
            return;
        }
        // chorus growth: climb while the column below is short, otherwise split into side shoots, otherwise open into a crown
        boolean grow = false;
        boolean onGround = false;
        BlockState below = level.getBlockState(pos.below());
        if (RattlethornBlock.desertGround(below)) {
            grow = true;
        } else if (below.getBlock() instanceof TuningCactusBlock) {
            int height = 1;
            for (int i = 0; i < 4; i++) {
                BlockState b = level.getBlockState(pos.below(height + 1));
                if (!(b.getBlock() instanceof TuningCactusBlock)) {
                    onGround = RattlethornBlock.desertGround(b);
                    break;
                }
                height++;
            }
            grow = height < 2 || height <= random.nextInt(onGround ? 5 : 4);
        } else if (below.isAir()) {
            grow = true;
        }
        if (grow && clearAround(level, above, null) && level.isEmptyBlock(pos.above(2))) {
            level.setBlock(pos, stem(level, pos), Block.UPDATE_CLIENTS);
            this.placeBud(level, above, age);
        } else if (age < 4) {
            int shoots = random.nextInt(4) + (onGround ? 1 : 0);
            boolean split = false;
            for (int i = 0; i < shoots; i++) {
                Direction d = Direction.Plane.HORIZONTAL.getRandomDirection(random);
                BlockPos side = pos.relative(d);
                if (level.isEmptyBlock(side) && level.isEmptyBlock(side.below()) && clearAround(level, side, d.getOpposite())) {
                    this.placeBud(level, side, age + 1);
                    split = true;
                }
            }
            if (split) {
                level.setBlock(pos, stem(level, pos), Block.UPDATE_CLIENTS);
            } else {
                level.setBlock(pos, state.setValue(AGE, 5), Block.UPDATE_CLIENTS);
            }
        } else {
            level.setBlock(pos, state.setValue(AGE, 5), Block.UPDATE_CLIENTS);
        }
    }

    private void placeBud(Level level, BlockPos pos, int age) {
        level.setBlock(pos, TuningCactusBlock.connected(level, pos, this.defaultBlockState().setValue(AGE, Math.min(age, 5))), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.4F, 1.6F);
    }

    private static BlockState stem(Level level, BlockPos pos) {
        return TuningCactusBlock.connected(level, pos, ModBlocks.TUNING_CACTUS.get().defaultBlockState());
    }

    private static boolean clearAround(LevelReader level, BlockPos pos, @Nullable Direction except) {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (d != except && !level.isEmptyBlock(pos.relative(d))) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(AGE) < 5) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            RandomSource r = server.getRandom();
            if (state.getValue(FRUIT)) {
                Block.popResource(server, pos.above(), new ItemStack(ModItems.TUNING_FRUIT.get(), 1 + r.nextInt(2)));
                server.setBlock(pos, state.setValue(FRUIT, false), Block.UPDATE_CLIENTS);
                server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.9F + r.nextFloat() * 0.3F);
            }
            // the bare fork rings like the tuning fork it is
            server.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.8F, 0.8F + r.nextFloat() * 0.6F);
            server.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        }
        return InteractionResult.SUCCESS;
    }
}
