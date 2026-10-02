package com.thesift.client.music;

import com.thesift.TheSift;
import com.thesift.item.MusicSheetItem;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import org.jspecify.annotations.Nullable;

/**
 * The music overlays.
 *
 * <ul>
 *   <li>The note ladder: while you hold an instrument ({@code #thesift:instruments}), a slim ladder
 *   of the 25 notes beside the crosshair lights the note your gaze would play.</li>
 *   <li>The music sheet: while you hold a Music Sheet (or after pinning one by using it), its notes
 *   sit on a parchment staff in the top-left corner, lighting up as you play them in order.</li>
 * </ul>
 */
public final class InstrumentHud {
    private static final int STEP = 3;
    private static final int NOTE_W = 18;
    private static final int INK = 0xFF4A3020;

    private InstrumentHud() {
    }

    public static void registerOverlays(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, TheSift.id("music_sheet"), InstrumentHud::drawSheet);
        event.registerAbove(TheSift.id("music_sheet"), TheSift.id("note_ladder"), InstrumentHud::drawLadder);
    }

    private static boolean holdsInstrument(LocalPlayer p) {
        return p.getMainHandItem().is(Notes.INSTRUMENTS) || p.getOffhandItem().is(Notes.INSTRUMENTS);
    }

    /** The sheet to show: one in hand, else the pinned one while it is still carried. */
    private static @Nullable Song shownSheet(LocalPlayer p) {
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof MusicSheetItem sheet) {
                return sheet.song();
            }
        }
        Song pinned = Notes.clientPinned;
        if (pinned != null && !SongTracker.carriesSheet(p, pinned)) {
            Notes.clientPinned = null;
            return null;
        }
        return pinned;
    }

    private static void drawLadder(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui || !holdsInstrument(p)) {
            return;
        }
        int pitch = Notes.lookPitch(p);
        int x = g.guiWidth() / 2 + 18;
        int top = g.guiHeight() / 2 - Notes.MAX_PITCH * STEP / 2;
        g.fill(x, top - 2, x + 1, top + Notes.MAX_PITCH * STEP + 3, 0x50FFFFFF);
        // the next note of the sheet in view, as a hint
        Song song = shownSheet(p);
        int hint = -1;
        if (song != null) {
            int done = Notes.clientProgress(song, p.level().getGameTime());
            hint = song.note(done >= song.length() ? 0 : done);
        }
        for (int n = 0; n <= Notes.MAX_PITCH; n++) {
            int y = top + (Notes.MAX_PITCH - n) * STEP;
            int len = n % 12 == 0 ? 5 : (n % 2 == 0 ? 3 : 2);
            g.fill(x + 1, y, x + 1 + len, y + 1, n % 12 == 0 ? 0x90FFFFFF : 0x50FFFFFF);
            if (n == hint && n != pitch) {
                g.fill(x - 4, y, x - 1, y + 1, 0xC0000000 | Notes.colour(n));
            }
        }
        int y = top + (Notes.MAX_PITCH - pitch) * STEP;
        int c = Notes.colour(pitch);
        g.fill(x - 1, y - 1, x + 8, y + 2, 0xFF000000 | c);
        g.fill(x, y, x + 7, y + 1, 0xFFFFFFFF);
        g.text(mc.font, Notes.name(pitch), x + 11, y - 4, 0xFF000000 | c, true);
    }

    private static void drawSheet(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null || mc.options.hideGui) {
            return;
        }
        Song song = shownSheet(p);
        if (song == null) {
            return;
        }
        Font font = mc.font;
        int done = Notes.clientProgress(song, p.level().getGameTime());
        Component title = Component.translatable("song.thesift." + song.id());
        int w = Math.max(16 + song.length() * NOTE_W, font.width(title) + 16);
        int h = 62;
        int l = 6;
        int t = 6;
        // parchment with a darker rim and a folded corner
        g.fill(l, t, l + w, t + h, 0xFF8A6A4A);
        g.fill(l + 1, t + 1, l + w - 1, t + h - 1, 0xF0F6ECD0);
        g.fill(l + w - 6, t + 1, l + w - 1, t + 6, 0xFFDCC89A);
        g.text(font, title, l + 8, t + 5, INK, false);
        // the staff
        int staffTop = t + 18;
        for (int i = 0; i < 5; i++) {
            g.fill(l + 6, staffTop + i * 6, l + w - 6, staffTop + i * 6 + 1, 0x90A08060);
        }
        for (int i = 0; i < song.length(); i++) {
            int n = song.note(i);
            int cx = l + 12 + i * NOTE_W + NOTE_W / 2 - 4;
            int cy = staffTop + 26 - n;
            boolean played = i < done;
            int col = Notes.colour(n);
            int alpha = played ? 0xFF000000 : 0xB0000000;
            g.fill(cx + 4, cy - 9, cx + 5, cy, alpha | 0x4A3020);
            g.fill(cx, cy - 1, cx + 5, cy + 2, alpha | col);
            g.fill(cx + 1, cy - 2, cx + 4, cy + 3, alpha | col);
            if (played) {
                g.fill(cx + 1, cy - 1, cx + 2, cy, 0xFFFFFFFF);
            }
            String name = Notes.name(n);
            g.text(font, name, cx + 2 - font.width(name) / 2, t + h - 11, played ? 0xFF000000 | col : 0xFF8A6A4A, false);
        }
        if (done >= song.length()) {
            g.fill(l + 1, t + h - 2, l + w - 1, t + h - 1, 0xFF000000 | Notes.colour(song.note(0)));
        }
    }
}
