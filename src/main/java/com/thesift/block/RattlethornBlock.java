package com.thesift.block;

import com.thesift.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-land: the Rattlethorn, one of the Rocky Dunes' two plants. A dry, spiky bramble like a sweet berry bush gone feral:
 * it drags at whoever pushes through it and pricks them as they move (Sifters know the way through), and its seed pods
 * rattle like a snare drum.
 */
public class RattlethornBlock extends VegetationBlock {
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 13.0);

    public RattlethornBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /** The desert floor the Rocky Dunes' plants root in: sand, Sift soils, and the dunes' own rock. */
    public static boolean desertGround(BlockState state) {
        return state.is(BlockTags.SAND) || SiftPlantBlock.isSiftSoil(state) || state.is(ModBlocks.DUNESTONE.get())
                || state.is(ModBlocks.BANDED_DUNESTONE.get()) || state.is(ModBlocks.CHIME_SANDSTONE.get());
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return desertGround(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (!(entity instanceof LivingEntity) || entity instanceof com.thesift.entity.Sifter
                || entity instanceof com.thesift.entity.dunes.DunesNative) { // P4-DESERT: the dunes' creatures know the way through too
            return;
        }
        entity.makeStuckInBlock(state, new Vec3(0.8F, 0.75, 0.8F));
        if (level instanceof ServerLevel server) {
            Vec3 moved = entity.isClientAuthoritative() ? entity.getKnownMovement() : entity.oldPosition().subtract(entity.position());
            if (moved.horizontalDistanceSqr() > 0.0 && (Math.abs(moved.x()) >= 0.003F || Math.abs(moved.z()) >= 0.003F)) {
                entity.hurtServer(server, level.damageSources().cactus(), 1.0F);
                if (server.getRandom().nextInt(5) == 0) {
                    server.playSound(null, pos, SoundEvents.NOTE_BLOCK_SNARE.value(), SoundSource.BLOCKS, 0.5F,
                            1.3F + server.getRandom().nextFloat() * 0.5F);
                }
            }
        }
    }
}
