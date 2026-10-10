package com.thesift.entity.dunes;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * The Monarch Reservoir: a rare Reservoir grown huge (some four blocks wide and taller than a tree), friendly
 * and rooted. Its spines do not prick, it never bursts, its flower is always open, it holds four times the
 * Chrome and fills twice as fast - and it shares it freely with a bucket. It hums deeply.
 */
public class MonarchReservoir extends Reservoir {
    public MonarchReservoir(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.xpReward = 20;
    }

    public static AttributeSupplier.Builder createMonarchAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 160.0)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.ARMOR, 8.0);
    }

    @Override
    public boolean isMonarch() {
        return true;
    }

    @Override
    public int capacity() {
        return 400;
    }

    /** It never moves. */
    @Override
    public boolean isResting() {
        return true;
    }
}
