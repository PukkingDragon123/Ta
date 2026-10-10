package com.thesift.item;

import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaveJungle;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: the Glow Lamp - a Glow Fly's light gland in a cage of copper. Held in either hand it pulses with a
 * soft light that follows you (the lamp's charge is the bar under it, and slowly drains while it shines). Use it for
 * one great pulse - a burst of light that hangs in the air around you for a while and dazzles monsters close by - at
 * a quarter of a full charge. It recharges itself in bright light: hold it, or keep it, where the light is strong.
 */
public class GlowLampItem extends Item {
    private static final int PULSE_COST = 24;

    public GlowLampItem(Item.Properties properties) {
        super(properties);
    }

    private static int charge(ItemStack stack) {
        return stack.getMaxDamage() - 1 - stack.getDamageValue();
    }

    private static void spend(ItemStack stack, int n) {
        stack.setDamageValue(Math.min(stack.getMaxDamage() - 1, stack.getDamageValue() + n));
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (owner.tickCount % 20 == 0) {
            // recharge in strong light
            int light = level.getMaxLocalRawBrightness(owner.blockPosition());
            if (light >= 13 && stack.getDamageValue() > 0) {
                stack.setDamageValue(stack.getDamageValue() - 1);
            }
        }
        boolean held = owner instanceof LivingEntity holder && (holder.getMainHandItem() == stack || holder.getOffhandItem() == stack);
        if (!held || charge(stack) <= 0) {
            return;
        }
        // the pulse: a moment of light at your head, every second
        if (owner.tickCount % 20 == 0) {
            BlockPos head = BlockPos.containing(owner.getEyePosition());
            for (BlockPos p : new BlockPos[]{head, head.above(), owner.blockPosition()}) {
                if (level.getBlockState(p).isAir()) {
                    level.setBlock(p, ModBlocks.LINGERING_GLOW.get().defaultBlockState(), Block.UPDATE_ALL);
                    level.scheduleTick(p.immutable(), ModBlocks.LINGERING_GLOW.get(), 24);
                    break;
                }
            }
            Vec3 e = owner.getEyePosition();
            level.sendParticles(ModParticles.GLOW_DUST.get(), e.x, e.y - 0.4, e.z, 2, 0.2, 0.2, 0.2, 0.0);
        }
        if (owner.tickCount % 120 == 0) {
            spend(stack, 1);
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (charge(stack) < PULSE_COST) {
            return InteractionResult.FAIL;
        }
        if (level instanceof ServerLevel server) {
            Vec3 at = player.getEyePosition();
            server.playSound(null, at.x, at.y, at.z, ModCaveJungle.GLOW_FLY_FLASH.get(), SoundSource.PLAYERS, 1.0F, 1.2F);
            server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 30, 0.3, 0.3, 0.3, 0.2);
            server.sendParticles(ModParticles.GLOW_DUST.get(), at.x, at.y, at.z, 40, 2.5, 1.5, 2.5, 0.02);
            BlockPos c = BlockPos.containing(at);
            int placed = 0;
            for (BlockPos p : BlockPos.betweenClosed(c.offset(-3, -2, -3), c.offset(3, 2, 3))) {
                if (placed >= 8) {
                    break;
                }
                if ((p.getX() + p.getY() * 3 + p.getZ() * 7 + server.getRandom().nextInt(4)) % 4 != 0 || !server.getBlockState(p).isAir()) {
                    continue;
                }
                server.setBlock(p, ModBlocks.LINGERING_GLOW.get().defaultBlockState(), Block.UPDATE_ALL);
                server.scheduleTick(p.immutable(), ModBlocks.LINGERING_GLOW.get(), 20 * (10 + server.getRandom().nextInt(6)));
                placed++;
            }
            for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(6.0), v -> v instanceof Enemy && v.isAlive())) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 80, 0), player);
                e.addEffect(new MobEffectInstance(MobEffects.GLOWING, 200, 0), player);
            }
            spend(stack, PULSE_COST);
            player.getCooldowns().addCooldown(stack, 40);
        }
        return InteractionResult.SUCCESS;
    }
}
