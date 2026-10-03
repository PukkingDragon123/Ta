package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * Pitcher Planter: a blush-brick planter of wet mud that a vanilla Pitcher Pod can be potted in.
 * Potted, the pitcher always has water and grows through four stages wherever the planter stands;
 * at the last it is a full Pitcher Plant in bloom. Pick it (use it with an empty hand) for the
 * Pitcher Plant - the base of the three Pitcher soups - now and then with a pod, and it drops back
 * to a young pitcher to grow again.
 */
public class PitcherPlanterBlock extends Block implements BonemealableBlock, com.thesift.music.Resonant {
    /** 0 = just mud, 1-3 = growing, 4 = a Pitcher Plant in bloom. */
    public static final IntegerProperty STAGE = IntegerProperty.create("stage", 0, 4);
    public static final int FULL = 4;
    private static final VoxelShape SHAPE = Shapes.or(Block.column(14.0, 0.0, 10.0), Block.column(10.0, 10.0, 15.0));
    private static final VoxelShape EMPTY_SHAPE = Block.column(14.0, 0.0, 10.0);

    public PitcherPlanterBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(STAGE) == 0 ? EMPTY_SHAPE : SHAPE;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        int s = state.getValue(STAGE);
        return s > 0 && s < FULL;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (CommonHooks.canCropGrow(level, pos, state, random.nextInt(4) == 0)) {
            this.grow(level, pos, state);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    private void grow(ServerLevel level, BlockPos pos, BlockState state) {
        int s = state.getValue(STAGE);
        if (s > 0 && s < FULL) {
            level.setBlock(pos, state.setValue(STAGE, s + 1), Block.UPDATE_CLIENTS);
            if (s + 1 == FULL) {
                level.playSound(null, pos, SoundEvents.BIG_DRIPLEAF_TILT_UP, SoundSource.BLOCKS, 0.7F, 1.4F);
                level.sendParticles(ModParticles.CHROME_DROPLET.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 6, 0.25, 0.2, 0.25, 0.01);
            }
        }
    }

    @Override
    public void onResonate(ServerLevel level, BlockPos pos, BlockState state, float strength) {
        if (level.getRandom().nextFloat() < 0.3F * strength) {
            this.grow(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (state.getValue(STAGE) == 0 && stack.is(Items.PITCHER_POD)) {
            if (level instanceof ServerLevel server) {
                BlockState planted = state.setValue(STAGE, 1);
                server.setBlock(pos, planted, Block.UPDATE_ALL);
                server.playSound(null, pos, SoundEvents.CROP_PLANTED, SoundSource.BLOCKS, 1.0F, 1.0F);
                server.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, planted));
                stack.consume(1, player);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (state.getValue(STAGE) != FULL) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            RandomSource r = server.getRandom();
            Block.popResource(server, pos.above(), new ItemStack(Items.PITCHER_PLANT));
            if (r.nextInt(3) == 0) {
                Block.popResource(server, pos.above(), new ItemStack(Items.PITCHER_POD));
            }
            server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 1.2F + r.nextFloat() * 0.3F);
            server.playSound(null, pos, SoundEvents.HONEY_BLOCK_SLIDE, SoundSource.BLOCKS, 0.6F, 1.5F);
            server.sendParticles(ModParticles.CHROME_DROPLET.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 8, 0.3, 0.2, 0.3, 0.02);
            BlockState picked = state.setValue(STAGE, 2);
            server.setBlock(pos, picked, Block.UPDATE_ALL);
            server.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, picked));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(STAGE) == FULL && random.nextInt(12) == 0) {
            level.addParticle(ModParticles.CHROME_DROPLET.get(), pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 1.3,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.0, 0.0);
        }
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, net.minecraft.core.Direction direction) {
        return state.getValue(STAGE) * 15 / FULL;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        int s = state.getValue(STAGE);
        return s > 0 && s < FULL;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        this.grow(level, pos, state);
    }
}
