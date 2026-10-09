package com.thesift.knowledge;

import com.thesift.TheSift;
import com.thesift.entity.MiniCreator;
import com.thesift.registry.ModDimensions;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModKnowledge;
import com.thesift.music.SongEvents;
import java.util.List;
import java.util.Map;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

/**
 * F3: writes the player's discoveries into their Knowledge Book and runs the Mini Creator's quests.
 *
 * <p>Every two seconds it looks around each player: the Sift creatures in sight, what they carry (items and
 * enchantments), the land and dimension they stand in and any Sift structure around them. Songs are recorded when
 * played or heard nearby; lore when read ({@link #read}).
 *
 * <p>Quests: on the first steps in the Sift the Mini Creator appears, gives you this book and your first goal. When
 * a goal is met he pops up (in the Sift) to cheer and give the next one ({@link Quest}).
 */
public final class KnowledgeTracker {
    private static final String SIFT = TheSift.MODID;

    private KnowledgeTracker() {
    }

    public static void register() {
        NeoForge.EVENT_BUS.addListener(KnowledgeTracker::onPlayerTick);
        SongEvents.listenSongs((level, player, at, song) -> {
            String key = "song:" + song.id();
            if (player instanceof ServerPlayer sp) {
                Knowledge.unlock(sp, key);
            }
            for (ServerPlayer p : level.players()) {
                if (p != player && p.position().distanceToSqr(at) < 24.0 * 24.0) {
                    Knowledge.unlock(p, key);
                }
            }
        });
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator() || !player.isAlive()) {
            return;
        }
        if ((player.tickCount + player.getId()) % 40 != 0) {
            return;
        }
        scan(player);
        quests(player);
    }

    // ------------------------------------------------------------------ discoveries

    /** Looks around the player once and records everything new. */
    public static void scan(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        // what they carry: Sift items (announced by the client) and the Sift's enchantments
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id.getNamespace().equals(SIFT)) {
                Knowledge.unlock(player, "item:" + id, true);
            }
            for (Holder<Enchantment> e : EnchantmentHelper.getEnchantmentsForCrafting(stack).keySet()) {
                e.unwrapKey().ifPresent(k -> {
                    if (k.identifier().getNamespace().equals(SIFT)) {
                        Knowledge.unlock(player, "ench:" + k.identifier());
                    }
                });
            }
        }
        // where they are
        Identifier dim = level.dimension().identifier();
        if (dim.getNamespace().equals(SIFT)) {
            Knowledge.unlock(player, "dim:" + dim);
        }
        BlockPos pos = player.blockPosition();
        level.getBiome(pos).unwrapKey().ifPresent(k -> {
            if (k.identifier().getNamespace().equals(SIFT)) {
                Knowledge.unlock(player, "biome:" + k.identifier());
            }
        });
        Map<Structure, it.unimi.dsi.fastutil.longs.LongSet> here = level.structureManager().getAllStructuresAt(pos);
        if (!here.isEmpty()) {
            var registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            for (Structure s : here.keySet()) {
                Identifier sid = registry.getKey(s);
                if (sid != null && sid.getNamespace().equals(SIFT) && !Knowledge.has(player, "structure:" + sid)) {
                    StructureStart start = level.structureManager().getStructureAt(pos, s);
                    if (start.isValid()) {
                        Knowledge.unlock(player, "structure:" + sid);
                    }
                }
            }
        }
        // the creatures in sight
        int checked = 0;
        List<Mob> mobs = level.getEntitiesOfClass(Mob.class, player.getBoundingBox().inflate(16.0), Mob::isAlive);
        for (Mob mob : mobs) {
            Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());
            if (!id.getNamespace().equals(SIFT) || Knowledge.has(player, "entity:" + id)) {
                continue;
            }
            if (checked++ > 6) {
                break;
            }
            if (player.hasLineOfSight(mob)) {
                Knowledge.unlock(player, "entity:" + id);
            }
        }
    }

    /** A Lore Book or Scroll was read: record it, and the ancient script grows clearer. */
    public static void read(ServerPlayer player, Lore lore) {
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModKnowledge.READ.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        if (Knowledge.unlock(player, lore.key(), true)) {
            int pct = Math.round(Glyphs.level(Knowledge.clues(player)) * 100.0F);
            player.sendOverlayMessage(Component.translatable("lore.thesift.decipher_up", pct).withStyle(ChatFormatting.DARK_AQUA));
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModKnowledge.DECIPHER.get(), SoundSource.PLAYERS, 0.7F, 1.0F);
        }
    }

    // ------------------------------------------------------------------ the Mini Creator's quests

    private static void quests(ServerPlayer player) {
        boolean inSift = player.level().dimension() == ModDimensions.THE_SIFT;
        if (!Knowledge.has(player, Quest.ARRIVAL.doneKey())) {
            // first steps in the Sift (and on solid ground, once any arrival cinematic is over): he appears
            if (inSift && player.onGround() && Knowledge.has(player, "dim:" + ModDimensions.THE_SIFT.identifier())) {
                MiniCreator guide = MiniCreator.guideOf(player, true);
                if (guide != null) {
                    meet(player, guide);
                }
            }
            return;
        }
        Quest q = Quest.current(player);
        if (!Knowledge.has(player, q.key())) {
            // a new goal: he comes to tell you (in the Sift; elsewhere his words reach you anyway)
            Knowledge.unlock(player, q.key(), true);
            MiniCreator guide = inSift ? MiniCreator.guideOf(player, true) : null;
            say(player, guide, Component.translatable("quest.thesift." + q.id() + ".line"));
            return;
        }
        if (q != Quest.SOUL && q.met(player)) {
            Knowledge.unlock(player, q.doneKey(), true);
            MiniCreator guide = inSift ? MiniCreator.guideOf(player, true) : null;
            if (guide != null) {
                guide.celebrate();
            }
            if (q.hasDoneLine()) {
                say(player, guide, Component.translatable("quest.thesift." + q.id() + ".done"));
            }
        }
    }

    /** The first meeting: a hello, this book (if you have none) and your first goal. */
    public static void meet(ServerPlayer player, MiniCreator guide) {
        Knowledge.unlock(player, Quest.ARRIVAL.key(), true);
        Knowledge.unlock(player, Quest.ARRIVAL.doneKey(), true);
        guide.wave();
        say(player, guide, Component.translatable("quest.thesift.arrival.line"));
        boolean hasBook = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(ModItems.KNOWLEDGE_BOOK.get())) {
                hasBook = true;
                break;
            }
        }
        if (!hasBook) {
            ItemStack book = new ItemStack(ModItems.KNOWLEDGE_BOOK.get());
            if (!player.getInventory().add(book)) {
                guide.spawnAtLocation((ServerLevel) player.level(), book);
            }
        }
        Knowledge.unlock(player, "entity:" + BuiltInRegistries.ENTITY_TYPE.getKey(ModKnowledge.MINI_CREATOR.get()), true);
    }

    /** He repeats your current goal (when you talk to him). */
    public static void tellCurrent(ServerPlayer player, MiniCreator guide) {
        if (!Knowledge.has(player, Quest.ARRIVAL.doneKey())) {
            meet(player, guide);
            return;
        }
        Quest q = Quest.current(player);
        say(player, guide, Component.translatable("quest.thesift." + q.id() + ".line"));
    }

    /** The Mini Creator speaks to the player: in chat, and (if he is here) with his bill and his bells. */
    public static void say(ServerPlayer player, @Nullable MiniCreator guide, Component line) {
        Component name = Component.translatable("entity.thesift.mini_creator").withStyle(ChatFormatting.GOLD);
        player.sendSystemMessage(Component.translatable("entity.thesift.mini_creator.says", name, line.copy().withStyle(ChatFormatting.WHITE)));
        if (guide != null) {
            guide.talk();
        } else {
            player.level().playSound(null, player.getX(), player.getY(), player.getZ(), ModKnowledge.GUIDE_TALK.get(), SoundSource.NEUTRAL, 0.5F, 1.1F);
        }
    }
}
