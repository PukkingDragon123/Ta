package com.thesift.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BrushableBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * W1 World &amp; terrain: a relic cache. The Sift's old ruins have crumbled away; what is left of
 * them lies a block or two under the sand and mud. A few {@code relic} blocks (Suspicious
 * Chime Sand) replace {@code hosts} ground just below the surface, each given its archaeology loot
 * table (rarely the {@code rare} one), and a fallen {@code marker} block or two pokes out of the
 * ground above to give the spot away - brush them for the Sift's relics and lost music sheets.
 */
public record RelicCacheFeature(BlockState relic, List<BlockState> hosts, ResourceKey<LootTable> common, ResourceKey<LootTable> rare, float rareChance,
        Optional<BlockState> marker) implements Feature {
    public static final MapCodec<RelicCacheFeature> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockState.CODEC.fieldOf("relic").forGetter(RelicCacheFeature::relic),
            BlockState.CODEC.listOf().fieldOf("hosts").forGetter(RelicCacheFeature::hosts),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("common").forGetter(RelicCacheFeature::common),
            ResourceKey.codec(Registries.LOOT_TABLE).fieldOf("rare").forGetter(RelicCacheFeature::rare),
            Codec.floatRange(0.0F, 1.0F).fieldOf("rare_chance").forGetter(RelicCacheFeature::rareChance),
            BlockState.CODEC.optionalFieldOf("marker").forGetter(RelicCacheFeature::marker)
    ).apply(i, RelicCacheFeature::new));

    @Override
    public MapCodec<RelicCacheFeature> codec() {
        return CODEC;
    }

    private boolean isHost(BlockState state) {
        for (BlockState h : this.hosts) {
            if (state.is(h.getBlock())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos origin) {
        int want = 2 + random.nextInt(4);
        int placed = 0;
        for (int tries = 0; tries < want * 4 && placed < want; tries++) {
            int x = origin.getX() + random.nextInt(7) - 3;
            int z = origin.getZ() + random.nextInt(7) - 3;
            int top = level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, x, z) - 1;
            BlockPos p = new BlockPos(x, top - random.nextInt(3), z);
            if (!this.isHost(level.getBlockState(p))) {
                continue;
            }
            level.setBlock(p, this.relic, Block.UPDATE_CLIENTS);
            if (level.getBlockEntity(p) instanceof BrushableBlockEntity relicEntity) {
                relicEntity.setLootTable(random.nextFloat() < this.rareChance ? this.rare : this.common, random.nextLong());
            }
            placed++;
        }
        if (placed > 0 && this.marker.isPresent()) {
            int markers = 1 + random.nextInt(2);
            for (int m = 0; m < markers; m++) {
                int mx = origin.getX() + random.nextInt(5) - 2;
                int mz = origin.getZ() + random.nextInt(5) - 2;
                BlockPos at = new BlockPos(mx, level.getHeight(Heightmap.Types.OCEAN_FLOOR_WG, mx, mz), mz);
                BlockState below = level.getBlockState(at.below());
                if (level.getBlockState(at).isAir() && !below.isAir() && below.getFluidState().isEmpty()) {
                    level.setBlock(at, this.marker.get(), Block.UPDATE_CLIENTS);
                }
            }
        }
        return placed > 0;
    }
}
