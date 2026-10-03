package com.thesift.block;

import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.HalfTransparentBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Cloud: the soft, faintly see-through ground of the Sound Garden. Falls onto it never hurt, and
 * walking on it kicks up little puffs of mist.
 */
public class CloudBlock extends HalfTransparentBlock {
    public CloudBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    /**
     * S1 spawning: creatures spawn on cloud like on any other ground. The block copies glass, whose
     * {@code isValidSpawn} is "never" - which kept every creature but the Sky Whales out of the Sound Garden.
     */
    public static boolean spawnable(BlockState state, BlockGetter level, BlockPos pos, EntityType<?> type) {
        return true;
    }

    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        entity.causeFallDamage(fallDistance, 0.0F, level.damageSources().fall());
        if (level.isClientSide() && fallDistance > 1.5) {
            for (int i = 0; i < 8; i++) {
                level.addParticle(ModParticles.SIFT_MIST.get(), entity.getRandomX(0.8), pos.getY() + 1.05, entity.getRandomZ(0.8),
                        (level.getRandom().nextDouble() - 0.5) * 0.1, 0.02, (level.getRandom().nextDouble() - 0.5) * 0.1);
            }
        }
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level.isClientSide() && entity instanceof LivingEntity && entity.getKnownMovement().horizontalDistanceSqr() > 1.0E-3
                && level.getRandom().nextInt(5) == 0) {
            level.addParticle(ModParticles.SIFT_MIST.get(), entity.getRandomX(0.5), pos.getY() + 1.02, entity.getRandomZ(0.5), 0.0, 0.01, 0.0);
        }
        super.stepOn(level, pos, state, entity);
    }
}
