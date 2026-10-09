package com.thesift.world.sky;

import com.thesift.registry.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * W-sky: swinging on Sky Vines, the client's half - the local player is moved by {@link SwingPhysics}.
 *
 * <p>Grab a vine by using it, or by jumping (or falling) into it: you hang from where it hangs from, on a rope as long as
 * the vine above your hands, and keep the speed you had. Hold forward facing the way you swing to pump it higher, back
 * to brake, left/right to swing sideways; sneak to slide down the vine, look up and hold forward to climb; jump to let
 * go with a little kick - for a moment the air hardly slows you, so a good swing throws you far. Holding a span (a vine
 * between islands), forward takes you hand over hand along it. Using another vine while you swing catches it.
 */
public final class SkySwingClient {
    /** How much of its horizontal speed a launched player keeps each tick (vanilla air keeps 0.91). */
    private static final double FLIGHT_KEEP = 0.985;
    private static final double HAND = 0.12;
    private static @Nullable Swing swing;
    private static int cooldown;
    private static int flight;
    private static boolean jumpWas;
    private static @Nullable BlockPos lastLetGo;
    private static int sinceLetGo;

    /** The rope the local player hangs on. */
    private static final class Swing {
        BlockPos anchor;
        Vec3 pivot;
        boolean span;
        double reach;
        double length;
        Vec3 velocity;
        Vec3 before = Vec3.ZERO;
        Vec3 intended = Vec3.ZERO;
        int age;
        int grounded;
        @Nullable BlockPos travelTo;

        Swing(SkyVines.Anchor a, double length, Vec3 velocity) {
            this.anchor = a.block();
            this.pivot = a.pivot();
            this.span = a.span();
            this.reach = a.reach();
            this.length = length;
            this.velocity = velocity;
        }
    }

    private SkySwingClient() {
    }

    public static void register(IEventBus modBus) {
        SkySwing.clientGrab = SkySwingClient::grab;
        modBus.addListener(SkySwingClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(SkySwingClient::onTickPre);
        NeoForge.EVENT_BUS.addListener(SkySwingClient::onTickPost);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(SkyIslands.SKY_ROPE.get(), SkyRopeRenderer::new);
    }

    /** True while the local player hangs on a vine. */
    public static boolean swinging() {
        return swing != null;
    }

    private static Vec3 hand(LocalPlayer player) {
        return player.position().add(0.0, player.getBbHeight() + HAND, 0.0);
    }

    private static boolean free(LocalPlayer player) {
        return player.isAlive() && !player.isSpectator() && !player.isPassenger() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isInWater() && !player.isSleeping();
    }

    /** Used a Sky Vine (SkyVineBlock.useWithoutItem). */
    private static void grab(BlockPos pos) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && free(player) && (swing != null || cooldown == 0)) {
            attach(player, pos);
        }
    }

    private static void attach(LocalPlayer player, BlockPos pos) {
        Level level = player.level();
        SkyVines.Anchor a = SkyVines.anchor(level, pos);
        Swing old = swing;
        if (old != null && old.anchor.equals(a.block())) {
            return; // already on this one
        }
        Vec3 hand = hand(player);
        double length = a.span() ? Mth.clamp(hand.distanceTo(a.pivot()), 0.9, 2.2) : Mth.clamp(hand.distanceTo(a.pivot()), 0.9, a.reach() + 0.6);
        swing = new Swing(a, length, old != null ? old.velocity : player.getDeltaMovement());
        ClientPacketDistributor.sendToServer(SkySwing.Swing.of(true, a.block(), a.span()));
        level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, SoundEvents.VINE_STEP, SoundSource.PLAYERS, 1.0F,
                0.8F + level.getRandom().nextFloat() * 0.2F, false);
        for (int i = 0; i < 5; i++) {
            level.addParticle(ModParticles.LULLWOOD_LEAF.get(), pos.getX() + level.getRandom().nextDouble(), pos.getY() + level.getRandom().nextDouble(),
                    pos.getZ() + level.getRandom().nextDouble(), 0.0, -0.03, 0.0);
        }
        player.resetFallDistance();
    }

    /** Let go: keep the swing's speed (with a little kick when jumping off) and fly. */
    private static void release(LocalPlayer player, boolean jumped) {
        Swing s = swing;
        if (s == null) {
            return;
        }
        swing = null;
        Vec3 v = s.velocity;
        if (jumped) {
            v = v.add(look(player).scale(0.1)).add(0.0, 0.3, 0.0);
            flight = 60;
        } else {
            flight = 30;
        }
        player.setDeltaMovement(v);
        player.resetFallDistance();
        cooldown = 8;
        sinceLetGo = 0;
        lastLetGo = s.anchor;
        ClientPacketDistributor.sendToServer(SkySwing.Swing.of(false, s.anchor, s.span));
        if (v.length() > 0.45) {
            player.level().playLocalSound(player.getX(), player.getY() + 1.0, player.getZ(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.PLAYERS, 0.45F,
                    1.6F, false);
        }
    }

    private static Vec3 look(LocalPlayer player) {
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0.0, Mth.cos(yaw));
    }

    private static void onTickPre(PlayerTickEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (event.getEntity() != mc.player || mc.player == null) {
            return;
        }
        LocalPlayer player = mc.player;
        Options keys = mc.options;
        boolean jump = keys.keyJump.isDown();
        boolean jumpPressed = jump && !jumpWas;
        jumpWas = jump;
        if (cooldown > 0) {
            cooldown--;
        }
        if (sinceLetGo < 1000) {
            sinceLetGo++;
        }
        Swing s = swing;
        if (s == null) {
            fly(player);
            if (cooldown == 0 && free(player) && !player.onGround() && !keys.keyShift.isDown()) {
                Vec3 v = player.getDeltaMovement();
                if (v.y < -0.12 || v.horizontalDistanceSqr() > 0.09 || jump) {
                    BlockPos vine = touching(player);
                    if (vine != null) {
                        attach(player, vine);
                    }
                }
            }
            return;
        }
        Level level = player.level();
        s.age++;
        if (!free(player) || !SkyVines.isVine(level.getBlockState(s.anchor))) {
            release(player, false);
            return;
        }
        if (player.onGround()) {
            if (++s.grounded > 3) {
                release(player, false);
                return;
            }
        } else {
            s.grounded = 0;
        }
        if (jumpPressed && s.age > 4) {
            release(player, true);
            return;
        }
        int forward = (keys.keyUp.isDown() ? 1 : 0) - (keys.keyDown.isDown() ? 1 : 0);
        int strafe = (keys.keyRight.isDown() ? 1 : 0) - (keys.keyLeft.isDown() ? 1 : 0);
        Vec3 look = look(player);
        if (s.span && s.length <= 2.3) {
            travel(player, s, forward, look);
            forward = 0;
        } else if (keys.keyShift.isDown()) {
            if (s.length >= s.reach + 0.55) {
                release(player, false); // slid off the end of the vine
                return;
            }
            s.length = Math.min(s.reach + 0.6, s.length + 0.12);
        } else if (forward > 0 && player.getXRot() < -45.0F) {
            s.length = Math.max(0.9, s.length - 0.1);
            forward = 0;
        }
        Vec3 hand = hand(player);
        Vec3 push = SwingPhysics.push(s.velocity, forward, strafe, look, hand.subtract(s.pivot));
        SwingPhysics.State next = SwingPhysics.step(hand, s.velocity, s.pivot, s.length, push);
        Vec3 move = next.hand().subtract(hand);
        player.setDeltaMovement(move);
        s.velocity = next.velocity();
        s.before = player.position();
        s.intended = move;
        player.resetFallDistance();
    }

    /** Hand over hand along a span: the grip slides to the next span block in the direction you face. */
    private static void travel(LocalPlayer player, Swing s, int forward, Vec3 look) {
        if (forward == 0) {
            s.travelTo = null;
            return;
        }
        Level level = player.level();
        Vec3 dir = forward > 0 ? look : look.scale(-1.0);
        if (s.travelTo == null || !SkyVines.isVine(level.getBlockState(s.travelTo))) {
            s.travelTo = SkyVines.along(level, s.anchor, dir);
            if (s.travelTo == null) {
                return;
            }
        }
        Vec3 target = Vec3.atCenterOf(s.travelTo);
        Vec3 to = target.subtract(s.pivot);
        double d = to.length();
        if (d <= 0.16) {
            s.pivot = target;
            s.anchor = s.travelTo;
            s.travelTo = null;
            ClientPacketDistributor.sendToServer(SkySwing.Swing.of(true, s.anchor, true));
            if (player.tickCount % 2 == 0) {
                level.playLocalSound(target.x, target.y, target.z, SoundEvents.VINE_STEP, SoundSource.PLAYERS, 0.5F, 1.1F, false);
            }
        } else {
            s.pivot = s.pivot.add(to.scale(0.16 / d));
        }
    }

    /** After the move: whatever a wall, a ceiling or the ground stopped is gone from the swing. */
    private static void onTickPost(PlayerTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        Swing s = swing;
        if (s == null || event.getEntity() != mc.player || mc.player == null) {
            return;
        }
        LocalPlayer player = mc.player;
        Vec3 moved = player.position().subtract(s.before);
        Vec3 v = s.velocity;
        double vx = v.x;
        double vy = v.y;
        double vz = v.z;
        if (player.horizontalCollision) {
            if (Math.abs(moved.x - s.intended.x) > 0.05) {
                vx = 0.0;
            }
            if (Math.abs(moved.z - s.intended.z) > 0.05) {
                vz = 0.0;
            }
        }
        if (player.verticalCollision && Math.abs(moved.y - s.intended.y) > 0.05) {
            vy = 0.0;
        }
        s.velocity = new Vec3(vx, vy, vz);
        double speed = s.velocity.length();
        if (speed > 0.55 && s.age % 14 == 0) {
            Level level = player.level();
            level.playLocalSound(s.pivot.x, s.pivot.y, s.pivot.z, SoundEvents.VINE_STEP, SoundSource.PLAYERS, 0.35F, 0.6F + (float) speed * 0.3F, false);
        }
    }

    /** A launched player sails: the air takes little of their speed for a moment. */
    private static void fly(LocalPlayer player) {
        if (flight <= 0) {
            return;
        }
        flight--;
        if (player.onGround() || !free(player)) {
            flight = 0;
            return;
        }
        Vec3 d = player.getDeltaMovement();
        if (d.horizontalDistanceSqr() > 0.04) {
            double k = FLIGHT_KEEP / 0.91;
            player.setDeltaMovement(d.x * k, d.y, d.z * k);
        }
    }

    /** The strand you just let go of does not catch you again straight away (you jumped off it on purpose). */
    private static boolean justLetGo(BlockPos p) {
        BlockPos last = lastLetGo;
        return last != null && sinceLetGo < 24 && p.getX() == last.getX() && p.getZ() == last.getZ() && p.getY() <= last.getY();
    }

    /** The Sky Vine closest to the player's hands, if they are touching one. */
    private static @Nullable BlockPos touching(LocalPlayer player) {
        Level level = player.level();
        Vec3 hand = hand(player);
        AABB reach = new AABB(hand.x - 0.45, hand.y - 0.9, hand.z - 0.45, hand.x + 0.45, hand.y + 0.35, hand.z + 0.45);
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(BlockPos.containing(reach.minX, reach.minY, reach.minZ),
                BlockPos.containing(reach.maxX, reach.maxY, reach.maxZ))) {
            if (SkyVines.isVine(level.getBlockState(p)) && !justLetGo(p)) {
                double d = Vec3.atCenterOf(p).distanceToSqr(hand);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }
}
