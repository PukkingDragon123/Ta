package com.thesift.block;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A near-invisible web of silvery threads strung across ruin corridors. Walking through it bursts
 * a cloud of sleep spores: slowness, darkness and a drowsy wobble for everything nearby.
 */
public class DreamSnareBlock extends Block {
    public static final BooleanProperty SPENT = BooleanProperty.create("spent");
    private static final VoxelShape SHAPE = Block.box(0.0, 1.0, 0.0, 16.0, 2.5, 16.0);

    public DreamSnareBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(SPENT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SPENT);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (state.getValue(SPENT) || !(level instanceof ServerLevel server) || !(entity instanceof LivingEntity)) {
            return;
        }
        server.playSound(null, pos, ModSounds.SNARE_TRIGGER.get(), SoundSource.BLOCKS, 1.0F, 0.9F + server.getRandom().nextFloat() * 0.2F);
        server.sendParticles(ModParticles.SLEEP_SPORE.get(), pos.getX() + 0.5, pos.getY() + 0.8, pos.getZ() + 0.5, 60, 1.6, 0.8, 1.6, 0.02);
        for (LivingEntity victim : server.getEntitiesOfClass(LivingEntity.class, new AABB(pos).inflate(3.5))) {
            victim.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 120, 2));
            victim.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 160, 0));
            victim.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 100, 0));
        }
        server.setBlock(pos, state.setValue(SPENT, true), Block.UPDATE_ALL);
        server.scheduleTick(pos, this, 20 * 45);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SPENT)) {
            level.setBlock(pos, state.setValue(SPENT, false), Block.UPDATE_ALL);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(SPENT) && random.nextInt(30) == 0) {
            level.addParticle(ModParticles.SLEEP_SPORE.get(), pos.getX() + random.nextDouble(), pos.getY() + 0.2, pos.getZ() + random.nextDouble(),
                    0, 0.005, 0);
        }
    }
}
