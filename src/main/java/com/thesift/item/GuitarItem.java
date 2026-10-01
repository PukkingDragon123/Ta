package com.thesift.item;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The Guitar, strung with the Strummer's Magic Strings. Strum a chord: the song hurts every
 * hostile creature around you, and fills your tamed pets with it - stronger, faster, healing.
 */
public class GuitarItem extends Item {
    private static final double RADIUS = 7.0;
    private static final int[][] CHORDS = {{0, 4, 7, 12}, {5, 9, 12, 17}, {7, 11, 14, 19}, {-3, 0, 4, 9}};

    public GuitarItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            int[] chord = CHORDS[server.getRandom().nextInt(CHORDS.length)];
            server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUITAR_STRUM.get(), SoundSource.PLAYERS, 1.5F, 1.0F);
            for (int n : chord) {
                float pitch = (float) Math.pow(2.0, (n - 6) / 12.0);
                server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_GUITAR.value(), SoundSource.PLAYERS, 1.0F, pitch);
            }
            server.sendParticles(ModParticles.RESONANCE_RING.get(), player.getX(), player.getY() + 0.1, player.getZ(), 0, RADIUS, 0.0, 0.0, 1.0);
            server.sendParticles(ModParticles.SIFT_NOTE.get(), player.getX(), player.getY() + 1.4, player.getZ(), 16, 1.5, 0.6, 1.5, 1.0);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(RADIUS, 3.0, RADIUS))) {
                if (e == player || !e.isAlive() || e.distanceTo(player) > RADIUS) {
                    continue;
                }
                if (e instanceof OwnableEntity pet && pet.getOwner() == player) {
                    e.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 240, 1), player);
                    e.addEffect(new MobEffectInstance(MobEffects.SPEED, 240, 1), player);
                    e.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1), player);
                    server.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 3, 0.3, 0.2, 0.3, 0.0);
                    server.sendParticles(ModParticles.SIFT_NOTE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.5, e.getZ(), 3, 0.3, 0.2, 0.3, 1.0);
                } else if (e instanceof Enemy) {
                    if (e.hurtServer(server, server.damageSources().indirectMagic(player, player), 6.0F)) {
                        double dx = e.getX() - player.getX();
                        double dz = e.getZ() - player.getZ();
                        double d = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
                        e.push(dx / d * 0.6, 0.25, dz / d * 0.6);
                        server.sendParticles(ModParticles.SIFT_NOTE.get(), e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 2, 0.2, 0.2, 0.2, 1.0);
                    }
                }
            }
            player.getCooldowns().addCooldown(stack, 40);
        }
        return InteractionResult.SUCCESS;
    }
}
