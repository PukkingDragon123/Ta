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
            "chrome_pool", "in_chrome", "portal", "altar", "creative_items");

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
        for (String s : new String[] {"chrome_well", "sift_ruins", "abandoned_altar", "collapsed_tower", "dream_statue"}) {
            scene("natural_" + s, 100, c -> structureShot(c, s));
        }
        scene("deep_shrine", 80, ClientSmokeTest::deepShrineShot);
        scene("templates_towers", 80, c -> c.camera(-200.5, 172, -212.5, -175, 150, -252));
        scene("templates_instruments", 80, c -> c.camera(-45.5, 168, -224.5, -40, 146, -255));
        scene("templates_statues", 80, c -> c.camera(140.5, 168, -226.5, 150, 146, -255));
        scene("mob_lineup", 60, ClientSmokeTest::mobStage);
        scene("mob_closeup_bulb", 40, c -> c.camera(-7.5, STAGE_Y + 2.2, STAGE_Z - 2.5, -8.5, STAGE_Y + 0.8, STAGE_Z + 6.5));
        scene("mob_closeup_slumbler", 40, c -> c.camera(0.5, STAGE_Y + 3.0, STAGE_Z - 3.5, 0.5, STAGE_Y + 0.8, STAGE_Z + 6.5));
        scene("mob_closeup_sifter_enchoer", 40, c -> c.camera(9.5, STAGE_Y + 3.0, STAGE_Z - 3.5, 9.5, STAGE_Y + 1.5, STAGE_Z + 6.5));
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
        scene("altar", 60, ClientSmokeTest::altarStage);
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

    private static void deepShrineShot(Ctx c) {
        Registry<Structure> registry = c.sift().registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder = registry.get(ResourceKey.create(Registries.STRUCTURE, TheSift.id("deep_shrine")));
        Pair<BlockPos, Holder<Structure>> found = holder.isEmpty() ? null : c.sift().getChunkSource().getGenerator()
                .findNearestMapStructure(c.sift(), HolderSet.direct(holder.get()), BlockPos.ZERO, 24, false);
        if (found == null) {
            fail("no deep shrine nearby");
            return;
        }
        BlockPos at = found.getFirst();
        StructureStart start = c.sift().getChunk(at.getX() >> 4, at.getZ() >> 4).getStartForStructure(holder.get().value());
        if (start == null || !start.isValid()) {
            fail("no deep shrine start");
            return;
        }
        BoundingBox box = start.getBoundingBox();
        // look at the Echo Frame gate from the far side of the room
        long gx = 0, gy = 0, gz = 0, n = 0;
        for (BlockPos p : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
            if (c.sift().getBlockState(p).is(ModBlocks.ECHO_FRAME.get())) {
                gx += p.getX();
                gy += p.getY();
                gz += p.getZ();
                n++;
            }
        }
        BlockPos centre = box.getCenter();
        if (n == 0) {
            fail("deep shrine has no echo frame");
            c.camera(centre.getX() + 0.5, box.minY() + 3.5, centre.getZ() + 0.5, centre.getX() + 5, box.minY() + 2, centre.getZ());
            return;
        }
        double tx = gx / (double) n + 0.5, ty = gy / (double) n + 0.5, tz = gz / (double) n + 0.5;
        double dx = centre.getX() + 0.5 - tx, dz = centre.getZ() + 0.5 - tz;
        double len = Math.max(0.001, Math.hypot(dx, dz));
        double reach = Math.min(6.5, Math.max(box.getXSpan(), box.getZSpan()) / 2.0 - 1.5);
        TheSift.LOGGER.info("CLIENTSMOKE: deep shrine box {} gate {} {} {}", box, tx, ty, tz);
        c.run("gamemode spectator @a");
        c.camera(tx + dx / len * reach * 1.6, ty + 0.5, tz + dz / len * reach * 1.6, tx, ty, tz);
    }

    private static final BlockState STAGE_FLOOR = ModBlocks.SIFT_GRASS_BLOCK.get().defaultBlockState();

    private static void floor(Ctx c, int x0, int z0, int x1, int z1) {
        c.fill(x0, STAGE_Y - 1, z0, x1, STAGE_Y - 1, z1, STAGE_FLOOR);
        c.fill(x0, STAGE_Y, z0, x1, STAGE_Y + 6, z1, Blocks.AIR.defaultBlockState());
    }

    private static void mobStage(Ctx c) {
        c.run("gamemode spectator @a");
        floor(c, -16, STAGE_Z - 6, 16, STAGE_Z + 12);
        // a little dressing: flowers and grass along the back
        Block[] plants = plantsFor(c);
        for (int x = -16; x <= 16; x++) {
            if (plants.length > 0 && Math.floorMod(x * 7, 3) != 0) {
                c.set(x, STAGE_Y, STAGE_Z + 11, plants[Math.floorMod(x, plants.length)].defaultBlockState());
            }
        }
        c.set(13, STAGE_Y + 3, STAGE_Z + 6, ModBlocks.DREAMSTONE_BRICKS.get().defaultBlockState());
        float face = 180.0F;
        c.spawn(ModEntities.BULB.get(), -9.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        Entity baby = c.spawn(ModEntities.BULB.get(), -7.0, STAGE_Y, STAGE_Z + 5.0, 150.0F, false);
        if (baby instanceof net.minecraft.world.entity.AgeableMob ageable) {
            ageable.setAge(-24000);
        }
        c.spawn(EntityTypes.SNIFFER, -3.5, STAGE_Y, STAGE_Z + 8.5, face, false);
        c.spawn(ModEntities.SLUMBLER.get(), 1.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.spawn(ModEntities.SIFTER.get(), 6.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.spawn(ModEntities.ENCHOER.get(), 10.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.spawn(ModEntities.RIVETER.get(), 13.5, STAGE_Y, STAGE_Z + 6.5, face, false);
        c.camera(2.5, STAGE_Y + 4.5, STAGE_Z - 9.5, 2.5, STAGE_Y + 1.0, STAGE_Z + 6.5);
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
        BlockState frame = ModBlocks.ECHO_FRAME.get().defaultBlockState();
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

    private static void altarStage(Ctx c) {
        int x0 = -60, z0 = 0;
        floor(c, x0 - 7, z0 - 7, x0 + 7, z0 + 7);
        c.set(x0, STAGE_Y, z0, ModBlocks.EUPHORY_ALTAR.get().defaultBlockState());
        BlockState drum = ModBlocks.SIFT_DRUM.get().defaultBlockState();
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0;
            c.set(x0 + (int) Math.round(Math.cos(a) * 3), STAGE_Y, z0 + (int) Math.round(Math.sin(a) * 3), drum);
        }
        c.camera(x0 + 0.5, STAGE_Y + 4.5, z0 - 6.5, x0 + 0.5, STAGE_Y + 0.5, z0 + 0.5);
    }
}
