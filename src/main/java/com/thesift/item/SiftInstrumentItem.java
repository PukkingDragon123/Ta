package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.component.UseEffects;
import net.minecraft.world.level.Level;

/**
 * An instrument, played freely in the world (INS free play). Hold use and you raise it into its
 * playing stance - the guitar across your body, the flute to your lips, the drums before you, the
 * chimes held up - and, while it is raised, the number keys 1-9 play its notes, the mouse wheel
 * shifts the register and attack accents or strums (client {@code FreePlay}); let go and you lower
 * it. Everyone around sees the stance, the hands and the notes ({@link com.thesift.music.InstrumentPlay}).
 * Normal instruments have no powers of their own - better versions play more notes, or play in a
 * new way, and the Prism versions play every note in a colour of light.
 */
public class SiftInstrumentItem extends Item {
    private final Instrument instrument;

    public SiftInstrumentItem(Instrument instrument, Item.Properties properties) {
        // while it is raised you can still walk - slowly, like a marching band - but not sprint
        super(properties.component(DataComponents.USE_EFFECTS, new UseEffects(false, false, 0.55F)));
        this.instrument = instrument;
    }

    /** The version of its family this item plays. */
    public Instrument instrument() {
        return this.instrument;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // raise it: the stance lasts while use is held (vanilla's using-item state, so every player sees it)
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.NONE;
    }

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Instrument i = this.instrument;
        builder.accept(Component.translatable("instrument.thesift.play.raise").withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("instrument.thesift.play." + i.family().id()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("instrument.thesift.range", i.noteCount(), Notes.name(i.lowest()), Notes.name(i.highest()))
                .withStyle(ChatFormatting.DARK_AQUA));
        if (i.registerCount() > 1) {
            builder.accept(Component.translatable("instrument.thesift.registers", i.registerCount()).withStyle(ChatFormatting.DARK_AQUA));
        }
        if (i.hasMechanic()) {
            builder.accept(Component.translatable("instrument.thesift.mechanic." + i.family().id()).withStyle(ChatFormatting.AQUA));
        }
        if (i.prism()) {
            builder.accept(Component.translatable("instrument.thesift.mechanic.prism").withStyle(ChatFormatting.LIGHT_PURPLE));
        }
        for (Song song : Song.values()) {
            if (song.accepts(i) && (song.instrument() != null || song.prism())) {
                builder.accept(Component.literal("♪ ").append(Component.translatable("song.thesift." + song.id())).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }
}
