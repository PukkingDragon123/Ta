package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.block.SculkSummonerBlock;
import com.thesift.block.entity.SculkSummonerBlockEntity;
import com.thesift.entity.Enchoer;
import com.thesift.entity.GlowballEntity;
import com.thesift.entity.Harmoner;
import com.thesift.entity.boss.Dictator;
import com.thesift.block.entity.BossDenBlockEntity;
import com.thesift.portal.PortalFrames;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

/**
 * Gameplay checks for the CI smoke test: arms a Sculk Summoner (a Warden Core in a Sculk Catalyst,
 * four Sculk Sensors) before a real Ancient City gate in the Overworld, plays the Sift Symphony on a
 * Conga Drum (first without its sheet, which must do nothing; then with it, the sensors lighting
 * note by note), walks a pig through the portal it opens, flings a Glowball at a Warden and soaks a hurt Bulb in Chrome (it comes out Rainbow Dazed, not healed).
 */
final class MechanicsTest {
    private enum Stage { RITUAL, PIG, DONE }

    private final MinecraftServer server;
    private final ServerLevel overworld;
    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;
    private Stage stage = Stage.RITUAL;
    private int ticks;

    // MANSION: the Sculk Summoner and the Sift Symphony (the drum's rhythm ritual is gone)
    private @Nullable SculkSummonerBlockEntity summoner;
    private BlockPos summonerPos = BlockPos.ZERO;
    private @Nullable FakePlayer player;
    private PortalFrames.@Nullable Frame gate;
    private final List<String> phases = new ArrayList<>();
    private String lastPhase = "";
    private int ritualTicks;
    private int nextNote;
    private int maxLit;
    private int litWithoutSheet = -1;
    private @Nullable UUID pig;
    private int pigTicks;

    private @Nullable EurophyTest europhy; // F1 Materials & Europhy Table
    private @Nullable Warden warden;
    private @Nullable Mob soaker;
    private float soakerStartHealth;

    MechanicsTest(MinecraftServer server, ServerLevel sift, BiConsumer<Boolean, String> check) {
        this.server = server;
        this.overworld = server.overworld();
        this.sift = sift;
        this.check = check;
    }

    boolean done() {
        return this.stage == Stage.DONE;
    }

    private void check(boolean ok, String what) {
        this.check.accept(ok, what);
    }

    // ------------------------------------------------------------------ setup

    void start() {
        this.startRitual();
        this.startGlowball();
        this.startChromeSoak();
        ChromeTest.run(this.sift, this.check); // A3 Chrome: chime sand, chrome fish bucket
        this.europhy = EurophyTest.start(this.sift, this.check); // F1: a Europhy craft by music, Bauxite's Chrome rule
        BandTableTest.run(this.sift, this.check); // F2 Band Table: a played song enchants; sloppy playing weakens it; the band requirement
        KnowledgeTest.run(this.sift, this.check); // F3 Knowledge & lore: unlocks saved/loaded/kept on death, lore, the Mini Creator
        SkyTest.run(this.sift, this.check); // W-sky: the islands float, ropes swing, vines/fruit/bunches work, bridges and sky trees build
        CoralOrganTest.run(this.sift, this.check); // WATER: the coral organ is hostile, gapes (telegraph) and bites
        this.checkTrades();
        this.checkHarmoners();
        this.checkSniffer();
        this.checkRot(); // E1 Sniffer & rot
        this.startDictator();
        this.songTest = SongTest.start(this.sift, this.check); // C4 songs: every song through the real tracker + the Echoer's reward
        this.bandTest = BandTest.start(this.sift, this.check); // M2 band: compatible songs recruit, others do not; play-along; leaving
        this.drillTest = DrillTest.start(this.sift, this.check); // RR: the Echoer Drill reads a rhythm (a straight bore, stairs down)
    }

    private @Nullable DrillTest drillTest; // RR Echoer Drill

    private @Nullable BandTest bandTest; // M2 band

    private @Nullable SongTest songTest; // C4 songs

    private @Nullable Dictator dictator;

    /** The Grand Stage (a hidden boss den) wakes the Dictator; at half health he should move on to his second phase. */
    private void startDictator() {
        int x = -24, z = 30;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2;
        for (BlockPos p : BlockPos.betweenClosed(x - 5, y - 1, z - 5, x + 5, y - 1, z + 5)) {
            this.sift.setBlock(p, ModBlocks.POLISHED_HUSHSLATE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : BlockPos.betweenClosed(x - 5, y, z - 5, x + 5, y + 5, z + 5)) {
            this.sift.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockPos at = new BlockPos(x, y, z);
        this.sift.setBlock(at, ModBlocks.BOSS_DEN.get().defaultBlockState().setValue(com.thesift.block.BossDenBlock.BOSS, com.thesift.block.BossDenBlock.STAGE), Block.UPDATE_ALL); // RR: the Grand Stage's hidden den
        if (!(this.sift.getBlockEntity(at) instanceof BossDenBlockEntity podium)) {
            check(false, "dictator: stage den block entity");
            return;
        }
        this.sift.setChunkForced(x >> 4, z >> 4, true);
        this.dictator = podium.wake(this.sift, at, null);
        check(this.dictator != null && this.dictator.isAlive(), "dictator: the Grand Stage wakes the Dictator");
        if (this.dictator != null) {
            // a real blow, as a player would land it, takes him just under half health
            this.dictator.setHealth(this.dictator.getMaxHealth() * 0.5F + 1.0F);
            this.dictator.hurtServer(this.sift, this.sift.damageSources().generic(), 2.0F);
        }
    }

    private void checkDictator() {
        Dictator d = this.dictator;
        if (d == null) return;
        TheSift.LOGGER.info("SMOKE: dictator alive={} phase={} health={}", d.isAlive(), d.getPhase(), d.getHealth());
        check(d.isAlive() && d.getPhase() == 2, "dictator: at half health he enters his second phase");
        d.discard();
    }

    private void checkHarmoners() {
        int found = 0;
        for (int v = 0; v < Harmoner.VARIANTS; v++) {
            Harmoner h = ModEntities.HARMONER.get().create(this.sift, EntitySpawnReason.COMMAND);
            if (h == null) {
                check(false, "harmoner: created");
                return;
            }
            h.setVariant(v);
            h.snapTo(8.5, this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, 8, 8) + 2, 8.5, 0.0F, 0.0F);
            boolean ok = h.startGuiding(this.sift, null);
            TheSift.LOGGER.info("SMOKE: harmoner {} -> {} at {}", Harmoner.NAMES[v], Harmoner.STRUCTURES[v], h.getGuideTarget());
            if (ok && h.isGuiding() && h.getGuideTarget() != null) {
                found++;
            }
            h.discard();
        }
        check(found >= 4, "harmoner: fed Harmoners find the structures of their colour (" + found + "/" + Harmoner.VARIANTS + ")");
    }

    /** A vanilla Sniffer digging in The Sift turns up the dimension's own seeds (the loot modifier on sniffer_digging). */
    private void checkSniffer() {
        net.minecraft.world.entity.animal.sniffer.Sniffer s = EntityTypes.SNIFFER.create(this.sift, EntitySpawnReason.COMMAND);
        if (s == null) {
            check(false, "sniffer: created");
            return;
        }
        s.snapTo(0.5, 100.0, 0.5, 0.0F, 0.0F);
        List<ItemStack> dug = new java.util.ArrayList<>();
        for (int i = 0; i < 24; i++) {
            s.dropFromGiftLootTable(this.sift, net.minecraft.world.level.storage.loot.BuiltInLootTables.SNIFFER_DIGGING, (l, stack) -> dug.add(stack));
        }
        boolean siftSeed = dug.stream().anyMatch(st -> st.is(ModItems.ECHO_SEED.get()) || st.is(net.minecraft.world.item.Items.PITCHER_POD)
                || st.is(net.minecraft.world.item.Items.TORCHFLOWER_SEEDS));
        TheSift.LOGGER.info("SMOKE: a Sniffer digging in The Sift found {}", dug);
        check(siftSeed, "sniffer: vanilla Sniffers dig up Sift seeds in The Sift");
        s.discard();
    }

    /**
     * E1 Sniffer &amp; rot: a Zombified (vanilla) Sniffer blooms into a Sift Sniffer in the Sift; a Sift creature in the
     * Overworld rots (lifeless, no band) and heals again back in the Sift; a Sift Sniffer rots all the way into a
     * Zombified Sniffer; a Sift Sniffer digs up something from its loot table.
     */
    private void checkRot() {
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, 4, -20) + 1;
        net.minecraft.world.entity.animal.sniffer.Sniffer zombified = EntityTypes.SNIFFER.create(this.sift, EntitySpawnReason.COMMAND);
        if (zombified == null) {
            check(false, "rot: sniffer created");
            return;
        }
        zombified.snapTo(4.5, y, -19.5, 0.0F, 0.0F);
        this.sift.addFreshEntity(zombified);
        com.thesift.entity.SiftRot.step(zombified, this.sift);
        List<com.thesift.entity.SiftSniffer> bloomed = this.sift.getEntitiesOfClass(com.thesift.entity.SiftSniffer.class,
                zombified.getBoundingBox().inflate(3.0));
        check(zombified.isRemoved() && !bloomed.isEmpty(), "rot: a Zombified Sniffer turns into a Sift Sniffer in the Sift");
        for (com.thesift.entity.SiftSniffer s : bloomed) {
            List<ItemStack> dug = new ArrayList<>();
            s.dropFromGiftLootTable(this.sift, com.thesift.registry.ModSiftSniffer.DIGGING_LOOT, (l, stack) -> dug.add(stack));
            check(!dug.isEmpty(), "rot: a Sift Sniffer digs something up");
            s.discard();
        }
        // a Bulb in the Overworld rots, stops playing music, then heals at home
        com.thesift.entity.Bulb bulb = ModEntities.BULB.get().create(this.overworld, EntitySpawnReason.COMMAND);
        if (bulb == null) {
            check(false, "rot: bulb created");
            return;
        }
        bulb.snapTo(0.5, 200.0, 0.5, 0.0F, 0.0F);
        for (int i = 0; i < com.thesift.entity.SiftRot.ROTTEN; i++) {
            com.thesift.entity.SiftRot.step(bulb, this.overworld);
        }
        TheSift.LOGGER.info("SMOKE: a Bulb two minutes in the Overworld has rot {}", com.thesift.entity.SiftRot.rot(bulb));
        check(com.thesift.entity.SiftRot.isRotten(bulb), "rot: a Sift creature rots outside the Sift");
        int rotten = com.thesift.entity.SiftRot.rot(bulb);
        for (int i = 0; i < 10; i++) {
            com.thesift.entity.SiftRot.step(bulb, this.sift);
        }
        check(com.thesift.entity.SiftRot.rot(bulb) < rotten, "rot: a rotting creature heals in the Sift");
        bulb.discard();
        // a Sift Sniffer at the end of its rot becomes a Zombified Sniffer
        com.thesift.entity.SiftSniffer sniffer = com.thesift.registry.ModSiftSniffer.SIFT_SNIFFER.get().create(this.overworld, EntitySpawnReason.COMMAND);
        if (sniffer == null) {
            check(false, "rot: sift sniffer created");
            return;
        }
        sniffer.snapTo(0.5, 200.0, 0.5, 0.0F, 0.0F);
        sniffer.setData(com.thesift.registry.ModSiftSniffer.ROT, com.thesift.entity.SiftRot.FULL - 1);
        com.thesift.entity.SiftRot.step(sniffer, this.overworld);
        check(sniffer.isRemoved(), "rot: a Sift Sniffer rots into a Zombified Sniffer outside the Sift");
        for (Entity e : this.overworld.getEntitiesOfClass(net.minecraft.world.entity.animal.sniffer.Sniffer.class, sniffer.getBoundingBox().inflate(3.0))) {
            e.discard();
        }
    }

    /** M3 Echoer: every song's gift table (and the Nibs' treasure) must give something. */
    private void checkTrades() {
        Enchoer e = ModEntities.ENCHOER.get().create(this.sift, EntitySpawnReason.COMMAND);
        if (e == null) {
            check(false, "echoer: created");
            return;
        }
        e.snapTo(0.5, 100.0, 0.5, 0.0F, 0.0F);
        List<ItemStack> got = new ArrayList<>();
        int songs = 0;
        for (com.thesift.music.Song song : com.thesift.music.Song.values()) {
            int before = got.size();
            e.dropFromGiftLootTable(this.sift, com.thesift.registry.ModEchoer.giftTable(song), (l, stack) -> got.add(stack));
            songs += got.size() > before ? 1 : 0;
        }
        for (int i = 0; i < 16; i++) {
            e.dropFromGiftLootTable(this.sift, com.thesift.registry.ModEchoer.NIB_TRANSFORM_LOOT, (l, stack) -> got.add(stack));
        }
        TheSift.LOGGER.info("SMOKE: echoer rewards and nib treasure {}", got);
        check(songs == com.thesift.music.Song.values().length, "echoer: every song's gift table gives a gift (" + songs + ")");
        check(got.size() >= songs + 16, "echoer: the gift and nib transform loot tables give items");
        // CAVE v4: the Soul Golem digs up something from every kind of ground (topsoil, deep, deepslate, cavern, sculk, sands)
        int digs = 0;
        for (var table : com.thesift.registry.ModEchoer.GOLEM_DIG_TABLES) {
            List<ItemStack> dug = new ArrayList<>();
            e.dropFromGiftLootTable(this.sift, table, (l, stack) -> dug.add(stack));
            digs += dug.isEmpty() ? 0 : 1;
        }
        TheSift.LOGGER.info("SMOKE: soul golem dig tables giving finds: {}/{}", digs, com.thesift.registry.ModEchoer.GOLEM_DIG_TABLES.size());
        check(digs == com.thesift.registry.ModEchoer.GOLEM_DIG_TABLES.size(), "soul golem: every dig table gives a find (" + digs + ")");
        e.discard();
    }

    private void startRitual() {
        Optional<StructureTemplate> city = this.overworld.getStructureTemplateManager()
                .get(Identifier.withDefaultNamespace("ancient_city/city_center/city_center_1"));
        check(city.isPresent(), "ritual: ancient city template");
        if (city.isEmpty()) {
            this.stage = Stage.DONE;
            return;
        }
        BlockPos origin = new BlockPos(-600, 120, -600);
        Vec3i size = city.get().getSize();
        // keep the city (and the drum's block entity) ticking with no player around
        for (int cx = origin.getX() >> 4; cx <= (origin.getX() + size.getX()) >> 4; cx++) {
            for (int cz = origin.getZ() >> 4; cz <= (origin.getZ() + size.getZ()) >> 4; cz++) {
                this.overworld.setChunkForced(cx, cz, true);
            }
        }
        city.get().placeInWorld(this.overworld, origin, origin, new StructurePlaceSettings(), this.overworld.getRandom(), Block.UPDATE_CLIENTS);
        long sx = 0, sy = 0, sz = 0, n = 0;
        for (BlockPos q : BlockPos.betweenClosed(origin, origin.offset(size).offset(-1, -1, -1))) {
            if (this.overworld.getBlockState(q).is(Blocks.REINFORCED_DEEPSLATE)) {
                sx += q.getX();
                sy += q.getY();
                sz += q.getZ();
                n++;
            }
        }
        if (n == 0) {
            check(false, "ritual: city has a reinforced deepslate gate");
            this.stage = Stage.DONE;
            return;
        }
        this.gate = PortalFrames.find(this.overworld, new BlockPos((int) (sx / n), (int) (sy / n), (int) (sz / n)), 20);
        check(this.gate != null, "ritual: overworld ancient city gate found");
        if (this.gate == null) {
            this.stage = Stage.DONE;
            return;
        }
        // MANSION: the summoner (a Sculk Catalyst) stands on the floor a few blocks in front of the gate, four sensors round it
        Direction.Axis axis = this.gate.axis();
        int minY = this.gate.interior().stream().mapToInt(BlockPos::getY).min().orElse(this.gate.center().getY());
        Direction out = axis == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        Direction side = out.getClockWise();
        BlockPos at = new BlockPos(this.gate.center().getX(), minY, this.gate.center().getZ()).relative(out, 3);
        for (BlockPos p : BlockPos.betweenClosed(at.relative(side, -2).below(), at.relative(side, 2).relative(out, 2).above(2))) {
            this.overworld.setBlock(p, p.getY() < at.getY() ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        this.overworld.setBlock(at, Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos sensorPos : new BlockPos[] {at.relative(side, 2), at.relative(side, -2), at.relative(out, 2), at.relative(side, 2).relative(out, 2)}) {
            this.overworld.setBlock(sensorPos, Blocks.SCULK_SENSOR.defaultBlockState(), Block.UPDATE_ALL);
        }
        this.player = FakePlayerFactory.get(this.overworld, new com.mojang.authlib.GameProfile(
                UUID.fromString("5f2b6c1d-7a3e-4b9f-8c2d-1e3f4a5b6c7d"), "[SiftSummoner]"));
        this.player.snapTo(at.relative(out, 1).relative(side, -1).getX() + 0.5, at.getY(), at.relative(out, 1).relative(side, -1).getZ() + 0.5, 0.0F, 0.0F);
        this.player.getInventory().clearContent();
        boolean armed = SculkSummonerBlock.arm(this.overworld, at, this.player);
        check(armed, "summoner: a Warden Core arms the Sculk Catalyst");
        if (!armed || !(this.overworld.getBlockEntity(at) instanceof SculkSummonerBlockEntity s)) {
            check(false, "summoner: block entity");
            this.stage = Stage.DONE;
            return;
        }
        this.summoner = s;
        this.summonerPos = at;
        SculkSummonerBlockEntity.SummonerState st = s.summonerState();
        TheSift.LOGGER.info("SMOKE: summoner at {} gate {} cells {} axis {} state {}", at, this.gate.center(), this.gate.interior().size(), axis, st);
        check(st.sensors() >= SculkSummonerBlockEntity.SENSORS && st.frame(), "summoner: finds its four sensors and the gate (" + st + ")");
        check(this.overworld.getBlockState(at).getValue(SculkSummonerBlock.READY), "summoner: blooms (ready) with its sensors and gate");
        // the instrument: any will do - a Conga Drum
        this.player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(ModItems.CONGA_DRUM.get()));
    }

    private void startGlowball() {
        int x = 5, z = 3;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (BlockPos p : BlockPos.betweenClosed(x - 2, y - 1, z - 2, x + 2, y - 1, z + 2)) {
            this.sift.setBlock(p, ModBlocks.DREAMSTONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : BlockPos.betweenClosed(x - 2, y, z - 2, x + 2, y + 6, z + 2)) {
            this.sift.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        Warden w = EntityTypes.WARDEN.create(this.sift, EntitySpawnReason.COMMAND);
        if (w == null) {
            check(false, "glowball: warden created");
            return;
        }
        w.snapTo(x + 0.5, y, z + 0.5, 0.0F, 0.0F);
        w.setNoAi(true);
        w.setPersistenceRequired();
        this.sift.addFreshEntity(w);
        this.warden = w;
        GlowballEntity ball = new GlowballEntity(this.sift, x + 0.5, y + 5.5, z + 0.5, GlowballEntity.defaultStack());
        ball.setDeltaMovement(0.0, -0.9, 0.0);
        ball.setPower(1.0F);
        check(this.sift.addFreshEntity(ball), "glowball: projectile added");
    }

    private void startChromeSoak() {
        int x = 3, z = 13;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 2;
        BlockState rim = ModBlocks.DREAMSTONE_BRICKS.get().defaultBlockState();
        // a walled 3x3 pool, two deep, so the Bulb can't hop out
        for (BlockPos p : BlockPos.betweenClosed(x - 2, y - 1, z - 2, x + 2, y + 3, z + 2)) {
            this.sift.setBlock(p, rim, Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : BlockPos.betweenClosed(x - 1, y + 2, z - 1, x + 1, y + 3, z + 1)) {
            this.sift.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockState chrome = ModBlocks.CHROME.get().defaultBlockState();
        for (BlockPos p : BlockPos.betweenClosed(x - 1, y, z - 1, x + 1, y + 1, z + 1)) {
            this.sift.setBlock(p, chrome, Block.UPDATE_ALL);
        }
        Mob bulb = ModEntities.BULB.get().create(this.sift, EntitySpawnReason.COMMAND);
        if (bulb == null) {
            check(false, "chrome: bulb created");
            return;
        }
        bulb.snapTo(x + 0.5, y + 0.2, z + 0.5, 0.0F, 0.0F);
        bulb.setPersistenceRequired();
        bulb.setHealth(2.0F);
        this.sift.addFreshEntity(bulb);
        this.soaker = bulb;
        this.soakerStartHealth = bulb.getHealth();
    }

    // ------------------------------------------------------------------ ticking

    void tick() {
        this.ticks++;
        if (this.ticks == 60) {
            this.checkGlowball();
        }
        if (this.ticks == 200) {
            this.checkChromeSoak();
            this.checkDictator();
        }
        if (this.europhy != null) {
            this.europhy.tick(this.ticks); // F1: play the tune at 40, check the ingot at 320
        }
        if (this.drillTest != null) {
            this.drillTest.tick(this.ticks); // RR: beats at 10-20, the tunnels checked at 160
        }
        if (this.ticks == 260 && this.songTest != null) {
            this.songTest.finish(); // C4 songs
        }
        if (this.bandTest != null) {
            this.bandTest.tick(); // M2 band
        }
        switch (this.stage) {
            case RITUAL -> this.tickRitual();
            case PIG -> this.tickPig();
            default -> {}
        }
        if (this.ticks > 2400 && this.stage != Stage.DONE) {
            check(false, "mechanics test timed out in stage " + this.stage + " after phases " + this.phases);
            this.stage = Stage.DONE;
        }
    }

    private void tickRitual() {
        SculkSummonerBlockEntity s = this.summoner;
        FakePlayer p = this.player;
        if (s == null || p == null || this.gate == null) {
            this.stage = Stage.DONE;
            return;
        }
        this.ritualTicks++;
        long now = this.overworld.getGameTime();
        if (s.isRemoved() || !this.overworld.getBlockState(this.summonerPos).is(ModBlocks.SCULK_SUMMONER.get())) {
            this.finishRitual();
            return;
        }
        SculkSummonerBlockEntity.SummonerState st = s.summonerState();
        if (!st.phase().equals(this.lastPhase)) {
            this.lastPhase = st.phase();
            this.phases.add(st.phase() + "@" + this.ritualTicks);
            TheSift.LOGGER.info("SMOKE: summoner phase {} at tick {} (lit {})", st.phase(), this.ritualTicks, st.lit());
        }
        this.maxLit = Math.max(this.maxLit, st.lit());
        com.thesift.music.Song symphony = com.thesift.music.Song.SYMPHONY;
        int[] notes = symphony.notes();
        if (this.ritualTicks == 10) {
            // without the sheet the sculk does not know the song: nothing lights
            for (int n : new int[]{notes[0], notes[1], notes[2]}) {
                com.thesift.music.InstrumentPlay.serverPlay(p, net.minecraft.world.InteractionHand.MAIN_HAND, n, -1, now, com.thesift.music.Notes.HEARD);
            }
            this.litWithoutSheet = s.summonerState().lit();
            com.thesift.music.SongTracker.forget(p);
            p.getInventory().add(new ItemStack(ModItems.MUSIC_SHEET_SYMPHONY.get()));
        }
        if (this.ritualTicks >= 30 && this.nextNote < notes.length && this.ritualTicks % 4 == 0 && st.phase().equals("IDLE")) {
            boolean sounded = com.thesift.music.InstrumentPlay.serverPlay(p, net.minecraft.world.InteractionHand.MAIN_HAND, notes[this.nextNote], -1, now,
                    com.thesift.music.Notes.HEARD);
            check(sounded, "summoner: the drum sounds note " + notes[this.nextNote]);
            this.nextNote++;
            int lit = s.summonerState().lit();
            if (this.nextNote < notes.length) {
                check(lit == this.nextNote, "summoner: the sensors light note by note (" + lit + " after " + this.nextNote + ")");
            }
        }
    }

    private void finishRitual() {
        PortalFrames.Frame g = this.gate;
        long lit = g.interior().stream().filter(q -> this.overworld.getBlockState(q).is(ModBlocks.SIFT_PORTAL.get())).count();
        TheSift.LOGGER.info("SMOKE: ritual finished, phases {} portal blocks {}/{} lit {} without sheet {}", this.phases, lit, g.interior().size(),
                this.maxLit, this.litWithoutSheet);
        check(this.litWithoutSheet == 0, "summoner: without the sheet the Symphony lights nothing");
        check(this.maxLit >= com.thesift.music.Song.SYMPHONY.length() - 1, "summoner: the sensors lit the Symphony note by note");
        check(this.phases.stream().anyMatch(q -> q.startsWith("OPENING")), "summoner: the Sift Symphony opens the gate");
        check(lit == g.interior().size(), "ritual: the whole gate is filled with portal");
        check(this.overworld.getBlockState(this.summonerPos).is(Blocks.SCULK_CATALYST), "summoner: the core is spent (a plain catalyst again)");
        // send a pig through
        BlockPos c = g.center();
        BlockPos spot = g.interior().stream()
                .min(Comparator.comparingInt((BlockPos p) -> p.getY()).thenComparingDouble(p -> p.distSqr(new BlockPos(c.getX(), p.getY(), c.getZ()))))
                .orElse(c);
        Entity e = EntityTypes.PIG.create(this.overworld, EntitySpawnReason.COMMAND);
        if (e == null) {
            check(false, "portal: pig created");
            this.stage = Stage.DONE;
            return;
        }
        e.snapTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, 0.0F, 0.0F);
        if (e instanceof Animal animal) {
            animal.setPersistenceRequired();
        }
        this.overworld.addFreshEntity(e);
        this.pig = e.getUUID();
        this.stage = Stage.PIG;
        TheSift.LOGGER.info("SMOKE: pig sent into the gate at {}", spot);
    }

    private void tickPig() {
        this.pigTicks++;
        Entity inSift = this.pig == null ? null : this.sift.getEntity(this.pig);
        if (inSift != null) {
            BlockPos at = inSift.blockPosition();
            boolean portalHere = false;
            for (BlockPos p : BlockPos.betweenClosed(at.offset(-3, -2, -3), at.offset(3, 4, 3))) {
                if (this.sift.getBlockState(p).is(ModBlocks.SIFT_PORTAL.get())) {
                    portalHere = true;
                    break;
                }
            }
            TheSift.LOGGER.info("SMOKE: pig arrived in The Sift at {} after {} ticks (arrival portal {})", at, this.pigTicks, portalHere);
            check(portalHere, "portal: an arrival portal was built in The Sift");
            this.stage = Stage.DONE;
        } else if (this.pigTicks > 200) {
            Entity still = this.pig == null ? null : this.overworld.getEntity(this.pig);
            check(false, "portal: pig reached The Sift (still in overworld: " + (still != null ? still.blockPosition() : "gone") + ")");
            this.stage = Stage.DONE;
        }
    }

    private void checkGlowball() {
        Warden w = this.warden;
        if (w == null) return;
        boolean deaf = w.hasEffect(ModEffects.DEAFENED);
        int glow = 0;
        for (BlockPos p : BlockPos.betweenClosed(w.blockPosition().offset(-4, -2, -4), w.blockPosition().offset(4, 6, 4))) {
            if (this.sift.getBlockState(p).is(ModBlocks.LINGERING_GLOW.get())) glow++;
        }
        TheSift.LOGGER.info("SMOKE: glowball hit warden: deafened={} lingering glow blocks={}", deaf, glow);
        check(deaf, "glowball: the warden is Deafened");
        check(glow > 0, "glowball: lingering light left behind");
        w.discard();
    }

    private void checkChromeSoak() {
        Mob m = this.soaker;
        if (m == null) return;
        TheSift.LOGGER.info("SMOKE: bulb soaking in chrome: health {} -> {} at {}", this.soakerStartHealth, m.getHealth(), m.blockPosition());
        // A3 Chrome: Chrome no longer heals; it leaves you Rainbow Dazed
        check(m.isAlive() && m.hasEffect(com.thesift.registry.ModChrome.RAINBOW_DAZE), "chrome: soaking dazes");
        check(m.getHealth() <= this.soakerStartHealth, "chrome: soaking no longer heals");
    }
}
