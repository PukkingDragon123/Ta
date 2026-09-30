package com.thesift.block;

import com.thesift.block.entity.SiftDrumBlockEntity;
import com.thesift.music.Resonance;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * The Sift Drum: a hide-and-lullwood drum. Hitting it (either mouse button, or a redstone pulse)
 * plays a beat whose tone depends on the block beneath, sends a resonance pulse through the world
 * and powers any Euphory Altar nearby. Slotting a Warden Core into a drum next to a portal frame
 * and sculk sensors starts the rhythm ritual that opens the way to The Sift.
 */
public class SiftDrumBlock extends BaseEntityBlock {
    public static final BooleanProperty BEAT = BooleanProperty.create("beat");
    public static final BooleanProperty CORE = BooleanProperty.create("core");
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    private static final VoxelShape SHAPE = Block.column(14.0, 0.0, 13.0);

    public SiftDrumBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(s -> s.getValue(CORE) ? 7 : 0));
        this.registerDefaultState(this.stateDefinition.any().setValue(BEAT, false).setValue(CORE, false).setValue(POWERED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(BEAT, CORE, POWERED);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SiftDrumBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, ModBlockEntities.SIFT_DRUM.get(), SiftDrumBlockEntity::serverTick);
    }

    /** 0 = low (stone), 1 = mid (wood), 2 = high (anything else). */
    public static int toneFor(Level level, BlockPos pos) {
        BlockState below = level.getBlockState(pos.below());
        if (below.is(BlockTags.BASE_STONE_OVERWORLD) || below.is(com.thesift.registry.ModTags.Blocks.SIFT_STONE) || below.is(BlockTags.STONE_BRICKS)
                || below.is(com.thesift.registry.ModTags.Blocks.PORTAL_FRAME)) {
            return 0;
        }
        if (below.is(BlockTags.PLANKS) || below.is(BlockTags.LOGS)) {
            return 1;
        }
        return 2;
    }

    public static SoundEvent soundFor(int tone) {
        return switch (tone) {
            case 0 -> ModSounds.DRUM_LOW.get();
            case 1 -> ModSounds.DRUM_MID.get();
            default -> ModSounds.DRUM_HIGH.get();
        };
    }

    /** Plays one beat of the drum: sound, bounce animation, particles and a resonance pulse. */
    public static void beat(ServerLevel level, BlockPos pos, float strength) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof SiftDrumBlock drum)) {
            return;
        }
        int tone = toneFor(level, pos);
        float pitch = 0.9F + level.getRandom().nextFloat() * 0.2F;
        level.playSound(null, pos, soundFor(tone), SoundSource.RECORDS, 1.2F, pitch);
        level.setBlock(pos, state.setValue(BEAT, true), Block.UPDATE_CLIENTS);
        level.scheduleTick(pos, drum, 3);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 0, tone / 3.0 + 0.1, 0.0, 0.0, 1.0);
        Resonance.pulse(level, pos, strength, 6);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(BEAT)) {
            level.setBlock(pos, state.setValue(BEAT, false), Block.UPDATE_CLIENTS);
        }
    }

    private InteractionResult hit(Level level, BlockPos pos, @Nullable Player player) {
        if (level instanceof ServerLevel server) {
            beat(server, pos, 0.8F);
            if (server.getBlockEntity(pos) instanceof SiftDrumBlockEntity drum) {
                drum.onPlayerBeat(player);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
            BlockHitResult hit) {
        if (stack.is(ModItems.WARDEN_CORE.get()) && !state.getValue(CORE)) {
            if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof SiftDrumBlockEntity drum) {
                if (drum.tryInsertCore(server, player)) {
                    stack.consume(1, player);
                } else {
                    return InteractionResult.FAIL;
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player.isShiftKeyDown() && state.getValue(CORE)) {
            if (level instanceof ServerLevel server && server.getBlockEntity(pos) instanceof SiftDrumBlockEntity drum) {
                drum.ejectCore(server);
                player.sendOverlayMessage(Component.translatable("message.thesift.drum.core_removed"));
            }
            return InteractionResult.SUCCESS;
        }
        return this.hit(level, pos, player);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        this.hit(level, pos, player);
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        boolean powered = level.hasNeighborSignal(pos);
        if (powered != state.getValue(POWERED)) {
            if (powered) {
                this.hit(level, pos, null);
            }
            level.setBlock(pos, level.getBlockState(pos).setValue(POWERED, powered), Block.UPDATE_ALL);
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(CORE) && random.nextInt(3) == 0) {
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SCULK_SOUL, pos.getX() + 0.5 + (random.nextDouble() - 0.5) * 0.6,
                    pos.getY() + 0.9, pos.getZ() + 0.5 + (random.nextDouble() - 0.5) * 0.6, 0.0, 0.03, 0.0);
        }
    }
}
