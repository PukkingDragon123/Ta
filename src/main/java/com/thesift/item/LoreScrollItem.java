package com.thesift.item;

import com.thesift.knowledge.Lore;
import com.thesift.knowledge.LoreReading;
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

/** F3: a Lore Scroll - a piece of the Sift's story ({@code thesift:lore}) on a rolled sheet. Use it to read. */
public class LoreScrollItem extends Item {
    public LoreScrollItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        LoreReading.read(level, player, Lore.of(player.getItemInHand(hand)));
        return InteractionResult.SUCCESS;
    }

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        tooltip(stack, builder, false);
    }

    /** Title (in the old script until read), who wrote it, and how to use it. */
    static void tooltip(ItemStack stack, Consumer<Component> builder, boolean placeable) {
        Lore lore = Lore.of(stack);
        if (lore == null) {
            builder.accept(Component.translatable("lore.thesift.blank").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        builder.accept(LoreReading.title(lore).withStyle(ChatFormatting.ITALIC));
        builder.accept(Component.translatable("knowledge.thesift.lore.from", Component.translatable("lore.thesift.origin." + lore.origin.id()))
                .withStyle(ChatFormatting.GRAY));
        if (!LoreReading.clientKnows.test(lore.key())) {
            builder.accept(Component.translatable("lore.thesift.tooltip.unread").withStyle(ChatFormatting.GOLD));
        }
        builder.accept(Component.translatable("lore.thesift.tooltip.read").withStyle(ChatFormatting.DARK_GRAY));
        if (placeable) {
            builder.accept(Component.translatable("lore.thesift.tooltip.place").withStyle(ChatFormatting.DARK_GRAY));
        }
    }
}
