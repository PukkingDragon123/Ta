package com.thesift.item;

import com.thesift.music.Instrument;
import net.minecraft.world.item.Item;

/**
 * The Prism Drum: the conga drum hooped in prism gems. Its shockwave rolls half as far again,
 * shatters a wider ring of soft blocks, hits harder and rings out sooner.
 */
public class PrismDrumItem extends CongaDrumItem {
    public PrismDrumItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected double radius() {
        return 12.0;
    }

    @Override
    protected int breakRadius() {
        return 4;
    }

    @Override
    protected float maxDamage() {
        return 16.0F;
    }

    @Override
    protected int cooldown() {
        return 400;
    }

    @Override
    protected Instrument instrument() {
        return Instrument.PRISM_DRUM;
    }
}
