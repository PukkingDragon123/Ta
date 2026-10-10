package com.thesift.client.music;

import com.thesift.item.SiftInstrumentItem;
import com.thesift.music.Instrument;
import com.thesift.music.InstrumentPlay;
import com.thesift.music.Notes;
import com.thesift.music.PrismLight;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * INS free play: playing an instrument in the world, with no screen and no HUD.
 *
 * <p>Hold use and the instrument is raised into its playing stance (vanilla's using-item state, so
 * everyone sees it). While it is raised, one scheme plays every instrument:
 * <ul>
 *   <li>the number keys (the hotbar keys, 1-9) play the nine notes of the current register - the
 *   hotbar stays where it is;</li>
 *   <li>the mouse wheel shifts the register up or down (a soft note tells you where you are); with
 *   sneak held it turns a Prism instrument's light instead;</li>
 *   <li>attack accents the last note - or, on a chord instrument (the Weaver's
 *   Guitar), strums its whole chord.</li>
 * </ul>
 * and each family adds its own flourish: hold attack and sweep the mouse sideways across strings
 * (guitar, lute, harp) for a glissando; a held flute note breathes on; a held pad of the Thunder or
 * Prism Drums rolls; chimes swing and ring on. Every note sounds at once, feeds the local song
 * guide, goes to the server ({@link InstrumentPlay.PlayNote}) and is drawn and animated here
 * ({@link PlayAnim}, {@link InstrumentFx}); other players' notes arrive as {@link InstrumentPlay.Shown}.
 */
public final class FreePlay {
    /** Degrees of turning per string a sweep crosses. */
    private static final float SWEEP_STEP = 5.0F;
    /** Ticks between the breaths of a held flute note, and between the strokes of a drum roll. */
    private static final int BREATH_EVERY = 6;
    private static final int ROLL_EVERY = 2;
    private static final int ROLL_AFTER = 6;

    private static int register = -1;
    private static @Nullable Instrument registerFor;
    private static int light;
    private static final int[] HELD = new int[Instrument.KEYS];
    private static int lastPitch = -1;
    private static int lastKey = -1;
    private static float lastYaw;
    private static float sweep;
    private static boolean sweeping;
    private static int tick;
    private static int demoTicks;
    private static int demoStep;
    private static @Nullable ClientLevel seenLevel;

    private FreePlay() {
    }

    public static void register(IEventBus modBus) {
        InstrumentPlay.clientShown = FreePlay::onShown;
        NeoForge.EVENT_BUS.addListener(FreePlay::onKey);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FreePlay::onScroll);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, FreePlay::onInteract);
        NeoForge.EVENT_BUS.addListener(FreePlay::onTick);
        InstrumentParticle.register(modBus);
        InstrumentPlayProperty.register(modBus);
        InstrumentPoses.register(modBus);
    }

    /** The player's own music clock, in ticks with fractions (rhythm is judged on it, never on the network's delay). */
    public static double clock() {
        return System.nanoTime() / 5.0E7;
    }

    /** The hand of the instrument the local player has raised, or null. */
    public static @Nullable InteractionHand playing(@Nullable LocalPlayer p) {
        if (p == null || !p.isUsingItem() || !(p.getUseItem().getItem() instanceof SiftInstrumentItem)) {
            return null;
        }
        return p.getUsedItemHand();
    }

    /** The register the keys play on {@code ins} now (its home register until the wheel moves it). */
    public static int register(Instrument ins) {
        if (registerFor != ins || register < 0 || register >= ins.registerCount()) {
            registerFor = ins;
            register = ins.homeRegister();
        }
        return register;
    }

    private static @Nullable Instrument instrument(LocalPlayer p, InteractionHand hand) {
        ItemStack stack = p.getItemInHand(hand);
        return stack.getItem() instanceof SiftInstrumentItem item ? item.instrument() : null;
    }

    // ------------------------------------------------------------------ input

    private static void onKey(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        InteractionHand hand = playing(p);
        if (hand == null || mc.gui.screen() != null) {
            return;
        }
        Instrument ins = instrument(p, hand);
        if (ins == null) {
            return;
        }
        int action = event.getAction();
        for (int k = 0; k < Instrument.KEYS && k < mc.options.keyHotbarSlots.length; k++) {
            if (!mc.options.keyHotbarSlots[k].matches(event.getKeyEvent())) {
                continue;
            }
            if (action == 1) {
                press(p, hand, ins, k);
            } else if (action == 0) {
                HELD[k] = 0;
            }
        }
    }

    private static void onScroll(InputEvent.MouseScrollingEvent event) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer p = mc.player;
        InteractionHand hand = playing(p);
        if (hand == null || mc.gui.screen() != null || event.getScrollDeltaY() == 0.0) {
            return;
        }
        Instrument ins = instrument(p, hand);
        if (ins == null) {
            return;
        }
        event.setCanceled(true);
        int dir = event.getScrollDeltaY() > 0.0 ? 1 : -1;
        if (ins.prism() && p.isShiftKeyDown()) {
            light = Math.floorMod(light + dir, PrismLight.COUNT);
            InstrumentFx.light(p, ins, light);
            return;
        }
        int r = Mth.clamp(register(ins) + dir, 0, ins.registerCount() - 1);
        if (r != register) {
            register = r;
            int n = ins.keyNote(r, 0);
            if (n >= 0) {
                // a soft note at the bottom of the new register, for the player only
                p.playSound(ins.sound(), 0.3F, Notes.soundPitch(n));
            }
        }
    }

    private static void onInteract(InputEvent.InteractionKeyMappingTriggered event) {
        LocalPlayer p = Minecraft.getInstance().player;
        if (playing(p) != null && (event.isAttack() || event.isPickBlock())) {
            // while an instrument is raised, attack plays it (see onTick) - it never swings or breaks blocks
            event.setSwingHand(false);
            event.setCanceled(true);
        }
    }

    private static void onTick(ClientTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != seenLevel) {
            seenLevel = mc.level;
            PlayAnim.clear();
        }
        LocalPlayer p = mc.player;
        InteractionHand hand = playing(p);
        tick++;
        if (hand == null) {
            java.util.Arrays.fill(HELD, 0);
            sweeping = false;
            demoTicks = 0;
            return;
        }
        Instrument ins = instrument(p, hand);
        if (ins == null || mc.gui.screen() != null) {
            return;
        }
        // the number keys play notes now: the hotbar stays where it is
        for (KeyMapping key : mc.options.keyHotbarSlots) {
            while (key.consumeClick()) {
                // swallowed
            }
        }
        int attacks = 0;
        while (mc.options.keyAttack.consumeClick()) {
            attacks++;
        }
        boolean strings = ins.family() == Instrument.Family.STRINGS;
        if (strings && mc.options.keyAttack.isDown()) {
            sweepTick(p, hand, ins, attacks > 0);
        } else {
            sweeping = false;
            if (attacks > 0) {
                accent(p, hand, ins);
            }
        }
        for (int k = 0; k < Instrument.KEYS; k++) {
            if (HELD[k] <= 0) {
                continue;
            }
            HELD[k]++;
            int pitch = ins.keyNote(register(ins), k);
            if (pitch < 0) {
                continue;
            }
            if (ins.family() == Instrument.Family.FLUTE && k == lastKey && HELD[k] % BREATH_EVERY == 0) {
                breathe(p, hand, ins, pitch);
            } else if (ins.family() == Instrument.Family.DRUM && ins.extra() > 0 && HELD[k] > ROLL_AFTER && HELD[k] % ROLL_EVERY == 0) {
                play(p, hand, ins, pitch, 0);
            }
        }
        if (demoTicks > 0) {
            demoTicks--;
            if (demoTicks % 5 == 0) {
                int dk = Math.min(Instrument.KEYS - 1, demoStep % 8 < 5 ? demoStep % 8 : 8 - demoStep % 8);
                demoStep++;
                press(p, hand, ins, dk);
                HELD[dk] = 0;
            }
        }
    }

    // ------------------------------------------------------------------ playing

    private static void press(LocalPlayer p, InteractionHand hand, Instrument ins, int key) {
        int pitch = ins.keyNote(register(ins), key);
        if (pitch < 0) {
            return;
        }
        HELD[key] = 1;
        lastKey = key;
        play(p, hand, ins, pitch, 0);
    }

    /** Attack: the last note again, with weight - or its whole chord on a chord instrument. */
    private static void accent(LocalPlayer p, InteractionHand hand, Instrument ins) {
        int pitch = lastPitch >= 0 && ins.canPlay(lastPitch) ? lastPitch : ins.keyNote(register(ins), 0);
        if (pitch >= 0) {
            play(p, hand, ins, pitch, Notes.STRONG | (ins.chords() ? Notes.CHORD : 0));
        }
    }

    /** Strings: attack held and the mouse swept sideways runs up (to the right) or down the strings. */
    private static void sweepTick(LocalPlayer p, InteractionHand hand, Instrument ins, boolean clicked) {
        float yaw = p.getYRot();
        if (!sweeping) {
            sweeping = true;
            sweep = 0.0F;
            lastYaw = yaw;
            if (clicked) {
                accent(p, hand, ins);
            }
            return;
        }
        sweep += Mth.wrapDegrees(yaw - lastYaw);
        lastYaw = yaw;
        int played = 0;
        while (Math.abs(sweep) >= SWEEP_STEP && played < 2) {
            int dir = sweep > 0.0F ? 1 : -1;
            sweep -= dir * SWEEP_STEP;
            int k = Mth.clamp((lastKey < 0 ? 4 : lastKey) + dir, 0, Instrument.KEYS - 1);
            int pitch = ins.keyNote(register(ins), k);
            if (pitch < 0) {
                sweep = 0.0F;
                break;
            }
            lastKey = k;
            play(p, hand, ins, pitch, 0);
            played++;
        }
        if (played == 2) {
            sweep = 0.0F;
        }
    }

    /** A held flute note breathes on: soft, heard by everyone, seen - but no new note for the songs. */
    private static void breathe(LocalPlayer p, InteractionHand hand, Instrument ins, int pitch) {
        p.playSound(ins.sound(), 0.35F, Notes.soundPitch(pitch));
        int colour = ins.prism() ? light : -1;
        ClientPacketDistributor.sendToServer(InstrumentPlay.PlayNote.of(hand, pitch, colour, clock(), Notes.SUSTAIN));
        PlayAnim.sustain(p.getId(), 0.45);
        InstrumentFx.note(p, ins, pitch, 0, colour, -1, true);
    }

    /** Plays a note: it sounds at once, the guide hears it, it goes to the server, it is drawn and animated. */
    private static void play(LocalPlayer p, InteractionHand hand, Instrument ins, int pitch, int flags) {
        if (!ins.canPlay(pitch)) {
            return;
        }
        int colour = ins.prism() ? light : -1;
        double clock = clock();
        int f = flags | Notes.HEARD;
        if (!ins.chords()) {
            f &= ~Notes.CHORD;
        }
        Notes.play(p.level(), p, ins, pitch, colour, clock, f);
        ClientPacketDistributor.sendToServer(InstrumentPlay.PlayNote.of(hand, pitch, colour, clock, f));
        lastPitch = pitch;
        Song song = SongTracker.leading(Notes.CLIENT, p.level().getGameTime());
        PlayAnim.note(p.getId(), ins, pitch, (f & Notes.STRONG) != 0, (f & Notes.CHORD) != 0);
        InstrumentFx.note(p, ins, pitch, f, colour, song == null ? -1 : song.tint(), false);
    }

    /** Someone else played a note (from the server). */
    private static void onShown(InstrumentPlay.Shown shown) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }
        Entity e = mc.level.getEntity(shown.entity());
        if (!(e instanceof Player player) || player == mc.player) {
            return;
        }
        SiftInstrumentItem item = null;
        for (ItemStack stack : new ItemStack[]{player.getUseItem(), player.getMainHandItem(), player.getOffhandItem()}) {
            if (stack.getItem() instanceof SiftInstrumentItem held) {
                item = held;
                break;
            }
        }
        if (item == null) {
            return;
        }
        Instrument ins = item.instrument();
        if ((shown.flags() & Notes.SUSTAIN) != 0) {
            PlayAnim.sustain(player.getId(), 0.45);
            InstrumentFx.note(player, ins, shown.pitch(), 0, shown.light(), -1, true);
            return;
        }
        PlayAnim.note(player.getId(), ins, shown.pitch(), (shown.flags() & Notes.STRONG) != 0, (shown.flags() & Notes.CHORD) != 0);
        InstrumentFx.note(player, ins, shown.pitch(), shown.flags(), shown.light(), shown.tint(), false);
    }

    // ------------------------------------------------------------------ the client smoke test

    /** CI: plays a little rising and falling phrase on the raised instrument for {@code ticks} ticks. */
    public static void demo(int ticks) {
        demoTicks = ticks;
        demoStep = 0;
    }
}
