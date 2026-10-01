package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Bulb: a small, bouncy, jelly bunny of the Sift plains. Bulbs hop everywhere, wobble their long
 * ears, love Pitcher Bulbs (their breeding food) and every so often squeeze out a Glowing Slime
 * Ball. Play music near them and they dance.
 */
public class Bulb extends Animal implements HopMoveControl.Hopper, MusicListener {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DANCE = SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.INT);
    public static final int VARIANTS = 4;

    private int slimeTime;

    // ---- client animation state (squash & stretch, ears)
    public final Spring squash = new Spring(0.28F, 0.22F);
    public final Spring earLeft = new Spring(0.18F, 0.16F);
    public final Spring earRight = new Spring(0.16F, 0.14F);
    public final Spring earPerk = new Spring(0.08F, 0.25F);
    private boolean wasOnGround = true;
    private double lastVelY;

    public Bulb(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new HopMoveControl<>(this);
        this.slimeTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.32).add(Attributes.JUMP_STRENGTH, 0.42);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, s -> s.is(ModTags.Items.BULB_FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(DANCE, 0);
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, v);
    }

    public boolean isDancing() {
        return this.entityData.get(DANCE) > 0;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.Items.BULB_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Bulb baby = ModEntities.BULB.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null && partner instanceof Bulb other) {
            int v = this.random.nextInt(12) == 0 ? this.random.nextInt(VARIANTS) : (this.random.nextBoolean() ? this.getVariant() : other.getVariant());
            baby.setVariant(v);
        }
        return baby;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        int roll = this.random.nextInt(100);
        this.setVariant(roll < 45 ? 0 : roll < 80 ? 1 : roll < 97 ? 2 : 3);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ------------------------------------------------------------------ hopping

    @Override
    public int hopDelay() {
        return this.isDancing() ? 2 : 4 + this.random.nextInt(6);
    }

    @Override
    public void onHop() {
        this.playSound(ModSounds.BULB_HOP.get(), 0.5F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + (this.isBaby() ? 1.5F : 1.1F));
    }

    @Override
    protected float getJumpPower() {
        return super.getJumpPower() * (this.isDancing() ? 0.8F : 1.0F);
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (!this.level().isClientSide()) {
            this.entityData.set(DANCE, 60 + this.random.nextInt(40));
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            int dance = this.entityData.get(DANCE);
            if (dance > 0) {
                this.entityData.set(DANCE, dance - 1);
                if (this.onGround() && this.tickCount % 8 == 0) {
                    this.getJumpControl().jump();
                    this.setYRot(this.getYRot() + 45.0F);
                    this.yBodyRot = this.getYRot();
                    if (this.random.nextInt(3) == 0) {
                        server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(),
                                0, this.random.nextDouble(), 0, 0, 1);
                    }
                }
            }
            if (this.isAlive() && !this.isBaby() && --this.slimeTime <= 0) {
                this.spawnAtLocation(server, new ItemStack(ModItems.GLOWING_SLIME_BALL.get()));
                this.playSound(ModSounds.BULB_LAY.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                server.sendParticles(ModParticles.GLOW_SPLAT.get(), this.getX(), this.getY() + 0.2, this.getZ(), 5, 0.2, 0.1, 0.2, 0.05);
                this.slimeTime = this.random.nextInt(6000) + 6000;
            }
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.tickAnimation();
        }
    }

    private void tickAnimation() {
        double vy = this.getDeltaMovement().y;
        boolean onGround = this.onGround();
        if (onGround && !this.wasOnGround) {
            // Landing: squash, flatten ears, splash a little glow.
            float impact = (float) Mth.clamp(-this.lastVelY * 1.6, 0.15, 0.55);
            this.squash.kick(-impact);
            this.earLeft.kick(impact * 1.4F);
            this.earRight.kick(impact * 1.2F);
            this.slimeTrail(1 + this.random.nextInt(2));
        } else if (!onGround && this.wasOnGround && vy > 0) {
            // Take-off: stretch tall and let the ears trail.
            this.squash.kick(0.28F);
            this.earLeft.kick(-0.4F);
            this.earRight.kick(-0.35F);
        }
        this.wasOnGround = onGround;
        this.lastVelY = vy;
        // Ears perk up when a player is close by, droop a little otherwise.
        Player near = this.level().getNearestPlayer(this, 6.0);
        this.earPerk.setTarget(near != null ? 1.0F : 0.0F);
        if (onGround && this.getDeltaMovement().horizontalDistanceSqr() > 0.0004 && this.random.nextInt(6) == 0) {
            this.slimeTrail(1);
        }
        this.squash.tick();
        this.earLeft.tick();
        this.earRight.tick();
        this.earPerk.tick();
        if (this.getVariant() == 3 && this.random.nextInt(10) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.01, 0);
        }
    }

    /** Jelly tint of each variant, for the slime it leaves behind. */
    private static final float[][] SLIME_TINT = {{0.42F, 0.80F, 0.90F}, {0.30F, 0.84F, 0.95F}, {0.92F, 0.62F, 0.84F}, {0.34F, 0.50F, 0.86F}};

    private void slimeTrail(int count) {
        float[] c = SLIME_TINT[Mth.clamp(this.getVariant(), 0, SLIME_TINT.length - 1)];
        for (int i = 0; i < count; i++) {
            this.level().addParticle(ModParticles.SLIME_TRAIL.get(), this.getX() + (this.random.nextDouble() - 0.5) * 0.5, this.getY() + 0.02,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.5, c[0], c[1], c[2]);
        }
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean food = this.isFood(stack);
        InteractionResult result = super.mobInteract(player, hand);
        if (food && result.consumesAction() && this.level() instanceof ServerLevel server) {
            this.slimeTime = Math.max(20, this.slimeTime / 2);
            server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            this.playSound(ModSounds.BULB_HAPPY.get(), 0.8F, 1.2F);
            this.entityData.set(DANCE, 20);
        }
        return result;
    }

    // ------------------------------------------------------------------ sounds & save

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.BULB_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.BULB_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.BULB_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.BULB_SQUISH.get(), 0.15F, 1.2F);
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? 1.5F + this.random.nextFloat() * 0.2F : 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.15F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
        output.putInt("SlimeTime", this.slimeTime);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.getIntOr("Variant", 0));
        this.slimeTime = input.getIntOr("SlimeTime", 6000);
    }

    private static final int[][] BURST = {{0x78A5E3, 0x63C6DF}, {0x3FD0EF, 0x9CF0FF}, {0xA58FE6, 0xE59AD0}, {0x3B4AA0, 0xFFF1A8}};

    /** Killed, it bursts into jelly: droplets and stars in its own colours, and a few splats on the ground. */
    @Override
    public void makePoofParticles() {
        int[] c = BURST[Math.floorMod(this.getVariant(), BURST.length)];
        KillBurst.pop(this, c[0], c[1], KillBurst.DROP, null);
        for (int i = 0; i < 5; i++) {
            this.level().addParticle(ModParticles.SLIME_TRAIL.get(), this.getRandomX(0.9), this.getY() + 0.02, this.getRandomZ(0.9),
                    ((c[1] >> 16) & 255) / 255.0, ((c[1] >> 8) & 255) / 255.0, (c[1] & 255) / 255.0);
        }
    }
}
