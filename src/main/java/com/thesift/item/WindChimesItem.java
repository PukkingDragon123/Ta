package com.thesift.item;

import com.thesift.music.Instrument;
import com.thesift.music.Notes;
import com.thesift.music.Song;
import com.thesift.registry.ModParticles;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Wind Chimes ({@code thesift:wind_chimes}): five tuned tubes hung from a little crossbar. Use to
 * ring one note - its pitch from where you look, as with every instrument (straight ahead F#4,
 * each 5 degrees up or down a semitone). The Echoer's Offering and the Caravans' Crystal Hymn are
 * played on them.
 */
public class WindChimesItem extends Item {
    public static final int NOTE_COOLDOWN = 4;

    public WindChimesItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Notes.play(level, player, Instrument.WIND_CHIMES, Notes.lookPitch(player));
        if (level instanceof ServerLevel server) {
            // the neighbouring tubes answer softly
            Vec3 at = Notes.mouth(player);
            server.playSound(null, at.x, at.y, at.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.5F,
                    1.4F + server.getRandom().nextFloat() * 0.4F);
            server.sendParticles(ModParticles.STAR_SPARKLE.get(), at.x, at.y - 0.2, at.z, 3, 0.15, 0.25, 0.15, 0.01);
        }
        player.getCooldowns().addCooldown(stack, NOTE_COOLDOWN);
        return InteractionResult.SUCCESS;
    }

    @Override
    @Deprecated
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable("item.thesift.wind_chimes.desc").withStyle(ChatFormatting.GRAY));
        for (Song song : Song.values()) {
            if (song.accepts(Instrument.WIND_CHIMES) && song.instrument() != null) {
                builder.accept(Component.literal("♪ ").append(Component.translatable("song.thesift." + song.id())).withStyle(ChatFormatting.DARK_AQUA));
            }
        }
    }
}
