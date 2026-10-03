package com.thesift.music.band;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import org.jspecify.annotations.Nullable;

/**
 * One creature's seat in a band: the instrument it plays (a vanilla sound, tuned to every note the
 * player plays), the songs and instrument families that call it, and how it behaves while it plays.
 * Made with {@link BandRegistry#voice} (or the shorthand {@link BandRegistry#register}); see
 * {@link BandRegistry} for the whole API.
 */
public final class BandVoice {
    /** How readily a creature joins a band. */
    public enum Temper {
        /** Wild ones join on the first compatible song they hear. */
        EAGER,
        /**
         * Wild ones must hear a compatible song from the same player twice within
         * {@link Bands#WARM_TICKS}: the first only makes them curious (they hum one note back).
         */
        SHY,
        /** Only creatures the player owns or tamed join; wild ones just hum along once. */
        LOYAL,
        /**
         * Never joins. It answers a compatible song from where it stands with a phrase in its own
         * voice - or jeers (a sour tritone) if it is hunting someone.
         */
        HOSTILE
    }

    /** How a band member keeps up with its player. */
    public enum Movement {
        /** Walks (or hops) after the player with its own path navigation. */
        WALK,
        /** Swims after the player with its own navigation (fish stay in their water). */
        SWIM,
        /** Flies: steered straight towards its place above the band (works for custom fliers too). */
        FLY
    }

    /** Whose a creature is, seen from the player who is playing. */
    public enum Bond {
        /** Owned or tamed by this player. */
        OWN,
        /** Owned or tamed by someone else: it will not play for this player. */
        OTHER,
        /** Nobody's. */
        WILD
    }

    private final EntityType<?> type;
    private final Supplier<SoundEvent> sound;
    private final @Nullable Supplier<SoundEvent> layer;
    private final float layerVolume;
    private final float volume;
    private final int transpose;
    private final int every;
    private final EnumSet<Song> songs;
    private final EnumSet<Instrument.Family> families;
    private final Temper temper;
    private final Movement movement;
    private final int colour;
    private final String instrument;
    private final Predicate<Mob> ready;
    private final BiFunction<Mob, Player, Bond> bond;

    private BandVoice(EntityType<?> type, Builder b) {
        this.type = type;
        this.sound = b.sound;
        this.layer = b.layer;
        this.layerVolume = b.layerVolume;
        this.volume = b.volume;
        this.transpose = b.transpose;
        this.every = Math.max(1, b.every);
        this.songs = EnumSet.copyOf(b.songs);
        this.families = EnumSet.copyOf(b.families);
        this.temper = b.temper;
        this.movement = b.movement;
        this.colour = b.colour;
        this.instrument = b.instrument;
        this.ready = b.ready;
        this.bond = b.bond;
    }

    public EntityType<?> type() {
        return this.type;
    }

    public Temper temper() {
        return this.temper;
    }

    public Movement movement() {
        return this.movement;
    }

    /** RGB colour of this voice on the band HUD. */
    public int colour() {
        return this.colour;
    }

    /** The instrument's translation key ({@code band.thesift.instrument.<id>}). */
    public String instrumentKey() {
        return "band.thesift.instrument." + this.instrument;
    }

    /** The instrument's id, readable as a fallback name ("whale_song" reads "whale song"). */
    public String instrumentId() {
        return this.instrument;
    }

    /** It plays along with every {@code every()}-th note (big, slow creatures play every second). */
    public int every() {
        return this.every;
    }

    /** The songs that call it whatever they are played on. */
    public Set<Song> songs() {
        return EnumSet.copyOf(this.songs);
    }

    /** The instrument families whose songs call it. */
    public Set<Instrument.Family> families() {
        return EnumSet.copyOf(this.families);
    }

    /**
     * True if this song, completed on {@code played} (null: not on a hand-held instrument), calls the
     * creature: it is one of its songs, or it was played on one of its instrument families.
     */
    public boolean likes(Song song, @Nullable Instrument played) {
        return this.songs.contains(song) || played != null && this.families.contains(played.family());
    }

    /**
     * True if the creature is free to play right now: not asleep or busy with something of its own,
     * not ridden or riding, not told to sit.
     */
    public boolean ready(Mob mob) {
        return !mob.isVehicle() && !mob.isPassenger() && !(mob instanceof TamableAnimal t && t.isOrderedToSit()) && this.ready.test(mob);
    }

    public Bond bond(Mob mob, Player player) {
        return this.bond.apply(mob, player);
    }

    /** The note this voice plays for the player's {@code note}: transposed, then folded by octaves into 0..24. */
    public int pitchFor(int note) {
        return fold(note + this.transpose);
    }

    /** Folds a pitch by octaves into the playable range 0..24 (sound pitch 0.5..2). */
    public static int fold(int pitch) {
        int p = pitch;
        while (p > Notes.MAX_PITCH) {
            p -= 12;
        }
        while (p < 0) {
            p += 12;
        }
        return p;
    }

    /**
     * The creature plays one note (already in this voice's register when {@code raw} is true,
     * otherwise transposed with {@link #pitchFor}): its sound, its layer, and a note above its head.
     */
    public void play(ServerLevel level, Mob mob, int note, float loudness, boolean raw) {
        int p = raw ? fold(note) : this.pitchFor(note);
        float sp = Notes.soundPitch(p);
        double x = mob.getX();
        double y = mob.getY() + mob.getBbHeight() * 0.85;
        double z = mob.getZ();
        level.playSound(null, x, y, z, this.sound.get(), SoundSource.NEUTRAL, this.volume * loudness, sp);
        if (this.layer != null) {
            level.playSound(null, x, y, z, this.layer.get(), SoundSource.NEUTRAL, this.layerVolume * loudness, sp);
        }
        level.sendParticles(ParticleTypes.NOTE, x + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth() * 0.6,
                mob.getY() + mob.getBbHeight() + 0.35, z + (mob.getRandom().nextDouble() - 0.5) * mob.getBbWidth() * 0.6, 0, p / 24.0, 0.0, 0.0, 1.0);
    }

    /** Vanilla ownership: a tamed {@link TamableAnimal}, or any {@link OwnableEntity} with an owner. */
    static Bond vanillaBond(Mob mob, Player player) {
        if (mob instanceof TamableAnimal t) {
            return !t.isTame() ? Bond.WILD : t.isOwnedBy(player) ? Bond.OWN : Bond.OTHER;
        }
        if (mob instanceof OwnableEntity o && o.getOwnerReference() != null) {
            return o.getOwner() == player ? Bond.OWN : Bond.OTHER;
        }
        return Bond.WILD;
    }

    /**
     * Describes a voice; {@link #register()} adds it to the {@link BandRegistry}. Defaults: no
     * songs or families (call at least one), no layer, volume 1, unison, every note, {@link
     * Temper#EAGER}, {@link Movement#WALK}, a colour from the sound's name, the instrument id from the
     * sound's name (e.g. {@code block.note_block.pling} - "pling"), always ready, vanilla ownership.
     */
    public static final class Builder {
        private final Supplier<? extends EntityType<?>> type;
        private final Supplier<SoundEvent> sound;
        private @Nullable Supplier<SoundEvent> layer;
        private float layerVolume = 0.4F;
        private float volume = 1.0F;
        private int transpose;
        private int every = 1;
        private final EnumSet<Song> songs = EnumSet.noneOf(Song.class);
        private final EnumSet<Instrument.Family> families = EnumSet.noneOf(Instrument.Family.class);
        private Temper temper = Temper.EAGER;
        private Movement movement = Movement.WALK;
        private int colour = -1;
        private @Nullable String instrument;
        private Predicate<Mob> ready = m -> true;
        private BiFunction<Mob, Player, Bond> bond = BandVoice::vanillaBond;

        Builder(Supplier<? extends EntityType<?>> type, Supplier<SoundEvent> sound) {
            this.type = type;
            this.sound = sound;
        }

        /** Songs that call this creature whatever instrument they are played on. */
        public Builder songs(Song... songs) {
            this.songs.addAll(List.of(songs));
            return this;
        }

        public Builder songs(Collection<Song> songs) {
            this.songs.addAll(songs);
            return this;
        }

        /** Instrument families: any song completed on an instrument of one of them calls this creature. */
        public Builder families(Instrument.Family... families) {
            this.families.addAll(List.of(families));
            return this;
        }

        public Builder families(Collection<Instrument.Family> families) {
            this.families.addAll(families);
            return this;
        }

        /** A second sound played with every note (tuned the same), at {@code volume}. */
        public Builder layer(Holder<SoundEvent> sound, float volume) {
            this.layer = sound::value;
            this.layerVolume = volume;
            return this;
        }

        public Builder layer(SoundEvent sound, float volume) {
            this.layer = () -> sound;
            this.layerVolume = volume;
            return this;
        }

        public Builder volume(float volume) {
            this.volume = volume;
            return this;
        }

        /**
         * Semitones added to every note it plays along with: 12 an octave up, -12 down, 7 a fifth
         * above (a harmony). Notes are folded back into the playable two octaves.
         */
        public Builder transpose(int semitones) {
            this.transpose = semitones;
            return this;
        }

        /** Plays along with every n-th note only (1 = every note). */
        public Builder every(int n) {
            this.every = n;
            return this;
        }

        public Builder temper(Temper temper) {
            this.temper = temper;
            return this;
        }

        public Builder movement(Movement movement) {
            this.movement = movement;
            return this;
        }

        /** RGB colour on the band HUD. */
        public Builder colour(int rgb) {
            this.colour = rgb & 0xFFFFFF;
            return this;
        }

        /** The instrument's id: its name is the lang key {@code band.thesift.instrument.<id>}. */
        public Builder instrument(String id) {
            this.instrument = id;
            return this;
        }

        /** When it is free to join and play (e.g. not asleep). Ridden or riding creatures never play. */
        public Builder when(Predicate<Mob> ready) {
            this.ready = ready;
            return this;
        }

        /** Who owns it, for creatures that keep their owner their own way (default: vanilla taming). */
        public Builder bond(BiFunction<Mob, Player, Bond> bond) {
            this.bond = bond;
            return this;
        }

        /** Adds the voice to the registry (replacing an earlier voice of the same creature). */
        public void register() {
            BandRegistry.add(this);
        }

        BandVoice build() {
            EntityType<?> t = this.type.get();
            if (this.instrument == null || this.colour < 0) {
                Identifier key = BuiltInRegistries.SOUND_EVENT.getKey(this.sound.get());
                String path = key == null ? "voice" : key.getPath();
                String id = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
                if (this.instrument == null) {
                    this.instrument = id;
                }
                if (this.colour < 0) {
                    this.colour = Notes.colour(Math.floorMod(id.hashCode(), Notes.MAX_PITCH + 1));
                }
            }
            return new BandVoice(t, this);
        }
    }
}
