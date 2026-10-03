package com.thesift.entity;

import com.thesift.block.SculkBloomBlock;
import com.thesift.music.MusicListener;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Bulb: a little bouncy jelly bunny of the Sift plains - one see-through cube that is all head, on
 * four stubby feet, with two tall ears. Bulbs hop everywhere, love Pitcher Bulbs (their breeding
 * food) and every so often squeeze out a Glowing Slime Ball. They sniff the air and groom their
 * ears, curl up asleep at night, wiggle with joy when fed and bounce in time to every note.
 *
 * <p>Each Bulb carries one to three small flowers on its back. Give it any small flower and it
 * nibbles it out of your hand, tosses it onto its back, then plucks one of its own and pops it over
 * to you. About one Bulb in thirty grows a rare Sculk Bloom, which keeps every Warden near it calm
 * and now and then puffs a little Sculk smoke. Bulbs born in the White Forest are snowy white.</p>
 */
public class Bulb extends Animal implements HopMoveControl.Hopper, MusicListener {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DANCE = SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.BOOLEAN);
    /** The flowers on its back (air: an empty spot), and the one it is nibbling from your hand. */
    private static final List<EntityDataAccessor<BlockState>> FLOWERS = List.of(
            SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.BLOCK_STATE),
            SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.BLOCK_STATE),
            SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.BLOCK_STATE));
    private static final EntityDataAccessor<BlockState> HELD = SynchedEntityData.defineId(Bulb.class, EntityDataSerializers.BLOCK_STATE);
    public static final int BLUE = 0;
    public static final int WHITE = 1;
    public static final int VARIANTS = 2;
    public static final int FLOWER_SLOTS = 3;
    /** Where each back flower stands, in model units (x across, z towards the tail), and how it is turned. */
    public static final float[][] FLOWER_SPOTS = {{0.4F, 1.4F, -48.0F}, {-2.6F, 3.3F, 42.0F}, {2.7F, 3.7F, -78.0F}};
    private static final BlockState AIR = Blocks.AIR.defaultBlockState();
    private static final byte EVENT_CROUCH = 120;
    private static final byte EVENT_SNIFF = 121;
    private static final byte EVENT_GROOM = 122;
    private static final byte EVENT_WIGGLE = 123;
    private static final byte EVENT_BEAT = 124;
    /** The flower swap: nibbling the flower from your hand, tossing it onto its back, plucking one off. */
    private static final byte EVENT_NIBBLE = 77;
    private static final byte EVENT_PLACE = 78;
    private static final byte EVENT_PLUCK = 79;
    private static final int SWAP_PLACE = 16;
    private static final int SWAP_PLUCK = 30;

    public final AnimationState sniffAnimation = new AnimationState();
    public final AnimationState groomAnimation = new AnimationState();
    public final AnimationState wiggleAnimation = new AnimationState();
    public final AnimationState nibbleAnimation = new AnimationState();
    public final AnimationState placeAnimation = new AnimationState();
    public final AnimationState pluckAnimation = new AnimationState();
    /** Client: ticks left of the crouch before a hop, and the age of the last music beat. */
    private int crouchTicks;
    public int lastBeat = -100;
    private int beatCooldown;
    /** Server: while notes keep coming it hops on them, not on its own dance rhythm. */
    private int noteQuiet;

    private int slimeTime;
    /** Server: the flower swap in progress (-1: none), the flower it was given and who gave it. */
    private int swapTicks = -1;
    private BlockState swapFlower = AIR;
    private java.util.@Nullable UUID swapPlayer;
    private int swapSlot = -1;
    /** Server: ticks to a Sculk Bloom on its back puffing smoke. */
    private int bloomPuff = 200;

    // ---- client animation state (squash & stretch, ears, back flowers)
    public final Spring squash = new Spring(0.28F, 0.22F);
    public final Spring earLeft = new Spring(0.18F, 0.16F);
    public final Spring earRight = new Spring(0.16F, 0.14F);
    public final Spring earPerk = new Spring(0.08F, 0.25F);
    /** The flowers on its back nod after every hop and landing. */
    public final Spring flowerSway = new Spring(0.2F, 0.14F);
    /** Client: when each back flower last changed, for its pop-in. */
    public final int[] flowerChanged = {-100, -100, -100};
    private final BlockState[] seenFlowers = {AIR, AIR, AIR};
    private boolean flowersSeen;
    private boolean wasOnGround = true;
    private double lastVelY;

    public Bulb(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new HopMoveControl<>(this);
        this.slimeTime = this.random.nextInt(6000) + 6000;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 6.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.JUMP_STRENGTH, 0.4);
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
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.6));
        this.goalSelector.addGoal(2, new SleepGoal());
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1, s -> s.is(ModTags.Items.BULB_FOOD), false));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.9));
        this.goalSelector.addGoal(6, new FidgetGoal());
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, BLUE);
        builder.define(DANCE, 0);
        builder.define(SLEEPING, false);
        for (EntityDataAccessor<BlockState> slot : FLOWERS) {
            builder.define(slot, AIR);
        }
        builder.define(HELD, AIR);
    }

    public boolean isSleepingBulb() {
        return this.entityData.get(SLEEPING);
    }

    private void setSleepingBulb(boolean b) {
        this.entityData.set(SLEEPING, b);
    }

    /** {@link #BLUE} or {@link #WHITE}. */
    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, Mth.clamp(v, 0, VARIANTS - 1));
    }

    public boolean isWhite() {
        return this.getVariant() == WHITE;
    }

    public boolean isDancing() {
        return this.entityData.get(DANCE) > 0;
    }

    public BlockState getBackFlower(int slot) {
        return this.entityData.get(FLOWERS.get(slot));
    }

    private void setBackFlower(int slot, BlockState flower) {
        this.entityData.set(FLOWERS.get(slot), flower);
    }

    /** The flower it is nibbling from your hand right now (air if none). */
    public BlockState getHeldFlower() {
        return this.entityData.get(HELD);
    }

    public boolean carriesSculkBloom() {
        for (int i = 0; i < FLOWER_SLOTS; i++) {
            if (this.getBackFlower(i).is(ModBlocks.SCULK_BLOOM.get())) {
                return true;
            }
        }
        return false;
    }

    /** Any small flower can go on a Bulb's back. */
    public static boolean isBackFlower(ItemStack stack) {
        return stack.getItem() instanceof BlockItem item && item.getBlock().defaultBlockState().is(BlockTags.SMALL_FLOWERS);
    }

    /** The Codex shows both coats, each with a little garden (client-side display entity only). */
    public void codexPose(int step) {
        this.setVariant(step % VARIANTS);
        this.setBackFlower(0, (step % VARIANTS == WHITE ? ModBlocks.SOULPETAL.get() : ModBlocks.DREAMBLOOM.get()).defaultBlockState());
        this.setBackFlower(1, (step % VARIANTS == WHITE ? Blocks.LILY_OF_THE_VALLEY : Blocks.CORNFLOWER).defaultBlockState());
        this.setBackFlower(2, (step % 4 == 2 ? ModBlocks.SCULK_BLOOM.get() : ModBlocks.LULLABY_BELL.get()).defaultBlockState());
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.Items.BULB_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Bulb baby = ModEntities.BULB.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null) {
            Bulb other = partner instanceof Bulb b ? b : this;
            Bulb from = this.random.nextBoolean() ? this : other;
            baby.setVariant(from.getVariant());
            // a little sprig from a parent's back to start its own garden
            BlockState sprig = from.randomBackFlower(-1);
            baby.setBackFlower(0, sprig.isAir() || sprig.is(ModBlocks.SCULK_BLOOM.get()) ? baby.naturalFlower(baby.isWhite()) : sprig);
        }
        return baby;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        boolean white = SnowCoat.at(level, this.blockPosition());
        this.setVariant(white ? WHITE : BLUE);
        this.growBackFlowers(white);
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    /** One to three flowers from its home's meadows; about one Bulb in thirty grows a Sculk Bloom. */
    private void growBackFlowers(boolean white) {
        int n = 1 + this.random.nextInt(FLOWER_SLOTS);
        for (int i = 0; i < FLOWER_SLOTS; i++) {
            this.setBackFlower(i, i < n ? this.naturalFlower(white) : AIR);
        }
        if (this.random.nextInt(100) < 3) {
            this.setBackFlower(this.random.nextInt(n), ModBlocks.SCULK_BLOOM.get().defaultBlockState());
        }
    }

    private BlockState naturalFlower(boolean white) {
        Block[] pool = white
                ? new Block[]{ModBlocks.SOULPETAL.get(), Blocks.LILY_OF_THE_VALLEY, Blocks.OXEYE_DAISY, Blocks.WHITE_TULIP, Blocks.AZURE_BLUET,
                        ModBlocks.LULLABY_BELL.get()}
                : new Block[]{ModBlocks.DREAMBLOOM.get(), ModBlocks.LULLABY_BELL.get(), ModBlocks.NEBULA_IRIS.get(), ModBlocks.HUMMINGBLOOM.get(),
                        Blocks.CORNFLOWER, Blocks.BLUE_ORCHID, Blocks.ALLIUM, Blocks.AZURE_BLUET};
        return pool[this.random.nextInt(pool.length)].defaultBlockState();
    }

    /** A random filled spot's flower, never the one in {@code skip} (air if there is none). */
    private BlockState randomBackFlower(int skip) {
        int slot = this.randomFilledSlot(skip);
        return slot < 0 ? AIR : this.getBackFlower(slot);
    }

    private int randomFilledSlot(int skip) {
        int[] filled = new int[FLOWER_SLOTS];
        int n = 0;
        for (int i = 0; i < FLOWER_SLOTS; i++) {
            if (i != skip && !this.getBackFlower(i).isAir()) {
                filled[n++] = i;
            }
        }
        return n == 0 ? -1 : filled[this.random.nextInt(n)];
    }

    // ------------------------------------------------------------------ hopping

    @Override
    public int hopDelay() {
        return this.isDancing() ? 2 : 4 + this.random.nextInt(6);
    }

    @Override
    public void onHop() {
        this.playSound(ModSounds.BULB_HOP.get(), 0.4F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + (this.isBaby() ? 1.7F : 1.3F));
    }

    @Override
    public int windup() {
        return 3;
    }

    @Override
    public void onWindup() {
        this.level().broadcastEntityEvent(this, EVENT_CROUCH);
    }

    @Override
    protected float getJumpPower() {
        return super.getJumpPower() * (this.isDancing() ? 0.8F : 1.0F);
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (!this.level().isClientSide()) {
            this.setSleepingBulb(false);
            this.entityData.set(DANCE, 60 + this.random.nextInt(40));
        }
    }

    /**
     * A single note was played nearby (see {@link CreatureLife}): the Bulb wakes, and bounces on the
     * beat - a crouch, then a little straight-up hop with a note.
     */
    public void hearNote(ServerLevel level, int pitch) {
        this.setSleepingBulb(false);
        this.entityData.set(DANCE, Math.max(this.entityData.get(DANCE), 40));
        if (this.beatCooldown > 0 || !this.onGround() || this.swapTicks >= 0) {
            return;
        }
        this.beatCooldown = 3;
        this.noteQuiet = 20;
        level.broadcastEntityEvent(this, EVENT_BEAT);
        this.getJumpControl().jump();
        this.getNavigation().stop();
        level.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 0, pitch / 24.0, 0.0, 0.0, 1.0);
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_CROUCH -> this.crouchTicks = 3;
            case EVENT_SNIFF -> this.sniffAnimation.start(this.tickCount);
            case EVENT_GROOM -> this.groomAnimation.start(this.tickCount);
            case EVENT_WIGGLE -> {
                this.wiggleAnimation.start(this.tickCount);
                this.squash.kick(0.25F);
                this.earLeft.kick(-0.5F);
                this.earRight.kick(0.5F);
                this.flowerSway.kick(0.5F);
            }
            case EVENT_BEAT -> {
                this.lastBeat = this.tickCount;
                this.squash.kick(-0.3F);
                this.earLeft.kick(0.4F);
                this.earRight.kick(0.35F);
            }
            case EVENT_NIBBLE -> {
                this.nibbleAnimation.start(this.tickCount);
                this.squash.kick(-0.12F);
                this.earPerk.kick(0.3F);
            }
            case EVENT_PLACE -> {
                this.placeAnimation.start(this.tickCount);
                this.squash.kick(0.3F);
                this.earLeft.kick(0.45F);
                this.earRight.kick(0.4F);
                this.flowerSway.kick(-0.6F);
            }
            case EVENT_PLUCK -> {
                this.pluckAnimation.start(this.tickCount);
                this.squash.kick(-0.25F);
                this.earLeft.kick(-0.35F);
                this.earRight.kick(-0.4F);
                this.flowerSway.kick(0.7F);
            }
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        this.setSleepingBulb(false);
        if (this.swapTicks >= 0) {
            // startled, it drops the flower it was nibbling
            if (!this.getHeldFlower().isAir()) {
                this.spawnAtLocation(level, new ItemStack(this.getHeldFlower().getBlock()));
            }
            this.endSwap();
        }
        return super.hurtServer(level, source, damage);
    }

    /** The flowers on its back fall off when it pops. */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        for (int i = 0; i < FLOWER_SLOTS; i++) {
            if (!this.getBackFlower(i).isAir()) {
                this.spawnAtLocation(level, new ItemStack(this.getBackFlower(i).getBlock()));
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.beatCooldown > 0) {
                this.beatCooldown--;
            }
            if (this.noteQuiet > 0) {
                this.noteQuiet--;
            }
            this.tickSwap(server);
            if (this.isSleepingBulb() && this.tickCount % 50 == 0) {
                server.sendParticles(ModParticles.SLEEP_SPORE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 1, 0.1, 0.05, 0.1, 0.0);
            }
            int dance = this.entityData.get(DANCE);
            if (dance > 0) {
                this.entityData.set(DANCE, dance - 1);
                if (this.onGround() && this.tickCount % 8 == 0 && this.noteQuiet <= 0 && this.swapTicks < 0) {
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
                this.playSound(ModSounds.BULB_LAY.get(), 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.2F);
                server.sendParticles(ModParticles.GLOW_SPLAT.get(), this.getX(), this.getY() + 0.1, this.getZ(), 4, 0.15, 0.05, 0.15, 0.05);
                this.slimeTime = this.random.nextInt(6000) + 6000;
            }
            // a Sculk Bloom on its back hums the Wardens calm, and now and then puffs a little smoke
            if (this.tickCount % 20 == 7 && this.carriesSculkBloom()) {
                SculkBloomBlock.calmWardens(server, this.position(), SculkBloomBlock.CALM_RADIUS);
                if ((this.bloomPuff -= 20) <= 0) {
                    this.bloomPuff = 300 + this.random.nextInt(400);
                    SculkBloomBlock.puff(server, this.position().add(0.0, this.getBbHeight() + 0.1, 0.0), this, 0.45F);
                }
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
            // landing: splat, the ears and flowers whip on through, a little jelly splashes
            float impact = (float) Mth.clamp(-this.lastVelY * 1.6, 0.15, 0.55);
            this.squash.kick(-impact);
            this.earLeft.kick(impact * 1.4F);
            this.earRight.kick(impact * 1.2F);
            this.flowerSway.kick(impact * 1.5F);
            this.slimeTrail(1 + this.random.nextInt(2));
        } else if (!onGround && this.wasOnGround && vy > 0) {
            // take-off: stretch tall and let the ears and flowers trail behind
            this.squash.kick(0.3F);
            this.earLeft.kick(-0.45F);
            this.earRight.kick(-0.38F);
            this.flowerSway.kick(-0.6F);
        }
        this.wasOnGround = onGround;
        this.lastVelY = vy;
        // anticipation: it squats down for a moment before every hop
        if (this.crouchTicks > 0) {
            this.crouchTicks--;
            this.squash.setTarget(-0.24F);
            if (this.crouchTicks == 0) {
                this.squash.setTarget(0.0F);
            }
        } else {
            this.squash.setTarget(this.isSleepingBulb() ? -0.12F : 0.0F);
        }
        // ears perk up when a player is close by, droop a little otherwise
        Player near = this.level().getNearestPlayer(this, 6.0);
        this.earPerk.setTarget(near != null && !this.isSleepingBulb() ? 1.0F : 0.0F);
        if (onGround && this.getDeltaMovement().horizontalDistanceSqr() > 0.0004 && this.random.nextInt(6) == 0) {
            this.slimeTrail(1);
        }
        this.squash.tick();
        this.earLeft.tick();
        this.earRight.tick();
        this.earPerk.tick();
        this.flowerSway.tick();
        // a flower that just landed on its back (or was plucked) pops in
        for (int i = 0; i < FLOWER_SLOTS; i++) {
            BlockState now = this.getBackFlower(i);
            if (now != this.seenFlowers[i]) {
                if (this.flowersSeen) {
                    this.flowerChanged[i] = this.tickCount;
                }
                this.seenFlowers[i] = now;
            }
        }
        this.flowersSeen = true;
        if (this.isWhite() && this.random.nextInt(40) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.01, 0);
        }
    }

    /** Jelly tint of each coat, for the slime it leaves behind. */
    private static final float[][] SLIME_TINT = {{0.42F, 0.74F, 0.95F}, {0.86F, 0.93F, 0.98F}};

    private void slimeTrail(int count) {
        float[] c = SLIME_TINT[this.getVariant()];
        for (int i = 0; i < count; i++) {
            this.level().addParticle(ModParticles.SLIME_TRAIL.get(), this.getX() + (this.random.nextDouble() - 0.5) * 0.3, this.getY() + 0.02,
                    this.getZ() + (this.random.nextDouble() - 0.5) * 0.3, c[0], c[1], c[2]);
        }
    }

    // ------------------------------------------------------------------ the flower swap

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isBackFlower(stack) && !this.isFood(stack)) {
            if (this.swapTicks >= 0) {
                return InteractionResult.PASS;
            }
            if (this.level() instanceof ServerLevel server) {
                this.startSwap(server, player, ((BlockItem) stack.getItem()).getBlock().defaultBlockState());
                this.usePlayerItem(player, hand, stack);
            }
            return InteractionResult.SUCCESS;
        }
        boolean food = this.isFood(stack);
        InteractionResult result = super.mobInteract(player, hand);
        if (food && result.consumesAction() && this.level() instanceof ServerLevel server) {
            this.slimeTime = Math.max(20, this.slimeTime / 2);
            server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.3, this.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
            this.playSound(ModSounds.BULB_HAPPY.get(), 0.8F, 1.3F);
            this.setSleepingBulb(false);
            server.broadcastEntityEvent(this, EVENT_WIGGLE);
        }
        return result;
    }

    private void startSwap(ServerLevel level, Player player, BlockState flower) {
        this.setSleepingBulb(false);
        this.swapTicks = 0;
        this.swapFlower = flower;
        this.swapPlayer = player.getUUID();
        this.swapSlot = -1;
        this.entityData.set(HELD, flower);
        this.getNavigation().stop();
        level.broadcastEntityEvent(this, EVENT_NIBBLE);
        this.playSound(ModSounds.BULB_NIBBLE.get(), 0.8F, 1.0F + this.random.nextFloat() * 0.2F);
    }

    /**
     * Nibbles the flower (a few munches), tosses it onto its back with a little hop, then plucks one
     * of its own and pops it over to the giver. With a full back it swaps one straight away.
     */
    private void tickSwap(ServerLevel level) {
        if (this.swapTicks < 0) {
            return;
        }
        this.swapTicks++;
        this.getNavigation().stop();
        Player giver = this.swapPlayer == null ? null : level.getPlayerByUUID(this.swapPlayer);
        if (giver != null) {
            this.getLookControl().setLookAt(giver, 30.0F, 30.0F);
        }
        if (this.swapTicks == 7 || this.swapTicks == 12) {
            this.playSound(ModSounds.BULB_NIBBLE.get(), 0.6F, 1.2F + this.random.nextFloat() * 0.2F);
        }
        if (this.swapTicks == SWAP_PLACE) {
            this.entityData.set(HELD, AIR);
            int free = -1;
            for (int i = 0; i < FLOWER_SLOTS && free < 0; i++) {
                if (this.getBackFlower(i).isAir()) {
                    free = i;
                }
            }
            if (free >= 0) {
                this.setBackFlower(free, this.swapFlower);
                this.swapSlot = free;
                level.broadcastEntityEvent(this, EVENT_PLACE);
                this.playSound(ModSounds.BULB_HAPPY.get(), 0.6F, 1.5F);
                if (this.onGround()) {
                    this.getJumpControl().jump();
                }
            } else {
                int slot = this.random.nextInt(FLOWER_SLOTS);
                BlockState old = this.getBackFlower(slot);
                this.setBackFlower(slot, this.swapFlower);
                this.popFlower(level, old, slot, giver);
                this.endSwap();
            }
        } else if (this.swapTicks == SWAP_PLUCK) {
            int slot = this.randomFilledSlot(this.swapSlot);
            if (slot >= 0) {
                BlockState old = this.getBackFlower(slot);
                this.setBackFlower(slot, AIR);
                this.popFlower(level, old, slot, giver);
            } else {
                // nothing else on its back to give: it just wiggles, pleased with its new flower
                level.broadcastEntityEvent(this, EVENT_WIGGLE);
                this.playSound(ModSounds.BULB_HAPPY.get(), 0.8F, 1.3F);
            }
            this.endSwap();
        }
    }

    private void endSwap() {
        this.swapTicks = -1;
        this.swapFlower = AIR;
        this.swapPlayer = null;
        this.swapSlot = -1;
        this.entityData.set(HELD, AIR);
    }

    /** Plucks the flower in {@code slot} off its back and pops it over to {@code to}. */
    private void popFlower(ServerLevel level, BlockState flower, int slot, @Nullable Player to) {
        level.broadcastEntityEvent(this, EVENT_PLUCK);
        this.playSound(ModSounds.BULB_PLUCK.get(), 0.9F, 1.0F + this.random.nextFloat() * 0.2F);
        Vec3 from = this.backSpot(slot);
        ItemEntity item = new ItemEntity(level, from.x, from.y, from.z, new ItemStack(flower.getBlock()));
        Vec3 aim = to != null ? to.position().subtract(from) : this.getLookAngle();
        Vec3 flat = new Vec3(aim.x, 0.0, aim.z);
        flat = flat.lengthSqr() < 1.0E-4 ? Vec3.ZERO : flat.normalize().scale(0.2);
        item.setDeltaMovement(flat.x, 0.3, flat.z);
        item.setDefaultPickUpDelay();
        level.addFreshEntity(item);
        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, flower), from.x, from.y, from.z, 8, 0.06, 0.04, 0.06, 0.05);
        level.sendParticles(ParticleTypes.HEART, from.x, from.y + 0.25, from.z, 1, 0.1, 0.05, 0.1, 0.0);
    }

    /** Where a back flower stands in the world. */
    private Vec3 backSpot(int slot) {
        double unit = 0.6 / 16.0 * (this.isBaby() ? 0.55 : 1.0);
        float yaw = this.yBodyRot * Mth.DEG_TO_RAD;
        double mx = FLOWER_SPOTS[slot][0] * unit;
        double mz = FLOWER_SPOTS[slot][1] * unit;
        // model +x is the Bulb's left, model +z its back
        double x = this.getX() + Mth.cos(yaw) * mx + Mth.sin(yaw) * mz;
        double z = this.getZ() + Mth.sin(yaw) * mx - Mth.cos(yaw) * mz;
        return new Vec3(x, this.getY() + 9.0 * unit + 0.08, z);
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
        this.playSound(ModSounds.BULB_SQUISH.get(), 0.12F, 1.4F);
    }

    @Override
    public float getVoicePitch() {
        return this.isBaby() ? 1.7F + this.random.nextFloat() * 0.2F : 1.25F + (this.random.nextFloat() - this.random.nextFloat()) * 0.15F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Coat", this.getVariant());
        output.putInt("SlimeTime", this.slimeTime);
        List<BlockState> flowers = new ArrayList<>();
        for (int i = 0; i < FLOWER_SLOTS; i++) {
            flowers.add(this.getBackFlower(i));
        }
        output.store("BackFlowers", BlockState.CODEC.listOf(), flowers);
        if (!this.getHeldFlower().isAir()) {
            output.store("Nibbling", BlockState.CODEC, this.getHeldFlower());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.getIntOr("Coat", BLUE));
        this.slimeTime = input.getIntOr("SlimeTime", 6000);
        List<BlockState> flowers = input.read("BackFlowers", BlockState.CODEC.listOf()).orElse(null);
        if (flowers == null) {
            // a Bulb from before Bulbs carried flowers grows its garden now
            this.growBackFlowers(this.isWhite());
        } else {
            for (int i = 0; i < FLOWER_SLOTS; i++) {
                this.setBackFlower(i, i < flowers.size() ? flowers.get(i) : AIR);
            }
        }
        // saved mid-swap: the flower it was nibbling goes onto its back
        input.read("Nibbling", BlockState.CODEC).ifPresent(held -> {
            for (int i = 0; i < FLOWER_SLOTS; i++) {
                if (this.getBackFlower(i).isAir()) {
                    this.setBackFlower(i, held);
                    return;
                }
            }
        });
    }

    /** Curls up and sleeps through the night; noise, a hit or a note wakes it. */
    private final class SleepGoal extends Goal {
        SleepGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Bulb b = Bulb.this;
            if (b.isSleepingBulb()) {
                return true;
            }
            return b.level().isDarkOutside() && b.onGround() && !b.isDancing() && !b.isInLove() && b.swapTicks < 0 && b.random.nextInt(160) == 0
                    && b.level().getNearestPlayer(b, 4.0) == null;
        }

        @Override
        public boolean canContinueToUse() {
            Bulb b = Bulb.this;
            return b.isSleepingBulb() && b.level().isDarkOutside() && !b.isDancing() && b.hurtTime == 0;
        }

        @Override
        public void start() {
            Bulb.this.setSleepingBulb(true);
            Bulb.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Bulb.this.getNavigation().stop();
            Bulb.this.setZza(0.0F);
        }

        @Override
        public void stop() {
            Bulb.this.setSleepingBulb(false);
        }
    }

    /** Sitting still it sniffs the air or grooms its long ears. */
    private final class FidgetGoal extends Goal {
        private int ticks;

        FidgetGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Bulb b = Bulb.this;
            return b.onGround() && !b.isSleepingBulb() && !b.isDancing() && b.swapTicks < 0 && b.getNavigation().isDone() && b.random.nextInt(140) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks > 0;
        }

        @Override
        public void start() {
            Bulb b = Bulb.this;
            boolean groom = b.random.nextInt(3) == 0;
            this.ticks = groom ? 40 : 30;
            b.level().broadcastEntityEvent(b, groom ? EVENT_GROOM : EVENT_SNIFF);
            b.getNavigation().stop();
        }

        @Override
        public void tick() {
            this.ticks--;
            Bulb.this.getNavigation().stop();
        }
    }

    private static final int[][] BURST = {{0x6AABE8, 0x65CFE7}, {0xEEF4FC, 0xBFEAF4}};

    /** Killed, it bursts into jelly: droplets and stars in its own colours, and a few splats on the ground. */
    @Override
    public void makePoofParticles() {
        int[] c = BURST[this.getVariant()];
        KillBurst.pop(this, c[0], c[1], KillBurst.DROP, null);
        for (int i = 0; i < 4; i++) {
            this.level().addParticle(ModParticles.SLIME_TRAIL.get(), this.getRandomX(0.9), this.getY() + 0.02, this.getRandomZ(0.9),
                    ((c[1] >> 16) & 255) / 255.0, ((c[1] >> 8) & 255) / 255.0, (c[1] & 255) / 255.0);
        }
    }
}
