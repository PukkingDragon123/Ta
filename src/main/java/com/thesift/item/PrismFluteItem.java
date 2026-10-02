package com.thesift.item;

import com.thesift.music.Instrument;
import net.minecraft.world.item.Item;

/**
 * The Prism Flute: the crane flute inlaid with prism gems. Its beam reaches further, hits harder,
 * shimmers through every colour - and pierces, running on through its target to hurt everything
 * along the line.
 */
public class PrismFluteItem extends CraneFluteItem {
    public PrismFluteItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected double range() {
        return 32.0;
    }

    @Override
    protected float pulseDamage() {
        return 2.5F;
    }

    @Override
    protected boolean piercing() {
        return true;
    }

    @Override
    protected Instrument instrument() {
        return Instrument.PRISM_FLUTE;
    }

    @Override
    protected int beamColour(long time) {
        return prismColour(time);
    }
}
