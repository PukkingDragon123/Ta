package com.thesift.entity.dunes;

import com.thesift.entity.KillBurst;
import com.thesift.entity.Resting;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModDunes;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Reservoir: a worm-cactus of the Rocky Dunes, striped pink and blue, spined, with a flower of a mouth on
 * top. It drinks Chrome from the air (faster in the rain, faster still beside a pool of it) and stores it: the
 * fuller it is the more it swells, pulses and glows (the renderer reads {@link #fill}). Draw a Chrome Bucket
 * from it with an empty bucket; pour one in to feed it. Its spines prick whatever pushes against it (dunes
 * creatures excepted). Hit when it is full enough and it may burst, spraying Chrome all round - everything in
 * the spray is dazed - and it is empty again. Grubs drink from it. It creeps about, very slowly.
 */
public class Reservoir extends PathfinderMob implements DunesNative, Resting {
    /** One bucket's worth of Chrome. */
    public static final int BUCKET = 30;
    private static final EntityDataAccessor<Integer> CHROME = SynchedEntityData.defineId(Reservoir.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> OPEN = SynchedEntityData.defineId(Reservoir.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_BURST = 101;
    private static final byte EVENT_GULP = 102;

    public final AnimationState burstAnimation = new AnimationState();
    public final AnimationState gulpAnimation = new AnimationState();
    /** Client: how open the flower is (eased). */
    public float bloom;
    public float bloomO;
    private int prickCooldown;

    public Reservoir(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 24.0)
                .add(Attributes.MOVEMENT_SPEED, 0.12)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.ARMOR, 2.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHROME, 0);
        builder.define(OPEN, true);
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        if (!this.isMonarch()) {
            this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.6, 360));
        }
    }

    /** The huge friendly one. */
    public boolean isMonarch() {
        return false;
    }

    public int capacity() {
        return 100;
    }

    public int chrome() {
        return this.entityData.get(CHROME);
    }

    public void setChrome(int c) {
        this.entityData.set(CHROME, Mth.clamp(c, 0, this.capacity()));
    }

    /** 0 empty - 1 full. */
    public float fill() {
        return this.chrome() / (float) this.capacity();
    }

    public boolean isOpen() {
        return this.entityData.get(OPEN);
    }

    /** A Grub drinks: up to {@code amount} Chrome, returned as what it got. */
    public int drain(int amount) {
        int got = Math.min(amount, this.chrome());
        this.setChrome(this.chrome() - got);
        return got;
    }

    /** It hardly ever moves: the never-frozen check lets it off whenever it is standing. */
    @Override
    public boolean isResting() {
        return this.getNavigation().isDone();
    }

    @Override
    public boolean isPushable() {
        return !this.isMonarch();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public float getWalkTargetValue(BlockPos pos, LevelReader level) {
        return ModDunes.dunesGround(level.getBlockState(pos.below())) ? 10.0F : 0.0F;
    }

    @Override
    public void push(Entity entity) {
        super.push(entity);
        this.prick(entity);
    }

    private void prick(Entity entity) {
        if (this.isMonarch() || this.prickCooldown > 0 || !(entity instanceof LivingEntity living) || entity instanceof DunesNative
                || !(this.level() instanceof ServerLevel server)) {
            return;
        }
        if (living.hurtServer(server, this.damageSources().cactus(), 1.0F)) {
            this.prickCooldown = 10;
        }
    }

    // ------------------------------------------------------------------ the server tick

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        if (this.prickCooldown > 0) {
            this.prickCooldown--;
        }
        int every = this.isMonarch() ? 20 : 40;
        if (this.tickCount % every == 0 && this.chrome() < this.capacity()) {
            int gain = 1;
            if (level.isRainingAt(this.blockPosition().above(2))) {
                gain += 2;
            }
            if (this.nearChrome(level)) {
                gain += 3;
            }
            this.setChrome(this.chrome() + gain);
        }
        // it opens its flower by day and when full; closes it at night and when hurt
        if (this.tickCount % 20 == 0) {
            boolean open = this.hurtTime == 0 && (!level.isDarkOutside() || this.fill() > 0.7F || this.isMonarch());
            if (open != this.isOpen()) {
                this.entityData.set(OPEN, open);
                if (open) {
                    this.playSound(ModDunes.RESERVOIR_BLOOM.get(), 0.6F, 1.0F);
                }
            }
        }
        // anything pressing against its spines is pricked (players don't always push)
        if (this.tickCount % 10 == 0 && !this.isMonarch()) {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(0.12), x -> x != this && x.isAlive())) {
                this.prick(e);
            }
        }
    }

    private boolean nearChrome(ServerLevel level) {
        BlockPos at = this.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-2, -1, -2), at.offset(2, 0, 2))) {
            if (level.getBlockState(p).is(ModBlocks.CHROME.get())) {
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------------ buckets

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(Items.BUCKET)) {
            if (this.chrome() < BUCKET) {
                if (!this.level().isClientSide()) {
                    player.sendOverlayMessage(Component.translatable("message.thesift.reservoir.empty"));
                }
                return InteractionResult.SUCCESS;
            }
            if (this.level() instanceof ServerLevel server) {
                this.setChrome(this.chrome() - BUCKET);
                swap(player, hand, stack, new ItemStack(ModItems.CHROME_BUCKET.get()));
                this.playSound(ModDunes.RESERVOIR_DRAIN.get(), 1.0F, 1.0F);
                server.broadcastEntityEvent(this, EVENT_GULP);
            }
            return InteractionResult.SUCCESS;
        }
        if (stack.is(ModItems.CHROME_BUCKET.get()) && this.chrome() + BUCKET <= this.capacity()) {
            if (this.level() instanceof ServerLevel server) {
                this.setChrome(this.chrome() + BUCKET);
                swap(player, hand, stack, new ItemStack(Items.BUCKET));
                this.playSound(ModDunes.RESERVOIR_GULP.get(), 1.0F, 1.0F);
                server.broadcastEntityEvent(this, EVENT_GULP);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private static void swap(Player player, InteractionHand hand, ItemStack used, ItemStack result) {
        if (player.isCreative()) {
            if (!player.getInventory().add(result)) {
                player.drop(result, false);
            }
            return;
        }
        used.shrink(1);
        if (used.isEmpty()) {
            player.setItemInHand(hand, result);
        } else if (!player.getInventory().add(result)) {
            player.drop(result, false);
        }
    }

    // ------------------------------------------------------------------ bursting

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.entityData.set(OPEN, false);
            if (!this.isMonarch() && this.chrome() >= BUCKET && this.random.nextFloat() < 0.25F + this.fill() * 0.6F) {
                this.burst(level);
            } else if (this.chrome() > 0) {
                this.playSound(ModDunes.RESERVOIR_SWELL.get(), 0.8F, 1.0F);
            }
        }
        return hurt;
    }

    /** It bursts: Chrome sprays all round, dazing everything it soaks, and the Reservoir is empty. */
    public void burst(ServerLevel level) {
        float k = this.fill();
        this.setChrome(0);
        this.playSound(ModDunes.RESERVOIR_BURST.get(), 1.4F, 0.9F + this.random.nextFloat() * 0.2F);
        level.broadcastEntityEvent(this, EVENT_BURST);
        double y = this.getY() + this.getBbHeight() * 0.6;
        level.sendParticles(ModParticles.CHROME_DROPLET.get(), this.getX(), y, this.getZ(), 60, 0.8, 0.6, 0.8, 0.3);
        level.sendParticles(ModChrome.CHROME_SPARK.get(), this.getX(), y, this.getZ(), 30, 1.2, 0.6, 1.2, 0.1);
        level.sendParticles(ModChrome.RAINBOW_MOTE.get(), this.getX(), y, this.getZ(), 20, 1.5, 0.8, 1.5, 0.05);
        double r = 3.0 + 2.0 * k;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(r), LivingEntity::isAlive)) {
            if (e == this || e instanceof Reservoir) {
                continue;
            }
            e.addEffect(new MobEffectInstance(ModChrome.RAINBOW_DAZE, 100 + (int) (100 * k), 0), this);
            Vec3 push = e.position().subtract(this.position()).normalize().scale(0.6 + 0.5 * k);
            e.push(push.x, 0.3, push.z);
        }
    }

    // ------------------------------------------------------------------ client

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_BURST -> this.burstAnimation.start(this.tickCount);
            case EVENT_GULP -> this.gulpAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.bloomO = this.bloom;
            this.bloom += ((this.isOpen() ? 1.0F : 0.0F) - this.bloom) * 0.08F;
            float k = this.fill();
            if (k > 0.3F && this.random.nextFloat() < k * 0.08F) {
                // Chrome glints rise out of its open mouth
                this.level().addParticle(ModChrome.RAINBOW_MOTE.get(), this.getX() + (this.random.nextDouble() - 0.5) * 0.3 * this.getBbWidth(),
                        this.getY() + this.getBbHeight(), this.getZ() + (this.random.nextDouble() - 0.5) * 0.3 * this.getBbWidth(), 0.0, 0.03, 0.0);
            }
        }
    }

    /** The Codex page: it fills and empties, opening its flower. */
    public void codexPose(int t) {
        this.setChrome((int) ((Mth.sin(t * 0.05F) * 0.5F + 0.5F) * this.capacity()));
        if (t % 160 == 120) {
            this.burstAnimation.start(this.tickCount);
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isMonarch() ? ModDunes.RESERVOIR_HUM.get() : ModDunes.RESERVOIR_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModDunes.RESERVOIR_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModDunes.RESERVOIR_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xC95686, 0x5F8AD4, KillBurst.DROP, ModParticles.CHROME_DROPLET.get());
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Chrome", this.chrome());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setChrome(input.getIntOr("Chrome", 0));
    }
}
