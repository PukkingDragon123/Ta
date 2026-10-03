package com.thesift.entity.caravan;

import com.thesift.block.CrystalColor;
import com.thesift.block.MusicCrystalBlock;
import com.thesift.block.entity.MusicCrystalBlockEntity;
import com.thesift.entity.KillBurst;
import com.thesift.music.Song;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModCaravans;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Caravan: a crystal-backed ant of the Caravans Cavern, in one of five colours.
 *
 * <ul>
 *   <li>Workers dig natural ore out of the rock nearby (never anything a player built: the ore must
 *   sit embedded in natural stone), carry the chunk home in their mandibles and grow it into a new
 *   Music Crystal of their colour - a precious ore is sometimes frozen inside as treasure.</li>
 *   <li>Soldiers are half as big again, with great crystal jaws, and guard the colony: anyone who
 *   comes close is attacked.</li>
 *   <li>Idle Caravans tap the colony's crystals with their antennae and play phrases of the Crystal
 *   Hymn (soldiers a fifth below the workers).</li>
 *   <li>Hit one, or break a crystal near them, and the whole colony swarms. Play them the Crystal
 *   Hymn and they calm down for a few minutes and sing along.</li>
 * </ul>
 */
public class Caravan extends Monster {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SOLDIER = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> CALM = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<ItemStack> CARRIED = SynchedEntityData.defineId(Caravan.class, EntityDataSerializers.ITEM_STACK);
    private static final byte EVENT_TAP = 100;
    private static final byte EVENT_MINE = 101;
    private static final byte EVENT_BITE = 102;
    private static final byte EVENT_BUILD = 103;
    private static final int MINE_TICKS = 60;
    private static final int MAX_CRYSTALS = 40;
    private static final double SWARM_RANGE = 24.0;

    public final AnimationState tapAnimation = new AnimationState();
    public final AnimationState mineAnimation = new AnimationState();
    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState buildAnimation = new AnimationState();
    private @Nullable BlockPos home;
    private int calmTicks;
    private int mineCooldown = 100;
    private int singCooldown = 200;
    private int alarmCooldown;

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
        this.goalSelector.addGoal(3, new CarryHomeGoal());
        this.goalSelector.addGoal(4, new MineOreGoal());
        this.goalSelector.addGoal(5, new SingGoal());
        this.goalSelector.addGoal(6, new ReturnHomeGoal());
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true, (target, level) -> this.isHostileTo(target)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(SOLDIER, false);
        builder.define(CALM, false);
        builder.define(CARRIED, ItemStack.EMPTY);
    }

    // ------------------------------------------------------------------ state

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public CrystalColor colour() {
        return CrystalColor.byId(this.getVariant());
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

    /** Makes this Caravan a soldier: bigger, tougher, with crystal jaws. */
    public void setSoldier(boolean soldier) {
        this.entityData.set(SOLDIER, soldier);
        base(Attributes.MAX_HEALTH, soldier ? 28.0 : 12.0);
        base(Attributes.ATTACK_DAMAGE, soldier ? 7.0 : 3.0);
        base(Attributes.ARMOR, soldier ? 6.0 : 2.0);
        base(Attributes.MOVEMENT_SPEED, soldier ? 0.27 : 0.3);
        base(Attributes.SCALE, soldier ? 1.45 : 1.0);
        this.setHealth(this.getMaxHealth());
    }

    private void base(net.minecraft.core.Holder<net.minecraft.world.entity.ai.attributes.Attribute> attribute, double value) {
        AttributeInstance a = this.getAttribute(attribute);
        if (a != null) {
            a.setBaseValue(value);
        }
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        int roll = this.random.nextInt(100);
        this.entityData.set(VARIANT, roll < 8 ? CrystalColor.GOLD.ordinal() : roll % 4);
        this.setSoldier(this.random.nextInt(10) < 3);
        this.home = this.blockPosition();
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    private boolean isHostileTo(net.minecraft.world.entity.LivingEntity target) {
        if (this.calmTicks > 0) {
            return false;
        }
        if (this.isSoldier()) {
            return this.home == null || target.distanceToSqr(Vec3.atCenterOf(this.home)) < 18.0 * 18.0;
        }
        return this.distanceToSqr(target) < 5.0 * 5.0;
    }

    // ------------------------------------------------------------------ calm & swarm

    /** The Crystal Hymn: forget every grudge for a while and sing along. */
    public void calm(int ticks) {
        this.calmTicks = ticks;
        this.entityData.set(CALM, true);
        this.setTarget(null);
        this.setAggressive(false);
        this.getNavigation().stop();
        this.singCooldown = 10 + this.random.nextInt(60);
    }

    /** A player broke a colony crystal at {@code pos}: every Caravan that sees it swarms them. */
    public static void alarm(ServerLevel level, BlockPos pos, Player player) {
        boolean sounded = false;
        for (Caravan c : level.getEntitiesOfClass(Caravan.class, new AABB(pos).inflate(SWARM_RANGE))) {
            if (c.calmTicks > 0 || !c.isAlive()) {
                continue;
            }
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
            case EVENT_BUILD -> this.buildAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.mineCooldown > 0) {
                this.mineCooldown--;
            }
            if (this.singCooldown > 0) {
                this.singCooldown--;
            }
            if (this.alarmCooldown > 0) {
                this.alarmCooldown--;
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
            if (this.home == null) {
                this.home = this.blockPosition();
            }
        } else if (!this.getCarried().isEmpty() && this.random.nextInt(20) == 0) {
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getX(), this.getY() + 0.4, this.getZ(), 0.0, 0.02, 0.0);
        }
    }

    // ------------------------------------------------------------------ the colony's work

    /** Natural ore: an ore embedded in natural rock (at least three rock faces) with a face open to dig at. */
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
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-8, -3, -8), me.offset(8, 4, 8))) {
            double d = p.distSqr(me);
            if (d < bestD && isNaturalOre(this.level(), p) && (this.home == null || !p.closerThan(this.home, 4.0))) {
                best = p.immutable();
                bestD = d;
            }
        }
        return best;
    }

    private @Nullable BlockPos findCrystal(int r) {
        BlockPos me = this.blockPosition();
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(me.offset(-r, -2, -r), me.offset(r, 3, r))) {
            double d = p.distSqr(me);
            if (d < bestD && this.level().getBlockState(p).is(ModBlocks.MUSIC_CRYSTAL.get())) {
                best = p.immutable();
                bestD = d;
            }
        }
        return best;
    }

    /** Grows the carried ore into a music crystal of this Caravan's colour near home; a precious ore may be frozen inside. */
    private void growCrystal(ServerLevel level) {
        ItemStack ore = this.getCarried();
        BlockPos h = this.home != null ? this.home : this.blockPosition();
        int crystals = 0;
        for (BlockPos p : BlockPos.betweenClosed(h.offset(-6, -3, -6), h.offset(6, 4, 6))) {
            if (level.getBlockState(p).is(ModBlocks.MUSIC_CRYSTAL.get())) {
                crystals++;
            }
        }
        if (crystals < MAX_CRYSTALS) {
            for (int attempt = 0; attempt < 24; attempt++) {
                BlockPos p = h.offset(this.random.nextInt(9) - 4, this.random.nextInt(5) - 2, this.random.nextInt(9) - 4);
                if (!level.getBlockState(p).isAir()) {
                    continue;
                }
                for (Direction facing : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                    BlockState state = ModBlocks.MUSIC_CRYSTAL.get().defaultBlockState().setValue(MusicCrystalBlock.COLOR, this.colour())
                            .setValue(MusicCrystalBlock.FACING, facing);
                    if (!state.canSurvive(level, p)) {
                        continue;
                    }
                    ItemStack treasure = ItemStack.EMPTY;
                    if (this.random.nextInt(3) == 0 && ore.getItem() instanceof net.minecraft.world.item.BlockItem bi) {
                        List<ItemStack> drops = Block.getDrops(bi.getBlock().defaultBlockState(), level, p, null, this, new ItemStack(Items.DIAMOND_PICKAXE));
                        if (!drops.isEmpty()) {
                            treasure = drops.get(0);
                        }
                    }
                    level.setBlock(p, state.setValue(MusicCrystalBlock.FROZEN, !treasure.isEmpty()), Block.UPDATE_ALL);
                    if (!treasure.isEmpty() && level.getBlockEntity(p) instanceof MusicCrystalBlockEntity be) {
                        be.setItem(treasure);
                    }
                    level.broadcastEntityEvent(this, EVENT_BUILD);
                    level.playSound(null, p, ModSounds.CARAVAN_BUILD.get(), SoundSource.NEUTRAL, 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
                    level.sendParticles(ModParticles.STAR_SPARKLE.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
                    this.setCarried(ItemStack.EMPTY);
                    return;
                }
            }
        }
        // no room left to grow: the ore goes on the colony's pile
        this.spawnAtLocation(level, ore);
        this.setCarried(ItemStack.EMPTY);
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel server && !this.getCarried().isEmpty()) {
            this.spawnAtLocation(server, this.getCarried());
            this.setCarried(ItemStack.EMPTY);
        }
        super.die(source);
    }

    /** Digs a natural ore out of the rock nearby. */
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
            if (c.isSoldier() || !c.getCarried().isEmpty() || c.getTarget() != null || c.mineCooldown > 0 || !(c.level() instanceof ServerLevel server)
                    || !net.neoforged.neoforge.event.EventHooks.canEntityGrief(server, c)) {
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
            Caravan.this.getNavigation().moveTo(this.target.getX() + 0.5, this.target.getY(), this.target.getZ() + 0.5, 1.0);
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
            if (this.target == null) {
                return;
            }
            this.timeout--;
            Vec3 at = Vec3.atCenterOf(this.target);
            c.getLookControl().setLookAt(at.x, at.y, at.z);
            if (c.distanceToSqr(at) > 4.0) {
                if (c.getNavigation().isDone() && this.timeout % 20 == 0) {
                    c.getNavigation().moveTo(at.x, this.target.getY(), at.z, 1.0);
                }
                return;
            }
            c.getNavigation().stop();
            this.progress++;
            BlockState state = c.level().getBlockState(this.target);
            if (this.progress % 6 == 0) {
                c.level().broadcastEntityEvent(c, EVENT_MINE);
                c.playSound(state.getSoundType().getHitSound(), 0.7F, 1.3F);
                c.level().destroyBlockProgress(c.getId(), this.target, Math.min(9, this.progress * 10 / MINE_TICKS));
            }
            if (this.progress >= MINE_TICKS) {
                c.level().destroyBlockProgress(c.getId(), this.target, -1);
                c.level().destroyBlock(this.target, false, c, 512);
                c.setCarried(new ItemStack(state.getBlock()));
                c.mineCooldown = 200 + c.random.nextInt(400);
                this.target = null;
            }
        }
    }

    /** Carries the ore home and grows it into a crystal. */
    private class CarryHomeGoal extends Goal {
        private int timer;
        private int timeout;

        CarryHomeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return !Caravan.this.getCarried().isEmpty() && Caravan.this.getTarget() == null && Caravan.this.home != null;
        }

        @Override
        public void start() {
            this.timer = 0;
            this.timeout = 900;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Caravan c = Caravan.this;
            if (c.home == null || !(c.level() instanceof ServerLevel server)) {
                return;
            }
            Vec3 h = Vec3.atBottomCenterOf(c.home);
            if (--this.timeout <= 0) {
                c.growCrystal(server); // lost the way: grow it right here
                return;
            }
            if (c.distanceToSqr(h) > 9.0) {
                if (c.getNavigation().isDone() || this.timeout % 40 == 0) {
                    c.getNavigation().moveTo(h.x, h.y, h.z, 1.0);
                }
                return;
            }
            c.getNavigation().stop();
            if (++this.timer >= 20) {
                c.growCrystal(server);
            }
        }
    }

    /** Taps a crystal with the antennae and plays a phrase of the Crystal Hymn on it. */
    private class SingGoal extends Goal {
        private @Nullable BlockPos crystal;
        private int note;
        private int timer;

        SingGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            if (c.getTarget() != null || !c.getCarried().isEmpty() || c.singCooldown > 0) {
                return false;
            }
            this.crystal = c.findCrystal(6);
            if (this.crystal == null) {
                c.singCooldown = 200;
            }
            return this.crystal != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.crystal != null && this.note < Song.CRYSTAL.length() && this.timer < 400 && Caravan.this.getTarget() == null
                    && Caravan.this.level().getBlockState(this.crystal).is(ModBlocks.MUSIC_CRYSTAL.get());
        }

        @Override
        public void start() {
            this.note = 0;
            this.timer = 0;
            Caravan.this.getNavigation().moveTo(this.crystal.getX() + 0.5, this.crystal.getY(), this.crystal.getZ() + 0.5, 0.9);
        }

        @Override
        public void stop() {
            Caravan c = Caravan.this;
            c.singCooldown = c.calmTicks > 0 ? 100 + c.random.nextInt(200) : 400 + c.random.nextInt(800);
            this.crystal = null;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Caravan c = Caravan.this;
            if (this.crystal == null || !(c.level() instanceof ServerLevel server)) {
                return;
            }
            this.timer++;
            Vec3 at = Vec3.atCenterOf(this.crystal);
            c.getLookControl().setLookAt(at.x, at.y, at.z);
            if (c.distanceToSqr(at) > 5.0) {
                if (c.getNavigation().isDone() && this.timer % 20 == 0) {
                    c.getNavigation().moveTo(at.x, this.crystal.getY(), at.z, 0.9);
                }
                return;
            }
            c.getNavigation().stop();
            if (this.timer % 8 == 0) {
                BlockState state = server.getBlockState(this.crystal);
                int pitch = Math.max(0, Song.CRYSTAL.note(this.note) - (c.isSoldier() ? 7 : 0));
                server.broadcastEntityEvent(c, EVENT_TAP);
                MusicCrystalBlock.ring(server, this.crystal, state, pitch, null);
                this.note++;
            }
        }
    }

    /** Wanders back towards the colony when it strays too far. */
    private class ReturnHomeGoal extends Goal {
        ReturnHomeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Caravan c = Caravan.this;
            return c.home != null && c.getTarget() == null && c.random.nextInt(20) == 0 && c.distanceToSqr(Vec3.atCenterOf(c.home)) > 22.0 * 22.0;
        }

        @Override
        public boolean canContinueToUse() {
            Caravan c = Caravan.this;
            return c.home != null && !c.getNavigation().isDone() && c.distanceToSqr(Vec3.atCenterOf(c.home)) > 8.0 * 8.0;
        }

        @Override
        public void start() {
            Caravan c = Caravan.this;
            if (c.home != null) {
                c.getNavigation().moveTo(c.home.getX() + 0.5, c.home.getY(), c.home.getZ() + 0.5, 1.0);
            }
        }
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
        if (!this.getCarried().isEmpty()) {
            output.store("Carried", ItemStack.CODEC, this.getCarried());
        }
        if (this.home != null) {
            output.store("Home", BlockPos.CODEC, this.home);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.entityData.set(VARIANT, input.getIntOr("Variant", 0));
        this.entityData.set(SOLDIER, input.getBooleanOr("Soldier", false));
        this.calmTicks = input.getIntOr("CalmTicks", 0);
        this.entityData.set(CALM, this.calmTicks > 0);
        this.setCarried(input.read("Carried", ItemStack.CODEC).orElse(ItemStack.EMPTY));
        this.home = input.read("Home", BlockPos.CODEC).orElse(null);
    }

    /** A burst of crystal shards in the Caravan's colour. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.colour().rgb(), 0xFFFFFF, KillBurst.STAR, ModParticles.GLOW_DUST.get());
    }
}
