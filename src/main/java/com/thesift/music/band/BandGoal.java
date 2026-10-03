package com.thesift.music.band;

import java.util.EnumSet;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Injected into a creature while it plays in a band (and taken out again when it leaves): it keeps
 * to its place in the band's half-circle behind the player - walking or swimming there with its own
 * navigation, or, for fliers, steered straight there - and watches the player play.
 */
final class BandGoal extends Goal {
    /** How close to its place counts as there. */
    private static final double SETTLED = 1.6;

    private final Mob mob;
    private final Band band;
    private final Band.Member member;
    private int repath;

    BandGoal(Mob mob, Band band, Band.Member member) {
        this.mob = mob;
        this.band = band;
        this.member = member;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    private boolean playing() {
        Player leader = this.band.leader;
        return this.member.inBand && !leader.isRemoved() && leader.level() == this.mob.level() && this.member.voice.ready(this.mob)
                && this.mob.getTarget() == null;
    }

    @Override
    public boolean canUse() {
        return this.playing();
    }

    @Override
    public boolean canContinueToUse() {
        return this.playing();
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void start() {
        this.repath = 0;
    }

    @Override
    public void stop() {
        this.mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        Player leader = this.band.leader;
        Vec3 slot = this.band.slot(this.member);
        this.mob.getLookControl().setLookAt(leader, 30.0F, 30.0F);
        double d2 = this.mob.position().distanceToSqr(slot);
        if (this.member.voice.movement() == BandVoice.Movement.FLY) {
            this.fly(slot);
            return;
        }
        // there, or (when its place cannot be reached) close enough to the player
        double close = 1.8 + this.mob.getBbWidth();
        if (d2 < SETTLED * SETTLED || d2 < 36.0 && this.mob.distanceToSqr(leader) < close * close) {
            this.mob.getNavigation().stop();
            this.repath = 0;
            return;
        }
        if (--this.repath <= 0) {
            this.repath = 8 + this.mob.getRandom().nextInt(5);
            double speed = d2 > 12.0 * 12.0 ? 1.4 : d2 > 5.0 * 5.0 ? 1.2 : 1.0;
            if (!this.mob.getNavigation().moveTo(slot.x, slot.y, slot.z, speed)) {
                this.mob.getNavigation().moveTo(leader, speed);
            }
        }
    }

    /** Fliers (also the ones that steer themselves) are pulled towards their place. */
    private void fly(Vec3 slot) {
        Vec3 to = slot.subtract(this.mob.position());
        double d = to.length();
        double max = Math.max(0.12, Math.min(0.45, this.mob.getBbWidth() * 0.09 + 0.12));
        Vec3 want = d < 0.4 ? Vec3.ZERO : to.scale(Math.min(max, d * 0.07) / d);
        Vec3 v = this.mob.getDeltaMovement();
        this.mob.setDeltaMovement(v.add(want.subtract(v).scale(0.18)));
        if (want.horizontalDistanceSqr() > 1.0E-4) {
            float yaw = (float) (Mth.atan2(want.z, want.x) * Mth.RAD_TO_DEG) - 90.0F;
            this.mob.setYRot(this.mob.getYRot() + Mth.clamp(Mth.wrapDegrees(yaw - this.mob.getYRot()), -6.0F, 6.0F));
            this.mob.yBodyRot = this.mob.getYRot();
        }
    }
}
