package com.thesift.entity.caravan;

import com.thesift.block.CrystalColor;
import com.thesift.entity.KillBurst;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandRegistry;
import com.thesift.music.band.BandVoice;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Caravan: a nomadic gem-crab of the Caravans Cavern, its shell crusted with gems of its caravan's
 * colour (amber, rose, teal, violet or rare gold).
 *
 * <ul>
 *   <li>Caravans travel the caves in little columns behind a leader. They dig natural ore and
 *   crystal out of the rock - never anything a player built: the ore must sit embedded in natural
 *   stone - and carry it in their claws to their {@link CaravanQueen} in her lair. In the socket the
 *   ore leaves they often lay their eggs: Egg-laden Ore, which lets out a {@link CaravanLarva} when
 *   it is mined. Without a Queen they wander on as nomads, carrying their ore with them.</li>
 *   <li>The gem crust grows as a Caravan ages: one fresh from its larva is small and bare-shelled.
 *   Soldiers are half as big again, with crystal-studded claws.</li>
 *   <li>They have their own music: a caravan at rest plays together as a band - the leader (or their
 *   Queen, on her bells) the Crystal Hymn, the others harmony, the soldiers the bass and everyone a
 *   clicking rhythm on their claws.</li>
 *   <li>Neutral but territorial: they leave you be, but come into their Queen's lair and they rear up
 *   and clack their claws at you - stay, and they attack. Hit one, or break a crystal or an
 *   egg-laden ore near them, and the whole caravan swarms. The Crystal Hymn calms them for a few
 *   minutes, and they play along.</li>
 * </ul>
 */
public class Caravan extends Monster {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SOLDIER = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CALM = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.ITEM_STACK);
    /** How far the gem crust on its shell has grown (0-100); see {@link #setGrowth}. */
    private static final EntityDataAccessor<Integer> GROWTH = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.INT);
    protected static final byte EVENT_TAP = 100;
    private static final byte EVENT_MINE = 101;
    private static final byte EVENT_BITE = 102;
    protected static final byte EVENT_OFFER = 103;
    /** CR2: rearing up and clacking its claws at an intruder. */
    protected static final byte EVENT_WARN = 104;
    private static final int MINE_TICKS = 60;
    private static final double SWARM_RANGE = 24.0;
    /** How far a Caravan looks for a Queen to serve, and how far a caravan's colour carries. */
    private static final double QUEEN_RANGE = 64.0;
    private static final double KIN_RANGE = 24.0;
    /** A caravan with a Queen forages within this range of her lair. */
    private static final int FORAGE_RANGE = 40;
    /** One point of crust every this many ticks; a young one reaches full size at ADULT_GROWTH. */
    private static final int GROWTH_TICKS = 400;
    private static final int ADULT_GROWTH = 40;
    /** How often a mined ore socket is left full of eggs. */
    private static final float EGG_CHANCE = 0.35F;
    /** How long an intruder is warned before the caravan attacks (ticks). */
    private static final int WARN_TICKS = 60;
    /** A note of a jam every BEAT ticks: the Crystal Hymn twice through, then a chord. */
    public static final int BEAT = 8;

    /** CAVE client: how far it has turned side-on to scuttle (eased, see aiStep), for the renderer. */
    public float scuttleO, scuttle;
    public final AnimationState tapAnimation = new AnimationState();
    public final AnimationState mineAnimation = new AnimationState();
    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState offerAnimation = new AnimationState();
    public final AnimationState warnAnimation = new AnimationState();
    private @Nullable UUID queenId;
    /** The caravan it walks behind (never saved: caravans re-form as they meet). */
    private @Nullable Caravan leader;
    protected int calmTicks;
    private int mineCooldown = 100;
    private int jamCooldown = 300;
    /** While above 0 it is playing in a jam: it holds still and plays its part. */
    private int jamTicks;
    private int alarmCooldown;
    private int warnTicks;
    private @Nullable Player warned;
    private int queenSearch;

    public Caravan(EntityType<? extends Monster> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 12.0)
                .add(Attributes.MOVEMENT_SPEED, 0.3)
                .add(Attributes.ATTACK_DAMAGE, 3.0)
                .add(Attributes.ARMOR, 2.0)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    /**
     * S1 never freeze: vanilla stops a mob's random strolls once it has been 100 ticks out of
     * a player's 32-block reach, so Sift creatures seen across a valley stood frozen. The field
     * itself (which drives despawning) is left alone.
     */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.25, true));
        this.goalSelector.addGoal(3, new DeliverGoal());
        this.goalSelector.addGoal(4, new MineOreGoal());
        this.goalSelector.addGoal(5, new JamGoal());
        this.goalSelector.addGoal(6, new FollowLeaderGoal());
        this.goalSelector.addGoal(7, new TravelGoal());
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(SOLDIER, false);
        builder.define(CALM, false);
        builder.define(CARRIED, ItemStack.EMPTY);
        builder.define(GROWTH, 100);
    }

    // ------------------------------------------------------------------ state

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public CrystalColor colour() {
        return CrystalColor.byId(this.getVariant());
    }

    protected void setVariant(CrystalColor colour) {
        this.entityData.set(VARIANT, colour.ordinal());
    }

    public boolean isSoldier() {
        return this.entityData.get(SOLDIER);
    }

    public boolean isCalm() {
        return this.entityData.get(CALM);
    }

    public ItemStack getCarried() {
        return this.entityData.get(CARRIED);
    }

    private void setCarried(ItemStack stack) {
        this.entityData.set(CARRIED, stack);
    }

    /** True while it plays in a jam. */
    public boolean isJamming() {
        return this.jamTicks > 0;
    }

    /** Makes this Caravan a soldier: bigger, tougher, with crystal-studded claws. */
    public void setSoldier(boolean soldier) {
        this.entityData.set(SOLDIER, soldier);
        base(Attributes.MAX_HEALTH, soldier ? 28.0 : 12.0);
        base(Attributes.ATTACK_DAMAGE, soldier ? 7.0 : 3.0);
        base(Attributes.ARMOR, soldier ? 6.0 : 2.0);
        base(Attributes.MOVEMENT_SPEED, soldier ? 0.27 : 0.3);
        this.updateSize();
        this.setHealth(this.getMaxHealth());
    }

    /** How far the gem crust has grown, 0 (fresh from its larva) to 100. */
    public int getGrowth() {
        return this.entityData.get(GROWTH);
    }

    /** Grows (or sets) the gem crust; a young worker grows to full size on the way. */
    public void setGrowth(int growth) {
        this.entityData.set(GROWTH, Mth.clamp(growth, 0, 100));
        this.updateSize();
    }

    /** Soldiers are half as big again; workers grow from a little over half size to full size with their first crust. */
    protected void updateSize() {
        base(Attributes.SCALE, this.isSoldier() ? 1.45 : 0.55 + 0.45 * Math.min(1.0, this.getGrowth() / (double) ADULT_GROWTH));
    }

    protected void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance a = this.getAttribute(attribute);
        if (a != null) {
            a.setBaseValue(value);
        }
    }

    /** A young worker, just grown out of its larva: small, bare-shelled, in its caravan's colour. */
    public void hatch(CrystalColor colour) {
        this.setVariant(colour);
        this.setSoldier(false);
        this.setGrowth(0);
        this.mineCooldown = 600;
    }

    // ------------------------------------------------------------------ the caravan: colour, Queen, leader

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        SpawnGroupData group = this.joinCaravan(level, reason, data);
        return super.finalizeSpawn(level, difficulty, reason, group);
    }

    /**
     * Takes its caravan's colour and a role: a spawn group shares one colour, else it wears the
     * colour of the Queen or the Caravans around it (see {@link #caravanColour}). Most are workers
     * with a grown crust, one in four a soldier.
     */
    protected SpawnGroupData joinCaravan(ServerLevelAccessor level, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        CrystalColor colour = data instanceof Group group ? group.colour() : caravanColour(level, this.blockPosition(), this.random);
        this.setVariant(colour);
        this.setSoldier(this.random.nextInt(4) == 0);
        this.setGrowth(this.isSoldier() ? 100 : 40 + this.random.nextInt(61));
        return new Group(colour);
    }

    /** The spawn group of a caravan: Caravans spawned together wear the same colour. */
    public record Group(CrystalColor colour) implements SpawnGroupData {
    }

    /** The colour of the caravan at {@code pos}: its Queen's, else the nearest Caravan's, else a new one (gold is rare). */
    public static CrystalColor caravanColour(ServerLevelAccessor level, BlockPos pos, RandomSource random) {
        Vec3 at = Vec3.atCenterOf(pos);
        Caravan best = null;
        double bestD = Double.MAX_VALUE;
        for (CaravanQueen q : level.getEntitiesOfClass(CaravanQueen.class, new AABB(pos).inflate(QUEEN_RANGE), Caravan::isAlive)) {
            if (q.distanceToSqr(at) < bestD) {
                best = q;
                bestD = q.distanceToSqr(at);
            }
        }
        if (best == null) {
            for (Caravan c : level.getEntitiesOfClass(Caravan.class, new AABB(pos).inflate(KIN_RANGE), Caravan::isAlive)) {
                if (c.distanceToSqr(at) < bestD) {
                    best = c;
                    bestD = c.distanceToSqr(at);
                }
            }
        }
        if (best != null) {
            return best.colour();
        }
        int roll = random.nextInt(100);
        return CrystalColor.byId(roll < 8 ? CrystalColor.GOLD.ordinal() : roll % 4);
    }

    /** The Queen this Caravan serves, if she is alive and in this world. */
    public @Nullable CaravanQueen queen() {
        if (this.queenId != null && this.level() instanceof ServerLevel server && server.getEntity(this.queenId) instanceof CaravanQueen q
                && q.isAlive()) {
            return q;
        }
        return null;
    }

    /** Looks for a Queen nearby to serve (every few seconds while it has none). */
    private void findQueen(ServerLevel level) {
        if (this.queen() != null || --this.queenSearch > 0) {
            return;
        }
        this.queenSearch = 100;
        CaravanQueen best = null;
        double bestD = QUEEN_RANGE * QUEEN_RANGE;
        for (CaravanQueen q : level.getEntitiesOfClass(CaravanQueen.class, this.getBoundingBox().inflate(QUEEN_RANGE), o -> o.isAlive() && o != this)) {
            double d = q.distanceToSqr(this);
            if (d < bestD) {
                best = q;
                bestD = d;
            }
        }
        this.queenId = best == null ? null : best.getUUID();
    }

    /** The caravan it follows: the oldest Caravan near it that leads (or null: it leads, or walks alone). */
    public @Nullable Caravan leader() {
        Caravan l = this.leader;
        if (l != null && (!l.isAlive() || l.distanceToSqr(this) > 28.0 * 28.0 || l.leader != null)) {
            this.leader = null;
        }
        return this.leader;
    }

    private void findLeader() {
        if (this.leader() != null || this instanceof CaravanQueen || this.tickCount % 40 != this.getId() % 40) {
            return;
        }
        for (Caravan c : this.level().getEntitiesOfClass(Caravan.class, this.getBoundingBox().inflate(16.0),
                o -> o != this && o.isAlive() && !(o instanceof CaravanQueen) && o.leader == null && o.getId() < this.getId())) {
            if (this.leader == null || c.getId() < this.leader.getId()) {
                this.leader = c;
            }
        }
    }

    // ------------------------------------------------------------------ territory, calm and swarm

    /** Their territory is their Queen's lair. */
    protected boolean inTerritory(LivingEntity target) {
        CaravanQueen q = this.queen();
        return q != null && q.inLair(target.position());
    }

    /**
     * Neutral but territorial: a player who comes into the Queen's lair is warned - the caravan rears
     * up and clacks its claws - and attacked if they are still there when the warning runs out.
     */
    private void warnIntruders(ServerLevel level) {
        if (this.getTarget() != null || this.calmTicks > 0) {
            this.warnTicks = 0;
            this.warned = null;
            return;
        }
        Player intruder = null;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(14.0),
                o -> o.isAlive() && !o.isCreative() && !o.isSpectator() && this.inTerritory(o))) {
            intruder = p;
            break;
        }
        if (intruder == null) {
            this.warnTicks = 0;
            this.warned = null;
            return;
        }
        if (this.warned != intruder) {
            this.warned = intruder;
            this.warnTicks = 0;
        }
        if (this.warnTicks % 30 == 0 && this.warnTicks < WARN_TICKS) {
            level.broadcastEntityEvent(this, EVENT_WARN);
            this.playSound(ModSounds.CARAVAN_WARN.get(), 1.0F, (this.isSoldier() ? 0.8F : 1.1F) + this.random.nextFloat() * 0.2F);
        }
        this.getLookControl().setLookAt(intruder, 30.0F, 30.0F);
        this.warnTicks += 10;
        if (this.warnTicks >= WARN_TICKS) {
            this.setTarget(intruder);
        }
    }

    /** The Crystal Hymn: forget every grudge for a while and play along. */
    public void calm(int ticks) {
        this.calmTicks = ticks;
        this.entityData.set(CALM, true);
        this.setTarget(null);
        this.setAggressive(false);
        this.getNavigation().stop();
        this.jamCooldown = Math.min(this.jamCooldown, 20 + this.random.nextInt(60));
    }

    /** Their Queen was attacked: drop everything and go for the attacker. */
    public void defend(LivingEntity attacker) {
        if (attacker instanceof Player p && (p.isCreative() || p.isSpectator())) {
            return;
        }
        this.calmTicks = 0;
        this.entityData.set(CALM, false);
        this.jamTicks = 0;
        this.setTarget(attacker);
    }

    /** A player broke a crystal or an egg-laden ore at {@code pos}: every Caravan that sees it swarms them. */
    public static void alarm(ServerLevel level, BlockPos pos, Player player) {
        boolean sounded = false;
        for (Caravan c : level.getEntitiesOfClass(Caravan.class, new AABB(pos).inflate(SWARM_RANGE))) {
            if (c.calmTicks > 0 || !c.isAlive()) {
                continue;
            }
            c.jamTicks = 0;
            c.setTarget(player);
            if (!sounded && c.alarmCooldown <= 0) {
                c.playSound(ModSounds.CARAVAN_ALARM.get(), 1.2F, 0.9F + c.random.nextFloat() * 0.2F);
                sounded = true;
            }
            c.alarmCooldown = 60;
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, c.getX(), c.getY() + c.getBbHeight() + 0.3, c.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (this.calmTicks > 0) {
            this.calmTicks = 0;
            this.entityData.set(CALM, false);
        }
        this.jamTicks = 0;
        if (this.alarmCooldown <= 0 && source.getEntity() instanceof Player) {
            this.playSound(ModSounds.CARAVAN_ALARM.get(), 1.0F, 1.0F);
            this.alarmCooldown = 60;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_TAP -> this.tapAnimation.start(this.tickCount);
            case EVENT_MINE -> this.mineAnimation.start(this.tickCount);
            case EVENT_BITE -> this.biteAnimation.start(this.tickCount);
            case EVENT_OFFER -> this.offerAnimation.start(this.tickCount);
            case EVENT_WARN -> this.warnAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            // CAVE: the side-on turn eases in and out - it used to snap a quarter turn whenever the crab picked up or
            // set down its load mid-stride (a carrying worker walks claws-first)
            this.scuttleO = this.scuttle;
            float want = this.getCarried().isEmpty() ? Mth.clamp(this.walkAnimation.speed() * 2.4F - 0.15F, 0.0F, 1.0F) : 0.0F;
            this.scuttle += Mth.clamp(want - this.scuttle, -0.08F, 0.08F);
        }
        if (this.level() instanceof ServerLevel server) {
            if (this.mineCooldown > 0) {
                this.mineCooldown--;
            }
            if (this.jamCooldown > 0) {
                this.jamCooldown--;
            }
            if (this.alarmCooldown > 0) {
                this.alarmCooldown--;
            }
            if (this.jamTicks > 0) {
                this.jamTicks--;
                this.getNavigation().stop();
            }
            if (this.calmTicks > 0) {
                this.calmTicks--;
                if (this.calmTicks == 0) {
                    this.entityData.set(CALM, false);
                } else if (this.tickCount % 30 == 0) {
                    server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 0,
                            this.random.nextDouble(), 0.0, 0.0, 1.0);
                }
            }
            // the crust grows slowly with age
            if (!this.isSoldier() && this.getGrowth() < 100 && this.tickCount % GROWTH_TICKS == 0) {
                this.setGrowth(this.getGrowth() + 1);
            }
            this.findQueen(server);
            this.findLeader();
            if (this.tickCount % 10 == 0) {
                this.warnIntruders(server);
            }
        } else if (!this.getCarried().isEmpty() && this.random.nextInt(20) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.02, 0.0);
        }
    }

    // ------------------------------------------------------------------ the caravan's work

    /** Natural ore: an ore (or crystal) embedded in natural rock (at least three rock faces) with a face open to dig at. */
    public static boolean isNaturalOre(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!state.is(ModCaravans.MINABLE) || state.getDestroySpeed(level, pos) < 0.0F) {
            return false;
        }
        int rock = 0;
        int open = 0;
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(ModCaravans.ROCK)) {
                rock++;
            } else if (n.isAir()) {
                open++;
            }
        }
        return rock >= 3 && open >= 1;
    }

    private @Nullable BlockPos findOre() {
        BlockPos me = this.blockPosition();
        CaravanQueen q = this.queen();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-8, -3, -8), me.offset(8, 4, 8))) {
            double d = p.distSqr(me);
            // never strip the Queen's own lair
            if (d < bestD && isNaturalOre(this.level(), p) && (q == null || !q.inLair(Vec3.atCenterOf(p)))) {
                best = p.immutable();
                bestD = d;
            }
        }
        return best;
    }

    /** What a Caravan leaves in an ore socket: its eggs, in the rock the ore was set in. */
    private static BlockState eggsFor(Level level, BlockPos pos, BlockState ore) {
        boolean deep = BuiltInRegistries.BLOCK.getKey(ore.getBlock()).getPath().contains("deep");
        for (Direction d : Direction.values()) {
            BlockState n = level.getBlockState(pos.relative(d));
            if (n.is(ModBlocks.HUSHSLATE.get()) || n.is(Blocks.DEEPSLATE) || n.is(Blocks.TUFF)) {
                deep = true;
            }
        }
        return (deep ? ModBlocks.DEEP_EGG_LADEN_ORE.get() : ModBlocks.EGG_LADEN_ORE.get()).defaultBlockState();
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel server && !this.getCarried().isEmpty()) {
            this.spawnAtLocation(server, this.getCarried());
            this.setCarried(ItemStack.EMPTY);
        }
        super.die(source);
    }

    /** Digs a natural ore or crystal out of the rock nearby - and often lays its eggs in the socket. */
    private class MineOreGoal extends Goal {
        private @Nullable BlockPos target;
        private int progress;
        private int timeout;

        MineOreGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            if (c.isSoldier() || !c.getCarried().isEmpty() || c.getTarget() != null || c.mineCooldown > 0 || c.isJamming()
                    || !(c.level() instanceof ServerLevel server) || !net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, c)) {
                return false;
            }
            this.target = c.findOre();
            if (this.target == null) {
                c.mineCooldown = 160 + c.random.nextInt(160);
            }
            return this.target != null;
        }

        @Override
        public boolean canContinueToUse() {
            Caravan c = Caravan.this;
            return this.target != null && this.timeout > 0 && c.getCarried().isEmpty() && c.getTarget() == null && isNaturalOre(c.level(), this.target);
        }

        @Override
        public void start() {
            this.progress = 0;
            this.timeout = 400;
            BlockPos t = this.target;
            if (t != null) {
                Caravan.this.getNavigation().moveTo(t.getX() + 0.5, t.getY(), t.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public void stop() {
            if (this.target != null) {
                Caravan.this.level().destroyBlockProgress(Caravan.this.getId(), this.target, -1);
            }
            this.target = null;
            Caravan.this.mineCooldown = Math.max(Caravan.this.mineCooldown, 60);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Caravan c = Caravan.this;
            BlockPos t = this.target;
            if (t == null) {
                return;
            }
            this.timeout--;
            Vec3 at = Vec3.atCenterOf(t);
            c.getLookControl().setLookAt(at.x, at.y, at.z);
            if (c.distanceToSqr(at) > 4.0) {
                if (c.getNavigation().isDone() && this.timeout % 20 == 0) {
                    c.getNavigation().moveTo(at.x, t.getY(), at.z, 1.0);
                }
                return;
            }
            c.getNavigation().stop();
            this.progress++;
            BlockState state = c.level().getBlockState(t);
            if (this.progress % 6 == 0) {
                c.level().broadcastEntityEvent(c, EVENT_MINE);
                c.playSound(state.getSoundType().getHitSound(), 0.7F, 1.3F);
                c.level().destroyBlockProgress(c.getId(), t, Math.min(9, this.progress * 10 / MINE_TICKS));
            }
            if (this.progress >= MINE_TICKS) {
                c.level().destroyBlockProgress(c.getId(), t, -1);
                BlockState eggs = eggsFor(c.level(), t, state);
                c.level().destroyBlock(t, false, c, 512);
                c.setCarried(new ItemStack(state.getBlock()));
                if (c.random.nextFloat() < EGG_CHANCE && c.level() instanceof ServerLevel server) {
                    // its eggs go into the empty socket, sealed in with a little of the rock
                    server.setBlock(t, eggs, Block.UPDATE_ALL);
                    server.sendParticles(ModParticles.GLOW_DUST.get(), at.x, at.y, at.z, 8, 0.3, 0.3, 0.3, 0.01);
                    c.playSound(ModSounds.CARAVAN_LAY_EGGS.get(), 0.8F, 1.0F + c.random.nextFloat() * 0.2F);
                }
                c.mineCooldown = 200 + c.random.nextInt(400);
                this.target = null;
            }
        }
    }

    /** Carries its ore to its Queen and lays it before her. */
    private class DeliverGoal extends Goal {
        private int timeout;

        DeliverGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            return !c.getCarried().isEmpty() && c.getTarget() == null && !c.isJamming() && c.queen() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.timeout > 0 && this.canUse();
        }

        @Override
        public void start() {
            this.timeout = 900;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Caravan c = Caravan.this;
            CaravanQueen q = c.queen();
            if (q == null) {
                return;
            }
            this.timeout--;
            c.getLookControl().setLookAt(q, 20.0F, 20.0F);
            double reach = q.getBbWidth() * 0.5 + 2.0;
            if (c.distanceToSqr(q) > reach * reach) {
                if (c.getNavigation().isDone() || this.timeout % 30 == 0) {
                    c.getNavigation().moveTo(q, 1.0);
                }
                return;
            }
            c.getNavigation().stop();
            c.level().broadcastEntityEvent(c, EVENT_OFFER);
            q.receive(c, c.getCarried());
            c.setCarried(ItemStack.EMPTY);
            c.setGrowth(c.getGrowth() + 6); // a sliver of every gift goes into its own crust
            this.timeout = 0;
        }
    }

    /** Walks in the column behind its leader. */
    private class FollowLeaderGoal extends Goal {
        FollowLeaderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            Caravan l = c.leader();
            return l != null && c.getTarget() == null && !c.isJamming() && c.distanceToSqr(l) > 4.5 * 4.5;
        }

        @Override
        public boolean canContinueToUse() {
            Caravan c = Caravan.this;
            Caravan l = c.leader();
            return l != null && c.getTarget() == null && !c.isJamming() && c.distanceToSqr(l) > 2.5 * 2.5 && !c.getNavigation().isDone();
        }

        @Override
        public void start() {
            Caravan l = Caravan.this.leader();
            if (l != null) {
                Caravan.this.getNavigation().moveTo(l, 1.0);
            }
        }

        @Override
        public void tick() {
            Caravan l = Caravan.this.leader();
            if (l != null && Caravan.this.tickCount % 10 == 0) {
                Caravan.this.getNavigation().moveTo(l, 1.0);
            }
        }
    }

    /** A leader (or a lone Caravan) roams: around its Queen's lair if it has one, far through the caves if not. */
    private class TravelGoal extends Goal {
        TravelGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            return c.leader() == null && c.getTarget() == null && !c.isJamming() && c.random.nextInt(c.queen() != null ? 60 : 40) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return !Caravan.this.getNavigation().isDone() && Caravan.this.getTarget() == null && !Caravan.this.isJamming();
        }

        @Override
        public void start() {
            Caravan c = Caravan.this;
            CaravanQueen q = c.queen();
            BlockPos lair = q == null ? null : q.lair();
            Vec3 to;
            if (lair != null && c.distanceToSqr(Vec3.atCenterOf(lair)) > FORAGE_RANGE * FORAGE_RANGE) {
                to = DefaultRandomPos.getPosTowards(c, 16, 6, Vec3.atCenterOf(lair), Math.PI / 3.0);
            } else {
                to = DefaultRandomPos.getPos(c, q != null ? 12 : 20, 6);
            }
            if (to != null) {
                c.getNavigation().moveTo(to.x, to.y, to.z, 0.8);
            }
        }
    }

    // ------------------------------------------------------------------ the caravan's music

    /**
     * A caravan at rest plays together as a band: the leader (or the Queen, if she is near) plays
     * the Crystal Hymn twice through, workers a harmony a third and a fifth below, soldiers the bass,
     * and everyone clicks its claws on the off-beat; it ends on a chord. Each plays in its own band
     * voice ({@link BandRegistry}).
     */
    private class JamGoal extends Goal {
        private final List<Caravan> band = new ArrayList<>();
        private int beat;
        private int ticks;

        JamGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            if (c.leader() != null || c.jamCooldown > 0 || c.getTarget() != null || !c.getCarried().isEmpty() || c.isJamming()) {
                return false;
            }
            c.jamCooldown = 200;
            this.band.clear();
            CaravanQueen q = c.queen();
            if (q != null && q.distanceToSqr(c) < 12.0 * 12.0 && q.getTarget() == null) {
                this.band.add(q);
            }
            this.band.add(c);
            for (Caravan o : c.level().getEntitiesOfClass(Caravan.class, c.getBoundingBox().inflate(8.0),
                    m -> m != c && m.isAlive() && m.getTarget() == null && !m.isJamming() && !(m instanceof CaravanQueen) && m.getCarried().isEmpty())) {
                if (this.band.size() < 6) {
                    this.band.add(o);
                }
            }
            return this.band.size() >= 2;
        }

        @Override
        public boolean canContinueToUse() {
            return this.beat <= Song.CRYSTAL.length() * 2 && Caravan.this.getTarget() == null;
        }

        @Override
        public void start() {
            this.beat = 0;
            this.ticks = 0;
            int length = (Song.CRYSTAL.length() * 2 + 2) * BEAT + 10;
            for (Caravan m : this.band) {
                m.jamTicks = length;
                m.getNavigation().stop();
            }
        }

        @Override
        public void stop() {
            Caravan c = Caravan.this;
            for (Caravan m : this.band) {
                m.jamTicks = 0;
            }
            this.band.clear();
            c.jamCooldown = c.calmTicks > 0 ? 200 + c.random.nextInt(200) : 900 + c.random.nextInt(1200);
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Caravan c = Caravan.this;
            if (!(c.level() instanceof ServerLevel level)) {
                return;
            }
            this.band.removeIf(m -> !m.isAlive() || m.getTarget() != null);
            if (this.band.isEmpty()) {
                this.beat = Integer.MAX_VALUE / 2;
                return;
            }
            Caravan lead = this.band.get(0);
            for (Caravan m : this.band) {
                if (m != lead) {
                    m.getLookControl().setLookAt(lead, 20.0F, 20.0F);
                }
            }
            this.ticks++;
            int notes = Song.CRYSTAL.length() * 2;
            if (this.ticks % BEAT == BEAT / 2 && this.beat < notes) {
                // the off-beat: every claw clicks
                for (Caravan m : this.band) {
                    level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.NOTE_BLOCK_HAT.value(), SoundSource.NEUTRAL, 0.35F,
                            1.3F + m.getRandom().nextFloat() * 0.2F);
                }
            }
            if (this.ticks % BEAT != 0) {
                return;
            }
            if (this.beat < notes) {
                int melody = Song.CRYSTAL.note(this.beat % Song.CRYSTAL.length());
                for (int i = 0; i < this.band.size(); i++) {
                    Caravan m = this.band.get(i);
                    int part = i == 0 ? melody : m.isSoldier() ? melody - 12 : melody - (i % 2 == 1 ? 4 : 7);
                    // the melody on every beat, soldiers' bass too, the harmony on every other beat
                    if (i == 0 || this.beat % 2 == 0 || m.isSoldier()) {
                        playPart(level, m, part, i == 0 ? 1.0F : 0.7F);
                    }
                }
                SongEvents.note(level, null, lead.position().add(0.0, lead.getBbHeight(), 0.0), melody);
            } else if (this.beat == notes) {
                // the closing chord
                int root = Song.CRYSTAL.note(0);
                int[] chord = {0, 4, 7, 12};
                for (int i = 0; i < this.band.size(); i++) {
                    playPart(level, this.band.get(i), root - chord[i % 4], 0.9F);
                }
            }
            this.beat++;
        }
    }

    /** One note of a jam in this Caravan's band voice, with the tap of its claws. */
    private static void playPart(ServerLevel level, Caravan m, int pitch, float loudness) {
        BandVoice voice = BandRegistry.voiceOf(m);
        if (voice != null) {
            voice.play(level, m, pitch, loudness, true);
        } else {
            float sp = (float) Math.pow(2.0, (BandVoice.fold(pitch) - 12) / 12.0);
            level.playSound(null, m.getX(), m.getY(), m.getZ(), SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), SoundSource.NEUTRAL, loudness, sp);
        }
        level.broadcastEntityEvent(m, EVENT_TAP);
    }

    // ------------------------------------------------------------------ sounds, saving, death

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.CARAVAN_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CARAVAN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CARAVAN_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.CARAVAN_STEP.get(), 0.25F, this.isSoldier() ? 1.0F : 1.3F);
    }

    @Override
    public float getVoicePitch() {
        return super.getVoicePitch() * (this.isSoldier() ? 0.75F : 1.1F);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
        output.putBoolean("Soldier", this.isSoldier());
        output.putInt("CalmTicks", this.calmTicks);
        output.putInt("Growth", this.getGrowth());
        if (!this.getCarried().isEmpty()) {
            output.store("Carried", ItemStack.CODEC, this.getCarried());
        }
        if (this.queenId != null) {
            output.store("Queen", UUIDUtil.CODEC, this.queenId);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(VARIANT, input.getIntOr("Variant", 0));
        this.entityData.set(SOLDIER, input.getBooleanOr("Soldier", false));
        this.calmTicks = input.getIntOr("CalmTicks", 0);
        this.entityData.set(CALM, this.calmTicks > 0);
        this.entityData.set(GROWTH, input.getIntOr("Growth", 100));
        this.setCarried(input.read("Carried", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.queenId = input.read("Queen", UUIDUtil.CODEC).orElse(null);
    }

    /** A burst of crystal shards in the Caravan's colour. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.colour().rgb(), 0xFFFFFF, KillBurst.STAR, ModParticles.GLOW_DUST.get());
    }

    /** True for a mob that is no part of a caravan (the Queen's targets). */
    public static boolean outsider(LivingEntity e) {
        return e instanceof Mob && !(e instanceof Caravan) && !(e instanceof CaravanLarva);
    }
}
