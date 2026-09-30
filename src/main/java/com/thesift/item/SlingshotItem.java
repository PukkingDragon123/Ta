package com.thesift.item;

import com.thesift.entity.GlowballEntity;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * A Slumbler-hide slingshot. Draw it back like a bow and let fly a Glowing Slime Ball: it bursts
 * into a dazzling area of light, lights up the cave for a while and leaves any Warden in the
 * blast Deafened.
 */
public class SlingshotItem extends ProjectileWeaponItem {
    public static final Predicate<ItemStack> AMMO = s -> s.is(ModTags.Items.SLINGSHOT_AMMO);

    public SlingshotItem(Properties properties) {
        super(properties);
    }

    @Override
    public Predicate<ItemStack> getAllSupportedProjectiles() {
        return AMMO;
    }

    @Override
    public int getDefaultProjectileRange() {
        return 18;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    public static float powerFor(int ticks) {
        float f = ticks / 16.0F;
        f = (f * f + f * 2.0F) / 3.0F;
        return Math.min(1.0F, f);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        boolean hasAmmo = !player.getProjectile(stack).isEmpty();
        if (!player.hasInfiniteMaterials() && !hasAmmo) {
            return InteractionResult.FAIL;
        }
        player.startUsingItem(hand);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SLINGSHOT_PULL.get(), SoundSource.PLAYERS, 0.6F, 1.0F);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        ItemStack ammo = player.getProjectile(stack);
        if (ammo.isEmpty() && !player.hasInfiniteMaterials()) {
            return false;
        }
        float power = powerFor(this.getUseDuration(stack, entity) - remainingTime);
        if (power < 0.15F) {
            return false;
        }
        if (level instanceof ServerLevel server) {
            GlowballEntity ball = new GlowballEntity(server, player, ammo.isEmpty() ? GlowballEntity.defaultStack() : ammo.copyWithCount(1));
            ball.setPower(power);
            ball.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, power * 2.4F, 0.8F);
            server.addFreshEntity(ball);
            stack.hurtAndBreak(1, player, hand(player));
            if (!player.hasInfiniteMaterials()) {
                ammo.shrink(1);
            }
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), ModSounds.SLINGSHOT_SHOOT.get(), SoundSource.PLAYERS, 1.0F,
                0.9F + power * 0.3F);
        player.awardStat(Stats.ITEM_USED.get(this));
        return true;
    }

    private static net.minecraft.world.entity.EquipmentSlot hand(Player player) {
        return player.getUsedItemHand() == InteractionHand.MAIN_HAND ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                : net.minecraft.world.entity.EquipmentSlot.OFFHAND;
    }

    @Override
    protected void shootProjectile(LivingEntity shooter, Projectile projectile, int index, float power, float uncertainty, float angle,
            @Nullable LivingEntity target) {
        projectile.shootFromRotation(shooter, shooter.getXRot(), shooter.getYRot() + angle, 0.0F, power, uncertainty);
    }
}
