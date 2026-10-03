package com.thesift.entity.cave;

import com.thesift.entity.KillBurst;
import com.thesift.entity.Spring;
import com.thesift.music.MusicListener;
import com.thesift.registry.ModCaveCreatures;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Sculklings: small, blind sculk goblins with enormous bat ears (and no wings) that skitter through
 * the Sift's dark caves in packs of three to five. They hear everything - except a player who
 * sneaks. Hear you, and they screech, swarm and scratch, giggling; now and then one snatches
 * something shiny out of your pockets (gold, gems, ingots) and runs off with it. Kill the thief to
 * get it back. Music is far too loud for those ears: play a note near them and they clap their
 * hands over their ears and flee.
 */
public class Sculkling extends Monster implements MusicListener {
    public static final double HEAR_RANGE = 14.0;
    public static final double MUSIC_RANGE = 16.0;
    private static final EntityDataAccessor<ItemStack> LOOT = SynchedEntityData.defineId(Sculkling.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Boolean> SCARED = SynchedEntityData.defineId(Sculkling.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_SNATCH = 94;
    private static final byte EVENT_COVER = 95;
    private static final byte EVENT_TWITCH = 96;
    private static final byte EVENT_GIGGLE = 97;
    private static final byte EVENT_SCREECH = 98;

    public final AnimationState snatchAnimation = new AnimationState();
    public final AnimationState coverAnimation = new AnimationState();
    public final AnimationState giggleAnimation = new AnimationState();
    public final AnimationState screechAnimation = new AnimationState();
    /** The ears twitch and flop on springs (client side). */
    public final Spring leftEar = new Spring(0.22F, 0.16F);
    public final Spring rightEar = new Spring(0.24F, 0.16F);

    private int scaredTicks;
    private @Nullable Vec3 scaredFrom;
    private int listenTimer;
    private int snatchCooldown = 100;
    private int fleeTicks;
    private @Nullable LivingEntity robbed;

    public Sculkling(EntityType<? extends Monster> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.32)
                .add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(LOOT, ItemStack.EMPTY);
        builder.define(SCARED, false);
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
        this.goalSelector.addGoal(1, new FleeMusicGoal());
        this.goalSelector.addGoal(2, new RunWithLootGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.3, true) {
            @Override
            public boolean canUse() {
                return Sculkling.this.isBold() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Sculkling.this.isBold() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(5, new StayWithPackGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers());
    }

    public ItemStack getLoot() {
        return this.entityData.get(LOOT);
    }

    private void setLoot(ItemStack stack) {
        this.entityData.set(LOOT, stack);
    }

    public boolean isScared() {
        return this.entityData.get(SCARED);
    }

    /** Not scared and not running off with something: ready to swarm. */
    private boolean isBold() {
        return !this.isScared() && this.getLoot().isEmpty();
    }

    // ------------------------------------------------------------------ ears

    /** Music: the ears cannot bear it. Hands over the ears, then away from the sound. */
    public void scare(Vec3 from, float strength) {
        if (!(this.level() instanceof ServerLevel server)) {
            return;
        }
        boolean already = this.isScared();
        this.scaredTicks = Math.max(this.scaredTicks, (int) (140 + 60 * strength));
        this.scaredFrom = from;
        this.entityData.set(SCARED, true);
        this.setTarget(null);
        if (!already) {
            server.broadcastEntityEvent(this, EVENT_COVER);
            this.playSound(ModCaveCreatures.SCULKLING_SCARED.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.3F);
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        this.scare(Vec3.atCenterOf(source), strength);
    }

    /** Footsteps, jumps and swings; a sneaking player is silent. */
    private static float loudness(Player p) {
        if (p.isSteppingCarefully() || p.isPassenger()) {
            return 0.0F;
        }
        Vec3 v = p.getKnownMovement();
        float loud = (float) Math.min(1.0, v.horizontalDistance() * (p.isSprinting() ? 6.0 : 4.5));
        if (!p.onGround() && v.y < -0.35 || p.isSwinging() || p.hurtTime > 6) {
            loud = Math.max(loud, 0.8F);
        }
        return loud;
    }

    private void listen(ServerLevel level) {
        if (this.isScared() || --this.listenTimer > 0) {
            return;
        }
        this.listenTimer = 6;
        for (Player p : level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(HEAR_RANGE),
                pl -> pl.isAlive() && !pl.isCreative() && !pl.isSpectator())) {
            float loud = loudness(p);
            double range = HEAR_RANGE * loud;
            if (loud > 0.05F && this.distanceToSqr(p) < range * range) {
                this.heard(level, p, true);
                return;
            }
        }
    }

    /** It heard a player: a thief runs, anyone else screeches and calls the pack in. */
    private void heard(ServerLevel level, Player player, boolean alertPack) {
        if (!this.getLoot().isEmpty()) {
            boolean news = this.fleeTicks <= 0;
            this.robbed = player;
            this.fleeTicks = 140;
            if (news) {
                level.broadcastEntityEvent(this, EVENT_TWITCH);
            }
            return;
        }
        if (this.getTarget() == player) {
            return;
        }
        this.setTarget(player);
        level.broadcastEntityEvent(this, EVENT_TWITCH);
        this.playSound(ModCaveCreatures.SCULKLING_TWITCH.get(), 0.6F, 1.0F + this.random.nextFloat() * 0.3F);
        if (alertPack) {
            level.broadcastEntityEvent(this, EVENT_SCREECH);
            this.playSound(ModCaveCreatures.SCULKLING_SCREECH.get(), 1.2F, 0.95F + this.random.nextFloat() * 0.2F);
            for (Sculkling mate : level.getEntitiesOfClass(Sculkling.class, this.getBoundingBox().inflate(12.0),
                    s -> s != this && s.isAlive() && s.isBold() && s.getTarget() == null)) {
                mate.heard(level, player, false);
            }
        }
    }

    // ------------------------------------------------------------------ snatching

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (target instanceof Player player && this.snatchCooldown <= 0 && this.getLoot().isEmpty() && this.random.nextFloat() < 0.3F
                && this.snatch(level, player)) {
            return true;
        }
        return super.doHurtTarget(level, target);
    }

    /** Grabs a few of something shiny from the player's pockets and runs. */
    private boolean snatch(ServerLevel level, Player player) {
        Inventory inv = player.getInventory();
        int slot = -1;
        int seen = 0;
        for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
            if (inv.getItem(i).is(ModCaveCreatures.SHINIES) && this.random.nextInt(++seen) == 0) {
                slot = i; // a fair pick among every shiny stack
            }
        }
        if (slot < 0) {
            this.snatchCooldown = 60;
            return false;
        }
        ItemStack taken = inv.removeItem(slot, Math.min(inv.getItem(slot).getCount(), 1 + this.random.nextInt(3)));
        if (taken.isEmpty()) {
            return false;
        }
        this.setLoot(taken);
        this.robbed = player;
        this.fleeTicks = 200;
        this.snatchCooldown = 400;
        this.setTarget(null);
        level.broadcastEntityEvent(this, EVENT_SNATCH);
        this.playSound(ModCaveCreatures.SCULKLING_SNATCH.get(), 1.2F, 1.0F + this.random.nextFloat() * 0.2F);
        player.sendOverlayMessage(Component.translatable("message.thesift.sculkling.snatched", taken.getHoverName()));
        return true;
    }

    @Override
    public void die(DamageSource source) {
        if (this.level() instanceof ServerLevel server && !this.getLoot().isEmpty()) {
            this.spawnAtLocation(server, this.getLoot());
            this.setLoot(ItemStack.EMPTY);
        }
        super.die(source);
    }

    @Override
    public boolean requiresCustomPersistence() {
        return !this.getLoot().isEmpty() || super.requiresCustomPersistence();
    }

    // ------------------------------------------------------------------ ticking

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.snatchCooldown > 0) {
            this.snatchCooldown--;
        }
        if (this.fleeTicks > 0) {
            this.fleeTicks--;
        }
        if (this.scaredTicks > 0 && --this.scaredTicks == 0) {
            this.entityData.set(SCARED, false);
            this.scaredFrom = null;
        }
        this.listen(level);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.leftEar.tick();
            this.rightEar.tick();
            // the ears never quite keep still
            if (this.random.nextInt(70) == 0) {
                this.leftEar.kick((this.random.nextFloat() - 0.3F) * 0.5F);
            }
            if (this.random.nextInt(70) == 0) {
                this.rightEar.kick((this.random.nextFloat() - 0.3F) * 0.5F);
            }
            if (this.random.nextInt(20) == 0) {
                // the ear veins and the soul in its chest give off a faint sculk glow
                this.level().addParticle(ParticleTypes.SCULK_CHARGE_POP, this.getRandomX(0.6), this.getY() + 0.7 + this.random.nextDouble() * 0.6,
                        this.getRandomZ(0.6), 0.0, 0.01, 0.0);
            }
        }
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_SNATCH -> {
                this.snatchAnimation.start(this.tickCount);
                this.leftEar.kick(-0.6F);
                this.rightEar.kick(-0.6F);
            }
            case EVENT_COVER -> this.coverAnimation.start(this.tickCount);
            case EVENT_TWITCH -> {
                this.leftEar.kick(-0.9F + this.random.nextFloat() * 0.3F);
                this.rightEar.kick(-0.8F + this.random.nextFloat() * 0.3F);
            }
            case EVENT_GIGGLE -> this.giggleAnimation.start(this.tickCount);
            case EVENT_SCREECH -> {
                this.screechAnimation.start(this.tickCount);
                this.leftEar.kick(-1.2F);
                this.rightEar.kick(-1.2F);
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void playAmbientSound() {
        super.playAmbientSound();
        if (this.level() instanceof ServerLevel server) {
            server.broadcastEntityEvent(this, EVENT_GIGGLE);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!this.getLoot().isEmpty()) {
            output.store("Loot", ItemStack.CODEC, this.getLoot());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setLoot(input.read("Loot", ItemStack.CODEC).orElse(ItemStack.EMPTY));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveCreatures.SCULKLING_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 160;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveCreatures.SCULKLING_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveCreatures.SCULKLING_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModCaveCreatures.SCULKLING_STEP.get(), 0.25F, 1.2F + this.random.nextFloat() * 0.3F);
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 5;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x123B45, 0x3FF5E6, KillBurst.STAR, ParticleTypes.SCULK_SOUL);
    }

    // ------------------------------------------------------------------ goals

    /** Hands clapped over its ears for a moment, then away from the music as fast as it can go. */
    private class FleeMusicGoal extends Goal {
        private int cover;

        FleeMusicGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Sculkling.this.isScared() && Sculkling.this.scaredFrom != null;
        }

        @Override
        public void start() {
            this.cover = 14;
            Sculkling.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Sculkling s = Sculkling.this;
            if (this.cover > 0) {
                this.cover--;
                return;
            }
            Vec3 from = s.scaredFrom;
            if (from != null && (s.getNavigation().isDone() || s.random.nextInt(30) == 0)) {
                Vec3 to = DefaultRandomPos.getPosAway(s, 14, 6, from);
                if (to != null) {
                    s.getNavigation().moveTo(to.x, to.y, to.z, 1.5);
                }
            }
        }
    }

    /** A thief with its prize runs from whoever it took it from, giggling. */
    private class RunWithLootGoal extends Goal {
        RunWithLootGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Sculkling s = Sculkling.this;
            return !s.getLoot().isEmpty() && s.fleeTicks > 0 && s.robbed != null && s.robbed.isAlive() && s.distanceToSqr(s.robbed) < 30 * 30;
        }

        @Override
        public void tick() {
            Sculkling s = Sculkling.this;
            LivingEntity from = s.robbed;
            if (from != null && (s.getNavigation().isDone() || s.random.nextInt(20) == 0)) {
                Vec3 to = DefaultRandomPos.getPosAway(s, 16, 7, from.position());
                if (to != null) {
                    s.getNavigation().moveTo(to.x, to.y, to.z, 1.45);
                }
            }
            if (s.random.nextInt(50) == 0 && s.level() instanceof ServerLevel server) {
                server.broadcastEntityEvent(s, EVENT_GIGGLE);
                s.playSound(ModCaveCreatures.SCULKLING_AMBIENT.get(), 1.0F, 1.2F + s.random.nextFloat() * 0.2F);
            }
        }
    }

    /** Sculklings keep together: a straggler scurries back to the nearest of its pack. */
    private class StayWithPackGoal extends Goal {
        private @Nullable Sculkling mate;
        private int timer;

        StayWithPackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            Sculkling s = Sculkling.this;
            if (s.getTarget() != null || s.isScared() || --this.timer > 0) {
                return false;
            }
            this.timer = 40;
            this.mate = null;
            double best = 6.0 * 6.0;
            for (Sculkling other : s.level().getEntitiesOfClass(Sculkling.class, s.getBoundingBox().inflate(16.0), o -> o != s && o.isAlive())) {
                double d = s.distanceToSqr(other);
                if (d > best && (this.mate == null || d < s.distanceToSqr(this.mate))) {
                    this.mate = other;
                }
            }
            return this.mate != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.mate != null && this.mate.isAlive() && Sculkling.this.distanceToSqr(this.mate) > 3.0 * 3.0
                    && !Sculkling.this.getNavigation().isDone();
        }

        @Override
        public void start() {
            if (this.mate != null) {
                Sculkling.this.getNavigation().moveTo(this.mate, 1.1);
            }
        }
    }
}
