package com.thesift.worldgen;

import com.thesift.block.LooseBookshelfBlock;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModMansion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.BarrelBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePiecesBuilder;
import net.minecraft.world.level.storage.loot.LootTable;

/**
 * MANSION: the mansion's secret room (and its corridor guards), built straight from code in the mansion's own frame.
 *
 * <p>Coordinates are local: {@code u} runs along the mansion's rotated east, {@code v} along its rotated south, {@code y}
 * up from the mansion's ground floor (its origin's height). The origin of the frame is the front-north corner corridor of
 * the ground floor - WoodlandMansionPieces always puts corridors at grid cells (8, 2) and (8, 3) beside the entrance hall,
 * with the mansion's outer walls on that cell's north (v = -1) and east (u = 7) sides; a cell's floor is u, v = 0..6.
 *
 * <ul>
 *   <li>In that corridor a tall bookcase stands one block off the north wall (v = 1). Three of its shelves are Loose
 *   Bookshelves (at heights 1, 3 and 5), and two by two of them, at its east end, are a Bookshelf Door.</li>
 *   <li>Behind it (v = 0) a landing with a lever (to let you out) and a stair go down west under the floor...</li>
 *   <li>...into the vault under the corridors: dark oak and deepslate, a library wall, soul lanterns, the chest that always
 *   keeps the Sift Symphony, two barrels of Sift loot, the illagers' practice summoner (a Sculk Catalyst ringed with Sculk
 *   Sensors), and a Hornblower and a Bard on guard.</li>
 *   <li>Two guard pieces put another Hornblower and Bard in the ground floor's fixed corridors (cells (8, 6) and (6, 3)).</li>
 * </ul>
 */
public class MansionVaultPiece extends StructurePiece {
    public static final int VAULT = 0;
    public static final int HORNBLOWER = 1;
    public static final int BARD = 2;

    private final int kind;
    private final BlockPos origin;
    private final Rotation rotation;

    private MansionVaultPiece(int kind, BlockPos origin, Rotation rotation, int u0, int y0, int v0, int u1, int y1, int v1) {
        super(ModMansion.MANSION_VAULT_PIECE.get(), 0, box(origin, rotation, u0, y0, v0, u1, y1, v1));
        this.kind = kind;
        this.origin = origin;
        this.rotation = rotation;
    }

    public MansionVaultPiece(CompoundTag tag) {
        super(ModMansion.MANSION_VAULT_PIECE.get(), tag);
        this.kind = tag.getIntOr("Kind", VAULT);
        this.origin = new BlockPos(tag.getIntOr("OX", 0), tag.getIntOr("OY", 0), tag.getIntOr("OZ", 0));
        this.rotation = Rotation.values()[Math.floorMod(tag.getIntOr("Rot", 0), Rotation.values().length)];
    }

    @Override
    protected void addAdditionalSaveData(StructurePieceSerializationContext context, CompoundTag tag) {
        tag.putInt("Kind", this.kind);
        tag.putInt("OX", this.origin.getX());
        tag.putInt("OY", this.origin.getY());
        tag.putInt("OZ", this.origin.getZ());
        tag.putInt("Rot", this.rotation.ordinal());
    }

    /**
     * The pieces for a mansion whose start (and floor) is {@code start} and rotation {@code rotation}: the vault from the
     * corridor cell (8, 2), and the two corridor guards.
     */
    public static void addPieces(StructurePiecesBuilder builder, BlockPos start, Rotation rotation) {
        BlockPos cell = start.relative(rotation.rotate(Direction.SOUTH), -16); // cell (8, 2): 8 + (2 - 5) * 8 blocks "south"
        builder.addPiece(vault(cell, rotation));
        builder.addPiece(new MansionVaultPiece(HORNBLOWER, cell, rotation, 3, 1, 35, 3, 2, 35)); // cell (8, 6)
        builder.addPiece(new MansionVaultPiece(BARD, cell, rotation, -13, 1, 11, -13, 2, 11)); // cell (6, 3)
    }

    /** The vault piece alone, its frame's origin at {@code cell} (the CI tests and the client smoke scene build one). */
    public static MansionVaultPiece vault(BlockPos cell, Rotation rotation) {
        return new MansionVaultPiece(VAULT, cell, rotation, -11, -7, -1, 7, 7, 9);
    }

    // ------------------------------------------------------------------ the local frame

    private static BlockPos at(BlockPos origin, Rotation rotation, int u, int y, int v) {
        Direction east = rotation.rotate(Direction.EAST);
        Direction south = rotation.rotate(Direction.SOUTH);
        return origin.offset(east.getStepX() * u + south.getStepX() * v, y, east.getStepZ() * u + south.getStepZ() * v);
    }

    private static BoundingBox box(BlockPos origin, Rotation rotation, int u0, int y0, int v0, int u1, int y1, int v1) {
        BlockPos a = at(origin, rotation, u0, y0, v0);
        BlockPos b = at(origin, rotation, u1, y1, v1);
        return BoundingBox.fromCorners(a, b);
    }

    /** World position of a local point. */
    public BlockPos at(int u, int y, int v) {
        return at(this.origin, this.rotation, u, y, v);
    }

    private void set(WorldGenLevel level, BoundingBox chunk, int u, int y, int v, BlockState state) {
        BlockPos p = this.at(u, y, v);
        if (chunk.isInside(p)) {
            level.setBlock(p, state.rotate(this.rotation), Block.UPDATE_CLIENTS);
        }
    }

    private void fill(WorldGenLevel level, BoundingBox chunk, int u0, int y0, int v0, int u1, int y1, int v1, BlockState state) {
        for (int u = Math.min(u0, u1); u <= Math.max(u0, u1); u++) {
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++) {
                for (int v = Math.min(v0, v1); v <= Math.max(v0, v1); v++) {
                    this.set(level, chunk, u, y, v, state);
                }
            }
        }
    }

    private void container(WorldGenLevel level, BoundingBox chunk, RandomSource random, int u, int y, int v, BlockState state,
            ResourceKey<LootTable> loot) {
        BlockPos p = this.at(u, y, v);
        if (!chunk.isInside(p)) {
            return;
        }
        level.setBlock(p, state.rotate(this.rotation), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(p) instanceof RandomizableContainerBlockEntity c) {
            c.setLootTable(loot, random.nextLong());
        }
    }

    private void spawn(WorldGenLevel level, BoundingBox chunk, EntityType<? extends Mob> type, int u, int y, int v) {
        BlockPos p = this.at(u, y, v);
        if (!chunk.isInside(p)) {
            return;
        }
        Mob mob = type.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
        if (mob == null) {
            return;
        }
        mob.setPersistenceRequired();
        mob.snapTo(p.getX() + 0.5, p.getY() + 0.05, p.getZ() + 0.5, this.rotation.rotate(Direction.SOUTH).toYRot(), 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), EntitySpawnReason.STRUCTURE, null);
        level.addFreshEntityWithPassengers(mob);
    }

    // ------------------------------------------------------------------ building

    @Override
    public void postProcess(WorldGenLevel level, StructureManager structureManager, ChunkGenerator generator, RandomSource random, BoundingBox chunk,
            ChunkPos chunkPos, BlockPos referencePos) {
        switch (this.kind) {
            case HORNBLOWER -> this.spawn(level, chunk, ModMansion.HORNBLOWER.get(), 0, 1, 0);
            case BARD -> this.spawn(level, chunk, ModMansion.BARD.get(), 0, 1, 0);
            default -> this.buildVault(level, chunk, random);
        }
    }

    /** Red carpet (26.3 dropped the per-colour Blocks.*_CARPET fields, so look it up by id). */
    private static BlockState redCarpet() {
        return net.minecraft.core.registries.BuiltInRegistries.BLOCK.getValue(net.minecraft.resources.Identifier.withDefaultNamespace("red_carpet")).defaultBlockState();
    }

    /** Builds the whole piece at once (CI tests, the client smoke scene). */
    public void placeWhole(ServerLevel level) {
        this.postProcess(level, level.structureManager(), level.getChunkSource().getGenerator(), level.getRandom(), this.getBoundingBox(),
                new ChunkPos(this.origin.getX() >> 4, this.origin.getZ() >> 4), this.origin);
    }

    private void buildVault(WorldGenLevel level, BoundingBox chunk, RandomSource random) {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState planks = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState log = Blocks.DARK_OAK_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        BlockState bricks = Blocks.DEEPSLATE_BRICKS.defaultBlockState();
        BlockState shelf = Blocks.BOOKSHELF.defaultBlockState();

        // ---- the vault (inside: u -10..-1, v 0..8, y -5..-2): deepslate brick walls, a dark oak ceiling, a tiled floor
        this.fill(level, chunk, -11, -6, -1, 0, -2, 9, bricks);
        this.fill(level, chunk, -11, -1, -1, 0, -1, 9, planks);
        this.fill(level, chunk, -10, -6, 0, -1, -6, 8, Blocks.DEEPSLATE_TILES.defaultBlockState());
        this.fill(level, chunk, -10, -5, 0, -1, -2, 8, air);
        for (int[] c : new int[][]{{-10, 0}, {-10, 8}, {-1, 8}}) {
            this.fill(level, chunk, c[0], -5, c[1], c[0], -2, c[1], log);
        }
        // a dark oak floor down the middle and a red rug before the chest
        this.fill(level, chunk, -9, -6, 1, -2, -6, 7, planks);
        this.fill(level, chunk, -7, -5, 1, -5, -5, 2, redCarpet());
        // the library wall (north) and the chest that keeps the Sift Symphony
        this.fill(level, chunk, -9, -5, 0, -2, -3, 0, shelf);
        this.container(level, chunk, random, -6, -5, 0, Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.SOUTH),
                ModMansion.VAULT_CHEST);
        // two barrels of Sift loot against the west wall
        BlockState barrel = Blocks.BARREL.defaultBlockState().setValue(BarrelBlock.FACING, Direction.EAST);
        this.container(level, chunk, random, -10, -5, 3, barrel, ModMansion.VAULT_BARREL);
        this.container(level, chunk, random, -10, -5, 5, barrel, ModMansion.VAULT_BARREL);
        // the illagers' practice summoner: a Sculk Catalyst ringed by four Sculk Sensors, on a creeping patch of sculk
        this.fill(level, chunk, -7, -6, 4, -5, -6, 6, Blocks.SCULK.defaultBlockState());
        this.set(level, chunk, -6, -5, 5, Blocks.SCULK_CATALYST.defaultBlockState());
        for (int[] c : new int[][]{{-8, 5}, {-4, 5}, {-6, 3}, {-6, 7}}) {
            this.set(level, chunk, c[0], -5, c[1], Blocks.SCULK_SENSOR.defaultBlockState());
        }
        // soul lanterns hanging from the ceiling
        BlockState lantern = Blocks.SOUL_LANTERN.defaultBlockState().setValue(LanternBlock.HANGING, true);
        for (int[] c : new int[][]{{-3, 3}, {-8, 3}, {-3, 7}, {-8, 7}}) {
            this.set(level, chunk, c[0], -2, c[1], lantern);
        }

        // ---- the stair: its shell under the corridor (u 0..7, v -1..1, below the floor) and the hidden column behind the
        // bookcase (v 0, above the floor) are solid; the landing and the stair down to the vault are carved out of them
        this.fill(level, chunk, 0, -7, -1, 7, -1, 1, bricks);
        this.fill(level, chunk, 0, 1, 0, 6, 7, 0, planks);
        this.fill(level, chunk, 5, 0, 0, 6, 0, 0, planks); // the landing's floor
        this.fill(level, chunk, 5, 1, 0, 6, 3, 0, air); // the landing
        for (int k = 1; k <= 5; k++) {
            int u = 5 - k;
            this.set(level, chunk, u, -k, 0, Blocks.DARK_OAK_STAIRS.defaultBlockState().setValue(StairBlock.FACING, Direction.EAST));
            this.fill(level, chunk, u, -k + 1, 0, u, -k + 3, 0, air);
        }
        // the lever on the landing's floor, beside the door: it lets you out (and holds the door open while on)
        this.set(level, chunk, 6, 1, 0, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, AttachFace.FLOOR)
                .setValue(LeverBlock.FACING, Direction.SOUTH));

        // ---- the bookcase (v 1): a dark oak post, shelves to the ceiling, the door at its east end, three loose books
        this.fill(level, chunk, 0, 1, 1, 0, 7, 1, log);
        this.fill(level, chunk, 1, 1, 1, 6, 6, 1, shelf);
        this.fill(level, chunk, 1, 7, 1, 6, 7, 1, planks);
        this.fill(level, chunk, 5, 1, 1, 6, 2, 1, ModBlocks.SECRET_BOOKSHELF.get().defaultBlockState());
        BlockState loose = ModBlocks.LOOSE_BOOKSHELF.get().defaultBlockState().setValue(LooseBookshelfBlock.FACING, Direction.SOUTH);
        this.set(level, chunk, 3, 1, 1, loose); // pulled first: the lowest
        this.set(level, chunk, 1, 3, 1, loose);
        this.set(level, chunk, 4, 5, 1, loose); // and the highest last
        // a reading rug in front of it
        this.fill(level, chunk, 1, 1, 2, 4, 1, 3, redCarpet());

        // ---- its guards
        this.spawn(level, chunk, ModMansion.HORNBLOWER.get(), -3, -5, 6);
        this.spawn(level, chunk, ModMansion.BARD.get(), -9, -5, 7);
    }

    /** Local positions the tests check: the vault chest, the door, the loose books (lowest first) and the lever. */
    public BlockPos chestPos() {
        return this.at(-6, -5, 0);
    }

    public BlockPos doorPos() {
        return this.at(6, 1, 1);
    }

    public BlockPos[] loosePositions() {
        return new BlockPos[]{this.at(3, 1, 1), this.at(1, 3, 1), this.at(4, 5, 1)};
    }

    public BlockPos landingPos() {
        return this.at(5, 1, 0);
    }
}
