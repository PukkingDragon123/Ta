package com.thesift.entity;

import com.thesift.TheSift;
import com.thesift.music.SongEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import org.jspecify.annotations.Nullable;

/**
 * The creatures' shared ears and reins (agent A1):
 * <ul>
 *   <li>every note played anywhere ({@link SongEvents#note}) reaches the Bulbs, Harmoners,
 *   Slumblers and baby Stompers close enough to hear it, each in its own way;</li>
 *   <li>a rider's attack key on a tame Stomper arrives as {@link RiderStomp} and makes it stomp.</li>
 * </ul>
 */
public final class CreatureLife {
    public static final double HEARING = 14.0;

    private CreatureLife() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(CreatureLife::registerPayloads);
        SongEvents.listenNotes(CreatureLife::onNote);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(Stomper::onDismount); // S1: a Stomper's trunk holds its victim fast
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(RiderStomp.TYPE, RiderStomp.CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player player = context.player();
            if (player.getVehicle() instanceof Stomper stomper && stomper.getControllingPassenger() == player) {
                stomper.riderStomp(player);
            }
        }));
    }

    private static void onNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        AABB box = new AABB(at, at).inflate(HEARING);
        double r2 = HEARING * HEARING;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive() && m.distanceToSqr(at) <= r2)) {
            if (mob instanceof Bulb bulb) {
                bulb.hearNote(level, pitch);
            } else if (mob instanceof Harmoner harmoner) {
                harmoner.hearNote(level, pitch);
            } else if (mob instanceof Slumbler slumbler) {
                slumbler.hearNote(level, pitch);
            } else if (mob instanceof Stomper stomper) {
                stomper.hearNote(level, player, pitch);
            }
        }
    }

    /** Sent by the client when a Stomper's rider presses the attack key (see client RiderInput). */
    public record RiderStomp() implements CustomPacketPayload {
        public static final RiderStomp INSTANCE = new RiderStomp();
        public static final CustomPacketPayload.Type<RiderStomp> TYPE = new CustomPacketPayload.Type<>(TheSift.id("rider_stomp"));
        public static final StreamCodec<ByteBuf, RiderStomp> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
