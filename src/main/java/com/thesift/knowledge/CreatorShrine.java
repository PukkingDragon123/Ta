package com.thesift.knowledge;

import com.mojang.datafixers.util.Pair;
import com.thesift.TheSift;
import com.thesift.entity.MiniCreator;
import com.thesift.music.Song;
import com.thesift.music.SongEvents;
import com.thesift.registry.ModBlocks;
import java.util.Optional;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * S1 land: the Creator's Ruin and its dais. The Mini Creator no longer appears on his own: on a player's first steps in
 * the Sift a faint hymn on the wind tells them which way the nearest Creator's Ruin lies ({@link #hint}); its dais
 * gives them The Creator's Hymn as a Music Sheet; played beside the dais (any instrument), the hymn summons him - he
 * rises out of the dais in a column of light ({@link MiniCreator#summonAt}), gives them a Knowledge Book (if they
 * have none) and their first goal.
 */
public final class CreatorShrine {
    public static final String RUIN = "creators_ruin";
    /** How far from where the hymn is played the dais may be. */
    private static final int REACH = 8;

    private CreatorShrine() {
    }

    public static void register() {
        SongEvents.listenSongs(CreatorShrine::onSong);
    }

    private static void onSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        if (song != Song.HYMN || !(player instanceof ServerPlayer sp)) {
            return;
        }
        BlockPos dais = findDais(level, BlockPos.containing(at), REACH);
        if (dais == null) {
            sp.sendOverlayMessage(Component.translatable("message.thesift.hymn.no_dais").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
            return;
        }
        if (MiniCreator.summonAt(level, dais, sp) != null) {
            sp.sendOverlayMessage(Component.translatable("message.thesift.hymn.summon").withStyle(ChatFormatting.GOLD));
        }
    }

    /** The nearest Creator's Dais within {@code reach} blocks (and four up or down) of {@code at}, or null. */
    public static @Nullable BlockPos findDais(ServerLevel level, BlockPos at, int reach) {
        BlockPos best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(at.offset(-reach, -4, -reach), at.offset(reach, 4, reach))) {
            if (level.getBlockState(p).is(ModBlocks.CREATOR_DAIS.get())) {
                double d = p.distSqr(at);
                if (d < bestD) {
                    bestD = d;
                    best = p.immutable();
                }
            }
        }
        return best;
    }

    /** The nearest Creator's Ruin to {@code from} (searching about 40 chunks out), or null. */
    public static @Nullable BlockPos nearestRuin(ServerLevel level, BlockPos from) {
        Registry<Structure> registry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
        Optional<Holder.Reference<Structure>> holder = registry.get(ResourceKey.create(Registries.STRUCTURE, TheSift.id(RUIN)));
        if (holder.isEmpty()) {
            return null;
        }
        Pair<BlockPos, Holder<Structure>> found = level.getChunkSource().getGenerator()
                .findNearestMapStructure(level, HolderSet.direct(holder.get()), from, 40, false);
        return found == null ? null : found.getFirst();
    }

    /** The first-arrival whisper: which way (and how far) the nearest Creator's Ruin lies. */
    public static void hint(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        BlockPos ruin = nearestRuin(level, player.blockPosition());
        Component line;
        if (ruin == null) {
            line = Component.translatable("quest.thesift.arrival.hint_far");
        } else {
            double dx = ruin.getX() + 0.5 - player.getX();
            double dz = ruin.getZ() + 0.5 - player.getZ();
            int dist = (int) Math.round(Math.sqrt(dx * dx + dz * dz) / 10.0) * 10;
            line = Component.translatable("quest.thesift.arrival.hint", Component.translatable("quest.thesift.dir." + compass(dx, dz)), Math.max(10, dist));
        }
        player.sendSystemMessage(line.copy().withStyle(ChatFormatting.AQUA, ChatFormatting.ITALIC));
    }

    /** One of the eight compass points for a horizontal offset (north is -z). */
    static String compass(double dx, double dz) {
        String[] points = {"south", "south_west", "west", "north_west", "north", "north_east", "east", "south_east"};
        double a = Math.toDegrees(Math.atan2(-dx, dz)); // 0 = south, 90 = west, 180 = north, -90 = east
        int i = Mth.floor(((a + 360.0 + 22.5) % 360.0) / 45.0);
        return points[i % 8];
    }
}
