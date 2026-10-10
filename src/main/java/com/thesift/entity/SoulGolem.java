package com.thesift.entity;

import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModTags;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrushableBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.Tags;
import org.jspecify.annotations.Nullable;

/**
 * A Soul Golem (CAVE v4): a copper mole built like the Copper Golem - riveted copper going green in the seams,
 * button eyes, a lightning-rod antenna with a soul lamp in its cap - with a drill for a nose and great spade
 * forepaws. It digs.
 *
 * <ul>
 *   <li>Every so often it scrabbles into the ground (soil, sand, gravel, soul soil), burrows out of sight
 *   with only its antenna showing, and comes up again holding a find. What it finds depends on where it digs
 *   ({@link ModEchoer#golemDigTable}): topsoil finds, ores deeper down, rich ores below zero, prism gems in the
 *   Caravans' cavern, sculk finds in sculk country, relics in the sands; now and then a relic or a music
 *   sheet.</li>
 *   <li>Underground it senses ore nearby: sparks run from its burrow to the ore, it pings, and it comes up
 *   with something far more often.</li>
 *   <li>Yours (a Soul Golem Core used on soul soil) digs near you and carries its finds to a chest or barrel
 *   within ten blocks, like a copper golem sorting, or else to you; with nowhere to put a find it sets it
 *   down at its feet. Wild golems (around an Echoer's hearth) keep their finds - use one empty-handed to be
 *   given it - and pick up anything shiny left lying about, and peer at suspicious blocks.</li>
 *   <li>Owned golems run on soul energy, which slowly drains as they work; at zero they slump where they
 *   stand. Any music nearby recharges them a little and the Golem Hymn completely.</li>
 * </ul>
 */
public class SoulGolem extends PathfinderMob {
    public static final int MAX_ENERGY = 100;
    public static final int NORMAL = 0;
    /** Scrabbling its way into the ground. */
    public static final int DIGGING = 1;
    public static final int PEEKING = 2;
    /** Under the ground, only its antenna showing. */
    public static final int BURROWED = 3;
    /** Coming up again, with its find. */
    public static final int EMERGING = 4;
    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OWNED = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.BOOLEAN);
    /** Its find (or a wild golem's treasure): held up in its forepaws. */
    private static final EntityDataAccessor<ItemStack> STASH = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.ITEM_STACK);
    private static final byte EVENT_HAPPY = 101;
    private static final byte EVENT_EMERGE = 102;
    private static final int DIG_COST = 8;
    private static final int DIG_IN_TIME = 30;
    private static final int EMERGE_TIME = 22;
    private static final int CHEST_RANGE = 10;

    public final AnimationState happyAnimation = new AnimationState();
    public final AnimationState emergeAnimation = new AnimationState();
    /** Client-side smoothed amounts: slump (0 upright, 1 run down), digging, peeking and how far under the ground it is. */
    public float slumpO, slump, digO, dig, peekO, peek, burrowO, burrow;

    private java.util.@Nullable UUID owner;
    private int drainTimer;
    /** It walks about a while before its first dig. */
    private int digCooldown = 600;

    public SoulGolem(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 16.0).add(Attributes.MOVEMENT_SPEED, 0.24).add(Attributes.FOLLOW_RANGE, 20.0);
    }

    /** Any note recharges nearby golems a little; the Golem Hymn fills them up. */
    public static void listen() {
        SongEvents.listenNotes((level, player, at, pitch) -> {
            for (SoulGolem g : level.getEntitiesOfClass(SoulGolem.class, new AABB(at, at).inflate(10.0))) {
                g.addEnergy(3, false);
            }
        });
        SongEvents.listenSongs((level, player, at, song) -> {
            if (song == Song.GOLEM) {
                for (SoulGolem g : level.getEntitiesOfClass(SoulGolem.class, new AABB(at, at).inflate(16.0))) {
                    g.addEnergy(MAX_ENERGY, true);
                }
            }
        });
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
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.3) {
            @Override
            public boolean canUse() {
                return !SoulGolem.this.isSlumped() && super.canUse();
            }
        });
        this.goalSelector.addGoal(2, new DeliverGoal());
        this.goalSelector.addGoal(3, new DigGoal());
        this.goalSelector.addGoal(4, new FollowOwner());
        this.goalSelector.addGoal(5, new FetchItemGoal());
        this.goalSelector.addGoal(6, new PeekGoal());
        this.goalSelector.addGoal(7, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override
            public boolean canUse() {
                return !SoulGolem.this.isSlumped() && super.canUse();
            }
        });
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 6.0F) {
            @Override
            public boolean canUse() {
                return !SoulGolem.this.isSlumped() && super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ENERGY, MAX_ENERGY);
        builder.define(MODE, NORMAL);
        builder.define(OWNED, false);
        builder.define(STASH, ItemStack.EMPTY);
    }

    public int getEnergy() {
        return this.entityData.get(ENERGY);
    }

    public int getMode() {
        return this.entityData.get(MODE);
    }

    private void setMode(int mode) {
        this.entityData.set(MODE, mode);
    }

    /** What it is holding: its find, or (wild) the treasure it picked up. */
    public ItemStack getStash() {
        return this.entityData.get(STASH);
    }

    private void setStash(ItemStack stack) {
        this.entityData.set(STASH, stack);
    }

    public boolean isSlumped() {
        return this.getEnergy() <= 0;
    }

    public boolean isOwned() {
        return this.entityData.get(OWNED);
    }

    public void setOwner(Player player) {
        this.owner = player.getUUID();
        this.entityData.set(OWNED, true);
        this.setPersistenceRequired();
    }

    private @Nullable Player ownerPlayer() {
        return this.owner == null ? null : this.level().getPlayerByUUID(this.owner);
    }

    public void addEnergy(int amount, boolean song) {
        int before = this.getEnergy();
        int after = Math.min(MAX_ENERGY, before + amount);
        this.entityData.set(ENERGY, after);
        if (this.level() instanceof ServerLevel server && (song || before == 0) && after > before) {
            this.playSound(ModEchoer.GOLEM_RECHARGE.get(), 1.0F, song ? 1.2F : 1.0F);
            server.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 0.6, this.getZ(), song ? 24 : 6, 0.3, 0.3, 0.3, 0.03);
            if (song) {
                server.broadcastEntityEvent(this, EVENT_HAPPY);
                server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 1.2, this.getZ(), 0, this.random.nextDouble(), 0, 0, 1);
                if (this.onGround() && this.getMode() == NORMAL) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.42, 0.0));
                }
            }
        }
    }

    private void spendEnergy(int amount) {
        if (!this.isOwned()) {
            return; // wild golems drink from the hut's soul fire
        }
        int before = this.getEnergy();
        int after = Math.max(0, before - amount);
        this.entityData.set(ENERGY, after);
        if (before > 0 && after == 0) {
            this.getNavigation().stop();
            this.setMode(NORMAL);
            this.playSound(ModEchoer.GOLEM_SLUMP.get(), 1.0F, 1.0F);
            if (this.ownerPlayer() instanceof Player p && p.distanceToSqr(this) < 32 * 32) {
                p.sendOverlayMessage(Component.translatable("message.thesift.soul_golem.slumped"));
            }
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level().isClientSide()) {
            int mode = this.getMode();
            this.slumpO = this.slump;
            this.slump = Mth.clamp(this.slump + (this.isSlumped() ? 0.06F : -0.1F), 0.0F, 1.0F);
            this.digO = this.dig;
            this.peekO = this.peek;
            this.burrowO = this.burrow;
            this.dig = Mth.clamp(this.dig + (mode == DIGGING || mode == EMERGING ? 0.14F : -0.08F), 0.0F, 1.0F);
            this.peek = Mth.clamp(this.peek + (mode == PEEKING ? 0.08F : -0.08F), 0.0F, 1.0F);
            // it sinks steadily into its burrow and pops up out of it quickly
            this.burrow = Mth.clamp(this.burrow + (mode == BURROWED ? 0.06F : mode == DIGGING ? 0.012F : -0.12F), 0.0F, 1.0F);
            if (!this.isSlumped() && this.random.nextInt(14) == 0) {
                // a wisp of soul fire from its antenna's lamp
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.25 - this.burrow * 0.6, this.getZ(), 0, 0.01, 0);
            }
            return;
        }
        if (this.isSlumped()) {
            this.getNavigation().stop();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        } else if (++this.drainTimer >= 60) {
            this.drainTimer = 0;
            this.spendEnergy(1);
        }
        this.digCooldown = Math.max(0, this.digCooldown - 1);
        // wild golems pick up valuables they walk over (only one stack at a time)
        if (!this.isOwned() && this.getStash().isEmpty() && !this.isSlumped() && this.getMode() == NORMAL && this.tickCount % 5 == 0) {
            List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.6),
                    e -> e.isAlive() && !e.hasPickUpDelay());
            if (!items.isEmpty()) {
                ItemEntity it = items.get(0);
                this.setStash(it.getItem().copy());
                this.take(it, it.getItem().getCount());
                it.discard();
                this.playSound(ModEchoer.GOLEM_FIND.get(), 0.8F, 1.3F);
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getItemInHand(hand).isEmpty()) {
            return super.mobInteract(player, hand);
        }
        if (!this.level().isClientSide()) {
            if (!this.getStash().isEmpty()) {
                // it shows you its treasure and hands it over
                ItemStack give = this.getStash().copy();
                this.setStash(ItemStack.EMPTY);
                if (!player.getInventory().add(give)) {
                    this.spawnAtLocation((ServerLevel) this.level(), give);
                }
                this.playSound(ModEchoer.GOLEM_FIND.get(), 1.0F, 1.1F);
                this.level().broadcastEntityEvent(this, EVENT_HAPPY);
            } else if (this.isOwned()) {
                player.sendOverlayMessage(Component.translatable("message.thesift.soul_golem.energy", this.getEnergy()));
            } else {
                this.playSound(ModEchoer.GOLEM_AMBIENT.get(), 1.0F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_HAPPY) {
            this.happyAnimation.start(this.tickCount);
        } else if (id == EVENT_EMERGE) {
            this.emergeAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!this.getStash().isEmpty()) {
            this.spawnAtLocation(level, this.getStash().copy());
            this.setStash(ItemStack.EMPTY);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.owner);
        output.putInt("Energy", this.getEnergy());
        if (!this.getStash().isEmpty()) {
            output.store("Stash", ItemStack.CODEC, this.getStash());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(OWNED, this.owner != null);
        this.entityData.set(ENERGY, Mth.clamp(input.getIntOr("Energy", MAX_ENERGY), 0, MAX_ENERGY));
        this.setStash(input.read("Stash", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isSlumped() || this.getMode() == BURROWED ? null : ModEchoer.GOLEM_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModEchoer.GOLEM_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModEchoer.GOLEM_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModEchoer.GOLEM_STEP.get(), 0.4F, 1.0F + this.random.nextFloat() * 0.3F);
    }

    /** Copper and soul-cyan motes. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xC87456, 0x5FE9FF, KillBurst.NOTE, ModParticles.DRIFTING_SOUL.get());
    }

    // ------------------------------------------------------------------ goals

    /** Owned: keep near the owner (and catch up if left far behind). */
    private final class FollowOwner extends Goal {
        FollowOwner() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = SoulGolem.this.ownerPlayer();
            return p != null && !SoulGolem.this.isSlumped() && !p.isSpectator() && SoulGolem.this.distanceToSqr(p) > 7.0 * 7.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = SoulGolem.this.ownerPlayer();
            return p != null && !SoulGolem.this.isSlumped() && SoulGolem.this.distanceToSqr(p) > 3.0 * 3.0;
        }

        @Override
        public void tick() {
            Player p = SoulGolem.this.ownerPlayer();
            if (p == null) {
                return;
            }
            SoulGolem.this.getLookControl().setLookAt(p);
            if (SoulGolem.this.distanceToSqr(p) > 28.0 * 28.0 && p.onGround()) {
                SoulGolem.this.snapTo(p.getX(), p.getY(), p.getZ(), SoulGolem.this.getYRot(), 0.0F);
                SoulGolem.this.getNavigation().stop();
            } else if (SoulGolem.this.tickCount % 10 == 0) {
                SoulGolem.this.getNavigation().moveTo(p, 1.15);
            }
        }

        @Override
        public void stop() {
            SoulGolem.this.getNavigation().stop();
        }
    }

    /** Ground it can burrow into. */
    private static boolean diggable(BlockState st) {
        return st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(ModTags.Blocks.SIFT_PLANTABLE) || st.is(BlockTags.SOUL_FIRE_BASE_BLOCKS)
                || st.is(Tags.Blocks.GRAVELS);
    }

    /** The nearest ore within a few blocks of its burrow (it can feel it through the ground), or null. */
    private static @Nullable BlockPos senseOre(ServerLevel level, BlockPos at) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-5, -6, -5), at.offset(5, 2, 5))) {
            if (level.getBlockState(p).is(Tags.Blocks.ORES)) {
                double d = p.distSqr(at);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** A chest or barrel within reach of {@code at} to put finds in (the nearest), or null. */
    private @Nullable BlockPos findChest(BlockPos at) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-CHEST_RANGE, -3, -CHEST_RANGE), at.offset(CHEST_RANGE, 3, CHEST_RANGE))) {
            var be = this.level().getBlockEntity(p);
            if (be instanceof ChestBlockEntity || be instanceof BarrelBlockEntity) {
                double d = p.distSqr(at);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /**
     * Burrows in, digs about underground for a while, and comes up with a find from the ground it dug in.
     * Owned golems dig near their owner (it costs them energy); wild ones dig wherever they wander, now and then.
     */
    private final class DigGoal extends Goal {
        private @Nullable BlockPos spot;
        private @Nullable BlockPos ore;
        private int phase;
        private int time;
        private int underTime;

        DigGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            SoulGolem g = SoulGolem.this;
            if (g.isSlumped() || g.digCooldown > 0 || !g.getStash().isEmpty()) {
                return false;
            }
            BlockPos centre;
            if (g.isOwned()) {
                Player p = g.ownerPlayer();
                if (p == null || g.getEnergy() < DIG_COST + 2 || g.distanceToSqr(p) > 12 * 12) {
                    return false;
                }
                centre = p.blockPosition();
            } else {
                centre = g.blockPosition();
            }
            for (int i = 0; i < 12; i++) {
                BlockPos at = centre.offset(g.random.nextInt(9) - 4, -1, g.random.nextInt(9) - 4);
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos s = at.above(dy);
                    if (diggable(g.level().getBlockState(s)) && g.level().getBlockState(s.above()).isAir()
                            && g.level().getBlockState(s.above(2)).isAir()) {
                        this.spot = s;
                        return true;
                    }
                }
            }
            g.digCooldown = 100;
            return false;
        }

        @Override
        public void start() {
            this.phase = 0;
            this.time = 0;
            this.ore = null;
            BlockPos s = this.spot;
            if (s != null) {
                SoulGolem.this.getNavigation().moveTo(s.getX() + 0.5, s.getY() + 1.0, s.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.spot != null && !SoulGolem.this.isSlumped();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SoulGolem g = SoulGolem.this;
            BlockPos s = this.spot;
            if (s == null || !(g.level() instanceof ServerLevel server)) {
                return;
            }
            this.time++;
            Vec3 c = Vec3.atCenterOf(s);
            BlockState st = server.getBlockState(s);
            BlockParticleOption dirt = new BlockParticleOption(ParticleTypes.BLOCK, st.isAir() ? server.getBlockState(g.blockPosition().below()) : st);
            if (this.phase == 0) {
                // walk over to the spot
                if (g.distanceToSqr(c.x, s.getY() + 1.0, c.z) > 1.4 * 1.4) {
                    if (this.time % 20 == 0) {
                        g.getNavigation().moveTo(c.x, s.getY() + 1.0, c.z, 1.0);
                    }
                    if (this.time > 200) {
                        this.spot = null;
                    }
                    return;
                }
                g.getNavigation().stop();
                g.setMode(DIGGING);
                this.phase = 1;
                this.time = 0;
                return;
            }
            g.getNavigation().stop();
            g.setDeltaMovement(g.getDeltaMovement().multiply(0.0, 1.0, 0.0));
            if (this.phase == 1) {
                // scrabbling in, drill whirring, soil flying
                g.getLookControl().setLookAt(c.x, s.getY() + 0.5, c.z);
                if (this.time % 3 == 0) {
                    server.sendParticles(dirt, g.getX(), s.getY() + 1.05, g.getZ(), 6, 0.35, 0.08, 0.35, 0.12);
                }
                if (this.time % 6 == 0) {
                    g.playSound(ModEchoer.GOLEM_DIG.get(), 0.7F, 0.9F + g.random.nextFloat() * 0.3F);
                }
                if (this.time >= DIG_IN_TIME) {
                    g.setMode(BURROWED);
                    g.playSound(ModEchoer.GOLEM_BURROW.get(), 1.0F, 1.0F);
                    server.sendParticles(dirt, g.getX(), s.getY() + 1.05, g.getZ(), 20, 0.4, 0.1, 0.4, 0.15);
                    this.ore = senseOre(server, s);
                    this.phase = 2;
                    this.time = 0;
                    this.underTime = 80 + g.random.nextInt(60);
                }
            } else if (this.phase == 2) {
                // under the ground: the soil heaves where it digs, now and then a muffled scrape
                if (this.time % 7 == 0) {
                    server.sendParticles(dirt, g.getX() + (g.random.nextDouble() - 0.5) * 0.6, s.getY() + 1.02, g.getZ() + (g.random.nextDouble() - 0.5) * 0.6,
                            3, 0.15, 0.02, 0.15, 0.05);
                }
                if (this.time % 25 == 0) {
                    g.playSound(ModEchoer.GOLEM_DIG.get(), 0.4F, 0.7F);
                }
                BlockPos o = this.ore;
                if (o != null && this.time % 20 == 10) {
                    // it has felt ore: sparks run through the ground from its burrow to it, and it pings
                    Vec3 from = new Vec3(g.getX(), s.getY() + 1.1, g.getZ());
                    Vec3 to = Vec3.atCenterOf(o);
                    for (int k = 1; k <= 6; k++) {
                        Vec3 q = from.lerp(to, k / 6.0);
                        server.sendParticles(ParticleTypes.ELECTRIC_SPARK, q.x, Math.max(q.y, s.getY() + 1.05), q.z, 1, 0.05, 0.05, 0.05, 0.0);
                    }
                    if (this.time == 10) {
                        g.playSound(ModEchoer.GOLEM_PING.get(), 1.0F, 1.0F);
                    }
                }
                if (this.time >= this.underTime) {
                    this.emerge(server, s, c, dirt);
                    this.phase = 3;
                    this.time = 0;
                }
            } else if (this.time >= EMERGE_TIME) {
                this.spot = null;
            }
        }

        /** Up it comes in a spray of soil, holding whatever it found. */
        private void emerge(ServerLevel server, BlockPos s, Vec3 c, BlockParticleOption dirt) {
            SoulGolem g = SoulGolem.this;
            g.setMode(EMERGING);
            server.broadcastEntityEvent(g, EVENT_EMERGE);
            g.playSound(ModEchoer.GOLEM_EMERGE.get(), 1.0F, 1.0F);
            server.sendParticles(dirt, g.getX(), s.getY() + 1.1, g.getZ(), 28, 0.4, 0.2, 0.4, 0.2);
            float chance = g.isOwned() ? (this.ore != null ? 0.85F : 0.6F) : 0.5F;
            if (g.random.nextFloat() < chance) {
                List<ItemStack> found = new ArrayList<>();
                g.dropFromGiftLootTable(server, ModEchoer.golemDigTable(server, s), (l, stack) -> found.add(stack));
                for (int i = 0; i < found.size(); i++) {
                    if (i == 0) {
                        g.setStash(found.get(0));
                    } else {
                        g.spawnAtLocation(server, found.get(i));
                    }
                }
                if (!found.isEmpty()) {
                    g.playSound(ModEchoer.GOLEM_FIND.get(), 1.0F, 1.2F);
                    server.broadcastEntityEvent(g, EVENT_HAPPY);
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, s.getY() + 1.4, c.z, 6, 0.3, 0.2, 0.3, 0.0);
                }
            }
            g.spendEnergy(DIG_COST);
        }

        @Override
        public void stop() {
            SoulGolem g = SoulGolem.this;
            if (g.getMode() != NORMAL && g.level() instanceof ServerLevel server && this.phase == 2 && this.spot != null) {
                // interrupted underground (hurt, run down): it scrambles out empty-pawed
                server.broadcastEntityEvent(g, EVENT_EMERGE);
                g.playSound(ModEchoer.GOLEM_EMERGE.get(), 1.0F, 1.2F);
            }
            g.setMode(NORMAL);
            g.digCooldown = g.isOwned() ? 500 + g.random.nextInt(500) : 1600 + g.random.nextInt(1600);
            this.spot = null;
        }
    }

    /** Owned: carries a find to the nearest chest or barrel (as a copper golem sorts), else to its owner, else sets it down. */
    private final class DeliverGoal extends Goal {
        private @Nullable BlockPos chest;
        private int time;

        DeliverGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            SoulGolem g = SoulGolem.this;
            return g.isOwned() && !g.getStash().isEmpty() && !g.isSlumped() && g.getMode() == NORMAL;
        }

        @Override
        public boolean canContinueToUse() {
            return !SoulGolem.this.getStash().isEmpty() && !SoulGolem.this.isSlumped() && this.time < 400;
        }

        @Override
        public void start() {
            this.time = 0;
            this.chest = SoulGolem.this.findChest(SoulGolem.this.blockPosition());
        }

        @Override
        public void tick() {
            SoulGolem g = SoulGolem.this;
            if (!(g.level() instanceof ServerLevel server)) {
                return;
            }
            this.time++;
            BlockPos ch = this.chest;
            Player p = g.ownerPlayer();
            if (ch != null) {
                Vec3 at = Vec3.atCenterOf(ch);
                g.getLookControl().setLookAt(at.x, at.y, at.z);
                if (g.distanceToSqr(at) > 2.2 * 2.2) {
                    if (this.time % 10 == 1) {
                        g.getNavigation().moveTo(at.x, ch.getY(), at.z, 1.1);
                    }
                    return;
                }
                Container box = HopperBlockEntity.getContainerAt(server, ch);
                ItemStack left = box == null ? g.getStash().copy() : HopperBlockEntity.addItem(null, box, g.getStash().copy(), Direction.UP);
                g.setStash(ItemStack.EMPTY);
                if (!left.isEmpty()) {
                    g.spawnAtLocation(server, left);
                }
                g.playSound(ModEchoer.GOLEM_DELIVER.get(), 1.0F, 1.0F);
                server.broadcastEntityEvent(g, EVENT_HAPPY);
            } else if (p != null && !p.isSpectator() && g.distanceToSqr(p) < 32.0 * 32.0) {
                g.getLookControl().setLookAt(p);
                if (g.distanceToSqr(p) > 2.2 * 2.2) {
                    if (this.time % 10 == 1) {
                        g.getNavigation().moveTo(p, 1.15);
                    }
                    return;
                }
                ItemStack give = g.getStash().copy();
                g.setStash(ItemStack.EMPTY);
                if (!p.getInventory().add(give)) {
                    g.spawnAtLocation(server, give);
                }
                g.playSound(ModEchoer.GOLEM_DELIVER.get(), 1.0F, 1.1F);
                server.broadcastEntityEvent(g, EVENT_HAPPY);
            } else if (this.time > 60) {
                // nobody to give it to: it sets the find down where it stands
                g.spawnAtLocation(server, g.getStash().copy());
                g.setStash(ItemStack.EMPTY);
            }
        }

        @Override
        public void stop() {
            SoulGolem g = SoulGolem.this;
            g.getNavigation().stop();
            if (!g.getStash().isEmpty() && g.level() instanceof ServerLevel server && this.time >= 400) {
                g.spawnAtLocation(server, g.getStash().copy());
                g.setStash(ItemStack.EMPTY);
            }
            this.chest = null;
        }
    }

    /** Wild: waddles over to a dropped item to pick it up. */
    private final class FetchItemGoal extends Goal {
        private @Nullable ItemEntity item;

        FetchItemGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (SoulGolem.this.isOwned() || !SoulGolem.this.getStash().isEmpty() || SoulGolem.this.isSlumped() || SoulGolem.this.random.nextInt(20) != 0) {
                return false;
            }
            List<ItemEntity> items = SoulGolem.this.level().getEntitiesOfClass(ItemEntity.class, SoulGolem.this.getBoundingBox().inflate(8.0, 3.0, 8.0),
                    e -> e.isAlive() && e.onGround() && !e.hasPickUpDelay());
            this.item = items.isEmpty() ? null : items.get(SoulGolem.this.random.nextInt(items.size()));
            return this.item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.item != null && this.item.isAlive() && SoulGolem.this.getStash().isEmpty() && !SoulGolem.this.isSlumped()
                    && !SoulGolem.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (this.item != null) {
                SoulGolem.this.getNavigation().moveTo(this.item, 1.1);
            }
        }

        @Override
        public void stop() {
            this.item = null;
        }
    }

    /** Wild: stops at suspicious blocks and peers at them, its antenna lamp brightening, as if it could see inside. */
    private final class PeekGoal extends Goal {
        private @Nullable BlockPos spot;
        private int time;

        PeekGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (SoulGolem.this.isOwned() || SoulGolem.this.isSlumped() || SoulGolem.this.random.nextInt(200) != 0) {
                return false;
            }
            BlockPos me = SoulGolem.this.blockPosition();
            for (BlockPos p : BlockPos.betweenClosed(me.offset(-6, -3, -6), me.offset(6, 2, 6))) {
                if (SoulGolem.this.level().getBlockState(p).getBlock() instanceof BrushableBlock) {
                    this.spot = p.immutable();
                    return true;
                }
            }
            return false;
        }

        @Override
        public void start() {
            this.time = 0;
            if (this.spot != null) {
                SoulGolem.this.getNavigation().moveTo(this.spot.getX() + 0.5, this.spot.getY() + 1.0, this.spot.getZ() + 0.5, 0.9);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.spot != null && this.time < 260 && !SoulGolem.this.isSlumped();
        }

        @Override
        public void tick() {
            BlockPos s = this.spot;
            if (s == null) {
                return;
            }
            this.time++;
            SoulGolem.this.getLookControl().setLookAt(s.getX() + 0.5, s.getY() + 0.5, s.getZ() + 0.5);
            if (SoulGolem.this.distanceToSqr(Vec3.atCenterOf(s)) < 2.4 * 2.4) {
                SoulGolem.this.getNavigation().stop();
                SoulGolem.this.setMode(PEEKING);
                if (this.time % 20 == 0 && SoulGolem.this.level() instanceof ServerLevel server) {
                    server.sendParticles(ParticleTypes.SOUL, s.getX() + 0.5, s.getY() + 1.0, s.getZ() + 0.5, 1, 0.2, 0.1, 0.2, 0.01);
                }
            }
        }

        @Override
        public void stop() {
            SoulGolem.this.setMode(NORMAL);
            this.spot = null;
        }
    }
}
