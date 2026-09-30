package com.thesift.item;

import com.thesift.registry.ModSounds;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The still-beating heart of a Warden. It throbs softly in your inventory. Slotted into a Sift Drum
 * it begins the rhythm that opens the Ancient City's portal.
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
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, EquipmentSlot slot) {
        if (owner.tickCount % 60 == 0 && level.getRandom().nextInt(4) == 0) {
            level.playSound(null, owner.getX(), owner.getY(), owner.getZ(), ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.PLAYERS, 0.25F, 0.7F);
        }
    }
}
