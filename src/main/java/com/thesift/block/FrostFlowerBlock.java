package com.thesift.block;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModWorldLand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-land: the White Forest's frost flowers, each its own shape with its own trick (they still bloom to music like every
 * Sift flower, see {@link SiftFlowerBlock}):
 * <ul>
 *   <li>Halo Lily - a ring of petals floating over a glowing bud; music makes it heal the players around it.</li>
 *   <li>Snowglobe - a glass bulb full of rainbow snow; Rainbow Snow slowly gathers round it, and using it shakes up a flurry.</li>
 *   <li>Shiver Thistle - an icy thistle that frosts whatever brushes through it (powder-snow style: leather boots keep you
 *       warm).</li>
 * </ul>
 */
public class FrostFlowerBlock extends SiftFlowerBlock {
    private static final VoxelShape LOW = Block.column(8.0, 0.0, 12.0);
    private static final VoxelShape HIGH = Block.column(12.0, 0.0, 15.0);

    protected FrostFlowerBlock(Holder<MobEffect> effect, float seconds, int light, BlockBehaviour.Properties properties) {
        super(effect, seconds, light, properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return (this instanceof Snowglobe ? LOW : HIGH).move(state.getOffset(pos));
    }

    /** Halo Lily: a floating petal ring that glows; music played near it heals. */
    public static class HaloLily extends FrostFlowerBlock {
        public HaloLily(BlockBehaviour.Properties properties) {
            super(MobEffects.REGENERATION, 5.0F, 9, properties);
        }

        @Override
        public void onResonate(ServerLevel level, BlockPos pos, BlockState state, float strength) {
            super.onResonate(level, pos, state, strength);
            for (Player p : level.getEntitiesOfClass(Player.class, new AABB(pos.getX() - 5, pos.getY() - 3, pos.getZ() - 5,
                    pos.getX() + 6, pos.getY() + 4, pos.getZ() + 6))) {
                p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 0));
            }
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, 6, 0.4, 0.1, 0.4, 0.01);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            super.animateTick(state, level, pos, random);
            if (random.nextInt(10) == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5 + Math.cos(a) * 0.4, pos.getY() + 0.66,
                        pos.getZ() + 0.5 + Math.sin(a) * 0.4, 0.0, 0.004, 0.0);
            }
        }
    }

    /** Snowglobe: rainbow snow gathers round it; using it shakes up a flurry. */
    public static class Snowglobe extends FrostFlowerBlock {
        public Snowglobe(BlockBehaviour.Properties properties) {
            super(MobEffects.WATER_BREATHING, 6.0F, 5, properties);
        }

        @Override
        protected boolean isRandomlyTicking(BlockState state) {
            return true;
        }

        @Override
        protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
            if (random.nextInt(3) != 0) {
                return;
            }
            int x = pos.getX() + random.nextInt(7) - 3;
            int z = pos.getZ() + random.nextInt(7) - 3;
            BlockPos at = new BlockPos(x, level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z), z);
            BlockState here = level.getBlockState(at.below());
            BlockState snow = ModBlocks.RAINBOW_SNOW.get().defaultBlockState();
            if (here.is(ModBlocks.RAINBOW_SNOW.get()) && here.getValue(SnowLayerBlock.LAYERS) < 3) {
                level.setBlock(at.below(), here.setValue(SnowLayerBlock.LAYERS, here.getValue(SnowLayerBlock.LAYERS) + 1), Block.UPDATE_ALL);
            } else if (level.isEmptyBlock(at) && Math.abs(at.getY() - pos.getY()) <= 3 && snow.canSurvive(level, at)) {
                level.setBlock(at, snow, Block.UPDATE_ALL);
            }
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
            if (level instanceof ServerLevel server) {
                server.sendParticles(ModWorldLand.RAINBOW_SNOWFLAKE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 24, 0.6, 0.4, 0.6,
                        0.02);
                server.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.BLOCKS, 0.9F, 1.4F + server.getRandom().nextFloat() * 0.4F);
            }
            return InteractionResult.SUCCESS;
        }
    }

    /** Shiver Thistle: frosts whatever brushes through it. */
    public static class ShiverThistle extends FrostFlowerBlock {
        public ShiverThistle(BlockBehaviour.Properties properties) {
            super(MobEffects.SLOWNESS, 6.0F, 0, properties);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier,
                boolean isPrecise) {
            if (!(entity instanceof LivingEntity living) || !living.canFreeze()) {
                return;
            }
            if (!level.isClientSide()) {
                living.setTicksFrozen(Math.min(living.getTicksRequiredToFreeze(), living.getTicksFrozen() + 3));
            } else if (level.getRandom().nextInt(4) == 0) {
                level.addParticle(ModWorldLand.RAINBOW_SNOWFLAKE.get(), entity.getX(), entity.getY() + 0.6, entity.getZ(), 0.0, 0.03, 0.0);
            }
        }
    }
}
