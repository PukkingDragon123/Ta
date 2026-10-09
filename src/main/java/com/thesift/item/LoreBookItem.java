package com.thesift.item;

import com.thesift.knowledge.Lore;
import com.thesift.knowledge.LoreReading;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * F3: a Lore Book. Use it to read the piece of the Sift's story written in it ({@code thesift:lore}); sneak and use it
 * on a block to set it down as a decoration (it keeps its text).
 */
public class LoreBookItem extends BlockItem {
    public LoreBookItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!context.isSecondaryUseActive()) {
            return InteractionResult.PASS; // not sneaking: read it instead (use)
        }
        return super.useOn(context);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        LoreReading.read(level, player, Lore.of(player.getItemInHand(hand)));
        return InteractionResult.SUCCESS;
    }

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        LoreScrollItem.tooltip(stack, builder, true);
    }
}
