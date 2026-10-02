package com.thesift.block.entity;

import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** Holds one instrument; locked while the stage performs and while the Conductor lives. */
public class InstrumentAltarBlockEntity extends BlockEntity {
    private ItemStack item = ItemStack.EMPTY;
    private boolean locked;
    public int age;

    public InstrumentAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.INSTRUMENT_ALTAR.get(), pos, state);
    }

    public static boolean isInstrument(ItemStack stack) {
        return stack.is(ModItems.CONGA_DRUM.get()) || stack.is(ModItems.CRANE_FLUTE.get()) || stack.is(ModItems.WEAVER_GUITAR.get()); // only the Weaver's own guitar sounds on the Grand Stage
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setLocked(boolean locked) {
        if (this.locked != locked) {
            this.locked = locked;
            this.sync();
        }
    }

    public boolean isLocked() {
        return this.locked;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, InstrumentAltarBlockEntity altar) {
        altar.age++;
        if (level.isClientSide() && !altar.item.isEmpty() && level.getRandom().nextInt(altar.locked ? 2 : 8) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), pos.getX() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, pos.getY() + 1.6,
                    pos.getZ() + 0.5 + (level.getRandom().nextDouble() - 0.5) * 0.8, level.getRandom().nextDouble(), 0.0, 0.0);
        }
    }

    public InteractionResult place(Player player, ItemStack stack) {
        if (!isInstrument(stack) || !this.item.isEmpty() || this.locked) {
            return InteractionResult.PASS;
        }
        if (this.level instanceof ServerLevel server) {
            this.item = stack.split(1);
            this.sync();
            server.playSound(null, this.worldPosition, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.BLOCKS, 1.5F, 0.8F);
            server.playSound(null, this.worldPosition, SoundEvents.BEACON_POWER_SELECT, SoundSource.BLOCKS, 1.0F, 1.4F);
            server.sendParticles(ModParticles.RESONANCE_RING.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1,
                    this.worldPosition.getZ() + 0.5, 0, 1.5, 0.0, 0.0, 1.0);
            server.sendParticles(ModParticles.STAR_SPARKLE.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.5,
                    this.worldPosition.getZ() + 0.5, 12, 0.3, 0.3, 0.3, 0.02);
        }
        return InteractionResult.SUCCESS;
    }

    public InteractionResult take(Player player) {
        if (this.item.isEmpty() || this.locked) {
            return InteractionResult.PASS;
        }
        if (!this.level.isClientSide()) {
            if (!player.getInventory().add(this.item)) {
                Block.popResource(this.level, this.worldPosition.above(), this.item);
            }
            this.item = ItemStack.EMPTY;
            this.sync();
        }
        return InteractionResult.SUCCESS;
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.item.isEmpty()) {
            output.store("Item", ItemStack.CODEC, this.item);
        }
        output.putBoolean("Locked", this.locked);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.item = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.locked = input.getBooleanOr("Locked", false);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!this.item.isEmpty() && this.level != null) {
            Block.popResource(this.level, pos, this.item);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
