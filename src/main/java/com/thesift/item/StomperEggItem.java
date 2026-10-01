package com.thesift.item;

import com.thesift.entity.Stomper;
import com.thesift.registry.ModEntities;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.context.UseOnContext;

/**
 * A Stomper Egg, laid by two well-fed Stompers. Set it down on the ground and it cracks open in a
 * shower of shell and sparkles: out tumbles a baby Stomper, ready to be tamed with Hummingblooms.
 */
public class StomperEggItem extends Item {
    public StomperEggItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!(context.getLevel() instanceof ServerLevel level)) {
            return InteractionResult.SUCCESS;
        }
        BlockPos pos = context.getClickedPos().relative(context.getClickedFace());
        Stomper baby = ModEntities.STOMPER.get().create(level, EntitySpawnReason.BREEDING);
        if (baby == null) {
            return InteractionResult.FAIL;
        }
        Player player = context.getPlayer();
        float yaw = player != null ? player.getYRot() + 180.0F : level.getRandom().nextFloat() * 360.0F;
        baby.setAge(-24000);
        baby.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0.0F);
        baby.setYHeadRot(yaw);
        baby.setYBodyRot(yaw);
        level.addFreshEntity(baby);
        ItemStack stack = context.getItemInHand();
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.4;
        double z = pos.getZ() + 0.5;
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(stack)), x, y, z, 24, 0.3, 0.3, 0.3, 0.12);
        level.sendParticles(ParticleTypes.POOF, x, y, z, 12, 0.4, 0.3, 0.4, 0.03);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), x, y + 0.3, z, 20, 0.5, 0.4, 0.5, 0.05);
        level.sendParticles(ModParticles.CHROME_DROPLET.get(), x, y, z, 10, 0.3, 0.2, 0.3, 0.1);
        level.sendParticles(ParticleTypes.HEART, x, y + 0.8, z, 4, 0.4, 0.2, 0.4, 0.0);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), x, pos.getY() + 0.05, z, 0, 1.6, 0.0, 0.0, 1.0);
        level.playSound(null, pos, ModSounds.STOMPER_HATCH.get(), SoundSource.NEUTRAL, 1.0F, 1.0F);
        stack.consume(1, player);
        return InteractionResult.SUCCESS;
    }
}
