package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.registry.ModParticles;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * The Prism Harp: a lap harp strung between prism gems. Every note it plays (pitch from where you
 * look, as with every instrument) is a healing one: you, other players and your tamed creatures
 * close by mend a little with each note.
 */
public class PrismHarpItem extends Item {
    public static final double RADIUS = 8.0;
    public static final float HEAL = 1.0F;
    public static final int NOTE_COOLDOWN = 6;

    public PrismHarpItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        Notes.play(level, player, Instrument.HARP, Notes.lookPitch(player));
        if (level instanceof ServerLevel server) {
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(RADIUS),
                    m -> m.isAlive() && m.distanceToSqr(player) <= RADIUS * RADIUS && (m instanceof Player || m instanceof OwnableEntity pet && pet.getOwner() == player))) {
                if (e.getHealth() < e.getMaxHealth()) {
                    e.heal(e == player ? HEAL * 0.5F : HEAL);
                    server.sendParticles(ParticleTypes.HEART, e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
                }
                server.sendParticles(ModParticles.STAR_SPARKLE.get(), e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
        // both sides, so the client never plays (and the guide never counts) a note the server refuses
        player.getCooldowns().addCooldown(player.getItemInHand(hand), NOTE_COOLDOWN);
        return InteractionResult.SUCCESS;
    }
}
