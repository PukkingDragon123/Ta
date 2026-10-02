package com.thesift.block;

import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.common.CommonHooks;

/**
 * A seedling for the ancient plants Sniffers dig up in the Sift. Grows through AGE_3 and then
 * blooms into its mature plant. Music nearby makes it grow faster (see {@link #onResonate}).
 */
public abstract class SiftCropBlock extends VegetationBlock implements BonemealableBlock, com.thesift.music.Resonant {
    public static final IntegerProperty AGE = BlockStateProperties.AGE_3;
    private static final VoxelShape[] SHAPES = new VoxelShape[] {
            Block.column(6.0, 0.0, 5.0), Block.column(8.0, 0.0, 8.0), Block.column(10.0, 0.0, 11.0), Block.column(12.0, 0.0, 14.0)};

    protected SiftCropBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(AGE, 0));
    }

    protected abstract int maxAge();

    /** Replace this seedling with the grown plant. */
    protected abstract void bloom(ServerLevel level, BlockPos pos);

    protected abstract ItemStack seed();

    /** Whether this spot suits the plant (see {@link PlantHabitat}). Outside it the seedling just waits. */
    protected boolean likesHabitat(LevelReader level, BlockPos pos) {
        return level.getRawBrightness(pos, 0) >= 7;
    }

    /** Chance out of 1 that a random tick grows a seedling in its habitat. */
    protected float growthChance(LevelReader level, BlockPos pos) {
        return 0.25F;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[Math.min(state.getValue(AGE), 3)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return SiftPlantBlock.isSiftSoil(state) || state.is(Blocks.FARMLAND);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (this.likesHabitat(level, pos) && CommonHooks.canCropGrow(level, pos, state, random.nextFloat() < this.growthChance(level, pos))) {
            this.grow(level, pos, state);
            CommonHooks.fireCropGrowPost(level, pos, state);
        }
    }

    protected void grow(ServerLevel level, BlockPos pos, BlockState state) {
        int age = state.getValue(AGE);
        if (age >= this.maxAge()) {
            this.bloom(level, pos);
        } else {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onResonate(ServerLevel level, BlockPos pos, BlockState state, float strength) {
        // music hurries a plant along, but cannot make it grow where it does not belong
        if (this.likesHabitat(level, pos) && level.getRandom().nextFloat() < 0.25F * strength) {
            this.grow(level, pos, state);
        }
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return this.seed();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, BonemealSource source) {
        return this.likesHabitat(level, pos);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        return random.nextBoolean();
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, BonemealSource source) {
        this.grow(level, pos, state);
    }
}
