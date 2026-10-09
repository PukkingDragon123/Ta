package com.thesift.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * W-deep caves: the Shocker Plant of the Cave Jungle. Its bulb builds up a charge (it glows and crackles); anything that
 * brushes against it - or strikes it - takes the whole charge as a forked spark: a jolt of lightning damage, a shove and a
 * few seconds of numb, heavy legs. Spent, it goes dark and recharges after a few seconds.
 */
public class ShockerPlantBlock extends VegetationBlock {
    public static final BooleanProperty CHARGED = BooleanProperty.create("charged");
    private static final VoxelShape SHAPE = Block.column(10.0, 0.0, 14.0);
    private static final int RECHARGE_TICKS = 70;

    public ShockerPlantBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(CHARGED, true));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(CHARGED);
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.isFaceSturdy(level, pos, Direction.UP) || super.mayPlaceOn(state, level, pos);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE.move(state.getOffset(pos));
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effectApplier, boolean isPrecise) {
        if (level instanceof ServerLevel server && state.getValue(CHARGED) && entity instanceof LivingEntity living && canZap(living)) {
            zap(server, pos, state, living);
        }
    }

    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        if (level instanceof ServerLevel server && state.getValue(CHARGED) && canZap(player)) {
            zap(server, pos, state, player);
        }
    }

    private static boolean canZap(LivingEntity e) {
        return e.isAlive() && !(e instanceof Player p && (p.isCreative() || p.isSpectator()));
    }

    /** Discharges the bulb into {@code target}: a jagged arc of sparks from the bulb to it, a jolt, a shove and slowness. */
    public static void zap(ServerLevel level, BlockPos pos, BlockState state, LivingEntity target) {
        Vec3 bulb = Vec3.atBottomCenterOf(pos).add(state.getOffset(pos)).add(0.0, 0.75, 0.0);
        Vec3 hit = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        arc(level, bulb, hit, level.getRandom());
        target.hurtServer(level, level.damageSources().lightningBolt(), 3.0F);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 50, 2));
        Vec3 away = new Vec3(hit.x - bulb.x, 0.0, hit.z - bulb.z);
        if (away.lengthSqr() < 1.0E-4) {
            float a = level.getRandom().nextFloat() * Mth.TWO_PI;
            away = new Vec3(Mth.cos(a), 0.0, Mth.sin(a));
        }
        away = away.normalize().scale(0.4);
        target.push(away.x, 0.2, away.z);
        level.playSound(null, pos, SoundEvents.LIGHTNING_BOLT_IMPACT, SoundSource.BLOCKS, 0.55F, 1.6F + level.getRandom().nextFloat() * 0.3F);
        level.playSound(null, pos, SoundEvents.BEEHIVE_DRIP, SoundSource.BLOCKS, 0.6F, 0.5F);
        level.setBlock(pos, state.setValue(CHARGED, false), Block.UPDATE_ALL);
        level.scheduleTick(pos, state.getBlock(), RECHARGE_TICKS + level.getRandom().nextInt(40));
    }

    /** A forked lightning arc of sparks between two points. */
    public static void arc(ServerLevel level, Vec3 from, Vec3 to, RandomSource random) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        int steps = Math.max(4, (int) (len * 7.0));
        Vec3 side = Math.abs(d.y) < 0.9 * len ? d.cross(new Vec3(0.0, 1.0, 0.0)).normalize() : new Vec3(1.0, 0.0, 0.0);
        Vec3 up = side.cross(d).normalize();
        double jx = 0.0;
        double jy = 0.0;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double reach = Math.sin(t * Math.PI) * 0.35;
            jx = Mth.clamp(jx + (random.nextDouble() - 0.5) * 0.3, -reach, reach);
            jy = Mth.clamp(jy + (random.nextDouble() - 0.5) * 0.3, -reach, reach);
            Vec3 p = from.add(d.scale(t)).add(side.scale(jx)).add(up.scale(jy));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.WAX_OFF, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
            }
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.getValue(CHARGED)) {
            level.setBlock(pos, state.setValue(CHARGED, true), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 0.25F, 1.9F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Vec3 off = state.getOffset(pos);
        double x = pos.getX() + 0.5 + off.x;
        double y = pos.getY() + 0.78;
        double z = pos.getZ() + 0.5 + off.z;
        if (state.getValue(CHARGED)) {
            if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, x + (random.nextDouble() - 0.5) * 0.4, y + (random.nextDouble() - 0.3) * 0.3,
                        z + (random.nextDouble() - 0.5) * 0.4, (random.nextDouble() - 0.5) * 0.2, random.nextDouble() * 0.1,
                        (random.nextDouble() - 0.5) * 0.2);
            }
        } else if (random.nextInt(6) == 0) {
            level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.01, 0.0);
        }
    }
}
