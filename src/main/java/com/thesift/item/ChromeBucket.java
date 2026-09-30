package com.thesift.item;

import com.thesift.registry.ModFluids;
import net.minecraft.world.item.BucketItem;

public class ChromeBucket extends BucketItem {
    public ChromeBucket(Properties properties) {
        super(ModFluids.CHROME.get(), properties);
    }
}
