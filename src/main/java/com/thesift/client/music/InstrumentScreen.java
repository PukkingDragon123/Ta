package com.thesift.client.music;

import com.mojang.blaze3d.platform.InputConstants;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.item.MusicSheetItem;
import com.thesift.music.Instrument;
import com.thesift.music.InstrumentPlay;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * M1 instrument play: the play screen an instrument opens. It does not pause the game or hide the
 * world; the instrument sits at the bottom of the screen and the song guide at the top. Every note
 * sounds at once for the player, feeds the local guide and goes to the server
 * ({@link InstrumentPlay.PlayNote}).
 *
 * <p>Each family plays in its own way: {@link StringsScreen} (pick strings on a fretboard),
 * {@link HarpScreen} (the Prism Harp: a string for every note), {@link DrumScreen} (pads and a
 * scrolling beat lane, judged on timing), {@link ChimesScreen} (swinging chimes struck as they pass
 * the mark) and {@link FluteScreen} (hold your breath, finger the holes). On a Prism instrument the
 * keys 1-4 or the mouse wheel choose the colour of light every note is played in.
 *
 * <p>The guide follows the Music Sheet in view (the pinned one, else the carried sheet this
 * instrument plays that is furthest along): each family writes the next notes its own way
 * ({@link #glyph}) and shows them on the instrument itself; Prism songs add their light pattern.
 */
public abstract class InstrumentScreen extends Screen {
    protected static final int INK = 0xFFF4ECD8;
    protected static final int SOFT = 0xFFB8AC98;
    protected static final int DIM = 0xFF7C7264;
    protected static final int GOOD = 0xFF7CFF8A;
    protected static final int WARN = 0xFFFF8A5A;
    protected static final int GOLD = 0xFFFFD27A;

    protected final Instrument instrument;
    protected final InteractionHand hand;
    protected final Item item;
    protected final ItemStack icon;
    /** Keys held down (physical key codes), so a key repeat is not a new note. */
    protected final IntSet held = new IntOpenHashSet();
    /** The colour of light (Prism instruments). */
    protected int light;
    protected @Nullable Song song;
    protected boolean songFits;
    protected int age;
    private float flash;
    private int flashRgb = 0xFFFFFF;
    private @Nullable Component judgement;
    private int judgementColour;
    private double judgedAt = -100.0;
    private boolean completedNow;

    protected InstrumentScreen(Instrument instrument, InteractionHand hand, ItemStack stack) {
        super(stack.getHoverName());
        this.instrument = instrument;
        this.hand = hand;
        this.item = stack.getItem();
        this.icon = stack.copy();
    }

    /** Client hook ({@link InstrumentPlay#clientOpen}): opens the play screen for the instrument in this hand. */
    public static void open(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        if (p == null) {
            return;
        }
        ItemStack stack = p.getItemInHand(hand);
        if (!(stack.getItem() instanceof SiftInstrumentItem instrumentItem)) {
            return;
        }
        Instrument ins = instrumentItem.instrument();
        InstrumentScreen screen = switch (ins.family()) {
            case STRINGS -> ins == Instrument.HARP ? new HarpScreen(ins, hand, stack) : new StringsScreen(ins, hand, stack);
            case DRUM -> new DrumScreen(ins, hand, stack);
            case CHIMES -> new ChimesScreen(ins, hand, stack);
            case FLUTE -> new FluteScreen(ins, hand, stack);
        };
        mc.gui.setScreen(screen);
    }

    /** The player's own music clock, in ticks with fractions (rhythm is judged on it, never on the network's delay). */
    public static double clock() {
        return System.nanoTime() / 5.0E7;
    }

    // ------------------------------------------------------------------ what each family adds

    /** Draws the instrument (the bottom of the screen). */
    protected abstract void drawInstrument(GuiGraphicsExtractor g, int mouseX, int mouseY, double now);

    /** A key went down (not a repeat). */
    protected abstract void pressed(int key, boolean shift);

    /** A key came up. */
    protected void released(int key) {
    }

    protected abstract void mouseDown(double x, double y, boolean right);

    protected void mouseUp(double x, double y, boolean right) {
    }

    protected void mouseDrag(double x, double y) {
    }

    /** Every tick while open. */
    protected void playTick(LocalPlayer player) {
    }

    /** How to play {@code pitch} on this instrument, in a few characters (a key, a fret, a fingering). */
    protected abstract String glyph(int pitch);

    /** Translation key of the line of controls shown under the instrument. */
    protected String controlsKey() {
        return "instrument.thesift.controls." + this.instrument.family().id() + (this.instrument.hasMechanic() ? ".more" : "");
    }

    // ------------------------------------------------------------------ the screen

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean isInGameUi() {
        return true;
    }

    @Override
    public void extractTransparentBackground(GuiGraphicsExtractor g) {
        // the world stays in view: only a soft shade where the instrument sits
        g.fillGradient(0, this.height * 2 / 5, this.width, this.height, 0x00000000, 0xA0050710);
        g.fillGradient(0, 0, this.width, 46, 0x70050710, 0x00000000);
    }

    @Override
    public void tick() {
        super.tick();
        LocalPlayer p = this.minecraft.player;
        if (p == null || !p.isAlive() || p.getItemInHand(this.hand).getItem() != this.item) {
            this.onClose();
            return;
        }
        this.age++;
        this.flash *= 0.8F;
        // a key let go while the window was not listening is not held any more
        for (int key : this.held.toIntArray()) {
            if (!InputConstants.isKeyDown(key)) {
                this.held.remove(key);
                this.released(key);
            }
        }
        this.song = this.pickSong(p);
        this.songFits = this.song != null && this.song.accepts(this.instrument);
        this.playTick(p);
    }

    /** The sheet to follow: the pinned one, else the carried sheet this instrument plays that is furthest along, else one in hand. */
    private @Nullable Song pickSong(LocalPlayer p) {
        Song pinned = Notes.clientPinned;
        if (pinned != null && SongTracker.carriesSheet(p, pinned)) {
            return pinned;
        }
        long now = p.level().getGameTime();
        Song best = null;
        int bestScore = -1;
        for (Song s : Song.values()) {
            if (s.accepts(this.instrument) && SongTracker.carriesSheet(p, s)) {
                int score = Notes.clientProgress(s, now) * 4 + (s.prism() ? 2 : 0) + (s.instrument() != null ? 1 : 0);
                if (score > bestScore) {
                    bestScore = score;
                    best = s;
                }
            }
        }
        if (best != null) {
            return best;
        }
        for (ItemStack s : new ItemStack[]{p.getMainHandItem(), p.getOffhandItem()}) {
            if (s.getItem() instanceof MusicSheetItem sheet) {
                return sheet.song();
            }
        }
        return null;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        double now = clock();
        this.drawInstrument(g, mouseX, mouseY, now);
        this.drawHeader(g);
        this.drawGuide(g, now);
        if (this.instrument.prism()) {
            this.drawLenses(g, now);
        }
        if (this.flash > 0.03F) {
            int al = (int) (this.flash * 70.0F);
            g.fillGradient(0, this.height - 60, this.width, this.height, 0, al << 24 | this.flashRgb & 0xFFFFFF);
        }
        if (this.judgement != null && now - this.judgedAt < 16.0) {
            float t = (float) ((now - this.judgedAt) / 16.0);
            int al = Mth.clamp((int) ((1.0F - t * t) * 255.0F), 8, 255);
            g.centeredText(this.font, this.judgement.getString(), this.width / 2, this.judgementY() - (int) (t * 10.0F), al << 24 | this.judgementColour & 0xFFFFFF);
        }
    }

    /** Where the timing verdicts float up from. */
    protected int judgementY() {
        return this.height / 2 - 10;
    }

    /** Shows a verdict ("Perfect!", "Off the beat") for a moment. */
    protected void judge(Component text, int colour) {
        this.judgement = text;
        this.judgementColour = colour;
        this.judgedAt = clock();
    }

    /** The instrument's name in the top-left corner, how to play it along the bottom, how to stop top right. */
    private void drawHeader(GuiGraphicsExtractor g) {
        g.item(this.icon, 6, 4);
        String name = this.title.getString();
        g.text(this.font, name, 26, 8, GOLD, true);
        String stars = "✦".repeat(this.instrument.tier());
        g.text(this.font, stars, 26 + this.font.width(name) + 4, 8, this.instrument.prism() ? 0xFFFF9AF0 : 0xFF9FE8F0, true);
        Component controls = Component.translatable(this.controlsKey());
        if (this.instrument.prism()) {
            controls = Component.translatable("instrument.thesift.controls.with_light", controls);
        }
        List<FormattedCharSequence> lines = this.font.split(controls, this.width - 16);
        int n = Math.min(2, lines.size());
        for (int i = 0; i < n; i++) {
            g.text(this.font, lines.get(i), 8, this.height - 12 - (n - 1 - i) * 10, SOFT, true);
        }
        String esc = Component.translatable("instrument.thesift.controls.close").getString();
        g.text(this.font, esc, this.width - 8 - this.font.width(esc), 8, DIM, true);
    }

    /** The song guide: title, the notes written this instrument's way, the light pattern of a Prism song. */
    private void drawGuide(GuiGraphicsExtractor g, double now) {
        LocalPlayer p = this.minecraft.player;
        Song s = this.song;
        if (p == null) {
            return;
        }
        int cx = this.width / 2;
        if (s == null) {
            g.centeredText(this.font, Component.translatable("instrument.thesift.guide.no_sheet").getString(), cx, 8, DIM);
            return;
        }
        long time = p.level().getGameTime();
        Component title = Component.translatable("song.thesift." + s.id());
        if (!this.songFits) {
            g.centeredText(this.font, title.getString(), cx, 6, INK);
            g.centeredText(this.font, Component.translatable("music.thesift.guide.needs", Component.translatable(s.instrumentKey())).getString(), cx, 18,
                    WARN);
            return;
        }
        int done = Notes.clientProgress(s, time);
        boolean justPlayed = Notes.CLIENT.lastCompleted == s && time - Notes.CLIENT.completedAt < 50;
        int next = Notes.CLIENT.nextIndex(s, time);
        int cell = 30;
        int totalBeats = 0;
        for (int i = 0; i < s.length(); i++) {
            totalBeats += s.rhythmic() ? s.beat(i) : 1;
        }
        int unit = s.rhythmic() ? Math.max(10, Math.min(22, 220 / Math.max(1, totalBeats))) : cell;
        int w = s.rhythmic() ? totalBeats * unit : s.length() * cell;
        int x0 = cx - w / 2;
        int top = 18;
        int h = s.prism() ? 36 : 28;
        g.fill(x0 - 8, top - 14, x0 + w + 8, top + h + 2, 0x90080A14);
        g.outline(x0 - 8, top - 14, w + 16, h + 16, 0x60FFE7B0);
        String head = title.getString() + (justPlayed ? "  ✔" : "  " + done + "/" + s.length());
        g.centeredText(this.font, head, cx, top - 11, justPlayed ? GOOD : GOLD);
        float pulse = 0.5F + 0.5F * Mth.sin((float) now * 0.45F);
        int x = x0;
        for (int i = 0; i < s.length(); i++) {
            int cw = s.rhythmic() ? s.beat(i) * unit : cell;
            int n = s.note(i);
            boolean played = i < done || justPlayed;
            boolean isNext = i == next && !justPlayed;
            int col = 0xFF000000 | Notes.colour(n);
            int bx = x + 2;
            int bw = s.rhythmic() ? Math.max(12, cw - 4) : cell - 4;
            if (s.rhythmic()) {
                // a bar as long as the note lasts
                g.fill(x + 1, top + 20, x + cw - 1, top + 23, played ? col : 0x60FFFFFF);
            }
            if (isNext) {
                int al = (int) (0x60 + 0x9F * pulse);
                g.outline(bx - 1, top - 1, bw + 2, 20, al << 24 | 0xFFE7B0);
            }
            g.fill(bx, top, bx + bw, top + 18, played ? 0xC0203828 : isNext ? 0xC0303040 : 0x90181820);
            String gl = this.glyph(n);
            g.centeredText(this.font, gl, bx + bw / 2, top + 1, played ? GOOD : isNext ? INK : SOFT);
            g.centeredText(this.font, Notes.name(n), bx + bw / 2, top + 10, played ? col : isNext ? col : 0xA0000000 | Notes.colour(n) & 0xFFFFFF);
            if (s.prism()) {
                // the light pattern: a lamp in the note's colour
                int rgb = PrismLight.rgb(s.colour(i));
                int ly = top + 27;
                int lx = bx + bw / 2;
                int al = played ? 0xFF : isNext ? (int) (0x80 + 0x7F * pulse) : 0x70;
                if (played || isNext) {
                    g.fill(lx - 6, ly - 2, lx + 6, ly + 6, (al / 3) << 24 | rgb);
                }
                g.fill(lx - 3, ly, lx + 3, ly + 4, al << 24 | rgb);
                g.fill(lx - 2, ly + 1, lx, ly + 2, (al * 2 / 3) << 24 | 0xFFFFFF);
            }
            x += s.rhythmic() ? cw : cell;
        }
    }

    /** The Prism lenses: the four colours of light, the chosen one bright (keys 1-4, mouse wheel). */
    private void drawLenses(GuiGraphicsExtractor g, double now) {
        int x = this.width - 22;
        int y0 = this.height / 2 - 52;
        g.text(this.font, Component.translatable("instrument.thesift.light").getString(), x - 18, y0 - 12, SOFT, true);
        for (int c = 0; c < PrismLight.COUNT; c++) {
            int y = y0 + c * 24;
            int rgb = PrismLight.rgb(c);
            boolean on = c == this.light;
            if (on) {
                float pulse = 0.6F + 0.4F * Mth.sin((float) now * 0.3F);
                disc(g, x + 6, y + 8, 12, (int) (0x50 * pulse) << 24 | rgb);
                disc(g, x + 6, y + 8, 9, 0x70000000 | rgb);
            }
            disc(g, x + 6, y + 8, 6, (on ? 0xFF000000 : 0x80000000) | rgb);
            g.fill(x + 3, y + 5, x + 6, y + 7, on ? 0xE0FFFFFF : 0x60FFFFFF);
            g.text(this.font, String.valueOf(c + 1), x - 8, y + 4, on ? INK : DIM, true);
        }
        Song s = this.song;
        if (s != null && s.prism() && this.songFits && this.minecraft.player != null) {
            int want = Notes.CLIENT.nextColour(s, this.minecraft.player.level().getGameTime());
            if (want >= 0 && want != this.light) {
                int y = y0 + want * 24;
                g.text(this.font, "◀", x + 16, y + 4, 0xFF000000 | PrismLight.rgb(want), true);
            }
        }
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.isEscape()) {
            this.onClose();
            return true;
        }
        int key = event.key();
        if (this.instrument.prism() && key >= InputConstants.KEY_1 && key <= InputConstants.KEY_4) {
            this.light = key - InputConstants.KEY_1;
            return true;
        }
        if (!this.held.add(key)) {
            return true;
        }
        this.pressed(key, event.hasShiftDown());
        return true;
    }

    @Override
    public boolean keyReleased(KeyEvent event) {
        this.held.remove(event.key());
        this.released(event.key());
        return true;
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        this.mouseDown(event.x(), event.y(), event.button() == InputConstants.MOUSE_BUTTON_RIGHT);
        return true;
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        this.mouseUp(event.x(), event.y(), event.button() == InputConstants.MOUSE_BUTTON_RIGHT);
        return true;
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        this.mouseDrag(event.x(), event.y());
        return true;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        if (this.instrument.prism() && scrollY != 0.0) {
            this.light = Math.floorMod(this.light + (scrollY < 0.0 ? 1 : -1), PrismLight.COUNT);
        }
        return true;
    }

    protected boolean shiftHeld() {
        return this.held.contains(InputConstants.KEY_LSHIFT) || this.held.contains(InputConstants.KEY_RSHIFT);
    }

    // ------------------------------------------------------------------ playing

    /**
     * Plays {@code pitch} (with {@link Notes} flags): it sounds at once, the guide hears it, and it
     * goes to the server.
     *
     * @return false if this instrument cannot sound that note
     */
    protected boolean play(int pitch, int flags) {
        LocalPlayer p = this.minecraft.player;
        if (p == null || !this.instrument.canPlay(pitch)) {
            return false;
        }
        int colour = this.instrument.prism() ? this.light : -1;
        double clock = clock();
        int f = flags | Notes.HEARD;
        if (!this.instrument.chords()) {
            f &= ~Notes.CHORD;
        }
        this.completedNow = false;
        long time = p.level().getGameTime();
        Song before = Notes.CLIENT.lastCompleted;
        long beforeAt = Notes.CLIENT.completedAt;
        Notes.play(p.level(), p, this.instrument, pitch, colour, clock, f);
        ClientPacketDistributor.sendToServer(InstrumentPlay.PlayNote.of(this.hand, pitch, colour, clock, f));
        this.flash = 1.0F;
        this.flashRgb = colour >= 0 ? PrismLight.rgb(colour) : this.instrument.colour();
        Song done = Notes.CLIENT.lastCompleted;
        if (done != null && Notes.CLIENT.completedAt == time && (done != before || beforeAt != time)) {
            // the song is complete: say so over the instrument
            this.judge(Component.literal("♪ ").append(Component.translatable("song.thesift." + done.id())).append(" ♪"), GOLD);
            this.flash = 2.0F;
            this.completedNow = true;
        }
        return true;
    }

    /** True if the last note played completed a song (its verdict is the song's name, not a timing). */
    protected boolean justCompleted() {
        return this.completedNow;
    }

    /** The note the guide wants next on this instrument (the playable note nearest the written one), or -1. */
    protected int wanted() {
        Song s = this.song;
        LocalPlayer p = this.minecraft.player;
        if (s == null || !this.songFits || p == null) {
            return -1;
        }
        return this.instrument.nearest(Notes.CLIENT.nextNote(s, p.level().getGameTime()));
    }

    /** The colour of light the guide wants next (-1 for none). */
    protected int wantedLight() {
        Song s = this.song;
        LocalPlayer p = this.minecraft.player;
        if (s == null || !this.songFits || p == null || !s.prism()) {
            return -1;
        }
        return Notes.CLIENT.nextColour(s, p.level().getGameTime());
    }

    /** The highlight colour for the next note: its light for a Prism song, else gold. */
    protected int guideRgb() {
        int c = this.wantedLight();
        return c >= 0 ? PrismLight.rgb(c) : 0xFFE7A0;
    }

    /** The colour a struck part of the instrument glows: the chosen light on a Prism instrument, else the instrument's own. */
    protected int playRgb() {
        return this.instrument.prism() ? PrismLight.rgb(this.light) : this.instrument.colour();
    }

    /** A key's label ("A", ";") as the player's keyboard shows it. */
    protected static String keyName(int key) {
        String s = InputConstants.Type.KEYBOARD.getOrCreate(key).getDisplayName().getString();
        if (s.length() <= 2) {
            return s.toUpperCase(java.util.Locale.ROOT);
        }
        if (key >= InputConstants.KEY_A && key <= InputConstants.KEY_Z) {
            return String.valueOf((char) ('A' + key - InputConstants.KEY_A));
        }
        return switch (key) {
            case InputConstants.KEY_SEMICOLON -> ";";
            case InputConstants.KEY_APOSTROPHE -> "'";
            case InputConstants.KEY_SPACE -> s;
            default -> s.substring(0, 2);
        };
    }

    // ------------------------------------------------------------------ drawing helpers

    /** A filled disc. */
    protected static void disc(GuiGraphicsExtractor g, int cx, int cy, int r, int argb) {
        for (int dy = -r; dy <= r; dy++) {
            int hw = (int) Math.round(Math.sqrt(Math.max(0.0, r * r - dy * dy + r * 0.8)));
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, argb);
        }
    }

    /** A filled ellipse. */
    protected static void ellipse(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int argb) {
        for (int dy = -ry; dy <= ry; dy++) {
            double k = dy / (double) Math.max(1, ry);
            int hw = (int) Math.round(rx * Math.sqrt(Math.max(0.0, 1.0 - k * k)));
            g.fill(cx - hw, cy + dy, cx + hw + 1, cy + dy + 1, argb);
        }
    }

    /** The outline of an ellipse, one pixel wide. */
    protected static void ring(GuiGraphicsExtractor g, int cx, int cy, int rx, int ry, int argb) {
        int n = Math.max(12, (int) ((rx + ry) * 2.2));
        int lx = Integer.MIN_VALUE;
        int ly = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++) {
            double a = i * Math.PI * 2.0 / n;
            int x = cx + (int) Math.round(Math.cos(a) * rx);
            int y = cy + (int) Math.round(Math.sin(a) * ry);
            if (x != lx || y != ly) {
                g.fill(x, y, x + 1, y + 1, argb);
                lx = x;
                ly = y;
            }
        }
    }

    /** A straight line {@code w} pixels thick from (x0, y0) to (x1, y1). */
    protected static void line(GuiGraphicsExtractor g, float x0, float y0, float x1, float y1, int w, int argb) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        int len = Math.round((float) Math.sqrt(dx * dx + dy * dy));
        if (len <= 0) {
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x0, y0);
        g.pose().rotate((float) Math.atan2(dy, dx));
        g.fill(0, -w / 2, len, w - w / 2, argb);
        g.pose().popMatrix();
    }

    /** A soft glow: discs from wide and faint to small and bright. */
    protected static void glow(GuiGraphicsExtractor g, int cx, int cy, int r, int rgb, float strength) {
        for (int k = 3; k >= 1; k--) {
            int al = Mth.clamp((int) (strength * (0x30 + (3 - k) * 0x22)), 0, 255);
            disc(g, cx, cy, r * k / 3, al << 24 | rgb & 0xFFFFFF);
        }
    }

    /** A key cap with its label. */
    protected void keyCap(GuiGraphicsExtractor g, int cx, int y, String label, boolean down, int accent) {
        int w = Math.max(11, this.font.width(label) + 5);
        int x = cx - w / 2;
        g.fill(x, y + (down ? 1 : 0), x + w, y + 11, down ? 0xF0303848 : 0xE0181C26);
        g.fill(x, y + 11, x + w, y + 12, 0xFF080A10);
        g.outline(x, y + (down ? 1 : 0), w, 11 - (down ? 1 : 0), down ? 0xFF000000 | accent : 0x80FFFFFF);
        g.centeredText(this.font, label, cx, y + 2 + (down ? 1 : 0), down ? 0xFF000000 | accent : INK);
    }

    /** {@code argb} with its alpha scaled by {@code k} (0..1). */
    protected static int fade(int argb, float k) {
        int a = (int) ((argb >>> 24) * Mth.clamp(k, 0.0F, 1.0F));
        return a << 24 | argb & 0xFFFFFF;
    }

    /** Mixes two RGB colours. */
    protected static int mix(int a, int b, float t) {
        int r = (int) Mth.lerp(t, a >> 16 & 0xFF, b >> 16 & 0xFF);
        int gg = (int) Mth.lerp(t, a >> 8 & 0xFF, b >> 8 & 0xFF);
        int bb = (int) Mth.lerp(t, a & 0xFF, b & 0xFF);
        return r << 16 | gg << 8 | bb;
    }
}
