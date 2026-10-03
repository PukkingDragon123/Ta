package com.thesift.client.music;

import com.thesift.TheSift;
import com.thesift.item.CongaDrumItem;
import com.thesift.item.CraneFluteItem;
import com.thesift.item.GuitarItem;
import com.thesift.item.MusicSheetItem;
import com.thesift.item.PrismHarpItem;
import com.thesift.item.WindChimesItem;
import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongMatcher;
import com.thesift.music.SongTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;

/**
 * The music overlays.
 *
 * <ul>
 *   <li>The note ladder: while you hold an instrument ({@code #thesift:instruments}), a slim ladder
 *   of the 25 notes beside the crosshair lights the note your gaze would play, and marks (with
 *   its name) the note the song in view wants next.</li>
 *   <li>The song guide: a Music Sheet in hand, a pinned sheet, or - while you hold an instrument -
 *   the carried sheet that instrument can play. Its notes sit on a parchment staff in the top-left
 *   corner, lighting up as you play them; the next one is boxed, and the bottom line says where to
 *   look for it, or which instrument the song needs if yours will not do.</li>
 * </ul>
 */
public final class InstrumentHud {
    private static final int STEP = 3;
    private static final int NOTE_W = 18;
    private static final int INK = 0xFF4A3020;
    private static final int FADED = 0xFF8A6A4A;

    private InstrumentHud() {
    }

    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TheSift.id("music_sheet"), InstrumentHud::drawSheet);
        event.registerAbove(TheSift.id("music_sheet"), TheSift.id("note_ladder"), InstrumentHud::drawLadder);
    }

    private static boolean holdsInstrument(LocalPlayer p) {
        return p.getMainHandItem().is(Notes.INSTRUMENTS) || p.getOffhandItem().is(Notes.INSTRUMENTS);
    }

    /** The kind of the instrument in hand (main hand first), or null for none or one the songs do not know. */
    private static Instrument.@Nullable Family heldFamily(LocalPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            Item i = s.getItem();
            if (i instanceof WindChimesItem) {
                return Instrument.Family.CHIMES;
            } else if (i instanceof CraneFluteItem) {
                return Instrument.Family.FLUTE;
            } else if (i instanceof CongaDrumItem) {
                return Instrument.Family.DRUM;
            } else if (i instanceof GuitarItem || i instanceof PrismHarpItem) {
                return Instrument.Family.STRINGS;
            }
        }
        return null;
    }

    private static boolean fits(Song song, Instrument.@Nullable Family held) {
        return song.instrument() == null || song.instrument() == held;
    }

    /**
     * The sheet to show: one in hand, else the pinned one while it is still carried, else - while
     * an instrument is in hand - the carried sheet it can play (the one furthest along, songs
     * written for this very instrument first), or failing that the first carried sheet.
     */
    private static @Nullable Song shownSheet(LocalPlayer p, Instrument.@Nullable Family held, boolean holding) {
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
        if (!holding) {
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
                int score = Notes.clientProgress(s, now) * 2 + (s.instrument() != null ? 1 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
        }
        return best != null ? best : first;
    }

    private static void drawLadder(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || !holdsInstrument(p)) {
            return;
        }
        Font font = mc.font;
        int pitch = Notes.lookPitch(p);
        int x = g.guiWidth() / 2 + 18;
        int top = g.guiHeight() / 2 - Notes.MAX_PITCH * STEP / 2;
        g.fill(x, top - 2, x + 1, top + Notes.MAX_PITCH * STEP + 3, 0x50FFFFFF);
        // the next note of the song in view, marked and named on the left
        Instrument.Family held = heldFamily(p);
        Song song = shownSheet(p, held, true);
        int hint = -1;
        if (song != null && fits(song, held)) {
            hint = Notes.CLIENT.nextNote(song, p.level().getGameTime());
        }
        for (int n = 0; n <= Notes.MAX_PITCH; n++) {
            int y = top + (Notes.MAX_PITCH - n) * STEP;
            int len = n % 12 == 0 ? 5 : (n % 2 == 0 ? 3 : 2);
            g.fill(x + 1, y, x + 1 + len, y + 1, n % 12 == 0 ? 0x90FFFFFF : 0x50FFFFFF);
        }
        boolean onTarget = hint >= 0 && SongMatcher.matches(hint, pitch);
        if (hint >= 0) {
            int hy = top + (Notes.MAX_PITCH - hint) * STEP;
            int hc = 0xFF000000 | Notes.colour(hint);
            // the window of notes that count, then the written note itself
            int lo = top + (Notes.MAX_PITCH - Math.min(Notes.MAX_PITCH, hint + SongMatcher.TOLERANCE)) * STEP;
            int hi = top + (Notes.MAX_PITCH - Math.max(0, hint - SongMatcher.TOLERANCE)) * STEP;
            g.fill(x - 3, lo, x - 1, hi + 1, 0x60000000 | Notes.colour(hint));
            g.fill(x - 8, hy - 1, x - 1, hy + 2, hc);
            String name = Notes.name(hint);
            g.text(font, name, x - 10 - font.width(name), hy - 4, hc, true);
            if (!onTarget) {
                // an arrow from your gaze towards it
                String arrow = hint > pitch ? "▲" : "▼";
                int ay = top + (Notes.MAX_PITCH - pitch) * STEP;
                g.text(font, arrow, x + 11 + font.width(Notes.name(pitch)) + 3, ay - 4, hc, true);
            }
        }
        int y = top + (Notes.MAX_PITCH - pitch) * STEP;
        int c = onTarget ? 0x7CFF8A : Notes.colour(pitch);
        g.fill(x - 1, y - 1, x + 8, y + 2, 0xFF000000 | c);
        g.fill(x, y, x + 7, y + 1, 0xFFFFFFFF);
        g.text(font, Notes.name(pitch), x + 11, y - 4, 0xFF000000 | c, true);
    }

    private static void drawSheet(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        boolean holding = holdsInstrument(p);
        Instrument.Family held = heldFamily(p);
        Song song = shownSheet(p, held, holding);
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
        // the guide line: where to look next, or what the song needs
        Component guide;
        int guideColour;
        if (justPlayed) {
            guide = Component.translatable("music.thesift.guide.done");
            guideColour = 0xFF2E8A3A;
        } else if (holding && !fits) {
            guide = Component.translatable("music.thesift.guide.needs", Component.translatable(song.instrumentKey()));
            guideColour = 0xFFB0302A;
        } else {
            guide = Component.translatable("music.thesift.guide.next", Notes.name(song.note(next)), Notes.aim(song.note(next)));
            guideColour = INK;
        }
        @Nullable Component sneak = holding && fits && (held == Instrument.Family.FLUTE || held == Instrument.Family.DRUM)
                ? Component.translatable("music.thesift.guide.sneak") : null;
        int w = Math.max(16 + song.length() * NOTE_W, Math.max(font.width(title) + font.width(on) + 26, font.width(guide) + 16));
        if (sneak != null) {
            w = Math.max(w, font.width(sneak) + 16);
        }
        int h = sneak != null ? 88 : 78;
        int l = 6;
        int t = 6;
        // parchment with a darker rim and a folded corner
        g.fill(l, t, l + w, t + h, 0xFF8A6A4A);
        g.fill(l + 1, t + 1, l + w - 1, t + h - 1, 0xF0F6ECD0);
        g.fill(l + w - 6, t + 1, l + w - 1, t + 6, 0xFFDCC89A);
        g.text(font, title, l + 8, t + 5, INK, false);
        g.text(font, on, l + w - 10 - font.width(on), t + 5, fits || !holding ? 0xFF6E4C2A : 0xFFB0302A, false);
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
        }
        g.text(font, guide, l + 8, t + 67, guideColour, false);
        if (sneak != null) {
            g.text(font, sneak, l + 8, t + 77, FADED, false);
        }
        if (done >= song.length() || justPlayed) {
            g.fill(l + 1, t + h - 2, l + w - 1, t + h - 1, 0xFF000000 | Notes.colour(song.note(0)));
        }
    }
}
