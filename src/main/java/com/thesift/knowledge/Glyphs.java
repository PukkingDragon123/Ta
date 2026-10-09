package com.thesift.knowledge;

import net.minecraft.util.Mth;

/**
 * F3: the ancient script. Parts of the old texts are written in enchanting-table glyphs (the vanilla {@code alt} font).
 * Which words are in the script is fixed for each text (by its origin's share); how many of those the player can read
 * grows with every Lore Book and Scroll found ({@link #level}). A word is readable once the player's level passes its
 * own threshold, so each new clue turns a few more words to English.
 */
public final class Glyphs {
    private Glyphs() {
    }

    /** How much of the script this many clues (lore pieces read) let you read: a little at first, all of it after twelve. */
    public static float level(int clues) {
        return Mth.clamp(0.08F + clues * 0.077F, 0.0F, 1.0F);
    }

    private static float hash(String text, int word, int salt) {
        int h = text.hashCode() * 31 + word * 0x9E3779B1 + salt * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /** True if word {@code word} of text {@code text} is written in the ancient script at all. */
    public static boolean ancient(String text, int word, float share) {
        return share > 0.0F && hash(text, word, 1) < share;
    }

    /** The level at which an ancient word becomes readable (0..1). */
    public static float threshold(String text, int word) {
        return hash(text, word, 2) * 0.999F;
    }

    /** True if the word is shown in glyphs at this level of understanding. */
    public static boolean hidden(String text, int word, float share, float level) {
        return ancient(text, word, share) && threshold(text, word) >= level;
    }
}
