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
 * Sneak and tap it to play single notes instead (pitch from where you look) - songs can be drummed.
 */
public class CongaDrumItem extends Item {
    public static final int COOLDOWN = 600;

    public CongaDrumItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isSecondaryUseActive()) {
            com.thesift.music.Notes.play(level, player, this.instrument(), com.thesift.music.Notes.lookPitch(player));
            return InteractionResult.SUCCESS;
        }
        if (level instanceof ServerLevel server) {
            this.boom(server, player);
            player.getCooldowns().addCooldown(stack, this.cooldown());
        }
        return InteractionResult.SUCCESS;
    }

    // --------------------------------------------------------------- what the prism drum changes

    protected double radius() {
        return 8.0;
    }

    protected int breakRadius() {
        return 3;
    }

    /** Damage at the very centre of the shockwave (it falls off to 4 at the edge). */
    protected float maxDamage() {
        return 12.0F;
    }

    protected int cooldown() {
        return COOLDOWN;
    }

    protected com.thesift.music.Instrument instrument() {
        return com.thesift.music.Instrument.DRUM;
    }

    private void boom(ServerLevel level, Player player) {
        double radius = this.radius();
        int breakR = this.breakRadius();
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        level.playSound(null, x, y, z, ModSounds.CONGA_DRUM_BOOM.get(), SoundSource.PLAYERS, 3.0F, 0.9F + level.getRandom().nextFloat() * 0.2F);
        for (int i = 0; i < 5; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), x, y + 0.1 + i * 0.3, z, 0, (2.0 + i * 1.6) * radius / 8.0, 0.0, 0.0, 1.0);
        }
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y + 0.5, z, 1, 0, 0, 0, 0);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), x, y + 1.5, z, 24, 3.0, 1.0, 3.0, 1.0);
        // soft blocks shatter (never what you stand on)
        BlockPos feet = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(feet.offset(-breakR, 0, -breakR), feet.offset(breakR, 2, breakR))) {
            if (p.distSqr(feet) > breakR * breakR + 1 || !level.mayInteract(player, p)) {
                continue;
            }
            BlockState s = level.getBlockState(p);
            float hardness = s.getDestroySpeed(level, p);
            if (!s.isAir() && hardness >= 0.0F && hardness <= 0.8F && !s.hasBlockEntity() && s.getFluidState().isEmpty()) {
                level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, s), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.1);
                level.destroyBlock(p, true, player);
            }
        }
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(feet).inflate(radius, 4.0, radius))) {
            if (e == player || !e.isAlive() || (e instanceof TamableAnimal t && t.isOwnedBy(player)) || e.distanceTo(player) > radius) {
                continue;
            }
            double dx = e.getX() - x;
            double dz = e.getZ() - z;
            double d = Math.max(0.3, Math.sqrt(dx * dx + dz * dz));
            double k = 1.0 - d / (radius + 1.0);
            if (!(e instanceof Player)) {
                e.hurtServer(level, level.damageSources().playerAttack(player), (float) (4.0 + (this.maxDamage() - 4.0) * k));
            }
            e.push(dx / d * (1.2 + 1.8 * k), 0.5 + 0.5 * k, dz / d * (1.2 + 1.8 * k));
        }
        com.thesift.music.Resonance.pulse(level, feet, 1.0F, (int) (radius * 1.5));
        com.thesift.entity.Stomper.hearDrum(level, player.position(), 24.0);
    }
}
