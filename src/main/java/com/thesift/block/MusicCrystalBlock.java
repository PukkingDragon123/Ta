package com.thesift.block;

import com.thesift.block.entity.MusicCrystalBlockEntity;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * A Music Crystal, grown by the Caravans. It glows in one of five colours, grows out of any face
 * like amethyst, and rings its own note when hit or used (colour sets the register, see
 * {@link CrystalColor}). A frozen crystal is a chunky block of clear crystal with a treasure
 * locked inside ({@link MusicCrystalBlockEntity}); breaking it sets the treasure free. Breaking
 * any crystal near a colony brings the whole swarm.
 */
public class MusicCrystalBlock extends BaseEntityBlock {
    public static final EnumProperty<CrystalColor> COLOR = EnumProperty.create("color", CrystalColor.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.FACING;
    public static final BooleanProperty FROZEN = BooleanProperty.create("frozen");

    public MusicCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(COLOR, CrystalColor.ROSE).setValue(FACING, Direction.UP).setValue(FROZEN, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COLOR, FACING, FROZEN);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(FROZEN) ? new MusicCrystalBlockEntity(pos, state) : null;
    }

    // ------------------------------------------------------------------ shape & support

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        boolean frozen = state.getValue(FROZEN);
        double h = frozen ? 16.0 : 15.0;
        double a = frozen ? 2.0 : 3.0;
        double b = 16.0 - a;
        return switch (state.getValue(FACING)) {
            case UP -> Block.box(a, 0.0, a, b, h, b);
            case DOWN -> Block.box(a, 16.0 - h, a, b, 16.0, b);
            case NORTH -> Block.box(a, a, 16.0 - h, b, b, 16.0);
            case SOUTH -> Block.box(a, a, 0.0, b, b, h);
            case EAST -> Block.box(0.0, a, a, h, b, b);
            case WEST -> Block.box(16.0 - h, a, a, 16.0, b, b);
        };
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return level.getBlockState(support).isFaceSturdy(level, support, facing);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction, BlockPos neighborPos,
            BlockState neighborState, RandomSource random) {
        return direction == state.getValue(FACING).getOpposite() && !state.canSurvive(level, pos) ? Blocks.AIR.defaultBlockState()
                : super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockPos pos = context.getClickedPos();
        // a crystal placed by hand takes a colour from where it stands (a mined one keeps its own, from the item)
        BlockState state = this.defaultBlockState().setValue(FACING, context.getClickedFace())
                .setValue(COLOR, CrystalColor.byId((int) Math.floorMod(pos.asLong() * 31L + 7L, 5L)));
        return state.canSurvive(context.getLevel(), pos) ? state : null;
    }

    // ------------------------------------------------------------------ music

    /** The crystal's own note (0-24). */
    public static int noteOf(BlockState state, BlockPos pos) {
        return state.getValue(COLOR).note(pos.getX() * 3L + pos.getY() * 5L + pos.getZ() * 7L);
    }

    /** Rings the crystal at the given note: a glassy chime, a coloured burst and a rising note. */
    public static void ring(ServerLevel level, BlockPos pos, BlockState state, int pitch, @Nullable Player player) {
        float sp = (float) Math.pow(2.0, (pitch - 12) / 12.0);
        Vec3 at = Vec3.atCenterOf(pos);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 1.0F, sp);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 0.35F, sp);
        level.playSound(null, pos, ModSounds.MUSIC_CRYSTAL_CHIME.get(), SoundSource.BLOCKS, 0.5F, sp);
        int rgb = state.getValue(COLOR).rgb();
        level.sendParticles(new DustParticleOptions(rgb, 1.0F), at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.6, at.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.NOTE, at.x, at.y + 0.8, at.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
        SongEvents.note(level, player, at, pitch);
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server) {
            ring(server, pos, state, noteOf(state, pos), player);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel server) {
            ring(server, pos, state, noteOf(state, pos), player);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(state.getValue(FROZEN) ? 3 : 8) == 0) {
            level.addParticle(ModParticles.GLOW_DUST.get(), pos.getX() + 0.2 + random.nextDouble() * 0.6, pos.getY() + 0.2 + random.nextDouble() * 0.6,
                    pos.getZ() + 0.2 + random.nextDouble() * 0.6, 0.0, 0.01, 0.0);
        }
    }
}
