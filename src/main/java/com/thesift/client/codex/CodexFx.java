package com.thesift.client.codex;

import com.thesift.TheSift;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * The Codex's page effects and specimen plates. Each page has a theme picked from its key: Sculk pages grow veins in from the
 * corners that throb to a heartbeat while souls drift up; music pages float notes that bob to a beat over a soft melody; Chrome
 * pages shimmer in rainbow light and twinkle; sky pages drift clouds behind the subject; boss pages smoulder with embers. Creature
 * pages also show a framed pixel-art plate of the creature (textures/gui/codex_portraits.png, made by tools/ui_art.py).
 *
 * <p>All coordinates are relative to the book's top-left corner. Effects sit behind the text and fade in after a page turn.
 */
final class CodexFx {
    enum Theme { NONE, SCULK, MUSIC, CHROME, SKY, BOSS }

    static final Identifier PORTRAITS = TheSift.id("textures/gui/codex_portraits.png");
    static final Identifier FX = TheSift.id("textures/gui/codex_fx.png");

    private static final Map<String, Theme> THEMES = new java.util.HashMap<>();

    static {
        for (String k : new String[]{"jailer", "sculkling", "sculk_parasite", "strumling", "warden_core", "sculk_corruption", "sculk_swamp", "sculk_ocean",
                "sculk_castle", "sift_drum", "sculk_bloom", "gobbler", "musical_cobweb", "sculk_fish", "coral_organ"}) { // CR3: sculk fish, coral organ
            THEMES.put(k, Theme.SCULK);
        }
        for (String k : new String[]{"instruments", "wind_chimes", "star_lute", "serbim_flute", "thunder_drums", "glass_bells", "prism_chimes"}) {
            THEMES.put(k, Theme.MUSIC); // M1 instrument play
        }
        for (String k : new String[]{"harmoner", "enchoer", "songs", "prism_instruments", "music", "music_crystal", "conga_drum", "crane_flute",
                "guitar", "weaver_guitar", "vocals", "staff", "europhy_table", "echoer_device",
                "kazoo_fish", "fanfare_eel", "tubafish", "nib", "soul_golem"}) {
            THEMES.put(k, Theme.MUSIC);
        }
        for (String k : new String[]{"chrome", "prism", "skysong_gem", "caravan", "caravan_colony", "slumbler", "siftite"}) {
            THEMES.put(k, Theme.CHROME);
        }
        for (String k : new String[]{"sky_whale", "sea_and_sky", "portal", "white_forest", "swifter", "bulb", "stomper"}) {
            THEMES.put(k, Theme.SKY);
        }
        for (String k : new String[]{"dictator", "thumper", "strummer", "stage", "encore_sigil", "ancient_cannon"}) {
            THEMES.put(k, Theme.BOSS);
        }
    }

    /** Plate index in the portrait atlas (same order as PORTRAITS in tools/ui_art.py). */
    private static final List<String> PLATES = List.of("bulb", "bulb_white", "harmoner", "sniffer", "enchoer", "soul_golem", "nib", "slumbler",
            "sifter", "stomper", "sky_whale", "fanfare_eel", "kazoo_fish", "tubafish", "caravan", "gobbler", "swifter", "jailer",
            "sculkling", "dictator", "thumper", "strummer", "strumling", "sculk_parasite");

    static Theme theme(String key) {
        return THEMES.getOrDefault(key, Theme.NONE);
    }

    static boolean hasPlate(String key) {
        return PLATES.contains(key);
    }

    // ------------------------------------------------------------------ specimen plate

    /** The creature's framed plate, 48x48, at (x, y). The Bulb's plate turns over to show its White Forest cousin now and then. */
    static void plate(GuiGraphicsExtractor g, String key, int x, int y, int age, float a, Theme theme) {
        int i = PLATES.indexOf(key);
        if (i < 0) {
            return;
        }
        if (i == 0 && (age / 80) % 2 == 1) {
            i = 1;
        }
        g.fill(x + 2, y + 2, x + 50, y + 50, 0x40000000);
        g.blit(RenderPipelines.GUI_TEXTURED, PORTRAITS, x, y, (i % 10) * 48, (i / 10) * 48, 48, 48, 512, 256);
        if (theme == Theme.CHROME) {
            // a rainbow running round the frame
            float hue = (age + a) * 0.02F;
            for (int k = 0; k < 48; k += 4) {
                int c = 0xB0000000 | hsv(hue + k / 48.0F) & 0xFFFFFF;
                g.fill(x + k, y, x + k + 4, y + 1, c);
                g.fill(x + 47 - k - 3, y + 47, x + 47 - k + 1, y + 48, c);
                g.fill(x, y + 47 - k - 3, x + 1, y + 47 - k + 1, c);
                g.fill(x + 47, y + k, x + 48, y + k + 4, c);
            }
        }
    }

    // ------------------------------------------------------------------ particles

    private static final int NOTE = 0;
    private static final int SOUL = 1;
    private static final int EMBER = 2;
    private static final int SPARKLE = 3;
    private static final int CLOUD = 4;
    private static final int MOTE = 5;
    private static final int[] NOTE_COLOURS = {0xFF2EC9C0, 0xFFE070B0, 0xFFE8B840, 0xFFA070F0, 0xFF60A0F0};
    private static final float[] MELODY = {0, 2, 4, 7, 9, 7, 4, 2, 0, 4, 7, 12, 9, 7, 4, 2};

    private static final class Bit {
        float x, y, ox, oy, vx, vy, seed;
        int age, life, kind, colour, frame;
    }

    private final List<Bit> bits = new ArrayList<>();
    private final Random random = new Random();
    private Theme theme = Theme.NONE;
    private int age;

    void reset(Theme theme) {
        this.bits.clear();
        this.theme = theme;
        this.age = 0;
        if (theme == Theme.SKY) {
            for (int i = 0; i < 3; i++) {
                Bit b = this.spawn(CLOUD, 10 + this.random.nextInt(110), 50 + this.random.nextInt(70), 0.12F + this.random.nextFloat() * 0.1F, 0, 100000, 0xFFFFFFFF);
                b.frame = i % 2;
            }
        }
    }

    private Bit spawn(int kind, float x, float y, float vx, float vy, int life, int colour) {
        Bit b = new Bit();
        b.kind = kind;
        b.x = b.ox = x;
        b.y = b.oy = y;
        b.vx = vx;
        b.vy = vy;
        b.life = life;
        b.colour = colour;
        b.seed = this.random.nextFloat() * 6.28F;
        b.frame = this.random.nextInt(3);
        this.bits.add(b);
        return b;
    }

    /** Book page interior (relative): left page x 16..144, right page x 154..282, y 14..160. */
    private float pageX() {
        return this.random.nextBoolean() ? 18 + this.random.nextFloat() * 124 : 156 + this.random.nextFloat() * 124;
    }

    void tick(@Nullable LocalPlayer player, int bookH) {
        this.age++;
        int t = this.age;
        Random r = this.random;
        switch (this.theme) {
            case SCULK -> {
                if (t % 5 == 0) {
                    this.spawn(SOUL, this.pageX(), bookH - 22, 0, -0.3F - r.nextFloat() * 0.25F, 50 + r.nextInt(30), 0xFFFFFFFF);
                }
                if (player != null && t % 36 == 12) {
                    player.playSound(SoundEvents.WARDEN_HEARTBEAT, 0.35F, 1.0F);
                }
                if (player != null && t % 90 == 50) {
                    player.playSound(SoundEvents.SCULK_CLICKING, 0.2F, 0.8F + r.nextFloat() * 0.4F);
                }
            }
            case MUSIC -> {
                if (t % 7 == 0) {
                    this.spawn(NOTE, 30 + r.nextFloat() * 100, 120 + r.nextFloat() * 20, (r.nextFloat() - 0.5F) * 0.3F, -0.28F - r.nextFloat() * 0.15F,
                            80 + r.nextInt(30), NOTE_COLOURS[r.nextInt(NOTE_COLOURS.length)]);
                }
                if (player != null && t % 20 == 0) {
                    float semis = MELODY[(t / 20) % MELODY.length];
                    player.playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 0.14F, (float) Math.pow(2.0, (semis - 6.0) / 12.0));
                }
            }
            case CHROME -> {
                if (t % 4 == 0) {
                    this.spawn(SPARKLE, 20 + r.nextFloat() * 120, 30 + r.nextFloat() * 110, 0, -0.05F, 24 + r.nextInt(16), hsv(r.nextFloat()));
                }
                if (player != null && t % 55 == 20) {
                    player.playSound(SoundEvents.AMETHYST_BLOCK_CHIME, 0.3F, 0.8F + r.nextFloat() * 0.6F);
                }
            }
            case SKY -> {
                if (t % 9 == 0) {
                    this.spawn(MOTE, 20 + r.nextFloat() * 120, 40 + r.nextFloat() * 100, 0.2F + r.nextFloat() * 0.2F, (r.nextFloat() - 0.5F) * 0.1F, 60, 0xFFFFFFFF);
                }
            }
            case BOSS -> {
                if (t % 3 == 0) {
                    int c = r.nextInt(3) == 0 ? 0xFFFFD070 : 0xFFFF7A30;
                    this.spawn(EMBER, this.pageX(), bookH - 20, (r.nextFloat() - 0.5F) * 0.3F, -0.35F - r.nextFloat() * 0.4F, 40 + r.nextInt(30), c);
                }
                if (player != null && t % 60 == 30) {
                    player.playSound(SoundEvents.NOTE_BLOCK_BASEDRUM.value(), 0.25F, 0.5F);
                }
            }
            default -> {
                if (t % 14 == 0) {
                    this.spawn(MOTE, 20 + r.nextFloat() * 120, 40 + r.nextFloat() * 100, (r.nextFloat() - 0.5F) * 0.1F, -0.06F, 70, 0xFFFFF4D8);
                }
            }
        }
        for (Iterator<Bit> it = this.bits.iterator(); it.hasNext(); ) {
            Bit b = it.next();
            b.ox = b.x;
            b.oy = b.y;
            b.age++;
            b.x += b.vx + (b.kind == SOUL || b.kind == EMBER ? Mth.sin(b.age * 0.15F + b.seed) * 0.25F : 0.0F);
            b.y += b.vy;
            if (b.kind == CLOUD && b.x > 140) {
                b.x = b.ox = -24;
                b.y = b.oy = 46 + this.random.nextInt(70);
            }
            if (b.age >= b.life) {
                it.remove();
            }
        }
    }

    // ------------------------------------------------------------------ drawing

    /** Draws the page's effects (behind the text), fading them in as the page settles after a turn. */
    void render(GuiGraphicsExtractor g, int l, int t, int bw, int bh, float a) {
        float time = this.age + a;
        float fade = Mth.clamp(time / 12.0F, 0.0F, 1.0F);
        if (fade <= 0.0F) {
            return;
        }
        switch (this.theme) {
            case SCULK -> {
                // veins creep in from the lower outer corners and throb with the heartbeat
                float reveal = Mth.clamp(time / 45.0F, 0.0F, 1.0F);
                int size = Math.max(1, (int) (64 * reveal));
                float beat = (this.age % 36 + a);
                float throb = Math.max(pulse(beat - 12.0F), pulse(beat - 18.0F) * 0.7F);
                int va = (int) (200 * fade);
                int ga = (int) ((70 + 185 * throb) * fade);
                int y0 = t + bh - 14 - size;
                g.enableScissor(l + 10, y0, l + 10 + size, t + bh - 14);
                g.blit(RenderPipelines.GUI_TEXTURED, FX, l + 10, t + bh - 78, 128, 0, 64, 64, 64, 64, 256, 128, va << 24 | 0xFFFFFF);
                g.blit(RenderPipelines.GUI_TEXTURED, FX, l + 10, t + bh - 78, 128, 64, 64, 64, 64, 64, 256, 128, ga << 24 | 0xFFFFFF);
                g.disableScissor();
                g.enableScissor(l + bw - 10 - size, y0, l + bw - 10, t + bh - 14);
                g.blit(RenderPipelines.GUI_TEXTURED, FX, l + bw - 74, t + bh - 78, 192, 0, 64, 64, 64, 64, 256, 128, va << 24 | 0xFFFFFF);
                g.blit(RenderPipelines.GUI_TEXTURED, FX, l + bw - 74, t + bh - 78, 192, 64, 64, 64, 64, 64, 256, 128, ga << 24 | 0xFFFFFF);
                g.disableScissor();
                int shade = (int) ((24 + 30 * throb) * fade);
                g.fillGradient(l + 12, t + bh - 46, l + bw - 12, t + bh - 12, 0x000A2A30, shade << 24 | 0x0A2A30);
            }
            case CHROME -> {
                // a soft band of rainbow light sweeping over the left page
                float sweep = (time * 1.4F) % 200.0F - 30.0F;
                for (int i = 0; i < 24; i++) {
                    int sx = l + 14 + (int) sweep + i;
                    if (sx < l + 14 || sx >= l + bw / 2 - 2) {
                        continue;
                    }
                    int bandA = (int) ((1.0F - Math.abs(i - 12) / 12.0F) * 46 * fade);
                    g.fill(sx, t + 12, sx + 1, t + bh - 12, bandA << 24 | hsv(time * 0.01F + i / 24.0F) & 0xFFFFFF);
                }
            }
            case SKY -> {
                int top = (int) (48 * fade);
                g.fillGradient(l + 18, t + 46, l + 144, t + 146, top << 24 | 0x9ACCF0, 0x009ACCF0);
            }
            case BOSS -> {
                float flick = 0.75F + 0.25F * Mth.sin(time * 0.31F) * Mth.sin(time * 0.17F);
                int glow = (int) (60 * flick * fade);
                g.fillGradient(l + 12, t + bh - 50, l + bw - 12, t + bh - 12, 0x00FF5020, glow << 24 | 0xC02810);
            }
            default -> {
            }
        }
        float beatPop = pulse((time % 10.0F));
        for (Bit b : this.bits) {
            float x = l + Mth.lerp(a, b.ox, b.x);
            float y = t + Mth.lerp(a, b.oy, b.y);
            float life = (b.age + a) / b.life;
            float k = Math.min(1.0F, Math.min(life * 5.0F, (1.0F - life) * 3.0F)) * fade;
            if (k <= 0.0F) {
                continue;
            }
            int ix = (int) x;
            int iy = (int) y;
            switch (b.kind) {
                case NOTE -> {
                    // notes bob to the beat
                    int bob = (int) (-2.0F * beatPop);
                    int u = b.frame * 8;
                    g.blit(RenderPipelines.GUI_TEXTURED, FX, ix, iy + bob, u, 0, 7, 10, 7, 10, 256, 128, alpha(b.colour, k));
                }
                case SOUL -> g.blit(RenderPipelines.GUI_TEXTURED, FX, ix, iy, 32 + ((b.age / 4) % 4) * 6, 0, 6, 6, 6, 6, 256, 128, alpha(0xFFFFFFFF, k * 0.85F));
                case EMBER -> {
                    float fl = 0.6F + 0.4F * Mth.sin(b.age * 0.9F + b.seed);
                    g.blit(RenderPipelines.GUI_TEXTURED, FX, ix, iy, 56, 0, 3, 3, 3, 3, 256, 128, alpha(b.colour, k * fl));
                }
                case SPARKLE -> {
                    float tw = Mth.sin(life * 3.14159F);
                    g.blit(RenderPipelines.GUI_TEXTURED, FX, ix - 3, iy - 3, 64, 0, 7, 7, 7, 7, 256, 128, alpha(b.colour, tw * fade));
                }
                case CLOUD -> {
                    // clouds drift behind the subject, clipped to the left page
                    g.enableScissor(l + 16, t + 12, l + bw / 2 - 4, t + bh - 12);
                    g.blit(RenderPipelines.GUI_TEXTURED, FX, ix, iy, b.frame * 32, 16, 32, 14, 32, 14, 256, 128, alpha(0xFFFFFFFF, 0.7F * fade));
                    g.disableScissor();
                }
                default -> g.fill(ix, iy, ix + 1, iy + 1, alpha(b.colour, k * 0.7F));
            }
        }
    }

    private static float pulse(float x) {
        return x < 0.0F || x > 6.0F ? 0.0F : 1.0F - x / 6.0F;
    }

    private static int alpha(int colour, float k) {
        return (int) (Mth.clamp(k, 0.0F, 1.0F) * 255.0F) << 24 | colour & 0xFFFFFF;
    }

    static int hsv(float hue) {
        return 0xFF000000 | com.thesift.client.ChromeClient.hsv(hue, 0.55F, 1.0F);
    }
}
