package com.thesift.dev;

import com.mojang.datafixers.util.Pair;
import com.thesift.TheSift;
import com.thesift.portal.PortalFrames;
import com.thesift.portal.SiftTeleporter;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModDimensions;
import com.thesift.registry.ModEntities;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.function.Supplier;
import javax.imageio.ImageIO;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.LootTable;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * Headless end-to-end check used by CI (only active with -Dthesift.smoketest=true). Generates a
 * patch of The Sift, renders top-down and side-on terrain maps, spawns every mob and lets them
 * run their AI for a while, builds portals, validates loot tables, then stops the server.
 */
public final class SmokeTest {
    private static final int RADIUS = 9;
    private static final List<Entity> SPAWNED = new ArrayList<>();
    private static final List<String> FAILURES = new ArrayList<>();
    private static int ticks = -1;
    private static @Nullable MechanicsTest mechanics;

    private SmokeTest() {
    }

    public static void registerIfEnabled() {
        if (Boolean.getBoolean("thesift.smoketest")) {
            TheSift.LOGGER.info("SMOKE: enabled");
            NeoForge.EVENT_BUS.addListener(SmokeTest::onStarted);
            NeoForge.EVENT_BUS.addListener(SmokeTest::onTick);
        }
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            FAILURES.add(what);
            TheSift.LOGGER.error("SMOKE FAIL: {}", what);
        }
    }

    private static void onStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        try {
            ServerLevel sift = server.getLevel(ModDimensions.THE_SIFT);
            check(sift != null, "the_sift dimension exists");
            if (sift == null) {
                return;
            }
            long t0 = System.currentTimeMillis();
            for (int cx = -RADIUS; cx <= RADIUS; cx++) {
                for (int cz = -RADIUS; cz <= RADIUS; cz++) {
                    sift.getChunk(cx, cz);
                }
            }
            TheSift.LOGGER.info("SMOKE: generated {} chunks in {} ms", (2 * RADIUS + 1) * (2 * RADIUS + 1), System.currentTimeMillis() - t0);
            maps(sift);
            structures(sift);
            validateLoot(server);
            portals(server, sift);
            spawnMobs(sift);
            mechanics = new MechanicsTest(server, sift, SmokeTest::check);
            mechanics.start();
            ticks = 0;
        } catch (Throwable t) {
            TheSift.LOGGER.error("SMOKE FAIL: exception", t);
            FAILURES.add("exception " + t);
            ticks = 3001;
        }
    }

    private static void onTick(ServerTickEvent.Post event) {
        if (ticks < 0) {
            return;
        }
        ticks++;
        if (mechanics != null && !mechanics.done()) {
            mechanics.tick();
        }
        boolean finished = ticks >= 200 && (mechanics == null || mechanics.done());
        if (finished || ticks > 3000) {
            for (Entity e : SPAWNED) {
                TheSift.LOGGER.info("SMOKE: {} alive={} removal={} pos={} health={}", BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()), e.isAlive(),
                        e.getRemovalReason(), e.blockPosition(), e instanceof LivingEntity living ? living.getHealth() + "/" + living.getMaxHealth() : "-");
            }
            if (FAILURES.isEmpty()) {
                TheSift.LOGGER.info("SMOKE TEST PASSED");
            } else {
                TheSift.LOGGER.error("SMOKE TEST FAILED: {}", FAILURES);
            }
            ticks = -1;
            event.getServer().halt(false);
        }
    }

    // ------------------------------------------------------------------ maps

    private static void maps(ServerLevel level) throws IOException {
        int size = (2 * RADIUS + 1) * 16;
        int min = -RADIUS * 16;
        BufferedImage top = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        BufferedImage biomes = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Map<String, Integer> biomeCount = new TreeMap<>();
        Map<String, Integer> blockCount = new TreeMap<>();
        Map<String, Integer> biomeColors = new HashMap<>();
        int[] palette = {0x6fe2dc, 0xf59ac6, 0xe8a9c6, 0x7fe8ff, 0x3a586c, 0xb3c0ee, 0xffd66b, 0x8a84ad};
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = 0; x < size; x++) {
            for (int z = 0; z < size; z++) {
                int wx = min + x;
                int wz = min + z;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, wx, wz) - 1;
                p.set(wx, y, wz);
                BlockState st = level.getBlockState(p);
                int col = st.getMapColor(level, p).col;
                float shade = 0.55F + 0.45F * Math.min(1.0F, Math.max(0.0F, (y - 40) / 140.0F));
                top.setRGB(x, z, scale(col, shade));
                String bid = BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString();
                blockCount.merge(bid, 1, Integer::sum);
                String biome = level.getBiome(p).getRegisteredName();
                biomeCount.merge(biome, 1, Integer::sum);
                int bc = biomeColors.computeIfAbsent(biome, k -> palette[biomeColors.size() % palette.length]);
                biomes.setRGB(x, z, bc);
            }
        }
        // side-on slice through z = 0 showing caves, floating islands and the Deep Sift
        int height = level.getHeight();
        BufferedImage side = new BufferedImage(size, height, BufferedImage.TYPE_INT_RGB);
        Map<String, Integer> sliceCount = new TreeMap<>();
        for (int x = 0; x < size; x++) {
            for (int yy = 0; yy < height; yy++) {
                int wy = level.getMinY() + yy;
                p.set(min + x, wy, 0);
                BlockState st = level.getBlockState(p);
                int col = st.isAir() ? 0x151028 : st.getMapColor(level, p).col;
                if (!st.getFluidState().isEmpty()) {
                    col = 0x9ff5ff;
                }
                side.setRGB(x, height - 1 - yy, col);
                if (!st.isAir()) {
                    sliceCount.merge(BuiltInRegistries.BLOCK.getKey(st.getBlock()).toString(), 1, Integer::sum);
                }
            }
        }
        TheSift.LOGGER.info("SMOKE: biomes {}", biomeCount);
        TheSift.LOGGER.info("SMOKE: surface blocks {}", top(blockCount, 40));
        TheSift.LOGGER.info("SMOKE: slice blocks {}", top(sliceCount, 40));
        TheSift.LOGGER.info("SMOKE: biome colours {}", biomeColors);
        check(biomeCount.size() >= 2, "more than one biome generated");
        emit("top", top);
        emit("biomes", biomes);
        emit("side", side);
    }

    private static String top(Map<String, Integer> counts, int n) {
        StringBuilder sb = new StringBuilder();
        counts.entrySet().stream().sorted((a, b) -> b.getValue() - a.getValue()).limit(n)
                .forEach(e -> sb.append(e.getKey()).append('=').append(e.getValue()).append(' '));
        return sb.toString();
    }

    private static int scale(int rgb, float f) {
        int r = Math.min(255, (int) (((rgb >> 16) & 255) * f));
        int g = Math.min(255, (int) (((rgb >> 8) & 255) * f));
        int b = Math.min(255, (int) ((rgb & 255) * f));
        return (r << 16) | (g << 8) | b;
    }

    private static void emit(String name, BufferedImage img) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        String b64 = Base64.getEncoder().encodeToString(out.toByteArray());
        for (int i = 0; i < b64.length(); i += 3000) {
            TheSift.LOGGER.info("SMOKEPNG {} {}", name, b64.substring(i, Math.min(b64.length(), i + 3000)));
        }
    }

    // ------------------------------------------------------------------ structures

    private static final String[] STRUCTURES = {"collapsed_tower", "musical_temple", "chrome_well", "abandoned_altar", "stone_instrument",
            "ruined_bridge", "buried_settlement", "dream_statue", "deep_shrine", "sift_ruins", "sculk_castle", "thumper_arena",
            "caravan_colony", // C: the Caravan colony
            "echoer_hut"}; // A2 Echoer: the Echoer's Hut

    private static void structures(ServerLevel sift) {
        // every template parses and can be stamped into the world
        StructureTemplateManager templates = sift.getStructureTemplateManager();
        int x = -240;
        for (String name : STRUCTURES) {
            for (int i = 0; i < 4; i++) {
                Identifier id = TheSift.id(name + "/" + name + "_" + i);
                Optional<StructureTemplate> template = templates.get(id);
                if (template.isEmpty()) {
                    check(i > 0, "template " + id);
                    break;
                }
                Vec3i size = template.get().getSize();
                check(size.getX() > 0 && size.getY() > 0, "template size " + id);
                BlockPos at = new BlockPos(x, 140, -260);
                boolean ok = template.get().placeInWorld(sift, at, at, new StructurePlaceSettings(), sift.getRandom(), Block.UPDATE_CLIENTS);
                int inTemplate = 0;
                for (Block b : new Block[] {ModBlocks.POLISHED_DREAMSTONE.get(), ModBlocks.BLUSH_BRICKS.get(), ModBlocks.HUSHSLATE_BRICKS.get(),
                        ModBlocks.DREAMSTONE_BRICKS.get()}) {
                    inTemplate += template.get().filterBlocks(BlockPos.ZERO, new StructurePlaceSettings(), b).size();
                }
                int solid = 0;
                for (BlockPos q : BlockPos.betweenClosed(at, at.offset(size).offset(-1, -1, -1))) {
                    if (!sift.getBlockState(q).isAir()) solid++;
                }
                TheSift.LOGGER.info("SMOKE: placed {} size {} at {} ok={} signature blocks in template {} non-air in world {}", id, size, at, ok,
                        inTemplate, solid);
                check(solid > 0, "template actually placed " + id);
                x += size.getX() + 4;
            }
        }
        // every structure can be found by the chunk generator
        Registry<Structure> registry = sift.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        for (String name : STRUCTURES) {
            Optional<Holder.Reference<Structure>> holder = registry.get(ResourceKey.create(Registries.STRUCTURE, TheSift.id(name)));
            check(holder.isPresent(), "structure registered " + name);
            if (holder.isEmpty()) {
                continue;
            }
            Pair<BlockPos, Holder<Structure>> found = sift.getChunkSource().getGenerator()
                    .findNearestMapStructure(sift, HolderSet.direct(holder.get()), BlockPos.ZERO, 40, false);
            TheSift.LOGGER.info("SMOKE: nearest {} = {}", name, found == null ? "none" : found.getFirst());
            check(found != null, "structure locatable " + name);
            if (found != null && found.getFirst().distManhattan(BlockPos.ZERO) < 260) {
                // what actually got built: the start's box and the blocks inside it
                BlockPos at = found.getFirst();
                StructureStart start = sift.getChunk(at.getX() >> 4, at.getZ() >> 4).getStartForStructure(holder.get().value());
                if (start != null && start.isValid()) {
                    BoundingBox box = start.getBoundingBox();
                    Map<String, Integer> mine = new TreeMap<>();
                    for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
                        Identifier id = BuiltInRegistries.BLOCK.getKey(sift.getBlockState(p).getBlock());
                        if (id.getNamespace().equals(TheSift.MODID) || id.getPath().contains("sculk") || id.getPath().equals("chest")) {
                            mine.merge(id.getPath(), 1, Integer::sum);
                        }
                    }
                    TheSift.LOGGER.info("SMOKE: built {} box {} pieces {} blocks {}", name, box, start.getPieces().size(), mine);
                    if (name.equals("chrome_well")) {
                        // experiment: is the piece placeable at all? place it by hand over its whole box and recount
                        var piece = start.getPieces().get(0);
                        var chunk = sift.getChunk(at.getX() >> 4, at.getZ() >> 4);
                        TheSift.LOGGER.info("SMOKE: chrome_well piece {} box {} chunk status {} refs {}", piece.getClass().getSimpleName(),
                                piece.getBoundingBox(), chunk.getPersistedStatus(), chunk.getReferencesForStructure(holder.get().value()));
                        BoundingBox pb = piece.getBoundingBox();
                        for (int cx = pb.minX() >> 4; cx <= pb.maxX() >> 4; cx++) {
                            for (int cz = pb.minZ() >> 4; cz <= pb.maxZ() >> 4; cz++) {
                                BoundingBox cb = new BoundingBox(cx << 4, sift.getMinY(), cz << 4, (cx << 4) + 15, sift.getMaxY(), (cz << 4) + 15);
                                start.placeInChunk(sift, sift.structureManager(), sift.getChunkSource().getGenerator(), sift.getRandom(), cb,
                                        new net.minecraft.world.level.ChunkPos(cx, cz));
                            }
                        }
                        int polished = 0;
                        for (BlockPos p : BlockPos.betweenClosed(pb.minX(), pb.minY(), pb.minZ(), pb.maxX(), pb.maxY(), pb.maxZ())) {
                            if (sift.getBlockState(p).is(ModBlocks.POLISHED_DREAMSTONE.get())) polished++;
                        }
                        TheSift.LOGGER.info("SMOKE: chrome_well after manual placement: polished dreamstone {}", polished);
                    }
                    if (name.equals("deep_shrine")) {
                        check(mine.getOrDefault("sift_gate_frame", 0) > 0, "natural deep shrine keeps its gate frame keystones");
                    }
                }
            }
        }
        Map<String, Integer> starts = new TreeMap<>();
        for (int cx = -RADIUS; cx <= RADIUS; cx++) {
            for (int cz = -RADIUS; cz <= RADIUS; cz++) {
                for (Structure st : sift.getChunk(cx, cz).getAllStarts().keySet()) {
                    starts.merge(String.valueOf(registry.getKey(st)), 1, Integer::sum);
                }
            }
        }
        TheSift.LOGGER.info("SMOKE: structure starts near spawn {}", starts);
    }

    // ------------------------------------------------------------------ data

    private static void validateLoot(MinecraftServer server) {
        int checked = 0;
        for (Supplier<? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            Block block = holder.get();
            if (block.getLootTable().isEmpty()) {
                continue;
            }
            LootTable table = server.reloadableRegistries().getLootTable(block.getLootTable().get());
            String id = BuiltInRegistries.BLOCK.getKey(block).toString();
            boolean exempt = id.endsWith("portal") || id.endsWith("harmony_stone") || id.endsWith("harmony_seal") || id.endsWith("lingering_glow")
                    || id.endsWith(":chrome") || id.endsWith("suspicious_dreamsand") || id.endsWith("crumbling_dreamstone") || id.endsWith("sift_cake")
                    || id.endsWith("conductors_podium") || id.endsWith("encore_sigil") || id.endsWith("instrument_altar");
            if (!exempt) {
                check(table != LootTable.EMPTY, "loot table for " + id);
            }
            checked++;
        }
        TheSift.LOGGER.info("SMOKE: checked {} block loot tables", checked);
    }

    // ------------------------------------------------------------------ portals

    private static void portals(MinecraftServer server, ServerLevel sift) {
        // a Sift Gate Frame portal built by hand in The Sift, filled via the same code the drum ritual uses
        BlockPos base = new BlockPos(0, sift.getHeight(Heightmap.Types.WORLD_SURFACE, 0, 0) + 1, 0);
        for (int dx = -1; dx <= 2; dx++) {
            for (int dy = 0; dy <= 4; dy++) {
                boolean edge = dx == -1 || dx == 2 || dy == 0 || dy == 4;
                sift.setBlockAndUpdate(base.offset(dx, dy, 0), edge ? ModBlocks.SIFT_GATE_FRAME.get().defaultBlockState() : Blocks.AIR.defaultBlockState());
            }
        }
        PortalFrames.Frame frame = PortalFrames.find(sift, base.above(), 12);
        check(frame != null, "portal frame found");
        if (frame != null) {
            PortalFrames.fill(sift, frame);
            check(sift.getBlockState(base.offset(0, 1, 0)).is(ModBlocks.SIFT_PORTAL.get()), "portal filled");
        }
        // The real Ancient City gate: stamp a vanilla city centre high in the sky and light it like the drum ritual does.
        Optional<StructureTemplate> city = sift.getStructureTemplateManager().get(Identifier.withDefaultNamespace("ancient_city/city_center/city_center_1"));
        check(city.isPresent(), "ancient city centre template");
        if (city.isPresent()) {
            BlockPos origin = new BlockPos(400, 250, 400);
            city.get().placeInWorld(sift, origin, origin, new StructurePlaceSettings(), sift.getRandom(), Block.UPDATE_CLIENTS);
            Vec3i size = city.get().getSize();
            long sx = 0;
            long sy = 0;
            long sz = 0;
            int n = 0;
            for (BlockPos q : BlockPos.betweenClosed(origin, origin.offset(size).offset(-1, -1, -1))) {
                if (sift.getBlockState(q).is(Blocks.REINFORCED_DEEPSLATE)) {
                    sx += q.getX();
                    sy += q.getY();
                    sz += q.getZ();
                    n++;
                }
            }
            check(n > 0, "ancient city centre has reinforced deepslate");
            if (n > 0) {
                BlockPos centre = new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n));
                dumpSlice(sift, origin, size, centre.getX() - origin.getX());
                PortalFrames.Frame gate = PortalFrames.find(sift, centre, 20);
                TheSift.LOGGER.info("SMOKE: ancient city size={} frame blocks={} centre={} gate={}", size, n, centre,
                        gate == null ? "none" : gate.axis() + " cells=" + gate.interior().size() + " at " + gate.center());
                check(gate != null, "ancient city gate recognised as a portal frame");
                if (gate != null) {
                    PortalFrames.fill(sift, gate);
                }
            }
        }
        ServerLevel overworld = server.overworld();
        BlockPos arrival = SiftTeleporter.buildArrivalPortal(overworld, new BlockPos(64, 80, 64), Direction.Axis.X);
        TheSift.LOGGER.info("SMOKE: overworld arrival portal at {}", arrival);
        check(overworld.getBlockState(arrival).is(ModBlocks.SIFT_PORTAL.get()) || overworld.getBlockState(arrival.above()).is(ModBlocks.SIFT_PORTAL.get()),
                "arrival portal built");
    }

    /** Logs one YZ slice of a placed template: # frame, o solid, . open. */
    private static void dumpSlice(ServerLevel level, BlockPos origin, Vec3i size, int x) {
        for (int y = size.getY() - 1; y >= 0; y--) {
            StringBuilder row = new StringBuilder();
            for (int z = 0; z < size.getZ(); z++) {
                BlockState s = level.getBlockState(origin.offset(x, y, z));
                row.append(PortalFrames.isFrame(s) ? '#' : PortalFrames.isFillable(s) ? '.' : 'o');
            }
            TheSift.LOGGER.info("SMOKE: gate x={} y={} {}", x, y, row);
        }
    }

    // ------------------------------------------------------------------ mobs

    private static void spawnMobs(ServerLevel sift) {
        List<EntityType<?>> types = List.of(ModEntities.BULB.get(), ModEntities.SLUMBLER.get(), ModEntities.SIFTER.get(), ModEntities.ENCHOER.get(),
                ModEntities.RIVETER.get(), EntityTypes.SNIFFER, ModEntities.HARMONER.get(), ModEntities.THUMPER.get(),
                ModEntities.STRUMMER.get(), ModEntities.SCULK_PARASITE.get(), ModEntities.STRUMLING.get(),
                com.thesift.registry.ModCaravans.CARAVAN.get(), // C: the Caravan
                com.thesift.registry.ModEchoer.SOUL_GOLEM.get(), com.thesift.registry.ModEchoer.NIB.get()); // A2 Echoer
        // no player is in The Sift, so force the test chunks to stay loaded and entity-ticking
        sift.setChunkForced(0, 0, true);
        sift.setChunkForced(1, 0, true);
        int i = 0;
        for (EntityType<?> type : types) {
            int x = 8 + i * 4;
            int z = 8;
            int y = sift.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            Entity e = type.create(sift, EntitySpawnReason.COMMAND);
            check(e != null, "create " + type);
            if (e == null) {
                continue;
            }
            e.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
            if (e instanceof Mob mob) {
                mob.finalizeSpawn(sift, sift.getCurrentDifficultyAt(e.blockPosition()), EntitySpawnReason.COMMAND, null);
            }
            check(sift.addFreshEntity(e), "add " + type);
            SPAWNED.add(e);
            i++;
        }
    }
}
