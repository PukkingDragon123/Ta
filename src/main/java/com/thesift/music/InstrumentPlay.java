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
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * How a note gets from the player's hands to the world (INS free play: no screens any more).
 *
 * <p>The client plays an instrument in its playing stance (client {@code FreePlay}): every note
 * sounds at once for the player and is sent here as {@link PlayNote}; {@link #serverPlay} checks the
 * player really holds that instrument and that it can sound that note, and plays it through
 * {@link Notes#play} - for everyone else, for the song tracker and for every listener - then tells
 * the players watching ({@link Shown}) so they see the hands move, the parts ring and the notes fly
 * in the song's colour. A held flute note breathes on ({@link Notes#SUSTAIN}): heard and seen by
 * everyone, but never a new note for the songs. The song test calls {@link #serverPlay} too, so CI
 * proves the songs through the very path free play uses.
 */
public final class InstrumentPlay {
    /** Client: a note someone else played, to animate and draw (a no-op on a dedicated server). */
    public static volatile Consumer<Shown> clientShown = s -> {
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
        event.registrar("1").playToClient(Shown.TYPE, Shown.CODEC, (payload, context) -> context.enqueueWork(() -> clientShown.accept(payload)));
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
        if ((flags & Notes.SUSTAIN) != 0) {
            sustain(level, player, instrument, pitch, light);
            return true;
        }
        int f = flags & (Notes.HEARD | Notes.CHORD | Notes.STRONG);
        if (!instrument.chords()) {
            f &= ~Notes.CHORD;
        }
        Notes.play(level, player, instrument, pitch, light, clock, f | Notes.SHOWN);
        Song song = SongTracker.leading(player, level.getGameTime());
        PacketDistributor.sendToPlayersTrackingEntity(player, new Shown(player.getId(), pitch, f, light, song == null ? -1 : song.tint()));
        return true;
    }

    /** A held flute note breathing on: soft, for everyone but the player (who hears it already), and seen. */
    private static void sustain(ServerLevel level, Player player, Instrument instrument, int pitch, int light) {
        Vec3 at = Notes.mouth(player);
        SoundEvent sound = instrument.sound();
        level.playSound(player, at.x, at.y, at.z, sound, SoundSource.PLAYERS, 0.5F, Notes.soundPitch(pitch));
        PacketDistributor.sendToPlayersTrackingEntity(player, new Shown(player.getId(), pitch, Notes.SUSTAIN, light, -1));
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

    /** Client to server: one note played in free play. {@code hand} 0 main, 1 off hand. */
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

    /**
     * Server to the players watching: entity {@code entity} played {@code pitch} ({@link Notes} flags; {@code light}
     * the Prism light or -1; {@code tint} the colour of the song it is playing, or -1).
     */
    public record Shown(int entity, int pitch, int flags, int light, int tint) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Shown> TYPE = new CustomPacketPayload.Type<>(TheSift.id("note_shown"));
        public static final StreamCodec<ByteBuf, Shown> CODEC = StreamCodec.of(Shown::write, Shown::read);

        private static void write(ByteBuf buf, Shown s) {
            buf.writeInt(s.entity);
            buf.writeByte(s.pitch);
            buf.writeByte(s.flags);
            buf.writeByte(s.light + 1);
            buf.writeInt(s.tint);
        }

        private static Shown read(ByteBuf buf) {
            return new Shown(buf.readInt(), buf.readUnsignedByte(), buf.readUnsignedByte(), buf.readUnsignedByte() - 1, buf.readInt());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
