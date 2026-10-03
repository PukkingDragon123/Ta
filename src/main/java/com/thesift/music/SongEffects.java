package com.thesift.music;

import com.thesift.block.HarmonySealBlock;
import com.thesift.entity.SkyWhale;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import java.util.ArrayList;
import java.util.List;
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
 * offering, the Nibs, the golems, the crystals, the tide - are heard by those creatures and blocks
 * themselves through {@link SongEvents#listenSongs}.)
 *
 * <ul>
 *   <li>{@link Song#LULLABY}: hostile creatures nearby fall asleep where they stand, and any
 *   Harmony Seal close by dissolves as if its stones had been tuned.</li>
 *   <li>{@link Song#WHALE}: the nearest Sky Whale hears it and comes to sing back.</li>
 * </ul>
 * M2 band: both reach further (and the Lullaby lasts longer) the bigger the player's band
 * ({@link Bands#power}).
 */
public final class SongEffects {
    public static final double LULLABY_RADIUS = 12.0;
    public static final int LULLABY_TICKS = 20 * 12;
    public static final int SEAL_RADIUS = 10;
    public static final double WHALE_RADIUS = 96.0;

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
            default -> { }
        }
    }

    private static void lullaby(ServerLevel level, @Nullable Player player, Vec3 at) {
        float power = Bands.power(player); // M2 band: a bigger band sings further and longer
        double radius = LULLABY_RADIUS * power;
        int sleep = Math.round(LULLABY_TICKS * power);
        int sealRadius = Math.round(SEAL_RADIUS * power);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(radius),
                m -> m.isAlive() && m instanceof Enemy && m instanceof Mob && m.distanceToSqr(at) <= radius * radius)) {
            if (e instanceof com.thesift.entity.boss.MiniBoss || e instanceof com.thesift.entity.boss.Dictator || e instanceof net.minecraft.world.entity.boss.wither.WitherBoss || e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon || e instanceof net.minecraft.world.entity.monster.warden.Warden) {
                continue;
            }
            e.addEffect(new MobEffectInstance(ModEffects.ENTRANCED, sleep, 0, false, true), player);
            level.sendParticles(ModParticles.SLEEP_SPORE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.2, e.getZ(), 6, 0.3, 0.2, 0.3, 0.01);
        }
        // Harmony Seals answer the lullaby as though their stones had been tuned
        BlockPos c = BlockPos.containing(at);
        List<BlockPos> seals = new ArrayList<>();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-sealRadius, -sealRadius / 2, -sealRadius), c.offset(sealRadius, sealRadius / 2, sealRadius))) {
            if (level.getBlockState(p).is(ModBlocks.HARMONY_SEAL.get())) {
                seals.add(p.immutable());
            }
        }
        for (BlockPos seal : seals) {
            if (level.getBlockState(seal).is(ModBlocks.HARMONY_SEAL.get())) {
                HarmonySealBlock.dissolve(level, seal);
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
