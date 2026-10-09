package com.thesift.world.sky;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * W-sky: the Sky Islands - the Sift's sky biome ({@code thesift:sky_island}, once the Sound Garden). Giant floating
 * islands (the density function thesift:sift/sky_islands, tools/sky_islands.py) joined by Sky Roots and swingable Sky
 * Vines ({@link SkySwing}); Skypalms whose vines link tree to tree; Cloudpuff trees; giant Driftfruits at the ends of
 * vines; Skyrinds in bunches under the fronds.
 *
 * <p>Phase 4 hooks: {@link #SKY_WHALE_FOOD} (what a Sky Whale grazes on), {@link #SWINGER_FOOD} (what tames a Swinger),
 * {@link #SWINGABLE} and {@link SkyVines#anchor} (where a creature can hang and swing), {@link SwingPhysics} (the rope
 * physics, reusable for anything else a player may grab later, such as the Macho Dunkos' tail).
 */
public final class SkyIslands {
    public static final DeferredRegister<MapCodec<? extends Feature>> FEATURE_TYPES = DeferredRegister.create(Registries.FEATURE_TYPE, TheSift.MODID);
    public static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(TheSift.MODID);

    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SkyBridgeFeature>> SKY_BRIDGE = FEATURE_TYPES.register("sky_bridge",
            () -> SkyBridgeFeature.CODEC);
    public static final DeferredHolder<MapCodec<? extends Feature>, MapCodec<SkyTreeFeature>> SKY_TREE = FEATURE_TYPES.register("sky_tree",
            () -> SkyTreeFeature.CODEC);

    /** The vine a swinging player holds: drawn from where it hangs to the player's hands (never saved). */
    public static final DeferredHolder<EntityType<?>, EntityType<SkyRope>> SKY_ROPE = ENTITIES.registerEntityType("sky_rope", SkyRope::new,
            MobCategory.MISC, b -> b.sized(0.25F, 0.25F).clientTrackingRange(8).updateInterval(2).noSave().noSummon().fireImmune());

    /** Blocks a Sky Whale grazes on (the Driftfruit). */
    public static final TagKey<Block> SKY_WHALE_FOOD = TagKey.create(Registries.BLOCK, TheSift.id("sky_whale_food"));
    /** Items that tame a Swinger (the Skyrind). */
    public static final TagKey<Item> SWINGER_FOOD = TagKey.create(Registries.ITEM, TheSift.id("swinger_food"));
    /** Blocks a player (or a Swinger) can grab and swing from. */
    public static final TagKey<Block> SWINGABLE = TagKey.create(Registries.BLOCK, TheSift.id("swingable"));

    public static final ResourceKey<Feature> SKYPALM_TREE = ResourceKey.create(Registries.FEATURE, TheSift.id("skypalm_tree"));
    public static final ResourceKey<Feature> CLOUD_TREE = ResourceKey.create(Registries.FEATURE, TheSift.id("cloud_tree"));
    public static final TreeGrower SKYPALM = new TreeGrower("thesift_skypalm", WeightedList.of(SKYPALM_TREE),
            WeightedList.<ResourceKey<Feature>>of(), WeightedList.<ResourceKey<Feature>>of(), SKYPALM_TREE);
    public static final TreeGrower CLOUDPUFF = new TreeGrower("thesift_cloudpuff", WeightedList.of(CLOUD_TREE),
            WeightedList.<ResourceKey<Feature>>of(), WeightedList.<ResourceKey<Feature>>of(), CLOUD_TREE);

    private SkyIslands() {
    }

    public static void register(IEventBus modBus) {
        FEATURE_TYPES.register(modBus);
        ENTITIES.register(modBus);
        SkySwing.register(modBus);
    }
}
