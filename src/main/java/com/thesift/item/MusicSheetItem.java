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
 * can read them while you play. Carry it to perform its song on the instrument it names
 * ({@link Song#instrument()}); the tooltip lists the notes - with their lengths for a drum song and
 * their lights for a Prism song (M1 instrument play).
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
        // the instrument it needs, then every note - with its length (drum songs) and its light (Prism songs)
        builder.accept(Component.translatable("item.thesift.music_sheet.instrument", Component.translatable(s.instrumentKey()))
                .withStyle(ChatFormatting.GOLD));
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            line.append(line.isEmpty() ? "" : "   ").append(i + 1).append(". ").append(Notes.name(s.note(i)));
            if (s.rhythmic()) {
                line.append(' ').append(s.beat(i) >= 4 ? "\u2669\u2669" : s.beat(i) >= 2 ? "\u2669" : "\u266A");
            }
            if (i % 4 == 3 || i == s.length() - 1) {
                builder.accept(Component.literal(line.toString()).withStyle(ChatFormatting.DARK_AQUA));
                line.setLength(0);
            }
        }
        if (s.prism()) {
            net.minecraft.network.chat.MutableComponent lights = Component.empty();
            for (int i = 0; i < s.length(); i++) {
                if (i > 0) {
                    lights.append(Component.literal(" "));
                }
                lights.append(Component.literal("\u25CF").withColor(com.thesift.music.PrismLight.rgb(s.colour(i))));
            }
            builder.accept(Component.translatable("item.thesift.music_sheet.lights", lights).withStyle(ChatFormatting.GRAY));
        }
        if (s.rhythmic()) {
            builder.accept(Component.translatable("item.thesift.music_sheet.rhythm").withStyle(ChatFormatting.DARK_GRAY));
        }
        builder.accept(Component.translatable("item.thesift.music_sheet.hint").withStyle(ChatFormatting.DARK_GRAY));
    }
}
