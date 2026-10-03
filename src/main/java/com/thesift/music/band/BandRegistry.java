package com.thesift.music.band;

import com.thesift.TheSift;
import com.thesift.music.Instrument;
import com.thesift.music.Song;
import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import org.jspecify.annotations.Nullable;

/**
 * The band registry: every creature that can play in a player's band, with its own instrument and
 * the songs that call it. Music is the core of the Sift - <b>register every creature you add or
 * remake here</b>, so it has a voice and can join the band.
 *
 * <h2>How a band works</h2>
 * <ul>
 *   <li>When a player completes a {@link Song} (see {@code SongTracker}), every registered creature
 *   within {@link Bands#RECRUIT_RADIUS} (its owner's songs reach {@link Bands#OWNED_RECRUIT_RADIUS})
 *   that {@linkplain BandVoice#likes likes} the song - it is one of its songs, or it was played on
 *   one of its instrument families - may join that player's band, depending on its
 *   {@link BandVoice.Temper}: eager ones join at once, shy ones the second time, loyal ones only for
 *   their owner, hostile ones never ({@code Enemy} creatures and anything hunting never join, whatever
 *   they registered). Owned and tamed creatures always join their owner at once.</li>
 *   <li>Members gather behind the player and follow (a goal injected into the creature - no code in
 *   the creature's class), play every note the player plays in their own voice (tuned to it,
 *   transposed into their register), bob and sway to the music, and play a rolled chord when a song
 *   is completed. The player sees them on a small band HUD.</li>
 *   <li>They leave {@link Bands#STAY_TICKS} after the last song that called them (owned ones later),
 *   when the player goes further than {@link Bands#LEAVE_DISTANCE}, changes dimension or dies, or when
 *   they start a fight. At most {@link Bands#MAX_MEMBERS} play at once.</li>
 *   <li>A bigger band rings louder: {@link Bands#power} (1 alone, +{@link Bands#POWER_PER_MEMBER}
 *   per member) scales song effects - the Lullaby and the Whale Song use it; any
 *   {@code SongEvents.listenSongs} listener may.</li>
 * </ul>
 *
 * <h2>Registering a creature</h2>
 * Call from anywhere before the server starts - the mod constructor, a {@code register(IEventBus)}
 * method, a static initialiser. Entity types may be passed as their {@code DeferredHolder} (they are
 * looked up the first time a band needs them). Sounds are vanilla sound events
 * ({@code SoundEvents.NOTE_BLOCK_*} holders or plain {@code SoundEvent}s), tuned per note: a
 * note-block sound plays exactly in tune, anything else keeps the melody's shape.
 * <pre>{@code
 * // the short form: type, sound, the songs that call it, the instrument families that call it
 * BandRegistry.register(ModThings.GLIMMER, SoundEvents.NOTE_BLOCK_BELL, Set.of(Song.LULLABY), Set.of(Instrument.Family.CHIMES));
 *
 * // the builder, for everything else
 * BandRegistry.voice(ModThings.GLIMMER, SoundEvents.NOTE_BLOCK_BELL)
 *         .families(Instrument.Family.CHIMES).songs(Song.LULLABY)
 *         .transpose(12)                                // an octave up (notes fold back into range)
 *         .layer(SoundEvents.AMETHYST_BLOCK_CHIME, 0.3F) // a second, quieter sound on every note
 *         .every(2)                                     // plays along with every second note only
 *         .temper(BandVoice.Temper.SHY)                 // EAGER (default), SHY, LOYAL or HOSTILE
 *         .movement(BandVoice.Movement.FLY)             // WALK (default), SWIM or FLY
 *         .when(m -> !((Glimmer) m).isAsleep())         // free to play right now?
 *         .bond((m, player) -> ...)                     // ownership kept outside vanilla taming
 *         .instrument("glimmer_bells").colour(0xFFD27A) // HUD name key band.thesift.instrument.<id>, colour
 *         .register();
 * }</pre>
 * Give the instrument a name in the lang file ({@code band.thesift.instrument.<id>}; the id defaults
 * to the last part of the sound's name, e.g. {@code pling}). The creature also needs nothing else:
 * no goal, no event and no renderer code.
 */
public final class BandRegistry {
    private static final List<BandVoice.Builder> PENDING = new ArrayList<>();
    private static final Map<EntityType<?>, BandVoice> VOICES = new IdentityHashMap<>();
    private static boolean defaults;

    private BandRegistry() {
    }

    /** Starts describing a creature's voice; finish with {@link BandVoice.Builder#register()}. */
    public static BandVoice.Builder voice(Supplier<? extends EntityType<?>> type, Holder<SoundEvent> sound) {
        return new BandVoice.Builder(type, sound::value);
    }

    /** Starts describing a creature's voice with a plain sound event. */
    public static BandVoice.Builder voice(Supplier<? extends EntityType<?>> type, SoundEvent sound) {
        return new BandVoice.Builder(type, () -> sound);
    }

    /** Starts describing the voice of an entity type that is already registered. */
    public static BandVoice.Builder voice(EntityType<?> type, Holder<SoundEvent> sound) {
        return new BandVoice.Builder(() -> type, sound::value);
    }

    /**
     * Registers a creature: it plays {@code sound} (tuned to every note), and joins a band when a
     * song from {@code songs}, or one played on an instrument of {@code families}, is completed near it.
     * Eager, walking, unison - use {@link #voice} for anything else.
     */
    public static void register(Supplier<? extends EntityType<?>> type, Holder<SoundEvent> sound, Collection<Song> songs,
            Collection<Instrument.Family> families) {
        voice(type, sound).songs(songs).families(families).register();
    }

    /** {@link #register(Supplier, Holder, Collection, Collection)} for an entity type that is already registered. */
    public static void register(EntityType<?> type, Holder<SoundEvent> sound, Collection<Song> songs, Collection<Instrument.Family> families) {
        voice(type, sound).songs(songs).families(families).register();
    }

    static synchronized void add(BandVoice.Builder builder) {
        PENDING.add(builder);
    }

    /** The voice of this creature, or null if it has none (it cannot join a band). */
    public static @Nullable BandVoice voiceOf(EntityType<?> type) {
        resolve();
        return VOICES.get(type);
    }

    public static @Nullable BandVoice voiceOf(Entity entity) {
        return voiceOf(entity.getType());
    }

    /** Every registered voice. */
    public static synchronized List<BandVoice> voices() {
        resolve();
        return List.copyOf(VOICES.values());
    }

    /**
     * Builds the voices registered so far (their entity types must exist by now). The Sift's own
     * voices ({@link BandVoices}) go first, so any voice registered elsewhere replaces its default.
     */
    private static synchronized void resolve() {
        if (!defaults) {
            defaults = true;
            List<BandVoice.Builder> explicit = new ArrayList<>(PENDING);
            PENDING.clear();
            BandVoices.registerSiftCreatures();
            PENDING.addAll(explicit);
        }
        if (PENDING.isEmpty()) {
            return;
        }
        for (BandVoice.Builder b : PENDING) {
            try {
                BandVoice v = b.build();
                VOICES.put(v.type(), v);
            } catch (RuntimeException e) {
                TheSift.LOGGER.error("Band: could not register a voice", e);
            }
        }
        PENDING.clear();
    }
}
