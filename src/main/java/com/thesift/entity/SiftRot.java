package com.thesift.entity;

import com.thesift.TheSift;
import com.thesift.registry.ModDimensions;
import com.thesift.registry.ModSiftSniffer;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * E1 the rot: the Sift's gentle creatures only stay alive in the Sift's dream. Taken anywhere else
 * a passive Sift creature slowly rots - its colours sour to blotchy red and dark green, it groans,
 * it sheds rotten motes, and from half-way on it is a lifeless shambling thing that will not join
 * a band. A Sift Sniffer rots all the way back into a plain Zombified Sniffer (the vanilla
 * Sniffer), and a Zombified Sniffer brought into the Sift blooms into a Sift Sniffer again.
 * Brought back to the Sift, a rotting creature slowly heals.
 *
 * <p>Progress is a synced data attachment ({@link ModSiftSniffer#ROT}, seconds of rot), stepped
 * once a second for every mob; the renderers read it for the tint and the blotches.
 */
public final class SiftRot {
    /** Seconds outside the Sift until a creature is fully rotten. */
    public static final int FULL = 300;
    /** From here on it is lifeless: it shambles, groans and plays no music. */
    public static final int ROTTEN = 120;
    private static final Identifier SHAMBLE = TheSift.id("rot_shamble");
    private static final int RED = 0x8E2A22;
    private static final int GREEN = 0x2F4A22;

    private SiftRot() {
    }

    public static void onEntityTick(EntityTickEvent.Post event) {
        Entity e = event.getEntity();
        if (e.tickCount % 20 == 0 && e instanceof Mob mob && mob.isAlive() && e.level() instanceof ServerLevel level) {
            step(mob, level);
        }
    }

    /** True for the creatures that rot: the Sift's own passive and neutral animals (not its monsters, bosses or summons). */
    public static boolean rots(Entity e) {
        if (!(e instanceof Mob) || e instanceof Enemy) {
            return false;
        }
        EntityType<?> type = e.getType();
        if (!BuiltInRegistries.ENTITY_TYPE.getKey(type).getNamespace().equals(TheSift.MODID)) {
            return false;
        }
        MobCategory c = type.getCategory();
        return c == MobCategory.CREATURE || c == MobCategory.WATER_CREATURE || c == MobCategory.WATER_AMBIENT || c == MobCategory.AMBIENT
                || c == MobCategory.UNDERGROUND_WATER_CREATURE;
    }

    /** Seconds of rot (0 = healthy). */
    public static int rot(Entity e) {
        Integer r = e.getExistingDataOrNull(ModSiftSniffer.ROT);
        return r == null ? 0 : r;
    }

    /** 0 (healthy) to 1 (fully rotten), for the renderers. */
    public static float amount(Entity e) {
        return Math.min(1.0F, rot(e) / (float) FULL);
    }

    public static boolean isRotten(Entity e) {
        return rot(e) >= ROTTEN;
    }

    public static boolean inSift(Entity e) {
        return e.level().dimension().equals(ModDimensions.THE_SIFT);
    }

    /** One second of life for {@code mob}: conversion, rotting or healing. */
    public static void step(Mob mob, ServerLevel level) {
        boolean sift = level.dimension().equals(ModDimensions.THE_SIFT);
        if (sift && mob.getType() == EntityTypes.SNIFFER) {
            // a Zombified Sniffer breathes the Sift's air and blooms
            SiftSniffer s = convert(mob, ModSiftSniffer.SIFT_SNIFFER.get(), level);
            if (s != null) {
                s.setGarden(1);
                level.sendParticles(ParticleTypes.CHERRY_LEAVES, s.getX(), s.getY() + 1.2, s.getZ(), 40, 0.8, 0.6, 0.8, 0.02);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, s.getX(), s.getY() + 1.0, s.getZ(), 16, 0.8, 0.6, 0.8, 0.0);
                level.playSound(null, s.getX(), s.getY(), s.getZ(), ModSiftSniffer.BLOOM.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
            }
            return;
        }
        if (!rots(mob)) {
            return;
        }
        int rot = rot(mob);
        int next = sift ? Math.max(0, rot - 2) : Math.min(FULL, rot + 1);
        if (next != rot) {
            mob.setData(ModSiftSniffer.ROT, next);
            shamble(mob, next >= ROTTEN);
        }
        if (!sift && next >= FULL && mob instanceof SiftSniffer) {
            // all the way gone: a plain, grey-mossed Zombified Sniffer
            Mob z = convert(mob, EntityTypes.SNIFFER, level);
            if (z != null) {
                level.sendParticles(new DustParticleOptions(GREEN, 1.6F), z.getX(), z.getY() + 1.0, z.getZ(), 40, 0.8, 0.6, 0.8, 0.0);
                level.playSound(null, z.getX(), z.getY(), z.getZ(), ModSiftSniffer.ROT_GROAN.get(), SoundSource.NEUTRAL, 1.2F, 0.6F);
            }
            return;
        }
        if (next > 0 && !sift) {
            // rotten motes, more of them the further gone it is, and the odd groan
            float k = next / (float) FULL;
            int n = 1 + (int) (k * 4);
            double h = mob.getBbHeight();
            level.sendParticles(new DustParticleOptions(mob.getRandom().nextBoolean() ? RED : GREEN, 0.9F + k), mob.getX(), mob.getY() + h * 0.6,
                    mob.getZ(), n, mob.getBbWidth() * 0.4, h * 0.3, mob.getBbWidth() * 0.4, 0.0);
            if (next >= ROTTEN && mob.getRandom().nextInt(10) == 0) {
                mob.playSound(ModSiftSniffer.ROT_GROAN.get(), 0.7F, (mob.getVoicePitch() * 0.7F));
            }
        } else if (sift && rot > 0 && mob.getRandom().nextInt(3) == 0) {
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight(), mob.getZ(), 2, 0.3, 0.2, 0.3, 0.0);
        }
    }

    /** Lifeless: a rotten creature drags itself along at half speed. */
    private static void shamble(Mob mob, boolean rotten) {
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) {
            return;
        }
        if (rotten) {
            speed.addOrUpdateTransientModifier(new AttributeModifier(SHAMBLE, -0.55, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        } else {
            speed.removeModifier(SHAMBLE);
        }
    }

    /** Replaces {@code from} with a new mob of {@code to} in the same place, keeping its age, name, health share and persistence. */
    public static <T extends Mob> @Nullable T convert(Mob from, EntityType<T> to, ServerLevel level) {
        T m = to.create(level, EntitySpawnReason.CONVERSION);
        if (m == null) {
            return null;
        }
        m.snapTo(from.getX(), from.getY(), from.getZ(), from.getYRot(), from.getXRot());
        m.yHeadRot = from.yHeadRot;
        m.yBodyRot = from.yBodyRot;
        if (from instanceof AgeableMob a && m instanceof AgeableMob b) {
            b.setAge(a.getAge());
        }
        if (from.hasCustomName()) {
            m.setCustomName(from.getCustomName());
            m.setCustomNameVisible(from.isCustomNameVisible());
        }
        if (from.isPersistenceRequired()) {
            m.setPersistenceRequired();
        }
        m.setHealth(Math.max(1.0F, m.getMaxHealth() * from.getHealth() / from.getMaxHealth()));
        level.addFreshEntity(m);
        from.discard();
        return m;
    }
}
