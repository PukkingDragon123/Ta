package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModFluids;
import com.thesift.registry.ModItems;
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
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * P4 Cave Jungle: a young Giant Pitcher, sprouted from a Pitcher Pod planted on Sift dirt or grass (or moss). It only
 * grows when it has been watered with Chrome - pour a Chrome Bucket over it, or let it root within four blocks of
 * Chrome - and every stage drinks the water up. After three stages it rises into a Giant Pitcher Plant.
 */
public class PitcherSproutBlock extends VegetationBlock implements BonemealableBlock {
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, 2);
    public static final BooleanProperty WATERED = BooleanProperty.create("watered");
    private static final VoxelShape[] SHAPES = {Block.column(6.0, 0.0, 6.0), Block.column(8.0, 0.0, 10.0), Block.column(10.0, 0.0, 14.0)};

    public PitcherSproutBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0).setValue(WATERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE, WATERED);
    }

    /** Where pitchers root: Sift soils, dirt and grass, moss and the jungle's Lumen Moss, mud and clay. */
    public static boolean soil(BlockState state) {
        return SiftPlantBlock.isSiftSoil(state) || state.is(ModBlocks.LUMEN_MOSS_BLOCK.get()) || PlantHabitat.dampSoil(state);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return soil(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(AGE)];
    }

    /** Chrome within four blocks of its soil waters it all the time. */
    public static boolean nearChrome(LevelReader level, BlockPos pos) {
        BlockPos soil = pos.below();
        for (BlockPos p : BlockPos.betweenClosed(soil.offset(-4, -1, -4), soil.offset(4, 1, 4))) {
            FluidState fluid = level.getFluidState(p);
            if (!fluid.isEmpty() && fluid.getType().isSame(ModFluids.CHROME.get())) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(3) == 0 && (state.getValue(WATERED) || nearChrome(level, pos))) {
            this.grow(level, pos, state);
        }
    }

    private void grow(ServerLevel level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age < 2) {
            level.setBlock(pos, state.setValue(AGE, age + 1).setValue(WATERED, false), Block.UPDATE_CLIENTS);
            return;
        }
        if (level.isEmptyBlock(pos.above())) {
            GiantPitcherBlock.grow(level, pos, false);
            level.playSound(null, pos, SoundEvents.BIG_DRIPLEAF_TILT_UP, SoundSource.BLOCKS, 1.0F, 0.7F);
            level.sendParticles(ModParticles.CHROME_DROPLET.get(), pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 10, 0.3, 0.4, 0.3, 0.02);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(ModItems.CHROME_BUCKET.get()) || state.getValue(WATERED)) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        if (level instanceof ServerLevel server) {
            server.setBlock(pos, state.setValue(WATERED, true), Block.UPDATE_CLIENTS);
            server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.1F);
            server.sendParticles(ModParticles.CHROME_DROPLET.get(), pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5, 12, 0.35, 0.2, 0.35, 0.02);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return state.getValue(AGE) < 2 || level.isEmptyBlock(pos.above());
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return random.nextInt(3) == 0;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        this.grow(level, pos, state);
    }
}
