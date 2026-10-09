package com.thesift.item;

import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * F3: the Knowledge Book (it grew out of the old Sift Codex): records every creature, song, enchantment, story, place
 * and recipe you discover. Crafted from a book, an ink sac and a gold nugget; the Mini Creator hands you one too.
 */
public class SiftKnowledgeBookItem extends Item {
    public SiftKnowledgeBookItem(Item.Properties properties) {
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

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thesift.knowledge_book.desc").withStyle(ChatFormatting.GRAY));
    }
}
