package com.thesift.worldgen;

import com.mojang.serialization.MapCodec;
import com.thesift.registry.ModMansion;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

/**
 * MANSION: the Woodland Mansion's secret room, as a structure of its own that rides along with every mansion.
 *
 * <p>Its structure set ({@code thesift:mansion_vaults}) has the mansions' exact placement - the same random spread,
 * spacing, separation and salt - so it is tried in the very chunk where a mansion is. And it finds its generation point
 * exactly as {@code WoodlandMansionStructure} does: the same biome check, the same first draw of the chunk's structure
 * random (the mansion's rotation), the same lowest corner of the 5x5 box and the same "not below y 60". So wherever a
 * mansion generates, this generates, knowing the mansion's origin and rotation - and with them where the mansion's
 * fixed ground-floor corridors are (WoodlandMansionPieces always lays out the cells round the entrance the same way).
 *
 * <p>Its step ({@code strongholds}, in data/thesift/worldgen/structure/mansion_vault.json) comes right after the
 * mansion's ({@code surface_structures}), so in every chunk the vault is built after the mansion and its cobblestone
 * foundation. The pieces are {@link MansionVaultPiece}: the bookcase door and the vault under the mansion, and the
 * Hornblower and Bard who keep the corridors.
 */
public class MansionVaultStructure extends Structure {
    public static final MapCodec<MansionVaultStructure> CODEC = simpleCodec(MansionVaultStructure::new);

    public MansionVaultStructure(Structure.StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<Structure.GenerationStub> findGenerationPoint(Structure.GenerationContext context) {
        // exactly WoodlandMansionStructure.findGenerationPoint (keep in step with it)
        ChunkPos chunk = context.chunkPos();
        int x = chunk.getBlockX(7);
        int z = chunk.getBlockZ(7);
        if (!context.couldValidBiomeExistInTerrainColumn(x, z)) {
            return Optional.empty();
        }
        Rotation rotation = Rotation.getRandom(context.random());
        BlockPos origin = this.getLowestYIn5by5Box(context, x, z, rotation);
        if (origin.getY() < 60) {
            return Optional.empty();
        }
        return Optional.of(new Structure.GenerationStub(origin, builder -> MansionVaultPiece.addPieces(builder, origin, rotation)));
    }

    @Override
    public StructureType<?> type() {
        return ModMansion.MANSION_VAULT.get();
    }
}
