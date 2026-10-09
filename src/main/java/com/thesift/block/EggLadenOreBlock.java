package com.thesift.block;

import com.thesift.entity.caravan.CaravanLarva;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;

/**
 * CR2: Egg-laden Ore - an ore socket a Caravan has dug clean and filled with its eggs, sealed in with
 * a little of the rock; the eggs glow through the stone. Mine it and a Caravan larva or two wriggles
 * out and goes for you (a silk-touched pickaxe lifts the eggs out whole). Caravans that see it broken
 * swarm the miner. Also found in the rock of the Caravans Cavern.
 */
public class EggLadenOreBlock extends Block {
    public EggLadenOreBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        if (level.getGameRules().get(GameRules.BLOCK_DROPS) && !EnchantmentHelper.hasTag(tool, EnchantmentTags.PREVENTS_INFESTED_SPAWNS)) {
            CaravanLarva.emerge(level, pos, state, 1 + level.getRandom().nextInt(2));
        }
    }

    /** Now and then a glint of the eggs' light seeps out of an open face. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) {
            return;
        }
        Direction face = Direction.getRandom(random);
        BlockPos out = pos.relative(face);
        if (level.getBlockState(out).isSolidRender()) {
            return;
        }
        double x = pos.getX() + 0.5 + face.getStepX() * 0.55 + (face.getStepX() == 0 ? random.nextDouble() - 0.5 : 0.0) * 0.8;
        double y = pos.getY() + 0.5 + face.getStepY() * 0.55 + (face.getStepY() == 0 ? random.nextDouble() - 0.5 : 0.0) * 0.8;
        double z = pos.getZ() + 0.5 + face.getStepZ() * 0.55 + (face.getStepZ() == 0 ? random.nextDouble() - 0.5 : 0.0) * 0.8;
        level.addParticle(ModParticles.GLOW_DUST.get(), x, y, z, face.getStepX() * 0.01, 0.01, face.getStepZ() * 0.01);
    }
}
