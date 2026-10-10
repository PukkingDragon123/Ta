package com.thesift.block;

import com.thesift.entity.jungle.GlowFly;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaveJungle;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * P4 Cave Jungle: the Giant Pitcher Plant - a carnivorous pitcher two blocks tall, its mouth a ribbed lip under a
 * hood, its belly half full of digestive juice. It smells like a plant starving for light, so Glow Flies come to
 * feed it - and linger over its mouth, and are snapped up (see {@link GlowFly}). A fed pitcher sets Pitcher Pods
 * round its lip; pick them with an empty hand (cook them for a Stomper). Anything that climbs into the mouth is
 * stung by the juice. Pods planted on Sift dirt or grass (or moss) sprout and, watered with Chrome, grow into new
 * pitchers ({@link PitcherSproutBlock}); a Chrome Bucket poured into a hungry pitcher sets its pods at once.
 */
public class GiantPitcherBlock extends DoublePlantBlock {
    public static final BooleanProperty PODS = BooleanProperty.create("pods");
    private static final VoxelShape LOWER = Block.column(11.0, 0.0, 16.0);
    private static final VoxelShape UPPER = Block.column(13.0, 0.0, 12.0);

    public GiantPitcherBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(HALF, DoubleBlockHalf.LOWER).setValue(PODS, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(PODS);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return PitcherSproutBlock.soil(state) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER ? LOWER : UPPER;
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return state.getValue(HALF) == DoubleBlockHalf.LOWER && !state.getValue(PODS);
    }

    /** Slowly, even unfed, a pitcher sets new pods. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) == 0) {
            setPods(level, pos, true);
        }
    }

    /** Sets the pods of the whole plant, given its lower half. */
    public static void setPods(Level level, BlockPos base, boolean pods) {
        BlockState lower = level.getBlockState(base);
        BlockState upper = level.getBlockState(base.above());
        if (lower.getBlock() instanceof GiantPitcherBlock && upper.getBlock() instanceof GiantPitcherBlock) {
            level.setBlock(base, lower.setValue(PODS, pods), Block.UPDATE_CLIENTS);
            level.setBlock(base.above(), upper.setValue(PODS, pods), Block.UPDATE_CLIENTS);
        }
    }

    private static BlockPos base(BlockState state, BlockPos pos) {
        return state.getValue(HALF) == DoubleBlockHalf.UPPER ? pos.below() : pos;
    }

    /** The mouth closes on a Glow Fly: a snap, a gulp, a fizz of light - and the pitcher is fed. */
    public static void devour(ServerLevel level, BlockPos mouth, GlowFly fly) {
        BlockState st = level.getBlockState(mouth);
        BlockPos base = base(st, mouth);
        Vec3 c = Vec3.atCenterOf(mouth).add(0.0, 0.35, 0.0);
        level.playSound(null, mouth, ModCaveJungle.PITCHER_SNAP.get(), SoundSource.BLOCKS, 1.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        level.playSound(null, mouth, ModCaveJungle.PITCHER_GULP.get(), SoundSource.BLOCKS, 0.8F, 0.8F);
        level.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 16, 0.25, 0.2, 0.25, 0.05);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 8, 0.15, 0.15, 0.15, 0.06);
        level.sendParticles(ModParticles.CHROME_DROPLET.get(), c.x, c.y, c.z, 6, 0.3, 0.1, 0.3, 0.02);
        fly.discard();
        setPods(level, base, true);
    }

    /** The juice stings whatever climbs into the mouth (Glow Flies are swallowed whole). */
    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel server && state.getValue(HALF) == DoubleBlockHalf.UPPER && entity instanceof LivingEntity living
                && !(living instanceof GlowFly) && living.tickCount % 20 == 0 && living.getY() < pos.getY() + 0.8) {
            AcidWeeperBlock.corrode(server, living, 1.0F);
        }
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (stack.is(ModItems.CHROME_BUCKET.get()) && !state.getValue(PODS)) {
            if (level instanceof ServerLevel server) {
                BlockPos base = base(state, pos);
                setPods(server, base, true);
                server.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
                server.sendParticles(ModParticles.CHROME_DROPLET.get(), base.getX() + 0.5, base.getY() + 1.9, base.getZ() + 0.5, 12, 0.3, 0.2, 0.3, 0.02);
                player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!state.getValue(PODS)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel server) {
            BlockPos base = base(state, pos);
            RandomSource r = server.getRandom();
            Block.popResource(server, base.above(), new ItemStack(ModItems.PITCHER_POD.get(), 1 + r.nextInt(2)));
            setPods(server, base, false);
            server.playSound(null, pos, SoundEvents.SWEET_BERRY_BUSH_PICK_BERRIES, SoundSource.BLOCKS, 1.0F, 0.8F + r.nextFloat() * 0.2F);
            server.playSound(null, pos, SoundEvents.BIG_DRIPLEAF_TILT_DOWN, SoundSource.BLOCKS, 0.6F, 1.2F);
            server.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, state));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(HALF) == DoubleBlockHalf.UPPER && random.nextInt(5) == 0) {
            // the sweet, glowing breath of the juice drifting up out of the mouth
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
                    pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0.0, 0.015, 0.0);
        }
    }

    /** Places a whole pitcher (worldgen and the sprout growing up). */
    public static void grow(Level level, BlockPos base, boolean pods) {
        DoublePlantBlock.placeAt(level, ModBlocks.GIANT_PITCHER.get().defaultBlockState().setValue(PODS, pods), base, Block.UPDATE_ALL);
    }
}
