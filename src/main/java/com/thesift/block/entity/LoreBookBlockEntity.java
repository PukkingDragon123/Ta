package com.thesift.block.entity;

import com.thesift.registry.ModKnowledge;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/** F3: a placed Lore Book keeps its text ({@code thesift:lore}), shows it to whoever opens it and drops with it. */
public class LoreBookBlockEntity extends BlockEntity {
    private String lore = "";

    public LoreBookBlockEntity(BlockPos pos, BlockState state) {
        super(ModKnowledge.LORE_BOOK.get(), pos, state);
    }

    public String lore() {
        return this.lore;
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        String id = components.get(ModKnowledge.LORE.get());
        this.lore = id == null ? "" : id;
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (!this.lore.isEmpty()) {
            components.set(ModKnowledge.LORE.get(), this.lore);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putString("Lore", this.lore);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.lore = input.getStringOr("Lore", "");
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }
}
