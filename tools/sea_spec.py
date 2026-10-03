"""Sea & sky (agents F + W): the blocks and items of the Magic Kelp Forest, the Deep Dark Ocean and
the Sound Garden, the Gobbler's drops, every fish's raw and cooked meat and the sushi made from them.

Called once from the end of spec.py; the Java side lives in registry/ModSeaSky + ModSeaFoods.
"""

KELPS = ('rose', 'azure', 'amber')
# fish -> (raw item, cooked item); kazoo_fish / cooked_kazoo_fish already exist in spec.py
FISH = {'kazoo_fish': ('kazoo_fish', 'cooked_kazoo_fish'), 'tubafish': ('tubafish', 'cooked_tubafish'),
        'fanfare_eel': ('fanfare_eel', 'cooked_fanfare_eel'), 'gobbler': ('gobbler_fillet', 'cooked_gobbler_fillet')}
SUSHI = ('kazoo_fish_sushi', 'tubafish_sushi', 'fanfare_eel_sushi', 'gobbler_sushi')


def declare(block, item):
    sea = "BlockBehaviour.Properties.ofFullCopy(Blocks.KELP_PLANT)"
    block("coral_sand", "falling", "BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_PINK)", dust="0xF58A9C",
          tags=["shovel", "sand"], tab="nature")
    for k, colour in zip(KELPS, ("MapColor.COLOR_PINK", "MapColor.COLOR_LIGHT_BLUE", "MapColor.COLOR_ORANGE")):
        block(f"{k}_glowkelp", "custom", f"{sea}.mapColor({colour}).lightLevel(s -> s.getValue(com.thesift.block.GlowKelpBlock.TIP) ? 12 : 7)",
              cls="GlowKelpBlock", model="sea_kelp", tab="nature", name=f"{k.capitalize()} Glowkelp")
    block("abyss_anemone", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.SEAGRASS).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 9)"
          ".replaceable()", cls="WaterPlantBlock", model="sea_anemone", loot="self", tab="nature")
    block("cloud_block", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(MapColor.SNOW).strength(0.2F).sound(SoundType.WOOL)"
          ".isValidSpawn(com.thesift.block.CloudBlock::spawnable)",  # S1: glass forbids spawning; creatures live on the clouds
          cls="CloudBlock", model="glass", name="Cloud", tab="nature")
    block("chime_bell", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 6)",
          cls="ChimeBellBlock", model="sea_bell", tags=["flowers"], tab="nature")
    block("organ_reed", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.PEONY).mapColor(MapColor.COLOR_PURPLE).lightLevel(s -> 4)",
          cls="OrganReedBlock", model="sea_reed", loot="double_flower", item_kind="double", tab="nature")

    item("tubafish", props="new Item.Properties().food(ModSeaFoods.TUBAFISH)", name="Raw Tubafish")
    item("cooked_tubafish", props="new Item.Properties().food(ModSeaFoods.COOKED_TUBAFISH)")
    item("fanfare_eel", props="new Item.Properties().food(ModSeaFoods.FANFARE_EEL)", name="Raw Fanfare Eel")
    item("cooked_fanfare_eel", props="new Item.Properties().food(ModSeaFoods.COOKED_FANFARE_EEL)")
    item("gobbler_fillet", props="new Item.Properties().food(ModSeaFoods.GOBBLER_FILLET)", name="Raw Gobbler Fillet")
    item("cooked_gobbler_fillet", props="new Item.Properties().food(ModSeaFoods.COOKED_GOBBLER_FILLET)")
    item("sculk_bladder", props="new Item.Properties().rarity(Rarity.UNCOMMON)", name="Glowing Sculk Bladder")
    for s in SUSHI:
        C = s.upper()
        item(s, props=f"new Item.Properties().food(ModSeaFoods.{C}, ModSeaFoods.{C}_CONSUMABLE)")
    item("sushi_platter", props="new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON).food(ModSeaFoods.SUSHI_PLATTER, ModSeaFoods.SUSHI_PLATTER_CONSUMABLE)")
    item("gobbler_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModSeaSky.GOBBLER.get())", tab="eggs")
