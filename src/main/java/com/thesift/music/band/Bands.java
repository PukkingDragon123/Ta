package com.thesift.music.band;

import com.thesift.music.Instrument;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModParticles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

/**
 * The players' bands (server side): who joins when a song is completed, playing along with every
 * note, the chord at the end of each song, and who leaves when. See {@link BandRegistry} for how
 * it all fits together and how to give a creature a voice.
 *
 * <p>Song listeners can make their effect grow with the band: {@link #power(Player)} is 1 for a
 * player alone and {@link #POWER_PER_MEMBER} more for every creature playing with them.
 */
public final class Bands {
    /** The most creatures that play in one band. */
    public static final int MAX_MEMBERS = 8;
    /** How near a creature must be to a completed song to join. */
    public static final double RECRUIT_RADIUS = 16.0;
    /** How near its owner's song an owned or tamed creature must be. */
    public static final double OWNED_RECRUIT_RADIUS = 32.0;
    /** Members leave when the player gets further away than this (owned ones: {@link #OWNED_LEAVE_DISTANCE}). */
    public static final double LEAVE_DISTANCE = 40.0;
    public static final double OWNED_LEAVE_DISTANCE = 64.0;
    /** How long a member stays after the last song that called it (owned ones: {@link #OWNED_STAY_TICKS}). */
    public static final int STAY_TICKS = 20 * 120;
    public static final int OWNED_STAY_TICKS = 20 * 300;
    /** Every note the player plays keeps the band together at least this much longer. */
    public static final int NOTE_KEEPS = 20 * 30;
    /** How long a shy creature remembers the first song it heard from a player. */
    public static final int WARM_TICKS = 20 * 300;
    /** How long a creature refuses to play again after its player hurt it. */
    public static final int SPURN_TICKS = 20 * 120;
    /** How much stronger songs get for every member (see {@link #power}). */
    public static final float POWER_PER_MEMBER = 0.15F;
    /** How far from the note a member may be and still play along. */
    private static final double PLAY_RANGE = 32.0;
    private static final int SYNC_EVERY = 10;

    private static final Map<UUID, Band> BANDS = new LinkedHashMap<>();
    private static final Map<Mob, Band.Member> MEMBERS = new IdentityHashMap<>();
    private static final Map<UUID, Warmth> WARM = new HashMap<>();
    private static final Map<UUID, Long> SPURNED = new HashMap<>();
    private static final List<Cue> CUES = new ArrayList<>();
    private static boolean started;

    /** A shy creature heard its first song from this player at this game time. */
    private record Warmth(UUID player, long at) {
    }

    /** A note a creature plays a few ticks from now (server tick {@code at}). */
    private record Cue(Mob mob, BandVoice voice, int pitch, int at, float volume, @Nullable Band band, boolean accent) {
    }

    private Bands() {
    }

    /** Hooks the bands into the song system and the server (once, from the mod constructor). */
    public static void register(IEventBus modBus) {
        if (started) {
            return;
        }
        started = true;
        modBus.addListener(BandPayloads::register);
        SongEvents.listenNotes(Bands::onNote);
        SongEvents.listenSongs(Bands::onSong);
        NeoForge.EVENT_BUS.addListener(Bands::onServerTick);
        NeoForge.EVENT_BUS.addListener(Bands::onServerStopped);
    }

    // ------------------------------------------------------------------ queries

    /** How strongly this player's songs ring out: 1 alone, {@link #POWER_PER_MEMBER} more per band member. */
    public static float power(@Nullable Player player) {
        Band band = player == null ? null : bandOf(player);
        // F2 Band Table: Reverb on the instrument in hand makes songs reach further still
        return (band == null ? 1.0F : power(band)) * com.thesift.enchant.SiftEnchantEffects.reverb(player);
    }

    private static float power(Band band) {
        int n = 0;
        for (Band.Member m : band.members) {
            if (m.inBand && m.mob.isAlive()) {
                n++;
            }
        }
        return 1.0F + POWER_PER_MEMBER * n;
    }

    /** The creatures playing in this player's band. */
    public static List<Mob> members(Player player) {
        Band band = bandOf(player);
        List<Mob> out = new ArrayList<>();
        if (band != null) {
            for (Band.Member m : band.members) {
                out.add(m.mob);
            }
        }
        return out;
    }

    public static int size(Player player) {
        Band band = bandOf(player);
        return band == null ? 0 : band.members.size();
    }

    /** True if the creature plays in this player's band. */
    public static boolean isMember(Player player, Entity entity) {
        Band.Member m = entity instanceof Mob mob ? MEMBERS.get(mob) : null;
        Band band = bandOf(player);
        return m != null && m.inBand && band != null && band.members.contains(m);
    }

    /** The player whose band this creature plays in, or null. */
    public static @Nullable Player leaderOf(Entity entity) {
        if (entity instanceof Mob mob) {
            for (Band band : BANDS.values()) {
                for (Band.Member m : band.members) {
                    if (m.mob == mob) {
                        return band.leader;
                    }
                }
            }
        }
        return null;
    }

    /** How many notes this player's band has played along with so far. */
    public static int notesPlayed(Player player) {
        Band band = bandOf(player);
        return band == null ? 0 : band.notes;
    }

    /** Everyone in this player's band leaves at once. */
    public static void disband(Player player) {
        Band band = BANDS.remove(player.getUUID());
        if (band == null) {
            return;
        }
        for (Band.Member m : band.members) {
            depart(m, Leave.SILENT);
        }
        band.members.clear();
        sync(band, 0L);
    }

    private static @Nullable Band bandOf(Player player) {
        Band band = BANDS.get(player.getUUID());
        return band != null && band.leader == player ? band : null;
    }

    // ------------------------------------------------------------------ songs and notes

    /** A song was completed: creatures that like it may join, and the band plays a chord. */
    private static void onSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        if (player == null || player.isSpectator() || !player.isAlive()) {
            return;
        }
        Instrument instrument = SongEvents.instrument();
        long now = level.getGameTime();
        Band stale = BANDS.get(player.getUUID());
        if (stale != null && stale.leader != player) {
            disband(stale.leader);
        }
        Band band = bandOf(player);
        List<Mob> near = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(OWNED_RECRUIT_RADIUS),
                m -> m.isAlive() && BandRegistry.voiceOf(m.getType()) != null
                        && !com.thesift.entity.SiftRot.isRotten(m)); // E1: a rotten creature plays no music
        near.sort(Comparator.comparingDouble(m -> m.distanceToSqr(player)));
        List<Band.Member> joined = new ArrayList<>();
        for (Mob mob : near) {
            BandVoice voice = BandRegistry.voiceOf(mob.getType());
            if (voice == null || !voice.likes(song, instrument)) {
                continue;
            }
            Band.Member member = MEMBERS.get(mob);
            if (member != null) {
                if (band != null && band.members.contains(member)) {
                    member.staysUntil = Math.max(member.staysUntil, now + (member.owned ? OWNED_STAY_TICKS : STAY_TICKS));
                }
                continue;
            }
            boolean close = mob.distanceToSqr(player) <= RECRUIT_RADIUS * RECRUIT_RADIUS;
            if (voice.temper() == BandVoice.Temper.HOSTILE || mob instanceof Enemy) {
                if (close) {
                    answer(level, mob, voice, song, mob.getTarget() != null);
                }
                continue;
            }
            BandVoice.Bond bond = voice.bond(mob, player);
            if (bond == BandVoice.Bond.OTHER || bond == BandVoice.Bond.WILD && !close) {
                continue;
            }
            if (!voice.ready(mob) || mob.getTarget() != null || spurned(mob, now)) {
                continue;
            }
            boolean owned = bond == BandVoice.Bond.OWN;
            if (!owned && (voice.temper() == BandVoice.Temper.LOYAL || voice.temper() == BandVoice.Temper.SHY && !warm(mob, player, now))) {
                hum(level, mob, voice, song, player);
                continue;
            }
            if (band == null) {
                band = new Band(player);
                BANDS.put(player.getUUID(), band);
            }
            if (band.members.size() >= MAX_MEMBERS) {
                continue;
            }
            joined.add(join(level, band, mob, voice, owned, now));
        }
        if (band != null && !band.members.isEmpty()) {
            finale(level, band, song, joined);
        }
    }

    /** The player played a note: everyone in their band plays it too, in their own voice. */
    private static void onNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        if (player == null) {
            return;
        }
        Band band = bandOf(player);
        if (band == null || band.members.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        band.lastNote = now;
        int[] ids = new int[band.members.size()];
        int n = 0;
        for (Band.Member m : band.members) {
            if (!m.inBand || m.mob.level() != level || !m.voice.ready(m.mob) || m.mob.distanceToSqr(at) > PLAY_RANGE * PLAY_RANGE) {
                continue;
            }
            m.staysUntil = Math.max(m.staysUntil, now + NOTE_KEEPS);
            if (m.heard++ % m.voice.every() != 0) {
                continue;
            }
            m.voice.play(level, m.mob, pitch, 1.0F, false);
            band.notes++;
            ids[n++] = m.mob.getId();
        }
        if (n > 0) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(player, new BandPayloads.Pulse(Arrays.copyOf(ids, n), pitch));
        }
    }

    // ------------------------------------------------------------------ joining

    private static Band.Member join(ServerLevel level, Band band, Mob mob, BandVoice voice, boolean owned, long now) {
        Band.Member m = new Band.Member(mob, voice, owned, now, now + (owned ? OWNED_STAY_TICKS : STAY_TICKS));
        m.goal = new BandGoal(mob, band, m);
        band.members.add(m);
        band.dirty = true;
        MEMBERS.put(mob, m);
        WARM.remove(mob.getUUID());
        mob.getNavigation().stop();
        mob.goalSelector.addGoal(1, m.goal);
        double w = mob.getBbWidth() * 0.4;
        level.sendParticles(owned ? ParticleTypes.HEART : ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(),
                owned ? 3 : 7, w, 0.25, w, 0.0);
        return m;
    }

    /**
     * The song is complete: the band rolls a chord on its last note, one member after another, each in
     * its own register; newcomers then answer with the song's last three notes.
     */
    private static void finale(ServerLevel level, Band band, Song song, List<Band.Member> joined) {
        int tick = tickCount(level);
        int root = song.note(song.length() - 1);
        int[] chord = {root, root + 4, root + 7, root + 12};
        int i = 0;
        for (Band.Member m : band.members) {
            if (!m.voice.ready(m.mob)) {
                continue;
            }
            cue(m.mob, m.voice, m.voice.pitchFor(chord[i % chord.length]), tick + 3 + i, 1.0F, band, true);
            i++;
        }
        for (Band.Member m : joined) {
            for (int k = 0; k < 3; k++) {
                int note = song.note(Math.max(0, song.length() - 3 + k));
                cue(m.mob, m.voice, m.voice.pitchFor(note), tick + 6 + i + k * 4, 0.9F, band, false);
            }
        }
    }

    /** A shy or loyal creature listens: it turns to the player and hums the song's first note back. */
    private static void hum(ServerLevel level, Mob mob, BandVoice voice, Song song, Player player) {
        mob.getLookControl().setLookAt(player, 30.0F, 30.0F);
        cue(mob, voice, voice.pitchFor(song.note(0)), tickCount(level) + 4, 0.55F, null, false);
    }

    /**
     * A monster hears a song it knows: it answers with the song's last two notes from where it is -
     * or, if it is hunting, jeers with a sour tritone.
     */
    private static void answer(ServerLevel level, Mob mob, BandVoice voice, Song song, boolean jeer) {
        int tick = tickCount(level);
        int last = voice.pitchFor(song.note(song.length() - 1));
        if (jeer) {
            cue(mob, voice, last, tick + 3, 0.9F, null, false);
            cue(mob, voice, last + 6, tick + 5, 0.9F, null, false);
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ(), 2, 0.3, 0.1, 0.3, 0.0);
        } else {
            cue(mob, voice, voice.pitchFor(song.note(song.length() - 2)), tick + 4, 0.7F, null, false);
            cue(mob, voice, last, tick + 8, 0.7F, null, false);
        }
    }

    /** True if this shy creature already heard a song from this player lately; else it remembers this one. */
    private static boolean warm(Mob mob, Player player, long now) {
        Warmth w = WARM.get(mob.getUUID());
        if (w != null && w.player().equals(player.getUUID()) && now >= w.at() && now - w.at() <= WARM_TICKS) {
            WARM.remove(mob.getUUID());
            return true;
        }
        WARM.put(mob.getUUID(), new Warmth(player.getUUID(), now));
        return false;
    }

    private static boolean spurned(Mob mob, long now) {
        Long until = SPURNED.get(mob.getUUID());
        return until != null && now < until;
    }

    // ------------------------------------------------------------------ leaving

    private enum Leave { SILENT, FAREWELL, ANGRY }

    private static void depart(Band.Member m, Leave how) {
        m.inBand = false;
        Mob mob = m.mob;
        MEMBERS.remove(mob);
        if (m.goal != null) {
            mob.goalSelector.removeGoal(m.goal);
        }
        if (!mob.isAlive() || !(mob.level() instanceof ServerLevel level)) {
            return;
        }
        mob.getNavigation().stop();
        switch (how) {
            case FAREWELL -> {
                // a falling fifth to say goodbye
                int tick = tickCount(level);
                cue(mob, m.voice, m.voice.pitchFor(19), tick + 1, 0.7F, null, false);
                cue(mob, m.voice, m.voice.pitchFor(12), tick + 6, 0.6F, null, false);
            }
            case ANGRY -> {
                SPURNED.put(mob.getUUID(), level.getGameTime() + SPURN_TICKS);
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.2, mob.getZ(), 3, 0.3, 0.1, 0.3, 0.0);
            }
            default -> {
            }
        }
    }

    // ------------------------------------------------------------------ ticking

    private static void onServerTick(ServerTickEvent.Post event) {
        if (!CUES.isEmpty()) {
            runCues(event.getServer().getTickCount());
        }
        if (BANDS.isEmpty()) {
            return;
        }
        for (Iterator<Band> it = BANDS.values().iterator(); it.hasNext(); ) {
            Band band = it.next();
            if (!tick(band)) {
                it.remove();
            }
        }
        if (event.getServer().getTickCount() % 1200 == 0) {
            long now = event.getServer().overworld().getGameTime();
            WARM.values().removeIf(w -> now - w.at() > WARM_TICKS);
            SPURNED.values().removeIf(until -> now >= until);
        }
    }

    /** One tick of a band; false once it has broken up. */
    private static boolean tick(Band band) {
        Player leader = band.leader;
        boolean gone = leader.isRemoved() || !leader.isAlive() || leader.isSpectator();
        long now = leader.level().getGameTime();
        band.follow();
        for (Iterator<Band.Member> it = band.members.iterator(); it.hasNext(); ) {
            Band.Member m = it.next();
            Mob mob = m.mob;
            Leave how = null;
            if (gone || !mob.isAlive() || mob.isRemoved() || mob.level() != leader.level()) {
                how = Leave.SILENT;
            } else if (mob.getTarget() == leader || mob.getLastHurtByMob() == leader && mob.tickCount - mob.getLastHurtByMobTimestamp() < 40) {
                how = Leave.ANGRY;
            } else if (mob.getTarget() != null) {
                // off to a fight of its own
                how = Leave.SILENT;
            } else {
                double far = m.owned ? OWNED_LEAVE_DISTANCE : LEAVE_DISTANCE;
                if (mob.distanceToSqr(leader) > far * far || now > m.staysUntil) {
                    how = Leave.FAREWELL;
                }
            }
            if (how != null) {
                it.remove();
                depart(m, how);
                band.dirty = true;
            }
        }
        if (band.members.isEmpty()) {
            sync(band, now);
            return false;
        }
        if (band.dirty || now % SYNC_EVERY == 0) {
            band.dirty = false;
            sync(band, now);
        }
        return true;
    }

    private static void sync(Band band, long now) {
        int n = band.members.size();
        int[] ids = new int[n];
        int[] colours = new int[n];
        int[] stay = new int[n];
        for (int i = 0; i < n; i++) {
            Band.Member m = band.members.get(i);
            ids[i] = m.mob.getId();
            colours[i] = m.voice.colour();
            stay[i] = Math.round(m.stayLeft(now) * 255.0F);
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(band.leader, new BandPayloads.Sync(band.leader.getId(), power(band), ids, colours, stay));
    }

    private static int tickCount(ServerLevel level) {
        return level.getServer().getTickCount();
    }

    private static void cue(Mob mob, BandVoice voice, int pitch, int at, float volume, @Nullable Band band, boolean accent) {
        if (CUES.size() < 512) {
            CUES.add(new Cue(mob, voice, BandVoice.fold(pitch), at, volume, band, accent));
        }
    }

    private static void runCues(int tick) {
        List<Cue> due = new ArrayList<>();
        for (Iterator<Cue> it = CUES.iterator(); it.hasNext(); ) {
            Cue c = it.next();
            if (tick >= c.at() || c.at() - tick > 200) {
                it.remove();
                due.add(c);
            }
        }
        for (Cue c : due) {
            Mob mob = c.mob();
            if (!mob.isAlive() || !(mob.level() instanceof ServerLevel level)) {
                continue;
            }
            c.voice().play(level, mob, c.pitch(), c.volume(), true);
            if (c.accent()) {
                double w = mob.getBbWidth() * 0.5;
                level.sendParticles(ModParticles.SIFT_NOTE.get(), mob.getX(), mob.getY() + mob.getBbHeight() + 0.5, mob.getZ(), 0, c.pitch() / 24.0, 0.0, 0.0, 1.0);
                level.sendParticles(ModParticles.STAR_SPARKLE.get(), mob.getX(), mob.getY() + mob.getBbHeight() * 0.6, mob.getZ(), 4, w, 0.3, w, 0.02);
            }
            Band band = c.band();
            if (band != null && MEMBERS.containsKey(mob)) {
                PacketDistributor.sendToPlayersTrackingEntityAndSelf(band.leader, new BandPayloads.Pulse(new int[]{mob.getId()}, c.pitch()));
            }
        }
    }

    private static void onServerStopped(ServerStoppedEvent event) {
        BANDS.clear();
        MEMBERS.clear();
        WARM.clear();
        SPURNED.clear();
        CUES.clear();
    }
}
