package com.thesift.item;

import com.thesift.entity.GlowballEntity;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;

/**
 * Glowing slime produced by Bulbs. Edible (a little regeneration and a soft glow), slingshot ammo,
 * dispensable, and the key ingredient of Sift Cake.
 */
public class GlowingSlimeBallItem extends Item implements ProjectileItem {
    public GlowingSlimeBallItem(Properties properties) {
        super(properties);
    }

    @Override
    public Projectile asProjectile(Level level, Position position, ItemStack stack, Direction direction) {
        GlowballEntity ball = new GlowballEntity(level, position.x(), position.y(), position.z(), stack.copyWithCount(1));
        ball.setPower(0.7F);
        return ball;
    }
}
