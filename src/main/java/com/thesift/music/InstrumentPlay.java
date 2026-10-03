package com.thesift.music;

import com.thesift.TheSift;
import com.thesift.item.SiftInstrumentItem;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * M1 instrument play: how a note gets from a play screen to the world.
 *
 * <p>Using an {@link SiftInstrumentItem} opens its play screen on the client ({@link #clientOpen}, set
 * by the client at start-up). Every note played there sounds at once for the player and is sent
 * here as {@link PlayNote}; {@link #serverPlay} checks the player really holds that instrument and
 * that it can sound that note, and plays it through {@link Notes#play} - for everyone else, for the
 * song tracker and for every listener. The song test calls {@link #serverPlay} too, so CI proves
 * the songs through the very path the screens use.
 */
public final class InstrumentPlay {
    /** Client: opens the play screen for the instrument in this hand (a no-op on a dedicated server). */
    public static volatile Consumer<InteractionHand> clientOpen = hand -> {
    };
    /** At most this many notes per player per tick (a drum roll is one every two). */
    private static final int MAX_PER_TICK = 4;
    private static final Map<UUID, long[]> RATE = new HashMap<>();

    private InstrumentPlay() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InstrumentPlay::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(PlayNote.TYPE, PlayNote.CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player player = context.player();
            serverPlay(player, payload.interactionHand(), payload.pitch(), payload.colour(), payload.clock(), payload.flags());
        }));
    }

    /**
     * Server: {@code player} plays {@code pitch} on the instrument in {@code hand}, in light
     * {@code colour} (Prism instruments; ignored otherwise) at {@code clock} on their own clock.
     *
     * @return false if the note was refused (no instrument in that hand, a note it cannot sound, too many notes at once)
     */
    public static boolean serverPlay(Player player, InteractionHand hand, int pitch, int colour, double clock, int flags) {
        if (!(player.level() instanceof ServerLevel level) || !player.isAlive() || player.isSpectator()) {
            return false;
        }
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof SiftInstrumentItem item)) {
            return false;
        }
        Instrument instrument = item.instrument();
        if (!instrument.canPlay(pitch) || !(player instanceof FakePlayer) && !allowed(player, level.getGameTime())) {
            return false;
        }
        int light = instrument.prism() ? Mth.clamp(colour, 0, PrismLight.COUNT - 1) : -1;
        int f = flags & (Notes.HEARD | Notes.CHORD | Notes.STRONG);
        if (!instrument.chords()) {
            f &= ~Notes.CHORD;
        }
        Notes.play(level, player, instrument, pitch, light, clock, f);
        return true;
    }

    /** Spam guard: true while the player has played fewer than {@link #MAX_PER_TICK} notes this tick. */
    private static boolean allowed(Player player, long now) {
        long[] r = RATE.computeIfAbsent(player.getUUID(), u -> new long[]{Long.MIN_VALUE, 0L});
        if (r[0] != now) {
            r[0] = now;
            r[1] = 0L;
        }
        if (RATE.size() > 256) {
            RATE.clear();
        }
        return ++r[1] <= MAX_PER_TICK;
    }

    /** Client to server: one note from a play screen. {@code hand} 0 main, 1 off hand. */
    public record PlayNote(int hand, int pitch, int colour, double clock, int flags) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<PlayNote> TYPE = new CustomPacketPayload.Type<>(TheSift.id("play_note"));
        public static final StreamCodec<ByteBuf, PlayNote> CODEC = StreamCodec.of(PlayNote::write, PlayNote::read);

        public static PlayNote of(InteractionHand hand, int pitch, int colour, double clock, int flags) {
            return new PlayNote(hand == InteractionHand.OFF_HAND ? 1 : 0, pitch, colour, clock, flags);
        }

        private static void write(ByteBuf buf, PlayNote p) {
            buf.writeByte(p.hand);
            buf.writeByte(p.pitch);
            buf.writeByte(p.colour);
            buf.writeDouble(p.clock);
            buf.writeByte(p.flags);
        }

        private static PlayNote read(ByteBuf buf) {
            return new PlayNote(buf.readByte(), buf.readByte(), buf.readByte(), buf.readDouble(), buf.readByte());
        }

        public InteractionHand interactionHand() {
            return this.hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
