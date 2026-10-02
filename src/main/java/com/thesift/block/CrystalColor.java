package com.thesift.block;

import java.util.Locale;
import net.minecraft.util.StringRepresentable;

/**
 * The five colours of music crystal - and of the Caravans that grow them. Each colour rings in its
 * own part of the scale (amber lowest, gold highest); tools/caravans.py paints them in this order.
 */
public enum CrystalColor implements StringRepresentable {
    AMBER(0xFFB43A, 4),
    ROSE(0xFF7FAE, 8),
    TEAL(0x4FF0DC, 11),
    VIOLET(0xB98CFF, 15),
    GOLD(0xFFE45A, 18);

    /** Pentatonic steps above the colour's base note, so neighbouring crystals always sound well together. */
    private static final int[] STEPS = {0, 2, 4, 7};

    private final int rgb;
    private final int base;

    CrystalColor(int rgb, int base) {
        this.rgb = rgb;
        this.base = base;
    }

    public int rgb() {
        return this.rgb;
    }

    /** The note (0-24) a crystal of this colour rings with, varied by a small per-crystal step. */
    public int note(long seed) {
        return Math.min(24, this.base + STEPS[(int) Math.floorMod(seed, (long) STEPS.length)]);
    }

    public static CrystalColor byId(int id) {
        CrystalColor[] all = values();
        return all[Math.floorMod(id, all.length)];
    }

    @Override
    public String getSerializedName() {
        return this.name().toLowerCase(Locale.ROOT);
    }
}
