package com.thesift;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(TheSift.MODID)
public class TheSift {
    public static final String MODID = "thesift";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TheSift(IEventBus modBus, ModContainer container) {
        LOGGER.info("The Sift is dreaming...");
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MODID, path);
    }
}
