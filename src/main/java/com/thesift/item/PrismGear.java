package com.thesift.item;

import com.thesift.TheSift;
import com.thesift.registry.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Prism tools: the Seraphim's gear, cut from prism gems. A tier between Diamond and Netherite that
 * enchants beautifully. The many-eyed Prism Sword sees for you: every strike briefly reveals the
 * hostile creatures around you with Glowing, so nothing hides in the dark while you fight.
 */
public final class PrismGear {
    public static final TagKey<Item> TOOL_MATERIALS = TagKey.create(Registries.ITEM, TheSift.id("prism_tool_materials"));
    public static final ToolMaterial TOOL = new ToolMaterial(BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5F, 3.5F, 22, TOOL_MATERIALS);

    private static final double REVEAL_RADIUS = 14.0;
    private static final int REVEAL_TICKS = 100;

    private PrismGear() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(PrismGear::onIncomingDamage);
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getSource().getEntity() instanceof Player player) || event.getSource().getDirectEntity() != player) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !player.getMainHandItem().is(ModItems.PRISM_SWORD.get())) {
            return;
        }
        int seen = 0;
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(REVEAL_RADIUS),
                e -> e.isAlive() && e != player && e instanceof Enemy)) {
            e.addEffect(new MobEffectInstance(MobEffects.GLOWING, REVEAL_TICKS, 0, false, false));
            seen++;
        }
        if (seen > 0) {
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                    0.6F, 1.5F + level.getRandom().nextFloat() * 0.3F);
        }
    }
}
