package com.thesift.item;

import com.thesift.block.SculkBloomBlock;
import com.thesift.registry.ModBlocks;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The Sculk Bloom as an item: it plants like any flower, and held in either hand it calms every
 * Warden within 16 blocks (a picked bloom no longer puffs smoke).
 */
public class SculkBloomItem extends BlockItem {
    public SculkBloomItem(Item.Properties properties) {
        super(ModBlocks.SCULK_BLOOM.get(), properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        super.inventoryTick(stack, level, owner, slot);
        if (owner.tickCount % 10 == 0 && owner instanceof LivingEntity holder && (holder.getMainHandItem() == stack || holder.getOffhandItem() == stack)) {
            SculkBloomBlock.calmWardens(level, holder.position(), SculkBloomBlock.CALM_RADIUS);
        }
    }
}
