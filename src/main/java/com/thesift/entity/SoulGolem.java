package com.thesift.entity;

import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModTags;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Soul Golem: a small ancient construct of soulstone, round as a frog, lit from inside by soul
 * fire. It waddles and hops about on stubby legs.
 *
 * <ul>
 *   <li>Wild golems (around an Echoer's hearth) potter about looking for valuables: they pick up
 *   dropped items (use one to be shown its find) and stop to peer at suspicious blocks.</li>
 *   <li>A Soul Golem Core used on soul soil builds one that is yours. It follows you and every now
 *   and then sifts through the ground near you, sometimes finding gems
 *   ({@code thesift:gameplay/soul_golem_dig}).</li>
 *   <li>Owned golems run on soul energy, which slowly drains as they work; at zero they slump
 *   where they stand. Any music nearby recharges them a little and the Golem Hymn completely.</li>
 * </ul>
 */
public class SoulGolem extends PathfinderMob {
    public static final int MAX_ENERGY = 100;
    public static final int NORMAL = 0;
    public static final int DIGGING = 1;
    public static final int PEEKING = 2;
    private static final EntityDataAccessor<Integer> ENERGY = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MODE = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OWNED = SynchedEntityData.defineId(SoulGolem.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_HAPPY = 101;
    private static final int DIG_COST = 8;

    public final AnimationState happyAnimation = new AnimationState();
    /** Client-side smoothed slump (0 upright, 1 run down). */
    public float slumpO, slump;

    private java.util.@Nullable UUID owner;
    private ItemStack stash = ItemStack.EMPTY;
    private int drainTimer;
    private int digCooldown = 400;

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
        this.goalSelector.addGoal(2, new DigGoal());
        this.goalSelector.addGoal(3, new FollowOwner());
        this.goalSelector.addGoal(4, new FetchItemGoal());
        this.goalSelector.addGoal(5, new PeekGoal());
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
                if (this.onGround()) {
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
            this.slumpO = this.slump;
            this.slump = Mth.clamp(this.slump + (this.isSlumped() ? 0.06F : -0.1F), 0.0F, 1.0F);
            if (!this.isSlumped() && this.random.nextInt(14) == 0) {
                this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, this.getX(), this.getY() + 1.0, this.getZ(), 0, 0.01, 0);
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
        if (!this.isOwned() && this.stash.isEmpty() && !this.isSlumped() && this.tickCount % 5 == 0) {
            List<ItemEntity> items = this.level().getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(0.6),
                    e -> e.isAlive() && !e.hasPickUpDelay());
            if (!items.isEmpty()) {
                ItemEntity it = items.get(0);
                this.stash = it.getItem().copy();
                this.take(it, this.stash.getCount());
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
            if (!this.stash.isEmpty()) {
                // it shows you its treasure and hands it over
                ItemStack give = this.stash;
                this.stash = ItemStack.EMPTY;
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
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!this.stash.isEmpty()) {
            this.spawnAtLocation(level, this.stash);
            this.stash = ItemStack.EMPTY;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Owner", UUIDUtil.CODEC, this.owner);
        output.putInt("Energy", this.getEnergy());
        if (!this.stash.isEmpty()) {
            output.store("Stash", ItemStack.CODEC, this.stash);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.entityData.set(OWNED, this.owner != null);
        this.entityData.set(ENERGY, Mth.clamp(input.getIntOr("Energy", MAX_ENERGY), 0, MAX_ENERGY));
        this.stash = input.read("Stash", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isSlumped() ? null : ModEchoer.GOLEM_AMBIENT.get();
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

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x6C5A4B, 0x5FE9FF, KillBurst.NOTE, ModParticles.DRIFTING_SOUL.get());
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

    /** Owned: every so often it sifts through the ground near its owner and may turn up a gem. */
    private final class DigGoal extends Goal {
        private @Nullable BlockPos spot;
        private int time;

        DigGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Player p = SoulGolem.this.ownerPlayer();
            if (p == null || SoulGolem.this.digCooldown > 0 || SoulGolem.this.getEnergy() < DIG_COST + 2 || SoulGolem.this.distanceToSqr(p) > 12 * 12) {
                return false;
            }
            for (int i = 0; i < 12; i++) {
                BlockPos at = p.blockPosition().offset(SoulGolem.this.random.nextInt(9) - 4, -1, SoulGolem.this.random.nextInt(9) - 4);
                for (int dy = 2; dy >= -2; dy--) {
                    BlockPos g = at.above(dy);
                    BlockState st = SoulGolem.this.level().getBlockState(g);
                    if (diggable(st) && SoulGolem.this.level().getBlockState(g.above()).isAir()) {
                        this.spot = g;
                        return true;
                    }
                }
            }
            SoulGolem.this.digCooldown = 100;
            return false;
        }

        private static boolean diggable(BlockState st) {
            return st.is(BlockTags.DIRT) || st.is(BlockTags.SAND) || st.is(ModTags.Blocks.SIFT_PLANTABLE) || st.is(BlockTags.SOUL_FIRE_BASE_BLOCKS);
        }

        @Override
        public void start() {
            this.time = 0;
            BlockPos s = this.spot;
            if (s != null) {
                SoulGolem.this.getNavigation().moveTo(s.getX() + 0.5, s.getY() + 1.0, s.getZ() + 0.5, 1.0);
            }
        }

        @Override
        public boolean canContinueToUse() {
            return this.spot != null && this.time < 300 && !SoulGolem.this.isSlumped();
        }

        @Override
        public void tick() {
            BlockPos s = this.spot;
            if (s == null || !(SoulGolem.this.level() instanceof ServerLevel server)) {
                return;
            }
            this.time++;
            Vec3 c = Vec3.atCenterOf(s);
            if (SoulGolem.this.distanceToSqr(c.x, s.getY() + 1.0, c.z) > 1.6 * 1.6) {
                if (this.time % 20 == 0) {
                    SoulGolem.this.getNavigation().moveTo(c.x, s.getY() + 1.0, c.z, 1.0);
                }
                if (this.time > 200) {
                    this.spot = null;
                }
                return;
            }
            SoulGolem.this.getNavigation().stop();
            SoulGolem.this.getLookControl().setLookAt(c.x, s.getY() + 0.5, c.z);
            if (SoulGolem.this.getMode() != DIGGING) {
                SoulGolem.this.setMode(DIGGING);
                this.time = 200;
            }
            BlockState st = server.getBlockState(s);
            if (this.time % 6 == 0) {
                server.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, st), c.x, s.getY() + 1.05, c.z, 5, 0.25, 0.05, 0.25, 0.1);
                SoulGolem.this.playSound(ModEchoer.GOLEM_DIG.get(), 0.6F, 1.0F + SoulGolem.this.random.nextFloat() * 0.3F);
            }
            if (this.time >= 260) {
                SoulGolem.this.spendEnergy(DIG_COST);
                if (SoulGolem.this.random.nextFloat() < 0.4F) {
                    Player p = SoulGolem.this.ownerPlayer();
                    SoulGolem.this.dropFromGiftLootTable(server, ModEchoer.GOLEM_DIG_LOOT, (l, stack) -> {
                        ItemEntity e = new ItemEntity(server, c.x, s.getY() + 1.2, c.z, stack);
                        if (p != null) {
                            Vec3 v = p.position().subtract(e.position()).normalize().scale(0.25);
                            e.setDeltaMovement(v.x, 0.3, v.z);
                        }
                        e.setDefaultPickUpDelay();
                        server.addFreshEntity(e);
                    });
                    SoulGolem.this.playSound(ModEchoer.GOLEM_FIND.get(), 1.0F, 1.2F);
                    server.broadcastEntityEvent(SoulGolem.this, EVENT_HAPPY);
                    server.sendParticles(ParticleTypes.HAPPY_VILLAGER, c.x, s.getY() + 1.4, c.z, 6, 0.3, 0.2, 0.3, 0.0);
                }
                this.spot = null;
            }
        }

        @Override
        public void stop() {
            SoulGolem.this.setMode(NORMAL);
            SoulGolem.this.digCooldown = 600 + SoulGolem.this.random.nextInt(600);
            this.spot = null;
        }
    }

    /** Waddles over to a dropped item to pick it up. */
    private final class FetchItemGoal extends Goal {
        private @Nullable ItemEntity item;

        FetchItemGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            if (SoulGolem.this.isOwned() || !SoulGolem.this.stash.isEmpty() || SoulGolem.this.isSlumped() || SoulGolem.this.random.nextInt(20) != 0) {
                return false;
            }
            List<ItemEntity> items = SoulGolem.this.level().getEntitiesOfClass(ItemEntity.class, SoulGolem.this.getBoundingBox().inflate(8.0, 3.0, 8.0),
                    e -> e.isAlive() && e.onGround() && !e.hasPickUpDelay());
            this.item = items.isEmpty() ? null : items.get(SoulGolem.this.random.nextInt(items.size()));
            return this.item != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.item != null && this.item.isAlive() && SoulGolem.this.stash.isEmpty() && !SoulGolem.this.isSlumped()
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

    /** Wild: stops at suspicious blocks and peers at them, lamp brightening, as if it could see inside. */
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
