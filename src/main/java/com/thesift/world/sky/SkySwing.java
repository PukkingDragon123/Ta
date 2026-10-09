package com.thesift.world.sky;

import com.thesift.TheSift;
import io.netty.buffer.ByteBuf;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * W-sky: swinging on Sky Vines, the server's half.
 *
 * <p>Players move on their own client, so the rope physics runs there ({@link SkySwingClient}, {@link SwingPhysics}).
 * The client tells the server when its player grabs a vine and lets go ({@link Swing}). While a player hangs on a rope
 * the server keeps their fall distance at zero (swinging up and down must not add up to a deadly fall - the fall
 * after you let go counts from where you let go), keeps the anti-flying check quiet, and shows everyone else the vine
 * the player holds ({@link SkyRope}). A claimed rope must really be there: a vine block at the anchor, within reach.
 */
public final class SkySwing {
    /** Client: grab the Sky Vine at this block (set by SkySwingClient; a no-op on a dedicated server). */
    public static volatile Consumer<BlockPos> clientGrab = pos -> {
    };
    /** The farthest a swinging player may be from the block their rope hangs from. */
    public static final double MAX_REACH = SkyVines.MAX_STRAND + 6.0;
    private static final Map<UUID, Hold> HOLDING = new HashMap<>();

    private record Hold(BlockPos anchor, SkyRope rope) {
    }

    private SkySwing() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SkySwing::registerPayloads);
        NeoForge.EVENT_BUS.addListener(SkySwing::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SkySwing::onLogout);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Swing.TYPE, Swing.CODEC, (payload, context) -> context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                serverSwing(player, payload.on(), BlockPos.of(payload.anchor()), payload.span());
            }
        }));
    }

    /** True while the server knows this player hangs on a rope. */
    public static boolean isSwinging(Player player) {
        return HOLDING.containsKey(player.getUUID());
    }

    /** True while this rope entity is the one the player holds. */
    static boolean holds(Player player, SkyRope rope) {
        Hold h = HOLDING.get(player.getUUID());
        return h != null && h.rope() == rope;
    }

    /** Where the rope hangs from: the top of a strand, the middle of a span block. */
    public static Vec3 pivot(BlockPos anchor, boolean span) {
        return span ? Vec3.atCenterOf(anchor) : new Vec3(anchor.getX() + 0.5, anchor.getY() + 1.0, anchor.getZ() + 0.5);
    }

    /**
     * Server: {@code player} took hold of (on) or let go of (off) the vine anchored at {@code anchor}. Grabbing again
     * while swinging moves the rope (to another vine, or along a span). Returns the rope the player now holds, if any.
     */
    public static @Nullable SkyRope serverSwing(ServerPlayer player, boolean on, BlockPos anchor, boolean span) {
        if (!(player.level() instanceof ServerLevel level)) {
            return null;
        }
        Hold old = HOLDING.get(player.getUUID());
        if (!on) {
            if (old != null) {
                release(level, player, old);
                player.resetFallDistance(); // the fall from here on is a fall from where you let go
                level.playSound(player, player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS,
                        0.5F, 1.5F);
            }
            return null;
        }
        Vec3 pivot = pivot(anchor, span);
        if (!SkyVines.isVine(level.getBlockState(anchor)) || player.position().distanceTo(pivot) > MAX_REACH || player.isSpectator()) {
            if (old != null) {
                release(level, player, old);
            }
            return null;
        }
        SkyRope rope = old != null && !old.rope().isRemoved() ? old.rope() : null;
        if (rope == null) {
            rope = SkyRope.create(level, player, pivot);
            level.playSound(player, anchor, SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1.0F, 0.8F);
        } else {
            rope.setPos(pivot.x, pivot.y, pivot.z);
        }
        HOLDING.put(player.getUUID(), new Hold(anchor.immutable(), rope));
        player.resetFallDistance();
        return rope;
    }

    private static void release(ServerLevel level, ServerPlayer player, Hold hold) {
        HOLDING.remove(player.getUUID());
        hold.rope().discard();
    }

    /** A swinging player never builds up a fall, and is not taken for a flyer; a rope that is gone lets them go. */
    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Hold hold = HOLDING.get(player.getUUID());
        if (hold == null) {
            return;
        }
        if (!player.isAlive() || !SkyVines.isVine(level.getBlockState(hold.anchor()))
                || player.position().distanceTo(Vec3.atCenterOf(hold.anchor())) > MAX_REACH) {
            release(level, player, hold);
            return;
        }
        player.resetFallDistance();
        player.connection.resetFlyingTicks();
    }

    private static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        Hold hold = HOLDING.remove(player.getUUID());
        if (hold != null) {
            hold.rope().discard();
        }
    }

    /** Client to server: the player grabbed (on) or let go of (off) the vine anchored at a block. */
    public record Swing(boolean on, long anchor, boolean span) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Swing> TYPE = new CustomPacketPayload.Type<>(TheSift.id("sky_swing"));
        public static final StreamCodec<ByteBuf, Swing> CODEC = StreamCodec.of(Swing::write, Swing::read);

        public static Swing of(boolean on, BlockPos anchor, boolean span) {
            return new Swing(on, anchor.asLong(), span);
        }

        private static void write(ByteBuf buf, Swing s) {
            buf.writeBoolean(s.on);
            buf.writeLong(s.anchor);
            buf.writeBoolean(s.span);
        }

        private static Swing read(ByteBuf buf) {
            return new Swing(buf.readBoolean(), buf.readLong(), buf.readBoolean());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
