package com.thesift.block.entity;

import com.thesift.block.EchoerDeviceBlock;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.event.level.NoteBlockEvent;
import org.jspecify.annotations.Nullable;

/**
 * RR: the Echoer Drill's ears and drill (see {@link EchoerDeviceBlock}). It listens for a rhythm - redstone pulses,
 * taps by hand, notes played or note blocks sounding within eight blocks - and when the rhythm ends (a rest of
 * {@link #REST} ticks) it reads it:
 * <ul>
 *   <li>how many beats: the pattern - 1 a single echo shot (the first block in reach, as the old Echoer did),
 *   2 a bore (a 1x1 tunnel), 3 a wide bore (3x3, at most 8 deep), 4 or more a vein hunt (it bores until it hits an
 *   ore, then eats the whole vein, up to {@link #VEIN_MAX} blocks);</li>
 *   <li>the tempo: the direction - a fast rhythm (beats {@link #FAST} ticks apart or less) digs a staircase down,
 *   a slow one ({@link #SLOW} or more) a staircase up, anything between straight ahead (sideways drills only);</li>
 *   <li>the loudest beat: the depth - a redstone pulse's strength, a note's pitch (low 2 to high 16 blocks) or the
 *   dial (4, 8 or 16) when tapped by hand.</li>
 * </ul>
 * The drill stops at unbreakable blocks and anything holding a block entity; drops go into a container touching it
 * (the back first) or pop out of it, as before. Clients hear block events for the animation (beats, the program,
 * every bite, the end).
 */
public class EchoerDeviceBlockEntity extends BlockEntity {
    public static final int SHOT = 0;
    public static final int BORE = 1;
    public static final int WIDE = 2;
    public static final int VEIN = 3;
    /** A rhythm ends after this many ticks without a beat. */
    public static final int REST = 24;
    public static final int MAX_BEATS = 8;
    /** Mean gap between beats (ticks): at or under FAST the rhythm is fast (digs down), at or over SLOW it is slow (digs up). */
    public static final int FAST = 6;
    public static final int SLOW = 14;
    public static final double HEARING = 8.0;
    public static final int VEIN_MAX = 32;
    public static final int EV_BEAT = 1;
    public static final int EV_PROGRAM = 2;
    public static final int EV_BITE = 3;
    public static final int EV_END = 4;
    /** It mines like a diamond pickaxe would (no enchantments); made when needed, as the class loads at start-up (listen()). */
    private static @Nullable ItemStack tool;
    /** The drills loaded on the server, so a note can find the ones in earshot. */
    private static final Set<EchoerDeviceBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    // ---- listening (server)
    private int beats;
    private long firstBeat = -1L;
    private long lastBeat = -1L;
    private int depth;
    private @Nullable UUID tapper;
    // ---- the running program (server); mode -1 = idle
    private int mode = -1;
    private int slope;
    private int length;
    private int step;
    private int cooldown;
    private int veinLeft;
    private final ArrayDeque<BlockPos> vein = new ArrayDeque<>();
    private final Set<BlockPos> veinSeen = new HashSet<>();

    // ---- client animation (read by EchoerDrillRenderer)
    public float spin;
    public float spinO;
    public float spinSpeed;
    public float recoil;
    public float recoilO;
    public float ear;
    public float earO;
    public float dial;
    public float dialO;
    public float dialTarget;
    public boolean drilling;
    public int shownMode = -1;
    public long age;

    public EchoerDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(ModEchoer.ECHOER_DEVICE_BE.get(), pos, state);
    }

    /** Hooked once from ModEchoer: played notes and sounding note blocks are beats for every drill in earshot. */
    public static void listen() {
        SongEvents.listenNotes((level, player, at, pitch) -> hearAll(level, player, at, noteDepth(pitch)));
        NeoForge.EVENT_BUS.addListener(EchoerDeviceBlockEntity::onNoteBlock);
    }

    private static void onNoteBlock(NoteBlockEvent.Play event) {
        if (event.getLevel() instanceof ServerLevel server) {
            hearAll(server, null, Vec3.atCenterOf(event.getPos()), noteDepth(event.getVanillaNoteId()));
        }
    }

    /** A low note asks for a short reach, a high one for a long one (pitch 0-24 -> 2-16 blocks). */
    private static int noteDepth(int pitch) {
        return 2 + Mth.clamp(pitch, 0, 24) * 14 / 24;
    }

    private static void hearAll(ServerLevel level, @Nullable Player player, Vec3 at, int blocks) {
        for (EchoerDeviceBlockEntity d : new ArrayList<>(LOADED)) {
            if (d.level == level && !d.isRemoved() && Vec3.atCenterOf(d.worldPosition).distanceToSqr(at) <= HEARING * HEARING) {
                d.hear(level, player, blocks);
            }
        }
    }

    @Override
    public void onLoad() {
        if (this.level != null && !this.level.isClientSide()) {
            LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        LOADED.remove(this);
    }

    private Direction facing() {
        BlockState s = this.getBlockState();
        return s.getBlock() instanceof EchoerDeviceBlock ? s.getValue(EchoerDeviceBlock.FACING) : Direction.NORTH;
    }

    // ------------------------------------------------------------------ listening

    /** One beat: a redstone pulse, a tap by hand or a note nearby, asking for {@code blocks} of reach (1-16). */
    public void hear(ServerLevel level, @Nullable Player player, int blocks) {
        if (this.mode >= 0) {
            return; // busy drilling: it does not listen
        }
        long now = level.getGameTime();
        if (this.lastBeat >= 0L && now - this.lastBeat < 2L) {
            return; // one beat heard twice (a powered note block next to it)
        }
        BlockPos pos = this.worldPosition;
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof EchoerDeviceBlock)) {
            return;
        }
        if (this.beats == 0) {
            this.firstBeat = now;
            this.depth = 0;
            if (!state.getValue(EchoerDeviceBlock.CHARGING)) {
                level.setBlock(pos, state.setValue(EchoerDeviceBlock.CHARGING, true), Block.UPDATE_CLIENTS);
            }
            level.playSound(null, pos, ModEchoer.DEVICE_CHARGE.get(), SoundSource.BLOCKS, 0.6F, 1.2F);
        }
        this.beats++;
        this.lastBeat = now;
        this.depth = Math.max(this.depth, Mth.clamp(blocks, 1, 16));
        if (player != null) {
            this.tapper = player.getUUID();
        }
        level.blockEvent(pos, state.getBlock(), EV_BEAT, this.beats);
        level.playSound(null, pos, ModEchoer.DEVICE_BEAT.get(), SoundSource.BLOCKS, 0.8F, 0.7F + 0.12F * this.beats);
        if (this.beats >= MAX_BEATS) {
            this.read(level);
        }
        this.setChanged();
    }

    /** The rhythm is over: turn it into a program. */
    private void read(ServerLevel level) {
        int n = this.beats;
        int pattern = n >= 4 ? VEIN : n - 1;
        int dir = 0;
        if (n >= 2 && this.facing().getAxis().isHorizontal()) {
            float gap = (this.lastBeat - this.firstBeat) / (float) (n - 1);
            dir = gap <= FAST ? -1 : gap >= SLOW ? 1 : 0;
        }
        int len = Math.max(1, pattern == WIDE ? Math.min(this.depth, 8) : this.depth);
        Player who = this.tapper != null ? level.getPlayerByUUID(this.tapper) : null;
        this.beats = 0;
        this.firstBeat = -1L;
        this.lastBeat = -1L;
        this.tapper = null;
        this.begin(level, pattern, dir, len, who);
    }

    private void begin(ServerLevel level, int pattern, int dir, int len, @Nullable Player who) {
        this.mode = pattern;
        this.slope = dir;
        this.length = len;
        this.step = 0;
        this.cooldown = 6;
        this.veinLeft = VEIN_MAX;
        this.vein.clear();
        this.veinSeen.clear();
        level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), EV_PROGRAM, pattern * 3 + dir + 1);
        if (who != null) {
            who.sendOverlayMessage(Component.translatable("message.thesift.echoer_device.program",
                    Component.translatable("echoer_drill.thesift.pattern." + pattern), Component.translatable("echoer_drill.thesift.slope." + (dir + 1)), len));
        }
        if (pattern == SHOT) {
            this.shot(level);
            this.end(level);
        }
        this.setChanged();
    }

    private void end(ServerLevel level) {
        this.mode = -1;
        this.vein.clear();
        this.veinSeen.clear();
        BlockState st = level.getBlockState(this.worldPosition);
        if (st.getBlock() instanceof EchoerDeviceBlock) {
            level.blockEvent(this.worldPosition, st.getBlock(), EV_END, 0);
            if (st.getValue(EchoerDeviceBlock.CHARGING)) {
                level.setBlock(this.worldPosition, st.setValue(EchoerDeviceBlock.CHARGING, false), Block.UPDATE_CLIENTS);
            }
        }
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EchoerDeviceBlockEntity d) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (d.beats > 0 && server.getGameTime() - d.lastBeat >= REST) {
            d.read(server);
        }
        if (d.mode >= 0) {
            if (--d.cooldown <= 0) {
                d.bite(server);
            }
            d.setChanged();
        } else if (d.beats == 0 && state.getValue(EchoerDeviceBlock.CHARGING)) {
            server.setBlock(pos, state.setValue(EchoerDeviceBlock.CHARGING, false), Block.UPDATE_CLIENTS); // nothing to do: settle
        }
    }

    // ------------------------------------------------------------------ drilling

    private static boolean isProtected(ServerLevel level, BlockPos p, BlockState st) {
        return st.getDestroySpeed(level, p) < 0.0F || st.hasBlockEntity();
    }

    private static boolean isEmpty(ServerLevel level, BlockPos p, BlockState st) {
        return st.isAir() || st.getCollisionShape(level, p).isEmpty() && !st.getFluidState().isEmpty();
    }

    /** The blocks one step of the program clears around the centre {@code c}. */
    private List<BlockPos> slice(BlockPos c, Direction f) {
        List<BlockPos> out = new ArrayList<>();
        if (this.mode == WIDE) {
            Direction a = f.getAxis() == Direction.Axis.Y ? Direction.EAST : f.getClockWise();
            Direction b = f.getAxis() == Direction.Axis.Y ? Direction.SOUTH : Direction.UP;
            for (int i = -1; i <= 1; i++) {
                for (int j = -1; j <= 1; j++) {
                    out.add(c.relative(a, i).relative(b, j));
                }
            }
        } else {
            out.add(c);
            if (this.slope != 0) {
                // a staircase: three blocks tall, so you can walk it
                out.add(c.above());
                out.add(c.above(2));
            }
        }
        return out;
    }

    private void bite(ServerLevel level) {
        Direction f = this.facing();
        if (this.mode == VEIN && !this.veinSeen.isEmpty()) {
            this.veinBite(level, f);
            return;
        }
        this.step++;
        if (this.step > this.length) {
            this.end(level);
            return;
        }
        BlockPos c = this.worldPosition.relative(f, this.step).above(this.slope * this.step);
        if (!level.isLoaded(c)) {
            this.end(level);
            return;
        }
        BlockState cs = level.getBlockState(c);
        if (this.mode == VEIN && cs.is(Tags.Blocks.ORES) && !isProtected(level, c, cs)) {
            // found one: eat the whole vein
            this.veinSeen.add(c);
            this.vein.add(c);
            this.veinBite(level, f);
            return;
        }
        if (!isEmpty(level, c, cs) && isProtected(level, c, cs)) {
            // something it may not break: the drill jams
            this.fizzle(level, c);
            this.end(level);
            return;
        }
        int broke = 0;
        for (BlockPos p : this.slice(c, f)) {
            if (this.dig(level, p, f)) {
                broke++;
            }
        }
        this.cooldown = broke == 0 ? 2 : this.mode == WIDE ? 8 : 5;
        this.effects(level, c, broke);
    }

    private void veinBite(ServerLevel level, Direction f) {
        while (!this.vein.isEmpty() && this.veinLeft > 0) {
            BlockPos p = this.vein.poll();
            BlockState st = level.getBlockState(p);
            if (!st.is(Tags.Blocks.ORES) || isProtected(level, p, st)) {
                continue;
            }
            this.dig(level, p, f);
            this.veinLeft--;
            for (BlockPos q : BlockPos.betweenClosed(p.offset(-1, -1, -1), p.offset(1, 1, 1))) {
                BlockPos iq = q.immutable();
                if (level.isLoaded(iq) && this.veinSeen.add(iq) && level.getBlockState(iq).is(Tags.Blocks.ORES)) {
                    this.vein.add(iq);
                }
            }
            this.cooldown = 5;
            this.effects(level, p, 1);
            return;
        }
        this.end(level);
    }

    /** Breaks one block (if it may) and hands on its drops. */
    private boolean dig(ServerLevel level, BlockPos p, Direction f) {
        if (!level.isLoaded(p)) {
            return false;
        }
        BlockState st = level.getBlockState(p);
        if (isEmpty(level, p, st) || isProtected(level, p, st)) {
            return false;
        }
        if (tool == null) {
            tool = new ItemStack(Items.DIAMOND_PICKAXE);
        }
        List<ItemStack> drops = Block.getDrops(st, level, p, null, null, tool);
        level.destroyBlock(p, false);
        for (ItemStack d : drops) {
            this.deliver(level, this.worldPosition, f, d);
        }
        return true;
    }

    private void effects(ServerLevel level, BlockPos c, int broke) {
        Vec3 at = Vec3.atCenterOf(c);
        if (broke > 0) {
            level.sendParticles(ModParticles.RESONANCE_RING.get(), at.x, at.y, at.z, 0, 1.0, 0.0, 0.0, 1.0);
            level.sendParticles(ParticleTypes.SCULK_SOUL, at.x, at.y, at.z, 3, 0.3, 0.3, 0.3, 0.02);
            level.playSound(null, c, ModEchoer.DEVICE_BITE.get(), SoundSource.BLOCKS, 0.9F, 0.85F + level.getRandom().nextFloat() * 0.3F);
        }
        level.blockEvent(this.worldPosition, this.getBlockState().getBlock(), EV_BITE, broke > 0 ? 1 : 0);
    }

    private void fizzle(ServerLevel level, BlockPos hit) {
        Vec3 c = Vec3.atCenterOf(hit);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y, c.z, 0, 1.0, 0.0, 0.0, 1.0);
        level.playSound(null, hit, ModEchoer.DEVICE_FIZZLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
    }

    /** One beat: the old Echoer's echo shot - it shatters the first breakable block in reach. */
    private void shot(ServerLevel level) {
        BlockPos pos = this.worldPosition;
        Direction f = this.facing();
        Vec3 from = Vec3.atCenterOf(pos).add(f.getStepX() * 0.9, f.getStepY() * 0.9, f.getStepZ() * 0.9);
        BlockPos hit = null;
        boolean broke = false;
        int steps = this.length;
        for (int i = 1; i <= this.length; i++) {
            BlockPos at = pos.relative(f, i);
            BlockState st = level.getBlockState(at);
            if (isEmpty(level, at, st)) {
                continue; // through air and fluids
            }
            hit = at;
            steps = i;
            broke = this.dig(level, at, f);
            break;
        }
        // the beam: a line of sonic rings out to what it hit (or as far as it reaches)
        Vec3 dir = new Vec3(f.getStepX(), f.getStepY(), f.getStepZ());
        double len = hit == null ? this.length : steps - 0.9;
        for (double d = 0.0; d <= len; d += 1.0) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, pos, ModEchoer.DEVICE_FIRE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        level.blockEvent(pos, this.getBlockState().getBlock(), EV_BITE, 1);
        if (hit != null && !broke) {
            this.fizzle(level, hit);
        }
    }

    /** Into a touching container (the back first), otherwise out of the drill. */
    private void deliver(ServerLevel level, BlockPos pos, Direction facing, ItemStack stack) {
        ItemStack left = stack;
        for (int i = -1; i < Direction.values().length && !left.isEmpty(); i++) {
            Direction d = i < 0 ? facing.getOpposite() : Direction.values()[i];
            if (d == facing || (i >= 0 && d == facing.getOpposite())) {
                continue;
            }
            Container c = HopperBlockEntity.getContainerAt(level, pos.relative(d));
            if (c != null) {
                left = HopperBlockEntity.addItem(null, c, left, d.getOpposite());
            }
        }
        if (!left.isEmpty()) {
            Block.popResourceFromFace(level, pos, facing == Direction.UP ? Direction.NORTH : Direction.UP, left);
        }
    }

    // ------------------------------------------------------------------ the client: animation

    @Override
    public boolean triggerEvent(int id, int param) {
        if (id < EV_BEAT || id > EV_END) {
            return super.triggerEvent(id, param);
        }
        if (this.level != null && this.level.isClientSide()) {
            if (id == EV_BEAT) {
                this.ear = 1.0F;
                this.dialTarget += Mth.PI / 6.0F; // the music drum turns a notch per beat
            } else if (id == EV_PROGRAM) {
                this.shownMode = param / 3;
                this.drilling = true;
                this.dialTarget += Mth.PI;
            } else if (id == EV_BITE) {
                if (param > 0) {
                    this.recoil = 1.0F;
                }
            } else {
                this.drilling = false;
            }
        }
        return true;
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EchoerDeviceBlockEntity d) {
        d.age++;
        d.spinO = d.spin;
        d.recoilO = d.recoil;
        d.earO = d.ear;
        d.dialO = d.dial;
        boolean busy = state.getValue(EchoerDeviceBlock.CHARGING);
        if (!busy) {
            d.drilling = false;
        }
        float target = d.drilling ? 0.95F : busy ? 0.25F : 0.0F;
        d.spinSpeed += (target - d.spinSpeed) * 0.12F;
        d.spin += d.spinSpeed;
        if (d.spin > 400.0F) {
            d.spin -= 64.0F * Mth.PI;
            d.spinO -= 64.0F * Mth.PI;
        }
        d.recoil *= 0.7F;
        d.ear *= 0.82F;
        if (d.drilling) {
            d.dialTarget += 0.02F;
        }
        d.dial += (d.dialTarget - d.dial) * 0.2F;
        if (d.dial > 400.0F) {
            d.dial -= 64.0F * Mth.PI;
            d.dialO -= 64.0F * Mth.PI;
            d.dialTarget -= 64.0F * Mth.PI;
        }
        if (d.drilling && d.spinSpeed > 0.5F && level.getRandom().nextInt(3) == 0) {
            // sparks off the bit
            Direction f = state.getValue(EchoerDeviceBlock.FACING);
            double x = pos.getX() + 0.5 + f.getStepX() * 0.95 + (level.getRandom().nextDouble() - 0.5) * 0.3;
            double y = pos.getY() + 0.5 + f.getStepY() * 0.95 + (level.getRandom().nextDouble() - 0.5) * 0.3;
            double z = pos.getZ() + 0.5 + f.getStepZ() * 0.95 + (level.getRandom().nextDouble() - 0.5) * 0.3;
            level.addParticle(ParticleTypes.ELECTRIC_SPARK, x, y, z, f.getStepX() * 0.1, 0.05, f.getStepZ() * 0.1);
        }
    }

    // ------------------------------------------------------------------ saving

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Beats", this.beats);
        output.putLong("FirstBeat", this.firstBeat);
        output.putLong("LastBeat", this.lastBeat);
        output.putInt("Depth", this.depth);
        output.putInt("Mode", this.mode);
        output.putInt("Slope", this.slope);
        output.putInt("Length", this.length);
        output.putInt("Step", this.step);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.beats = input.getIntOr("Beats", 0);
        this.firstBeat = input.getLongOr("FirstBeat", -1L);
        this.lastBeat = input.getLongOr("LastBeat", -1L);
        this.depth = input.getIntOr("Depth", 8);
        this.mode = input.getIntOr("Mode", -1);
        this.slope = input.getIntOr("Slope", 0);
        this.length = input.getIntOr("Length", 8);
        this.step = input.getIntOr("Step", 0);
    }
}
