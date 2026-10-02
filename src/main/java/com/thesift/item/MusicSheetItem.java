package com.thesift.item;

import com.thesift.music.Notes;
import com.thesift.music.Song;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A Music Sheet ({@code thesift:music_sheet_<song>}): one of the Sift's songs, written out note by
 * note. Holding it shows the notes on screen; using it pins them there (use again to unpin) so you
 * can read them while you play. Carry it to perform its song on any instrument.
 */
public class MusicSheetItem extends Item {
    private @Nullable Song song;

    public MusicSheetItem(Item.Properties properties) {
        super(properties);
    }

    /** The song on this sheet (from the item id, {@code music_sheet_<song id>}). */
    public Song song() {
        if (this.song == null) {
            String path = BuiltInRegistries.ITEM.getKey(this).getPath();
            this.song = Song.valueOf(path.substring("music_sheet_".length()).toUpperCase(Locale.ROOT));
        }
        return this.song;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            Notes.clientPinned = Notes.clientPinned == this.song() ? null : this.song();
        } else {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.1F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        Song s = this.song();
        builder.accept(Component.translatable("song.thesift." + s.id() + ".desc").withStyle(ChatFormatting.GRAY));
        StringBuilder notes = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            notes.append(i == 0 ? "" : " ").append(Notes.name(s.note(i)));
        }
        builder.accept(Component.literal(notes.toString()).withStyle(ChatFormatting.DARK_AQUA));
        builder.accept(Component.translatable("item.thesift.music_sheet.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
