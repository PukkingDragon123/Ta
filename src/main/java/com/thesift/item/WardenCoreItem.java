package com.thesift.item;

import com.thesift.block.SculkSummonerBlock;
import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;

/**
 * The still-beating heart of a Warden. It throbs softly in your inventory. MANSION: used on a Sculk Catalyst it settles in
 * the catalyst's claws and makes a Sculk Summoner, which wakes a Sift gate to the Sift Symphony.
 */
public class WardenCoreItem extends Item {
    public WardenCoreItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.getLevel().getBlockState(context.getClickedPos()).is(Blocks.SCULK_CATALYST)) {
            return InteractionResult.PASS;
        }
        if (context.getLevel() instanceof ServerLevel level && SculkSummonerBlock.arm(level, context.getClickedPos(), context.getPlayer())) {
            context.getItemInHand().consume(1, context.getPlayer());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (owner.tickCount % 60 == 0 && level.getRandom().nextInt(4) == 0) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.PLAYERS, 0.25F, 0.7F);
        }
    }
}
