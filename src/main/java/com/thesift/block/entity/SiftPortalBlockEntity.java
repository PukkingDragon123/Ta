package com.thesift.block.entity;

import com.thesift.registry.ModGateFx;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * B1 Portal & sky FX: one cell of an open Sift portal. It holds nothing; it is only here so the
 * client can draw the cell as a window onto the Sift's sky (see
 * {@code com.thesift.client.gate.SiftPortalRenderer}).
 */
public class SiftPortalBlockEntity extends BlockEntity {
    public SiftPortalBlockEntity(BlockPos pos, BlockState state) {
        super(ModGateFx.SIFT_PORTAL.get(), pos, state);
    }
}
