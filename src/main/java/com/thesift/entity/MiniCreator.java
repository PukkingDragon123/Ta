package com.thesift.entity;

import com.thesift.knowledge.KnowledgeTracker;
import com.thesift.registry.ModKnowledge;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * F3 Knowledge &amp; lore: the Mini Creator, a small platypus in the Creator's white and gold with four little blocks
 * floating over his back. He guides one player: he appears beside them on their first steps in the Sift, gives
 * them the Knowledge Book and their first goal, and pops back to cheer and give the next goal each time one is met
 * ({@link KnowledgeTracker}). Use him to hear your current goal again. He waddles after you for a while, then waves,
 * spins and vanishes in a swirl of his blocks. He cannot be hurt.
 *
 * <p>Animations (entity events, ids 100..104 - none used by PathfinderMob): talk (he rears up on his tail and his
 * bill clacks), wave, celebrate (a hop and a spin, his blocks flung wide), appear and vanish.
 */
public class MiniCreator extends PathfinderMob {
    public static final byte EVENT_TALK = 100;
    public static final byte EVENT_WAVE = 101;
    public static final byte EVENT_CELEBRATE = 102;
    public static final byte EVENT_APPEAR = 103;
    public static final byte EVENT_POOF = 104;
    /** How long he keeps you company after his last words (5 minutes). */
    private static final int LINGER = 6000;

    public final AnimationState talkAnimation = new AnimationState();
    public final AnimationState waveAnimation = new AnimationState();
    public final AnimationState celebrateAnimation = new AnimationState();
    public final AnimationState appearAnimation = new AnimationState();
    public final AnimationState poofAnimation = new AnimationState();

    private java.util.@Nullable UUID guiding;
    private int linger = LINGER;
    private int poof = -1;

    public MiniCreator(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 20.0).add(Attributes.MOVEMENT_SPEED, 0.26).add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new FollowGuided());
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.7));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    @Override
    public int getNoActionTime() {
        return 0;
    }

    // ------------------------------------------------------------------ the guide

    public boolean isGuiding(@Nullable Player player) {
        return player != null && player.getUUID().equals(this.guiding);
    }

    private @Nullable Player guided() {
        return this.guiding == null ? null : this.level().getPlayerByUUID(this.guiding);
    }

    /**
     * The player's Mini Creator nearby, or (with {@code summon}) a new one that pops up beside them. Null if there is
     * nowhere to stand.
     */
    public static @Nullable MiniCreator guideOf(ServerPlayer player, boolean summon) {
        ServerLevel level = (ServerLevel) player.level();
        List<MiniCreator> near = level.getEntitiesOfClass(MiniCreator.class, player.getBoundingBox().inflate(48.0), g -> g.isGuiding(player) && g.poof < 0);
        if (!near.isEmpty()) {
            near.get(0).linger = LINGER;
            return near.get(0);
        }
        if (!summon) {
            return null;
        }
        Vec3 look = player.getLookAngle();
        for (int attempt = 0; attempt < 8; attempt++) {
            double a = Math.atan2(look.z, look.x) + (attempt % 2 == 0 ? 1 : -1) * attempt * 0.35;
            int x = Mth.floor(player.getX() + Math.cos(a) * 3.5);
            int z = Mth.floor(player.getZ() + Math.sin(a) * 3.5);
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            if (Math.abs(y - player.getY()) > 4) {
                y = Mth.floor(player.getY());
            }
            BlockPos at = new BlockPos(x, y, z);
            if (!level.getBlockState(at).isAir() || !level.getBlockState(at.below()).isSolid()) {
                continue;
            }
            MiniCreator guide = ModKnowledge.MINI_CREATOR.get().create(level, EntitySpawnReason.TRIGGERED);
            if (guide == null) {
                return null;
            }
            guide.snapTo(x + 0.5, y, z + 0.5, (float) Math.toDegrees(a) + 90.0F, 0.0F);
            guide.guiding = player.getUUID();
            level.addFreshEntity(guide);
            guide.appear();
            return guide;
        }
        return null;
    }

    public void talk() {
        this.linger = LINGER;
        this.level().broadcastEntityEvent(this, EVENT_TALK);
        this.playSound(ModKnowledge.GUIDE_TALK.get(), 0.9F, 0.95F + this.random.nextFloat() * 0.15F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 1.0, this.getZ(), 3, 0.4, 0.2, 0.4, 1.0);
        }
    }

    public void wave() {
        this.level().broadcastEntityEvent(this, EVENT_WAVE);
    }

    public void celebrate() {
        this.linger = LINGER;
        this.level().broadcastEntityEvent(this, EVENT_CELEBRATE);
        this.playSound(ModKnowledge.GUIDE_CELEBRATE.get(), 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, this.getX(), this.getY() + 0.8, this.getZ(), 10, 0.6, 0.4, 0.6, 0.0);
            server.sendParticles(ParticleTypes.NOTE, this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.6, 0.3, 0.6, 1.0);
        }
    }

    private void appear() {
        this.level().broadcastEntityEvent(this, EVENT_APPEAR);
        this.playSound(ModKnowledge.GUIDE_APPEAR.get(), 1.0F, 1.0F);
        if (this.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5, this.getZ(), 16, 0.5, 0.5, 0.5, 0.04);
            server.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 8, 0.4, 0.1, 0.4, 0.02);
        }
    }

    /** Waves goodbye, spins and vanishes in a swirl of his blocks (gone half a second later). */
    public void leave() {
        if (this.poof < 0) {
            this.poof = 14;
            this.level().broadcastEntityEvent(this, EVENT_POOF);
            this.playSound(ModKnowledge.GUIDE_POOF.get(), 1.0F, 1.0F);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            // a faint sparkle trails off his floating blocks
            if (this.random.nextFloat() < 0.06F) {
                double a = this.tickCount * 0.08 + this.random.nextInt(4) * Math.PI / 2.0;
                this.level().addParticle(ParticleTypes.WAX_ON, this.getX() + Math.cos(a) * 0.45, this.getY() + 0.85, this.getZ() + Math.sin(a) * 0.45,
                        0.0, 0.0, 0.0);
            }
            return;
        }
        if (this.poof >= 0) {
            if (--this.poof == 0 && this.level() instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.END_ROD, this.getX(), this.getY() + 0.5, this.getZ(), 20, 0.4, 0.5, 0.4, 0.06);
                this.discard();
            }
            return;
        }
        if (this.guiding != null) {
            Player p = this.guided();
            // he stays while you need him; with no player around (gone, or in another world) he soon goes home
            boolean away = p == null || p.distanceToSqr(this) > 64.0 * 64.0;
            this.linger -= away ? 20 : 1;
            if (this.linger <= 0) {
                this.leave();
            }
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp && this.poof < 0) {
            if (this.guiding == null) {
                this.guiding = player.getUUID(); // from a spawn egg: he guides whoever greets him first
            }
            if (this.isGuiding(player)) {
                this.getLookControl().setLookAt(player);
                KnowledgeTracker.tellCurrent(sp, this);
            } else {
                this.wave();
                this.playSound(ModKnowledge.GUIDE_AMBIENT.get(), 1.0F, 1.2F);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_TALK -> this.talkAnimation.start(this.tickCount);
            case EVENT_WAVE -> this.waveAnimation.start(this.tickCount);
            case EVENT_CELEBRATE -> this.celebrateAnimation.start(this.tickCount);
            case EVENT_APPEAR -> this.appearAnimation.start(this.tickCount);
            case EVENT_POOF -> this.poofAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ an untouchable little guide

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        return !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.storeNullable("Guiding", UUIDUtil.CODEC, this.guiding);
        output.putInt("Linger", this.linger);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.guiding = input.read("Guiding", UUIDUtil.CODEC).orElse(null);
        this.linger = input.getIntOr("Linger", LINGER);
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModKnowledge.GUIDE_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 240;
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModKnowledge.GUIDE_STEP.get(), 0.4F, 1.0F + this.random.nextFloat() * 0.3F);
    }

    @Override
    public SoundSource getSoundSource() {
        return SoundSource.NEUTRAL;
    }

    /** Keeps a few steps from the player he guides; catches up (a hop through the Rift) if left far behind. */
    private final class FollowGuided extends Goal {
        FollowGuided() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            Player p = MiniCreator.this.guided();
            return p != null && !p.isSpectator() && MiniCreator.this.poof < 0 && MiniCreator.this.distanceToSqr(p) > 6.0 * 6.0;
        }

        @Override
        public boolean canContinueToUse() {
            Player p = MiniCreator.this.guided();
            return p != null && MiniCreator.this.poof < 0 && MiniCreator.this.distanceToSqr(p) > 3.0 * 3.0;
        }

        @Override
        public void tick() {
            Player p = MiniCreator.this.guided();
            if (p == null) {
                return;
            }
            MiniCreator.this.getLookControl().setLookAt(p);
            if (MiniCreator.this.distanceToSqr(p) > 24.0 * 24.0 && p.onGround()) {
                MiniCreator.this.snapTo(p.getX(), p.getY(), p.getZ(), MiniCreator.this.getYRot(), 0.0F);
                MiniCreator.this.getNavigation().stop();
                MiniCreator.this.appear();
            } else if (MiniCreator.this.tickCount % 10 == 0) {
                MiniCreator.this.getNavigation().moveTo(p, 1.25);
            }
        }

        @Override
        public void stop() {
            MiniCreator.this.getNavigation().stop();
        }
    }
}
