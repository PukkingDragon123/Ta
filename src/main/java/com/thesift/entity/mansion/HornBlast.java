package com.thesift.entity.mansion;

import com.thesift.registry.ModMansion;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * MANSION: the Giant Goat Horn's blast - a deep call and a gust that throws everything in a cone in front of the horn back
 * and up, the harder the nearer. Shared by the Hornblower and by players blowing the horn.
 */
public final class HornBlast {
    /** How far the gust reaches, in blocks. */
    public static final double RANGE = 9.0;
    /** The cone's half angle: cos 50 degrees. */
    private static final double CONE = Math.cos(Math.toRadians(50.0));

    private HornBlast() {
    }

    /**
     * Blows the horn from {@code source} along {@code dir}. Creatures in the cone are pushed away (allies of the source
     * are spared) and take {@code damage} (0 for none).
     *
     * @return how many creatures the gust caught
     */
    public static int blast(ServerLevel level, LivingEntity source, Vec3 dir, float power, float damage) {
        Vec3 look = new Vec3(dir.x, Math.max(-0.3, Math.min(0.3, dir.y)), dir.z).normalize();
        Vec3 mouth = source.getEyePosition().add(look.scale(0.8)).subtract(0.0, 0.2, 0.0);
        level.playSound(null, mouth.x, mouth.y, mouth.z, ModMansion.HORN_BLAST.get(), SoundSource.HOSTILE, 4.0F, 1.0F);
        level.playSound(null, mouth.x, mouth.y, mouth.z, ModMansion.HORN_GUST.get(), SoundSource.HOSTILE, 1.6F, 1.0F);
        level.playSound(null, mouth.x, mouth.y, mouth.z, SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.5F, 0.55F);
        level.gameEvent(source, GameEvent.INSTRUMENT_PLAY, mouth);
        // the gust: puffs racing out along the cone, wider as they go
        Vec3 side = look.cross(new Vec3(0.0, 1.0, 0.0));
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1.0, 0.0, 0.0);
        }
        side = side.normalize();
        Vec3 up = side.cross(look).normalize();
        for (double d = 1.0; d < RANGE; d += 1.0) {
            double spread = d * 0.75;
            for (int k = 0; k < 3; k++) {
                double a = level.getRandom().nextDouble() * Math.PI * 2.0;
                double r = level.getRandom().nextDouble() * spread;
                Vec3 p = mouth.add(look.scale(d)).add(side.scale(Math.cos(a) * r)).add(up.scale(Math.sin(a) * r * 0.6));
                level.sendParticles(ParticleTypes.CLOUD, p.x, p.y, p.z, 0, look.x, look.y + 0.05, look.z, 0.35 + 0.05 * d);
            }
            if ((int) d % 3 == 1) {
                Vec3 p = mouth.add(look.scale(d));
                level.sendParticles(ParticleTypes.GUST, p.x, p.y, p.z, 1, spread * 0.3, 0.2, spread * 0.3, 0.0);
            }
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, mouth.x + look.x, mouth.y, mouth.z + look.z, 2, 0.2, 0.2, 0.2, 0.0);
        int caught = 0;
        AABB box = new AABB(mouth, mouth).inflate(RANGE);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, LivingEntity::isAlive)) {
            if (e == source || e.isAlliedTo(source) || e instanceof Player p && (p.isSpectator() || p.isCreative() && !(source instanceof Player))) {
                continue;
            }
            Vec3 to = e.getBoundingBox().getCenter().subtract(mouth);
            double dist = to.length();
            if (dist > RANGE || dist < 1.0E-3 || to.normalize().dot(look) < CONE) {
                continue;
            }
            double k = power * (1.0 - 0.55 * dist / RANGE) * (1.0 - e.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.KNOCKBACK_RESISTANCE) * 0.6);
            Vec3 push = new Vec3(to.x, 0.0, to.z).normalize().scale(1.9 * k);
            e.push(push.x, 0.42 + 0.25 * k, push.z);
            if (damage > 0.0F) {
                e.hurtServer(level, level.damageSources().mobAttack(source), damage);
            }
            level.sendParticles(ParticleTypes.CLOUD, e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 6, 0.3, 0.3, 0.3, 0.05);
            caught++;
        }
        return caught;
    }

    /** The windup's telegraph: air drawn in towards the horn's bell. */
    public static void inhale(ServerLevel level, Entity source, Vec3 dir) {
        Vec3 mouth = source.getEyePosition().add(dir.normalize().scale(1.2));
        for (int i = 0; i < 3; i++) {
            Vec3 from = mouth.add((level.getRandom().nextDouble() - 0.5) * 3.0, (level.getRandom().nextDouble() - 0.5) * 2.0,
                    (level.getRandom().nextDouble() - 0.5) * 3.0);
            Vec3 v = mouth.subtract(from).scale(0.12);
            level.sendParticles(ParticleTypes.CLOUD, from.x, from.y, from.z, 0, v.x, v.y, v.z, 1.0);
        }
    }
}
