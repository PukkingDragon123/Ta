package com.thesift.item;

import com.thesift.registry.ModFluids;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.MobBucketItem;

/**
 * A Chrome Bucket with a fish in it - vanilla's fish bucket, filled with Chrome instead of water.
 * Emptying it pours the Chrome out and lets the fish (name, health and all) swim off again.
 * Fish are scooped up in {@link com.thesift.entity.SiftFish#canBePickedUpWithBucket}.
 */
public class ChromeFishBucketItem extends MobBucketItem {
    public ChromeFishBucketItem(EntityType<? extends Mob> type, Properties properties) {
        super(type, ModFluids.CHROME.get(), SoundEvents.BUCKET_EMPTY_FISH, properties);
    }
}
