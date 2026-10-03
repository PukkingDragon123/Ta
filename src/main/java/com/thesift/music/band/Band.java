package com.thesift.music.band;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * One player's band (server side): its members, and the half-circle they stand in behind the
 * player. The half-circle turns only when the player walks off somewhere, never when they just
 * look around to aim their notes.
 */
final class Band {
    final Player leader;
    final List<Member> members = new ArrayList<>();
    /** Where the player stood when the band last turned to face their way, and that way (radians). */
    private Vec3 anchor;
    private double heading;
    /** Notes the members have played along with (for the CI check). */
    int notes;
    /** Game time of the last note the player played. */
    long lastNote;
    boolean dirty = true;

    Band(Player leader) {
        this.leader = leader;
        this.anchor = leader.position();
        this.heading = Math.toRadians(leader.getYRot() + 90.0F);
    }

    /** One creature in the band. */
    static final class Member {
        final Mob mob;
        final BandVoice voice;
        final boolean owned;
        final long joined;
        long staysUntil;
        int heard;
        boolean inBand = true;
        BandGoal goal;

        Member(Mob mob, BandVoice voice, boolean owned, long joined, long staysUntil) {
            this.mob = mob;
            this.voice = voice;
            this.owned = owned;
            this.joined = joined;
            this.staysUntil = staysUntil;
        }

        /** How much of its stay is left, 0..1, for the HUD. */
        float stayLeft(long now) {
            long span = this.owned ? Bands.OWNED_STAY_TICKS : Bands.STAY_TICKS;
            return Mth.clamp((this.staysUntil - now) / (float) span, 0.0F, 1.0F);
        }
    }

    /** Turns the half-circle when the player has walked on: it trails behind the way they went. */
    void follow() {
        Vec3 at = this.leader.position();
        double dx = at.x - this.anchor.x;
        double dz = at.z - this.anchor.z;
        if (dx * dx + dz * dz > 16.0) {
            this.heading = Math.atan2(dz, dx);
            this.anchor = at;
        }
    }

    /**
     * Where this member stands: on a half-circle behind the player (bigger creatures further out),
     * fliers above it - the Sky Whale far above.
     */
    Vec3 slot(Member m) {
        int n = this.members.size();
        int i = Math.max(0, this.members.indexOf(m));
        double spread = n <= 1 ? 0.0 : (i / (double) (n - 1) - 0.5) * Math.toRadians(150.0);
        double a = this.heading + Math.PI + spread;
        double w = m.mob.getBbWidth();
        double r = 2.4 + w * 0.8 + (i % 2) * 0.9;
        double y = this.leader.getY();
        if (m.voice.movement() == BandVoice.Movement.FLY) {
            boolean huge = w > 2.5;
            r = huge ? 4.0 + w : r * 0.8;
            y += huge ? 9.0 + m.mob.getBbHeight() : 1.6 + Mth.sin((this.leader.tickCount + i * 20) * 0.05F) * 0.3;
        }
        Vec3 p = this.leader.position();
        return new Vec3(p.x + Math.cos(a) * r, y, p.z + Math.sin(a) * r);
    }
}
