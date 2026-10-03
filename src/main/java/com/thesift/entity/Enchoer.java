package com.thesift.entity;

import com.thesift.music.MusicListener;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.band.BandPlayer;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModRings;
import com.thesift.registry.ModSounds;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Echoer (entity id {@code enchoer}), CR1: a speaker-bat of the Sift. Its furry body is a
 * loudspeaker with a woofer in its chest, its ears are tweeters, there is a little speaker cone in
 * each wing, and its snout is a spiralling brass drill that whirrs round when it sings. It flutters
 * a few blocks over the meadows sending out rings of echolocation, and it does not trade: it accepts
 * offerings.
 *
 * <p>The ceremony: drop an offering ({@code #thesift:echoer_offerings}) near it. It flies over,
 * hovers low and pings the gift with its drill, then tucks it away and waits about a minute, ringing
 * the first notes of <em>The Offering</em> to remind you. Play that song nearby on the Wind Chimes
 * ({@link Song#OFFERING}) and it dances - and gives something valuable in return
 * ({@code thesift:gameplay/echoer_reward}). If no song comes it droops and gives the offering back.
 *
 * <p>Between ceremonies it flutters about, cocks its ears at things, bows to visitors, sings along
 * to any music (its drill spinning, every cone pumping) and roosts with its wings wrapped round it
 * when nobody is around.
 */
public class Enchoer extends PathfinderMob implements MusicListener, BandPlayer {
    public static final int IDLE = 0;
    public static final int INSPECT = 1;
    public static final int WAITING = 2;
    public static final int DANCING = 3;
    public static final int DISAPPOINTED = 4;
    public static final int SLEEPING = 5;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Enchoer.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> SINGING = SynchedEntityData.defineId(Enchoer.class, EntityDataSerializers.INT);
    private static final byte EVENT_BOW = 100;
    /** CR1: it sounded a note - its cones pump, its drill kicks round and a ring flies out of its snout. */
    private static final byte EVENT_PULSE = -89;
    private static final int INSPECT_TIME = 60;
    private static final int WAIT_TIME = 1200;
    private static final int DANCE_TIME = 110;
    private static final int SAD_TIME = 70;

    public final AnimationState bowAnimation = new AnimationState();
    /** Client-side smoothed weights of each pose (0..1), for the model. */
    public float inspectO, inspect, waitO, wait, danceO, dance, sadO, sad, sleepO, sleep, singO, sing;
    /** Client: the wingbeat (radians, one beat per turn) and how hard it is beating. */
    public float flapO, flap, beatO, beat;
    /** Client: the drill's spin (radians) and its speed. */
    public float drillO, drill;
    private float drillSpeed;
    /** Client: how fast it flies (blocks per tick, smoothed), for its lean. */
    public float flySpeed;
    /** Client: the speaker cones, kicked on every note. */
    public final Spring pump = new Spring(0.45F, 0.3F);
    private int pingCooldown = 40;

    private ItemStack offering = ItemStack.EMPTY;
    private @Nullable ItemEntity target;
    private @Nullable Player giver;
    private int stateTime;
    private int cooldown;
    private int greetCooldown;
    private int walkTime;

    public Enchoer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 12, true);
        // it hovers where it is set down until it roosts
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.45)
                .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    /** Registers the Offering song (once, at start-up). */
    public static void listen() {
        SongEvents.listenSongs((level, player, at, song) -> {
            if (song != Song.OFFERING) {
                return;
            }
            for (Enchoer e : level.getEntitiesOfClass(Enchoer.class, new AABB(at, at).inflate(16.0))) {
                if (e.getState() == WAITING) {
                    e.giver = player;
                    e.setState(DANCING);
                    e.playSound(ModSounds.ENCHOER_YES.get(), 1.2F, 1.0F);
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
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.2) {
            @Override
            public boolean canUse() {
                return Enchoer.this.getState() != SLEEPING && super.canUse();
            }
        });
        // it flutters about a few blocks over the ground
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 0.7) {
            @Override
            public boolean canUse() {
                return Enchoer.this.isFree() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return Enchoer.this.isFree() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 10.0F) {
            @Override
            public boolean canUse() {
                return Enchoer.this.getState() != SLEEPING && super.canUse();
            }
        });
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this) {
            @Override
            public boolean canUse() {
                return Enchoer.this.isFree() && super.canUse();
            }
        });
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(STATE, IDLE);
        builder.define(SINGING, 0);
    }

    public int getState() {
        return this.entityData.get(STATE);
    }

    private void setState(int s) {
        this.entityData.set(STATE, s);
        this.stateTime = 0;
        if (s != IDLE) {
            this.getNavigation().stop();
        }
        // it roosts on the ground, and takes to the air again when it wakes
        this.setNoGravity(s != SLEEPING);
    }

    /**
     * Takes an offering at once and waits for the song (what inspecting a dropped offering ends
     * in; used by the CI song test).
     */
    public void acceptOffering(ItemStack stack) {
        this.offering = stack.copyWithCount(1);
        this.target = null;
        this.walkTime = 0;
        this.setState(WAITING);
    }

    /** Free to wander: no ceremony under way and awake. */
    private boolean isFree() {
        return this.getState() == IDLE;
    }

    public boolean isSinging() {
        return this.entityData.get(SINGING) > 0;
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (!this.level().isClientSide()) {
            if (this.getState() == SLEEPING) {
                this.setState(IDLE);
            }
            if (!this.isSinging()) {
                this.playSound(ModSounds.ENCHOER_HUM.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            }
            this.entityData.set(SINGING, 80);
        }
    }

    /** In a band: every note it plays goes through its speakers - the cones pump and the drill sings. */
    @Override
    public void playedBandNote(int pitch, float loudness) {
        if (this.level() instanceof ServerLevel server) {
            this.entityData.set(SINGING, Math.max(this.entityData.get(SINGING), 30));
            server.broadcastEntityEvent(this, EVENT_PULSE);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // a flier never takes a fall
        this.resetFallDistance();
        if (this.level() instanceof ServerLevel server) {
            this.serverStep(server);
        } else {
            this.clientStep();
        }
    }

    private void serverStep(ServerLevel server) {
        if (this.getState() != SLEEPING) {
            if (this.onGround() && this.isFree() && this.random.nextInt(20) == 0) {
                // set down on the ground and awake: it takes off again
                this.getMoveControl().setWantedPosition(this.getX(), this.getY() + 1.5, this.getZ(), 0.6);
            }
            if (this.getNavigation().isDone() && Bands.leaderOf(this) == null) {
                // a hover, not a drift
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x * 0.92, v.y * 0.8, v.z * 0.92);
            }
        }
        this.stateTime++;
        this.cooldown = Math.max(0, this.cooldown - 1);
        this.greetCooldown = Math.max(0, this.greetCooldown - 1);
        int s = this.entityData.get(SINGING);
        if (s > 0) {
            this.entityData.set(SINGING, s - 1);
            if (s % 12 == 0) {
                server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getEyeY() + 0.6, this.getZ(), 0, this.random.nextDouble(), 0, 0, 1);
            }
        }
        switch (this.getState()) {
            case IDLE -> this.idle(server);
            case INSPECT -> this.inspecting(server);
            case WAITING -> this.waiting(server);
            case DANCING -> this.dancing(server);
            case DISAPPOINTED -> {
                if (this.stateTime == 10) {
                    this.playSound(ModSounds.ENCHOER_NO.get(), 1.0F, 0.8F);
                    this.giveBack(server);
                }
                if (this.stateTime >= SAD_TIME) {
                    this.cooldown = 200;
                    this.setState(IDLE);
                }
            }
            case SLEEPING -> {
                if (this.stateTime % 50 == 0) {
                    server.sendParticles(ModParticles.DRIFTING_SOUL.get(), this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
                }
                // S1 never freeze: it wakes as you walk up (not only once you stand right beside it); sneak to get close
                Player near = server.getNearestPlayer(this, 7.0);
                if (this.stateTime > 1200 + this.random.nextInt(400) || (near != null && !near.isShiftKeyDown()) || this.hurtTime > 0) {
                    this.setState(IDLE);
                    this.cooldown = 600;
                }
            }
            default -> this.setState(IDLE);
        }
    }

    private void idle(ServerLevel server) {
        if (this.cooldown > 0 || this.tickCount % 10 != 0) {
            return;
        }
        // an offering on the ground nearby?
        List<ItemEntity> items = server.getEntitiesOfClass(ItemEntity.class, this.getBoundingBox().inflate(6.0, 3.0, 6.0),
                e -> e.isAlive() && e.onGround() && e.getItem().is(ModEchoer.ECHOER_OFFERINGS));
        if (!items.isEmpty()) {
            this.target = items.get(0);
            this.setState(INSPECT);
            this.playSound(ModSounds.ENCHOER_AMBIENT.get(), 1.0F, 1.2F);
            return;
        }
        Player near = server.getNearestPlayer(this, 5.0);
        if (near != null && this.greetCooldown == 0 && !near.isSpectator()) {
            // a polite bow to a visitor
            this.getLookControl().setLookAt(near);
            server.broadcastEntityEvent(this, EVENT_BOW);
            this.playSound(ModSounds.ENCHOER_AMBIENT.get(), 0.8F, 1.0F);
            this.greetCooldown = 2400;
        } else if (near == null && server.getNearestPlayer(this, 12.0) == null && this.random.nextInt(server.isDarkOutside() ? 20 : 160) == 0) {
            this.setState(SLEEPING);
        }
    }

    private void inspecting(ServerLevel server) {
        ItemEntity it = this.target;
        if (it == null || !it.isAlive() || this.walkTime > 300) {
            this.cooldown = this.walkTime > 300 ? 400 : 0;
            this.walkTime = 0;
            this.target = null;
            this.setState(IDLE);
            return;
        }
        double d = this.distanceToSqr(it);
        if (d > 2.6 * 2.6) {
            if (this.walkTime++ % 10 == 0) {
                // fly to just above the gift
                this.getNavigation().moveTo(it.getX(), it.getY() + 1.2, it.getZ(), 0.7);
            }
            this.stateTime = 0;
            return;
        }
        // hovering low over it, pinging it with its drill
        this.getNavigation().stop();
        this.getLookControl().setLookAt(it.getX(), it.getY(), it.getZ());
        if (this.stateTime % 12 == 0) {
            server.playSound(null, it.getX(), it.getY(), it.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.NEUTRAL, 0.6F, 1.6F + this.random.nextFloat() * 0.3F);
            server.sendParticles(ParticleTypes.ENCHANT, it.getX(), it.getY() + 0.3, it.getZ(), 6, 0.2, 0.2, 0.2, 0.3);
        }
        if (this.stateTime >= INSPECT_TIME) {
            // it accepts: the offering is tucked away into its cabinet
            ItemStack stack = it.getItem();
            this.offering = stack.split(1);
            if (stack.isEmpty()) {
                it.discard();
            } else {
                it.setItem(stack);
            }
            this.target = null;
            this.walkTime = 0;
            this.setState(WAITING);
            this.playSound(ModSounds.ENCHOER_YES.get(), 1.0F, 1.1F);
            server.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, 1.6, 0.0, 0.0, 1.0);
            for (Player p : server.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(10.0))) {
                p.sendOverlayMessage(Component.translatable("message.thesift.echoer.waiting"));
            }
        }
    }

    private void waiting(ServerLevel server) {
        Player near = server.getNearestPlayer(this, 12.0);
        if (near != null) {
            this.getLookControl().setLookAt(near);
        }
        // every eight seconds it plays the start of the Offering through its speakers, to remind you how it goes
        int t = this.stateTime % 160;
        if (t % 8 == 0 && t / 8 < 3) {
            int pitch = Song.OFFERING.note(t / 8);
            server.playSound(null, this.getX(), this.getEyeY() + 0.3, this.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 1.0F,
                    Notes.soundPitch(pitch));
            server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getEyeY() + 0.5, this.getZ(), 0, pitch / 24.0, 0, 0, 1);
            server.broadcastEntityEvent(this, EVENT_PULSE);
        }
        if (this.stateTime % 6 == 0) {
            server.sendParticles(ParticleTypes.ENCHANT, this.getX(), this.getY() + 1.0, this.getZ(), 2, 0.3, 0.3, 0.3, 0.4);
        }
        if (this.stateTime >= WAIT_TIME) {
            this.setState(DISAPPOINTED);
        }
    }

    private void dancing(ServerLevel server) {
        if (this.stateTime % 5 == 0) {
            server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getRandomX(1.2), this.getY() + 0.8 + this.random.nextDouble() * 1.6,
                    this.getRandomZ(1.2), 0, this.random.nextDouble(), 0, 0, 1);
            server.sendParticles(ModParticles.STAR_SPARKLE.get(), this.getRandomX(1.0), this.getY() + this.random.nextDouble() * 2.0,
                    this.getRandomZ(1.0), 1, 0, 0.05, 0, 0.0);
        }
        if (this.stateTime % 20 == 0) {
            this.playSound(ModSounds.ENCHOER_HUM.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
        }
        // its speakers play the whole Offering back to you as it dances
        if (this.stateTime % 9 == 0 && this.stateTime / 9 < Song.OFFERING.length()) {
            int pitch = Song.OFFERING.note(this.stateTime / 9);
            server.playSound(null, this.getX(), this.getEyeY() + 0.3, this.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.NEUTRAL, 0.9F,
                    Notes.soundPitch(pitch));
            server.broadcastEntityEvent(this, EVENT_PULSE);
        }
        if (this.stateTime >= DANCE_TIME) {
            this.reward(server);
            this.offering = ItemStack.EMPTY;
            server.broadcastEntityEvent(this, EVENT_BOW);
            this.cooldown = 400;
            this.setState(IDLE);
        }
    }

    private void reward(ServerLevel server) {
        Vec3 from = this.getEyePosition();
        Player to = this.giver != null && this.giver.isAlive() ? this.giver : server.getNearestPlayer(this, 16.0);
        this.dropFromGiftLootTable(server, ModEchoer.ECHOER_REWARD, (l, stack) -> toss(server, from, to, stack));
        this.giver = null;
        server.sendParticles(ModParticles.RESONANCE_RING.get(), this.getX(), this.getY() + 0.1, this.getZ(), 0, 3.0, 0.0, 0.0, 1.0);
        server.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, from.x, from.y, from.z, 30, 0.4, 0.4, 0.4, 0.3);
        server.playSound(null, from.x, from.y, from.z, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.NEUTRAL, 1.5F, 1.2F);
    }

    private void giveBack(ServerLevel server) {
        if (!this.offering.isEmpty()) {
            toss(server, this.getEyePosition().add(0, -0.6, 0), server.getNearestPlayer(this, 12.0), this.offering);
            this.offering = ItemStack.EMPTY;
        }
    }

    private static void toss(ServerLevel server, Vec3 from, @Nullable Player to, ItemStack stack) {
        ItemEntity e = new ItemEntity(server, from.x, from.y, from.z, stack);
        if (to != null) {
            Vec3 v = to.position().add(0, 1.0, 0).subtract(from).normalize().scale(0.3);
            e.setDeltaMovement(v.x, v.y + 0.2, v.z);
        }
        e.setDefaultPickUpDelay();
        server.addFreshEntity(e);
    }

    // ------------------------------------------------------------------ the client: wings, drill, cones, echolocation

    private void clientStep() {
        int st = this.getState();
        this.inspectO = this.inspect;
        this.waitO = this.wait;
        this.danceO = this.dance;
        this.sadO = this.sad;
        this.sleepO = this.sleep;
        this.singO = this.sing;
        this.inspect = approach(this.inspect, st == INSPECT && this.getDeltaMovement().horizontalDistanceSqr() < 0.002, 0.08F);
        this.wait = approach(this.wait, st == WAITING, 0.06F);
        this.dance = approach(this.dance, st == DANCING, 0.1F);
        this.sad = approach(this.sad, st == DISAPPOINTED, 0.07F);
        this.sleep = approach(this.sleep, st == SLEEPING, 0.04F);
        this.sing = approach(this.sing, this.isSinging() || st == DANCING, 0.08F);
        // how fast it is flying, from how far it actually moved this tick (a client mob's velocity is not kept up to date)
        double dx = this.getX() - this.xo;
        double dz = this.getZ() - this.zo;
        double speed = Math.min(0.6, Math.sqrt(dx * dx + dz * dz));
        this.flySpeed += ((float) speed - this.flySpeed) * 0.3F;
        // the wingbeat: quicker when it flies, frantic in the dance, slow and droopy when it is sad, folded asleep
        this.flapO = this.flap;
        this.beatO = this.beat;
        float awake = 1.0F - this.sleep;
        float wantBeat = awake * (st == DANCING ? 1.0F : Math.min(1.0F, 0.6F + (float) speed * 3.0F));
        this.beat += (wantBeat - this.beat) * 0.15F;
        float rate = (0.42F + 0.28F * this.beat + 0.15F * this.dance - 0.12F * this.sad) * awake;
        if (this.flap > Mth.TWO_PI * 512.0F) {
            // keep the phase small, so floats stay exact
            this.flap -= Mth.TWO_PI * 512.0F;
            this.flapO -= Mth.TWO_PI * 512.0F;
        }
        int before = Mth.floor(this.flap / Mth.TWO_PI);
        this.flap += rate;
        int after = Mth.floor(this.flap / Mth.TWO_PI);
        if (after != before && (after % 2 == 0 || speed > 0.08) && awake > 0.5F) {
            this.level().playLocalSound(this.getX(), this.getY() + 0.6, this.getZ(), ModEchoer.ENCHOER_FLAP.get(), SoundSource.NEUTRAL,
                    0.12F + Math.min(0.25F, (float) speed * 1.5F), 0.95F + this.random.nextFloat() * 0.15F, false);
        }
        // the drill: it spins up when it sings (with a whirr), idles slowly while it pings, and runs down after
        this.drillO = this.drill;
        float wantSpin = this.sing > 0.3F ? 0.9F : this.inspect > 0.5F ? 0.3F : 0.0F;
        boolean spinningUp = this.drillSpeed < 0.3F && wantSpin >= 0.9F;
        this.drillSpeed += (wantSpin - this.drillSpeed) * (wantSpin > this.drillSpeed ? 0.12F : 0.05F);
        this.drill += this.drillSpeed;
        if (this.drill > Mth.TWO_PI * 512.0F) {
            this.drill -= Mth.TWO_PI * 512.0F;
            this.drillO -= Mth.TWO_PI * 512.0F;
        }
        if (spinningUp && !this.isSilent()) {
            this.level().playLocalSound(this.getX(), this.getEyeY(), this.getZ(), ModEchoer.ENCHOER_DRILL.get(), SoundSource.NEUTRAL, 0.5F,
                    0.9F + this.random.nextFloat() * 0.2F, false);
        }
        // the cones: kicked by every note, and throbbing while it sings
        this.pump.setTarget(this.sing > 0.0F ? Math.max(0.0F, Mth.sin(this.tickCount * 0.9F)) * 0.35F * this.sing : 0.0F);
        this.pump.tick();
        // echolocation: now and then a few rings out of its drill; quick pings at an offering it is looking over
        if (st != SLEEPING && --this.pingCooldown <= 0) {
            boolean looking = st == INSPECT;
            this.pingCooldown = looking ? 14 : 60 + this.random.nextInt(80);
            this.ping(looking ? 2 : 3, looking ? 0.22 : 0.32);
        }
        // the glowing cones shed motes of light
        if (this.random.nextInt(st == DANCING ? 2 : 10) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(0.6), this.getY() + 0.5 + this.random.nextDouble() * 0.5,
                    this.getRandomZ(0.6), 0, 0.01, 0);
        }
    }

    /** Client: a burst of echolocation rings flying out of the drill's tip, along where it looks. */
    private void ping(int rings, double speed) {
        Vec3 look = this.getViewVector(1.0F);
        Vec3 tip = this.getEyePosition().add(look.scale(0.65));
        for (int i = 0; i < rings; i++) {
            double v = speed * (1.0 - i * 0.18);
            this.level().addParticle(ModRings.ECHO_RING.get(), tip.x, tip.y, tip.z, look.x * v, look.y * v, look.z * v);
        }
        if (!this.isSilent()) {
            this.level().playLocalSound(tip.x, tip.y, tip.z, ModEchoer.ENCHOER_CHIMES.get(), SoundSource.NEUTRAL, 0.35F,
                    0.95F + this.random.nextFloat() * 0.2F, false);
        }
    }

    private static float approach(float v, boolean on, float k) {
        return Mth.clamp(v + (on ? k : -k), 0.0F, 1.0F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BOW) {
            this.bowAnimation.start(this.tickCount);
            this.pump.kick(0.4F);
        } else if (id == EVENT_PULSE) {
            // a note through its speakers: every cone pumps, the drill kicks round, a ring flies out
            this.pump.kick(0.7F);
            this.drillSpeed += 0.35F;
            Vec3 look = this.getViewVector(1.0F);
            Vec3 tip = this.getEyePosition().add(look.scale(0.65));
            this.level().addParticle(ModRings.ECHO_RING.get(), tip.x, tip.y, tip.z, look.x * 0.3, look.y * 0.3, look.z * 0.3);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (this.getState() == SLEEPING || this.getState() == WAITING || this.getState() == INSPECT) {
            this.cooldown = 600;
            if (this.getState() == WAITING) {
                this.giveBack(level);
            }
            this.setState(IDLE);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (!this.offering.isEmpty()) {
            this.spawnAtLocation(level, this.offering);
            this.offering = ItemStack.EMPTY;
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (!this.offering.isEmpty()) {
            output.store("Offering", ItemStack.CODEC, this.offering);
        }
        // a ceremony in progress resumes as waiting; inspecting starts over
        int st = this.getState();
        output.putInt("Ceremony", st == INSPECT ? IDLE : st);
        output.putInt("CeremonyTime", this.stateTime);
        output.putInt("Cooldown", this.cooldown);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.offering = input.read("Offering", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        int st = input.getIntOr("Ceremony", IDLE);
        if ((st == WAITING || st == DANCING) && this.offering.isEmpty()) {
            st = IDLE;
        }
        this.entityData.set(STATE, Mth.clamp(st, IDLE, SLEEPING));
        this.setNoGravity(st != SLEEPING);
        this.stateTime = input.getIntOr("CeremonyTime", 0);
        this.cooldown = input.getIntOr("Cooldown", 0);
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        int st = this.getState();
        return st == SLEEPING ? null : st == WAITING ? ModSounds.ENCHOER_TRADE.get() : ModSounds.ENCHOER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENCHOER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENCHOER_DEATH.get();
    }

    /** Pearl-white motes and a last little hum. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xE9E3F3, 0x7FF7FF, KillBurst.NOTE, ModParticles.DRIFTING_SOUL.get());
    }
}
