package com.thesift.client.bandtable;

import com.thesift.TheSift;
import com.thesift.enchant.SiftEnchant;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import com.thesift.registry.ModBandTable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * F2 Band Table: the table's score. On the left, everything in your bag the table can enchant (instruments, armour,
 * weapons, books); on the right, the Sift enchantments that item can take; below, the chosen one's song written out
 * on a stave (with its beats for a drum song), the instrument it is played on, and what it asks of you - its music
 * sheet, experience, amethyst shards and, for some, tamed creatures playing in your band. "Play" lays the item on the
 * table; then play the song on your instrument.
 */
public class BandTableScreen extends Screen {
    private static final Identifier TEXTURE = TheSift.id("textures/gui/band_table.png");
    private static final int TW = 512;
    private static final int TH = 256;
    private static final int W = 276;
    private static final int H = 232;
    private static final int COLS = 5;
    private static final int ROWS = 4;
    private static final int ROW_H = 12;
    private static final int INK = 0xFF3A2414;
    private static final int INK_SOFT = 0xFF7A5A3A;
    private static final int GOOD = 0xFF2E7D3A;
    private static final int BAD = 0xFFA8302E;
    private static final int GOLD = 0xFFB07A10;

    private final BlockPos pos;
    private final List<Integer> slots = new ArrayList<>();
    private final List<SiftEnchant> enchants = new ArrayList<>();
    private int selectedSlot = -1;
    private @Nullable SiftEnchant selected;
    private int age;

    public BandTableScreen(BlockPos pos) {
        super(Component.translatable("block.thesift.band_table"));
        this.pos = pos;
    }

    /** Client hook ({@code BandTableBlock.clientOpen}). */
    public static void open(BlockPos pos) {
        Minecraft.getInstance().gui.setScreen(new BandTableScreen(pos));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private int left() {
        return (this.width - W) / 2;
    }

    private int top() {
        return (this.height - H) / 2;
    }

    private HolderLookup.@Nullable Provider registries() {
        return this.minecraft == null || this.minecraft.level == null ? null : this.minecraft.level.registryAccess();
    }

    @Override
    protected void init() {
        super.init();
        this.refresh();
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;
        this.refresh();
        LocalPlayer p = this.minecraft.player;
        if (p == null || p.distanceToSqr(this.pos.getX() + 0.5, this.pos.getY() + 0.5, this.pos.getZ() + 0.5) > 64.0) {
            this.onClose();
        }
    }

    /** The items in the bag the table can work on, and the enchantments the chosen one can take. */
    private void refresh() {
        LocalPlayer p = this.minecraft == null ? null : this.minecraft.player;
        HolderLookup.Provider reg = this.registries();
        this.slots.clear();
        this.enchants.clear();
        if (p == null || reg == null) {
            return;
        }
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize() && this.slots.size() < COLS * ROWS; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && anyFor(s, reg)) {
                this.slots.add(i);
            }
        }
        if (!this.slots.contains(this.selectedSlot)) {
            this.selectedSlot = this.slots.isEmpty() ? -1 : this.slots.get(0);
        }
        if (this.selectedSlot >= 0) {
            ItemStack s = inv.getItem(this.selectedSlot);
            for (SiftEnchant e : SiftEnchant.values()) {
                if (e.appliesTo(s, reg)) {
                    this.enchants.add(e);
                }
            }
        }
        if (this.selected == null || !this.enchants.contains(this.selected)) {
            this.selected = this.enchants.isEmpty() ? null : this.enchants.get(0);
        }
    }

    private static boolean anyFor(ItemStack s, HolderLookup.Provider reg) {
        for (SiftEnchant e : SiftEnchant.values()) {
            if (e.appliesTo(s, reg)) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ what the player can afford (the server checks again)

    private boolean hasSheet(SiftEnchant e) {
        return this.minecraft.player != null && SongTracker.carriesSheet(this.minecraft.player, e.song());
    }

    private boolean hasLevels(int max) {
        LocalPlayer p = this.minecraft.player;
        return p != null && (p.hasInfiniteMaterials() || p.experienceLevel >= SiftEnchant.levelsNeeded(max));
    }

    private boolean hasShards(int max) {
        LocalPlayer p = this.minecraft.player;
        if (p == null) {
            return false;
        }
        if (p.hasInfiniteMaterials()) {
            return true;
        }
        int n = 0;
        Inventory inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).is(Items.AMETHYST_SHARD)) {
                n += inv.getItem(i).getCount();
            }
        }
        return n >= SiftEnchant.shardsTaken(max);
    }

    private boolean ready(SiftEnchant e, int max) {
        return this.hasSheet(e) && this.hasLevels(max) && this.hasShards(max) && this.selectedSlot >= 0;
    }

    private int max(SiftEnchant e) {
        HolderLookup.Provider reg = this.registries();
        return reg == null ? 1 : e.maxLevel(reg);
    }

    private Component fullName(SiftEnchant e) {
        HolderLookup.Provider reg = this.registries();
        Optional<Holder.Reference<Enchantment>> h = reg == null ? Optional.empty() : e.holder(reg);
        return h.isPresent() ? Enchantment.getFullname(h.get(), h.get().value().getMaxLevel()) : Component.translatable("enchantment.thesift." + e.id());
    }

    // ------------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int l = this.left();
        int t = this.top();
        for (int i = 0; i < this.slots.size(); i++) {
            int x = l + 12 + (i % COLS) * 18;
            int y = t + 32 + (i / COLS) * 18;
            if (mx >= x && mx < x + 18 && my >= y && my < y + 18) {
                this.selectedSlot = this.slots.get(i);
                this.selected = null;
                this.refresh();
                this.click(1.2F);
                return true;
            }
        }
        for (int i = 0; i < this.enchants.size(); i++) {
            int y = t + 32 + i * ROW_H;
            if (mx >= l + 110 && mx < l + 266 && my >= y && my < y + ROW_H) {
                this.selected = this.enchants.get(i);
                this.click(1.5F);
                return true;
            }
        }
        SiftEnchant e = this.selected;
        if (e != null && mx >= l + 182 && mx < l + 264 && my >= t + 208 && my < t + 224 && this.ready(e, this.max(e))) {
            ClientPacketDistributor.sendToServer(new ModBandTable.Choose(this.pos, this.selectedSlot, e.ordinal()));
            if (this.minecraft.player != null) {
                this.minecraft.player.playSound(SoundEvents.NOTE_BLOCK_BELL.value(), 1.0F, Notes.soundPitch(e.song().note(0)));
            }
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private void click(float pitch) {
        if (this.minecraft.player != null) {
            this.minecraft.player.playSound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.6F, pitch);
        }
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        super.extractRenderState(g, mouseX, mouseY, a);
        int l = this.left();
        int t = this.top();
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l, t, 0, 0, W, H, TW, TH);
        Component title = this.getTitle();
        g.text(this.font, title, l + (W - this.font.width(title)) / 2, t + 8, 0xFFF6E7C1, false);
        g.text(this.font, Component.translatable("gui.thesift.band_table.items"), l + 12, t + 22, INK_SOFT, false);
        g.text(this.font, Component.translatable("gui.thesift.band_table.enchantments"), l + 110, t + 22, INK_SOFT, false);
        LocalPlayer p = this.minecraft.player;
        if (p == null) {
            return;
        }
        Inventory inv = p.getInventory();

        // the items
        ItemStack hoveredItem = ItemStack.EMPTY;
        for (int i = 0; i < COLS * ROWS; i++) {
            int x = l + 12 + (i % COLS) * 18;
            int y = t + 32 + (i / COLS) * 18;
            boolean has = i < this.slots.size();
            boolean sel = has && this.slots.get(i) == this.selectedSlot;
            boolean hover = has && mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, sel ? 298 : hover ? 316 : 280, 0, 18, 18, TW, TH);
            if (has) {
                ItemStack s = inv.getItem(this.slots.get(i));
                g.item(s, x + 1, y + 1);
                g.itemDecorations(this.font, s, x + 1, y + 1);
                if (hover) {
                    hoveredItem = s;
                }
            }
        }
        if (this.slots.isEmpty()) {
            List<FormattedCharSequence> lines = this.font.split(Component.translatable("gui.thesift.band_table.nothing"), 90);
            for (int i = 0; i < lines.size(); i++) {
                g.text(this.font, lines.get(i), l + 12, t + 110 + i * 9, INK_SOFT, false);
            }
        }

        // the enchantments the chosen item can take
        List<Component> rowTip = null;
        for (int i = 0; i < this.enchants.size(); i++) {
            SiftEnchant e = this.enchants.get(i);
            int y = t + 32 + i * ROW_H;
            boolean sel = e == this.selected;
            boolean hover = mouseX >= l + 110 && mouseX < l + 266 && mouseY >= y && mouseY < y + ROW_H;
            boolean ok = this.ready(e, this.max(e));
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 110, y, 280, sel ? 52 : hover ? 36 : ok ? 20 : 68, 156, ROW_H, TW, TH);
            // its emblem: the family it is played on
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 112, y + 1, 440 + familyIcon(e.song()) * 9, 20, 9, 9, TW, TH);
            g.text(this.font, this.fullName(e), l + 124, y + 2, ok ? INK : INK_SOFT, false);
            if (e.bandNeeded() > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 254, y + 1, 450, 0, 10, 10, TW, TH);
            }
            if (hover) {
                rowTip = List.of(this.fullName(e), Component.translatable("enchantment.thesift." + e.id() + ".desc").withStyle(ChatFormatting.GRAY));
            }
        }

        // the chosen enchantment: what it does, its song, what it asks
        SiftEnchant chosen = this.selected;
        if (chosen != null) {
            int top = this.max(chosen);
            Song song = chosen.song();
            List<FormattedCharSequence> desc = this.font.split(Component.translatable("enchantment.thesift." + chosen.id() + ".desc"), W - 24);
            for (int i = 0; i < Math.min(2, desc.size()); i++) {
                g.text(this.font, desc.get(i), l + 12, t + 140 + i * 9, INK, false);
            }
            Component songLine = Component.translatable("gui.thesift.band_table.song", Component.translatable("song.thesift." + song.id()),
                    Component.translatable(song.instrumentKey() + ".short"));
            g.text(this.font, songLine, l + 12, t + 160, GOLD, false);
            this.drawStaff(g, l + 12, t + 172, 160, song);
            int rx = l + 182;
            boolean sheet = this.hasSheet(chosen);
            this.requirement(g, rx, t + 168, 440, sheet, Component.translatable(sheet ? "gui.thesift.band_table.sheet" : "gui.thesift.band_table.no_sheet"));
            this.requirement(g, rx, t + 178, 460, this.hasLevels(top), Component.translatable("gui.thesift.band_table.levels",
                    SiftEnchant.levelsNeeded(top), SiftEnchant.levelsTaken(top)));
            this.requirement(g, rx, t + 188, 470, this.hasShards(top), Component.translatable("gui.thesift.band_table.shards", SiftEnchant.shardsTaken(top)));
            if (chosen.bandNeeded() > 0) {
                g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, rx, t + 197, 450, 0, 10, 10, TW, TH);
                g.text(this.font, Component.translatable("gui.thesift.band_table.band", chosen.bandNeeded()), rx + 12, t + 198, INK_SOFT, false);
            }
            boolean ready = this.ready(chosen, top);
            boolean overPlay = mouseX >= l + 182 && mouseX < l + 264 && mouseY >= t + 208 && mouseY < t + 224;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, l + 182, t + 208, 280, !ready ? 120 : overPlay ? 102 : 84, 82, 16, TW, TH);
            Component play = Component.translatable("gui.thesift.band_table.play");
            g.text(this.font, play, l + 182 + (82 - this.font.width(play)) / 2, t + 212, ready ? 0xFFFFF4D8 : 0xFF9A8A78, false);
        }

        if (!hoveredItem.isEmpty()) {
            g.setTooltipForNextFrame(this.font, hoveredItem, mouseX, mouseY);
        } else if (rowTip != null) {
            g.setComponentTooltipForNextFrame(this.font, rowTip, mouseX, mouseY);
        }
    }

    private static int familyIcon(Song song) {
        if (song.instrument() == null) {
            return 4;
        }
        return switch (song.instrument()) {
            case FLUTE -> 0;
            case DRUM -> 1;
            case STRINGS -> 2;
            case CHIMES -> 3;
        };
    }

    private void requirement(GuiGraphicsExtractor g, int x, int y, int u, boolean ok, Component text) {
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y - 1, u, 0, 10, 10, TW, TH);
        g.text(this.font, text, x + 12, y, ok ? GOOD : BAD, false);
    }

    /** The song on a five-line stave: each note a head in its colour, as high as it sounds; a drum song's beats as flags. */
    private void drawStaff(GuiGraphicsExtractor g, int x, int y, int w, Song song) {
        for (int i = 0; i < 5; i++) {
            g.fill(x, y + 8 + i * 5, x + w, y + 9 + i * 5, 0x80694A2C);
        }
        g.fill(x, y + 8, x + 1, y + 29, 0xFF694A2C);
        g.fill(x + w - 1, y + 8, x + w, y + 29, 0xFF694A2C);
        int n = song.length();
        int step = (w - 20) / Math.max(1, n);
        long now = this.minecraft.level == null ? 0L : this.minecraft.level.getGameTime();
        int lit = Notes.clientProgress(song, now);
        for (int i = 0; i < n; i++) {
            int pitch = song.note(i);
            int nx = x + 12 + i * step;
            int ny = y + 30 - Math.round(pitch * 26 / 24.0F) + 1;
            int col = 0xFF000000 | Notes.colour(pitch);
            boolean bounce = (this.age / 4) % n == i;
            if (bounce) {
                ny -= 1;
            }
            g.fill(nx - 2, ny - 1, nx + 2, ny + 2, i < lit ? 0xFFFFFFFF : col);
            g.fill(nx - 1, ny - 2, nx + 1, ny + 3, i < lit ? 0xFFFFFFFF : col);
            g.fill(nx + 1, ny - 8, nx + 2, ny, 0xFF3A2414);
            if (song.rhythmic() && song.beat(i) == 1) {
                g.fill(nx + 2, ny - 8, nx + 4, ny - 7, 0xFF3A2414);
                g.fill(nx + 3, ny - 7, nx + 4, ny - 5, 0xFF3A2414);
            }
            Component name = Component.literal(Notes.name(pitch));
            if (i % 2 == 0) {
                g.text(this.font, name, nx - this.font.width(name) / 2, y + 32, 0xFF8A6A4A, false);
            }
        }
        // a little bobbing note marks where the tune is
        float bob = Mth.sin(this.age * 0.3F) * 1.5F;
        g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x + w - 10, y + Math.round(bob), 480, 0, 10, 10, TW, TH);
    }
}
