package com.thesift.block;

import com.thesift.entity.SiftSniffer;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSiftSniffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * E1 the Sift Sniffer's egg: a big pink egg wrapped in white fluff, a sprout on top. Like the
 * vanilla Sniffer egg it cracks twice and hatches a baby after a day (half that on moss or the
 * other blocks in the vanilla hatch-boost tag), the sprout trembling as it gets close.
 */
public class SiftSnifferEggBlock extends Block {
    public static final IntegerProperty HATCH = IntegerProperty.create("hatch", 0, 2);
    private static final VoxelShape SHAPE = Block.box(1.0, 0.0, 2.0, 15.0, 16.0, 14.0);  // vanilla's Sniffer egg
    private static final TagKey<Block> HATCH_BOOST = TagKey.create(Registries.BLOCK, Identifier.withDefaultNamespace("sniffer_egg_hatch_boost"));
    private static final int REGULAR_HATCH_TICKS = 24000;
    private static final int BOOSTED_HATCH_TICKS = 12000;

    public SiftSnifferEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HATCH, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HATCH);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    private static boolean boosted(BlockGetter level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(HATCH_BOOST);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level instanceof ServerLevel server && !oldState.is(this)) {
            int stage = (boosted(level, pos) ? BOOSTED_HATCH_TICKS : REGULAR_HATCH_TICKS) / 3;
            server.scheduleTick(pos, this, stage + server.getRandom().nextInt(300));
            if (boosted(level, pos)) {
                server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, pos.getX() + 0.5, pos.getY() + 0.6, pos.getZ() + 0.5,
                        10, 0.4, 0.3, 0.4, 0.0);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        int hatch = state.getValue(HATCH);
        if (hatch < 2) {
            level.playSound(null, pos, ModSiftSniffer.EGG_CRACK.get(), SoundSource.BLOCKS, 0.7F, 0.9F + random.nextFloat() * 0.2F);
            level.setBlock(pos, state.setValue(HATCH, hatch + 1), Block.UPDATE_CLIENTS);
            int stage = (boosted(level, pos) ? BOOSTED_HATCH_TICKS : REGULAR_HATCH_TICKS) / 3;
            level.scheduleTick(pos, this, stage + random.nextInt(300));
            return;
        }
        hatch(level, pos);
    }

    /** Breaks the egg open and lets the baby out. */
    public static void hatch(ServerLevel level, BlockPos pos) {
        level.playSound(null, pos, ModSiftSniffer.EGG_HATCH.get(), SoundSource.BLOCKS, 0.7F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.destroyBlock(pos, false);
        SiftSniffer baby = ModSiftSniffer.SIFT_SNIFFER.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            Vec3 at = Vec3.atBottomCenterOf(pos);
            baby.setBaby(true);
            baby.setGarden(0);
            baby.snapTo(at.x, at.y, at.z, Mth.wrapDegrees(level.getRandom().nextFloat() * 360.0F), 0.0F);
            level.addFreshEntity(baby);
            level.sendParticles(ModParticles.DREAM_POLLEN.get(), at.x, at.y + 0.5, at.z, 16, 0.4, 0.3, 0.4, 0.02);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        // the closer it is to hatching, the more fluff it sheds
        if (random.nextInt(8 - state.getValue(HATCH) * 3) == 0) {
            level.addParticle(ModParticles.DREAM_POLLEN.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.9,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        }
    }
}
