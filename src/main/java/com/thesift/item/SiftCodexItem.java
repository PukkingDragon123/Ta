package com.thesift.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** The Sift Codex: opens the illustrated field guide to The Sift. */
public class SiftCodexItem extends Item {
    public SiftCodexItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            // only ever reached on the client, so the screen class is never loaded on a server
            com.thesift.client.codex.CodexOpener.open();
        }
        return InteractionResult.SUCCESS;
    }
}
