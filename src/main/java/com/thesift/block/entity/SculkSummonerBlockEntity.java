package com.thesift.block.entity;

import com.thesift.block.SculkSummonerBlock;
import com.thesift.music.Song;
import com.thesift.music.SongTracker;
import com.thesift.portal.GateAwakening;
import com.thesift.portal.PortalFrames;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModMansion;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * MANSION: the Sculk Summoner - a Warden Core in a vanilla Sculk Catalyst, the new way to wake a Sift gate (it replaces
 * the drum's rhythm ritual).
 *
 * <ol>
 *   <li>A Warden Core used on a Sculk Catalyst arms it ({@link SculkSummonerBlock#arm}): the core settles in the
 *   catalyst's claws and beats.</li>
 *   <li>It needs {@link #SENSORS} Sculk Sensors (normal or calibrated) within {@link #SENSOR_RANGE} blocks
 *   (and {@link #SENSOR_HEIGHT} up or down), and a Sift gate frame within {@link #FRAME_RANGE} blocks. With both it is
 *   READY: the catalyst blooms. Use it (empty hand) to hear what it still needs.</li>
 *   <li>Carrying the sheet, play the Sift Symphony on any instrument within {@link #REACH} blocks: the song tracker follows
 *   the notes, and the sensors light up one by one with them (a slip and the sculk loses the tune).</li>
 *   <li>The last note wakes the gate (see {@link GateAwakening}): the rim lights up note by note, souls stream in from the
 *   sensors and the core, the portal closes ring by ring from the rim inward, and the core is spent - the summoner
 *   sinks back into a plain Sculk Catalyst. The opening is synced to clients for the cinematic camera.</li>
 * </ol>
 */
public class SculkSummonerBlockEntity extends BlockEntity {
    public static final int SENSORS = 4;
    public static final int SENSOR_RANGE = 8;
    public static final int SENSOR_HEIGHT = 4;
    public static final int FRAME_RANGE = 16;
    /** How far from the summoner a played note is heard (blocks). */
    public static final int REACH = 10;
    /** The vibration frequency the sensors light with (a note block's). */
    private static final int NOTE_FREQUENCY = 10;
    /** Even the smallest gate closes in at least this many steps, a chime each. */
    private static final int MIN_STEPS = 6;
    private static final long HINT_TICKS = 60L;

    private enum Phase { IDLE, OPENING }

    private Phase phase = Phase.IDLE;
    private final List<BlockPos> sensors = new ArrayList<>();
    private PortalFrames.@Nullable Frame frame;
    private long scannedAt = -1000L;
    private long lastHint = -1000L;
    /** Notes of the Symphony lit on the sensors so far. */
    private int lit;
    /** Set when loaded from disk mid-opening; the next server tick finishes the opening. */
    private boolean resumeOpening;
    /** The core is out (taken back) or spent: nothing pops out when the block goes. */
    private boolean coreGone;

    // The opening. `opening`, `gateCentre` and `gateSpan` are synced to clients for the camera.
    private int opening = -1;
    private @Nullable Vec3 gateCentre;
    private float gateSpan = 8.0F;
    private double gateBottom;
    private Vec3 planeU = new Vec3(1.0, 0.0, 0.0);
    private List<List<Vec3>> rimGroups = List.of();
    private List<List<BlockPos>> rings = List.of();
    private int ringsPlaced;

    public SculkSummonerBlockEntity(BlockPos pos, BlockState state) {
        super(ModMansion.SCULK_SUMMONER.get(), pos, state);
    }

    // ------------------------------------------------------------------ the summoner's state

    /** A read-only snapshot, for the codex, debugging and the CI tests. */
    public record SummonerState(String phase, int sensors, boolean frame, int lit) {}

    public SummonerState summonerState() {
        return new SummonerState(this.phase.name(), this.sensors.size(), this.frame != null, this.lit);
    }

    /** Looks for the sensors and the gate again (at most once a second unless forced) and blooms when it has both. */
    public void scan(ServerLevel level, boolean force) {
        long now = level.getGameTime();
        if (!force && now - this.scannedAt < 20L) {
            return;
        }
        this.scannedAt = now;
        this.sensors.clear();
        this.sensors.addAll(findSensors(level, this.worldPosition));
        this.frame = PortalFrames.find(level, this.worldPosition, FRAME_RANGE);
        boolean ready = this.sensors.size() >= SENSORS && this.frame != null && !PortalFrames.isOpen(level, this.frame);
        BlockState state = this.getBlockState();
        if (state.getBlock() instanceof SculkSummonerBlock && state.getValue(SculkSummonerBlock.READY) != ready) {
            level.setBlock(this.worldPosition, state.setValue(SculkSummonerBlock.READY, ready), Block.UPDATE_ALL);
            if (ready) {
                // it blooms: every sensor answers, and the gate it will open shimmers
                level.playSound(null, this.worldPosition, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.0F, 0.8F);
                for (BlockPos s : this.sensors) {
                    this.activateSensor(level, s);
                }
                this.outlineGate(level, this.frame);
            }
        }
    }

    /** Just armed by a Warden Core: the core beats, the sensors answer, and the player hears what it still needs. */
    public void armed(ServerLevel level, @Nullable Player player) {
        level.playSound(null, this.worldPosition, ModMansion.SUMMONER_ARM.get(), SoundSource.BLOCKS, 2.0F, 0.8F);
        level.playSound(null, this.worldPosition, ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.BLOCKS, 2.0F, 0.8F);
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.6F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.03);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.1,
                this.worldPosition.getZ() + 0.5, 0, 4.0, 0.0, 0.0, 1.0);
        this.scan(level, true);
        for (BlockPos s : this.sensors) {
            this.activateSensor(level, s);
        }
        if (player != null) {
            this.report(level, player);
        }
        this.setChanged();
    }

    /** Tells the player what the summoner still needs (or that it is listening). */
    public void report(ServerLevel level, Player player) {
        this.scan(level, true);
        Component msg;
        if (this.frame == null) {
            msg = Component.translatable("message.thesift.summoner.no_frame");
        } else if (PortalFrames.isOpen(level, this.frame)) {
            msg = Component.translatable("message.thesift.summoner.already_open");
        } else if (this.sensors.size() < SENSORS) {
            msg = Component.translatable("message.thesift.summoner.need_sensors", this.sensors.size(), SENSORS);
        } else if (!SongTracker.carriesSheet(player, Song.SYMPHONY)) {
            msg = Component.translatable("message.thesift.summoner.need_sheet");
        } else {
            msg = Component.translatable("message.thesift.summoner.ready");
        }
        player.sendOverlayMessage(msg);
        if (this.frame != null) {
            this.outlineGate(level, this.frame);
        }
    }

    /** Takes the core back out (the catalyst stays), unless it is already pouring itself into an opening gate. */
    public boolean ejectCore(ServerLevel level) {
        if (this.phase == Phase.OPENING) {
            return false;
        }
        this.coreGone = true;
        Block.popResource(level, this.worldPosition.above(), new ItemStack(ModItems.WARDEN_CORE.get()));
        level.setBlock(this.worldPosition, Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_ALL);
        return true;
    }

    // ------------------------------------------------------------------ listening (SongEvents, hooked in ModMansion)

    /** Every note played by a player: the nearest summoner within {@link #REACH} follows the Symphony. */
    public static void hearNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        if (player == null) {
            return;
        }
        SculkSummonerBlockEntity s = nearest(level, BlockPos.containing(at));
        if (s != null) {
            s.onNote(level, player);
        }
    }

    /** A whole song: the Sift Symphony played near a ready summoner wakes its gate. */
    public static void hearSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        if (song != Song.SYMPHONY) {
            return;
        }
        SculkSummonerBlockEntity s = nearest(level, BlockPos.containing(at));
        if (s != null) {
            s.onSymphony(level, player);
        }
    }

    /** The nearest summoner within {@link #REACH} of {@code at}, or null. */
    public static @Nullable SculkSummonerBlockEntity nearest(ServerLevel level, BlockPos at) {
        SculkSummonerBlockEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-REACH, -4, -REACH), at.offset(REACH, 4, REACH))) {
            if (level.getBlockState(p).is(ModBlocks.SCULK_SUMMONER.get()) && level.getBlockEntity(p) instanceof SculkSummonerBlockEntity s) {
                double d = p.distSqr(at);
                if (d < bestDist) {
                    bestDist = d;
                    best = s;
                }
            }
        }
        return best;
    }

    private void onNote(ServerLevel level, Player player) {
        if (this.phase == Phase.OPENING) {
            return;
        }
        this.scan(level, false);
        long now = level.getGameTime();
        if (this.sensors.size() < SENSORS || this.frame == null || PortalFrames.isOpen(level, this.frame)
                || !SongTracker.carriesSheet(player, Song.SYMPHONY)) {
            if (now - this.lastHint >= HINT_TICKS) {
                this.lastHint = now;
                this.report(level, player);
            }
            return;
        }
        int progress = SongTracker.progress(player, Song.SYMPHONY, now);
        if (progress > this.lit) {
            // the next notes of the Symphony: a sensor lights up with each
            for (int i = this.lit; i < progress; i++) {
                this.flashSensor(level, i, Song.SYMPHONY.note(Math.min(i, Song.SYMPHONY.length() - 1)));
            }
            this.lit = progress;
            player.sendOverlayMessage(Component.translatable("message.thesift.summoner.note", this.lit, Song.SYMPHONY.length())
                    .withStyle(ChatFormatting.AQUA));
            this.setChanged();
        } else if (progress <= 1 && this.lit > 1) {
            // a slip: the sculk loses the tune
            this.lit = progress;
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.2F, 0.7F);
            player.sendOverlayMessage(Component.translatable("message.thesift.summoner.lost").withStyle(ChatFormatting.GRAY));
        }
    }

    private void onSymphony(ServerLevel level, @Nullable Player player) {
        if (this.phase == Phase.OPENING) {
            return;
        }
        this.scan(level, true);
        if (this.sensors.size() < SENSORS || this.frame == null || PortalFrames.isOpen(level, this.frame)) {
            if (player != null) {
                this.report(level, player);
            }
            return;
        }
        // the last sensor lights with the last note, then the gate wakes
        this.flashSensor(level, Song.SYMPHONY.length() - 1, Song.SYMPHONY.note(Song.SYMPHONY.length() - 1));
        this.lit = Song.SYMPHONY.length();
        this.beginOpening(level);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, SculkSummonerBlockEntity summoner) {
        if (level instanceof ServerLevel server) {
            summoner.tick(server);
        }
    }

    /** Client side: follows a waking gate for the camera (see {@link GateAwakening}). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SculkSummonerBlockEntity summoner) {
        if (summoner.opening < 0 || summoner.gateCentre == null) {
            return;
        }
        summoner.opening++;
        if (summoner.opening > GateAwakening.LENGTH + 20) {
            summoner.opening = -1;
            return;
        }
        GateAwakening.clientSource = pos;
        GateAwakening.clientCentre = summoner.gateCentre;
        GateAwakening.clientSpan = summoner.gateSpan;
        GateAwakening.clientTick = summoner.opening;
        GateAwakening.clientSeen = level.getGameTime();
    }

    private void tick(ServerLevel level) {
        if (this.resumeOpening) {
            // the world was saved while the gate was waking: carry on instead of leaving a half-closed gate
            this.resumeOpening = false;
            this.beginOpening(level);
            return;
        }
        if (this.phase == Phase.OPENING) {
            this.tickOpening(level);
            return;
        }
        if (level.getGameTime() % 40L == (this.worldPosition.asLong() & 31L)) {
            this.scan(level, true);
        }
        if (level.getGameTime() % 50L == 0L) {
            // the core's heartbeat
            level.playSound(null, this.worldPosition, ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.BLOCKS, 0.6F, 0.75F);
        }
    }

    // ------------------------------------------------------------------ the gate wakes

    private void beginOpening(ServerLevel level) {
        PortalFrames.Frame f = PortalFrames.find(level, this.worldPosition, FRAME_RANGE);
        if (f == null) {
            this.phase = Phase.IDLE;
            this.frame = null;
            this.message(level, "message.thesift.summoner.no_frame");
            this.setChanged();
            return;
        }
        if (this.sensors.isEmpty()) {
            this.sensors.addAll(findSensors(level, this.worldPosition));
        }
        this.frame = f;
        this.prepareGate(f);
        this.phase = Phase.OPENING;
        this.opening = 0;
        this.ringsPlaced = 0;
        this.message(level, "message.thesift.summoner.opening");
        this.sync();
    }

    /** Works out the gate's centre, its rim (grouped bottom to top) and its rings (outermost first). */
    private void prepareGate(PortalFrames.Frame f) {
        Set<BlockPos> cells = f.interior();
        Direction[] dirs = PortalFrames.planeDirections(f.axis());
        boolean alongX = f.axis() == Direction.Axis.X;
        double sx = 0, sy = 0, sz = 0;
        int minU = Integer.MAX_VALUE, maxU = Integer.MIN_VALUE, minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
        for (BlockPos b : cells) {
            sx += b.getX() + 0.5;
            sy += b.getY() + 0.5;
            sz += b.getZ() + 0.5;
            int u = alongX ? b.getX() : b.getZ();
            minU = Math.min(minU, u);
            maxU = Math.max(maxU, u);
            minY = Math.min(minY, b.getY());
            maxY = Math.max(maxY, b.getY());
        }
        int count = Math.max(1, cells.size());
        Vec3 c = new Vec3(sx / count, sy / count, sz / count);
        this.gateCentre = c;
        this.gateSpan = cells.isEmpty() ? 8.0F : Math.max(maxU - minU + 1, maxY - minY + 1);
        this.gateBottom = cells.isEmpty() ? c.y : minY + 0.05;
        this.planeU = alongX ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 0.0, 1.0);

        // the rim glows just inside the frame; it lights up from the bottom, both sides together, meeting at the top
        List<RimPoint> rim = new ArrayList<>();
        Map<BlockPos, Integer> depth = new HashMap<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        for (BlockPos b : cells) {
            boolean edge = false;
            for (Direction d : dirs) {
                if (!cells.contains(b.relative(d))) {
                    edge = true;
                    Vec3 p = Vec3.atCenterOf(b).add(d.getStepX() * 0.42, d.getStepY() * 0.42, d.getStepZ() * 0.42);
                    rim.add(new RimPoint(this.angleUp(p), p));
                }
            }
            if (edge) {
                depth.put(b, 0);
                queue.add(b);
            }
        }
        rim.sort(Comparator.comparingDouble(RimPoint::angle));
        List<List<Vec3>> groups = new ArrayList<>();
        for (int i = 0; i < GateAwakening.RIM_NOTES; i++) {
            groups.add(new ArrayList<>());
        }
        if (rim.size() >= GateAwakening.RIM_NOTES) {
            for (int i = 0; i < rim.size(); i++) {
                groups.get(Math.min(GateAwakening.RIM_NOTES - 1, i * GateAwakening.RIM_NOTES / rim.size())).add(rim.get(i).pos());
            }
        } else if (!rim.isEmpty()) {
            for (int g = 0; g < GateAwakening.RIM_NOTES; g++) {
                groups.get(g).add(rim.get(g * rim.size() / GateAwakening.RIM_NOTES).pos());
            }
        }
        this.rimGroups = groups;

        // the rings: every cell by how far in from the rim it lies
        int deepest = 0;
        while (!queue.isEmpty()) {
            BlockPos b = queue.poll();
            int next = depth.get(b) + 1;
            for (Direction d : dirs) {
                BlockPos n = b.relative(d);
                if (cells.contains(n) && !depth.containsKey(n)) {
                    depth.put(n, next);
                    deepest = Math.max(deepest, next);
                    queue.add(n);
                }
            }
        }
        List<List<BlockPos>> rs = new ArrayList<>();
        for (int i = 0; i <= deepest; i++) {
            rs.add(new ArrayList<>());
        }
        for (Map.Entry<BlockPos, Integer> e : depth.entrySet()) {
            rs.get(e.getValue()).add(e.getKey());
        }
        int perRing = Math.max(1, (MIN_STEPS + rs.size() - 1) / rs.size());
        List<List<BlockPos>> steps = new ArrayList<>();
        for (List<BlockPos> ring : rs) {
            ring.sort(Comparator.comparingDouble((BlockPos b) -> this.angleUp(Vec3.atCenterOf(b))));
            int parts = Math.min(perRing, ring.size());
            for (int p = 0; p < parts; p++) {
                steps.add(new ArrayList<>(ring.subList(p * ring.size() / parts, (p + 1) * ring.size() / parts)));
            }
        }
        this.rings = steps;
    }

    /** How far round from the bottom of the gate a point is: 0 straight below the centre, PI straight above. */
    private double angleUp(Vec3 p) {
        Vec3 c = this.gateCentre;
        if (c == null) {
            return 0.0;
        }
        double u = this.planeU.x != 0.0 ? p.x - c.x : p.z - c.z;
        return Math.abs(Math.atan2(u, c.y - p.y));
    }

    private record RimPoint(double angle, Vec3 pos) {}

    private void tickOpening(ServerLevel level) {
        PortalFrames.Frame f = this.frame;
        Vec3 c = this.gateCentre;
        if (f == null || c == null) {
            this.endOpening(level);
            return;
        }
        int t = this.opening++;
        RandomSource r = level.getRandom();
        if (t == 0) {
            this.openingBegins(level, c);
        }
        // 1. the rim lights up, a stretch and a note at a time
        int lit = t < GateAwakening.RIM_START ? 0 : Math.min(GateAwakening.RIM_NOTES, (t - GateAwakening.RIM_START) / GateAwakening.RIM_STEP + 1);
        if (t >= GateAwakening.RIM_START && (t - GateAwakening.RIM_START) % GateAwakening.RIM_STEP == 0) {
            int i = (t - GateAwakening.RIM_START) / GateAwakening.RIM_STEP;
            if (i < GateAwakening.RIM_NOTES) {
                this.lightRim(level, c, i);
            }
        }
        if (t % 3 == 0 && lit > 0) {
            for (int g = 0; g < lit; g++) {
                List<Vec3> group = this.rimGroups.get(g);
                if (group.isEmpty() || t > GateAwakening.CLIMAX && r.nextInt(3) != 0) continue;
                Vec3 p = group.get(r.nextInt(group.size()));
                level.sendParticles(r.nextInt(3) == 0 ? ModParticles.DRIFTING_SOUL.get() : ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, 1, 0.12, 0.12,
                        0.12, 0.0);
            }
        }
        // 2. souls stream in from the sensors and the core and spiral into a vortex
        if (t < GateAwakening.CLIMAX) {
            this.souls(level, r, c, t);
        }
        // 3. the surface comes together, ring by ring from the rim inward
        while (this.ringsPlaced < this.rings.size() && GateAwakening.ringTick(this.ringsPlaced, this.rings.size()) <= t
                && t < GateAwakening.CLIMAX) {
            this.placeRing(level, c, this.ringsPlaced);
            this.ringsPlaced++;
        }
        // 4. the last ring closes: the final chord
        if (t == GateAwakening.CLIMAX) {
            this.climax(level, f, c);
            if (this.phase != Phase.OPENING) {
                return;
            }
        }
        if (t >= GateAwakening.TAIL_START && (t - GateAwakening.TAIL_START) % GateAwakening.TAIL_STEP == 0) {
            int i = (t - GateAwakening.TAIL_START) / GateAwakening.TAIL_STEP;
            if (i < GateAwakening.TAIL.length) {
                GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_CHIME, GateAwakening.TAIL[i], 1.4F - i * 0.2F);
            }
        }
        if (t > GateAwakening.CLIMAX && t % 4 == 0) {
            List<BlockPos> any = this.rings.isEmpty() ? List.of() : this.rings.get(r.nextInt(this.rings.size()));
            if (!any.isEmpty()) {
                BlockPos b = any.get(r.nextInt(any.size()));
                level.sendParticles(ModParticles.DRIFTING_SOUL.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.01);
            }
        }
        if (t >= GateAwakening.LENGTH) {
            this.endOpening(level);
        }
    }

    /** The core's last, biggest beat; the sensors flare and a whoosh rises under everything. */
    private void openingBegins(ServerLevel level, Vec3 c) {
        level.playSound(null, this.worldPosition, ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.BLOCKS, 2.5F, 0.7F);
        level.playSound(null, this.worldPosition, SoundEvents.SCULK_CATALYST_BLOOM, SoundSource.BLOCKS, 2.5F, 0.6F);
        level.playSound(null, c.x, c.y, c.z, ModSounds.GATE_SWELL.get(), SoundSource.BLOCKS, 2.5F, 1.0F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                30, 0.4, 0.4, 0.4, 0.05);
        for (BlockPos s : this.sensors) {
            this.activateSensor(level, s);
        }
    }

    /** One note of the arpeggio, and the next stretch of the rim lights up with it. */
    private void lightRim(ServerLevel level, Vec3 c, int i) {
        int n = GateAwakening.ARPEGGIO[i];
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_HARP, n, 2.4F);
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_BELL, n, 0.45F);
        if (i % 4 == 0) {
            GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_BASS, GateAwakening.BASS[i / 4], 2.2F);
            GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_PLING, n, 0.8F);
            if (!this.sensors.isEmpty()) {
                this.activateSensor(level, this.sensors.get((i / 4) % this.sensors.size()));
            }
        }
        List<Vec3> group = this.rimGroups.get(i);
        for (int k = 0; k < group.size(); k++) {
            Vec3 p = group.get(k);
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, 3, 0.15, 0.15, 0.15, 0.0);
            if (k % 2 == 0) {
                level.sendParticles(ModParticles.PORTAL_SOUL.get(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.01);
            }
        }
        if (!group.isEmpty()) {
            Vec3 p = group.get(level.getRandom().nextInt(group.size()));
            level.sendParticles(ModParticles.SIFT_NOTE.get(), p.x, p.y + 0.4, p.z, 0, i / (double) GateAwakening.RIM_NOTES, 0.0, 0.0, 1.0);
        }
    }

    /** Souls stream in from every sensor and from the core and feed a three-armed vortex that tightens as the gate closes. */
    private void souls(ServerLevel level, RandomSource r, Vec3 c, int t) {
        double k = Math.min(1.0, t / (double) GateAwakening.CLIMAX);
        double radius = Math.max(0.6, this.gateSpan * 0.42 * (1.0 - 0.75 * k));
        if (t % 2 == 0) {
            for (BlockPos s : this.sensors) {
                this.stream(level, r, Vec3.atCenterOf(s).add(0.0, 0.45, 0.0), c, radius);
            }
            this.stream(level, r, Vec3.atCenterOf(this.worldPosition).add(0.0, 0.7, 0.0), c, radius);
        }
        double spin = t * (0.18 + 0.22 * k);
        for (int arm = 0; arm < 3; arm++) {
            double a = spin + arm * (Math.PI * 2.0 / 3.0);
            double cos = Math.cos(a);
            double sin = Math.sin(a);
            Vec3 p = c.add(this.planeU.scale(cos * radius)).add(0.0, sin * radius, 0.0);
            Vec3 tangent = this.planeU.scale(-sin).add(0.0, cos, 0.0);
            Vec3 inward = c.subtract(p).normalize();
            Vec3 v = tangent.scale(0.05 + 0.07 * k).add(inward.scale(0.03));
            level.sendParticles(ModParticles.PORTAL_SOUL.get(), p.x, p.y, p.z, 0, v.x, v.y, v.z, 1.0);
            if (r.nextInt(3) == 0) {
                level.sendParticles(ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    private void stream(ServerLevel level, RandomSource r, Vec3 from, Vec3 c, double radius) {
        double a = r.nextDouble() * Math.PI * 2.0;
        Vec3 target = c.add(this.planeU.scale(Math.cos(a) * radius)).add(0.0, Math.sin(a) * radius, 0.0);
        Vec3 v = target.subtract(from).scale(1.0 / 28.0);
        level.sendParticles(ModParticles.PORTAL_SOUL.get(), from.x, from.y, from.z, 0, v.x, v.y + 0.01, v.z, 1.0);
    }

    /** One ring of cyan light gathers inside the frame, with a chime (the whole portal appears at once on the climax). */
    private void placeRing(ServerLevel level, Vec3 c, int k) {
        List<BlockPos> ring = this.rings.get(k);
        for (int j = 0; j < ring.size(); j += 2) {
            BlockPos cell = ring.get(j);
            level.sendParticles(ParticleTypes.GLOW, cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5, 1, 0.25, 0.25, 0.25, 0.02);
        }
        int count = this.rings.size();
        int n = GateAwakening.ringNote(k, count);
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_CHIME, n, 2.0F);
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_HARP, n, 1.2F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 0.9F, 0.8F + 0.6F * k / Math.max(1, count));
        for (int i = 0; i < ring.size(); i++) {
            BlockPos b = ring.get(i);
            level.sendParticles(ModParticles.PORTAL_SOUL.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.03);
            if (i % 3 == 0) {
                level.sendParticles(ModParticles.STAR_SPARKLE.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 2, 0.4, 0.4, 0.4, 0.0);
            }
            if (i % 5 == 0) {
                level.sendParticles(ModParticles.SIFT_NOTE.get(), b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 0, n / 24.0, 0.0, 0.0, 1.0);
            }
        }
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, this.gateBottom, c.z, 0, 1.5 + k * 0.6, 0.0, 0.0, 1.0);
    }

    /** The gate is whole: A major, a soft boom, rings of light - and the core is spent. */
    private void climax(ServerLevel level, PortalFrames.Frame f, Vec3 c) {
        BlockPos probe = f.interior().iterator().next();
        if (!PortalFrames.flood(level, probe, f.axis()).valid()) {
            // the frame was broken while the gate closed: the membrane tears, the core survives
            for (BlockPos b : f.interior()) {
                if (level.getBlockState(b).is(ModBlocks.SIFT_PORTAL.get())) {
                    level.setBlock(b, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
                }
            }
            level.playSound(null, c.x, c.y, c.z, SoundEvents.SCULK_SHRIEKER_SHRIEK, SoundSource.BLOCKS, 2.0F, 0.6F);
            this.message(level, "message.thesift.summoner.gate_broken");
            this.phase = Phase.IDLE;
            this.opening = -1;
            this.lit = 0;
            this.rimGroups = List.of();
            this.rings = List.of();
            this.ringsPlaced = 0;
            this.sync();
            return;
        }
        PortalFrames.fill(level, f);
        this.ringsPlaced = this.rings.size();
        for (int n : GateAwakening.FINAL_CHORD) {
            GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_HARP, n, 2.5F);
        }
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_BASS, 3, 2.5F);
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_BELL, 3, 1.2F);
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_CHIME, 10, 1.0F);
        GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_FLUTE, 7, 1.2F);
        level.playSound(null, c.x, c.y, c.z, ModSounds.PORTAL_ACTIVATE.get(), SoundSource.BLOCKS, 3.0F, 1.0F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.7F, 1.6F);
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, this.gateBottom + i * 1.5, c.z, 0, 4.0 + i, 0, 0, 1.0);
        }
        RandomSource r = level.getRandom();
        // the flash: a burst of white light out of the new surface and a thunderclap
        level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.BLOCKS, 1.6F, 1.4F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.END_PORTAL_SPAWN, SoundSource.BLOCKS, 1.4F, 1.2F);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 80, this.gateSpan * 0.2, this.gateSpan * 0.2, this.gateSpan * 0.2, 0.45);
        level.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 60, this.gateSpan * 0.3, this.gateSpan * 0.3, this.gateSpan * 0.3, 0.3);
        for (BlockPos b : f.interior()) {
            level.sendParticles(ModParticles.PORTAL_SOUL.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 2, 0.3, 0.3, 0.3, 0.2);
            if (r.nextInt(3) == 0) {
                level.sendParticles(ModParticles.SIFT_NOTE.get(), b.getX() + 0.5, b.getY() + 0.5, b.getZ() + 0.5, 0, r.nextDouble(), 0, 0, 1);
            }
        }
        for (BlockPos s : this.sensors) {
            level.sendParticles(ParticleTypes.SONIC_BOOM, s.getX() + 0.5, s.getY() + 1.0, s.getZ() + 0.5, 1, 0, 0, 0, 0);
            this.activateSensor(level, s);
        }
        this.message(level, "message.thesift.summoner.opened");
        this.setChanged();
    }

    /** The gate is open: the core is spent and the summoner sinks back into a plain Sculk Catalyst. */
    private void endOpening(ServerLevel level) {
        this.phase = Phase.IDLE;
        this.opening = -1;
        this.lit = 0;
        this.rimGroups = List.of();
        this.rings = List.of();
        this.ringsPlaced = 0;
        this.sync();
        if (this.frame != null && PortalFrames.isOpen(level, this.frame)) {
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                    16, 0.3, 0.3, 0.3, 0.04);
            this.coreGone = true;
            level.setBlock(this.worldPosition, Blocks.SCULK_CATALYST.defaultBlockState(), Block.UPDATE_ALL);
        }
    }

    // ------------------------------------------------------------------ helpers

    /** Every Sculk Sensor (normal or calibrated) close enough to a summoner at {@code at} to listen for it. */
    public static List<BlockPos> findSensors(Level level, BlockPos at) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-SENSOR_RANGE, -SENSOR_HEIGHT, -SENSOR_RANGE), at.offset(SENSOR_RANGE, SENSOR_HEIGHT, SENSOR_RANGE))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SCULK_SENSOR) || s.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
                out.add(p.immutable());
            }
        }
        out.sort(Comparator.comparingDouble((BlockPos p) -> Math.atan2(p.getZ() - at.getZ(), p.getX() - at.getX())));
        return out;
    }

    /** Really sets a sculk sensor off - its glow, its redstone pulse, its clicking - if it is resting. */
    private boolean activateSensor(ServerLevel level, BlockPos s) {
        BlockState state = level.getBlockState(s);
        if (state.getBlock() instanceof SculkSensorBlock sensor && SculkSensorBlock.canActivate(state)) {
            double distance = Math.sqrt(s.distSqr(this.worldPosition));
            int power = Math.max(1, Math.min(15, 15 - (int) Math.floor(distance * 15.0 / 9.0)));
            sensor.activate(null, level, s, state, power, NOTE_FREQUENCY);
            return true;
        }
        return false;
    }

    /** Note {@code index} of the Symphony: the next sensor round the ring lights up, with the note's own chime. */
    private void flashSensor(ServerLevel level, int index, int note) {
        if (this.sensors.isEmpty()) return;
        int size = this.sensors.size();
        BlockPos s = this.sensors.get(Math.floorMod(index, size));
        if (!this.activateSensor(level, s)) {
            level.playSound(null, s, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F, 0.8F + index * 0.08F);
        }
        GateAwakening.note(level, Vec3.atCenterOf(s), SoundEvents.NOTE_BLOCK_CHIME, note, 1.0F);
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, s.getX() + 0.5, s.getY() + 0.7, s.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), s.getX() + 0.5, s.getY() + 1.1, s.getZ() + 0.5, 0, note / 24.0, 0, 0, 1);
        // a soul arcs from the core to the sensor
        Vec3 from = Vec3.atCenterOf(this.worldPosition).add(0.0, 0.7, 0.0);
        Vec3 v = Vec3.atCenterOf(s).add(0.0, 0.5, 0.0).subtract(from).scale(1.0 / 20.0);
        level.sendParticles(ModParticles.PORTAL_SOUL.get(), from.x, from.y, from.z, 0, v.x, v.y + 0.02, v.z, 1.0);
    }

    /** A shimmer all along the frame's rim: this is the gate the summoner will open. */
    private void outlineGate(ServerLevel level, PortalFrames.@Nullable Frame f) {
        if (f == null) {
            return;
        }
        Set<BlockPos> cells = f.interior();
        for (BlockPos b : cells) {
            for (Direction d : PortalFrames.planeDirections(f.axis())) {
                if (!cells.contains(b.relative(d))) {
                    level.sendParticles(ModParticles.STAR_SPARKLE.get(), b.getX() + 0.5 + d.getStepX() * 0.42, b.getY() + 0.5 + d.getStepY() * 0.42,
                            b.getZ() + 0.5 + d.getStepZ() * 0.42, 1, 0.1, 0.1, 0.1, 0.0);
                }
            }
        }
    }

    private void message(ServerLevel level, String key) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(20))) {
            p.sendOverlayMessage(Component.translatable(key));
        }
    }

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putBoolean("Waking", this.phase == Phase.OPENING);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.resumeOpening = input.getBooleanOr("Waking", false);
        // only ever in the update tag sent to clients, never on disk
        this.opening = input.getIntOr("Opening", -1);
        double gx = input.getDoubleOr("GateX", Double.NaN);
        if (Double.isNaN(gx)) {
            this.gateCentre = null;
        } else {
            this.gateCentre = new Vec3(gx, input.getDoubleOr("GateY", 0.0), input.getDoubleOr("GateZ", 0.0));
            this.gateSpan = (float) input.getDoubleOr("GateSpan", 8.0);
        }
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = this.saveWithoutMetadata(registries);
        Vec3 c = this.gateCentre;
        boolean waking = this.phase == Phase.OPENING && c != null;
        tag.putInt("Opening", waking ? this.opening : -1);
        if (waking) {
            tag.putDouble("GateX", c.x);
            tag.putDouble("GateY", c.y);
            tag.putDouble("GateZ", c.z);
            tag.putDouble("GateSpan", this.gateSpan);
        }
        return tag;
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (this.phase == Phase.OPENING && this.frame != null && this.level instanceof ServerLevel server) {
            // the core is already spent: the gate opens anyway
            PortalFrames.fill(server, this.frame);
        } else if (this.level != null && !this.coreGone) {
            // broken while armed: the core pops out
            Block.popResource(this.level, pos, new ItemStack(ModItems.WARDEN_CORE.get()));
        }
        super.preRemoveSideEffects(pos, state);
    }
}
