package com.thesift.entity;

import com.thesift.TheSift;
import com.thesift.music.MusicListener;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.item.trading.TradeSet;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * Enchoer: a big, gentle, melancholy wanderer of the Sift - a mound of mint fur with a pale sad face
 * and broad moose antlers. Enchoers barter in Sift goods - saplings, seeds, pearls, drums and even
 * the occasional Warden Core. They hum along to any music they hear, spreading their arms wide.
 */
public class Enchoer extends AbstractVillager implements MusicListener {
    private static final EntityDataAccessor<Integer> SINGING = SynchedEntityData.defineId(Enchoer.class, EntityDataSerializers.INT);
    public static final ResourceKey<TradeSet> COMMON = ResourceKey.create(Registries.TRADE_SET, TheSift.id("enchoer/common"));
    public static final ResourceKey<TradeSet> RARE = ResourceKey.create(Registries.TRADE_SET, TheSift.id("enchoer/rare"));

    /** Client-side smooth wing spread (0 folded, 1 open). */
    public float wingSpread;
    public float wingSpreadO;

    public Enchoer(EntityType<? extends AbstractVillager> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 30.0).add(Attributes.MOVEMENT_SPEED, 0.45).add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new TradeWithPlayerGoal(this));
        this.goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Monster.class, 8.0F, 0.6, 0.8));
        this.goalSelector.addGoal(1, new PanicGoal(this, 0.7));
        this.goalSelector.addGoal(1, new LookAtTradingPlayerGoal(this));
        this.goalSelector.addGoal(4, new MoveTowardsRestrictionGoal(this, 0.35));
        this.goalSelector.addGoal(8, new WaterAvoidingRandomStrollGoal(this, 0.35));
        this.goalSelector.addGoal(9, new InteractGoal(this, Player.class, 3.0F, 1.0F));
        this.goalSelector.addGoal(10, new LookAtPlayerGoal(this, Mob.class, 8.0F));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(SINGING, 0);
    }

    public boolean isSinging() {
        return this.entityData.get(SINGING) > 0;
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean showProgressBar() {
        return false;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.isAlive() && !this.isTrading() && !this.isBaby()) {
            if (hand == InteractionHand.MAIN_HAND) {
                player.awardStat(Stats.TALKED_TO_VILLAGER);
            }
            if (!this.level().isClientSide()) {
                if (this.getOffers().isEmpty()) {
                    return InteractionResult.CONSUME;
                }
                this.setTradingPlayer(player);
                this.openTradingScreen(player, this.getDisplayName(), 1);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void updateTrades(ServerLevel level) {
        MerchantOffers offers = this.getOffers();
        this.addOffersFromTradeSet(level, offers, COMMON);
        this.addOffersFromTradeSet(level, offers, RARE);
    }

    @Override
    protected void rewardTradeXp(MerchantOffer offer) {
        if (offer.shouldRewardExp()) {
            this.level().addFreshEntity(new ExperienceOrb(this.level(), this.getX(), this.getY() + 0.5, this.getZ(), 3 + this.random.nextInt(4)));
        }
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 2.6, this.getZ(), 0, this.random.nextDouble(), 0, 0, 1);
        }
    }

    @Override
    public void hearMusic(BlockPos source, float strength) {
        if (!this.level().isClientSide()) {
            if (!this.isSinging()) {
                this.playSound(ModSounds.ENCHOER_HUM.get(), 1.0F, 0.9F + this.random.nextFloat() * 0.2F);
            }
            this.entityData.set(SINGING, 80);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            int s = this.entityData.get(SINGING);
            if (s > 0) {
                this.entityData.set(SINGING, s - 1);
                if (s % 12 == 0 && this.level() instanceof ServerLevel server) {
                    server.sendParticles(ModParticles.SIFT_NOTE.get(), this.getX(), this.getY() + 2.5, this.getZ(), 0, this.random.nextDouble(), 0, 0, 1);
                }
            }
        } else {
            this.wingSpreadO = this.wingSpread;
            float target = this.isTrading() || this.isSinging() ? 1.0F : 0.0F;
            this.wingSpread += (target - this.wingSpread) * 0.08F;
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ModParticles.STAR_SPARKLE.get(), this.getRandomX(0.8), this.getY() + 1.2 + this.random.nextDouble(),
                        this.getRandomZ(0.8), 0, 0.01, 0);
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return this.isTrading() ? ModSounds.ENCHOER_TRADE.get() : ModSounds.ENCHOER_AMBIENT.get();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ENCHOER_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ENCHOER_DEATH.get();
    }

    @Override
    protected SoundEvent getTradeUpdatedSound(boolean validTrade) {
        return validTrade ? ModSounds.ENCHOER_YES.get() : ModSounds.ENCHOER_NO.get();
    }

    @Override
    public SoundEvent getNotifyTradeSound() {
        return ModSounds.ENCHOER_YES.get();
    }
}
