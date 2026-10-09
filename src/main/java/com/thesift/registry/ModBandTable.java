package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.entity.BandTableBlockEntity;
import com.thesift.enchant.SiftEnchantEffects;
import com.thesift.music.SongEvents;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * F2 Band Table: the Music Band Table's block entity, the score screen's one message to the server ({@link Choose}),
 * the table's ear on every note, and the Sift enchantments' effects ({@link SiftEnchantEffects}). The block itself is
 * declared in tools/spec.py ({@code thesift:band_table}), the enchantments in data (tools/band_table.py).
 */
public final class ModBandTable {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, TheSift.MODID);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BandTableBlockEntity>> BAND_TABLE_BE = BLOCK_ENTITIES.register(
            "band_table", () -> new BlockEntityType<>(BandTableBlockEntity::new, ModBlocks.BAND_TABLE.get()));
    private static boolean listening;

    private ModBandTable() {
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        modBus.addListener(ModBandTable::payloads);
        if (!listening) {
            listening = true;
            SongEvents.listenNotes(BandTableBlockEntity::hearNote);
        }
        SiftEnchantEffects.register();
    }

    private static void payloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(Choose.TYPE, Choose.CODEC, (payload, context) -> context.enqueueWork(() -> {
            Player player = context.player();
            if (player.level().getBlockEntity(payload.pos()) instanceof BandTableBlockEntity table) {
                Component why = table.choose(player, payload.slot(), payload.enchant());
                if (why != null) {
                    player.sendOverlayMessage(why.copy().withStyle(net.minecraft.ChatFormatting.RED));
                }
            }
        }));
    }

    /** Client to server: play for enchantment {@code enchant} (a SiftEnchant ordinal) on the item in inventory slot {@code slot}. */
    public record Choose(BlockPos pos, int slot, int enchant) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Choose> TYPE = new CustomPacketPayload.Type<>(TheSift.id("band_table_choose"));
        public static final StreamCodec<ByteBuf, Choose> CODEC = StreamCodec.of(Choose::write, Choose::read);

        private static void write(ByteBuf buf, Choose c) {
            buf.writeLong(c.pos.asLong());
            buf.writeByte(c.slot);
            buf.writeByte(c.enchant);
        }

        private static Choose read(ByteBuf buf) {
            return new Choose(BlockPos.of(buf.readLong()), buf.readByte(), buf.readByte());
        }

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
