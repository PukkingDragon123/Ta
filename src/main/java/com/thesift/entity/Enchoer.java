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
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
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
 * M3: the Echoer (entity id {@code enchoer}), a deer spirit of the Sift - a slender teal deer with a pale
 * bone mask and great palmate antlers whose tines glow with song (model and texture in tools/echoer.py).
 *
 * <ul>
 *   <li>It sings all the time: a slow phrase of a few notes every few seconds, heard from far off (its
 *   voice carries 48 blocks), softer and lower in its sleep. It skips through the air as if over a
 *   meadow, a glint of light under every hoof.</li>
 *   <li>Play near it and it stops singing, turns to you and listens. Finish a song (any Sift song, within
 *   24 blocks) and the nearest Echoer comes to you, sets down in front of you and bows its antlers - and a
 *   gift rises out of them for you to take. Each song has gifts of its own
 *   ({@code thesift:gameplay/echoer_gift/<song>}), and now and then something truly rare comes with it
 *   ({@code thesift:gameplay/echoer_gift/rare}). It gives one gift per player every
 *   {@link #GIFT_COOLDOWN} ticks, from any Echoer (the time is kept on the player, so it cannot be farmed);
 *   until then it dances to your song but has nothing for you.</li>
 *   <li>It nods to visitors, roosts on the ground at night when nobody is about, and plays in a band.</li>
 * </ul>
 */
public class Enchoer extends PathfinderMob implements MusicListener, BandPlayer {
    public static final int IDLE = 0;
    public static final int LISTENING = 1;
    /** Coming to the player and bowing; the gift rises out of its antlers halfway through the bow. */
    public static final int GIFTING = 2;
    public static final int DANCING = 3;
    /** Head low, holding the gift up between its antlers until it is taken. */
    public static final int PRESENTING = 4;
    public static final int SLEEPING = 5;

    /** One gift per player this often (ticks, ten minutes), whichever Echoer gives it. */
    public static final int GIFT_COOLDOWN = 12000;

    private static final EntityDataAccessor<Integer> STATE = SynchedEntityData.defineId(Enchoer.class, EntityDataSerializers.INT);
    private static final byte EVENT_BOW = 100;
    private static final byte EVENT_NOD = 102;
    /** It sang a note: its mouth opens, its antlers flare. */
    private static final byte EVENT_NOTE = -89;
    private static final int APPROACH_TIME = 160;
    private static final int LAND_TIME = 30;
    /** The bow lasts this long (ticks; the model's bow is 3 s); the gift rises at GIFT_AT. */
    private static final int BOW_TIME = 60;
    private static final int GIFT_AT = 28;
    private static final int PRESENT_TIME = 400;
    private static final int DANCE_TIME = 80;
    private static final int LISTEN_TIME = 100;
    private static final double SONG_RANGE = 24.0;
    private static final double NOTE_RANGE = 16.0;
    /** Its voice: 3 x vanilla's 16-block reach. */
    private static final float VOICE = 3.0F;
    /** It sings in a pentatonic scale (note-block pitches). */
    private static final int[] SCALE = {6, 8, 10, 13, 15, 18, 20, 22};
    /** Radians of the skipping gait per unit of limb swing (EnchoerModel uses the same). */
    public static final float SKIP_RATE = 0.42F;

    public final AnimationState bowAnimation = new AnimationState();
    public final AnimationState nodAnimation = new AnimationState();
    /** Client: smoothed amounts (0..1) of its poses, for the model. */
    public float airO, air, danceO, dance, listenO, listen, sleepO, sleep, presentO, present;
    /** Client: every note it sings opens its mouth and lights its antlers. */
    public final Spring voice = new Spring(0.3F, 0.35F);
    private int skipCount;

    private final List<ItemStack> gifts = new ArrayList<>();
    private final List<ItemEntity> raised = new ArrayList<>();
    private @Nullable UUID giftee;
    /** The player it gives to (a fake player in the song test is not in the level's player list). */
    private @Nullable Player giftPlayer;
    private @Nullable UUID listenTo;
    private @Nullable Song heard;
    private int stateTime;
    private int bowTime = -1;
    private int lastNote;
    private int greetCooldown = 100;
    private int[] phrase = new int[0];
    private int phrasePos;
    private int noteTimer;
    private int phraseCooldown = 20;

    public Enchoer(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 12, true);
        // it hovers where it is set down until it lands
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.3).add(Attributes.FLYING_SPEED, 0.36)
                .add(Attributes.FOLLOW_RANGE, 24.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    /** The music it answers (registered once, at start-up). */
    public static void listen() {
        SongEvents.listenSongs((level, player, at, song) -> {
            List<Enchoer> near = level.getEntitiesOfClass(Enchoer.class, new AABB(at, at).inflate(SONG_RANGE), Enchoer::isAlive);
            Enchoer nearest = null;
            for (Enchoer e : near) {
                if (nearest == null || e.distanceToSqr(at) < nearest.distanceToSqr(at)) {
                    nearest = e;
                }
            }
            // the nearest Echoer answers the song; any others dance to it
            for (Enchoer e : near) {
                e.heardSong(level, player, song, e == nearest);
            }
        });
        SongEvents.listenNotes((level, player, at, pitch) -> {
            if (player != null) {
                for (Enchoer e : level.getEntitiesOfClass(Enchoer.class, new AABB(at, at).inflate(NOTE_RANGE), Enchoer::isAlive)) {
                    e.heardNote(player);
                }
            }
        });
    }

    /** Whether {@code player} may be given a gift now (one every {@link #GIFT_COOLDOWN} ticks). */
    public static boolean canReceiveGift(ServerLevel level, Player player) {
        return level.getGameTime() >= player.getData(ModEchoer.NEXT_GIFT.get());
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
                return Enchoer.this.isFree() && super.canUse();
            }
        });
        // it skips about the meadows a little over the ground
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
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 12.0F) {
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
        if (s != GIFTING) {
            this.bowTime = -1;
        }
        // it lands to roost and to bow; anything else it does in the air
        this.setNoGravity(s != SLEEPING && s != GIFTING && s != PRESENTING);
    }

    /** Free to wander: awake and not busy with you. */
    private boolean isFree() {
        return this.getState() == IDLE;
    }

    /** The gifts it has promised and not yet raised (the song test reads them). */
    public List<ItemStack> pendingGifts() {
        return this.gifts;
    }

    private void heardSong(ServerLevel level, @Nullable Player player, Song song, boolean answer) {
        int st = this.getState();
        if (st == GIFTING || st == PRESENTING) {
            return;
        }
        this.heard = song;
        if (answer && player != null && !player.isSpectator()) {
            if (canReceiveGift(level, player)) {
                // the gift is chosen now - the song's own gifts, now and then something rare - and promised to you
                this.gifts.clear();
                this.dropFromGiftLootTable(level, ModEchoer.giftTable(song), (l, stack) -> this.gifts.add(stack));
                if (!this.gifts.isEmpty()) {
                    player.setData(ModEchoer.NEXT_GIFT.get(), level.getGameTime() + GIFT_COOLDOWN);
                    this.giftee = player.getUUID();
                    this.giftPlayer = player;
                    this.setState(GIFTING);
                    this.playSound(ModSounds.ENCHOER_YES.get(), 1.5F, 1.0F);
                    return;
                }
            } else {
                player.sendOverlayMessage(Component.translatable("message.thesift.echoer.no_gift"));
            }
        }
        this.setState(DANCING);
    }

    private void heardNote(Player player) {
        int st = this.getState();
        if (st == IDLE || st == LISTENING || st == SLEEPING) {
            if (st != LISTENING) {
                this.setState(LISTENING);
            }
            this.listenTo = player.getUUID();
            this.lastNote = this.stateTime;
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (!this.level().isClientSide() && this.getState() == SLEEPING) {
            this.setState(IDLE);
        }
    }

    /** In a band every note it plays goes through its voice: its mouth opens and its antlers flare. */
    @Override
    public void playedBandNote(int pitch, float loudness) {
        if (this.level() instanceof ServerLevel server) {
            server.broadcastEntityEvent(this, EVENT_NOTE);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        // a creature of the air never takes a fall
        this.resetFallDistance();
        if (this.level() instanceof ServerLevel server) {
            this.serverStep(server);
        } else {
            this.clientStep();
        }
    }

    private void serverStep(ServerLevel server) {
        int st = this.getState();
        if (st == IDLE || st == LISTENING || st == DANCING) {
            if (this.onGround() && st == IDLE && this.random.nextInt(80) == 0) {
                // standing in the meadow: now and then it skips up into the air again
                this.getMoveControl().setWantedPosition(this.getX(), this.getY() + 1.5, this.getZ(), 0.6);
            }
            if (this.getNavigation().isDone() && Bands.leaderOf(this) == null) {
                // a hover, not a drift
                Vec3 v = this.getDeltaMovement();
                this.setDeltaMovement(v.x * 0.9, v.y * 0.8, v.z * 0.9);
            }
        }
        this.stateTime++;
        this.greetCooldown = Math.max(0, this.greetCooldown - 1);
        this.sing(server);
        switch (st) {
            case IDLE -> this.idle(server);
            case LISTENING -> this.listening(server);
            case GIFTING -> this.gifting(server);
            case PRESENTING -> this.presenting(server);
            case DANCING -> this.dancing(server);
            case SLEEPING -> this.sleeping(server);
            default -> this.setState(IDLE);
        }
    }

    // ------------------------------------------------------------------ its song

    /** Phrase after phrase: a few notes up and down its scale, a pause, another. */
    private void sing(ServerLevel server) {
        int st = this.getState();
        if (st == LISTENING || st == DANCING || st == GIFTING && this.bowTime >= 0) {
            return; // it listens to you, sings your song back, or holds its breath for the bow
        }
        boolean sleepy = st == SLEEPING;
        if (this.phrasePos < this.phrase.length) {
            if (--this.noteTimer <= 0) {
                this.singNote(server, this.phrase[this.phrasePos++], sleepy ? 0.3F : 1.0F);
                this.noteTimer = sleepy ? 10 : 5 + this.random.nextInt(3);
            }
        } else if (--this.phraseCooldown <= 0) {
            int n = sleepy ? 3 : 3 + this.random.nextInt(4);
            this.phrase = new int[n];
            int i = 2 + this.random.nextInt(4);
            for (int k = 0; k < n; k++) {
                this.phrase[k] = Math.max(0, SCALE[i] - (sleepy ? 6 : 0));
                // a phrase wanders a step or two at a time and settles down at its end
                i = Mth.clamp(i + (k == n - 2 ? -1 : this.random.nextInt(5) - 2), 0, SCALE.length - 1);
            }
            this.phrasePos = 0;
            this.noteTimer = 0;
            this.phraseCooldown = sleepy ? 160 + this.random.nextInt(120) : 40 + this.random.nextInt(60);
        }
    }

    private void singNote(ServerLevel server, int pitch, float loud) {
        if (!this.isSilent()) {
            server.playSound(null, this.getX(), this.getEyeY(), this.getZ(), ModEchoer.ENCHOER_SING.get(), SoundSource.NEUTRAL, VOICE * loud,
                    Notes.soundPitch(pitch));
        }
        server.broadcastEntityEvent(this, EVENT_NOTE);
        server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getEyeY() + 0.7, this.getZ(), 0, pitch / 24.0, 0, 0, 1);
    }

    // ------------------------------------------------------------------ what it does

    private void idle(ServerLevel server) {
        if (this.tickCount % 10 != 0) {
            return;
        }
        Player near = server.getNearestPlayer(this, 5.0);
        if (near != null && this.greetCooldown == 0 && !near.isSpectator()) {
            // a gentle nod to a visitor
            this.getLookControl().setLookAt(near);
            server.broadcastEntityEvent(this, EVENT_NOD);
            this.playSound(ModSounds.ENCHOER_AMBIENT.get(), 0.8F, 1.0F);
            this.greetCooldown = 2400;
        } else if (near == null && server.getNearestPlayer(this, 12.0) == null && this.random.nextInt(server.isDarkOutside() ? 20 : 160) == 0) {
            this.setState(SLEEPING);
        }
    }

    private void listening(ServerLevel server) {
        Player p = this.listenTo == null ? null : server.getPlayerByUUID(this.listenTo);
        if (p == null || !p.isAlive() || this.stateTime - this.lastNote > LISTEN_TIME) {
            this.setState(IDLE);
            return;
        }
        this.getLookControl().setLookAt(p, 20.0F, 20.0F);
        double d = this.distanceToSqr(p);
        if (d > 8.0 * 8.0 && this.stateTime % 20 == 1) {
            // drawn closer by the music
            Vec3 away = this.position().subtract(p.position()).multiply(1.0, 0.0, 1.0).normalize().scale(4.5);
            this.getNavigation().moveTo(p.getX() + away.x, p.getY() + 0.8, p.getZ() + away.z, 0.8);
        } else if (d < 5.0 * 5.0) {
            this.getNavigation().stop();
        }
    }

    private void gifting(ServerLevel server) {
        Player p = this.giftee == null ? null : this.giftPlayer(server);
        if (p == null || !p.isAlive() || p.distanceToSqr(this) > 48.0 * 48.0) {
            // its friend has gone: the gift is left where it stands, for them alone
            this.leaveGifts(server);
            this.setState(IDLE);
            return;
        }
        this.getLookControl().setLookAt(p, 30.0F, 30.0F);
        if (this.bowTime < 0) {
            double dx = this.getX() - p.getX();
            double dz = this.getZ() - p.getZ();
            double d = Math.sqrt(dx * dx + dz * dz);
            if (d > 3.6 && this.stateTime < APPROACH_TIME) {
                // it skips over to you and sets down a few steps away
                if (this.stateTime % 10 == 1) {
                    double k = d < 1.0E-3 ? 0.0 : 2.6 / d;
                    this.getNavigation().moveTo(p.getX() + dx * k, p.getY() + 0.2, p.getZ() + dz * k, 1.0);
                }
                return;
            }
            this.getNavigation().stop();
            this.setNoGravity(false);
            this.setYBodyRot(this.getYHeadRot());
            if (this.onGround() || this.stateTime > APPROACH_TIME + LAND_TIME || this.stateTime > LAND_TIME && d <= 3.6 && this.landed()) {
                this.bowTime = 0;
                server.broadcastEntityEvent(this, EVENT_BOW);
                this.playSound(ModEchoer.ENCHOER_GIFT.get(), 1.2F, 0.8F);
            }
            return;
        }
        this.bowTime++;
        this.setDeltaMovement(this.getDeltaMovement().multiply(0.0, 1.0, 0.0));
        if (this.bowTime == GIFT_AT) {
            this.raiseGifts(server, p);
        }
        if (this.bowTime >= BOW_TIME) {
            this.setState(PRESENTING);
        }
    }

    private @Nullable Player giftPlayer(ServerLevel server) {
        if (this.giftee == null) {
            return null;
        }
        Player p = server.getPlayerByUUID(this.giftee);
        if (p == null && this.giftPlayer != null && this.giftee.equals(this.giftPlayer.getUUID()) && this.giftPlayer.level() == server) {
            p = this.giftPlayer;
        }
        return p;
    }

    /** Close enough to the ground to bow (it does not wait for ever on a ledge). */
    private boolean landed() {
        return !this.level().noCollision(this, this.getBoundingBox().move(0.0, -0.6, 0.0));
    }

    /** The gift rises out of its lowered antlers and floats there, turning, for the player to take. */
    private void raiseGifts(ServerLevel server, Player p) {
        Vec3 fwd = Vec3.directionFromRotation(0.0F, this.yBodyRot);
        Vec3 at = this.position().add(fwd.x * 1.15, 1.45, fwd.z * 1.15);
        int i = 0;
        for (ItemStack stack : this.gifts) {
            ItemEntity e = new ItemEntity(server, at.x, at.y + i * 0.3, at.z, stack);
            e.setNoGravity(true);
            e.setDeltaMovement(0.0, 0.022, 0.0);
            e.setTarget(p.getUUID());
            e.setPickUpDelay(10);
            e.setExtendedLifetime();
            server.addFreshEntity(e);
            this.raised.add(e);
            i++;
        }
        this.gifts.clear();
        server.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y, at.z, 16, 0.3, 0.3, 0.3, 0.02);
        server.sendParticles(ParticleTypes.END_ROD, at.x, at.y, at.z, 10, 0.2, 0.2, 0.2, 0.03);
        server.sendParticles(ModParticles.RESONANCE_RING.get(), at.x, at.y, at.z, 0, 1.2, 0.0, 0.0, 1.0);
        server.playSound(null, at.x, at.y, at.z, ModEchoer.ENCHOER_GIFT.get(), SoundSource.NEUTRAL, 1.5F, 1.2F);
    }

    private void presenting(ServerLevel server) {
        Player p = this.giftee == null ? null : this.giftPlayer(server);
        if (p != null) {
            this.getLookControl().setLookAt(p, 20.0F, 20.0F);
        }
        this.raised.removeIf(e -> !e.isAlive());
        for (ItemEntity e : this.raised) {
            if (this.stateTime % 4 == 0) {
                server.sendParticles(ModParticles.STAR_SPARKLE.get(), e.getX(), e.getY() + 0.25, e.getZ(), 1, 0.15, 0.15, 0.15, 0.0);
            }
            // it rises a little way, then floats where it is
            if (e.getDeltaMovement().y < 0.002) {
                e.setDeltaMovement(Vec3.ZERO);
            }
        }
        if (this.raised.isEmpty()) {
            // taken: a happy little prance
            this.playSound(ModSounds.ENCHOER_HUM.get(), 1.2F, 1.2F);
            this.giftee = null;
            this.setState(DANCING);
            this.stateTime = DANCE_TIME / 2;
        } else if (this.stateTime > PRESENT_TIME) {
            this.leaveGifts(server);
            this.setState(IDLE);
        }
    }

    /** It moves on: a gift it was holding up drops to the ground (still for its player only). */
    private void leaveGifts(ServerLevel server) {
        for (ItemEntity e : this.raised) {
            e.setNoGravity(false);
        }
        this.raised.clear();
        Player p = this.giftee == null ? null : this.giftPlayer(server);
        for (ItemStack stack : this.gifts) {
            ItemEntity e = new ItemEntity(server, this.getX(), this.getY() + 1.0, this.getZ(), stack);
            if (this.giftee != null) {
                e.setTarget(this.giftee);
            }
            if (p != null) {
                Vec3 v = p.position().subtract(e.position()).normalize().scale(0.25);
                e.setDeltaMovement(v.x, 0.25, v.z);
            }
            e.setExtendedLifetime();
            server.addFreshEntity(e);
        }
        this.gifts.clear();
        this.giftee = null;
        this.giftPlayer = null;
    }

    private void dancing(ServerLevel server) {
        Song s = this.heard;
        // it prances in the air and sings your song back to you
        if (s != null && this.stateTime % 6 == 0 && this.stateTime / 6 < s.length()) {
            this.singNote(server, s.note(this.stateTime / 6), 1.0F);
        }
        if (this.stateTime % 5 == 0) {
            server.sendParticles(ModParticles.STAR_SPARKLE.get(), this.getRandomX(1.0), this.getY() + 1.0 + this.random.nextDouble() * 1.4,
                    this.getRandomZ(1.0), 1, 0, 0.05, 0, 0.0);
        }
        if (this.stateTime >= DANCE_TIME) {
            this.setState(IDLE);
        }
    }

    private void sleeping(ServerLevel server) {
        if (this.stateTime % 50 == 0) {
            server.sendParticles(ModParticles.DRIFTING_SOUL.get(), this.getX(), this.getY() + 1.0, this.getZ(), 1, 0.3, 0.1, 0.3, 0.0);
        }
        // S1 never freeze: it wakes as you walk up (not only once you stand right beside it); sneak to get close
        Player near = server.getNearestPlayer(this, 7.0);
        if (this.stateTime > 1200 + this.random.nextInt(400) || near != null && !near.isShiftKeyDown() || this.hurtTime > 0) {
            this.setState(IDLE);
        }
    }

    // ------------------------------------------------------------------ the client: poses, voice, air steps

    private void clientStep() {
        int st = this.getState();
        this.airO = this.air;
        this.danceO = this.dance;
        this.listenO = this.listen;
        this.sleepO = this.sleep;
        this.presentO = this.present;
        this.air = approach(this.air, !this.onGround() && st != SLEEPING, 0.07F);
        this.dance = approach(this.dance, st == DANCING, 0.08F);
        this.listen = approach(this.listen, st == LISTENING, 0.06F);
        this.sleep = approach(this.sleep, st == SLEEPING, 0.03F);
        this.present = approach(this.present, st == PRESENTING, 0.05F);
        this.voice.setTarget(0.0F);
        this.voice.tick();
        // every skip through the air lands on a glint of light, with a soft chime
        if (this.air > 0.5F && this.walkAnimation.speed() > 0.12F) {
            int skip = Mth.floor(this.walkAnimation.position() * SKIP_RATE / Mth.TWO_PI + 0.25F);
            if (skip != this.skipCount) {
                this.skipCount = skip;
                Vec3 fwd = Vec3.directionFromRotation(0.0F, this.yBodyRot);
                for (int k = -1; k <= 1; k += 2) {
                    this.level().addParticle(ModRings.ECHO_RING.get(), this.getX() + fwd.x * 0.5 * k, this.getY() + 0.02, this.getZ() + fwd.z * 0.5 * k,
                            0.0, -0.02, 0.0);
                }
                if (!this.isSilent()) {
                    this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), ModEchoer.ENCHOER_STEP.get(), SoundSource.NEUTRAL, 0.35F,
                            0.9F + this.random.nextFloat() * 0.25F, false);
                }
            }
        }
        // its antler tips shed motes of light
        if (st != SLEEPING && this.random.nextInt(st == DANCING || st == PRESENTING ? 3 : 14) == 0) {
            this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(0.9), this.getY() + 2.0 + this.random.nextDouble() * 0.4,
                    this.getRandomZ(0.9), 0, 0.01, 0);
        }
    }

    private static float approach(float v, boolean on, float k) {
        return Mth.clamp(v + (on ? k : -k), 0.0F, 1.0F);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == EVENT_BOW) {
            this.bowAnimation.start(this.tickCount);
        } else if (id == EVENT_NOD) {
            this.nodAnimation.start(this.tickCount);
        } else if (id == EVENT_NOTE) {
            this.voice.kick(0.55F);
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        int st = this.getState();
        if (st == GIFTING || st == PRESENTING) {
            this.leaveGifts(level);
        }
        if (st != IDLE) {
            this.setState(IDLE);
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.leaveGifts(level);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        // a promised gift is kept (it is still on its way to you after a reload); everything else starts over
        if (!this.gifts.isEmpty()) {
            output.store("Gifts", ItemStack.CODEC.listOf(), List.copyOf(this.gifts));
            output.storeNullable("Giftee", UUIDUtil.CODEC, this.giftee);
        }
        output.putBoolean("Roosting", this.getState() == SLEEPING);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.gifts.clear();
        this.gifts.addAll(input.read("Gifts", ItemStack.CODEC.listOf()).orElse(List.of()));
        this.giftee = input.read("Giftee", UUIDUtil.CODEC).orElse(null);
        if (!this.gifts.isEmpty() && this.giftee != null) {
            this.setState(GIFTING);
        } else {
            this.setState(input.getBooleanOr("Roosting", false) ? SLEEPING : IDLE);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public int getAmbientSoundInterval() {
        return 400;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.getState() == SLEEPING ? null : ModSounds.ENCHOER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENCHOER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENCHOER_DEATH.get();
    }

    /** Teal and pale-gold motes and a last little note. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0x4CC2B6, 0xF2D27A, KillBurst.NOTE, ModParticles.DRIFTING_SOUL.get());
    }
}
