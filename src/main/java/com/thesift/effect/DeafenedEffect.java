package com.thesift.effect;

import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Unit;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Deafened: the target cannot hear.
 *
 * <ul>
 *   <li>Players: the world goes muffled, then silent but for a ringing (client side, see
 *   {@code com.thesift.client.DunesClient}) - no creature cues, no music, no subtitles - and they cannot play:
 *   every note they play goes astray, so no song is ever heard ({@link #fumbles}, hooked into
 *   {@code SongEvents.note}).</li>
 *   <li>Wardens: their vibration listener stays on cooldown, they stop sniffing, and they forget whoever they
 *   were chasing when the effect lands.</li>
 *   <li>Other mobs: they lose track of a target they cannot see for a moment when it lands.</li>
 * </ul>
 * P4-DESERT: the Jaberora's piercing note ({@link #stun}) stuns as well as deafens.
 */
public class DeafenedEffect extends MobEffect {
    public DeafenedEffect(MobEffectCategory category, int color) {
        super(category, color);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity mob, int amplifier) {
        if (mob instanceof Warden warden) {
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.VIBRATION_COOLDOWN, Unit.INSTANCE, 20L);
            warden.getBrain().setMemoryWithExpiry(MemoryModuleType.SNIFF_COOLDOWN, Unit.INSTANCE, 20L);
            warden.getBrain().eraseMemory(MemoryModuleType.DISTURBANCE_LOCATION);
            if (warden.tickCount % 10 == 0) {
                level.sendParticles(ModParticles.SIFT_NOTE.get(), warden.getX(), warden.getY() + warden.getBbHeight() + 0.3, warden.getZ(),
                        0, 0.55, 0.0, 0.0, 1.0);
            }
        } else if (mob.tickCount % 20 == 0) {
            // a dull ring of grey notes over a deafened head
            level.sendParticles(ParticleTypes.SMOKE, mob.getX(), mob.getY() + mob.getBbHeight() + 0.25, mob.getZ(), 1, 0.15, 0.05, 0.15, 0.0);
        }
        return true;
    }

    @Override
    public void onEffectStarted(LivingEntity mob, int amplifier) {
        if (mob instanceof Warden warden) {
            warden.getEntityAngryAt().ifPresent(warden::clearAnger);
            warden.getBrain().eraseMemory(MemoryModuleType.ATTACK_TARGET);
            warden.getBrain().eraseMemory(MemoryModuleType.ROAR_TARGET);
        } else if (mob instanceof Mob m && m.getTarget() != null && !m.hasLineOfSight(m.getTarget())) {
            m.setTarget(null);
        }
    }

    /**
     * A deafened player playing an instrument: the note goes astray (a sour puff where it was played) and is not
     * heard by anything - no song, band or creature hears it. True if the note was lost.
     */
    public static boolean fumbles(ServerLevel level, Player player, Vec3 at) {
        if (!player.hasEffect(ModEffects.DEAFENED)) {
            return false;
        }
        level.sendParticles(ParticleTypes.SMOKE, at.x, at.y + 0.3, at.z, 3, 0.2, 0.1, 0.2, 0.01);
        if (player.tickCount % 40 < 4) {
            player.sendOverlayMessage(Component.translatable("message.thesift.deafened.fumble"));
        }
        return true;
    }

    /** A stunning note: the target reels (slowed almost to a stop, a mob loses its path), and it is Deafened for a while. */
    public static void stun(LivingEntity target, Entity source, int stunTicks, int deafTicks) {
        target.addEffect(new MobEffectInstance(ModEffects.DEAFENED, deafTicks, 0), source);
        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, stunTicks, 4), source);
        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, stunTicks, 1), source);
        if (target instanceof Player) {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, stunTicks + 40, 0), source);
        }
        if (target instanceof Mob m) {
            m.getNavigation().stop();
        }
        Vec3 push = target.position().subtract(source.position()).normalize().scale(0.45);
        target.push(push.x, 0.15, push.z);
    }
}
