package com.thesift.entity.jungle;

import com.thesift.block.GiantPitcherBlock;
import com.thesift.block.LingeringGlowBlock;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModCaveJungle;
import com.thesift.registry.ModParticles;
import java.util.EnumSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealSource;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.VegetationBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * P4 Cave Jungle: the Glow Fly - a big golden firefly-moth whose abdomen is a lantern. It drinks light: it hovers
 * against glowing moss, glowbells and torches and its lantern fills; then it carries the light to plants and pours
 * it into them, and they grow. That makes it the favourite prey of the Giant Pitcher Plant, which smells of a plant
 * in need of light: a Glow Fly that lingers over its mouth is snapped up. Hit one and it flashes - everything
 * watching is dazzled - and darts away. Its light gland makes the Glow Lamp, and a living Glow Fly near a Europhy
 * Table steadies Bauxite like Nib Dust does (#thesift:bauxite_stabilizers).
 */
public class GlowFly extends PathfinderMob {
    public static final int MAX_CHARGE = 100;
    private static final EntityDataAccessor<Integer> CHARGE = SynchedEntityData.defineId(GlowFly.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> FLEEING = SynchedEntityData.defineId(GlowFly.class, EntityDataSerializers.BOOLEAN);
    private static final byte EVENT_FLASH = 100;
    private static final byte EVENT_ABSORB = 101;
    private static final byte EVENT_GIVE = 102;

    public final AnimationState flashAnimation = new AnimationState();
    public final AnimationState absorbAnimation = new AnimationState();
    public final AnimationState giveAnimation = new AnimationState();

    private int fleeTicks;
    private @Nullable Vec3 fleeFrom;

    public GlowFly(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl<>(this, 20, true);
        this.setNoGravity(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes().add(Attributes.MAX_HEALTH, 8.0).add(Attributes.FLYING_SPEED, 0.55).add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 20.0);
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        return new FlyingPathNavigation(this, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(CHARGE, 40);
        builder.define(FLEEING, false);
    }

    /** S1 never freeze. */
    @Override
    public int getNoActionTime() {
        return 0;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new FleeGoal());
        this.goalSelector.addGoal(3, new AbsorbGoal());
        this.goalSelector.addGoal(4, new FeedPlantGoal());
        this.goalSelector.addGoal(8, new WaterAvoidingRandomFlyingGoal(this, 0.8));
        this.goalSelector.addGoal(9, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(10, new RandomLookAroundGoal(this));
    }

    /** How much light its lantern holds (0 - {@link #MAX_CHARGE}). */
    public int getCharge() {
        return this.entityData.get(CHARGE);
    }

    public void setCharge(int charge) {
        this.entityData.set(CHARGE, Mth.clamp(charge, 0, MAX_CHARGE));
    }

    /** Dazzled its attacker and darting away. */
    public boolean isFleeing() {
        return this.entityData.get(FLEEING);
    }

    /** How brightly the lantern shines (the glow layer): never quite dark, pulsing slowly, brightest when full. */
    public float glow(float partialTicks) {
        float t = this.tickCount + partialTicks + this.getId() * 7.0F;
        float fill = 0.3F + 0.7F * this.getCharge() / (float) MAX_CHARGE;
        return Mth.clamp(fill * (0.8F + 0.2F * Mth.sin(t * 0.12F)), 0.2F, 1.0F);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(Math.max(2, 9 - this.getCharge() / 15)) == 0) {
                Vec3 tail = this.lanternPos();
                this.level().addParticle(ModParticles.GLOW_DUST.get(), tail.x + (this.random.nextDouble() - 0.5) * 0.3, tail.y - 0.1,
                        tail.z + (this.random.nextDouble() - 0.5) * 0.3, 0.0, -0.01, 0.0);
            }
            return;
        }
        ServerLevel server = (ServerLevel) this.level();
        if (this.fleeTicks > 0 && --this.fleeTicks == 0) {
            this.entityData.set(FLEEING, false);
        }
        // a lantern left in the dark fades slowly; one in good light tops itself up
        if (this.tickCount % 100 == 0) {
            int light = server.getMaxLocalRawBrightness(this.blockPosition());
            this.setCharge(this.getCharge() + (light >= 12 ? 2 : light <= 3 ? -1 : 0));
        }
        // the Giant Pitcher Plant's mouth: linger over it and it snaps you up
        if (this.tickCount % 5 == 0) {
            for (BlockPos p : new BlockPos[]{this.blockPosition(), this.blockPosition().below()}) {
                BlockState st = server.getBlockState(p);
                if (st.getBlock() instanceof GiantPitcherBlock && st.getValue(GiantPitcherBlock.HALF) == DoubleBlockHalf.UPPER
                        && this.random.nextFloat() < 0.35F) {
                    GiantPitcherBlock.devour(server, p, this);
                    return;
                }
            }
        }
    }

    /** Where the lantern hangs (behind and a little below the body). */
    public Vec3 lanternPos() {
        Vec3 back = Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(-0.55);
        return this.position().add(back).add(0.0, 0.3, 0.0);
    }

    // ------------------------------------------------------------------ the flash

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive()) {
            this.flash(level, source.getEntity());
        }
        return hurt;
    }

    /** A blinding burst of all its stored light; then it darts off. */
    private void flash(ServerLevel level, @Nullable Entity attacker) {
        int charge = this.getCharge();
        float power = 0.4F + 0.6F * charge / (float) MAX_CHARGE;
        level.broadcastEntityEvent(this, EVENT_FLASH);
        this.playSound(ModCaveJungle.GLOW_FLY_FLASH.get(), 1.0F, 1.0F + this.random.nextFloat() * 0.2F);
        Vec3 c = this.lanternPos();
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, (int) (14 + 20 * power), 0.15, 0.15, 0.15, 0.22);
        level.sendParticles(ModParticles.GLOW_DUST.get(), c.x, c.y, c.z, 24, 0.8, 0.6, 0.8, 0.05);
        level.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 12, 0.4, 0.4, 0.4, 0.1);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(4.5),
                v -> v != this && v.isAlive() && !(v instanceof GlowFly))) {
            if (e.hasLineOfSight(this)) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, (int) (30 + 50 * power), 0), this);
            }
        }
        this.setCharge(charge - 25);
        this.fleeFrom = attacker != null ? attacker.position() : this.position().add(this.random.nextGaussian(), 0.0, this.random.nextGaussian());
        this.fleeTicks = 100;
        this.entityData.set(FLEEING, true);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_FLASH -> this.flashAnimation.start(this.tickCount);
            case EVENT_ABSORB -> this.absorbAnimation.start(this.tickCount);
            case EVENT_GIVE -> this.giveAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putInt("Charge", this.getCharge());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.setCharge(input.getIntOr("Charge", 40));
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModCaveJungle.GLOW_FLY_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 140;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModCaveJungle.GLOW_FLY_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModCaveJungle.GLOW_FLY_DEATH.get();
    }

    @Override
    protected float getSoundVolume() {
        return 0.7F;
    }

    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, 0xE6BC3E, 0xFFF08A, KillBurst.STAR, ParticleTypes.GLOW);
    }

    /** A light source to drink from: a glowing block with open air beside it (not the Glow Lamp's own fading light). */
    private @Nullable BlockPos findLight() {
        BlockPos origin = this.blockPosition();
        for (int i = 0; i < 90; i++) {
            BlockPos p = origin.offset(this.random.nextInt(17) - 8, this.random.nextInt(11) - 5, this.random.nextInt(17) - 8);
            BlockState st = this.level().getBlockState(p);
            if (st.getLightEmission() < 8 || st.getBlock() instanceof LingeringGlowBlock || !st.getFluidState().isEmpty()) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos a = p.relative(d);
                if (this.level().isEmptyBlock(a)) {
                    return a;
                }
            }
        }
        return null;
    }

    /** A plant to feed: anything that grows (a crop, a sapling, a flower, moss) - or the deadly mouth of a Giant Pitcher. */
    private @Nullable BlockPos findPlant() {
        BlockPos origin = this.blockPosition();
        for (int i = 0; i < 90; i++) {
            BlockPos p = origin.offset(this.random.nextInt(21) - 10, this.random.nextInt(9) - 5, this.random.nextInt(21) - 10);
            BlockState st = this.level().getBlockState(p);
            if (st.getBlock() instanceof GiantPitcherBlock) {
                if (st.getValue(GiantPitcherBlock.HALF) == DoubleBlockHalf.UPPER) {
                    return p;
                }
                continue;
            }
            if (st.getBlock() instanceof BonemealableBlock bb && st.getBlock() instanceof VegetationBlock
                    && bb.isValidBonemealTarget(this.level(), p, st, BonemealSource.MOB)) {
                return p;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------ goals

    /** Dazzled its attacker: away from it, fast and high. */
    private class FleeGoal extends Goal {
        FleeGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return GlowFly.this.fleeTicks > 0 && GlowFly.this.fleeFrom != null;
        }

        @Override
        public void tick() {
            GlowFly f = GlowFly.this;
            Vec3 from = f.fleeFrom;
            if (from != null && (f.getNavigation().isDone() || f.tickCount % 10 == 0)) {
                Vec3 away = f.position().subtract(from);
                away = away.lengthSqr() < 1.0E-3 ? new Vec3(1.0, 0.0, 0.0) : away.normalize();
                Vec3 to = f.position().add(away.scale(8.0)).add(0.0, 2.0, 0.0);
                f.getNavigation().moveTo(to.x, to.y, to.z, 2.2);
            }
        }
    }

    /** Drinks from a light: hovers against it, antennae spread, the lantern filling. */
    private class AbsorbGoal extends Goal {
        private @Nullable BlockPos spot;
        private int time;
        private int cooldown;

        AbsorbGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.cooldown > 0 || GlowFly.this.getCharge() >= 80 || GlowFly.this.isFleeing()) {
                return false;
            }
            this.cooldown = 40;
            this.spot = GlowFly.this.findLight();
            return this.spot != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.spot != null && this.time < 160 && GlowFly.this.getCharge() < MAX_CHARGE && !GlowFly.this.isFleeing();
        }

        @Override
        public void start() {
            this.time = 0;
        }

        @Override
        public void stop() {
            this.spot = null;
        }

        @Override
        public void tick() {
            GlowFly f = GlowFly.this;
            BlockPos s = this.spot;
            if (s == null) {
                return;
            }
            Vec3 c = Vec3.atCenterOf(s);
            this.time++;
            if (f.distanceToSqr(c) > 1.6) {
                if (f.getNavigation().isDone() || this.time % 20 == 0) {
                    f.getNavigation().moveTo(c.x, c.y, c.z, 1.0);
                }
                return;
            }
            f.getNavigation().stop();
            f.getLookControl().setLookAt(c.x, c.y, c.z);
            if (this.time % 4 == 0) {
                f.setCharge(f.getCharge() + 3);
            }
            if (f.level() instanceof ServerLevel server) {
                if (this.time % 30 == 0) {
                    server.broadcastEntityEvent(f, EVENT_ABSORB);
                    f.playSound(ModCaveJungle.GLOW_FLY_ABSORB.get(), 0.6F, 1.0F + f.random.nextFloat() * 0.3F);
                }
                if (this.time % 3 == 0) {
                    // motes of light drawn off the glowing block into the lantern
                    Vec3 to = f.lanternPos().subtract(c);
                    server.sendParticles(ParticleTypes.GLOW, c.x, c.y, c.z, 0, to.x, to.y, to.z, 0.08);
                }
            }
        }
    }

    /** Carries its light to a plant and pours it in: the plant grows. A Giant Pitcher's mouth is a fatal mistake. */
    private class FeedPlantGoal extends Goal {
        private @Nullable BlockPos plant;
        private int time;
        private int cooldown;

        FeedPlantGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (--this.cooldown > 0 || GlowFly.this.getCharge() < 35 || GlowFly.this.isFleeing()) {
                return false;
            }
            this.cooldown = 60;
            this.plant = GlowFly.this.findPlant();
            return this.plant != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.plant != null && this.time < 200 && !GlowFly.this.isFleeing();
        }

        @Override
        public void start() {
            this.time = 0;
        }

        @Override
        public void stop() {
            this.plant = null;
        }

        @Override
        public void tick() {
            GlowFly f = GlowFly.this;
            BlockPos p = this.plant;
            if (p == null) {
                return;
            }
            this.time++;
            Vec3 over = Vec3.atCenterOf(p).add(0.0, 0.9, 0.0);
            if (f.distanceToSqr(over) > 1.0) {
                if (f.getNavigation().isDone() || this.time % 20 == 0) {
                    f.getNavigation().moveTo(over.x, over.y, over.z, 1.0);
                }
                return;
            }
            f.getNavigation().stop();
            f.getLookControl().setLookAt(over.x, over.y - 1.0, over.z);
            if (!(f.level() instanceof ServerLevel server) || this.time % 25 != 0) {
                return;
            }
            BlockState st = server.getBlockState(p);
            if (st.getBlock() instanceof GiantPitcherBlock) {
                return; // it hovers on, drawn by the scent - the pitcher's chance (see tick)
            }
            if (st.getBlock() instanceof BonemealableBlock bb && bb.isValidBonemealTarget(server, p, st, BonemealSource.MOB)) {
                bb.performBonemeal(server, f.random, p, st, BonemealSource.MOB);
            }
            server.broadcastEntityEvent(f, EVENT_GIVE);
            f.playSound(ModCaveJungle.GLOW_FLY_GIVE.get(), 0.8F, 1.0F + f.random.nextFloat() * 0.2F);
            Vec3 from = f.lanternPos();
            for (int i = 0; i < 8; i++) {
                double t = i / 7.0;
                server.sendParticles(ParticleTypes.GLOW, Mth.lerp(t, from.x, p.getX() + 0.5), Mth.lerp(t, from.y, p.getY() + 0.5),
                        Mth.lerp(t, from.z, p.getZ() + 0.5), 1, 0.05, 0.05, 0.05, 0.0);
            }
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER, p.getX() + 0.5, p.getY() + 0.6, p.getZ() + 0.5, 6, 0.3, 0.3, 0.3, 0.0);
            f.setCharge(f.getCharge() - 30);
            this.plant = null;
        }
    }
}
