package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.registry.ModSculkSwamp;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.block.BlockAndTintGetter;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.fog.FogData;
import net.minecraft.client.renderer.fog.environment.FogEnvironment;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterFluidModelsEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Vector4f;
import org.jspecify.annotations.Nullable;

/**
 * W1 World &amp; terrain, client side: Sculk Water's look (its own dark teal, mote-filled textures,
 * the murky fog and overlay when your eyes are under it) and the Sculk Swamp's air - a low teal
 * mist that closes in as you walk in and lifts as you leave (eased, so it never snaps).
 */
public final class SculkSwampClient {
    /** Sculk Water carries its colour in its textures: no tint. */
    public static final FluidTintSource TINT = new FluidTintSource() {
        @Override
        public int color(FluidState state) {
            return 0xFFFFFFFF;
        }

        @Override
        public int colorInWorld(FluidState fluidState, BlockState blockState, BlockAndTintGetter level, BlockPos pos) {
            return 0xFFFFFFFF;
        }
    };
    /** How far into the Sculk Swamp the camera is, 0-1. */
    private static float swamp;

    private SculkSwampClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(SculkSwampClient::registerFluidModels);
        modBus.addListener(SculkSwampClient::registerClientExtensions);
        NeoForge.EVENT_BUS.addListener(SculkSwampClient::fogColour);
        NeoForge.EVENT_BUS.addListener(SculkSwampClient::fog);
    }

    private static void registerFluidModels(RegisterFluidModelsEvent event) {
        event.register(new FluidModel.Unbaked(new Material(TheSift.id("block/sculk_water_still")), new Material(TheSift.id("block/sculk_water_flow")),
                new Material(TheSift.id("block/sculk_water_overlay")), TINT), ModSculkSwamp.SCULK_WATER, ModSculkSwamp.FLOWING_SCULK_WATER);
    }

    private static void registerClientExtensions(RegisterClientExtensionsEvent event) {
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final Identifier OVERLAY = TheSift.id("textures/misc/in_sculk_water.png");

            @Override
            public Identifier getRenderOverlayTexture(Minecraft mc) {
                return OVERLAY;
            }

            @Override
            public void modifyFogColor(Camera camera, float partialTick, ClientLevel level, int renderDistance, float darkenWorldAmount,
                    Vector4f fluidFogColor) {
                // murky teal, with a slow glow pulsing through it
                float pulse = 0.5F + 0.5F * Mth.sin((level.getGameTime() + partialTick) * 0.05F);
                fluidFogColor.set(0.03F + 0.02F * pulse, 0.17F + 0.05F * pulse, 0.19F + 0.06F * pulse, 1.0F);
            }

            @Override
            public void modifyFogRender(Camera camera, @Nullable FogEnvironment environment, float renderDistance, float partialTick, FogData fogData) {
                fogData.environmentalStart = -2.0F;
                fogData.environmentalEnd = 15.0F;
            }
        }, ModSculkSwamp.SCULK_WATER_TYPE.get());
    }

    /** In the swamp the fog turns a dim sculk teal... */
    private static void fogColour(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        boolean inside = mc.level != null && mc.player != null && mc.level.getBiome(mc.player.blockPosition()).is(ModSculkSwamp.SCULK_SWAMP);
        swamp = Mth.clamp(swamp + (inside ? 0.008F : -0.008F), 0.0F, 1.0F);
        if (swamp <= 0.0F) {
            return;
        }
        float k = swamp * 0.65F;
        event.setRed(Mth.lerp(k, event.getRed(), 0.21F));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.36F));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.37F));
    }

    /** ...and closes in, so the far trees are only shapes in the mist. */
    private static void fog(ViewportEvent.RenderFog event) {
        if (swamp <= 0.0F) {
            return;
        }
        event.setFarPlaneDistance(Mth.lerp(swamp, event.getFarPlaneDistance(), Math.min(event.getFarPlaneDistance(), 56.0F)));
        event.setNearPlaneDistance(Mth.lerp(swamp, event.getNearPlaneDistance(), 0.0F));
    }
}
