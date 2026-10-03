package com.thesift.music.band;

import com.thesift.TheSift;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * What the clients learn about bands: who plays in which band ({@link Sync}, for the band HUD and
 * the members' bob and sway) and when they just played ({@link Pulse}, for the bounce on the beat).
 * The client side (client/music/BandClient) installs the handlers; on a dedicated server they
 * stay no-ops.
 */
public final class BandPayloads {
    public static volatile Consumer<Sync> clientSync = p -> {
    };
    public static volatile Consumer<Pulse> clientPulse = p -> {
    };

    private BandPayloads() {
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(Sync.TYPE, Sync.CODEC, (payload, context) -> context.enqueueWork(() -> clientSync.accept(payload)));
        event.registrar("1").playToClient(Pulse.TYPE, Pulse.CODEC, (payload, context) -> context.enqueueWork(() -> clientPulse.accept(payload)));
    }

    private static void writeInts(ByteBuf buf, int[] values) {
        buf.writeByte(values.length);
        for (int v : values) {
            buf.writeInt(v);
        }
    }

    private static int[] readInts(ByteBuf buf) {
        int n = buf.readUnsignedByte();
        int[] values = new int[n];
        for (int i = 0; i < n; i++) {
            values[i] = buf.readInt();
        }
        return values;
    }

    /**
     * Server to client: the band led by entity {@code leader} - its members' entity ids, their HUD
     * colours, how much of their stay is left (0..255) - and its power (1 alone). No members: the
     * band broke up.
     */
    public record Sync(int leader, float power, int[] members, int[] colours, int[] stay) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Sync> TYPE = new CustomPacketPayload.Type<>(TheSift.id("band_sync"));
        public static final StreamCodec<ByteBuf, Sync> CODEC = StreamCodec.of(Sync::write, Sync::read);

        private static void write(ByteBuf buf, Sync p) {
            buf.writeInt(p.leader);
            buf.writeFloat(p.power);
            writeInts(buf, p.members);
            writeInts(buf, p.colours);
            writeInts(buf, p.stay);
        }

        private static Sync read(ByteBuf buf) {
            return new Sync(buf.readInt(), buf.readFloat(), readInts(buf), readInts(buf), readInts(buf));
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Server to client: these band members just played a note ({@code pitch} 0-24): they bounce. */
    public record Pulse(int[] members, int pitch) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Pulse> TYPE = new CustomPacketPayload.Type<>(TheSift.id("band_pulse"));
        public static final StreamCodec<ByteBuf, Pulse> CODEC = StreamCodec.of(Pulse::write, Pulse::read);

        private static void write(ByteBuf buf, Pulse p) {
            writeInts(buf, p.members);
            buf.writeByte(p.pitch);
        }

        private static Pulse read(ByteBuf buf) {
            return new Pulse(readInts(buf), buf.readUnsignedByte());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
