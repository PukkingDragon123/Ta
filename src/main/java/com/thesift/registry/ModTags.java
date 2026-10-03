package com.thesift.registry;

import com.thesift.TheSift;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;

public final class ModTags {
    public static final class Blocks {
        /** Soils Sift plants happily take root in. */
        public static final TagKey<Block> SIFT_PLANTABLE = tag("sift_plantable");
        /** Blocks that can outline a Sift portal (reinforced deepslate of Ancient Cities and Echo Frames). */
        public static final TagKey<Block> PORTAL_FRAME = tag("portal_frame");
        /** Blocks Glowcaps and glowbells grow on in the Deep Sift. */
        public static final TagKey<Block> DEEP_SIFT_GROUND = tag("deep_sift_ground");
        public static final TagKey<Block> SIFT_STONE = tag("sift_stone");
        public static final TagKey<Block> SERBIM_ORES = tag("serbim_ores");
        public static final TagKey<Block> INCORRECT_FOR_SIFTITE_TOOL = tag("incorrect_for_siftite_tool");
        public static final TagKey<Block> RESONANT = tag("resonant");
        /** Soft ground a ridden Sniffer ploughs straight through. */
        public static final TagKey<Block> SNIFFER_MINEABLE = tag("sniffer_mineable");
        /** What a ridden Sniffer sniffs out: buried and hidden treasure. */
        public static final TagKey<Block> SNIFFER_TREASURE = tag("sniffer_treasure");

        private static TagKey<Block> tag(String name) {
            return TagKey.create(Registries.BLOCK, TheSift.id(name));
        }
    }

    public static final class Items {
        public static final TagKey<Item> BULB_FOOD = tag("bulb_food");
        /** Seeds a Harmoner will eat - and then lead you to the structure of its colour. */
        public static final TagKey<Item> HARMONER_FOOD = tag("harmoner_food");
        public static final TagKey<Item> SLUMBLER_FOOD = tag("slumbler_food");
        public static final TagKey<Item> SLINGSHOT_AMMO = tag("slingshot_ammo");
        public static final TagKey<Item> SIFTITE_TOOL_MATERIALS = tag("siftite_tool_materials");
        public static final TagKey<Item> REPAIRS_SIFTITE_ARMOR = tag("repairs_siftite_armor");
        public static final TagKey<Item> ALTAR_FUEL = tag("altar_fuel");

        private static TagKey<Item> tag(String name) {
            return TagKey.create(Registries.ITEM, TheSift.id(name));
        }
    }

    public static final class Entities {
        public static final TagKey<EntityType<?>> CHROME_DWELLERS = tag("chrome_dwellers");
        public static final TagKey<EntityType<?>> MUSIC_LOVERS = tag("music_lovers");

        private static TagKey<EntityType<?>> tag(String name) {
            return TagKey.create(Registries.ENTITY_TYPE, TheSift.id(name));
        }
    }

    public static final class Biomes {
        public static final TagKey<Biome> IS_SIFT = tag("is_sift");

        private static TagKey<Biome> tag(String name) {
            return TagKey.create(Registries.BIOME, TheSift.id(name));
        }
    }

    private ModTags() {}
}
