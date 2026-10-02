package com.thesift.block;

import com.thesift.registry.ModTags;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.pathfinder.PathType;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;

/**
 * Chrome: a thick liquid rainbow. Bathing in it leaves you Rainbow Dazed (it no longer heals), and
 * it is thick like quicksand: you slowly sink and cannot swim up. Hold Sneak to wade back up.
 */
public class ChromeFluidType extends FluidType {
    public ChromeFluidType() {
        super(FluidType.Properties.create()
                .descriptionId("block.thesift.chrome")
                .motionScale(0.004)
                .canPushEntity(true)
                .canSwim(false)
                .canDrown(false)
                .fallDistanceModifier(0.0F)
                .canExtinguish(true)
                .canConvertToSource(false)
                .supportsBoating(false)
                .canHydrate(true)
                .lightLevel(8)
                .density(3000)
                .viscosity(5000)
                .temperature(310)
                .rarity(Rarity.UNCOMMON)
                .pathType(PathType.WATER)
                .adjacentPathType(null)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL_POWDER_SNOW)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY_POWDER_SNOW)
                .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.AMETHYST_BLOCK_CHIME));
    }

    @Override
    public boolean move(LivingEntity entity, Vec3 movementVector, double gravity) {
        if (entity.getType().builtInRegistryHolder().is(ModTags.Entities.CHROME_DWELLERS)) {
            // Slumblers glide through Chrome like water.
            entity.moveRelative(0.035F, movementVector);
            entity.move(MoverType.SELF, entity.getDeltaMovement());
            Vec3 v = entity.getDeltaMovement();
            entity.setDeltaMovement(v.x * 0.82, v.y * 0.82 + (entity.horizontalCollision ? 0.05 : 0.0), v.z * 0.82);
            return true;
        }

        entity.moveRelative(0.012F, movementVector);
        entity.move(MoverType.SELF, entity.getDeltaMovement());
        Vec3 v = entity.getDeltaMovement();
        double y;
        if (entity.isShiftKeyDown()) {
            // Wading up through the thick liquid.
            y = Math.min(v.y + 0.022, entity.horizontalCollision ? 0.2 : 0.11);
        } else {
            // Quicksand-like: a slow, steady sink.
            y = Math.max(v.y * 0.55 - 0.011, -0.055);
        }
        entity.setDeltaMovement(v.x * 0.55, y, v.z * 0.55);
        entity.resetFallDistance();
        return true;
    }

    @Override
    public boolean canExtinguish(Entity entity) {
        return true;
    }

    @Override
    public void setItemMovement(ItemEntity entity) {
        Vec3 v = entity.getDeltaMovement();
        entity.setDeltaMovement(v.x * 0.9, v.y + (v.y < 0.03 ? 5.0E-4 : 0.0), v.z * 0.9);
    }
}
