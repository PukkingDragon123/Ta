package com.thesift.entity;

import com.mojang.datafixers.util.Pair;
import com.thesift.TheSift;
import com.thesift.music.MusicListener;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import java.util.EnumSet;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
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
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Harmoner: a chunky alien songbird of the Sift. Harmoners hop and flutter about, singing little
 * melodies in their own key. Feed one some seeds and it takes off, singing, and leads you to the
 * structure its colour belongs to - it waits for you when you fall behind and circles above the
 * place once you arrive.
 *
 * <p>A flock lives together: they peck about for seeds by day, preen each other, roost asleep with
 * their heads tucked under a wing at night, and when one starts to sing the others nearby join in
 * a moment later, in harmony. Play a note and one will sing it back to you.
 */
public class Harmoner extends Animal implements MusicListener {
    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(Harmoner.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> GUIDING = SynchedEntityData.defineId(Harmoner.class, EntityDataSerializers.BOOLEAN);
    /** Ids 60-67 are vanilla's. */
    private static final byte EVENT_SING = 113;
    private static final byte EVENT_PECK = 114;
    private static final byte EVENT_PREEN = 115;
    private static final EntityDataAccessor<Boolean> SLEEPING = SynchedEntityData.defineId(Harmoner.class, EntityDataSerializers.BOOLEAN);
    /** How far a song carries to the rest of the flock. */
    private static final double CHORUS_RANGE = 12.0;

    public static final String[] NAMES = {"rose", "azure", "gold", "violet", "jade", "coral", "night"};
    /** The structure each colour leads to. */
    public static final String[] STRUCTURES = {"abandoned_altar", "chrome_well", "dream_statue", "collapsed_tower", "sift_ruins", "musical_temple",
            "sculk_castle"};
    public static final int[] COLORS = {0xE8577F, 0x3F8FE8, 0xF2B632, 0x8C5AE0, 0x35C28F, 0xFF7A4A, 0x2EF2E2};
    public static final int VARIANTS = NAMES.length;
    /** Each colour's song, in semitones above F#3 (the note block's lowest note). */
    private static final int[][] SONGS = {
            {12, 16, 19, 24, 19}, {7, 11, 14, 19, 23, 19}, {14, 17, 21, 17, 14, 21}, {9, 12, 16, 21, 16}, {11, 14, 18, 23, 18, 14},
            {16, 19, 23, 21, 19, 16}, {4, 3, 7, 6, 0, 1}};
    private static final int NOTE_TICKS = 4;
    private static final int GUIDE_TICKS = 20 * 150;

    public final AnimationState singAnimation = new AnimationState();
    public final AnimationState peckAnimation = new AnimationState();
    public final AnimationState preenAnimation = new AnimationState();
    /** The melody being sung (its own, or the leader's when it joins a chorus) and its harmony offset. */
    private int @Nullable [] song;
    private int transpose;
    private int songDelay;
    private int echoNote = -1;
    private int echoTimer;
    private int socialCooldown = 300;
    /** Client: smoothed wing spread (0 folded, 1 beating). */
    public float flap;
    public float flapO;

    private @Nullable BlockPos guideTarget;
    private int guideTicks;
    private int arrivedTicks;
    private int songNote = -1;
    private int songTimer;
    private int singCooldown = 200;
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> TAME = net.minecraft.network.syncher.SynchedEntityData.defineId(
            Harmoner.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);
    private java.util.@Nullable UUID owner;

    public Harmoner(EntityType<? extends Animal> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Animal.createAnimalAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.MOVEMENT_SPEED, 0.25).add(Attributes.FLYING_SPEED, 0.6)
                .add(Attributes.FOLLOW_RANGE, 48.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
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
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.4));
        this.goalSelector.addGoal(2, new GuideGoal());
        this.goalSelector.addGoal(2, new RoostGoal());
        this.goalSelector.addGoal(3, new FollowOwner());
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1, s -> s.is(ModTags.Items.HARMONER_FOOD), false));
        this.goalSelector.addGoal(5, new FollowParentGoal(this, 1.1));
        this.goalSelector.addGoal(6, new ForageGoal());
        this.goalSelector.addGoal(6, new PreenGoal());
        this.goalSelector.addGoal(6, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(GUIDING, false);
        builder.define(TAME, false);
        builder.define(SLEEPING, false);
    }

    /** Roosting asleep for the night, head under its wing. */
    public boolean isRoosting() {
        return this.entityData.get(SLEEPING);
    }

    private void setRoosting(boolean b) {
        this.entityData.set(SLEEPING, b);
    }

    /** Tamed by sneaking and offering it a Hummingbloom: it follows you and sings along with your flute. */
    public boolean isTame() {
        return this.entityData.get(TAME);
    }

    public java.util.@Nullable UUID getOwnerId() {
        return this.owner;
    }

    public int getVariant() {
        return Mth.clamp(this.entityData.get(VARIANT), 0, VARIANTS - 1);
    }

    public void setVariant(int v) {
        this.entityData.set(VARIANT, Mth.clamp(v, 0, VARIANTS - 1));
    }

    public boolean isGuiding() {
        return this.entityData.get(GUIDING);
    }

    public @Nullable BlockPos getGuideTarget() {
        return this.guideTarget;
    }

    public boolean isFlying() {
        return !this.onGround();
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(ModTags.Items.HARMONER_FOOD);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Harmoner baby = ModEntities.HARMONER.get().create(level, EntitySpawnReason.BREEDING);
        if (baby != null && partner instanceof Harmoner other) {
            baby.setVariant(this.random.nextInt(10) == 0 ? this.random.nextInt(VARIANTS) : this.random.nextBoolean() ? this.getVariant() : other.getVariant());
        }
        return baby;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason,
            @Nullable SpawnGroupData data) {
        // the midnight Harmoner, who knows the way to the Dictator's castle, is the rarest
        this.setVariant(this.random.nextInt(14) == 0 ? VARIANTS - 1 : this.random.nextInt(VARIANTS - 1));
        return super.finalizeSpawn(level, difficulty, reason, data);
    }

    // ------------------------------------------------------------------ feeding & guiding

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && !this.isTame() && !this.isBaby() && stack.is(com.thesift.registry.ModItems.HUMMINGBLOOM.get())) {
            if (this.level() instanceof ServerLevel server) {
                this.usePlayerItem(player, hand, stack);
                if (this.random.nextInt(3) == 0) {
                    this.entityData.set(TAME, true);
                    this.owner = player.getUUID();
                    this.setPersistenceRequired();
                    server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 7, 0.3, 0.2, 0.3, 0.0);
                    this.playSound(ModSounds.HARMONER_SING.get(), 1.0F, pitch(13));
                    player.sendOverlayMessage(Component.translatable("message.thesift.harmoner.tamed"));
                } else {
                    server.sendParticles(ParticleTypes.SMOKE, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 6, 0.2, 0.2, 0.2, 0.0);
                }
            }
            return InteractionResult.SUCCESS;
        }
        if (this.isFood(stack) && !this.isBaby() && !this.isGuiding() && this.getAge() == 0 && !this.isInLove()) {
            if (this.level() instanceof ServerLevel server) {
                this.usePlayerItem(player, hand, stack);
                this.startGuiding(server, player);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Finds the nearest structure of this Harmoner's colour and starts leading the way there. */
    public boolean startGuiding(ServerLevel level, @Nullable Player player) {
        BlockPos target = this.findStructure(level);
        level.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 2, 0.2, 0.1, 0.2, 0.0);
        if (target == null) {
            // nothing of its colour anywhere near: a sad little two-note song
            this.playSound(ModSounds.HARMONER_SING.get(), 1.0F, pitch(6));
            this.playSound(ModSounds.HARMONER_SING.get(), 1.0F, pitch(1));
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.thesift.harmoner.lost"));
            }
            return false;
        }
        this.guideTarget = target;
        this.guideTicks = GUIDE_TICKS;
        this.arrivedTicks = 0;
        this.entityData.set(GUIDING, true);
        this.startSong();
        if (player != null) {
            player.sendOverlayMessage(Component.translatable("message.thesift.harmoner.guide." + NAMES[this.getVariant()]));
        }
        return true;
    }

    private @Nullable BlockPos findStructure(ServerLevel level) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder = registry.get(ResourceKey.create(Registries.STRUCTURE, TheSift.id(STRUCTURES[this.getVariant()])));
        if (holder.isEmpty()) {
            return null;
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), this.blockPosition(), 48, false);
        return found == null ? null : found.getFirst();
    }

    private void stopGuiding() {
        this.guideTarget = null;
        this.guideTicks = 0;
        this.entityData.set(GUIDING, false);
    }

    /** Leads the way: flies a stretch towards the target, waits for the player, circles on arrival. */
    /** A tamed Harmoner flies after its owner when it falls behind. */
    private final class FollowOwner extends Goal {
        FollowOwner() {
            this.setFlags(java.util.EnumSet.of(Goal.Flag.MOVE));
        }

        private @Nullable Player owner() {
            Harmoner h = Harmoner.this;
            return h.owner == null ? null : h.level().getPlayerByUUID(h.owner);
        }

        @Override
        public boolean canUse() {
            Player p = this.owner();
            return Harmoner.this.isTame() && !Harmoner.this.isGuiding() && p != null && p.distanceToSqr(Harmoner.this) > 64.0;
        }

        @Override
        public void tick() {
            Player p = this.owner();
            if (p == null) {
                return;
            }
            if (p.distanceToSqr(Harmoner.this) > 900.0) {
                Harmoner.this.teleportTo(p.getX(), p.getY() + 1.5, p.getZ());
            } else {
                Harmoner.this.getNavigation().moveTo(p.getX(), p.getY() + 1.5, p.getZ(), 1.2);
            }
        }
    }

    private final class GuideGoal extends Goal {
        private int repath;

        GuideGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return Harmoner.this.guideTarget != null && Harmoner.this.guideTicks > 0;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            Harmoner self = Harmoner.this;
            if (self.guideTarget == null || --this.repath > 0) {
                return;
            }
            this.repath = 8;
            Vec3 here = self.position();
            Vec3 target = Vec3.atCenterOf(self.guideTarget);
            double dx = target.x - here.x;
            double dz = target.z - here.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            Player player = self.level().getNearestPlayer(self, 48.0);
            double wx;
            double wz;
            double speed = 1.0;
            if (dist < 7.0) {
                // arrived: circle above the place and sing about it
                double a = self.tickCount * 0.08;
                wx = target.x + Math.cos(a) * 5.0;
                wz = target.z + Math.sin(a) * 5.0;
                speed = 0.8;
                if (self.arrivedTicks++ % 5 == 0 && self.songNote < 0) {
                    self.startSong();
                }
                if (self.arrivedTicks > 40) {
                    self.guideTicks = Math.min(self.guideTicks, 60);
                }
            } else if (player != null && self.distanceTo(player) > 14.0) {
                // the player fell behind: fly back and wait above them
                wx = player.getX();
                wz = player.getZ();
                speed = 1.2;
            } else {
                double step = Math.min(10.0, dist);
                wx = here.x + dx / dist * step;
                wz = here.z + dz / dist * step;
                speed = 1.1;
            }
            int ground = self.level().getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(wx), Mth.floor(wz));
            double wy = Math.max(ground + 4.0, Math.min(here.y + 3.0, ground + 9.0));
            self.getMoveControl().setWantedPosition(wx, wy, wz, speed);
            self.getLookControl().setLookAt(wx, wy, wz);
        }

        @Override
        public void stop() {
            if (Harmoner.this.guideTicks <= 0) {
                Harmoner.this.stopGuiding();
            }
        }
    }

    // ------------------------------------------------------------------ singing

    private static float pitch(int semitone) {
        return (float) Math.pow(2.0, (semitone - 12) / 12.0);
    }

    /** Starts its own song and calls the flock in: those nearby join a moment later, in harmony. */
    public void startSong() {
        int[] mine = SONGS[this.getVariant()];
        this.sing(mine, 0, 0);
        int voice = 0;
        for (Harmoner other : this.level().getEntitiesOfClass(Harmoner.class, this.getBoundingBox().inflate(CHORUS_RANGE),
                h -> h != this && h.isAlive() && h.songNote < 0 && !h.isRoosting())) {
            // thirds and fifths above and below, so the chorus spreads into chords
            int[] intervals = {4, 7, -5, 12, -8};
            other.sing(mine, intervals[voice % intervals.length], 3 + voice * 2);
            if (++voice >= 5) {
                break;
            }
        }
    }

    private void sing(int[] melody, int harmony, int delay) {
        this.setRoosting(false);
        this.song = melody;
        this.transpose = harmony;
        this.songDelay = delay;
        this.songNote = 0;
        this.songTimer = 0;
        this.singCooldown = 260 + this.random.nextInt(400);
        if (delay == 0) {
            this.level().broadcastEntityEvent(this, EVENT_SING);
        }
    }

    /** A note was played nearby (see {@link CreatureLife}): it sings the note back, a little higher. */
    public void hearNote(ServerLevel level, int pitch) {
        if (this.songNote >= 0 || this.echoTimer > 0) {
            return;
        }
        this.setRoosting(false);
        this.echoNote = Mth.clamp(pitch + (this.random.nextBoolean() ? 7 : 12), 0, 24);
        this.echoTimer = 6 + this.random.nextInt(6);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_SING) {
            this.singAnimation.start(this.tickCount);
        } else if (id == EVENT_PECK) {
            this.peckAnimation.start(this.tickCount);
        } else if (id == EVENT_PREEN) {
            this.preenAnimation.start(this.tickCount);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (this.songNote < 0 && this.random.nextFloat() < strength) {
            this.startSong();
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel server) {
            if (this.guideTicks > 0 && --this.guideTicks == 0) {
                this.stopGuiding();
            }
            if (this.socialCooldown > 0) {
                this.socialCooldown--;
            }
            if (this.echoTimer > 0 && --this.echoTimer == 0 && this.echoNote >= 0) {
                server.broadcastEntityEvent(this, EVENT_SING);
                this.playSound(ModSounds.HARMONER_SING.get(), 1.1F, pitch(this.echoNote));
                server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 0,
                        this.echoNote / 24.0, 0.0, 0.0, 1.0);
                this.echoNote = -1;
                this.echoTimer = 20;
            }
            if (this.songNote >= 0 && this.songDelay > 0) {
                if (--this.songDelay == 0) {
                    server.broadcastEntityEvent(this, EVENT_SING);
                }
            } else if (this.songNote >= 0 && this.songTimer-- <= 0) {
                int[] song = this.song != null ? this.song : SONGS[this.getVariant()];
                this.songTimer = NOTE_TICKS;
                int n = Mth.clamp(song[this.songNote] + this.transpose, 0, 24);
                this.playSound(ModSounds.HARMONER_SING.get(), 1.2F, pitch(n));
                server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 0, n / 24.0, 0.0, 0.0, 1.0);
                if (++this.songNote >= song.length) {
                    this.songNote = -1;
                }
            }
            if (this.songNote < 0 && --this.singCooldown <= 0 && !this.isRoosting() && this.random.nextInt(4) == 0) {
                this.startSong();
            }
            // a trail of notes in its own colour while leading the way
            if (this.isGuiding() && this.tickCount % 3 == 0) {
                int c = COLORS[this.getVariant()];
                server.sendParticles(ModParticles.GUIDE_NOTE.get(), this.getX(), this.getY() + 0.3, this.getZ(), 0, ((c >> 16) & 255) / 255.0,
                        ((c >> 8) & 255) / 255.0, (c & 255) / 255.0, 1.0);
            }
        } else {
            this.flapO = this.flap;
            float target = this.onGround() ? 0.0F : 1.0F;
            this.flap += (target - this.flap) * 0.3F;
        }
    }

    // ------------------------------------------------------------------ misc

    @Override
    public boolean causeFallDamage(double distance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.HARMONER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.HARMONER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.HARMONER_DEATH.get();
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Variant", this.getVariant());
        output.putBoolean("Tame", this.isTame());
        if (this.owner != null) {
            output.putString("Owner", this.owner.toString());
        }
        output.putInt("GuideTicks", this.guideTicks);
        if (this.guideTarget != null) {
            output.putInt("GuideX", this.guideTarget.getX());
            output.putInt("GuideY", this.guideTarget.getY());
            output.putInt("GuideZ", this.guideTarget.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setVariant(input.getIntOr("Variant", 0));
        this.entityData.set(TAME, input.getBooleanOr("Tame", false));
        String o = input.getStringOr("Owner", "");
        this.owner = o.isEmpty() ? null : java.util.UUID.fromString(o);
        this.guideTicks = input.getIntOr("GuideTicks", 0);
        if (this.guideTicks > 0) {
            this.guideTarget = new BlockPos(input.getIntOr("GuideX", 0), input.getIntOr("GuideY", 64), input.getIntOr("GuideZ", 0));
            this.entityData.set(GUIDING, true);
        }
    }

    /** At night, perched on the ground, it tucks its head under a wing and sleeps till morning. */
    private final class RoostGoal extends Goal {
        RoostGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.JUMP));
        }

        @Override
        public boolean canUse() {
            Harmoner h = Harmoner.this;
            if (h.isRoosting()) {
                return true;
            }
            return h.level().isDarkOutside() && h.onGround() && !h.isGuiding() && h.songNote < 0 && h.random.nextInt(80) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            Harmoner h = Harmoner.this;
            return h.isRoosting() && h.level().isDarkOutside() && !h.isGuiding() && h.hurtTime == 0;
        }

        @Override
        public void start() {
            Harmoner.this.setRoosting(true);
            Harmoner.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Harmoner.this.getNavigation().stop();
        }

        @Override
        public void stop() {
            Harmoner.this.setRoosting(false);
        }
    }

    /** By day, on the ground, it pecks about for seeds. */
    private final class ForageGoal extends Goal {
        private int ticks;

        ForageGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Harmoner h = Harmoner.this;
            return h.onGround() && !h.isGuiding() && !h.isRoosting() && h.songNote < 0 && h.random.nextInt(160) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            return this.ticks > 0;
        }

        @Override
        public void start() {
            this.ticks = 50;
            Harmoner.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            Harmoner h = Harmoner.this;
            this.ticks--;
            h.getNavigation().stop();
            if (this.ticks % 16 == 8 && h.level() instanceof ServerLevel server) {
                server.broadcastEntityEvent(h, EVENT_PECK);
                net.minecraft.world.level.block.state.BlockState below = server.getBlockState(h.blockPosition().below());
                if (!below.isAir()) {
                    Vec3 beak = h.position().add(Vec3.directionFromRotation(0.0F, h.yBodyRot).scale(0.3));
                    server.sendParticles(new net.minecraft.core.particles.BlockParticleOption(ParticleTypes.BLOCK, below), beak.x, h.getY() + 0.05, beak.z,
                            3, 0.05, 0.02, 0.05, 0.05);
                }
            }
        }
    }

    /** Two flock-mates sit together and preen each other's feathers. */
    private final class PreenGoal extends Goal {
        private @Nullable Harmoner mate;
        private int ticks;

        PreenGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Harmoner h = Harmoner.this;
            if (h.socialCooldown > 0 || h.isGuiding() || h.isRoosting() || h.random.nextInt(120) != 0) {
                return false;
            }
            for (Harmoner o : h.level().getEntitiesOfClass(Harmoner.class, h.getBoundingBox().inflate(6.0), o -> o != h && o.isAlive() && !o.isGuiding())) {
                this.mate = o;
                return true;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return this.mate != null && this.mate.isAlive() && this.ticks > 0;
        }

        @Override
        public void start() {
            this.ticks = 100;
        }

        @Override
        public void tick() {
            Harmoner h = Harmoner.this;
            Harmoner m = this.mate;
            if (m == null) {
                return;
            }
            this.ticks--;
            h.getLookControl().setLookAt(m, 30.0F, 30.0F);
            if (h.distanceToSqr(m) > 1.5) {
                h.getNavigation().moveTo(m, 1.0);
            } else {
                h.getNavigation().stop();
                h.level().broadcastEntityEvent(h, EVENT_PREEN);
                h.level().broadcastEntityEvent(m, EVENT_PREEN);
                h.playSound(ModSounds.HARMONER_AMBIENT.get(), 0.5F, 1.4F);
                h.socialCooldown = m.socialCooldown = 900 + h.random.nextInt(900);
                this.ticks = 0;
            }
        }
    }

    /** A puff of feathers and notes in its own colour. */
    @Override
    public void makePoofParticles() {
        int c = COLORS[Math.floorMod(this.getVariant(), COLORS.length)];
        KillBurst.pop(this, c, 0xFFF4C2, KillBurst.NOTE, ModParticles.WISHWOOD_LEAF.get());
    }
}
