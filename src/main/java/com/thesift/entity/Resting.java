package com.thesift.entity;

/**
 * CR2: a creature that sometimes lies still on purpose - napping, curled up, settled in - rather than
 * being stuck. The CI's never-frozen check (dev/CreatureCheck) lets a resting creature off.
 */
public interface Resting {
    /** True while it is resting of its own accord (it will get up again by itself). */
    boolean isResting();
}
