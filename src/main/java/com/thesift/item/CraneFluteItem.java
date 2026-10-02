package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Crane Flute, carved from a bone with an amethyst reed and an echo shard.
 *
 * <ul>
 *   <li>hold use: a held note that locks onto the creature you look at - a thin warning line
 *   first, then (after {@link #WARN_TICKS}) a sonic beam that hurts and slows it for as long as
 *   you keep playing and it stays in sight and within {@link #range()} blocks.</li>
 *   <li>sneak + use: one note, its pitch from where you look (up = higher) - play songs with it.</li>
 * </ul>
 *
 * <p>The beam runs on both sides with the same rules: the server deals the damage, the client
 * draws the beam.
 */
public class CraneFluteItem extends Item {
    public static final int WARN_TICKS = 10;
    private static final Map<UUID, Beam> SERVER_BEAMS = new HashMap<>();
    private static final Map<UUID, Beam> CLIENT_BEAMS = new HashMap<>();

    public CraneFluteItem(Item.Properties properties) {
        super(properties);
    }

    // --------------------------------------------------------------- what the prism flute changes

    protected double range() {
        return 24.0;
    }

    /** Damage of each beam pulse (one every 5 ticks). */
    protected float pulseDamage() {
        return 1.5F;
    }

    /** True if the beam runs on through its target, hurting everything along it. */
    protected boolean piercing() {
        return false;
    }

    protected Instrument instrument() {
        return Instrument.FLUTE;
    }

    /** The beam's colour this tick (RGB). */
    protected int beamColour(long time) {
        return this.instrument().colour();
    }

    // --------------------------------------------------------------- use

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            Notes.play(level, player, this.instrument(), Notes.lookPitch(player));
            return InteractionResult.SUCCESS;
        }
        player.startUsingItem(hand);
        if (level instanceof ServerLevel server) {
            server.playSound(null, player.getX(), player.getEyeY(), player.getZ(), ModSounds.CRANE_FLUTE_PLAY.get(), SoundSource.PLAYERS, 1.0F,
                    Notes.soundPitch(Notes.lookPitch(player)));
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.TOOT_HORN;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        (level.isClientSide() ? CLIENT_BEAMS : SERVER_BEAMS).remove(entity.getUUID());
        return true;
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int ticksRemaining) {
        if (!(entity instanceof Player player)) {
            return;
        }
        Map<UUID, Beam> beams = level.isClientSide() ? CLIENT_BEAMS : SERVER_BEAMS;
        long now = level.getGameTime();
        Beam beam = beams.computeIfAbsent(player.getUUID(), u -> new Beam());
        if (beam.lastTick != now - 1) {
            beam.reset();
        }
        beam.lastTick = now;

        if (beam.target != null && !this.holds(player, beam.target)) {
            if (beam.locked && level instanceof ServerLevel server) {
                server.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.6F);
            }
            beam.reset();
        }
        if (!beam.locked) {
            LivingEntity seen = this.lookTarget(player);
            if (seen != beam.target) {
                beam.target = seen;
                beam.warn = 0;
            } else if (seen != null && ++beam.warn >= WARN_TICKS) {
                beam.locked = true;
                if (level instanceof ServerLevel server) {
                    server.playSound(null, player.getX(), player.getEyeY(), player.getZ(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.6F, 1.8F);
                    server.playSound(null, seen.getX(), seen.getEyeY(), seen.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 1.5F);
                }
            }
        }
        LivingEntity target = beam.target;
        if (target == null) {
            return;
        }
        Vec3 from = Notes.mouth(player);
        Vec3 to = target.getBoundingBox().getCenter();
        if (level.isClientSide()) {
            this.drawBeam(level, player, from, to, beam);
        } else if (beam.locked && level instanceof ServerLevel server) {
            beam.held++;
            if (beam.held % 5 == 1) {
                this.pulse(server, player, from, to, target);
            }
            if (beam.held % 10 == 1) {
                server.playSound(null, from.x, from.y, from.z, this.instrument().sound(), SoundSource.PLAYERS, 0.9F,
                        Notes.soundPitch(Notes.lookPitch(player)));
            }
        }
    }

    /** One pulse of the locked beam: sonic damage and slowness to the target (and, piercing, to everything along it). */
    private void pulse(ServerLevel level, Player player, Vec3 from, Vec3 to, LivingEntity target) {
        this.zap(level, player, target);
        if (this.piercing()) {
            Vec3 dir = to.subtract(from).normalize();
            Vec3 end = from.add(dir.scale(this.range()));
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(from, end).inflate(1.0),
                    m -> m != target && m != player && m.isAlive() && !isFriend(player, m))) {
                Vec3 rel = e.getBoundingBox().getCenter().subtract(from);
                double along = rel.dot(dir);
                if (along > 0.0 && along < this.range() && rel.subtract(dir.scale(along)).length() < 0.7 + e.getBbWidth() * 0.5) {
                    this.zap(level, player, e);
                }
            }
        }
    }

    private void zap(ServerLevel level, Player player, LivingEntity e) {
        e.setInvulnerableTime(0);
        if (e.hurtServer(level, level.damageSources().indirectMagic(player, player), this.pulseDamage())) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 1), player);
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 4, 0.25, 0.25, 0.25, 0.1);
        }
    }

    /** Client: the thin flickering warning line, then the full beam once locked. */
    private void drawBeam(Level level, Player player, Vec3 from, Vec3 to, Beam beam) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        if (len < 0.5) {
            return;
        }
        var random = level.getRandom();
        int colour = this.beamColour(level.getGameTime());
        if (!beam.locked) {
            // a hair-thin dotted line that fills in as the lock closes
            float k = beam.warn / (float) WARN_TICKS;
            DustParticleOptions warn = new DustParticleOptions(0xFF5A6A, 0.35F + 0.25F * k);
            int n = (int) (len * (1.0 + 2.0 * k));
            for (int i = 0; i < n; i++) {
                if (random.nextFloat() < 0.5F) {
                    Vec3 p = from.add(d.scale(random.nextDouble()));
                    level.addParticle(warn, p.x, p.y, p.z, 0, 0, 0);
                }
            }
            return;
        }
        // locked: a glowing core in a shimmering sleeve, notes riding along it
        float swell = Math.min(1.0F, beam.held / 6.0F);
        DustParticleOptions outer = new DustParticleOptions(colour, 0.7F + 0.6F * swell);
        DustParticleOptions core = new DustParticleOptions(0xF4FFFF, 0.4F + 0.3F * swell);
        int n = (int) (len * 2.5);
        for (int i = 0; i < n; i++) {
            Vec3 p = from.add(d.scale(random.nextDouble()));
            double j = 0.18 * swell;
            level.addParticle(outer, p.x + (random.nextDouble() - 0.5) * j, p.y + (random.nextDouble() - 0.5) * j, p.z + (random.nextDouble() - 0.5) * j,
                    0, 0, 0);
            if (i % 2 == 0) {
                Vec3 q = from.add(d.scale(random.nextDouble()));
                level.addParticle(core, q.x, q.y, q.z, 0, 0, 0);
            }
        }
        if (beam.held % 4 == 0) {
            double t = (beam.held % 12) / 12.0;
            Vec3 p = from.add(d.scale(t));
            level.addParticle(ModParticles.SIFT_NOTE.get(), p.x, p.y + 0.15, p.z, Notes.lookPitch(player) / 24.0, 0, 0);
        }
        level.addParticle(ParticleTypes.ELECTRIC_SPARK, to.x + (random.nextDouble() - 0.5) * 0.5, to.y + (random.nextDouble() - 0.5) * 0.5,
                to.z + (random.nextDouble() - 0.5) * 0.5, 0, 0, 0);
        beam.held++;
    }

    /** True while the beam can stay locked: the target lives, is in range and in sight, and you still face it. */
    private boolean holds(Player player, LivingEntity target) {
        if (!target.isAlive() || target.isRemoved() || target.level() != player.level()) {
            return false;
        }
        Vec3 to = target.getBoundingBox().getCenter().subtract(player.getEyePosition());
        double dist = to.length();
        return dist <= this.range() && player.hasLineOfSight(target) && (dist < 2.0 || to.normalize().dot(player.getLookAngle()) > 0.82);
    }

    /** The creature in the crosshair (the nearest along the look ray, within range and in sight). */
    private @Nullable LivingEntity lookTarget(Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        double range = this.range();
        LivingEntity hit = null;
        double best = range;
        for (LivingEntity e : player.level().getEntitiesOfClass(LivingEntity.class, new AABB(eye, eye.add(look.scale(range))).inflate(1.5),
                m -> m != player && m.isAlive() && !m.isSpectator() && !isFriend(player, m))) {
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along > 0.0 && along < best && to.subtract(look.scale(along)).length() < 0.6 + e.getBbWidth() * 0.5 && player.hasLineOfSight(e)) {
                best = along;
                hit = e;
            }
        }
        return hit;
    }

    private static boolean isFriend(Player player, LivingEntity e) {
        return e instanceof OwnableEntity pet && pet.getOwner() == player;
    }

    /** One player's beam on one side. */
    private static final class Beam {
        @Nullable LivingEntity target;
        boolean locked;
        int warn;
        int held;
        long lastTick = Long.MIN_VALUE / 2;

        void reset() {
            this.target = null;
            this.locked = false;
            this.warn = 0;
            this.held = 0;
        }
    }

    /** Hue-cycling colour for the prism instruments (RGB). */
    static int prismColour(long time) {
        float h = (time % 60L) / 60.0F;
        int rgb = 0;
        for (int c = 0; c < 3; c++) {
            // a pastel rainbow: each channel a raised cosine a third of a turn apart
            double v = 0.72 + 0.28 * Math.cos((h - c / 3.0) * Math.PI * 2.0);
            rgb = rgb << 8 | (int) (v * 255.0);
        }
        return rgb;
    }
}
