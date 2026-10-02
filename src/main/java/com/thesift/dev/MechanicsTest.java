package com.thesift.dev;

import com.thesift.TheSift;
import com.thesift.block.SiftDrumBlock;
import com.thesift.block.entity.EuphoryAltarBlockEntity;
import com.thesift.block.entity.SiftDrumBlockEntity;
import com.thesift.entity.Enchoer;
import com.thesift.entity.GlowballEntity;
import com.thesift.entity.Harmoner;
import com.thesift.entity.boss.Dictator;
import com.thesift.block.entity.ConductorsPodiumBlockEntity;
import com.thesift.portal.PortalFrames;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import java.util.ArrayList;
import java.util.Arrays;
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
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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
 * Gameplay checks for the CI smoke test: plays the Warden Core rhythm ritual at a real Ancient City
 * gate in the Overworld (including one deliberate mistake), walks a pig through the portal it
 * opens, flings a Glowball at a Warden and soaks a hurt Bulb in Chrome (it comes out Rainbow Dazed, not healed).
 */
final class MechanicsTest {
    private enum Stage { RITUAL, PIG, DONE }

    private final MinecraftServer server;
    private final ServerLevel overworld;
    private final ServerLevel sift;
    private final BiConsumer<Boolean, String> check;
    private Stage stage = Stage.RITUAL;
    private int ticks;

    private @Nullable SiftDrumBlockEntity drum;
    private @Nullable FakePlayer player;
    private PortalFrames.@Nullable Frame gate;
    private final List<String> phases = new ArrayList<>();
    private String lastPhase = "";
    private long[] hits = new long[0];
    private int nextHit;
    private boolean mistakeMade;
    private @Nullable UUID pig;
    private int pigTicks;

    private @Nullable EuphoryAltarBlockEntity altar;
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
        this.startAltar();
        this.checkTrades();
        this.checkHarmoners();
        this.checkSniffer();
        this.startDictator();
    }

    private @Nullable Dictator dictator;

    /** A podium wakes the Dictator; at half health he should move on to his second phase. */
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
        this.sift.setBlock(at, ModBlocks.CONDUCTORS_PODIUM.get().defaultBlockState(), Block.UPDATE_ALL);
        if (!(this.sift.getBlockEntity(at) instanceof ConductorsPodiumBlockEntity podium)) {
            check(false, "dictator: podium block entity");
            return;
        }
        this.sift.setChunkForced(x >> 4, z >> 4, true);
        this.dictator = podium.wake(this.sift, at, null);
        check(this.dictator != null && this.dictator.isAlive(), "dictator: the podium wakes the Dictator");
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
        boolean siftSeed = dug.stream().anyMatch(st -> st.is(ModItems.CHOIR_POD.get()) || st.is(ModItems.ECHO_SEED.get()) || st.is(ModItems.PITCHER_BULB.get()));
        TheSift.LOGGER.info("SMOKE: a Sniffer digging in The Sift found {}", dug);
        check(siftSeed, "sniffer: vanilla Sniffers dig up Sift seeds in The Sift");
        s.discard();
    }

    /** A2 Echoer: the Echoer no longer trades - its ceremony reward table must give something. */
    private void checkTrades() {
        Enchoer e = ModEntities.ENCHOER.get().create(this.sift, EntitySpawnReason.COMMAND);
        if (e == null) {
            check(false, "echoer: created");
            return;
        }
        e.snapTo(0.5, 100.0, 0.5, 0.0F, 0.0F);
        List<ItemStack> got = new ArrayList<>();
        for (int i = 0; i < 16; i++) {
            e.dropFromGiftLootTable(this.sift, com.thesift.registry.ModEchoer.ECHOER_REWARD, (l, stack) -> got.add(stack));
            e.dropFromGiftLootTable(this.sift, com.thesift.registry.ModEchoer.NIB_TRANSFORM_LOOT, (l, stack) -> got.add(stack));
        }
        TheSift.LOGGER.info("SMOKE: echoer rewards and nib treasure {}", got);
        check(got.size() >= 32, "echoer: the offering reward and nib transform loot tables give items");
        e.discard();
    }

    private void startAltar() {
        int x = 22, z = 12;
        int y = this.sift.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 3;
        for (BlockPos p : BlockPos.betweenClosed(x - 4, y - 1, z - 4, x + 4, y - 1, z + 4)) {
            this.sift.setBlock(p, ModBlocks.POLISHED_DREAMSTONE.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        for (BlockPos p : BlockPos.betweenClosed(x - 4, y, z - 4, x + 4, y + 3, z + 4)) {
            this.sift.setBlock(p, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);
        }
        BlockPos altarPos = new BlockPos(x, y, z);
        this.sift.setBlock(altarPos, ModBlocks.EUPHORY_ALTAR.get().defaultBlockState(), Block.UPDATE_ALL);
        for (int i = 0; i < 6; i++) {
            double a = i * Math.PI / 3.0;
            this.sift.setBlock(altarPos.offset((int) Math.round(Math.cos(a) * 3), 0, (int) Math.round(Math.sin(a) * 3)),
                    ModBlocks.SIFT_DRUM.get().defaultBlockState(), Block.UPDATE_ALL);
        }
        if (!(this.sift.getBlockEntity(altarPos) instanceof EuphoryAltarBlockEntity a)) {
            check(false, "altar: block entity");
            return;
        }
        FakePlayer enchanter = FakePlayerFactory.getMinecraft(this.sift);
        enchanter.getAbilities().instabuild = true;
        a.use(enchanter, InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
        check(a.getItem().is(Items.DIAMOND_SWORD), "altar: sword placed on the altar");
        a.use(enchanter, InteractionHand.MAIN_HAND, new ItemStack(ModItems.CHROME_PEARL.get()));
        check(a.isRitualActive(), "altar: a chrome pearl starts the drum ritual");
        this.altar = a;
    }

    private void checkAltar() {
        EuphoryAltarBlockEntity a = this.altar;
        if (a == null) return;
        ItemStack out = a.getItem();
        TheSift.LOGGER.info("SMOKE: altar finished: active={} item={} enchantments={}", a.isRitualActive(), out, out.getEnchantments());
        check(!a.isRitualActive() && out.isEnchanted(), "altar: the sword comes out enchanted");
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
        // The drum stands on the floor a few blocks in front of the gate, ringed by sculk sensors.
        Direction.Axis axis = this.gate.axis();
        int minY = this.gate.interior().stream().mapToInt(BlockPos::getY).min().orElse(this.gate.center().getY());
        Direction out = axis == Direction.Axis.Z ? Direction.EAST : Direction.SOUTH;
        Direction side = out.getClockWise();
        BlockPos drumPos = new BlockPos(this.gate.center().getX(), minY, this.gate.center().getZ()).relative(out, 3);
        for (BlockPos p : BlockPos.betweenClosed(drumPos.relative(side, -2).below(), drumPos.relative(side, 2).relative(out, 1).above(2))) {
            this.overworld.setBlock(p, p.getY() < drumPos.getY() ? Blocks.DEEPSLATE_TILES.defaultBlockState() : Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        this.overworld.setBlock(drumPos, ModBlocks.SIFT_DRUM.get().defaultBlockState(), Block.UPDATE_ALL);
        for (BlockPos s : new BlockPos[] {drumPos.relative(side, 2), drumPos.relative(side, -2), drumPos.relative(out, 1)}) {
            this.overworld.setBlock(s, Blocks.SCULK_SENSOR.defaultBlockState(), Block.UPDATE_ALL);
        }
        if (!(this.overworld.getBlockEntity(drumPos) instanceof SiftDrumBlockEntity d)) {
            check(false, "ritual: drum block entity");
            this.stage = Stage.DONE;
            return;
        }
        this.drum = d;
        this.player = FakePlayerFactory.getMinecraft(this.overworld);
        this.player.snapTo(drumPos.getX() + 0.5, drumPos.getY(), drumPos.getZ() + 2.5, 0.0F, 0.0F);
        boolean inserted = d.tryInsertCore(this.overworld, this.player);
        check(inserted, "ritual: warden core accepted by the drum");
        TheSift.LOGGER.info("SMOKE: ritual drum at {} gate {} cells {} axis {}", drumPos, this.gate.center(), this.gate.interior().size(), axis);
        if (!inserted) {
            this.stage = Stage.DONE;
        }
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
        if (this.ticks == 320) {
            this.checkAltar();
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
        SiftDrumBlockEntity d = this.drum;
        if (d == null || this.player == null || this.gate == null) {
            this.stage = Stage.DONE;
            return;
        }
        SiftDrumBlockEntity.RitualState st = d.ritualState();
        long now = this.overworld.getGameTime();
        if (!st.phase().equals(this.lastPhase)) {
            this.lastPhase = st.phase();
            this.phases.add(st.phase() + "@r" + st.round());
            TheSift.LOGGER.info("SMOKE: ritual phase {} round {} pattern {}", st.phase(), st.round(), Arrays.toString(st.pattern()));
            if (st.phase().equals("ANSWER")) {
                // answer the call: a downbeat, then one hit per interval of the pattern
                int[] pattern = st.pattern();
                this.hits = new long[pattern.length + 1];
                this.hits[0] = now + 4;
                for (int i = 1; i <= pattern.length; i++) {
                    this.hits[i] = this.hits[i - 1] + pattern[i - 1];
                }
                if (!this.mistakeMade) {
                    // the very first answer comes in late on purpose: the sculk should shriek and the round restart
                    this.mistakeMade = true;
                    this.hits[1] += 8;
                }
                this.nextHit = 0;
            }
            if (st.phase().equals("IDLE")) {
                this.finishRitual(d);
                return;
            }
        }
        if (st.phase().equals("ANSWER") && this.nextHit < this.hits.length && now >= this.hits[this.nextHit]) {
            SiftDrumBlock.beat(this.overworld, d.getBlockPos(), 0.8F);
            d.onPlayerBeat(this.player);
            this.nextHit++;
        }
    }

    private void finishRitual(SiftDrumBlockEntity d) {
        PortalFrames.Frame g = this.gate;
        long lit = g.interior().stream().filter(p -> this.overworld.getBlockState(p).is(ModBlocks.SIFT_PORTAL.get())).count();
        TheSift.LOGGER.info("SMOKE: ritual finished, phases {} portal blocks {}/{}", this.phases, lit, g.interior().size());
        check(this.phases.stream().anyMatch(p -> p.startsWith("FAILED")), "ritual: a late answer fails the round");
        check(this.phases.stream().anyMatch(p -> p.startsWith("OPENING")), "ritual: three rounds open the gate");
        check(lit == g.interior().size(), "ritual: the whole gate is filled with portal");
        check(!d.getBlockState().getValue(SiftDrumBlock.CORE), "ritual: drum releases the spent core");
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
