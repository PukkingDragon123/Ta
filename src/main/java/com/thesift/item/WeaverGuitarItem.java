package com.thesift.item;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.world.TemporaryBlocks;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * The Weaver's Guitar, the Weaver's own instrument - a boss's spoils, so unlike the normal
 * instruments it keeps a power. Use it and it plays like the Star Lute (six strings, chords: M1
 * instrument play), but strum it while sneaking and it weaves: a ring of Musical Cobwebs springs up
 * around you, and every hostile creature nearby is snared in silk where it stands - bounced, bound
 * and slowed. Then the strings need a while to settle.
 */
public class WeaverGuitarItem extends GuitarItem {
    private static final double RADIUS = 6.0;
    private static final int WEAVE_COOLDOWN = 160;

    public WeaverGuitarItem(Item.Properties properties) {
        super(com.thesift.music.Instrument.WEAVER_GUITAR, properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!player.isShiftKeyDown()) {
            return super.use(level, player, hand);
        }
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            int note = noteFor(player);
            strum(server, player, com.thesift.music.Instrument.WEAVER_GUITAR, note);
            server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.GUITAR_STRUM.get(), SoundSource.PLAYERS, 1.5F, 0.8F);
            server.sendParticles(ModParticles.RESONANCE_RING.get(), player.getX(), player.getY() + 0.1, player.getZ(), 0, RADIUS, 0.0, 0.0, 1.0);
            TemporaryBlocks.webRing(server, player.blockPosition(), 3.5, 200);
            DustParticleOptions silk = new DustParticleOptions(0x7FF7FF, 1.0F);
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(RADIUS, 3.0, RADIUS))) {
                if (e == player || !(e instanceof Enemy) || !e.isAlive() || e.distanceTo(player) > RADIUS) {
                    continue;
                }
                // a strand flies to it and snares it where it stands
                for (int i = 1; i < 8; i++) {
                    double f = i / 8.0;
                    server.sendParticles(silk, player.getX() + (e.getX() - player.getX()) * f, player.getY() + 1.2 + (e.getY() + e.getBbHeight() * 0.5 - player.getY() - 1.2) * f,
                            player.getZ() + (e.getZ() - player.getZ()) * f, 1, 0, 0, 0, 0);
                }
                TemporaryBlocks.web(server, e.blockPosition(), 140);
                TemporaryBlocks.web(server, e.blockPosition().above(), 140);
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 3), player);
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0), player);
                e.hurtServer(server, server.damageSources().indirectMagic(player, player), 3.0F);
                e.push(0.0, 0.35, 0.0);
                server.sendParticles(ModParticles.SIFT_NOTE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 3, 0.3, 0.2, 0.3, 1.0);
            }
            player.getCooldowns().addCooldown(stack, WEAVE_COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }
}
