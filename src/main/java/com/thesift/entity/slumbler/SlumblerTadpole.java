package com.thesift.entity.slumbler;

import com.thesift.entity.SiftFish;
import com.thesift.music.band.BandPlayer;
import com.thesift.registry.ModChrome;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR2: a Slumbler tadpole, hatched from a clutch of {@link SlumblerEggs} - a little stingray of the
 * Chrome: a flat diamond body in its parents' rainbow scales, two wide wing-fins that ripple as it
 * glides, eyes on top and a long whip of a tail.
 *
 * <ul>
 *   <li>Aggressive: it hunts fish (and eats them) and goes for anyone swimming in its pool.</li>
 *   <li>It fits in a bucket like any fish - a Chrome Bucket or a water bucket.</li>
 *   <li>Tame it with lots of food and drum music: feed it fish ({@value #FOOD_TO_TAME} of them) and it
 *   stops biting you; then play it a drum and it is yours. A tamed tadpole follows you through
 *   the water, never bites you, and joins your band on crash cymbals.</li>
 * </ul>
 */
public class SlumblerTadpole extends SiftFish implements BandPlayer {
    private static final byte EVENT_BITE = 100;
    private static final byte EVENT_CRASH = 101;
    private static final byte EVENT_TAMED = 102;
    /** Fish it must be fed (by one player) before drum music can win it over. */
    public static final int FOOD_TO_TAME = 8;
    /** Drum notes it must hear from its feeder (no more than five seconds apart) to be tamed. */
    private static final int DRUM_NOTES_TO_TAME = 6;
    private static final double HUNT_RANGE = 10.0;

    public final AnimationState biteAnimation = new AnimationState();
    public final AnimationState crashAnimation = new AnimationState();
    private @Nullable UUID owner;
    private @Nullable UUID feeder;
    private int fed;
    private int drumNotes;
    private int drumWindow;
    /** While above 0 it leaves its feeder alone. */
    private int calmTicks;
    private @Nullable LivingEntity prey;
    private int preyCheck;
    private int biteCooldown;
    /** Whether the bucket scooping it up right now is a Chrome Bucket (else a water bucket). */
    private boolean chromeScoop = true;

    public SlumblerTadpole(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.7).add(Attributes.ATTACK_DAMAGE, 2.0)
                .add(Attributes.FOLLOW_RANGE, HUNT_RANGE);
    }

    @Override
    protected double cruiseSpeed() {
        return 0.08;
    }

    @Override
    protected double speedFactor() {
        return this.prey != null ? 2.8 : 1.0;
    }

    public boolean isTame() {
        return this.owner != null;
    }

    public @Nullable UUID getOwnerId() {
        return this.owner;
    }

    /** True once it has been fed enough to be tamed with drum music. */
    public boolean isReadyToTame() {
        return !this.isTame() && this.fed >= FOOD_TO_TAME;
    }

    // ------------------------------------------------------------------ hunting

    private boolean isPrey(LivingEntity e) {
        if (!e.isAlive() || e == this || !e.isInFluidType() || e instanceof SlumblerTadpole) {
            return false;
        }
        if (e instanceof Player p) {
            return !this.isTame() && !p.isCreative() && !p.isSpectator() && this.level().getDifficulty() != Difficulty.PEACEFUL
                    && !(this.calmTicks > 0 && p.getUUID().equals(this.feeder));
        }
        return e instanceof SiftFish || e instanceof net.minecraft.world.entity.animal.fish.AbstractFish;
    }

    private void huntCheck() {
        if (this.prey != null && (!this.isPrey(this.prey) || this.distanceToSqr(this.prey) > 16.0 * 16.0)) {
            this.prey = null;
        }
        if (this.prey == null) {
            double best = HUNT_RANGE * HUNT_RANGE;
            for (LivingEntity e : this.level().getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(HUNT_RANGE), this::isPrey)) {
                double d = this.distanceToSqr(e);
                if (d < best) {
                    best = d;
                    this.prey = e;
                }
            }
        }
        this.setAggressive(this.prey != null);
        this.setTarget(this.prey);
    }

    @Override
    protected @Nullable Vec3 wantedPosition() {
        if (--this.preyCheck <= 0) {
            this.preyCheck = 10;
            this.huntCheck();
        }
        if (this.prey != null) {
            if (this.biteCooldown > 0) {
                // a glide past and a turn, then in again
                double a = this.tickCount * 0.15 + this.getId();
                return this.prey.position().add(Math.cos(a) * 2.2, 0.5, Math.sin(a) * 2.2);
            }
            return this.prey.getBoundingBox().getCenter();
        }
        // a tamed tadpole keeps close to its owner while they are in the water
        if (this.owner != null && this.level().getPlayerByUUID(this.owner) instanceof Player p && p.isInFluidType() && this.distanceToSqr(p) < 24.0 * 24.0
                && this.distanceToSqr(p) > 3.0 * 3.0) {
            double a = this.getId() * 2.4;
            return p.position().add(Math.cos(a) * 1.8, 0.4, Math.sin(a) * 1.8);
        }
        return null;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.biteCooldown > 0) {
                this.biteCooldown--;
            }
            if (this.calmTicks > 0) {
                this.calmTicks--;
            }
            if (this.drumWindow > 0 && --this.drumWindow == 0) {
                this.drumNotes = 0;
            }
            LivingEntity p = this.prey;
            double reach = 0.7 + (p != null ? p.getBbWidth() * 0.5 : 0.0);
            if (p != null && this.biteCooldown <= 0 && this.inLiquid() && this.distanceToSqr(p.getBoundingBox().getCenter()) < reach * reach) {
                this.biteCooldown = 30 + this.random.nextInt(20);
                this.doHurtTarget(server, p);
                if (!p.isAlive() && !(p instanceof Player)) {
                    this.eat(server, p.position());
                }
            }
        }
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        level.broadcastEntityEvent(this, EVENT_BITE);
        this.playSound(ModSounds.SLUMBLER_TADPOLE_BITE.get(), 1.0F, 1.1F + this.random.nextFloat() * 0.3F);
        boolean hit = super.doHurtTarget(level, target);
        this.setDeltaMovement(this.getDeltaMovement().add(this.getLookAngle().scale(-0.25)).add(0.0, 0.04, 0.0));
        return hit;
    }

    /** It gulps down the fish it caught. */
    private void eat(ServerLevel level, Vec3 at) {
        this.heal(3.0F);
        this.prey = null;
        level.sendParticles(ModParticles.CHROME_BUBBLE.get(), at.x, at.y + 0.2, at.z, 8, 0.2, 0.2, 0.2, 0.03);
        this.playSound(ModSounds.SLUMBLER_EAT.get(), 0.6F, 1.6F);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && source.getEntity() instanceof LivingEntity attacker && this.isPrey(attacker)) {
            this.prey = attacker;
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_BITE -> this.biteAnimation.start(this.tickCount);
            case EVENT_CRASH -> this.crashAnimation.start(this.tickCount);
            case EVENT_TAMED -> {
                for (int i = 0; i < 7; i++) {
                    this.level().addParticle(ParticleTypes.HEART, this.getRandomX(0.6), this.getY() + 0.5, this.getRandomZ(0.6), 0.0, 0.05, 0.0);
                }
            }
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ taming: lots of food, then drum music

    /** Fish feeds it (and wins it over); anything else is a bucket. */
    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.is(ItemTags.FISHES)) {
            if (this.level() instanceof ServerLevel server) {
                stack.consume(1, player);
                this.feed(server, player);
            }
            return InteractionResult.SUCCESS;
        }
        this.chromeScoop = !stack.is(Items.WATER_BUCKET);
        return super.mobInteract(player, hand);
    }

    private void feed(ServerLevel level, Player player) {
        if (!player.getUUID().equals(this.feeder)) {
            this.feeder = player.getUUID();
            this.fed = 0;
        }
        this.fed++;
        this.calmTicks = 2400;
        if (this.prey == player) {
            this.prey = null;
            this.setTarget(null);
        }
        this.heal(2.0F);
        this.playSound(ModSounds.SLUMBLER_EAT.get(), 0.7F, 1.5F + this.random.nextFloat() * 0.2F);
        level.sendParticles(ModParticles.CHROME_BUBBLE.get(), this.getX(), this.getY() + 0.2, this.getZ(), 6, 0.2, 0.1, 0.2, 0.02);
        if (this.isReadyToTame()) {
            // full: now it wants to hear a drum
            level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + 0.5, this.getZ(), 1, 0.1, 0.1, 0.1, 0.0);
        }
    }

    /**
     * A note was played near it (SongEvents, see ModSlumbler): drum notes from the player who fed it
     * enough, one after another, tame it.
     */
    public void hearNote(ServerLevel level, @Nullable Player player, boolean drum) {
        if (!drum || player == null || !this.isReadyToTame() || !player.getUUID().equals(this.feeder)) {
            return;
        }
        this.drumNotes++;
        this.drumWindow = 100;
        level.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 0.5, this.getZ(), 0, this.random.nextDouble(), 0.0, 0.0, 1.0);
        if (this.drumNotes >= DRUM_NOTES_TO_TAME) {
            this.tame(level, player);
        }
    }

    private void tame(ServerLevel level, Player player) {
        this.owner = player.getUUID();
        this.prey = null;
        this.setTarget(null);
        this.setAggressive(false);
        this.drumNotes = 0;
        this.setPersistenceRequired();
        level.broadcastEntityEvent(this, EVENT_TAMED);
        level.broadcastEntityEvent(this, EVENT_CRASH);
        this.playSound(ModSounds.SLUMBLER_TADPOLE_CRASH.get(), 1.0F, 1.0F);
        level.sendParticles(ModChrome.CHROME_SPARK.get(), this.getX(), this.getY() + 0.4, this.getZ(), 0, this.random.nextDouble(), 0.06, 0.0, 1.0);
    }

    /** Every note it plays in a band: a clash of its wing-fins (the crash itself is its band voice). */
    @Override
    public void playedBandNote(int pitch, float loudness) {
        this.level().broadcastEntityEvent(this, EVENT_CRASH);
    }

    // ------------------------------------------------------------------ buckets: Chrome or water, keeping who tamed it

    @Override
    public boolean canBePickedUpWithBucket(ItemStack stack) {
        return stack.is(ModItems.CHROME_BUCKET.get()) || stack.is(Items.WATER_BUCKET);
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(this.chromeScoop ? ModItems.CHROME_SLUMBLER_TADPOLE_BUCKET.get() : ModItems.SLUMBLER_TADPOLE_BUCKET.get());
    }

    @Override
    public void saveToBucketTag(ItemStack bucket) {
        super.saveToBucketTag(bucket);
        UUID o = this.owner;
        int food = this.fed;
        CustomData.update(DataComponents.BUCKET_ENTITY_DATA, bucket, tag -> {
            tag.putInt("Fed", food);
            if (o != null) {
                tag.putString("Owner", o.toString());
            }
        });
    }

    @Override
    public void loadFromBucketTag(CompoundTag tag) {
        super.loadFromBucketTag(tag);
        this.fed = tag.getInt("Fed").orElse(0);
        this.owner = tag.getString("Owner").map(SlumblerTadpole::uuid).orElse(null);
    }

    private static @Nullable UUID uuid(String s) {
        try {
            return UUID.fromString(s);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ sounds and saving

    @Override
    protected SoundEvent getFlopSound() {
        return ModSounds.SLUMBLER_TADPOLE_FLOP.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.SLUMBLER_TADPOLE_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.SLUMBLER_TADPOLE_DEATH.get();
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return super.removeWhenFarAway(distSqr) && !this.isTame();
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Fed", this.fed);
        output.putInt("CalmTicks", this.calmTicks);
        if (this.owner != null) {
            output.store("Owner", UUIDUtil.CODEC, this.owner);
        }
        if (this.feeder != null) {
            output.store("Feeder", UUIDUtil.CODEC, this.feeder);
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.fed = input.getIntOr("Fed", 0);
        this.calmTicks = input.getIntOr("CalmTicks", 0);
        this.owner = input.read("Owner", UUIDUtil.CODEC).orElse(null);
        this.feeder = input.read("Feeder", UUIDUtil.CODEC).orElse(null);
    }

    /** Client: a faint rainbow shimmer off its wing-fins as it glides. */
    @Override
    protected void clientEffects() {
        if (this.random.nextInt(14) == 0) {
            float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
            double side = (this.random.nextBoolean() ? 1.0 : -1.0) * 0.35;
            this.level().addParticle(ModChrome.CHROME_SPARK.get(), this.getX() + Mth.cos(yaw) * side, this.getY() + 0.1, this.getZ() + Mth.sin(yaw) * side,
                    this.random.nextDouble(), 0.01, 0.0);
        }
    }
}
