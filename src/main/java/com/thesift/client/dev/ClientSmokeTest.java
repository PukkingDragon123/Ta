package com.thesift.client.dev;

import com.mojang.datafixers.util.Pair;
import com.thesift.TheSift;
import com.thesift.block.SiftPortalBlock;
import com.thesift.portal.PortalFrames;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCreativeTabs;
import com.thesift.registry.ModDimensions;
import com.thesift.registry.ModEntities;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * CI-only client check (enabled with -Dthesift.clientsmoke=true). Quick-plays into the world the
 * server smoke test generated, flies a spectator camera through The Sift and stages showcases of
 * the mobs, blocks, Chrome, the portal and the altar, saving a screenshot of each scene to
 * {@code screenshots/thesift_*.png}. Any crash or a stuck scene fails the run.
 */
public final class ClientSmokeTest {
    private static final boolean ENABLED = Boolean.getBoolean("thesift.clientsmoke");
    private static final int TIMEOUT_TICKS = 20 * 60 * 25;
    /** Showcase stage: high in the (pre-generated) sky above spawn. */
    private static final int STAGE_Y = 290;
    private static final int STAGE_Z = 60;

    private static final List<Scene> SCENES = new ArrayList<>();
    private static int tick;
    private static int sceneIndex = -1;
    private static int sceneTick;
    private static int readyTicks;
    private static volatile boolean setupDone;
    private static volatile @Nullable Vec3 target;
    private static boolean shotTaken;
    private static boolean confirmedBackup;
    private static int shotsSaved;
    private static final List<String> FAILURES = new ArrayList<>();

    /** One staged camera position: server-side setup, then a screenshot once everything has rendered. */
    private record Scene(String name, int settleTicks, Consumer<Ctx> setup, @Nullable Runnable clientAction) {}

    /** What a scene's setup gets to work with, on the server thread. */
    private record Ctx(MinecraftServer server, ServerLevel sift) {
        void run(String command) {
            CommandSourceStack source = this.server.createCommandSourceStack().withSuppressedOutput();
            this.server.getCommands().performPrefixedCommand(source, command);
        }

        /** Spectator camera at {@code eye} looking at {@code at}. */
        void camera(double x, double y, double z, double tx, double ty, double tz) {
            this.run(String.format(Locale.ROOT, "execute in %s run tp @a %.2f %.2f %.2f facing %.2f %.2f %.2f",
                    ModDimensions.THE_SIFT.identifier(), x, y - 1.62, z, tx, ty, tz));
            target = new Vec3(x, y - 1.62, z);
        }

        int surface(int x, int z) {
            return this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }

        void set(int x, int y, int z, BlockState state) {
            this.sift.setBlock(new BlockPos(x, y, z), state, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }

        void fill(int x0, int y0, int z0, int x1, int y1, int z1, BlockState state) {
            for (int x = x0; x <= x1; x++) for (int y = y0; y <= y1; y++) for (int z = z0; z <= z1; z++) this.set(x, y, z, state);
        }

        Entity spawn(EntityType<?> type, double x, double y, double z, float yaw, boolean ai) {
            Entity e = type.create(this.sift, EntitySpawnReason.COMMAND);
            if (e == null) {
                fail("could not create " + type);
                return null;
            }
            e.snapTo(x, y, z, yaw, 0.0F);
            if (e instanceof Mob mob) {
                mob.setNoAi(!ai);
                mob.setPersistenceRequired();
            }
            if (e instanceof LivingEntity living) {
                living.setYHeadRot(yaw);
                living.setYBodyRot(yaw);
            }
            this.sift.addFreshEntity(e);
            return e;
        }
    }

    private ClientSmokeTest() {}

    public static void registerIfEnabled() {
        if (!ENABLED) {
            return;
        }
        buildScenes();
        if (Boolean.getBoolean("thesift.clientsmoke.quick")) {
            SCENES.removeIf(scene -> !QUICK.contains(scene.name()));
        }
        NeoForge.EVENT_BUS.addListener(ClientSmokeTest::onTick);
        TheSift.LOGGER.info("CLIENTSMOKE: enabled with {} scenes", SCENES.size());
    }

    private static void fail(String what) {
        TheSift.LOGGER.error("CLIENTSMOKE FAIL: {}", what);
        FAILURES.add(what);
    }

    // ------------------------------------------------------------------ driver

    private static void onTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        tick++;
        Screen screen = mc.gui.screen();
        if (tick % 200 == 0) {
            TheSift.LOGGER.info("CLIENTSMOKE: tick {} scene {} screen {} dimension {} fps {} rendered-all {}", tick,
                    sceneIndex < 0 || sceneIndex >= SCENES.size() ? "-" : SCENES.get(sceneIndex).name(),
                    screen == null ? "none" : screen.getClass().getSimpleName(), mc.level == null ? "none" : mc.level.dimension().identifier(),
                    mc.getFps(), mc.level != null && mc.levelRenderer.hasRenderedAllSections());
        }
        if (tick > TIMEOUT_TICKS) {
            fail("timed out in scene " + sceneIndex);
            finish(mc);
            return;
        }
        if (mc.level == null && screen != null && screen.getClass().getSimpleName().equals("BackupConfirmScreen") && !confirmedBackup) {
            confirmedBackup = true;
            confirmBackupScreen(screen);
        }
        MinecraftServer server = mc.getSingleplayerServer();
        if (mc.level == null || mc.player == null || server == null) {
            return;
        }
        if (sceneIndex < 0) {
            mc.options.pauseOnLostFocus = false;
            next(mc, server);
            return;
        }
        if (sceneIndex >= SCENES.size()) {
            return;
        }
        Scene scene = SCENES.get(sceneIndex);
        sceneTick++;
        if (!setupDone) {
            if (sceneTick > 20 * 60) {
                fail("setup of " + scene.name() + " never finished");
                next(mc, server);
            }
            return;
        }
        if (!shotTaken) {
            if (sceneTick == 1 && scene.clientAction() != null) {
                scene.clientAction().run();
            }
            Vec3 want = target;
            boolean inPlace = want == null || mc.level.dimension() == ModDimensions.THE_SIFT && mc.player.position().distanceTo(want) < 1.5;
            // software rendering may never quite finish the far sections; 30 s of trying is plenty for a screenshot
            boolean rendered = scene.clientAction() != null || mc.levelRenderer.hasRenderedAllSections() || sceneTick > 20 * 30;
            readyTicks = inPlace && rendered ? readyTicks + 1 : 0;
            boolean overdue = sceneTick > 20 * 90;
            if (readyTicks >= scene.settleTicks() || overdue) {
                if (overdue) {
                    TheSift.LOGGER.warn("CLIENTSMOKE: scene {} not settled (in place {}, rendered {}), capturing anyway", scene.name(), inPlace, rendered);
                    if (!inPlace) {
                        fail("camera never reached scene " + scene.name());
                    }
                }
                capture(mc, scene.name());
                shotTaken = true;
                sceneTick = 0;
            }
            return;
        }
        // give the async screenshot writer a moment, then move on
        if (sceneTick > 20) {
            if (mc.gui.screen() != null && scene.clientAction() != null) {
                mc.gui.setScreen(null);
            }
            next(mc, server);
        }
    }

    /** The "experimental settings / make a backup?" prompt: answer "continue without backup". */
    private static void confirmBackupScreen(Screen screen) {
        try {
            for (Field f : screen.getClass().getDeclaredFields()) {
                if (!f.getType().isInterface()) continue;
                f.setAccessible(true);
                Object listener = f.get(screen);
                for (Method m : f.getType().getMethods()) {
                    if (m.getParameterCount() == 2 && m.getParameterTypes()[0] == boolean.class && m.getParameterTypes()[1] == boolean.class) {
                        TheSift.LOGGER.info("CLIENTSMOKE: answering {} via {}", screen.getClass().getSimpleName(), m.getName());
                        m.invoke(listener, false, false);
                        return;
                    }
                }
            }
            fail("could not answer " + screen.getClass().getName());
        } catch (ReflectiveOperationException e) {
            fail("could not answer backup screen: " + e);
        }
    }

    private static void next(Minecraft mc, MinecraftServer server) {
        sceneIndex++;
        sceneTick = 0;
        readyTicks = 0;
        shotTaken = false;
        setupDone = false;
        if (sceneIndex >= SCENES.size()) {
            finish(mc);
            return;
        }
        Scene scene = SCENES.get(sceneIndex);
        TheSift.LOGGER.info("CLIENTSMOKE: scene {} ({}/{})", scene.name(), sceneIndex + 1, SCENES.size());
        server.execute(() -> {
            try {
                ServerLevel sift = server.getLevel(ModDimensions.THE_SIFT);
                if (sift == null) {
                    fail("The Sift is not loaded");
                } else {
                    scene.setup().accept(new Ctx(server, sift));
                }
            } catch (Throwable t) {
                TheSift.LOGGER.error("CLIENTSMOKE: setup of {} threw", scene.name(), t);
                fail("setup " + scene.name() + ": " + t);
            }
            setupDone = true;
        });
    }

    private static void capture(Minecraft mc, String name) {
        String file = "thesift_" + String.format(Locale.ROOT, "%02d", sceneIndex) + "_" + name + ".png";
        TheSift.LOGGER.info("CLIENTSMOKE: capturing {} at {} fps={}", file, mc.player == null ? "?" : mc.player.blockPosition(), mc.getFps());
        Screenshot.grab(mc.gameDirectory, file, mc.gameRenderer.mainRenderTarget(), 1, message -> {
            shotsSaved++;
            TheSift.LOGGER.info("CLIENTSMOKE: saved {} ({})", file, message.getString());
        });
    }

    private static void finish(Minecraft mc) {
        sceneIndex = SCENES.size();
        if (shotsSaved < SCENES.size()) {
            TheSift.LOGGER.warn("CLIENTSMOKE: {} of {} screenshots saved so far", shotsSaved, SCENES.size());
        }
        if (FAILURES.isEmpty()) {
            TheSift.LOGGER.info("CLIENTSMOKE PASSED");
        } else {
            TheSift.LOGGER.error("CLIENTSMOKE FAILED: {}", FAILURES);
        }
        mc.execute(() -> {
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                // just stopping
            }
            mc.stop();
        });
    }

    // ------------------------------------------------------------------ scenes

    private static void scene(String name, int settle, Consumer<Ctx> setup) {
        SCENES.add(new Scene(name, settle, setup, null));
    }

    /** With -Dthesift.clientsmoke.quick=true only these scenes run (used for the second, Vulkan, pass). */
    private static final java.util.Set<String> QUICK = java.util.Set.of("arrival_east", "overview", "mob_lineup", "mob_closeup_bulb", "blocks_1",
            "chrome_pool", "in_chrome", "portal", "europhy", "creative_items");

    private static void buildScenes() {
        scene("arrival_east", 160, c -> {
            c.run("gamemode spectator @a");
            int h = c.surface(0, 0);
            c.camera(0.5, h + 28, 0.5, 60, h + 10, 0.5);
        });
        scene("vista_west", 80, c -> {
            int h = c.surface(0, 0);
            c.camera(0.5, h + 28, 0.5, -60, h + 12, 10);
        });
        scene("vista_north", 80, c -> {
            int h = c.surface(0, 0);
            c.camera(0.5, h + 22, 0.5, 10, h + 6, -60);
        });
        scene("overview", 100, c -> {
            int h = c.surface(0, 40);
            c.camera(0.5, h + 85, -50.5, 0.5, h, 50.5);
        });
        for (String s : new String[] {"thumper_arena", "sculk_castle"}) {
            scene("natural_" + s, 100, c -> structureShot(c, s));
        }
        // W1 World & terrain: the new swamp, the remade sculk sea (from above and from its floor)
        scene("sculk_swamp", 120, c -> biomeShot(c, com.thesift.registry.ModSculkSwamp.SCULK_SWAMP, false));
        scene("sculk_ocean", 100, c -> biomeShot(c, com.thesift.registry.ModSculkSwamp.SCULK_OCEAN, false));
        scene("sculk_ocean_floor", 100, c -> biomeShot(c, com.thesift.registry.ModSculkSwamp.SCULK_OCEAN, true));
        // the Sculk Castle template (stamped first, at x=-240, y=140, z=-260 by the server test; 27 wide, 72 tall)
        scene("templates_castle", 100, c -> c.camera(-226.5 + 46, 196, -246.5 + 40, -226.5, 168, -246.5));
        scene("templates_castle_arena", 80, c -> c.camera(-226.5 + 15, 214, -246.5 + 15, -226.5, 200, -246.5));
        scene("mob_lineup", 60, ClientSmokeTest::mobStage);
        // the stage runs east (+x) to west; looking south, east is on the left of the picture
        scene("mob_closeup_bulb", 40, c -> c.camera(-8.5, STAGE_Y + 1.6, STAGE_Z + 2.5, -8.5, STAGE_Y + 0.5, STAGE_Z + 6.0));
        // S2: the remade Slumbler sits up (1.6 tall): a higher three-quarter view; its tadpole in a little tank of its own
        scene("mob_closeup_slumbler", 40, c -> c.camera(3.4, STAGE_Y + 2.9, STAGE_Z + 1.8, 1.5, STAGE_Y + 0.9, STAGE_Z + 6.5));
        scene("mob_closeup_slumbler_tadpole", 40, ClientSmokeTest::tadpoleTank);
        // S2 water creatures: each in a glass tank of its own west of the stage; the Cypole on a pad of swamp grass
        scene("mob_closeup_kazoo_fish", 40, c -> tank(c, ModEntities.KAZOO_FISH.get(), -30, 3, 2));
        scene("mob_closeup_fanfare_eel", 40, c -> tank(c, ModEntities.FANFARE_EEL.get(), -37, 4, 2));
        scene("mob_closeup_sculk_fish", 40, c -> tank(c, com.thesift.registry.ModSculkSea.SCULK_FISH.get(), -43, 3, 2));
        scene("mob_closeup_gobbler", 40, c -> tank(c, com.thesift.registry.ModSeaSky.GOBBLER.get(), -53, 7, 3));
        scene("mob_closeup_coral_organ", 40, c -> tank(c, com.thesift.registry.ModSculkSea.CORAL_ORGAN.get(), -61, 5, 3));
        scene("mob_closeup_cypole", 40, ClientSmokeTest::cypolePad);
        scene("mob_closeup_sifter", 40, c -> c.camera(6.5, STAGE_Y + 1.8, STAGE_Z + 2.8, 6.5, STAGE_Y + 0.6, STAGE_Z + 6.5));
        scene("mob_closeup_enchoer", 40, c -> c.camera(10.5, STAGE_Y + 2.6, STAGE_Z + 1.5, 10.5, STAGE_Y + 1.6, STAGE_Z + 6.5));
        scene("mob_closeup_harmoners", 40, c -> c.camera(0.0, STAGE_Y + 1.5, STAGE_Z + 0.2, 0.0, STAGE_Y + 0.5, STAGE_Z + 3.0));
        scene("mob_closeup_sniffer", 40, c -> c.camera(-0.5, STAGE_Y + 3.4, STAGE_Z + 3.2, -3.5, STAGE_Y + 1.2, STAGE_Z + 8.5));
        scene("mob_closeup_dictator", 40, c -> c.camera(20.0, STAGE_Y + 2.8, STAGE_Z - 3.0, 20.0, STAGE_Y + 2.0, STAGE_Z + 5.5));
        scene("mob_closeup_stomper", 60, ClientSmokeTest::stomperStage); // S1: the Stomper elephant and a Stompling
        scene("mobs_live", 100, ClientSmokeTest::livePen);
        for (int part = 0; part < 3; part++) {
            int p = part;
            scene("blocks_" + (part + 1), 60, c -> blockShowcase(c, p));
        }
        scene("chrome_pool", 80, ClientSmokeTest::chromePool);
        scene("in_chrome", 60, c -> {
            c.run("gamemode creative @a");
            c.run(String.format(Locale.ROOT, "execute in %s run tp @a 40.5 %d 0.5 facing 44 %d 4", ModDimensions.THE_SIFT.identifier(), STAGE_Y - 3,
                    STAGE_Y - 2));
            target = new Vec3(40.5, STAGE_Y - 3, 0.5);
        });
        scene("portal", 80, ClientSmokeTest::portalStage);
        scene("europhy", 60, ClientSmokeTest::europhyStage);
        scene("drums", 40, ClientSmokeTest::drumStage);
        scene("bosses", 60, ClientSmokeTest::bossStage);
        scene("bosses_close", 40, c -> c.camera(-118.5, STAGE_Y + 4.5, -9.0, -120.5, STAGE_Y + 2.0, 0.5));
        // the Thumper with its vents open below a cannon tower; then the gunner's view from the tower top
        scene("turtle_cannon", 60, ClientSmokeTest::turtleCannonStage);
        scene("turtle_cannon_gunner", 40, c -> c.camera(-166.5, STAGE_Y + 7.0 + 1.62, -5.5, -160.0, STAGE_Y + 1.5, 0.5));
        // Sculk Corruption IV taking your sight (cleared again after a few seconds)
        scene("corruption", 80, c -> {
            c.run("effect give @a thesift:sculk_corruption 7 3");
            c.camera(-148.0, STAGE_Y + 6.0, -14.0, -160.0, STAGE_Y + 4.0, 0.5);
        });
        // the Drum Pit template (stamped after the castle by the server test)
        scene("templates_arena", 80, c -> {
            c.run("effect clear @a");
            c.camera(307.5, 178, -186.0, 307.5, 145, -236.5);
        });
        // F3: the Knowledge Book, opened at a few spreads (contents, live creatures, a song sheet, lore, the guide's goals)
        String[] codexPages = {"contents_0", "harmoner", "mini_creator", "song_whale", "lore_creator_rifts", "quest_arrival", "dictator", "staff"};
        for (String page : codexPages) {
            SCENES.add(new Scene("codex_" + page, 50, c -> target = null,
                    () -> Minecraft.getInstance().gui.setScreen(new com.thesift.client.codex.KnowledgeBookScreen(page))));
        }
        SCENES.add(new Scene("creative_blocks", 30, c -> {
            c.run("gamemode creative @a");
            target = null;
        }, () -> openCreativeTab(ModCreativeTabs.BLOCKS.get())));
        SCENES.add(new Scene("creative_items", 30, c -> target = null, () -> openCreativeTab(ModCreativeTabs.ITEMS.get())));
    }

    private static void openCreativeTab(CreativeModeTab tab) {
        Minecraft mc = Minecraft.getInstance();
        try {
            Field f = CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
            f.setAccessible(true);
            f.set(null, tab);
        } catch (ReflectiveOperationException e) {
            fail("cannot select creative tab: " + e);
        }
        if (mc.player != null) {
            mc.gui.setScreen(new CreativeModeInventoryScreen(mc.player, mc.player.connection.enabledFeatures(), false));
        }
    }

    // ------------------------------------------------------------------ helpers run on the server thread

    private static void structureShot(Ctx c, String name) {
        Registry<Structure> registry = c.sift().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder = registry.get(ResourceKey.create(Registries.STRUCTURE, TheSift.id(name)));
        if (holder.isEmpty()) {
            fail("structure " + name + " missing");
            return;
        }
        Pair<BlockPos, Holder<Structure>> found = c.sift().getChunkSource().getGenerator()
                .findNearestMapStructure(c.sift(), HolderSet.direct(holder.get()), BlockPos.ZERO, 24, false);
        if (found == null) {
            fail("no " + name + " nearby");
            return;
        }
        BlockPos at = found.getFirst();
        StructureStart start = c.sift().getChunk(at.getX() >> 4, at.getZ() >> 4).getStartForStructure(holder.get().value());
        if (start == null || !start.isValid()) {
            fail("no start for " + name + " at " + at);
            return;
        }
        BoundingBox box = start.getBoundingBox();
        BlockPos centre = box.getCenter();
        double span = Math.max(box.getXSpan(), box.getZSpan());
        double dist = span * 0.85 + 9;
        TheSift.LOGGER.info("CLIENTSMOKE: {} box {} centre {}", name, box, centre);
        c.camera(centre.getX() + dist * 0.72, box.maxY() + span * 0.35 + 5, centre.getZ() + dist * 0.72, centre.getX(),
                (box.minY() + box.maxY()) / 2.0, centre.getZ());
    }

    /** W1 World & terrain: a look over (or under) the nearest stretch of a biome. */
    private static void biomeShot(Ctx c, ResourceKey<net.minecraft.world.level.biome.Biome> biome, boolean underwater) {
        Pair<BlockPos, Holder<net.minecraft.world.level.biome.Biome>> found = c.sift().findClosestBiome3d(b -> b.is(biome), new BlockPos(0, 64, 0),
                1600, 64, 64);
        if (found == null) {
            fail("no " + biome.identifier() + " within reach");
            return;
        }
        BlockPos at = found.getFirst();
        int h = c.surface(at.getX(), at.getZ());
        TheSift.LOGGER.info("CLIENTSMOKE: {} at {} surface {}", biome.identifier(), at, h);
        c.run("gamemode spectator @a");
        if (underwater) {
            int floor = c.sift().getHeight(Heightmap.Types.OCEAN_FLOOR, at.getX(), at.getZ());
            c.camera(at.getX() + 12.5, Math.min(floor + 11, 58), at.getZ() + 9.5, at.getX() + 0.5, floor + 1, at.getZ() + 0.5);
        } else {
            c.camera(at.getX() + 30.5, h + 22, at.getZ() + 24.5, at.getX() + 0.5, h + 2, at.getZ() + 0.5);
        }
    }

    private static void floor(Ctx c, int x0, int z0, int x1, int z1) {
        c.fill(x0, STAGE_Y - 1, z0, x1, STAGE_Y - 1, z1, ModBlocks.SIFT_GRASS_BLOCK.get().defaultBlockState());
        c.fill(x0, STAGE_Y, z0, x1, STAGE_Y + 6, z1, Blocks.AIR.defaultBlockState());
    }

    private static void mobStage(Ctx c) {
        c.run("gamemode spectator @a");
        floor(c, -16, STAGE_Z - 6, 26, STAGE_Z + 12);
        // a little dressing: flowers and grass along the back
        Block[] plants = plantsFor(c);
        for (int x = -16; x <= 16; x++) {
            if (plants.length > 0 && Math.floorMod(x * 7, 3) != 0) {
                c.set(x, STAGE_Y, STAGE_Z + 11, plants[Math.floorMod(x, plants.length)].defaultBlockState());
            }
        }
        float face = 180.0F;
        c.spawn(ModEntities.BULB.get(), -9.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        Entity baby = c.spawn(ModEntities.BULB.get(), -7.0, STAGE_Y, STAGE_Z + 5.0, 150.0F, false);
        if (baby instanceof net.minecraft.world.entity.AgeableMob ageable) {
            ageable.setAge(-24000);
        }
        // E1: the Sift Sniffer, the Sift's seed digger
        c.spawn(com.thesift.registry.ModSiftSniffer.SIFT_SNIFFER.get(), -3.5, STAGE_Y, STAGE_Z + 8.5, face, false);
        // the Dictator and his orchestra, at the far end of the stage
        c.spawn(ModEntities.DICTATOR.get(), 19.5, STAGE_Y, STAGE_Z + 7.0, face, false);
        c.spawn(ModEntities.SCULK_HARMONER.get(), 21.5, STAGE_Y + 2.5, STAGE_Z + 6.0, face, false);
        c.spawn(ModEntities.SCULK_PARASITE.get(), 16.2, STAGE_Y, STAGE_Z + 4.0, face, false);
        c.spawn(ModEntities.STRUMLING.get(), 24.6, STAGE_Y, STAGE_Z + 5.5, face, false);
        // one Harmoner of every colour, perched in a row
        for (int i = 0; i < com.thesift.entity.Harmoner.VARIANTS; i++) {
            if (c.spawn(ModEntities.HARMONER.get(), -2.5 + i, STAGE_Y, STAGE_Z + 3.0, face, false) instanceof com.thesift.entity.Harmoner bird) {
                bird.setVariant(i);
            }
        }
        c.spawn(ModEntities.SLUMBLER.get(), 1.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.spawn(ModEntities.SIFTER.get(), 6.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.spawn(ModEntities.ENCHOER.get(), 10.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.camera(2.5, STAGE_Y + 4.5, STAGE_Z - 9.5, 2.5, STAGE_Y + 1.0, STAGE_Z + 6.5);
    }

    /** S2: a Slumbler tadpole in a small glass tank of water west of the stage, seen from above and in front. */
    private static void tadpoleTank(Ctx c) {
        tank(c, com.thesift.registry.ModSlumbler.SLUMBLER_TADPOLE.get(), -24, 3, 2);
    }

    /** S2: a water creature (no AI) in a glass tank of water (size x size, depth deep) west of the stage, seen from above and in front. */
    private static void tank(Ctx c, net.minecraft.world.entity.EntityType<?> type, int x0, int size, int depth) {
        int z0 = STAGE_Z;
        c.fill(x0 - 1, STAGE_Y - 1, z0 - 1, x0 + size, STAGE_Y - 1, z0 + size, Blocks.SAND.defaultBlockState());
        c.fill(x0 - 1, STAGE_Y, z0 - 1, x0 + size, STAGE_Y + depth - 1, z0 + size, Blocks.GLASS.defaultBlockState());
        c.fill(x0, STAGE_Y, z0, x0 + size - 1, STAGE_Y + depth - 1, z0 + size - 1, Blocks.WATER.defaultBlockState());
        c.fill(x0 - 1, STAGE_Y + depth, z0 - 1, x0 + size, STAGE_Y + depth + 3, z0 + size, Blocks.AIR.defaultBlockState());
        double mid = size / 2.0;
        c.spawn(type, x0 + mid, STAGE_Y + (depth > 2 ? 0.6 : 0.4), z0 + mid, 200.0F, false);
        c.camera(x0 + mid + size * 0.55 + 0.6, STAGE_Y + depth + 0.7 + size * 0.3, z0 - 0.4 - size * 0.25, x0 + mid, STAGE_Y + depth * 0.4,
                z0 + mid);
    }

    /** S2: a Cypole squatting on a pad of swamp grass, seen from the front. */
    private static void cypolePad(Ctx c) {
        int x0 = -68, z0 = STAGE_Z;
        c.fill(x0 - 2, STAGE_Y - 1, z0 - 2, x0 + 2, STAGE_Y - 1, z0 + 2, Blocks.MOSS_BLOCK.defaultBlockState());
        c.fill(x0 - 2, STAGE_Y, z0 - 2, x0 + 2, STAGE_Y + 3, z0 + 2, Blocks.AIR.defaultBlockState());
        c.spawn(com.thesift.registry.ModCaveCreatures.CYPOLE.get(), x0 + 0.5, STAGE_Y, z0 + 0.5, 200.0F, false);
        c.camera(x0 + 2.4, STAGE_Y + 1.9, z0 - 1.6, x0 + 0.5, STAGE_Y + 0.5, z0 + 0.5);
    }

    /** S1: the Stomper elephant and a Stompling on a patch of Sift Plains turf, three-quarters from the front. */
    private static void stomperStage(Ctx c) {
        c.run("gamemode spectator @a");
        floor(c, -48, STAGE_Z - 6, -28, STAGE_Z + 12);
        c.fill(-48, STAGE_Y - 1, STAGE_Z - 6, -28, STAGE_Y - 1, STAGE_Z + 12, ModBlocks.CORAL_TURF.get().defaultBlockState());
        Block[] plants = plantsFor(c);
        for (int x = -48; x <= -28; x++) {
            if (plants.length > 0 && Math.floorMod(x * 5, 3) != 0) {
                c.set(x, STAGE_Y, STAGE_Z + 11, plants[Math.floorMod(x, plants.length)].defaultBlockState());
            }
        }
        c.spawn(ModEntities.STOMPER.get(), -38.5, STAGE_Y, STAGE_Z + 5.5, 180.0F, false);
        if (c.spawn(ModEntities.STOMPER.get(), -34.0, STAGE_Y, STAGE_Z + 2.5, 150.0F, false) instanceof net.minecraft.world.entity.AgeableMob baby) {
            baby.setAge(-24000);
        }
        c.camera(-33.0, STAGE_Y + 3.0, STAGE_Z - 1.5, -38.0, STAGE_Y + 1.7, STAGE_Z + 5.0);
    }

    private static void livePen(Ctx c) {
        int z0 = STAGE_Z + 20;
        floor(c, -12, z0, 12, z0 + 14);
        BlockState fence = ModBlocks.DREAMSTONE_BRICKS.get().defaultBlockState();
        for (int x = -12; x <= 12; x++) {
            c.set(x, STAGE_Y, z0, fence);
            c.set(x, STAGE_Y, z0 + 14, fence);
        }
        for (int z = z0; z <= z0 + 14; z++) {
            c.set(-12, STAGE_Y, z, fence);
            c.set(12, STAGE_Y, z, fence);
        }
        for (int i = 0; i < 4; i++) {
            c.spawn(ModEntities.BULB.get(), -6.5 + i * 3, STAGE_Y, z0 + 5.5, i * 90.0F, true);
        }
        c.spawn(ModEntities.SLUMBLER.get(), 4.5, STAGE_Y, z0 + 8.5, 200.0F, true);
        c.spawn(ModEntities.ENCHOER.get(), -4.5, STAGE_Y, z0 + 10.5, 160.0F, true);
        for (int i = 0; i < 3; i++) {
            if (c.spawn(ModEntities.HARMONER.get(), -2.5 + i * 3, STAGE_Y + 3, z0 + 7.5, i * 120.0F, true) instanceof com.thesift.entity.Harmoner bird) {
                bird.setVariant(i * 2);
            }
        }
        c.camera(0.5, STAGE_Y + 7, z0 - 6.5, 0.5, STAGE_Y, z0 + 7);
    }

    private static Block[] plantsFor(Ctx c) {
        List<Block> out = new ArrayList<>();
        for (String id : new String[] {"dreambloom", "lullaby_bell", "soulpetal", "nebula_iris", "blushgrass", "glimmer_sprouts"}) {
            BuiltInRegistries.BLOCK.getOptional(TheSift.id(id)).ifPresent(out::add);
        }
        return out.toArray(new Block[0]);
    }

    private static List<Block> showcaseBlocks() {
        List<Block> out = new ArrayList<>();
        for (Supplier<? extends Block> holder : ModBlocks.BLOCKS.getEntries()) {
            Block b = holder.get();
            if (b instanceof LiquidBlock || b instanceof SiftPortalBlock) continue;
            out.add(b);
        }
        return out;
    }

    /** Every block of the mod laid out on a lawn, a third of them per shot. */
    private static void blockShowcase(Ctx c, int part) {
        List<Block> blocks = showcaseBlocks();
        int cols = 12;
        int rowsPerPart = (int) Math.ceil(blocks.size() / (double) cols / 3.0);
        int z0 = STAGE_Z + 50 + part * (rowsPerPart * 2 + 6);
        int x0 = -12;
        floor(c, x0 - 2, z0 - 2, x0 + cols * 2, z0 + rowsPerPart * 2 + 1);
        int first = part * rowsPerPart * cols;
        for (int i = first; i < Math.min(blocks.size(), first + rowsPerPart * cols); i++) {
            int k = i - first;
            int x = x0 + (k % cols) * 2;
            int z = z0 + (k / cols) * 2;
            BlockState state = blocks.get(i).defaultBlockState();
            if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF)) {
                c.set(x, STAGE_Y, z, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.LOWER));
                c.set(x, STAGE_Y + 1, z, state.setValue(BlockStateProperties.DOUBLE_BLOCK_HALF, DoubleBlockHalf.UPPER));
            } else {
                c.set(x, STAGE_Y, z, state);
            }
        }
        TheSift.LOGGER.info("CLIENTSMOKE: showcase part {} blocks {}..{} of {}", part, first, Math.min(blocks.size(), first + rowsPerPart * cols) - 1,
                blocks.size());
        double cx = x0 + cols - 1;
        c.camera(cx + 0.5, STAGE_Y + 9.5, z0 - 7.5, cx + 0.5, STAGE_Y, z0 + rowsPerPart);
    }

    private static void chromePool(Ctx c) {
        c.run("gamemode spectator @a");
        BlockState rim = ModBlocks.DREAMSTONE_BRICKS.get().defaultBlockState();
        c.fill(34, STAGE_Y - 5, -6, 46, STAGE_Y - 1, 7, rim);
        BlockState chrome = BuiltInRegistries.BLOCK.getValue(TheSift.id("chrome")).defaultBlockState();
        for (int x = 35; x <= 45; x++) {
            for (int z = -5; z <= 6; z++) {
                for (int y = STAGE_Y - 4; y <= STAGE_Y - 1; y++) {
                    c.sift().setBlock(new BlockPos(x, y, z), chrome, Block.UPDATE_ALL);
                }
            }
        }
        c.fill(34, STAGE_Y, -6, 46, STAGE_Y + 8, 7, Blocks.AIR.defaultBlockState());
        c.camera(40.5, STAGE_Y + 5.5, -10.5, 40.5, STAGE_Y - 1, 1.5);
    }

    private static void portalStage(Ctx c) {
        c.run("gamemode spectator @a");
        int x0 = -32, z = 0;
        floor(c, x0 - 6, z - 6, x0 + 8, z + 6);
        BlockState frame = ModBlocks.SIFT_GATE_FRAME.get().defaultBlockState();
        for (int dx = -1; dx <= 3; dx++) {
            for (int dy = 0; dy <= 5; dy++) {
                boolean edge = dx == -1 || dx == 3 || dy == 0 || dy == 5;
                c.set(x0 + dx, STAGE_Y + dy, z, edge ? frame : Blocks.AIR.defaultBlockState());
            }
        }
        PortalFrames.Frame f = PortalFrames.find(c.sift(), new BlockPos(x0 + 1, STAGE_Y + 2, z), 8);
        if (f == null) {
            fail("showcase portal frame not found");
        } else {
            PortalFrames.fill(c.sift(), f);
        }
        c.camera(x0 + 4.5, STAGE_Y + 3.5, z - 7.5, x0 + 1.5, STAGE_Y + 2.5, z + 0.5);
    }

    /** Three drums: at rest, mid-beat and with a glowing Warden Core inside. */
    private static void drumStage(Ctx c) {
        int x0 = -84, z0 = 0;
        floor(c, x0 - 4, z0 - 4, x0 + 4, z0 + 4);
        BlockState drum = ModBlocks.SIFT_DRUM.get().defaultBlockState();
        c.set(x0 - 2, STAGE_Y, z0, drum);
        c.set(x0, STAGE_Y, z0, drum.setValue(com.thesift.block.SiftDrumBlock.HIT, 1));
        c.set(x0 + 2, STAGE_Y, z0, drum.setValue(com.thesift.block.SiftDrumBlock.CORE, true));
        c.camera(x0 + 0.5, STAGE_Y + 2.6, z0 - 3.2, x0 + 0.5, STAGE_Y + 0.4, z0 + 0.5);
    }

    /** The mini-bosses with their young, the Mask, and the Grand Stage's altars. */
    private static void bossStage(Ctx c) {
        int x0 = -120, z0 = 0;
        floor(c, x0 - 14, z0 - 8, x0 + 14, z0 + 8);
        float face = 180.0F;
        c.spawn(ModEntities.THUMPER.get(), x0 - 8.5, STAGE_Y, z0 + 2.5, 160.0F, false);
        c.spawn(ModEntities.STRUMMER.get(), x0 + 0.5, STAGE_Y, z0 + 2.5, face, false);
        c.spawn(ModEntities.SCULK_PARASITE.get(), x0 - 5.5, STAGE_Y, z0 - 1.5, face, false);
        c.spawn(ModEntities.STRUMLING.get(), x0 + 3.0, STAGE_Y, z0 - 1.5, face, false);
        c.spawn(ModEntities.CONDUCTOR_MASK.get(), x0 + 12.0, STAGE_Y + 1.0, z0 - 3.0, face, false);
        BlockState altar = ModBlocks.INSTRUMENT_ALTAR.get().defaultBlockState();
        for (int i = 0; i < 3; i++) {
            c.set(x0 - 12 + i * 2, STAGE_Y, z0 - 4, altar);
        }
        c.set(x0 - 12, STAGE_Y, z0 - 6, ModBlocks.ENCORE_SIGIL.get().defaultBlockState());
        c.camera(x0 + 0.5, STAGE_Y + 6.0, z0 - 14.0, x0 + 0.5, STAGE_Y + 1.5, z0 + 1.0);
    }

    /** The Thumper, spent and gaping, a crumbling arena wall behind it, and a loaded cannon on its tower. */
    private static void turtleCannonStage(Ctx c) {
        int x0 = -160, z0 = 0;
        floor(c, x0 - 16, z0 - 16, x0 + 16, z0 + 16);
        c.fill(x0 - 16, STAGE_Y + 1, z0 - 16, x0 + 16, STAGE_Y + 12, z0 + 16, Blocks.AIR.defaultBlockState());
        // the tower: a hushslate column with the cannon on top, aimed at the turtle
        c.fill(x0 - 8, STAGE_Y, z0 - 7, x0 - 6, STAGE_Y + 5, z0 - 5, ModBlocks.HUSHSLATE_BRICKS.get().defaultBlockState());
        c.set(x0 - 7, STAGE_Y + 6, z0 - 6, ModBlocks.ANCIENT_CANNON.get().defaultBlockState()
                .setValue(com.thesift.block.AncientCannonBlock.FACING, net.minecraft.core.Direction.SOUTH)
                .setValue(com.thesift.block.AncientCannonBlock.LOADED, true));
        // a crumbling arena wall it has half smashed through
        c.fill(x0 + 5, STAGE_Y, z0 - 6, x0 + 6, STAGE_Y + 3, z0 + 6, ModBlocks.CRUMBLING_DREAMSTONE.get().defaultBlockState());
        c.fill(x0 + 5, STAGE_Y + 1, z0 - 1, x0 + 6, STAGE_Y + 3, z0 + 1, Blocks.AIR.defaultBlockState());
        if (c.spawn(ModEntities.THUMPER.get(), x0, STAGE_Y, z0, 200.0F, false) instanceof com.thesift.entity.boss.Thumper th) {
            th.codexPose(com.thesift.entity.boss.Thumper.EXPOSED);
        }
        c.camera(x0 + 12.0, STAGE_Y + 7.0, z0 - 12.0, x0, STAGE_Y + 1.5, z0);
    }

    /** F1: two Europhy Tables - one loaded with Siftite Dust and Copper and played until it forms, one idle with a Siftite Ingot on its hub. */
    private static void europhyStage(Ctx c) {
        int x0 = -60, z0 = 0;
        floor(c, x0 - 7, z0 - 7, x0 + 7, z0 + 7);
        c.set(x0, STAGE_Y, z0, ModBlocks.EUROPHY_TABLE.get().defaultBlockState());
        c.set(x0 + 2, STAGE_Y, z0 + 1, ModBlocks.EUROPHY_TABLE.get().defaultBlockState());
        BlockPos at = new BlockPos(x0, STAGE_Y, z0);
        if (c.sift().getBlockEntity(at) instanceof com.thesift.block.entity.EurophyTableBlockEntity t) {
            t.getItems().setItem(0, new net.minecraft.world.item.ItemStack(com.thesift.registry.ModItems.SIFTITE_DUST.get(), 4));
            t.getItems().setItem(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.COPPER_INGOT));
            for (int p : new int[]{6, 8, 10, 13, 15, 18, 20, 22}) {
                com.thesift.music.SongEvents.note(c.sift(), null, net.minecraft.world.phys.Vec3.atCenterOf(at), p);
            }
        }
        if (c.sift().getBlockEntity(at.offset(2, 0, 1)) instanceof com.thesift.block.entity.EurophyTableBlockEntity idle) {
            idle.getItems().setItem(com.thesift.block.entity.EurophyTableBlockEntity.OUTPUT,
                    new net.minecraft.world.item.ItemStack(com.thesift.registry.ModItems.SIFTITE_INGOT.get()));
        }
        c.camera(x0 + 1.5, STAGE_Y + 3.0, z0 - 4.0, x0 + 1.0, STAGE_Y + 0.8, z0 + 0.5);
    }
}
