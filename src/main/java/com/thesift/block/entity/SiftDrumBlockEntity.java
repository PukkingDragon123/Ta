package com.thesift.block.entity;

import com.thesift.block.SiftDrumBlock;
import com.thesift.portal.GateAwakening;
import com.thesift.portal.PortalFrames;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ShriekParticleOption;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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
 * Runs the Warden Core rhythm ritual.
 *
 * <p>Once a core is slotted in, the drum "calls": it plays a rhythm, a rising note on every beat,
 * while the nearby sculk sensors light up along. The player must answer by hitting the drum with
 * the same rhythm. Three rounds, each longer and stricter. A mistake makes the sculk shriek and the
 * round starts over; if nobody answers at all the drum falls quiet until it is struck again.</p>
 *
 * <p>Winning the third round wakes the gate (see {@link GateAwakening}): the rim lights up note by
 * note, souls spiral in from the sensors and the drum, and the portal closes from the rim inward,
 * a ring at a time. The opening is synced to clients (like the Grand Stage's podium) so players
 * nearby can watch it through the cinematic camera.</p>
 */
public class SiftDrumBlockEntity extends BlockEntity {
    public static final int ROUNDS = 3;
    /** Gaps between beats, in ticks: quick, steady and slow (0.4 s, 0.6 s, 0.8 s). */
    private static final int[] INTERVAL_CHOICES = {8, 12, 16};
    /** After this many calls in a row that nobody answered the drum stops calling and waits. */
    private static final int UNANSWERED_LIMIT = 2;
    /** Hints, and looking for a frame, at most this often. */
    private static final long HINT_TICKS = 60L;
    /** The vibration frequency of a note block, which the drum's beats share. */
    private static final int DRUM_FREQUENCY = 10;
    /** Even the smallest gate closes in at least this many steps, a chime each. */
    private static final int MIN_STEPS = 6;

    private enum Phase { IDLE, CALL, ANSWER, FAILED, OPENING }

    private Phase phase = Phase.IDLE;
    private int round;
    private int timer;
    private int beatIndex;
    private int[] pattern = new int[0];
    private long lastAnswerTick;
    private int unanswered;
    private long lastHint = -10000L;
    private final List<BlockPos> sensors = new ArrayList<>();
    private PortalFrames.@Nullable Frame frame;

    // The opening. `opening`, `gateCentre` and `gateSpan` are synced to clients for the camera.
    /** Ticks into the opening, or -1. */
    private int opening = -1;
    private @Nullable Vec3 gateCentre;
    private float gateSpan = 8.0F;
    /** The floor of the gate's opening, where the flat ripples spread. */
    private double gateBottom;
    /** The frame's horizontal direction within its plane (x or z). */
    private Vec3 planeU = new Vec3(1.0, 0.0, 0.0);
    /** Glowing points along the rim, one stretch per note of the arpeggio, bottom to top. */
    private List<List<Vec3>> rimGroups = List.of();
    /** The portal's cells, outermost ring first. */
    private List<List<BlockPos>> rings = List.of();
    private int ringsPlaced;

    public SiftDrumBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SIFT_DRUM.get(), pos, state);
    }

    // ------------------------------------------------------------------ interaction

    public boolean tryInsertCore(ServerLevel level, Player player) {
        List<BlockPos> found = this.findSensors(level);
        if (found.size() < 3) {
            player.sendOverlayMessage(Component.translatable("message.thesift.drum.need_sensors", found.size()));
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, 0.6F);
            return false;
        }
        PortalFrames.Frame f = PortalFrames.find(level, this.worldPosition, 16);
        if (f == null) {
            player.sendOverlayMessage(Component.translatable("message.thesift.drum.no_frame"));
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, 0.6F);
            return false;
        }
        this.sensors.clear();
        this.sensors.addAll(found);
        this.frame = f;
        this.unanswered = 0;
        level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, true), Block.UPDATE_ALL);
        // the core wakes: a heartbeat, a deep hum, and every sensor and the gate's rim answer it
        level.playSound(null, this.worldPosition, ModSounds.WARDEN_CORE_PULSE.get(), SoundSource.BLOCKS, 2.0F, 0.8F);
        level.playSound(null, this.worldPosition, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5F, 0.6F);
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_CALL.get(), SoundSource.BLOCKS, 1.2F, 0.6F);
        level.sendParticles(ParticleTypes.SCULK_SOUL, this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.0, this.worldPosition.getZ() + 0.5,
                20, 0.3, 0.3, 0.3, 0.03);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.1,
                this.worldPosition.getZ() + 0.5, 0, 4.0, 0.0, 0.0, 1.0);
        for (BlockPos s : this.sensors) {
            this.activateSensor(level, s);
        }
        this.outlineGate(level, f);
        this.round = 0;
        this.startCall(level, 40);
        this.message(level, "message.thesift.drum.listen", 1);
        this.setChanged();
        return true;
    }

    /** Takes the core back out, unless it is already being poured into an opening gate. */
    public boolean ejectCore(ServerLevel level) {
        if (this.phase == Phase.OPENING) {
            return false;
        }
        if (this.getBlockState().getValue(SiftDrumBlock.CORE)) {
            Block.popResource(level, this.worldPosition.above(), new ItemStack(ModItems.WARDEN_CORE.get()));
            level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, false), Block.UPDATE_ALL);
        }
        this.phase = Phase.IDLE;
        this.setChanged();
        return true;
    }

    public void onPlayerBeat(@Nullable Player player) {
        if (!(this.level instanceof ServerLevel level) || player == null) return;
        if (this.phase == Phase.IDLE) {
            if (this.getBlockState().getValue(SiftDrumBlock.CORE)) {
                this.wake(level, player);
            } else {
                this.hint(level, player);
            }
            return;
        }
        if (this.phase != Phase.ANSWER) return;
        long now = level.getGameTime();
        if (this.beatIndex == 0) {
            // First hit sets the downbeat.
            this.beatIndex = 1;
            this.lastAnswerTick = now;
            this.unanswered = 0;
            this.answerNote(level, 0);
            this.flashSensor(level, 0, GateAwakening.callNote(this.round, 0));
            this.timer = this.pattern.length > 0 ? this.pattern[0] + 12 : 40;
            return;
        }
        int expected = this.pattern[this.beatIndex - 1];
        int actual = (int) (now - this.lastAnswerTick);
        // generous enough to play by ear: 0.3 s early or late in the first round, 0.2 s by the last
        int tolerance = 6 - this.round;
        if (Math.abs(actual - expected) <= tolerance) {
            this.answerNote(level, this.beatIndex);
            this.flashSensor(level, this.beatIndex, GateAwakening.callNote(this.round, this.beatIndex));
            this.lastAnswerTick = now;
            this.beatIndex++;
            if (this.beatIndex > this.pattern.length) {
                this.roundComplete(level);
            } else {
                this.timer = this.pattern[this.beatIndex - 1] + 12;
            }
        } else {
            this.fail(level, actual < expected ? "message.thesift.drum.too_early" : "message.thesift.drum.too_late");
        }
    }

    /** A read-only snapshot of the ritual, for debugging and the CI smoke test. */
    public record RitualState(String phase, int round, int beatIndex, int[] pattern, int timer) {}

    public RitualState ritualState() {
        return new RitualState(this.phase.name(), this.round, this.beatIndex, this.pattern.clone(), this.timer);
    }

    /** Struck without a core near a frame: say what the ritual still needs. */
    private void hint(ServerLevel level, Player player) {
        long now = level.getGameTime();
        if (now - this.lastHint < HINT_TICKS) return;
        this.lastHint = now;
        List<BlockPos> found = this.findSensors(level);
        PortalFrames.Frame f = PortalFrames.find(level, this.worldPosition, 16);
        if (f == null) {
            // sensors around but no gate: clearly meant for the ritual
            if (!found.isEmpty()) {
                player.sendOverlayMessage(Component.translatable("message.thesift.drum.no_frame"));
            }
            return;
        }
        // the gate stirs at the beat, so you can see which one the drum would open
        this.outlineGate(level, f);
        player.sendOverlayMessage(found.size() < 3
                ? Component.translatable("message.thesift.drum.need_sensors", found.size())
                : Component.translatable("message.thesift.drum.need_core"));
    }

    /** Struck with a core in while it waits (it fell quiet, or the world was reloaded): call again. */
    private void wake(ServerLevel level, Player player) {
        long now = level.getGameTime();
        if (now - this.lastHint < HINT_TICKS) return;
        this.lastHint = now;
        List<BlockPos> found = this.findSensors(level);
        PortalFrames.Frame f = PortalFrames.find(level, this.worldPosition, 16);
        if (found.size() < 3 || f == null) {
            player.sendOverlayMessage(f == null
                    ? Component.translatable("message.thesift.drum.no_frame")
                    : Component.translatable("message.thesift.drum.need_sensors", found.size()));
            return;
        }
        this.sensors.clear();
        this.sensors.addAll(found);
        this.frame = f;
        this.unanswered = 0;
        if (this.round >= ROUNDS) {
            // the rounds were already won: the gate only has to wake
            this.beginOpening(level);
            return;
        }
        this.startCall(level, 20);
        this.message(level, "message.thesift.drum.listen", this.round + 1);
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, SiftDrumBlockEntity drum) {
        if (level instanceof ServerLevel server) {
            drum.tick(server);
        }
    }

    /** Client side: follows a waking gate for the camera (see {@link GateAwakening}). */
    public static void clientTick(Level level, BlockPos pos, BlockState state, SiftDrumBlockEntity drum) {
        if (drum.opening < 0 || drum.gateCentre == null) {
            return;
        }
        drum.opening++;
        if (drum.opening > GateAwakening.LENGTH + 20) {
            drum.opening = -1;
            return;
        }
        GateAwakening.clientDrum = pos;
        GateAwakening.clientCentre = drum.gateCentre;
        GateAwakening.clientSpan = drum.gateSpan;
        GateAwakening.clientTick = drum.opening;
        GateAwakening.clientSeen = level.getGameTime();
    }

    private void tick(ServerLevel level) {
        if (this.phase == Phase.OPENING) {
            // the core is spent by now; nothing stops the gate
            this.tickOpening(level);
            return;
        }
        if (this.phase == Phase.IDLE) {
            return;
        }
        if (!this.getBlockState().getValue(SiftDrumBlock.CORE)) {
            this.phase = Phase.IDLE;
            return;
        }
        if (this.timer > 0) {
            this.timer--;
        }
        switch (this.phase) {
            case CALL -> {
                if (this.timer == 0) {
                    this.callBeat(level);
                }
            }
            case ANSWER -> {
                if (this.timer == 0) {
                    this.answerTimedOut(level);
                }
            }
            case FAILED -> {
                if (this.timer == 0) {
                    this.startCall(level, 10);
                    this.message(level, "message.thesift.drum.listen", this.round + 1);
                }
            }
            default -> {}
        }
    }

    /** One beat of the call: the drum, a clear note a step higher than the last, a sensor lighting up. */
    private void callBeat(ServerLevel level) {
        int k = this.beatIndex;
        int n = GateAwakening.callNote(this.round, k);
        SiftDrumBlock.beat(level, this.worldPosition, 0.5F, null);
        Vec3 at = Vec3.atCenterOf(this.worldPosition).add(0.0, 0.6, 0.0);
        GateAwakening.note(level, at, SoundEvents.NOTE_BLOCK_PLING, n, 2.0F);
        GateAwakening.note(level, at, SoundEvents.NOTE_BLOCK_BELL, n, 0.7F);
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_CALL.get(), SoundSource.BLOCKS, 0.6F, 0.7F + k * 0.06F);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.7, at.z, 0, n / 24.0, 0.0, 0.0, 1.0);
        this.flashSensor(level, k, n);
        if (k >= this.pattern.length) {
            this.phase = Phase.ANSWER;
            this.beatIndex = 0;
            this.timer = 20 * 8;
            this.message(level, "message.thesift.drum.your_turn", this.round + 1);
        } else {
            this.timer = this.pattern[k];
            this.beatIndex++;
        }
    }

    /** A good answer: the call's note again, in a brighter voice. */
    private void answerNote(ServerLevel level, int beat) {
        int n = GateAwakening.callNote(this.round, beat);
        Vec3 at = Vec3.atCenterOf(this.worldPosition).add(0.0, 0.6, 0.0);
        GateAwakening.note(level, at, SoundEvents.NOTE_BLOCK_HARP, n, 1.8F);
        GateAwakening.note(level, at, SoundEvents.NOTE_BLOCK_CHIME, n, 0.6F);
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_GOOD.get(), SoundSource.BLOCKS, 0.4F, 1.0F + beat * 0.08F);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y + 0.4, at.z, 4, 0.25, 0.15, 0.25, 0.0);
    }

    private void answerTimedOut(ServerLevel level) {
        if (this.beatIndex == 0 && ++this.unanswered >= UNANSWERED_LIMIT) {
            // nobody is playing: stop calling, and wait for the drum to be struck again
            this.phase = Phase.IDLE;
            this.unanswered = 0;
            level.playSound(null, this.worldPosition, SoundEvents.SCULK_CLICKING_STOP, SoundSource.BLOCKS, 1.0F, 0.6F);
            this.message(level, "message.thesift.drum.waiting", 0);
            this.setChanged();
            return;
        }
        this.fail(level, "message.thesift.drum.too_late");
    }

    private void startCall(ServerLevel level, int delay) {
        RandomSource r = level.getRandom();
        int beats = 3 + this.round;
        this.pattern = new int[beats];
        for (int i = 0; i < beats; i++) {
            this.pattern[i] = INTERVAL_CHOICES[r.nextInt(INTERVAL_CHOICES.length)];
        }
        this.phase = Phase.CALL;
        this.beatIndex = 0;
        this.timer = delay;
        this.setChanged();
    }

    private void roundComplete(ServerLevel level) {
        this.round++;
        Vec3 at = Vec3.atCenterOf(this.worldPosition).add(0.0, 0.6, 0.0);
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_ROUND.get(), SoundSource.BLOCKS, 1.6F, 0.8F + this.round * 0.15F);
        for (int n : GateAwakening.roundChord(this.round)) {
            GateAwakening.note(level, at, SoundEvents.NOTE_BLOCK_HARP, n, 1.5F);
        }
        level.sendParticles(ModParticles.RESONANCE_RING.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 0.1,
                this.worldPosition.getZ() + 0.5, 0, 6.0, 0, 0, 1.0);
        for (BlockPos s : this.sensors) {
            this.activateSensor(level, s);
        }
        if (this.round >= ROUNDS) {
            this.beginOpening(level);
        } else {
            this.message(level, "message.thesift.drum.round_done", this.round);
            this.startCall(level, 40);
        }
        this.setChanged();
    }

    private void fail(ServerLevel level, String key) {
        this.phase = Phase.FAILED;
        this.timer = 60;
        this.beatIndex = 0;
        level.playSound(null, this.worldPosition, ModSounds.RHYTHM_FAIL.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        for (BlockPos s : this.sensors) {
            level.sendParticles(new ShriekParticleOption(0), s.getX() + 0.5, s.getY() + 0.6, s.getZ() + 0.5, 1, 0.0, 0.0, 0.0, 0.0);
        }
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(16))) {
            p.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 80, 0));
        }
        this.message(level, key, 0);
    }

    // ------------------------------------------------------------------ the gate wakes

    private void beginOpening(ServerLevel level) {
        PortalFrames.Frame f = this.frame;
        if (f == null) {
            f = PortalFrames.find(level, this.worldPosition, 16);
            if (f == null) {
                // no gate any more: the drum keeps its core and waits
                this.phase = Phase.IDLE;
                this.setChanged();
                return;
            }
            this.frame = f;
        }
        this.prepareGate(f);
        this.phase = Phase.OPENING;
        this.opening = 0;
        this.ringsPlaced = 0;
        this.message(level, "message.thesift.drum.opening", 0);
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

        // The rim glows just inside the frame, so it shows from both sides. It lights up from the bottom of the
        // gate, both sides together, meeting at the top: an equal stretch of it with every note.
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
            // a small frame: every note still lights a spot, some of them twice
            for (int g = 0; g < GateAwakening.RIM_NOTES; g++) {
                groups.get(g).add(rim.get(g * rim.size() / GateAwakening.RIM_NOTES).pos());
            }
        }
        this.rimGroups = groups;

        // The rings: every cell by how far in from the rim it lies.
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
        // A small gate is all rim: close each ring in a few steps too, from the bottom up both sides.
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
        // ...and what is lit keeps glowing
        if (t % 3 == 0 && lit > 0) {
            for (int g = 0; g < lit; g++) {
                List<Vec3> group = this.rimGroups.get(g);
                if (group.isEmpty() || t > GateAwakening.CLIMAX && r.nextInt(3) != 0) continue;
                Vec3 p = group.get(r.nextInt(group.size()));
                level.sendParticles(r.nextInt(3) == 0 ? ModParticles.DRIFTING_SOUL.get() : ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, 1, 0.12, 0.12,
                        0.12, 0.0);
            }
        }
        // 2. souls stream in from the sensors and the drum and spiral into a vortex
        if (t < GateAwakening.CLIMAX) {
            this.souls(level, r, c, t);
        }
        // 3. the surface comes together, ring by ring from the rim inward
        while (this.ringsPlaced < this.rings.size() && GateAwakening.ringTick(this.ringsPlaced, this.rings.size()) <= t
                && t < GateAwakening.CLIMAX) {
            this.placeRing(level, f, c, this.ringsPlaced);
            this.ringsPlaced++;
        }
        // 4. the last ring closes: the final chord
        if (t == GateAwakening.CLIMAX) {
            this.climax(level, f, c);
        }
        if (t >= GateAwakening.TAIL_START && (t - GateAwakening.TAIL_START) % GateAwakening.TAIL_STEP == 0) {
            int i = (t - GateAwakening.TAIL_START) / GateAwakening.TAIL_STEP;
            if (i < GateAwakening.TAIL.length) {
                GateAwakening.note(level, c, SoundEvents.NOTE_BLOCK_CHIME, GateAwakening.TAIL[i], 1.4F - i * 0.2F);
            }
        }
        if (t > GateAwakening.CLIMAX && t % 4 == 0) {
            // the new portal breathes out a few souls
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

    /** The drum's last, biggest beat; the sensors flare and a whoosh rises under everything. */
    private void openingBegins(ServerLevel level, Vec3 c) {
        SiftDrumBlock.beat(level, this.worldPosition, 1.0F, null);
        level.playSound(null, this.worldPosition, ModSounds.DRUM_BOOM.get(), SoundSource.BLOCKS, 2.0F, 0.8F);
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
            // each bar's chord, from the bass up, and a sensor flaring in time
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

    /** Souls stream in from every sensor and from the drum and feed a three-armed vortex that tightens as the gate closes. */
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
            // along the spiral, and a little towards its heart
            Vec3 tangent = this.planeU.scale(-sin).add(0.0, cos, 0.0);
            Vec3 inward = c.subtract(p).normalize();
            Vec3 v = tangent.scale(0.05 + 0.07 * k).add(inward.scale(0.03));
            level.sendParticles(ModParticles.PORTAL_SOUL.get(), p.x, p.y, p.z, 0, v.x, v.y, v.z, 1.0);
            if (r.nextInt(3) == 0) {
                level.sendParticles(ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, 1, 0.1, 0.1, 0.1, 0.0);
            }
        }
    }

    /** One soul from {@code from} to a point on the vortex, so the streams feed into the swirl. */
    private void stream(ServerLevel level, RandomSource r, Vec3 from, Vec3 c, double radius) {
        double a = r.nextDouble() * Math.PI * 2.0;
        Vec3 target = c.add(this.planeU.scale(Math.cos(a) * radius)).add(0.0, Math.sin(a) * radius, 0.0);
        Vec3 v = target.subtract(from).scale(1.0 / 28.0);
        level.sendParticles(ModParticles.PORTAL_SOUL.get(), from.x, from.y, from.z, 0, v.x, v.y + 0.01, v.z, 1.0);
    }

    /** One ring of the portal falls into place, with a chime. */
    private void placeRing(ServerLevel level, PortalFrames.Frame f, Vec3 c, int k) {
        List<BlockPos> ring = this.rings.get(k);
        PortalFrames.fillCells(level, f, ring);
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
                // notes rise off the new stretch, in the colour of its chime
                level.sendParticles(ModParticles.SIFT_NOTE.get(), b.getX() + 0.5, b.getY() + 0.8, b.getZ() + 0.5, 0, n / 24.0, 0.0, 0.0, 1.0);
            }
        }
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, this.gateBottom, c.z, 0, 1.5 + k * 0.6, 0.0, 0.0, 1.0);
    }

    /** The gate is whole: A major, a soft boom, rings of light, and the core is spent. */
    private void climax(ServerLevel level, PortalFrames.Frame f, Vec3 c) {
        // whatever happened to the rings, the gate is complete now
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
        level.playSound(null, c.x, c.y, c.z, ModSounds.DRUM_BOOM.get(), SoundSource.BLOCKS, 2.0F, 0.7F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.BLOCKS, 0.7F, 1.6F);
        for (int i = 0; i < 6; i++) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, this.gateBottom + i * 1.5, c.z, 0, 4.0 + i, 0, 0, 1.0);
        }
        RandomSource r = level.getRandom();
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
        this.message(level, "message.thesift.drum.opened", 0);
        if (this.getBlockState().getValue(SiftDrumBlock.CORE)) {
            level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, false), Block.UPDATE_ALL);
        }
        this.setChanged();
    }

    private void endOpening(ServerLevel level) {
        if (this.getBlockState().getValue(SiftDrumBlock.CORE)) {
            level.setBlock(this.worldPosition, this.getBlockState().setValue(SiftDrumBlock.CORE, false), Block.UPDATE_ALL);
        }
        this.phase = Phase.IDLE;
        this.opening = -1;
        this.round = 0;
        this.frame = null;
        this.rimGroups = List.of();
        this.rings = List.of();
        this.ringsPlaced = 0;
        this.sync();
    }

    // ------------------------------------------------------------------ helpers

    private List<BlockPos> findSensors(Level level) {
        List<BlockPos> out = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-8, -4, -8), this.worldPosition.offset(8, 4, 8))) {
            BlockState s = level.getBlockState(p);
            if (s.is(Blocks.SCULK_SENSOR) || s.is(Blocks.CALIBRATED_SCULK_SENSOR)) {
                out.add(p.immutable());
            }
        }
        return out;
    }

    /**
     * Really sets a sculk sensor off - its glow, its redstone pulse, its clicking and its cooldown -
     * if it is resting. With no source entity nothing tells the shriekers who played.
     */
    private boolean activateSensor(ServerLevel level, BlockPos s) {
        BlockState state = level.getBlockState(s);
        if (state.getBlock() instanceof SculkSensorBlock sensor && SculkSensorBlock.canActivate(state)) {
            double distance = Math.sqrt(s.distSqr(this.worldPosition));
            int power = Math.max(1, Math.min(15, 15 - (int) Math.floor(distance * 15.0 / 9.0)));
            sensor.activate(null, level, s, state, power, DRUM_FREQUENCY);
            return true;
        }
        return false;
    }

    /** Lights up the next resting sensor in turn (or at least sparks the one whose turn it is). */
    private void flashSensor(ServerLevel level, int index, int note) {
        if (this.sensors.isEmpty()) return;
        int size = this.sensors.size();
        BlockPos s = this.sensors.get(Math.floorMod(index, size));
        boolean lit = false;
        for (int k = 0; k < size && !lit; k++) {
            BlockPos candidate = this.sensors.get(Math.floorMod(index + k, size));
            if (this.activateSensor(level, candidate)) {
                s = candidate;
                lit = true;
            }
        }
        if (!lit) {
            level.playSound(null, s, SoundEvents.SCULK_CLICKING, SoundSource.BLOCKS, 1.0F, 0.8F + index * 0.1F);
        }
        level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, s.getX() + 0.5, s.getY() + 0.7, s.getZ() + 0.5, 6, 0.2, 0.1, 0.2, 0.02);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), s.getX() + 0.5, s.getY() + 1.1, s.getZ() + 0.5, 0, note / 24.0, 0, 0, 1);
    }

    /** A shimmer all along the frame's rim: this is the gate the drum is listening to. */
    private void outlineGate(ServerLevel level, PortalFrames.Frame f) {
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

    private void message(ServerLevel level, String key, int arg) {
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(20))) {
            p.sendOverlayMessage(Component.translatable(key, arg, ROUNDS));
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
        output.putInt("Round", this.round);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.round = input.getIntOr("Round", 0);
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
        } else if (state.getValue(SiftDrumBlock.CORE) && this.level != null) {
            Block.popResource(this.level, pos, new ItemStack(ModItems.WARDEN_CORE.get()));
        }
        super.preRemoveSideEffects(pos, state);
    }
}
