package com.thesift.block.entity;

import com.thesift.TheSift;
import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * The treasure frozen inside a frozen Music Crystal, shown spinning in the clear crystal. Crystals
 * frozen by worldgen or a structure have no treasure yet: it is drawn from
 * {@code thesift:gameplay/frozen_crystal} the first time the crystal is seen or broken. Breaking the
 * crystal shatters it and sets the treasure free.
 */
public class MusicCrystalBlockEntity extends BlockEntity {
    public static final ResourceKey<LootTable> TREASURE = ResourceKey.create(Registries.LOOT_TABLE, TheSift.id("gameplay/frozen_crystal"));
    private ItemStack item = ItemStack.EMPTY;

    public MusicCrystalBlockEntity(BlockPos pos, BlockState state) {
        super(ModCaravans.MUSIC_CRYSTAL_ENTITY.get(), pos, state);
    }

    public ItemStack getItem() {
        return this.item;
    }

    public void setItem(ItemStack stack) {
        this.item = stack.copyWithCount(1);
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Server side: draws a treasure for a crystal that was frozen empty. */
    private void ensureItem() {
        if (this.item.isEmpty() && this.level instanceof ServerLevel server) {
            LootTable table = server.getServer().reloadableRegistries().getLootTable(TREASURE);
            LootParams params = new LootParams.Builder(server).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(this.worldPosition))
                    .create(LootContextParamSets.CHEST);
            List<ItemStack> drops = table.getRandomItems(params);
            if (!drops.isEmpty()) {
                this.item = drops.get(0).copyWithCount(1);
                this.setChanged();
            }
        }
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.level instanceof ServerLevel server) {
            this.ensureItem();
            if (!this.item.isEmpty()) {
                Block.popResource(server, pos, this.item);
                this.item = ItemStack.EMPTY;
            }
            server.playSound(null, pos, ModSounds.MUSIC_CRYSTAL_SHATTER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            server.sendParticles(ModParticles.STAR_SPARKLE.get(), pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 16, 0.3, 0.3, 0.3, 0.05);
        }
        super.preRemoveSideEffects(pos, state);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.item.isEmpty()) {
            output.store("Item", ItemStack.CODEC, this.item);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.item = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        this.ensureItem();
        return this.saveWithoutMetadata(registries);
    }
}
