package com.thesift.block;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.pathfinder.PathType;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * W1 Sculk Water: the dark teal water of the Sculk Swamp. It swims, drowns and floats boats like
 * water, only heavier and slower; soaking in it slowly corrupts you (see {@link SculkWaterBlock}),
 * and its glowing motes, fog and colour live in client/SculkSwampClient.
 */
public class SculkWaterFluidType extends FluidType {
    public SculkWaterFluidType() {
        super(FluidType.Properties.create()
                .descriptionId("block.thesift.sculk_water")
                .motionScale(0.009)
                .canPushEntity(true)
                .canSwim(true)
                .canDrown(true)
                .fallDistanceModifier(0.0F)
                .canExtinguish(true)
                .canConvertToSource(true)
                .supportsBoating(true)
                .canHydrate(false)
                .lightLevel(2)
                .density(1300)
                .viscosity(1600)
                .temperature(290)
                .rarity(Rarity.UNCOMMON)
                .pathType(PathType.WATER)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH));
    }

    @Override
    public boolean canExtinguish(Entity entity) {
        return true;
    }
}
