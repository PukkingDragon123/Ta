package com.thesift.block.entity;

import com.thesift.block.SiftDrumBlock;
import com.thesift.music.Resonance;
import com.thesift.registry.ModBlockEntities;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import com.thesift.registry.ModTags;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class EuphoryAltarBlockEntity extends BlockEntity {
    public static final int DRUM_RADIUS = 4;

    private ItemStack item = ItemStack.EMPTY;
    private int ritual;          // ticks left in the current ritual, 0 = idle
    private int ritualLength;
    private int drumCount;
    private final List<BlockPos> drums = new ArrayList<>();

    // client-side animation
    public float spin;
    public float spinO;
    public float spinSpeed;
    public int age;

    public EuphoryAltarBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.EUPHORY_ALTAR.get(), pos, state);
    }

    public ItemStack getItem() {
        return this.item;
    }

    public boolean isRitualActive() {
        return this.ritual > 0;
    }

    public float ritualProgress() {
        return this.ritualLength == 0 ? 0 : 1.0F - this.ritual / (float) this.ritualLength;
    }

    // ------------------------------------------------------------------ interaction

    public InteractionResult use(Player player, InteractionHand hand, ItemStack stack) {
        if (this.ritual > 0) {
            return InteractionResult.CONSUME;
        }
        if (stack.is(ModTags.Items.ALTAR_FUEL) && !this.item.isEmpty()) {
            if (this.level instanceof ServerLevel server) {
                this.startRitual(server, player, stack);
            }
            return InteractionResult.SUCCESS;
        }
        if (this.item.isEmpty() && (stack.isEnchantable() || stack.is(Items.BOOK)) && !stack.isEnchanted()) {
            if (!this.level.isClientSide()) {
                this.item = stack.split(1);
                this.sync();
                this.level.playSound(null, this.worldPosition, ModSounds.ALTAR_HUM.get(), SoundSource.BLOCKS, 0.8F, 1.3F);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    public InteractionResult takeItem(Player player) {
        if (this.ritual > 0 || this.item.isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!this.level.isClientSide()) {
            if (!player.getInventory().add(this.item)) {
                net.minecraft.world.level.block.Block.popResource(this.level, this.worldPosition.above(), this.item);
            }
            this.item = ItemStack.EMPTY;
            this.sync();
        }
        return InteractionResult.SUCCESS;
    }

    private void startRitual(ServerLevel level, Player player, ItemStack fuel) {
        this.scanDrums(level);
        if (this.drums.size() < 2) {
            player.sendOverlayMessage(Component.translatable("message.thesift.altar.need_drums", this.drums.size()));
            return;
        }
        int cost = Math.min(10, 2 + this.drums.size() / 2);
        if (!player.getAbilities().instabuild) {
            if (player.experienceLevel < cost) {
                player.sendOverlayMessage(Component.translatable("message.thesift.altar.need_levels", cost));
                return;
            }
            player.giveExperienceLevels(-cost);
            fuel.shrink(1);
        }
        this.drumCount = this.drums.size();
        this.ritualLength = 80 + this.drumCount * 8;
        this.ritual = this.ritualLength;
        level.playSound(null, this.worldPosition, ModSounds.ALTAR_CHARGE.get(), SoundSource.BLOCKS, 1.4F, 1.0F);
        this.sync();
    }

    private void scanDrums(Level level) {
        this.drums.clear();
        for (BlockPos p : BlockPos.betweenClosed(this.worldPosition.offset(-DRUM_RADIUS, -1, -DRUM_RADIUS),
                this.worldPosition.offset(DRUM_RADIUS, 2, DRUM_RADIUS))) {
            if (level.getBlockState(p).is(ModBlocks.SIFT_DRUM.get())) {
                this.drums.add(p.immutable());
            }
        }
        this.drums.sort((a, b) -> Double.compare(angle(a), angle(b)));
    }

    private double angle(BlockPos p) {
        return Math.atan2(p.getZ() - this.worldPosition.getZ(), p.getX() - this.worldPosition.getX());
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, EuphoryAltarBlockEntity altar) {
        if (altar.ritual <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        altar.ritual--;
        int elapsed = altar.ritualLength - altar.ritual;
        int interval = Math.max(3, 12 - elapsed / 12);
        if (elapsed % interval == 0 && !altar.drums.isEmpty()) {
            BlockPos drum = altar.drums.get((elapsed / interval) % altar.drums.size());
            if (server.getBlockState(drum).is(ModBlocks.SIFT_DRUM.get())) {
                SiftDrumBlock.beat(server, drum, 0.6F);
                // A stream of light from the drum to the item.
                Vec3 from = Vec3.atCenterOf(drum).add(0, 0.6, 0);
                Vec3 to = Vec3.atCenterOf(pos).add(0, 1.0, 0);
                for (int i = 0; i < 8; i++) {
                    Vec3 p = from.lerp(to, i / 8.0);
                    server.sendParticles(ModParticles.STAR_SPARKLE.get(), p.x, p.y + Math.sin(i * 0.8) * 0.2, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                }
            }
        }
        if (elapsed % 20 == 0) {
            server.playSound(null, pos, ModSounds.ALTAR_HUM.get(), SoundSource.BLOCKS, 0.9F, 0.8F + elapsed / (float) altar.ritualLength * 0.8F);
        }
        if (altar.ritual == 0) {
            altar.finish(server);
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EuphoryAltarBlockEntity altar) {
        altar.age++;
        altar.spinO = altar.spin;
        float target = altar.ritual > 0 ? 0.35F + altar.ritualProgress() * 0.9F : (altar.item.isEmpty() ? 0.02F : 0.06F);
        altar.spinSpeed += (target - altar.spinSpeed) * 0.05F;
        altar.spin += altar.spinSpeed;
        if (altar.ritual > 0) {
            altar.ritual--;
            if (level.getRandom().nextInt(2) == 0) {
                double a = level.getRandom().nextDouble() * Math.PI * 2;
                double r = 1.2;
                level.addParticle(ModParticles.PORTAL_SOUL.get(), pos.getX() + 0.5 + Math.cos(a) * r, pos.getY() + 0.9, pos.getZ() + 0.5 + Math.sin(a) * r,
                        -Math.cos(a) * 0.06, 0.03, -Math.sin(a) * 0.06);
            }
        }
    }

    private void finish(ServerLevel level) {
        if (this.item.isEmpty()) {
            return;
        }
        int power = Math.min(60, 14 + this.drumCount * 4);
        Optional<HolderSet.Named<Enchantment>> pool = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(EnchantmentTags.IN_ENCHANTING_TABLE);
        Stream<Holder<Enchantment>> stream = pool.map(HolderSet::stream).orElseGet(Stream::empty);
        ItemStack result = EnchantmentHelper.enchantItem(level.getRandom(), this.item, power, stream);
        // Euphoric bonus: one enchantment may be pushed one level past its normal maximum.
        float bonusChance = Math.min(0.5F, this.drumCount / 24.0F);
        if (level.getRandom().nextFloat() < bonusChance) {
            ItemEnchantments ench = EnchantmentHelper.getEnchantmentsForCrafting(result);
            List<Holder<Enchantment>> keys = new ArrayList<>(ench.keySet());
            if (!keys.isEmpty()) {
                Holder<Enchantment> pick = keys.get(level.getRandom().nextInt(keys.size()));
                int lvl = ench.getLevel(pick);
                EnchantmentHelper.updateEnchantments(result, m -> m.set(pick, Math.min(255, lvl + 1)));
                level.sendParticles(ModParticles.RESONANCE_RING.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.1,
                        this.worldPosition.getZ() + 0.5, 0, 3.0, 0, 0, 1.0);
            }
        }
        this.item = result;
        level.playSound(null, this.worldPosition, ModSounds.ALTAR_ENCHANT.get(), SoundSource.BLOCKS, 1.5F, 1.0F);
        level.sendParticles(ModParticles.SIFT_NOTE.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.4,
                this.worldPosition.getZ() + 0.5, 0, 0.3, 0, 0, 1);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), this.worldPosition.getX() + 0.5, this.worldPosition.getY() + 1.2,
                this.worldPosition.getZ() + 0.5, 40, 0.6, 0.6, 0.6, 0.15);
        Resonance.pulse(level, this.worldPosition, 1.0F, 8);
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(8))) {
            p.addEffect(new MobEffectInstance(ModEffects.EUPHORIA, 20 * 30, 0));
        }
        this.sync();
    }

    // ------------------------------------------------------------------ sync & save

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (!this.item.isEmpty()) {
            output.store("Item", ItemStack.CODEC, this.item);
        }
        output.putInt("Ritual", this.ritual);
        output.putInt("RitualLength", this.ritualLength);
        output.putInt("Drums", this.drumCount);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.item = input.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        this.ritual = input.getIntOr("Ritual", 0);
        this.ritualLength = input.getIntOr("RitualLength", 0);
        this.drumCount = input.getIntOr("Drums", 0);
    }

    @Override
    public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public net.minecraft.nbt.CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        if (!this.item.isEmpty() && this.level != null) {
            Block.popResource(this.level, pos, this.item);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
