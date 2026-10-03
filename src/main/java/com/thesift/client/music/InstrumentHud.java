package com.thesift.client.music;

import com.thesift.TheSift;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.item.MusicSheetItem;
import com.thesift.music.Instrument;
import com.thesift.music.InstrumentPlay;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;

/**
 * The song guide on the HUD: a Music Sheet in hand, a pinned sheet, or - while you hold an
 * instrument - the carried sheet that instrument can play. Its notes sit on a parchment staff in the
 * top-left corner, lighting up as you play them; the next one is boxed. A drum song's notes carry
 * their lengths under the staff, a Prism song's its lamps of coloured light. The bottom line says
 * to use the instrument (its play screen then takes over the guide), or which instrument the song
 * needs if yours will not do.
 *
 * <p>M1 instrument play: this is also where the client hooks {@link InstrumentPlay#clientOpen} up
 * to {@link InstrumentScreen#open}, so using an instrument opens its play screen.
 */
public final class InstrumentHud {
    private static final int NOTE_W = 18;
    private static final int INK = 0xFF4A3020;
    private static final int FADED = 0xFF8A6A4A;

    private InstrumentHud() {
    }

    public static void registerOverlays(RegisterGuiLayersEvent event) {
        InstrumentPlay.clientOpen = InstrumentScreen::open; // M1 instrument play: using an instrument opens its play screen
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TheSift.id("music_sheet"), InstrumentHud::drawSheet);
    }

    /** The instrument in hand (main hand first), or null. */
    private static @Nullable Instrument heldInstrument(LocalPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof SiftInstrumentItem item) {
                return item.instrument();
            }
        }
        return null;
    }

    private static boolean fits(Song song, @Nullable Instrument held) {
        return held == null ? song.instrument() == null && !song.prism() : song.accepts(held);
    }

    /**
     * The sheet to show: one in hand, else the pinned one while it is still carried, else - while
     * an instrument is in hand - the carried sheet it can play (the one furthest along, songs
     * written for this very instrument first), or failing that the first carried sheet.
     */
    private static @Nullable Song shownSheet(LocalPlayer p, @Nullable Instrument held) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof MusicSheetItem sheet) {
                return sheet.song();
            }
        }
        Song pinned = Notes.clientPinned;
        if (pinned != null) {
            if (SongTracker.carriesSheet(p, pinned)) {
                return pinned;
            }
            Notes.clientPinned = null;
        }
        if (held == null) {
            return null;
        }
        long now = p.level().getGameTime();
        Song best = null;
        Song first = null;
        int bestScore = -1;
        for (Song s : Song.values()) {
            if (!SongTracker.carriesSheet(p, s)) {
                continue;
            }
            if (first == null) {
                first = s;
            }
            if (fits(s, held)) {
                int score = Notes.clientProgress(s, now) * 4 + (s.prism() ? 2 : 0) + (s.instrument() != null ? 1 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
        }
        return best != null ? best : first;
    }

    private static void drawSheet(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.gui.screen() instanceof InstrumentScreen) {
            return;
        }
        Instrument held = heldInstrument(p);
        Song song = shownSheet(p, held);
        if (song == null) {
            return;
        }
        Font font = mc.font;
        long now = p.level().getGameTime();
        int done = Notes.clientProgress(song, now);
        boolean justPlayed = Notes.CLIENT.lastCompleted == song && now - Notes.CLIENT.completedAt < 50;
        boolean fits = fits(song, held);
        int next = done >= song.length() ? 0 : done;
        Component title = Component.translatable("song.thesift." + song.id());
        Component on = Component.literal("♪ ").append(Component.translatable(song.instrumentKey() + ".short"));
        // the guide line: what to do next, or what the song needs
        Component guide;
        int guideColour;
        if (justPlayed) {
            guide = Component.translatable("music.thesift.guide.done");
            guideColour = 0xFF2E8A3A;
        } else if (held != null && !fits) {
            guide = Component.translatable("music.thesift.guide.needs", Component.translatable(song.instrumentKey()));
            guideColour = 0xFFB0302A;
        } else if (held != null) {
            guide = Component.translatable("music.thesift.guide.play", Notes.name(song.note(next)));
            guideColour = INK;
        } else {
            guide = Component.translatable("music.thesift.guide.next_note", Notes.name(song.note(next)));
            guideColour = INK;
        }
        int w = Math.max(16 + song.length() * NOTE_W, Math.max(font.width(title) + font.width(on) + 26, font.width(guide) + 16));
        int extra = song.prism() || song.rhythmic() ? 8 : 0;
        int h = 78 + extra;
        int l = 6;
        int t = 6;
        // parchment with a darker rim and a folded corner
        g.fill(l, t, l + w, t + h, 0xFF8A6A4A);
        g.fill(l + 1, t + 1, l + w - 1, t + h - 1, 0xF0F6ECD0);
        g.fill(l + w - 6, t + 1, l + w - 1, t + 6, 0xFFDCC89A);
        g.text(font, title, l + 8, t + 5, INK, false);
        g.text(font, on, l + w - 10 - font.width(on), t + 5, fits || held == null ? 0xFF6E4C2A : 0xFFB0302A, false);
        // the staff
        int staffTop = t + 24;
        for (int i = 0; i < 5; i++) {
            g.fill(l + 6, staffTop + i * 6, l + w - 6, staffTop + i * 6 + 1, 0x90A08060);
        }
        float pulse = 0.5F + 0.5F * Mth.sin(now * 0.3F);
        for (int i = 0; i < song.length(); i++) {
            int n = song.note(i);
            int cx = l + 12 + i * NOTE_W + NOTE_W / 2 - 4;
            int cy = staffTop + 26 - n;
            boolean played = i < done || justPlayed;
            int col = Notes.colour(n);
            int alpha = played ? 0xFF000000 : 0xB0000000;
            if (i == next && !justPlayed && fits) {
                // the next note, boxed and breathing
                int a = (int) (0x70 + 0x8F * pulse) << 24;
                g.fill(cx - 3, cy - 11, cx + 8, cy - 10, a | col);
                g.fill(cx - 3, cy + 4, cx + 8, cy + 5, a | col);
                g.fill(cx - 3, cy - 11, cx - 2, cy + 5, a | col);
                g.fill(cx + 7, cy - 11, cx + 8, cy + 5, a | col);
            }
            g.fill(cx + 4, cy - 9, cx + 5, cy, alpha | 0x4A3020);
            g.fill(cx, cy - 1, cx + 5, cy + 2, alpha | col);
            g.fill(cx + 1, cy - 2, cx + 4, cy + 3, alpha | col);
            if (played) {
                g.fill(cx + 1, cy - 1, cx + 2, cy, 0xFFFFFFFF);
            }
            String name = Notes.name(n);
            g.text(font, name, cx + 2 - font.width(name) / 2, staffTop + 32, played ? 0xFF000000 | col : (i == next ? INK : FADED), false);
            int below = staffTop + 42;
            if (song.rhythmic()) {
                // how long the note lasts, in half-beats
                int len = Math.min(NOTE_W - 2, song.beat(i) * 4);
                g.fill(cx, below, cx + len, below + 2, played ? 0xFF4A3020 : 0x904A3020);
            }
            if (song.prism()) {
                // the light pattern
                int rgb = PrismLight.rgb(song.colour(i));
                g.fill(cx, below + 3, cx + 5, below + 7, (played ? 0xFF000000 : 0xA0000000) | rgb);
            }
        }
        g.text(font, guide, l + 8, t + 67 + extra, guideColour, false);
        if (done >= song.length() || justPlayed) {
            g.fill(l + 1, t + h - 2, l + w - 1, t + h - 1, 0xFF000000 | Notes.colour(song.note(0)));
        }
    }
}
