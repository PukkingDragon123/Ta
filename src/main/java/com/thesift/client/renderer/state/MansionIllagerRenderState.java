package com.thesift.client.renderer.state;

import net.minecraft.client.renderer.entity.state.IllagerRenderState;

/** MANSION: the Hornblower and the Bard - the vanilla illager's state, and what their instruments are doing. */
public class MansionIllagerRenderState extends IllagerRenderState {
    /** True while the horn is at the Hornblower's lips / the Bard's guitar is raised to play. */
    public boolean playing;
    /** Ticks since the Hornblower's last blast / the Bard's last healing chord (large when long ago). */
    public float sinceFlourish = 1000.0F;
    /** Which of the two this is (their poses differ). */
    public boolean bard;
}
