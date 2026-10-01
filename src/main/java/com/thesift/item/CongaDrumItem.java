package com.thesift.item;

import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * The Thumper's Conga Drum. One beat and a massive shockwave rolls out of it: soft blocks around
 * you shatter, and everything nearby is hurt and hurled away. It takes half a minute to ring out.
 */
public class CongaDrumItem extends Item {
    public static final int COOLDOWN = 600;
    private static final double RADIUS = 8.0;
    private static final int BREAK_RADIUS = 3;

    public CongaDrumItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            boom(server, player);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
        }
        return InteractionResult.SUCCESS;
    }

    private static void boom(ServerLevel level, Player player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        level.playSound(null, x, y, z, ModSounds.CONGA_DRUM_BOOM.get(), SoundSource.PLAYERS, 3.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        for (int i = 0; i < 5; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), x, y + 0.1 + i * 0.3, z, 0, 2.0 + i * 1.6, 0.0, 0.0, 1.0);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.5, z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), x, y + 1.5, z, 24, 3.0, 1.0, 3.0, 1.0);
        // soft blocks shatter (never what you stand on)
        BlockPos feet = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-BREAK_RADIUS, 0, -BREAK_RADIUS), feet.offset(BREAK_RADIUS, 2, BREAK_RADIUS))) {
            if (p.distSqr(feet) > BREAK_RADIUS * BREAK_RADIUS + 1 || !level.mayInteract(player, p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            float hardness = s.getDestroySpeed(level, p);
            if (!s.isAir() && hardness >= 0.0F && hardness <= 0.8F && !s.hasBlockEntity() && s.getFluidState().isEmpty()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
                level.destroyBlock(p, true, player);
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(feet).inflate(RADIUS, 4.0, RADIUS))) {
            if (e == player || !e.isAlive() || (e instanceof TamableAnimal t && t.isOwnedBy(player)) || e.distanceTo(player) > RADIUS) {
                continue;
            }
            double dx = e.getX() - x;
            double dz = e.getZ() - z;
            double d = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
            double k = 1.0 - d / (RADIUS + 1.0);
            if (!(e instanceof Player)) {
                e.hurtServer(level, level.damageSources().playerAttack(player), (float) (4.0 + 8.0 * k));
            }
            e.push(dx / d * (1.2 + 1.8 * k), 0.5 + 0.5 * k, dz / d * (1.2 + 1.8 * k));
        }
        com.thesift.music.Resonance.pulse(level, feet, 1.0F, 12);
        com.thesift.entity.Stomper.hearDrum(level, player.position(), 24.0);
    }
}
