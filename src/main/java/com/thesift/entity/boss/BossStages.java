package com.thesift.entity.boss;

import com.thesift.TheSift;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Boss stage cutscenes (agent B2): whenever one of the great players loses a stage - the Thumper,
 * the Weaver (entity id strummer) and the Conductor (entity id dictator) each have three, and the
 * last one is their defeat - every player nearby is sent {@link StageCleared} and watches a short
 * film of it (client/BossCinematic). While it plays they cannot be hurt; sneaking skips it
 * ({@link Skip}), which also ends the protection.
 */
public final class BossStages {
    public static final int THUMPER = 0;
    public static final int WEAVER = 1;
    public static final int CONDUCTOR = 2;
    /** Stage number sent for the boss's defeat. */
    public static final int DEFEATED = 0;
    /** How long the film runs, in ticks. */
    public static final int LENGTH = 84;
    private static final double RANGE = 56.0;
    /** Who is watching, and until which game time they are protected. */
    private static final Map<UUID, Long> WATCHING = new HashMap<>();
    /** Set by the client (BossCinematic) to play the film; a no-op on a dedicated server. */
    public static volatile Consumer<StageCleared> clientHandler = p -> {
    };

    private BossStages() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(BossStages::registerPayloads);
        NeoForge.EVENT_BUS.addListener(BossStages::onIncomingDamage);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(StageCleared.TYPE, StageCleared.CODEC,
                (payload, context) -> context.enqueueWork(() -> clientHandler.accept(payload)));
        event.registrar("1").playToServer(Skip.TYPE, Skip.CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player player = context.player();
            WATCHING.remove(player.getUUID());
        }));
    }

    /**
     * A boss has lost a stage: {@code stage} is the stage it enters now (2 or 3), or
     * {@link #DEFEATED}. Plays the film for everyone close enough to have been fighting.
     */
    public static void cleared(ServerLevel level, LivingEntity boss, int kind, int stage) {
        StageCleared payload = new StageCleared(boss.getId(), kind, stage, boss.getX(), boss.getY(), boss.getZ(),
                boss.getBbWidth(), boss.getBbHeight());
        long until = level.getGameTime() + LENGTH + 10;
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, boss.getBoundingBox().inflate(RANGE))) {
            WATCHING.put(p.getUUID(), until);
            PacketDistributor.sendToPlayer(p, payload);
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || WATCHING.isEmpty()) {
            return;
        }
        Long until = WATCHING.get(player.getUUID());
        if (until == null) {
            return;
        }
        if (player.level().getGameTime() > until) {
            WATCHING.remove(player.getUUID());
            return;
        }
        event.setCanceled(true);
    }

    /** Server to client: play the cutscene of a boss losing a stage. */
    public record StageCleared(int entityId, int kind, int stage, double x, double y, double z, float width, float height)
            implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<StageCleared> TYPE = new CustomPacketPayload.Type<>(TheSift.id("boss_stage"));
        public static final StreamCodec<ByteBuf, StageCleared> CODEC = StreamCodec.of(StageCleared::write, StageCleared::read);

        private static void write(ByteBuf buf, StageCleared p) {
            buf.writeInt(p.entityId);
            buf.writeByte(p.kind);
            buf.writeByte(p.stage);
            buf.writeDouble(p.x);
            buf.writeDouble(p.y);
            buf.writeDouble(p.z);
            buf.writeFloat(p.width);
            buf.writeFloat(p.height);
        }

        private static StageCleared read(ByteBuf buf) {
            return new StageCleared(buf.readInt(), buf.readByte(), buf.readByte(), buf.readDouble(), buf.readDouble(), buf.readDouble(),
                    buf.readFloat(), buf.readFloat());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the player sneaked to skip the film. */
    public record Skip() implements CustomPacketPayload {
        public static final Skip INSTANCE = new Skip();
        public static final CustomPacketPayload.Type<Skip> TYPE = new CustomPacketPayload.Type<>(TheSift.id("boss_stage_skip"));
        public static final StreamCodec<ByteBuf, Skip> CODEC = StreamCodec.unit(INSTANCE);

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
