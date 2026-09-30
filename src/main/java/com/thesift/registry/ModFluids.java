package com.thesift.registry;

import com.thesift.TheSift;
import com.thesift.block.ChromeFluidType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModFluids {
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, TheSift.MODID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(BuiltInRegistries.FLUID, TheSift.MODID);

    public static final DeferredHolder<FluidType, FluidType> CHROME_TYPE = FLUID_TYPES.register("chrome", ChromeFluidType::new);

    public static final DeferredHolder<Fluid, FlowingFluid> CHROME = FLUIDS.register("chrome", () -> new BaseFlowingFluid.Source(properties()));
    public static final DeferredHolder<Fluid, FlowingFluid> FLOWING_CHROME = FLUIDS.register("flowing_chrome", () -> new BaseFlowingFluid.Flowing(properties()));

    private static BaseFlowingFluid.Properties properties() {
        return new BaseFlowingFluid.Properties(CHROME_TYPE::value, CHROME, FLOWING_CHROME)
                .bucket(ModItems.CHROME_BUCKET)
                .block(ModBlocks.CHROME)
                .slopeFindDistance(3)
                .levelDecreasePerBlock(2)
                .tickRate(15)
                .explosionResistance(100.0F);
    }

    private ModFluids() {}
}
