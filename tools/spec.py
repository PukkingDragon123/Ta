"""Single source of truth for The Sift's standard blocks and items.

Everything that follows a vanilla block family (stone sets, wood sets, flora, ores ...) is declared
here once. gen_java.py turns it into registry code and gen_assets.py into blockstates, models,
item definitions, loot tables, tags, recipes and lang entries. Bespoke blocks with their own
classes are still declared here (kind='custom') so that their assets are generated the same way.
"""

MODID = "thesift"

BLOCKS = []   # ordered list of dicts
ITEMS = []    # ordered list of dicts (non-block items)
LANG = {}


def title(s):
    small = {"of", "the", "and"}
    words = s.replace("_", " ").split()
    return " ".join(w if (i and w in small) else w.capitalize() for i, w in enumerate(words))


def block(id, kind="cube", props=None, **kw):
    d = dict(id=id, kind=kind, props=props or "", **kw)
    d.setdefault("item", True)
    d.setdefault("loot", "self")
    d.setdefault("tags", [])
    d.setdefault("name", title(id))
    d.setdefault("tab", "blocks")
    BLOCKS.append(d)
    return d


def item(id, **kw):
    d = dict(id=id, **kw)
    d.setdefault("name", title(id))
    d.setdefault("model", "generated")
    d.setdefault("tab", "items")
    ITEMS.append(d)
    return d


# ---------------------------------------------------------------- stone families
STONE = "BlockBehaviour.Properties.ofFullCopy(Blocks.STONE)"
DEEP = "BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE)"
DEEP_BRICK = "BlockBehaviour.Properties.ofFullCopy(Blocks.DEEPSLATE_BRICKS)"
SANDSTONE = "BlockBehaviour.Properties.ofFullCopy(Blocks.SANDSTONE)"
BRICK = "BlockBehaviour.Properties.ofFullCopy(Blocks.BRICKS)"


def stone_set(base, props, tex=None, stairs=True, slab=True, wall=True, mapcolor=None, kind="cube", tags=("pickaxe",), loot="self", **kw):
    """base block + optional stairs/slab/wall. `base` like 'dreamstone_bricks'; shape prefix drops plural."""
    p = props + (f".mapColor({mapcolor})" if mapcolor else "")
    b = block(base, kind, p, tex=tex or base, tags=list(tags), loot=loot, **kw)
    stem = base[:-1] if base.endswith("bricks") or base.endswith("tiles") else base
    if stairs:
        block(stem + "_stairs", "stairs", p, basis=base, tex=tex or base, tags=list(tags))
    if slab:
        block(stem + "_slab", "slab", p, basis=base, tex=tex or base, tags=list(tags), loot="slab")
    if wall:
        block(stem + "_wall", "wall", p, basis=base, tex=tex or base, tags=list(tags) + ["walls"])
    return b


DREAM_COLOR = "MapColor.COLOR_LIGHT_BLUE"
stone_set("dreamstone", STONE, mapcolor=DREAM_COLOR, wall=False, loot="drop:cobbled_dreamstone", tags=("pickaxe", "sift_stone"))
stone_set("cobbled_dreamstone", STONE, mapcolor=DREAM_COLOR)
stone_set("polished_dreamstone", STONE, mapcolor=DREAM_COLOR)
stone_set("dreamstone_bricks", STONE, mapcolor=DREAM_COLOR)
stone_set("cracked_dreamstone_bricks", STONE, mapcolor=DREAM_COLOR, stairs=False, slab=False, wall=False)
stone_set("mossy_dreamstone_bricks", STONE, mapcolor=DREAM_COLOR)
stone_set("dreamstone_tiles", STONE, mapcolor=DREAM_COLOR)
block("chiseled_dreamstone", "cube", STONE + f".mapColor({DREAM_COLOR})", tags=["pickaxe"])
block("dreamstone_pillar", "pillar", STONE + f".mapColor({DREAM_COLOR})", tags=["pickaxe"])

HUSH_COLOR = "MapColor.COLOR_CYAN"
block("hushslate", "pillar", DEEP + f".mapColor({HUSH_COLOR})", tags=["pickaxe", "sift_stone"], loot="drop:cobbled_hushslate")
stone_set("cobbled_hushslate", DEEP, mapcolor=HUSH_COLOR)
stone_set("polished_hushslate", DEEP, mapcolor=HUSH_COLOR)
stone_set("hushslate_bricks", DEEP_BRICK, mapcolor=HUSH_COLOR)
stone_set("cracked_hushslate_bricks", DEEP_BRICK, mapcolor=HUSH_COLOR, stairs=False, slab=False, wall=False)
stone_set("hushslate_tiles", DEEP_BRICK, mapcolor=HUSH_COLOR)
block("chiseled_hushslate", "cube", DEEP_BRICK + f".mapColor({HUSH_COLOR})", tags=["pickaxe"])

SAND_COLOR = "MapColor.COLOR_PINK"
block("dreamsand", "falling", "BlockBehaviour.Properties.ofFullCopy(Blocks.SAND).mapColor(MapColor.COLOR_PINK)",
      dust="0xF2A7C3", tags=["shovel", "sand"])
block("suspicious_dreamsand", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.SUSPICIOUS_SAND).mapColor(MapColor.COLOR_PINK)",
      cls="SuspiciousDreamsandBlock", model="suspicious", tags=["shovel"], loot="none", item_kind="block")
stone_set("dreamsandstone", SANDSTONE, mapcolor=SAND_COLOR, kind="sandstone")
stone_set("smooth_dreamsandstone", SANDSTONE, mapcolor=SAND_COLOR, wall=False, kind="cube")
stone_set("cut_dreamsandstone", SANDSTONE, mapcolor=SAND_COLOR, stairs=False, wall=False, kind="sandstone_cut")
block("chiseled_dreamsandstone", "sandstone_chiseled", SANDSTONE + f".mapColor({SAND_COLOR})", tags=["pickaxe"])

BLUSH_COLOR = "MapColor.COLOR_RED"
stone_set("blush_bricks", BRICK, mapcolor=BLUSH_COLOR)
block("cracked_blush_bricks", "cube", BRICK + f".mapColor({BLUSH_COLOR})", tags=["pickaxe"])
block("chiseled_blush_bricks", "cube", BRICK + f".mapColor({BLUSH_COLOR})", tags=["pickaxe"])

# ---------------------------------------------------------------- soils & mosses
block("sift_soil", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.DIRT).mapColor(MapColor.TERRACOTTA_PINK)", tags=["shovel", "dirt"])
block("sift_grass_block", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_PINK)",
      cls="SiftGrassBlock", model="grass_block", tags=["shovel", "dirt"], loot="silk:sift_soil")
block("coral_turf", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.GRASS_BLOCK).mapColor(MapColor.COLOR_PINK)",
      cls="SiftGrassBlock", model="grass_block", tags=["shovel", "dirt"], loot="silk:sift_soil")
block("lumen_moss_block", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_BLOCK).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 6)",
      cls="LumenMossBlock", model="cube_all", tags=["hoe", "dirt"])
block("lumen_moss_carpet", "carpet", "BlockBehaviour.Properties.ofFullCopy(Blocks.MOSS_CARPET).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 4)",
      tex="lumen_moss_block", tags=["hoe"])

# ---------------------------------------------------------------- ores & metals
block("serbim_ore", "ore", STONE + ".mapColor(MapColor.COLOR_LIGHT_BLUE).strength(3.0F, 3.0F).requiresCorrectToolForDrops()",
      tags=["pickaxe", "needs_iron", "serbim_ores"], loot="ore:raw_serbim")
block("deep_serbim_ore", "ore", DEEP + ".mapColor(MapColor.COLOR_CYAN).strength(4.5F, 3.0F).requiresCorrectToolForDrops()",
      tags=["pickaxe", "needs_iron", "serbim_ores"], loot="ore:raw_serbim")
block("raw_serbim_block", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.RAW_IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_BLUE)", tags=["pickaxe", "needs_iron"])
block("serbim_block", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_LIGHT_BLUE)", tags=["pickaxe", "needs_iron", "beacon"])
block("siftite_block", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.NETHERITE_BLOCK).mapColor(MapColor.COLOR_CYAN)", tags=["pickaxe", "needs_diamond", "beacon"])
block("chrome_glass", "glass", "BlockBehaviour.Properties.ofFullCopy(Blocks.GLASS).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 3)",
      tags=["glass"], loot="silk")

# ---------------------------------------------------------------- wood sets
WOODS = {
    "lullwood": dict(bark="MapColor.COLOR_GRAY", plank="MapColor.COLOR_LIGHT_GRAY", leaves_particle="ModParticles.LULLWOOD_LEAF",
                     leaf_chance="0.03F", sapling_on="sift"),
    "wishwood": dict(bark="MapColor.COLOR_PINK", plank="MapColor.COLOR_PINK", leaves_particle="ModParticles.WISHWOOD_LEAF",
                     leaf_chance="0.02F", sapling_on="sift"),
}
for w, c in WOODS.items():
    LOG = f"BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_LOG).mapColor({c['bark']})"
    PL = f"BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PLANKS).mapColor({c['plank']})"
    block(f"{w}_log", "log", LOG, tags=["axe", f"{w}_logs", "logs_that_burn"], wood=w, strip=f"stripped_{w}_log")
    block(f"{w}_wood", "wood", LOG, tex=f"{w}_log", tags=["axe", f"{w}_logs", "logs_that_burn"], wood=w, strip=f"stripped_{w}_wood")
    block(f"stripped_{w}_log", "log", LOG, tags=["axe", f"{w}_logs", "logs_that_burn"], wood=w)
    block(f"stripped_{w}_wood", "wood", LOG, tex=f"stripped_{w}_log", tags=["axe", f"{w}_logs", "logs_that_burn"], wood=w)
    block(f"{w}_planks", "cube", PL, tags=["axe", "planks"], wood=w)
    block(f"{w}_stairs", "stairs", PL, basis=f"{w}_planks", tex=f"{w}_planks", tags=["axe", "wooden_stairs"], wood=w)
    block(f"{w}_slab", "slab", PL, basis=f"{w}_planks", tex=f"{w}_planks", tags=["axe", "wooden_slabs"], loot="slab", wood=w)
    block(f"{w}_fence", "fence", PL, tex=f"{w}_planks", tags=["axe", "wooden_fences"], wood=w)
    block(f"{w}_fence_gate", "fence_gate", PL, tex=f"{w}_planks", tags=["axe", "fence_gates"], wood=w)
    block(f"{w}_door", "door", PL + ".noOcclusion()", tags=["axe", "wooden_doors"], loot="door", wood=w)
    block(f"{w}_trapdoor", "trapdoor", PL + ".noOcclusion()", tags=["axe", "wooden_trapdoors"], wood=w)
    block(f"{w}_button", "button", "BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_BUTTON)", tex=f"{w}_planks", tags=["axe", "wooden_buttons"], wood=w)
    block(f"{w}_pressure_plate", "pressure_plate", "BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_PRESSURE_PLATE)", tex=f"{w}_planks",
          tags=["axe", "wooden_pressure_plates"], wood=w)
    block(f"{w}_leaves", "leaves", "BlockBehaviour.Properties.ofFullCopy(Blocks.CHERRY_LEAVES).mapColor(MapColor.COLOR_LIGHT_BLUE)" if w == "wishwood"
          else "BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_OAK_LEAVES).mapColor(MapColor.SNOW)",
          tags=["hoe", "leaves"], loot=f"leaves:{w}_sapling", particle=c["leaves_particle"], chance=c["leaf_chance"], wood=w)
    block(f"{w}_sapling", "sapling", "BlockBehaviour.Properties.ofFullCopy(Blocks.OAK_SAPLING)", tags=["saplings"], grower=f"ModTreeGrowers.{w.upper()}", wood=w)
    block(f"potted_{w}_sapling", "pot", "BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_OAK_SAPLING)", plant=f"{w}_sapling", item=False,
          loot=f"pot:{w}_sapling")

block("hanging_lullwood_leaves", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.PALE_HANGING_MOSS).mapColor(MapColor.SNOW)",
      cls="HangingLullwoodLeavesBlock", model="hanging_leaves", tags=["hoe"], loot="shears")

# ---------------------------------------------------------------- flora
PLANT = "BlockBehaviour.Properties.ofFullCopy(Blocks.SHORT_GRASS).mapColor(MapColor.COLOR_PINK)"
FLOWER = "BlockBehaviour.Properties.ofFullCopy(Blocks.POPPY)"
block("blushgrass", "custom", PLANT, cls="BlushgrassBlock", model="cross", tags=["replaceable_plants", "sword_efficient"], loot="grass")
block("tall_blushgrass", "custom", PLANT, cls="SiftDoublePlantBlock", model="double_cross", tags=["replaceable_plants"], loot="double_grass",
      item_kind="double")
block("coral_fern", "custom", PLANT, cls="SiftPlantBlock", model="cross", tags=["replaceable_plants"], loot="grass")
block("coral_bush", "custom", PLANT, cls="SiftPlantBlock", model="cross", tags=["replaceable_plants", "sword_efficient"], loot="shears")
block("coral_thicket", "custom", PLANT, cls="SiftDoublePlantBlock", model="double_cross", tags=["replaceable_plants"], loot="double_grass",
      item_kind="double")
block("glimmer_sprouts", "custom", PLANT + ".lightLevel(s -> 5)", cls="SiftPlantBlock", model="cross", tags=["replaceable_plants"], loot="shears")
FLOWERS = {
    "lullaby_bell": ("MobEffects.REGENERATION", "4.0F", "cyan"),
    "dreambloom": ("MobEffects.JUMP_BOOST", "6.0F", "pink"),
    "soulpetal": ("MobEffects.NIGHT_VISION", "5.0F", "white"),
    "nebula_iris": ("MobEffects.SLOW_FALLING", "6.0F", "purple"),
}
for f, (eff, secs, col) in FLOWERS.items():
    block(f, "flower", FLOWER, effect=eff, secs=secs, light=7 if f == "soulpetal" else 0, tags=["flowers", "small_flowers"], cls="SiftFlowerBlock")
    block(f"potted_{f}", "pot", "BlockBehaviour.Properties.ofFullCopy(Blocks.POTTED_POPPY)" + (".lightLevel(s -> 7)" if f == "soulpetal" else ""), plant=f, item=False, loot=f"pot:{f}")
# the Stomper's favourite flower (taming and breeding food)
block("hummingbloom", "flower", FLOWER, effect="MobEffects.SPEED", secs="5.0F", light=0, tags=["flowers", "small_flowers"], cls="SiftFlowerBlock")
block("drift_petals", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.PINK_PETALS)", cls="DriftPetalsBlock", model="flowerbed", tags=["hoe"], loot="petals")
block("choir_lily", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.PEONY).lightLevel(s -> 4)", cls="ChoirLilyBlock", model="double_cross",
      tags=["flowers"], loot="double_flower", item_kind="double")
block("choir_lily_crop", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.PITCHER_CROP)", cls="ChoirLilyCropBlock", model="crop4", item=False,
      loot="crop:choir_pod")
block("echo_orchid", "custom", FLOWER + ".lightLevel(s -> 9)", cls="EchoOrchidBlock", model="cross", tags=["flowers", "small_flowers"])
block("echo_orchid_crop", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.TORCHFLOWER_CROP)", cls="EchoOrchidCropBlock", model="crop3", item=False,
      loot="crop:echo_seed")
block("pitcher_bulb_bush", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.SWEET_BERRY_BUSH).mapColor(MapColor.COLOR_LIGHT_BLUE)",
      cls="PitcherBulbBushBlock", model="bush4", item=False, loot="bush:pitcher_bulb")
block("chrome_reeds", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.SUGAR_CANE).mapColor(MapColor.COLOR_LIGHT_BLUE)", cls="ChromeReedsBlock",
      model="cross", loot="self")
block("glowcap", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.BROWN_MUSHROOM).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 10)",
      cls="GlowcapBlock", model="cross")
block("glowbell_vine", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.CAVE_VINES).mapColor(MapColor.COLOR_CYAN)", cls="GlowbellVineBlock",
      model="glowbell", item=True, loot="glowbell", item_kind="block")
block("glowbell_vine_plant", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.CAVE_VINES_PLANT).mapColor(MapColor.COLOR_CYAN)",
      cls="GlowbellVinePlantBlock", model="glowbell_plant", item=False, loot="glowbell")

# ---------------------------------------------------------------- functional & decorative
block("sift_drum", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.NOTE_BLOCK).mapColor(MapColor.WOOD).noOcclusion()", cls="SiftDrumBlock",
      model="drum", tags=["axe"], tab="functional")
block("euphory_altar", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.ENCHANTING_TABLE).mapColor(MapColor.COLOR_CYAN).lightLevel(s -> 9).noOcclusion()",
      cls="EuphoryAltarBlock", model="altar", tags=["pickaxe"], tab="functional")
block("sift_gate_frame", "custom", DEEP_BRICK + ".mapColor(MapColor.COLOR_CYAN).strength(30.0F, 1200.0F).requiresCorrectToolForDrops().lightLevel(s -> 3)",
      cls="SiftGateFrameBlock", model="cube_column", tags=["pickaxe", "needs_diamond", "portal_frame"], tab="functional")
block("sift_portal", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.NETHER_PORTAL).lightLevel(s -> 12)", cls="SiftPortalBlock", model="portal",
      item=False, loot="none")
block("conductors_podium", "custom", DEEP_BRICK + ".mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).lightLevel(s -> 7)",
      cls="ConductorsPodiumBlock", model="cube_column", tab="functional", loot="none", name="Conductor's Podium")
block("encore_sigil", "custom", DEEP_BRICK + ".mapColor(MapColor.COLOR_PURPLE).strength(-1.0F, 3600000.0F).lightLevel(s -> 6)",
      cls="EncoreSigilBlock", model="cube_column", tab="functional", loot="none", name="Encore Sigil")
block("instrument_altar", "custom", DEEP_BRICK + ".mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).lightLevel(s -> 8).noOcclusion()",
      cls="InstrumentAltarBlock", model="cube_column", tab="functional", loot="none", name="Instrument Altar")
block("harmony_stone", "custom", STONE + ".mapColor(MapColor.COLOR_LIGHT_BLUE).strength(-1.0F, 3600000.0F)", cls="HarmonyStoneBlock", model="harmony",
      tab="functional", loot="none")
block("harmony_seal", "custom", STONE + ".mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).lightLevel(s -> 5)", cls="HarmonySealBlock",
      model="cube_all", tab="functional", loot="none")
block("glyph_stone", "custom", STONE + ".mapColor(MapColor.COLOR_LIGHT_BLUE)", cls="GlyphStoneBlock", model="glyph", tags=["pickaxe"])
block("dream_snare", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.TRIPWIRE).noCollision()", cls="DreamSnareBlock", model="snare",
      tab="functional", loot="self")
# the Thumper's arena: its tower cannons (see registry/ModSiege for the block entity and the cannonball)
block("ancient_cannon", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).mapColor(MapColor.COLOR_ORANGE).strength(5.0F, 6.0F).noOcclusion()",
      cls="AncientCannonBlock", model="cannon", tags=["pickaxe"], tab="functional")
block("crumbling_dreamstone", "custom", STONE + ".mapColor(MapColor.COLOR_LIGHT_BLUE).strength(0.8F)", cls="CrumblingDreamstoneBlock",
      model="cube_all", tex="cracked_dreamstone_bricks", tags=["pickaxe"], tab="functional", loot="none")
block("sift_cake", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.CAKE).lightLevel(s -> 6)", cls="SiftCakeBlock", model="cake",
      loot="none", tab="items", item_kind="stack1")
block("bulb_lantern", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.LANTERN).lightLevel(s -> 15)", cls="BulbLanternBlock", model="lantern",
      tags=["pickaxe"], tab="functional")
block("glowing_slime_block", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.SLIME_BLOCK).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 12)",
      cls="GlowingSlimeBlock", model="slime", tab="functional")
block("soul_chime", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_CHAIN).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 6).noOcclusion()",
      cls="SoulChimeBlock", model="chime", tags=["pickaxe"], tab="functional")
# the Weaver (E2): glowing, tuned silk - barely slows you, bounces you, plays a note when touched
block("musical_cobweb", "custom", "BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).sound(net.minecraft.world.level.block.SoundType.COBWEB).noCollision().strength(1.0F).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED).lightLevel(s -> s.getValue(com.thesift.block.MusicalCobwebBlock.RINGING) ? 12 : 6)",
      cls="MusicalCobwebBlock", model="cross", tags=["sword_efficient"], tab="functional", loot="web:sculk_string")
block("lingering_glow", "custom", "BlockBehaviour.Properties.of().replaceable().noCollision().noLootTable().noOcclusion().instabreak().lightLevel(s -> 15).pushReaction(net.minecraft.world.level.material.PushReaction.POPPED)",
      cls="LingeringGlowBlock", model="none", item=False, loot="none")
block("chrome", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.WATER).mapColor(MapColor.COLOR_LIGHT_BLUE).lightLevel(s -> 8)",
      cls="ChromeLiquidBlock", model="liquid", item=False, loot="none")

# ---------------------------------------------------------------- items
item("glowing_slime_ball", cls="GlowingSlimeBallItem", props="new Item.Properties().food(ModFoods.GLOWING_SLIME_BALL, ModFoods.GLOWING_SLIME_BALL_CONSUMABLE)")
item("pitcher_bulb", cls="BlockItem:pitcher_bulb_bush", props="new Item.Properties().food(ModFoods.PITCHER_BULB).useItemDescriptionPrefix()")
item("thick_hide")
item("chrome_pearl", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
item("raw_serbim")
item("serbim_ingot")
item("siftite_ingot", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
item("siftite_nugget")
item("warden_core", cls="WardenCoreItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()")
item("choir_pod", cls="BlockItem:choir_lily_crop", props="new Item.Properties().useItemDescriptionPrefix()")
item("echo_seed", cls="BlockItem:echo_orchid_crop", props="new Item.Properties().useItemDescriptionPrefix()")
item("dream_stew", props="new Item.Properties().stacksTo(1).food(ModFoods.DREAM_STEW, ModFoods.DREAM_STEW_CONSUMABLE).usingConvertsTo(Items.BOWL)")
item("glowcap_skewer", props="new Item.Properties().food(ModFoods.GLOWCAP_SKEWER)")
item("chrome_bucket", cls="ChromeBucket", props="new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)")
item("slingshot", cls="SlingshotItem", props="new Item.Properties().durability(384).enchantable(1)", model="slingshot")
item("siftite_upgrade_smithing_template", cls="SiftiteTemplate", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
item("star_shard", props="new Item.Properties().rarity(Rarity.RARE)")
item("dream_journal_fragment")
for t in ["sword", "pickaxe", "axe", "shovel", "hoe"]:
    item(f"siftite_{t}", cls=f"tool:{t}", model="handheld", tab="combat" if t == "sword" else "tools")
item("siftite_spear", cls="tool:spear", model="spear", tab="combat")
for a in ["helmet", "chestplate", "leggings", "boots"]:
    item(f"siftite_{a}", cls=f"armor:{a}", tab="combat", model="armor")
for mob in ["bulb", "slumbler", "sifter", "enchoer", "riveter", "harmoner", "dictator", "thumper", "strummer",
            "strumling"]:
    item(f"{mob}_spawn_egg", cls=f"egg:{mob}", tab="eggs", model="generated")
for mob in ["stomper", "fanfare_eel", "kazoo_fish", "tubafish", "sky_whale"]: item(f"{mob}_spawn_egg", cls=f"egg:{mob}", tab="eggs", model="generated")
item("sculk_parasite_spawn_egg", cls="egg:sculk_parasite", tab="eggs", model="generated")
item("conductors_baton", cls="BatonItem", props="new Item.Properties().sword(ModMaterials.SIFTITE_TOOL, 5.0F, -2.0F).rarity(Rarity.EPIC).fireResistant()",
     model="handheld", tab="combat", name="Conductor's Baton")
item("conductors_staff", cls="StaffItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()",
     model="handheld", tab="combat", name="Conductor's Staff")
# the three instruments taken from the Conductor's great players
item("conga_drum", cls="CongaDrumItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()", model="handheld", tab="combat")
item("cannonball", props="new Item.Properties().stacksTo(16)", tab="combat")
item("crane_flute", cls="CraneFluteItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)", model="handheld", tab="combat")
# --- the Weaver (E2): sculk string from its brood, a plain guitar strung with it, the Weaver's own guitar
item("sculk_string")
item("guitar", cls="GuitarItem", props="new Item.Properties().stacksTo(1)", model="handheld", tab="combat")
item("weaver_guitar", cls="WeaverGuitarItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()", model="handheld",
     tab="combat", name="Weaver's Guitar")
for _d in ITEMS:
    if _d["id"] in ("strummer_spawn_egg", "strumling_spawn_egg"):
        _d["name"] = {"strummer_spawn_egg": "Weaver Spawn Egg", "strumling_spawn_egg": "Sculk Spider Spawn Egg"}[_d["id"]]
item("sift_codex", cls="SiftCodexItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)", name="Sift Codex")
item("music_disc_lullaby", props="new Item.Properties().stacksTo(1).rarity(Rarity.RARE).jukeboxPlayable(ModSounds.LULLABY_SONG)")
# --- songs & instruments (agent D): a Music Sheet per Song (music/Song.java) and the gem-inlaid instruments
SONG_TITLES = {"offering": "The Offering", "nib": "Song of the Nibs", "golem": "Golem Hymn", "crystal": "Crystal Hymn",
               "whale": "Whale Song", "tide": "Tide Song", "lullaby": "Lullaby"}
for _s, _t in SONG_TITLES.items():
    item(f"music_sheet_{_s}", cls="MusicSheetItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)", name=f"Music Sheet: {_t}")
item("prism_flute", cls="PrismFluteItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)", model="handheld", tab="combat")
item("prism_harp", cls="PrismHarpItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)", model="handheld", tab="combat")
item("prism_drum", cls="PrismDrumItem", props="new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()", model="handheld", tab="combat")
# --- end songs & instruments

# ---------------------------------------------------------------- the wild creatures' drops and gear
item("stomper_meat", props="new Item.Properties().food(ModFoods.STOMPER_MEAT)", name="Raw Stomper Meat")
item("stomper_steak", props="new Item.Properties().food(ModFoods.STOMPER_STEAK, ModFoods.STOMPER_STEAK_CONSUMABLE)")
item("stomper_egg", cls="StomperEggItem", props="new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON)")
item("kazoo_fish", props="new Item.Properties().food(ModFoods.KAZOO_FISH)", name="Raw Kazoo Fish")
item("cooked_kazoo_fish", props="new Item.Properties().food(ModFoods.COOKED_KAZOO_FISH)")
item("tuba_bubble")
item("bubble_gun", cls="BubbleGunItem", props="new Item.Properties().durability(256)", model="handheld", tab="tools")
item("skysong_gem", props="new Item.Properties().rarity(Rarity.EPIC)")

# ---------------------------------------------------------------- H: potted pitchers and their soups
block("pitcher_planter", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.DECORATED_POT).mapColor(MapColor.TERRACOTTA_PINK).strength(1.0F)"
      ".lightLevel(s -> s.getValue(com.thesift.block.PitcherPlanterBlock.STAGE) == 4 ? 6 : 0).noOcclusion()",
      cls="PitcherPlanterBlock", model="planter", tags=["pickaxe"], tab="functional")
item("pitcher_nectar", props="new Item.Properties().food(ModSoups.PITCHER_NECTAR, ModSoups.PITCHER_NECTAR_CONSUMABLE)")
for soup in ("lullaby_soup", "echo_chowder", "chrome_bisque"):
    C = soup.upper()
    item(soup, props=f"new Item.Properties().stacksTo(1).food(ModSoups.{C}, ModSoups.{C}_CONSUMABLE).usingConvertsTo(Items.BOWL)")

# ---------------------------------------------------------------- C: the Caravans Cavern (tools/caravans.py, registry/ModCaravans)
block("prism_ore", "ore", STONE + ".mapColor(MapColor.COLOR_MAGENTA).strength(3.0F, 3.0F).requiresCorrectToolForDrops()",
      tags=["pickaxe", "needs_diamond"], loot="ore:prism_gem")
block("deep_prism_ore", "ore", DEEP + ".mapColor(MapColor.COLOR_MAGENTA).strength(4.5F, 3.0F).requiresCorrectToolForDrops()",
      tags=["pickaxe", "needs_diamond"], loot="ore:prism_gem")
block("prism_block", "cube", "BlockBehaviour.Properties.ofFullCopy(Blocks.DIAMOND_BLOCK).mapColor(MapColor.COLOR_MAGENTA).lightLevel(s -> 4)",
      tags=["pickaxe", "needs_diamond", "beacon"])
block("music_crystal", "custom", "BlockBehaviour.Properties.ofFullCopy(Blocks.AMETHYST_CLUSTER).mapColor(MapColor.COLOR_PURPLE).strength(1.5F)"
      ".lightLevel(s -> s.getValue(com.thesift.block.MusicCrystalBlock.FROZEN) ? 10 : 7).noOcclusion()",
      cls="MusicCrystalBlock", model="music_crystal", tags=["pickaxe"], tab="functional", loot="none")
item("prism_gem", props="new Item.Properties().rarity(Rarity.RARE)")
for _a in ["helmet", "chestplate", "leggings", "boots"]:
    item(f"prism_{_a}", cls="Item", props=f"new Item.Properties().humanoidArmor(ModCaravans.PRISM_ARMOR, ArmorType.{_a.upper()}).rarity(Rarity.RARE)",
         tab="combat", model="armor")
# --- B4 gear: Prism tools (item/PrismGear.java holds the material and the sword's reveal)
for _t, _s in [("sword", "sword(com.thesift.item.PrismGear.TOOL, 3.0F, -2.4F)"), ("pickaxe", "pickaxe(com.thesift.item.PrismGear.TOOL, 1.0F, -2.8F)"),
               ("axe", "axe(com.thesift.item.PrismGear.TOOL, 5.0F, -3.0F)"), ("shovel", "shovel(com.thesift.item.PrismGear.TOOL, 1.5F, -3.0F)"),
               ("hoe", "hoe(com.thesift.item.PrismGear.TOOL, -3.5F, 0.0F)")]:
    item(f"prism_{_t}", cls="Item", props=f"new Item.Properties().{_s}.rarity(Rarity.RARE)", model="handheld",
         tab="combat" if _t == "sword" else "tools")
# --- end B4 gear
item("caravan_spawn_egg",cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModCaravans.CARAVAN.get())", tab="eggs", model="generated")
# ---------------------------------------------------------------- sea & sky (F + W): kelp, clouds, the Gobbler's drops, fish meat, sushi
__import__("sea_spec").declare(block, item)
# ---------------------------------------------------------------- A2 Echoer: the Echoer's Hut and its household
# The Echoer device (a mining-beam horn), the hut's hearthstone, the Soul Golem's core, Nib Dust (Nibs' treasure)
block("echoer_device", "custom", DEEP_BRICK + ".mapColor(MapColor.COLOR_CYAN).strength(3.5F, 6.0F).requiresCorrectToolForDrops()"
      ".lightLevel(s -> s.getValue(com.thesift.block.EchoerDeviceBlock.CHARGING) ? 11 : 4)",
      cls="EchoerDeviceBlock", model="echoer_device", tags=["pickaxe"], tab="functional", name="The Echoer")
block("echoer_hut_heart", "custom", DEEP_BRICK + ".mapColor(MapColor.COLOR_CYAN).strength(-1.0F, 3600000.0F).noLootTable().lightLevel(s -> 9)",
      cls="EchoerHutHeartBlock", model="echoer_hut_heart", item=False, loot="none", tab="functional", name="Echoer's Hearthstone")
item("soul_golem_core", cls="SoulGolemCoreItem", props="new Item.Properties().stacksTo(16).rarity(Rarity.RARE)")
item("nib_dust", props="new Item.Properties().rarity(Rarity.UNCOMMON)")
item("soul_golem_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModEchoer.SOUL_GOLEM.get())", tab="eggs")
item("nib_spawn_egg", cls="SpawnEggItem", props="new Item.Properties().spawnEgg(ModEchoer.NIB.get())", tab="eggs")
for _d in ITEMS:
    if _d["id"] == "enchoer_spawn_egg":
        _d["name"] = "Echoer Spawn Egg"
# ---------------------------------------------------------------- A1 Bulb & Stomper: the Sculk Bloom (tools/sculk_bloom.py)
__import__("sculk_bloom").declare(block, item)
# ---------------------------------------------------------------- A2 Swifter & White Forest (tools/swifter.py)
__import__("swifter").declare(block, item)
# ---------------------------------------------------------------- A3 Chrome: Chime Sand/Glass, Chrome fish buckets (tools/chrome.py)
__import__("chrome").declare(block, item)
# ---------------------------------------------------------------- A4 cave creatures: the Jailer and Sculklings (spawn eggs)
__import__("cave_creatures").declare(block, item)
