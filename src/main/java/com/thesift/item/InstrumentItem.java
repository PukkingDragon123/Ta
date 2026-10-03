package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.InstrumentPlay;
import com.thesift.music.Notes;
import com.thesift.music.Song;
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
 * M1 instrument play: an instrument you play on its own screen. Use it and its play screen opens
 * (strings to pick, drums to beat in rhythm, chimes to strike as they swing past the mark, a flute
 * to finger and blow); every note goes to the server ({@link InstrumentPlay}) and on to the songs.
 * Normal instruments have no powers of their own - better versions play more notes, or play in a
 * new way, and the Prism versions play every note in a colour of light.
 */
public class InstrumentItem extends Item {
    private final Instrument instrument;

    public InstrumentItem(Instrument instrument, Item.Properties properties) {
        super(properties);
        this.instrument = instrument;
    }

    /** The version of its family this item plays. */
    public Instrument instrument() {
        return this.instrument;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            InstrumentPlay.clientOpen.accept(hand);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Instrument i = this.instrument;
        builder.accept(Component.translatable("instrument.thesift.play." + i.family().id()).withStyle(ChatFormatting.GRAY));
        builder.accept(Component.translatable("instrument.thesift.range", i.noteCount(), Notes.name(i.lowest()), Notes.name(i.highest()))
                .withStyle(ChatFormatting.DARK_AQUA));
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
