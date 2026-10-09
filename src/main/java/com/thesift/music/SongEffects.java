package com.thesift.music;

import com.thesift.entity.SkyWhale;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * What the generic songs do wherever they are played. (Songs tied to one creature or block - the
 * offering, the Nibs, the golems, the crystals - are heard by those creatures and blocks
 * themselves through {@link SongEvents#listenSongs}.)
 *
 * <ul>
 *   <li>{@link Song#LULLABY}: hostile creatures nearby fall asleep where they stand (and a
 *   Gobbler is lulled, see ModSeaSky).</li>
 *   <li>{@link Song#WHALE}: the nearest Sky Whale hears it and comes to sing back.</li>
 *   <li>{@link Song#AURORA} (M1, a Prism song): a curtain of the four lights rises, the air around
 *   glows bright for half a minute, monsters nearby are outlined through walls and the players
 *   close by see in the dark.</li>
 * </ul>
 * M2 band: both reach further (and the Lullaby lasts longer) the bigger the player's band
 * ({@link Bands#power}).
 */
public final class SongEffects {
    public static final double LULLABY_RADIUS = 12.0;
    public static final int LULLABY_TICKS = 20 * 12;
    public static final double WHALE_RADIUS = 96.0;
    public static final double AURORA_RADIUS = 32.0;
    public static final int AURORA_TICKS = 20 * 30;

    private SongEffects() {
    }

    static void init() {
        SongEvents.listenSongs(SongEffects::onSong);
    }

    private static void onSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        switch (song) {
            case LULLABY -> lullaby(level, player, at);
            case WHALE -> {
                if (player != null) {
                    whale(level, player);
                }
            }
            case AURORA -> aurora(level, at);
            default -> { }
        }
    }

    private static void lullaby(ServerLevel level, @Nullable Player player, Vec3 at) {
        float power = Bands.power(player); // M2 band: a bigger band sings further and longer
        double radius = LULLABY_RADIUS * power;
        int sleep = Math.round(LULLABY_TICKS * power);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                m -> m.isAlive() && m instanceof Enemy && m instanceof Mob && m.distanceToSqr(at) <= radius * radius)) {
            if (e instanceof com.thesift.entity.boss.MiniBoss || e instanceof com.thesift.entity.boss.Dictator || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.monster.warden.Warden) {
                continue;
            }
            e.addEffect(new MobEffectInstance(ModEffects.ENTRANCED, sleep, 0, false, true), player);
            level.sendParticles(ModParticles.SLEEP_SPORE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
        }
    }

    /** M1: the Aurora - lights in the air, monsters outlined, eyes for the dark. */
    private static void aurora(ServerLevel level, Vec3 at) {
        PrismLight.burst(level, at);
        level.playSound(null, at.x, at.y, at.z, net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_RESONATE, net.minecraft.sounds.SoundSource.PLAYERS,
                1.5F, 0.7F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(AURORA_RADIUS),
                m -> m.isAlive() && m instanceof Enemy && m.distanceToSqr(at) <= AURORA_RADIUS * AURORA_RADIUS)) {
            e.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, AURORA_TICKS, 0, false, false));
        }
        for (Player p : level.getEntitiesOfClass(Player.class, new AABB(at, at).inflate(12.0), Player::isAlive)) {
            p.addEffect(new MobEffectInstance(net.minecraft.world.effect.MobEffects.NIGHT_VISION, AURORA_TICKS * 2, 0, false, true));
        }
        // the air itself glows: little lights hung all round, for half a minute
        BlockPos c = BlockPos.containing(at);
        for (int dx = -8; dx <= 8; dx += 4) {
            for (int dz = -8; dz <= 8; dz += 4) {
                if (dx * dx + dz * dz <= 72) {
                    PrismLight.light(level, c.offset(dx, 1, dz), 15, AURORA_TICKS);
                }
            }
        }
    }

    private static void whale(ServerLevel level, Player player) {
        SkyWhale best = null;
        double radius = WHALE_RADIUS * Bands.power(player); // M2 band: a bigger band is heard further off
        double bestD = radius * radius;
        for (SkyWhale w : level.getEntitiesOfClass(SkyWhale.class, player.getBoundingBox().inflate(radius), SkyWhale::isAlive)) {
            double d = w.distanceToSqr(player);
            if (d < bestD && !w.isAnswering()) {
                bestD = d;
                best = w;
            }
        }
        if (best != null) {
            best.answer(level, player);
        }
    }
}
