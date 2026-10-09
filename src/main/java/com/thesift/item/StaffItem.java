package com.thesift.item;

import com.thesift.entity.SculkHarmoner;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The Conductor's Staff, taken from the Dictator. It conducts the world:
 *
 * <ul>
 *   <li>use: a beam of song. Every creature along it is entranced for three seconds - it stops,
 *   forgets its target and just sings.</li>
 *   <li>sneak + use: the orchestra answers - a Sculk Harmoner appears and circles you for twenty
 *   seconds, its song keeping you strong, fast, healing and hard to hurt.</li>
 * </ul>
 */
public class StaffItem extends Item {
    private static final double REACH = 24.0;
    public static final int ENTRANCE_TICKS = 60;

    public StaffItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            if (player.isSecondaryUseActive()) {
                this.summon(server, player);
                player.getCooldowns().addCooldown(stack, 600);
            } else {
                this.beam(server, player);
                player.getCooldowns().addCooldown(stack, 50);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private void beam(ServerLevel server, Player player) {
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        HitResult block = server.clip(new ClipContext(eye, eye.add(look.scale(REACH)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        double reach = block.getType() == HitResult.Type.MISS ? REACH : block.getLocation().distanceTo(eye);
        List<LivingEntity> hits = new ArrayList<>();
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(player.blockPosition()).inflate(REACH))) {
            if (e == player || !e.isAlive() || e instanceof Player || e instanceof SculkHarmoner
                    || (e instanceof TamableAnimal t && t.isOwnedBy(player))) {
                continue;
            }
            Vec3 to = e.getBoundingBox().getCenter().subtract(eye);
            double along = to.dot(look);
            if (along > 0 && along < reach && to.subtract(look.scale(along)).length() < 0.9 + e.getBbWidth() * 0.5) {
                hits.add(e);
            }
        }
        // the beam: a twisting double helix of notes with rings pulsing along it
        Vec3 side = look.cross(new Vec3(0, 1, 0)).normalize();
        if (side.lengthSqr() < 1.0E-4) {
            side = new Vec3(1, 0, 0);
        }
        Vec3 up = side.cross(look).normalize();
        for (double d = 1.2; d < reach; d += 0.45) {
            double a = d * 1.4;
            for (int k = 0; k < 2; k++) {
                double aa = a + k * Math.PI;
                Vec3 p = eye.add(look.scale(d)).add(side.scale(Math.cos(aa) * 0.35)).add(up.scale(Math.sin(aa) * 0.35)).subtract(0, 0.25, 0);
                server.sendParticles(ModParticles.GUIDE_NOTE.get(), p.x, p.y, p.z, 0, 0.18 + k * 0.6, 0.95 - k * 0.3, 0.9, 1.0);
            }
            if ((int) (d / 0.45) % 6 == 0) {
                Vec3 p = eye.add(look.scale(d)).subtract(0, 0.25, 0);
                server.sendParticles(ModParticles.STAR_SPARKLE.get(), p.x, p.y, p.z, 2, 0.1, 0.1, 0.1, 0.0);
            }
        }
        server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.STAFF_NOTE.get(), SoundSource.PLAYERS, 1.2F, 1.3F);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.8F);
        for (LivingEntity e : hits) {
            e.addEffect(new MobEffectInstance(ModEffects.ENTRANCED, ENTRANCE_TICKS, 0, false, true), player);
            server.sendParticles(ModParticles.RESONANCE_RING.get(), e.getX(), e.getY() + 0.1, e.getZ(), 0, 1.5, 0.0, 0.0, 1.0);
            server.sendParticles(ModParticles.SIFT_NOTE.get(), e.getX(), e.getY() + e.getBbHeight() + 0.3, e.getZ(), 6, 0.3, 0.2, 0.3, 0.0);
        }
    }

    private void summon(ServerLevel server, Player player) {
        SculkHarmoner bird = ModEntities.SCULK_HARMONER.get().create(server, EntitySpawnReason.MOB_SUMMONED);
        if (bird == null) {
            return;
        }
        bird.setOwner(player);
        bird.snapTo(player.getX(), player.getY() + 2.4, player.getZ(), player.getYRot(), 0.0F);
        server.addFreshEntity(bird);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.DICTATOR_SUMMON.get(), SoundSource.PLAYERS, 1.0F, 1.4F);
        server.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.HARMONER_SING.get(), SoundSource.PLAYERS, 1.2F, 1.0F);
        for (int i = 0; i < 3; i++) {
            server.sendParticles(ModParticles.RESONANCE_RING.get(), player.getX(), player.getY() + 0.1 + i * 0.8, player.getZ(), 0, 1.5 + i, 0.0, 0.0, 1.0);
        }
        server.sendParticles(ModParticles.PORTAL_SOUL.get(), bird.getX(), bird.getY(), bird.getZ(), 30, 0.5, 0.5, 0.5, 0.08);
        player.sendOverlayMessage(Component.translatable("message.thesift.staff.summon"));
    }
}
