package com.thesift.enchant;

import com.thesift.item.SiftInstrumentItem;
import com.thesift.music.Notes;
import com.thesift.music.Resonance;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.music.band.Bands;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.VanillaGameEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * F2 Band Table: what the Sift enchantments do beyond their data (attributes and the like are in their JSON).
 *
 * <ul>
 *   <li>Melody Steps: a glowing note left behind every step or two, the ground chiming a rising scale.</li>
 *   <li>Hushed Step: steps, landings and splashes make no vibration at all.</li>
 *   <li>Sculk Ward: a chance per level (all four pieces add up) to shrug off Sculk Corruption, and less harm from
 *   the corruption's bite and from sonic booms.</li>
 *   <li>Echo Strike: every fourth hit (third at II) rings a sonic echo through the monsters around the target.</li>
 *   <li>Resonance: more damage for every creature playing in the attacker's band.</li>
 *   <li>Crescendo: hits within two seconds of each other build up, five steps at most.</li>
 *   <li>Reverb: songs on the instrument reach further ({@link #reverb(Player)}, used by {@code Bands.power}).</li>
 *   <li>Fortissimo: a finished song blasts the monsters around the player back.</li>
 * </ul>
 */
public final class SiftEnchantEffects {
    /** Melody Steps: blocks walked between two notes. */
    private static final double STEP_EVERY = 1.6;
    private static final int[] SCALE = {6, 8, 10, 13, 15, 18, 20, 22};
    private static final int CRESCENDO_WINDOW = 40;
    private static final int CRESCENDO_MAX = 5;
    private static final Map<UUID, double[]> WALKED = new HashMap<>();
    /** Per attacker: {last hit game time, crescendo stacks, hits counted for Echo Strike}. */
    private static final Map<UUID, long[]> HITS = new HashMap<>();
    private static boolean echoing;
    private static boolean started;

    private SiftEnchantEffects() {
    }

    public static void register() {
        if (started) {
            return;
        }
        started = true;
        NeoForge.EVENT_BUS.addListener(SiftEnchantEffects::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(SiftEnchantEffects::onGameEvent);
        NeoForge.EVENT_BUS.addListener(SiftEnchantEffects::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(SiftEnchantEffects::onIncomingDamage);
        SongEvents.listenSongs(SiftEnchantEffects::onSong);
    }

    // ------------------------------------------------------------------ Reverb (instruments)

    /** How much further this player's songs reach thanks to Reverb on the instrument in hand: 1 without it. */
    public static float reverb(@Nullable Player player) {
        if (player == null) {
            return 1.0F;
        }
        int lvl = Math.max(instrumentLevel(player, player.getMainHandItem(), SiftEnchant.REVERB),
                instrumentLevel(player, player.getOffhandItem(), SiftEnchant.REVERB));
        return 1.0F + 0.25F * lvl;
    }

    private static int instrumentLevel(Player player, ItemStack stack, SiftEnchant e) {
        return stack.getItem() instanceof SiftInstrumentItem ? e.level(stack, player.level().registryAccess()) : 0;
    }

    // ------------------------------------------------------------------ Melody Steps (boots)

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (!(p.level() instanceof ServerLevel level) || p.isSpectator() || (p.tickCount & 1) != 0) {
            return;
        }
        int lvl = SiftEnchant.MELODY_STEPS.level(p);
        if (lvl <= 0) {
            WALKED.remove(p.getUUID());
            return;
        }
        double[] w = WALKED.computeIfAbsent(p.getUUID(), u -> new double[]{p.getX(), p.getZ(), 0.0, 0.0});
        double dx = p.getX() - w[0];
        double dz = p.getZ() - w[1];
        w[0] = p.getX();
        w[1] = p.getZ();
        double moved = Math.sqrt(dx * dx + dz * dz);
        if (!p.onGround() || moved > 4.0) {
            return;
        }
        w[2] += moved;
        double every = STEP_EVERY - 0.2 * (lvl - 1);
        if (w[2] < every) {
            return;
        }
        w[2] = 0.0;
        int step = (int) w[3];
        w[3] = (step + 1) % (SCALE.length * 2 - 2);
        // up the scale and back down again
        int i = step < SCALE.length ? step : SCALE.length * 2 - 2 - step;
        int pitch = SCALE[Mth.clamp(i, 0, SCALE.length - 1)];
        int rgb = Notes.colour(pitch);
        double x = p.getX();
        double z = p.getZ();
        level.sendParticles(ModParticles.GUIDE_NOTE.get(), x, p.getY() + 0.15, z, 0, (rgb >> 16 & 255) / 255.0, (rgb >> 8 & 255) / 255.0,
                (rgb & 255) / 255.0, 1.0);
        if (lvl >= 2) {
            level.sendParticles(ModParticles.SIFT_NOTE.get(), x, p.getY() + 0.3, z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
        }
        if (lvl >= 3) {
            level.sendParticles(ModParticles.STAR_SPARKLE.get(), x, p.getY() + 0.1, z, 2, 0.15, 0.02, 0.15, 0.01);
        }
        float vol = p.isShiftKeyDown() ? 0.08F : 0.22F;
        level.playSound(null, x, p.getY(), z, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, vol, Notes.soundPitch(pitch));
        level.playSound(null, x, p.getY(), z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, vol * 0.4F, Notes.soundPitch(pitch));
    }

    // ------------------------------------------------------------------ Hushed Step (boots)

    private static void onGameEvent(VanillaGameEvent event) {
        if (event.getVanillaEvent() != net.minecraft.world.level.gameevent.GameEvent.STEP
                && event.getVanillaEvent() != net.minecraft.world.level.gameevent.GameEvent.HIT_GROUND
                && event.getVanillaEvent() != net.minecraft.world.level.gameevent.GameEvent.SWIM
                && event.getVanillaEvent() != net.minecraft.world.level.gameevent.GameEvent.SPLASH) {
            return;
        }
        Entity cause = event.getCause();
        if (cause instanceof LivingEntity living && !living.level().isClientSide() && SiftEnchant.HUSHED_STEP.level(living) > 0) {
            event.setCanceled(true);
        }
    }

    // ------------------------------------------------------------------ Sculk Ward (armour)

    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity e = event.getEntity();
        if (!event.getEffectInstance().is(ModEffects.SCULK_CORRUPTION) || !(e.level() instanceof ServerLevel level)) {
            return;
        }
        int ward = SiftEnchant.SCULK_WARD.armourTotal(e);
        if (ward <= 0) {
            return;
        }
        float chance = Math.min(0.8F, 0.07F * ward);
        if (e.getRandom().nextFloat() < chance) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 0, 1.6, 0.0, 0.0, 1.0);
            level.sendParticles(ParticleTypes.SCULK_CHARGE_POP, e.getX(), e.getY() + e.getBbHeight() * 0.6, e.getZ(), 8, 0.35, 0.4, 0.35, 0.02);
            level.playSound(null, e.getX(), e.getY(), e.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 0.8F, 1.6F);
        }
    }

    // ------------------------------------------------------------------ weapons, and Sculk Ward's softer blows

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // Sculk Ward: the corruption's bite and the Warden's sonic boom hurt less
        if (event.getSource().is(DamageTypes.SONIC_BOOM) || event.getSource().is(DamageTypes.WITHER) && victim.hasEffect(ModEffects.SCULK_CORRUPTION)) {
            int ward = SiftEnchant.SCULK_WARD.armourTotal(victim);
            if (ward > 0) {
                event.setAmount(event.getAmount() * (1.0F - Math.min(0.6F, 0.05F * ward)));
            }
        }
        if (echoing || !(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player) {
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (weapon.isEmpty() || !weapon.isEnchanted()) {
            return;
        }
        var reg = level.registryAccess();
        int resonance = SiftEnchant.RESONANCE.level(weapon, reg);
        int crescendo = SiftEnchant.CRESCENDO.level(weapon, reg);
        int echo = SiftEnchant.ECHO_STRIKE.level(weapon, reg);
        if (resonance + crescendo + echo == 0) {
            return;
        }
        long now = level.getGameTime();
        long[] h = HITS.computeIfAbsent(player.getUUID(), u -> new long[]{Long.MIN_VALUE / 2, 0L, 0L});
        float amount = event.getAmount();
        Vec3 at = victim.position().add(0.0, victim.getBbHeight() * 0.6, 0.0);
        if (resonance > 0) {
            int band = 0;
            for (Mob m : Bands.members(player)) {
                if (m.isAlive() && m.distanceToSqr(player) < 32.0 * 32.0) {
                    band++;
                }
            }
            band = Math.min(band, 8);
            if (band > 0) {
                amount += 0.5F * resonance * band;
                // the band rings through the blade: a chord, one note per member
                for (int i = 0; i < Math.min(band, 3); i++) {
                    int n = Notes.chord(10)[i];
                    level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_BELL.value(), SoundSource.PLAYERS, 0.45F, Notes.soundPitch(n));
                }
                level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.3, at.z, 0, (band % 6) / 6.0, 0.0, 0.0, 1.0);
            }
        }
        if (crescendo > 0) {
            int stacks = now - h[0] <= CRESCENDO_WINDOW ? (int) Math.min(CRESCENDO_MAX, h[1] + 1) : 0;
            h[1] = stacks;
            amount *= 1.0F + 0.06F * crescendo * stacks;
            int pitch = Mth.clamp(6 + stacks * 3, 0, Notes.MAX_PITCH);
            level.playSound(null, at.x, at.y, at.z, SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS, 0.5F + stacks * 0.08F,
                    Notes.soundPitch(pitch));
            if (stacks > 0) {
                level.sendParticles(ModParticles.SIFT_NOTE.get(), at.x, at.y + 0.2, at.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
            }
        }
        h[0] = now;
        event.setAmount(amount);
        if (echo > 0) {
            h[2]++;
            if (h[2] >= 5 - echo) {
                h[2] = 0;
                echoStrike(level, player, victim, echo);
            }
        }
        if (HITS.size() > 256) {
            HITS.clear();
        }
    }

    /** The sonic echo: a ring of sound out from the target that strikes the monsters around it. */
    private static void echoStrike(ServerLevel level, Player player, LivingEntity target, int lvl) {
        Vec3 c = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0);
        level.sendParticles(ParticleTypes.SONIC_BOOM, c.x, c.y, c.z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, target.getY() + 0.1, c.z, 0, 3.0 + lvl, 0.0, 0.0, 1.0);
        level.sendParticles(ParticleTypes.SCULK_SOUL, c.x, c.y, c.z, 6, 0.5, 0.4, 0.5, 0.03);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 0.35F, 1.7F);
        double r = 3.0 + lvl;
        echoing = true;
        try {
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(c, c).inflate(r),
                    m -> m.isAlive() && m != target && m != player && m instanceof Enemy && m.distanceToSqr(c) <= r * r)) {
                e.hurtServer(level, player.damageSources().playerAttack(player), 2.0F + 1.5F * lvl);
                Vec3 push = e.position().subtract(c).multiply(1.0, 0.0, 1.0).normalize().scale(0.6);
                e.push(push.x, 0.25, push.z);
            }
        } finally {
            echoing = false;
        }
    }

    // ------------------------------------------------------------------ Fortissimo (instruments)

    private static void onSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        if (player == null) {
            return;
        }
        int lvl = Math.max(instrumentLevel(player, player.getMainHandItem(), SiftEnchant.FORTISSIMO),
                instrumentLevel(player, player.getOffhandItem(), SiftEnchant.FORTISSIMO));
        if (lvl <= 0) {
            return;
        }
        double r = 4.0 + 2.0 * lvl;
        Vec3 c = player.position();
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.1, c.z, 0, r, 0.0, 0.0, 1.0);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 1.0, c.z, 0, r * 0.7, 0.0, 0.0, 1.0);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.NOTE_BLOCK_BASEDRUM.value(), SoundSource.PLAYERS, 1.6F, 0.6F);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.PLAYERS, 0.4F, 1.8F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(r),
                m -> m.isAlive() && m instanceof Enemy && m.distanceToSqr(c) <= r * r)) {
            Vec3 push = e.position().subtract(c).multiply(1.0, 0.0, 1.0).normalize().scale(0.9 + 0.3 * lvl);
            e.push(push.x, 0.35, push.z);
            e.hurtServer(level, player.damageSources().playerAttack(player), 2.0F * lvl);
            level.sendParticles(ModParticles.SIFT_NOTE.get(), e.getX(), e.getY() + e.getBbHeight(), e.getZ(), 0, level.getRandom().nextDouble(),
                    0.0, 0.0, 1.0);
        }
        Resonance.pulse(level, BlockPos.containing(c), 0.6F, (int) r);
    }
}
