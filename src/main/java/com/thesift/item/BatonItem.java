package com.thesift.item;

import com.thesift.registry.ModSounds;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * The Conductor's Baton, taken from the Dictator. It strikes like a sword, and a flick of it
 * (use) sends a single sonic note down the line you point it along, hurting the first creature
 * in its way.
 */
public class BatonItem extends Item {
    private static final double REACH = 14.0;

    public BatonItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            Vec3 eye = player.getEyePosition();
            Vec3 look = player.getLookAngle();
            LivingEntity hit = null;
            double best = REACH;
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(REACH))) {
                if (e == player || !e.isAlive()) {
                    continue;
                }
                Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
                double along = to.dot(look);
                if (along > 0 && along < best && to.subtract(look.scale(along)).length() < 0.9 + e.getBbWidth() * 0.5 && player.hasLineOfSight(e)) {
                    best = along;
                    hit = e;
                }
            }
            for (double d = 1.0; d < best; d += 1.5) {
                Vec3 p = eye.add(look.scale(d));
                server.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
            }
            server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.BATON_NOTE.get(), SoundSource.PLAYERS, 1.5F,
                    0.9F + server.getRandom().nextFloat() * 0.3F);
            if (hit != null) {
                hit.hurtServer(server, player.damageSources().sonicBoom(player), 8.0F);
                Vec3 push = look.multiply(1, 0, 1).normalize();
                hit.push(push.x * 0.8, 0.2, push.z * 0.8);
            }
            player.getCooldowns().addCooldown(stack, 40);
            stack.hurtAndBreak(1, player, hand);
        }
        return InteractionResult.SUCCESS;
    }
}
