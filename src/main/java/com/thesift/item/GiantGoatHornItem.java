package com.thesift.item;

import com.thesift.entity.mansion.HornBlast;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * MANSION: the Giant Goat Horn - three goat horns, a copper mouthpiece and a leather grip (or the Hornblower's own). Use
 * it: it bellows a deep call and a gust throws everything in the cone in front of you back and up ({@link HornBlast});
 * you hold it to your lips like a goat horn while it sounds, and it needs {@link #COOLDOWN} ticks before the next blast.
 */
public class GiantGoatHornItem extends Item {
    public static final int COOLDOWN = 100;
    private static final int SOUNDING = 30;

    public GiantGoatHornItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server) {
            HornBlast.blast(server, player, player.getLookAngle(), 1.0F, 0.0F);
            player.getCooldowns().addCooldown(stack, COOLDOWN);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        // a player sounds it for a breath; a Hornblower holds it at his lips until he blasts (his goal lowers it)
        return user instanceof Player ? SOUNDING : 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thesift.giant_goat_horn.desc").withStyle(ChatFormatting.GRAY));
    }
}
