package com.thesift.block;

import com.thesift.registry.ModEffects;
import com.thesift.registry.ModSounds;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sculk Bloom: a rare dark flower that now and then grows on a Bulb's back (give a Bulb any small
 * flower and it may well pluck you this one). Its hum is too low to hear, but Wardens feel it:
 * every Warden within 16 blocks of a planted or potted bloom - or of a Bulb carrying one, or of
 * anyone holding one - forgets its anger and stops hunting. The price: every few seconds a planted
 * bloom puffs a cloud of dark Sculk smoke, and whatever breathes it in is briefly Sculk Corrupted.
 */
public class SculkBloomBlock extends SiftFlowerBlock {
    /** How far the calm reaches. */
    public static final double CALM_RADIUS = 16.0;
    /** Ticks between two hums; about one in four also puffs smoke. */
    static final int HUM_INTERVAL = 20;
    /** The smoke: a deep sculk teal. */
    private static final int SMOKE = 0x0F2E36;

    public SculkBloomBlock(Holder<MobEffect> effect, float seconds, int light, BlockBehaviour.Properties properties) {
        super(effect, seconds, light, properties);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        level.scheduleTick(pos, this, HUM_INTERVAL);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    /** A safety net: a bloom that lost its tick picks its hum back up. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            level.scheduleTick(pos, this, HUM_INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        hum(level, Vec3.atBottomCenterOf(pos).add(0.0, 0.55, 0.0), null, random.nextInt(4) == 0);
        level.scheduleTick(pos, this, HUM_INTERVAL);
    }

    @Override
    protected ParticleOptions ambientParticle() {
        return ParticleTypes.SCULK_CHARGE_POP;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        super.animateTick(state, level, pos, random);
        if (random.nextInt(5) == 0) {
            // a thin dark wisp always curls off the petals
            level.addParticle(new DustParticleOptions(SMOKE, 0.9F), pos.getX() + 0.35 + random.nextDouble() * 0.3, pos.getY() + 0.6,
                    pos.getZ() + 0.35 + random.nextDouble() * 0.3, 0.0, 0.02, 0.0);
        }
    }

    /**
     * The bloom's hum, from wherever it is (planted, potted or on a Bulb's back): every Warden in
     * range calms down; with {@code puff} it also blows a cloud of Sculk smoke that briefly
     * corrupts whatever stands in it (except the {@code carrier}).
     */
    public static void hum(ServerLevel level, Vec3 at, @Nullable Entity carrier, boolean puff) {
        calmWardens(level, at, CALM_RADIUS);
        if (puff) {
            puff(level, at, carrier, 1.0F);
        }
    }

    /** Every Warden within {@code radius} forgets who it was angry at and stops hunting. */
    public static void calmWardens(ServerLevel level, Vec3 at, double radius) {
        for (Warden warden : level.getEntitiesOfClass(Warden.class, new AABB(at, at).inflate(radius), Warden::isAlive)) {
            boolean wasAngry = warden.getEntityAngryAt().isPresent() || warden.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
            for (int i = 0; i < 8; i++) {
                Optional<LivingEntity> suspect = warden.getEntityAngryAt();
                if (suspect.isEmpty()) {
                    break;
                }
                warden.clearAnger(suspect.get());
            }
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
            // and it stops listening for a moment, so the next footstep does not set it off again
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 40L);
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 40L);
            if (wasAngry) {
                level.playSound(null, warden.getX(), warden.getY(), warden.getZ(), ModSounds.SCULK_BLOOM_CALM.get(), SoundSource.HOSTILE, 1.0F, 0.9F);
                level.sendParticles(ParticleTypes.SCULK_SOUL, warden.getX(), warden.getY() + warden.getBbHeight() + 0.2, warden.getZ(), 6, 0.4, 0.2, 0.4,
                        0.02);
            }
        }
    }

    /** A cloud of dark Sculk smoke; living things in it (but Wardens and the carrier) are briefly corrupted. */
    public static void puff(ServerLevel level, Vec3 at, @Nullable Entity carrier, float size) {
        RandomSource random = level.getRandom();
        level.sendParticles(new DustParticleOptions(SMOKE, 2.2F * size), at.x, at.y + 0.35 * size, at.z, Math.max(4, (int) (16 * size)), 0.2 * size,
                0.32 * size, 0.2 * size, 0.01);
        level.sendParticles(ParticleTypes.LARGE_SMOKE, at.x, at.y + 0.2, at.z, Math.max(1, (int) (4 * size)), 0.12 * size, 0.1, 0.12 * size, 0.015);
        level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y + 0.4, at.z, Math.max(1, (int) (2 * size)), 0.15, 0.2, 0.15, 0.02);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, at.x, at.y + 0.3, at.z, Math.max(2, (int) (6 * size)), 0.25 * size, 0.3 * size, 0.25 * size,
                0.01);
        level.playSound(null, at.x, at.y, at.z, ModSounds.SCULK_BLOOM_PUFF.get(), SoundSource.BLOCKS, 0.4F + 0.4F * size, 0.8F + random.nextFloat() * 0.3F);
        double r = 1.3 * size;
        AABB cloud = new AABB(at.x - r, at.y - 0.6, at.z - r, at.x + r, at.y + 2.2 * size, at.z + r);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, cloud, e -> e.isAlive() && e != carrier && !(e instanceof Warden))) {
            if (e instanceof Player player && (player.isCreative() || player.isSpectator())) {
                continue;
            }
            e.addEffect(new MobEffectInstance(ModEffects.SCULK_CORRUPTION, 80, 0));
        }
    }
}
