package com.thesift.dev;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thesift.TheSift;
import com.thesift.entity.Resting;
import com.thesift.entity.SiftFish;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModCaveCreatures;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModSculkSea;
import com.thesift.registry.ModSeaSky;
import com.thesift.registry.ModSwifter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.Weighted;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

/**
 * S1 spawning &amp; AI checks for the CI smoke test (driven by {@link SmokeTest}).
 * <ol>
 *   <li><b>Spawn rules</b>: every Sift creature a Sift biome lists (its own spawn list plus the biome
 *   modifiers) must pass the checks the natural spawner makes - placement, spawn predicate, room to
 *   stand, the mob's own spawn rules (light / walk target) and obstruction - on that biome's ground
 *   under the open sky, in a pitch-dark cave room (cave dwellers and cave biomes) or in a tank of the
 *   biome's liquid (water creatures). A creature that fails here only ever appears at chunk generation.</li>
 *   <li><b>Never frozen</b>: every wild creature is set down in its own pen (the fish in a Chrome pool),
 *   far from any player, and must keep ticking and moving for a few seconds.</li>
 * </ol>
 * New biomes: add their ground to {@link #habitat}, or they are tested on Sift grass.
 */
final class CreatureCheck {
    /** Ground under the open sky, floor of its caves, whether its waters are water (else Chrome), whether it is a cave biome. */
    private record Habitat(Block ground, Block caveFloor, boolean water, boolean cave) {
    }

    /** One creature in its pen: where it started and the furthest it has been from there. */
    private static final class Watch {
        final String name;
        final Mob mob;
        final Vec3 start;
        final BlockPos centre;
        final int startTick;
        double moved;
        double movedAlone;
        /** Every block it has travelled, back and forth included. */
        double path;
        Vec3 last;

        Watch(String name, Mob mob, BlockPos centre) {
            this.name = name;
            this.mob = mob;
            this.start = mob.position();
            this.last = this.start;
            this.centre = centre;
            this.startTick = mob.tickCount;
        }
    }

    /** The spawn lab: chunk (7, -8), inside the smoke test's generated patch, well away from every Jailer. */
    private static final int LAB_X = 112;
    private static final int LAB_Z = -128;
    private static final int SKY_Y = 300;
    private static final int CAVE_Y = -44;
    private static final int TANK_Y = -43;
    /** Spawn rules are checked once the lab's light has settled; the pens are watched this long. */
    private static final int RULES_AT = 20;
    private static final int WATCH_TICKS = 180;
    private static final int ALONE_TICKS = 100;

    private final ServerLevel level;
    private final BiConsumer<Boolean, String> check;
    private final List<Watch> watches = new ArrayList<>();
    private int ticks;
    private boolean done;

    CreatureCheck(ServerLevel level, BiConsumer<Boolean, String> check) {
        this.level = level;
        this.check = check;
    }

    boolean done() {
        return this.done;
    }

    void start() {
        this.level.setChunkForced(LAB_X >> 4, LAB_Z >> 4, true);
        this.buildLab();
        this.spawnPens();
    }

    void tick() {
        if (this.done) {
            return;
        }
        this.ticks++;
        if (this.ticks == RULES_AT) {
            try {
                this.spawnRules();
            } catch (RuntimeException e) {
                TheSift.LOGGER.error("SMOKE: spawn rule check crashed", e);
                this.check.accept(false, "spawn rules: exception " + e);
            }
        }
        RandomSource random = this.level.getRandom();
        for (Watch w : this.watches) {
            if (!w.mob.isAlive()) {
                continue;
            }
            double d = w.mob.position().distanceTo(w.start);
            w.moved = Math.max(w.moved, d);
            w.path += w.mob.position().distanceTo(w.last);
            w.last = w.mob.position();
            if (this.ticks <= ALONE_TICKS) {
                w.movedAlone = w.moved;
            } else if (this.ticks % 20 == 0) {
                this.walk(w, random);
            }
        }
        if (this.ticks >= WATCH_TICKS) {
            for (Watch w : this.watches) {
                // CR2: a creature that has lain down for a nap is resting, not frozen (it wakes and goes on with its life)
                boolean resting = w.mob instanceof Resting r && r.isResting();
                TheSift.LOGGER.info("SMOKE: never frozen: {} alive={} moved {} blocks, travelled {} ({} on its own in the first {} ticks), ticked {} times{}",
                        w.name, w.mob.isAlive(), String.format(Locale.ROOT, "%.1f", w.moved), String.format(Locale.ROOT, "%.1f", w.path), String.format(Locale.ROOT, "%.1f", w.movedAlone),
                        ALONE_TICKS, w.mob.tickCount - w.startTick, resting ? ", resting" : "");
                this.check.accept(w.mob.isAlive(), "never frozen: " + w.name + " died in its pen");
                this.check.accept(w.moved >= 1.5 || w.path >= 3.0 || resting, "never frozen: " + w.name + " stood still for " + WATCH_TICKS + " ticks, even when walked");
                this.check.accept(w.mob.tickCount - w.startTick >= WATCH_TICKS / 2, "never frozen: " + w.name + " stopped ticking");
            }
            this.done = true;
        }
    }

    // ------------------------------------------------------------------ spawn rules

    private static Habitat habitat(String biome) {
        Block hush = ModBlocks.HUSHSLATE.get();
        return switch (biome) {
            case "sift_plains" -> new Habitat(ModBlocks.CORAL_TURF.get(), hush, false, false);
            case "rocky_dunes" -> new Habitat(ModBlocks.DREAMSAND.get(), hush, false, false);
            case "white_forest" -> new Habitat(ModBlocks.WHITE_TURF.get(), hush, false, false);
            case "sky_island" -> new Habitat(ModBlocks.SKY_GRASS_BLOCK.get(), hush, false, false); // W-sky (was the Sound Garden)
            case "magic_kelp_forest" -> new Habitat(ModBlocks.CORAL_SAND.get(), hush, true, false);
            case "deep_dark_ocean" -> new Habitat(hush, hush, true, false);
            case "deep_sift" -> new Habitat(ModBlocks.LUMEN_MOSS_BLOCK.get(), hush, false, true);
            case "caravans_cavern" -> new Habitat(Blocks.CALCITE, Blocks.CALCITE, false, true);
            case "sculk_swamp" -> new Habitat(ModBlocks.SCULK_MUD.get(), hush, false, false);
            // W-deep caves: the cave biomes (their creatures are tested in the pitch-dark room)
            case "sift_caves" -> new Habitat(ModBlocks.DREAMSTONE.get(), hush, false, true);
            case "cave_jungle" -> new Habitat(ModBlocks.LUMEN_MOSS_BLOCK.get(), hush, false, true); // (its glowing moss is lit: tested on dark rock)
            case "sculk_caves" -> new Habitat(Blocks.SCULK, Blocks.SCULK, false, true);
            // W-sea: the reef is real water over Copper Sand; the Chrome Coral Ocean is Chrome over Chime Sand
            case "brass_coral_reef" -> new Habitat(ModBlocks.COPPER_SAND.get(), hush, true, false);
            case "chrome_coral_ocean" -> new Habitat(ModBlocks.CHIME_SAND.get(), hush, false, false);
            // forest_mountains, chrome_lakes, wishing_grove (and any biome added later)
            default -> new Habitat(ModBlocks.SIFT_GRASS_BLOCK.get(), hush, false, false);
        };
    }

    private BlockPos skySpot() {
        return new BlockPos(LAB_X + 4, SKY_Y, LAB_Z + 4);
    }

    private BlockPos caveSpot() {
        return new BlockPos(LAB_X + 11, CAVE_Y, LAB_Z + 4);
    }

    private BlockPos tankSpot() {
        return new BlockPos(LAB_X + 4, TANK_Y, LAB_Z + 12);
    }

    private void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
        for (BlockPos p : BlockPos.betweenClosed(x0, y0, z0, x1, y1, z1)) {
            this.level.setBlock(p, state, Block.UPDATE_CLIENTS);
        }
    }

    /** A pad under the open sky, a sealed pitch-dark room deep in the rock and a sealed tank beside it. */
    private void buildLab() {
        BlockState hush = ModBlocks.HUSHSLATE.get().defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        fill(LAB_X + 1, SKY_Y - 2, LAB_Z + 1, LAB_X + 7, SKY_Y - 2, LAB_Z + 7, ModBlocks.DREAMSTONE.get().defaultBlockState());
        fill(LAB_X + 1, SKY_Y, LAB_Z + 1, LAB_X + 7, SKY_Y + 6, LAB_Z + 7, air);
        fill(LAB_X + 8, CAVE_Y - 2, LAB_Z + 1, LAB_X + 15, CAVE_Y + 6, LAB_Z + 8, hush);
        fill(LAB_X + 9, CAVE_Y, LAB_Z + 2, LAB_X + 14, CAVE_Y + 5, LAB_Z + 7, air);
        fill(LAB_X + 1, TANK_Y - 3, LAB_Z + 9, LAB_X + 7, TANK_Y + 5, LAB_Z + 15, hush);
    }

    /** Lays the biome's ground on the pad and the room floor, and fills the tank with its liquid. */
    private void dress(Habitat h) {
        fill(LAB_X + 1, SKY_Y - 1, LAB_Z + 1, LAB_X + 7, SKY_Y - 1, LAB_Z + 7, h.ground().defaultBlockState());
        fill(LAB_X + 9, CAVE_Y - 1, LAB_Z + 2, LAB_X + 14, CAVE_Y - 1, LAB_Z + 7, h.caveFloor().defaultBlockState());
        BlockState liquid = h.water() ? Blocks.WATER.defaultBlockState() : sculkWater(h) ? ModBlocks.SCULK_WATER.get().defaultBlockState()
                : ModBlocks.CHROME.get().defaultBlockState();
        fill(LAB_X + 2, TANK_Y - 2, LAB_Z + 10, LAB_X + 6, TANK_Y + 4, LAB_Z + 14, liquid);
    }

    /** CR3: the Sculk Swamp's pools are Sculk Water (its fish are tested in it). */
    private static boolean sculkWater(Habitat h) {
        return h.ground() == ModBlocks.SCULK_MUD.get();
    }

    /** The spawn lists of a biome: its own (from its json) plus every biome modifier that adds spawns to it. */
    private Map<EntityType<?>, MobCategory> spawnsOf(Identifier biome, Holder<Biome> holder) {
        Map<EntityType<?>, MobCategory> out = new LinkedHashMap<>();
        Identifier file = Identifier.fromNamespaceAndPath(biome.getNamespace(), "worldgen/biome/" + biome.getPath() + ".json");
        Optional<Resource> resource = this.level.getServer().getResourceManager().getResource(file);
        if (resource.isPresent()) {
            try (Reader reader = resource.get().openAsReader()) {
                JsonObject byCategory = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("attributes")
                        .getAsJsonObject("minecraft:gameplay/natural_mob_spawns").getAsJsonObject("argument").getAsJsonObject("spawns_by_category");
                for (Map.Entry<String, JsonElement> category : byCategory.entrySet()) {
                    MobCategory cat = MobCategory.valueOf(category.getKey().toUpperCase(Locale.ROOT));
                    for (JsonElement entry : category.getValue().getAsJsonArray()) {
                        Identifier id = Identifier.parse(entry.getAsJsonObject().get("type").getAsString());
                        BuiltInRegistries.ENTITY_TYPE.getOptional(id).ifPresent(type -> out.put(type, cat));
                    }
                }
            } catch (Exception e) {
                this.check.accept(false, "spawn rules: could not read the spawn list of " + biome + ": " + e);
            }
        }
        Registry<BiomeModifier> modifiers = this.level.registryAccess().lookupOrThrow(NeoForgeRegistries.Keys.BIOME_MODIFIERS);
        for (BiomeModifier modifier : modifiers) {
            if (modifier instanceof BiomeModifiers.AddSpawnsBiomeModifier add && add.biomes().contains(holder)) {
                for (Weighted<MobSpawnSettings.SpawnerData> spawner : add.spawners().unwrap()) {
                    out.put(spawner.value().type(), spawner.value().type().getCategory());
                }
            }
        }
        return out;
    }

    /** Ours, plus the vanilla creatures whose Sift spawn rules we set. */
    private static boolean tested(EntityType<?> type) {
        return BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(TheSift.MODID) || type == EntityTypes.SNIFFER || type == EntityTypes.ALLAY;
    }

    private static boolean caveDweller(EntityType<?> type) {
        return type == ModCaveCreatures.JAILER.get() || type == ModCaveCreatures.SCULKLING.get();
    }

    private void spawnRules() {
        Registry<Biome> biomes = this.level.registryAccess().lookupOrThrow(Registries.BIOME);
        List<Identifier> ids = new ArrayList<>();
        for (Biome biome : biomes) {
            Identifier key = biomes.getKey(biome);
            if (key != null && key.getNamespace().equals(TheSift.MODID)) {
                ids.add(key);
            }
        }
        ids.sort(Comparator.comparing(Identifier::toString));
        boolean peaceful = this.level.getDifficulty() == Difficulty.PEACEFUL;
        int tests = 0;
        for (Identifier id : ids) {
            Optional<Holder.Reference<Biome>> holder = biomes.get(ResourceKey.create(Registries.BIOME, id));
            if (holder.isEmpty()) {
                continue;
            }
            Habitat h = habitat(id.getPath());
            this.dress(h);
            StringBuilder log = new StringBuilder();
            for (Map.Entry<EntityType<?>, MobCategory> entry : this.spawnsOf(id, holder.get()).entrySet()) {
                EntityType<?> type = entry.getKey();
                MobCategory cat = entry.getValue();
                if (!tested(type) || peaceful && !type.isAllowedInPeaceful()) {
                    continue;
                }
                boolean wet = cat == MobCategory.WATER_CREATURE || cat == MobCategory.WATER_AMBIENT || cat == MobCategory.UNDERGROUND_WATER_CREATURE;
                boolean dark = !wet && (h.cave() || caveDweller(type));
                BlockPos at = wet ? this.tankSpot() : dark ? this.caveSpot() : this.skySpot();
                String where = wet ? (h.water() ? "water" : sculkWater(h) ? "sculk water" : "chrome") : (dark ? "dark " : "open ")
                        + BuiltInRegistries.BLOCK.getKey(dark ? h.caveFloor() : h.ground()).getPath();
                String why = this.whyNot(type, at);
                String name = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath();
                log.append(name).append('@').append(where).append(why == null ? " ok, " : " FAILS (" + why + "), ");
                this.check.accept(why == null, "spawn rules: " + name + " can never spawn naturally in " + id.getPath() + " (" + where + "): " + why);
                tests++;
            }
            TheSift.LOGGER.info("SMOKE: spawn rules {}: {}", id.getPath(), log);
        }
        this.check.accept(tests > 20, "spawn rules: only " + tests + " biome spawn entries were tested");
    }

    /** Null if a natural spawn of {@code type} at {@code pos} would pass every check the natural spawner makes, else what failed. */
    private @Nullable String whyNot(EntityType<?> type, BlockPos pos) {
        if (!SpawnPlacements.isSpawnPositionOk(type, this.level, pos)) {
            return "spawn placement";
        }
        boolean rules = false;
        for (int i = 0; i < 40 && !rules; i++) {
            rules = SpawnPlacements.checkSpawnRules(type, this.level, EntitySpawnReason.NATURAL, pos, this.level.getRandom());
        }
        if (!rules) {
            return "spawn predicate";
        }
        if (!this.level.noCollision(type.getSpawnAABB(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5))) {
            return "no room";
        }
        Entity e = type.create(this.level, EntitySpawnReason.NATURAL);
        if (!(e instanceof Mob mob)) {
            return "not a mob";
        }
        mob.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
        if (!mob.checkSpawnRules(this.level, EntitySpawnReason.NATURAL)) {
            return "the mob's own spawn rules (light / walk target)";
        }
        return mob.checkSpawnObstruction(this.level) ? null : "spawn obstruction";
    }

    // ------------------------------------------------------------------ never frozen

    /** Every wild creature, one per chunk-sized pen; the flyers sit in the middle row so their neighbours tick too. */
    private static List<EntityType<?>> penned() {
        return List.of(ModEntities.BULB.get(), ModEntities.SLUMBLER.get(), ModEntities.SIFTER.get(), ModCaravans.CARAVAN.get(),
                ModEntities.STOMPER.get(), ModSwifter.SWIFTER.get(),
                ModEchoer.SOUL_GOLEM.get(), ModEntities.HARMONER.get(), ModEntities.SKY_WHALE.get(), ModEchoer.NIB.get(),
                ModEntities.ENCHOER.get(), ModCaveCreatures.JAILER.get(), // CR1: the Echoer flies now, so it pens in the middle row
                ModCaveCreatures.SCULKLING.get(), ModEntities.FANFARE_EEL.get(), ModEntities.KAZOO_FISH.get(), ModEntities.TUBAFISH.get(),
                ModSeaSky.GOBBLER.get(), ModSculkSea.SCULK_FISH.get(), // CR3: the Coral Organ is rooted, so it has no pen
                ModCaveCreatures.CYPOLE.get(), // CR4: the Cypole
                com.thesift.registry.ModSiftSniffer.SIFT_SNIFFER.get(), // E1: the Sift Sniffer
                ModCaravans.CARAVAN_QUEEN.get()); // CR2: the Caravan Queen (she roams until she finds a cave to settle in)
    }

    private static boolean swims(EntityType<?> type) {
        return type == ModEntities.FANFARE_EEL.get() || type == ModEntities.KAZOO_FISH.get() || type == ModEntities.TUBAFISH.get()
                || type == ModSeaSky.GOBBLER.get() || type == ModSculkSea.SCULK_FISH.get();
    }

    /** Pens in the sky over chunks x -9..-4, z 4..6 (inside the generated patch), each chunk force-loaded so it keeps ticking. */
    private void spawnPens() {
        if (this.level.getDifficulty() == Difficulty.PEACEFUL) {
            TheSift.LOGGER.info("SMOKE: never frozen: peaceful difficulty, the monsters are left out");
        }
        // six pens a row; as many rows as there are creatures to pen
        int rows = (penned().size() + 5) / 6;
        for (int chunkX = -9; chunkX <= -4; chunkX++) {
            for (int chunkZ = 4; chunkZ < 4 + rows; chunkZ++) {
                this.level.setChunkForced(chunkX, chunkZ, true);
            }
        }
        BlockState glass = Blocks.GLASS.defaultBlockState();
        BlockState grass = ModBlocks.SIFT_GRASS_BLOCK.get().defaultBlockState();
        List<EntityType<?>> types = penned();
        for (int i = 0; i < types.size(); i++) {
            EntityType<?> type = types.get(i);
            if (this.level.getDifficulty() == Difficulty.PEACEFUL && !type.isAllowedInPeaceful()) {
                continue;
            }
            int cx = (-9 + i % 6) * 16 + 8;
            int cz = (4 + i / 6) * 16 + 8;
            BlockPos centre = new BlockPos(cx, SKY_Y, cz);
            Vec3 at;
            if (swims(type)) {
                // a Chrome pool four deep, glass all round
                fill(cx - 6, SKY_Y - 5, cz - 6, cx + 6, SKY_Y, cz + 6, glass);
                fill(cx - 5, SKY_Y - 4, cz - 5, cx + 5, SKY_Y - 1, cz + 5, ModBlocks.CHROME.get().defaultBlockState());
                fill(cx - 5, SKY_Y, cz - 5, cx + 5, SKY_Y, cz + 5, Blocks.AIR.defaultBlockState());
                at = new Vec3(cx + 0.5, SKY_Y - 3.0, cz + 0.5);
            } else if (type == ModEntities.SKY_WHALE.get()) {
                at = new Vec3(cx + 0.5, SKY_Y + 4.0, cz + 0.5); // open sky
            } else {
                // a meadow of Sift grass with a glass fence two blocks high
                fill(cx - 6, SKY_Y - 1, cz - 6, cx + 6, SKY_Y + 1, cz + 6, glass);
                fill(cx - 5, SKY_Y - 1, cz - 5, cx + 5, SKY_Y - 1, cz + 5, grass);
                fill(cx - 5, SKY_Y, cz - 5, cx + 5, SKY_Y + 1, cz + 5, Blocks.AIR.defaultBlockState());
                at = new Vec3(cx + 0.5, SKY_Y, cz + 0.5);
            }
            Entity e = type.create(this.level, EntitySpawnReason.COMMAND);
            if (!(e instanceof Mob mob)) {
                this.check.accept(false, "never frozen: could not create " + type);
                continue;
            }
            mob.snapTo(at.x, at.y, at.z, this.level.getRandom().nextFloat() * 360.0F, 0.0F);
            mob.finalizeSpawn(this.level, this.level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.COMMAND, null);
            if (!this.level.addFreshEntity(mob)) {
                this.check.accept(false, "never frozen: could not add " + type);
                continue;
            }
            this.watches.add(new Watch(BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath(), mob, centre));
        }
    }

    /** After the first five seconds on its own, a creature that has not gone anywhere yet is walked about its pen. */
    private void walk(Watch w, RandomSource random) {
        if (w.moved >= 1.5 || w.path >= 3.0) {
            return;
        }
        // the far side of the pen from where it stands, so the walk is always a real one
        double x = w.centre.getX() + 0.5 + (w.mob.getX() > w.centre.getX() + 0.5 ? -4 : 4) + random.nextInt(2) - 0.5;
        double z = w.centre.getZ() + 0.5 + (w.mob.getZ() > w.centre.getZ() + 0.5 ? -4 : 4) + random.nextInt(2) - 0.5;
        if (w.mob instanceof SiftFish fish) {
            fish.setSwimTarget(new Vec3(x, SKY_Y - 2.5, z));
        } else if (w.mob instanceof PathfinderMob walker && walker.getNavigation().isDone()) {
            walker.getNavigation().moveTo(x, w.mob.getType() == ModEntities.HARMONER.get() ? SKY_Y + 2.0 : SKY_Y, z, 1.0);
        }
    }
}
