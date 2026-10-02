package com.thesift.item;

import com.thesift.TheSift;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModItems;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * What makes Siftite more than a stronger Netherite.
 *
 * <p>Tools and weapons hit with a heavy, booming knockback. Armour is tuned to the Deep Dark's
 * sound: any piece keeps you from being Deafened, each piece takes an eighth off sonic damage
 * (half for the full set), the helmet lets you breathe under water, leggings and boots each make
 * you swim a quarter faster, and the full set halves how long Sculk Corruption lasts.</p>
 */
public final class SiftiteGear {
    private static final AttributeModifier KNOCKBACK_WEAPON = new AttributeModifier(TheSift.id("siftite_knockback"), 1.6,
            AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier KNOCKBACK_TOOL = new AttributeModifier(TheSift.id("siftite_knockback"), 1.0,
            AttributeModifier.Operation.ADD_VALUE);
    private static final AttributeModifier SWIM_LEGS = new AttributeModifier(TheSift.id("siftite_swim_legs"), 0.25,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final AttributeModifier SWIM_FEET = new AttributeModifier(TheSift.id("siftite_swim_feet"), 0.25,
            AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    private static final EquipmentSlot[] ARMOR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    /** Guards the halved Sculk Corruption from being halved again as it is re-applied. */
    private static boolean halving;

    private SiftiteGear() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(SiftiteGear::onAttributes);
        NeoForge.EVENT_BUS.addListener(SiftiteGear::onEffectApplicable);
        NeoForge.EVENT_BUS.addListener(SiftiteGear::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(SiftiteGear::onPlayerTick);
    }

    private static boolean isWeapon(Item item) {
        return item == ModItems.SIFTITE_SWORD.get() || item == ModItems.SIFTITE_AXE.get() || item == ModItems.SIFTITE_SPEAR.get();
    }

    private static boolean isTool(Item item) {
        return item == ModItems.SIFTITE_PICKAXE.get() || item == ModItems.SIFTITE_SHOVEL.get() || item == ModItems.SIFTITE_HOE.get();
    }

    private static Item armorFor(EquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> ModItems.SIFTITE_HELMET.get();
            case CHEST -> ModItems.SIFTITE_CHESTPLATE.get();
            case LEGS -> ModItems.SIFTITE_LEGGINGS.get();
            default -> ModItems.SIFTITE_BOOTS.get();
        };
    }

    public static boolean wears(LivingEntity entity, EquipmentSlot slot) {
        return entity.getItemBySlot(slot).is(armorFor(slot));
    }

    /** How many pieces of Siftite armour the entity wears, 0 to 4. */
    public static int pieces(LivingEntity entity) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR) {
            if (wears(entity, slot)) n++;
        }
        return n;
    }

    private static void onAttributes(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        Item item = stack.getItem();
        if (isWeapon(item)) {
            event.addModifier(Attributes.ATTACK_KNOCKBACK, KNOCKBACK_WEAPON, EquipmentSlotGroup.MAINHAND);
        } else if (isTool(item)) {
            event.addModifier(Attributes.ATTACK_KNOCKBACK, KNOCKBACK_TOOL, EquipmentSlotGroup.MAINHAND);
        } else if (item == ModItems.SIFTITE_LEGGINGS.get()) {
            event.addModifier(NeoForgeMod.SWIM_SPEED, SWIM_LEGS, EquipmentSlotGroup.LEGS);
        } else if (item == ModItems.SIFTITE_BOOTS.get()) {
            event.addModifier(NeoForgeMod.SWIM_SPEED, SWIM_FEET, EquipmentSlotGroup.FEET);
        }
    }

    private static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        MobEffectInstance inst = event.getEffectInstance();
        if (inst.is(ModEffects.DEAFENED) && pieces(entity) > 0) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        } else if (inst.is(ModEffects.SCULK_CORRUPTION) && !halving && !inst.isInfiniteDuration() && pieces(entity) == 4
                && !entity.level().isClientSide()) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            halving = true;
            try {
                entity.addEffect(new MobEffectInstance(inst.getEffect(), Math.max(1, inst.getDuration() / 2), inst.getAmplifier(), inst.isAmbient(),
                        inst.isVisible(), inst.showIcon()), event.getEffectSource());
            } finally {
                halving = false;
            }
        }
    }

    private static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.SONIC_BOOM)) {
            int n = pieces(event.getEntity());
            if (n > 0) {
                event.setAmount(event.getAmount() * (1.0F - 0.125F * n));
            }
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (wears(player, EquipmentSlot.HEAD) && player.isEyeInFluid(FluidTags.WATER)) {
            player.setAirSupply(player.getMaxAirSupply());
        }
    }
}
