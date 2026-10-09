package com.thesift.entity.caravan;

import com.thesift.block.CrystalColor;
import com.thesift.block.MusicCrystalBlock;
import com.thesift.block.entity.MusicCrystalBlockEntity;
import com.thesift.entity.KillBurst;
import com.thesift.registry.ModBlocks;
import com.thesift.registry.ModItems;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * CR2: the Caravan Queen - a vast, very fat hermit crab under a spiral shell crusted with gems, gems
 * growing out of her soft body too. Rare: she only comes where no other Queen keeps the caves.
 *
 * <ul>
 *   <li>She finds a cave of her own and settles in it: her lair. The Caravans around serve her and
 *   bring her the ore they dig. She eats it all - the ore goes into her hoard - and now and then a
 *   music crystal grows up in her lair from what she has eaten, sometimes with a treasure frozen
 *   inside.</li>
 *   <li>She leads her caravans' music: when they play together hers is the melody, rung on deep
 *   hymn bells.</li>
 *   <li>Territorial: a player who comes into her lair is warned (she rears up and clacks her great
 *   claw) and then attacked; any other mob that strays in is attacked at once. She spits gems -
 *   they drop where they land, real gems for the taking - and swats whoever comes close with her
 *   huge claw. Hurt her and she screams, and every Caravan around comes to her defence.</li>
 *   <li>Slain, she bursts in a shower of ores: everything she has eaten, and the ore grown into
 *   her body.</li>
 * </ul>
 */
public class CaravanQueen extends Caravan {
    // entity events: Caravan uses 100-104
    private static final byte EVENT_SPIT = 105;
    private static final byte EVENT_SWAT = 106;
    private static final byte EVENT_FEED = 107;
    private static final byte EVENT_ROAR = 108;
    private static final byte EVENT_SETTLE = 109;
    /** The spit: she rears and gulps, the gems fly at SPIT_AT, recovered by SPIT_END (ticks). */
    public static final int SPIT_AT = 14;
    private static final int SPIT_END = 30;
    /** The swat: the great claw swings back and comes round at SWAT_HIT, recovered by SWAT_END. */
    public static final int SWAT_HIT = 10;
    private static final int SWAT_END = 24;
    /** Dying: she swells and shudders, then bursts at BURST_AT (death ticks). */
    public static final int BURST_AT = 16;
    private static final int DEATH_END = 20;
    /** Her lair: this far round its heart (and LAIR_DEPTH up and down). */
    private static final double LAIR_RADIUS = 12.0;
    private static final double LAIR_DEPTH = 6.0;
    private static final int MAX_HOARD = 27;
    private static final int MAX_CRYSTALS = 14;
    /** What she spits, and how often: amethyst, lapis, quartz, emerald, prism, diamond. */
    private static final int[] GEM_WEIGHTS = {40, 25, 20, 6, 4, 2};

    public final AnimationState spitAnimation = new AnimationState();
    public final AnimationState swatAnimation = new AnimationState();
    public final AnimationState feedAnimation = new AnimationState();
    public final AnimationState roarAnimation = new AnimationState();
    public final AnimationState settleAnimation = new AnimationState();
    private @Nullable BlockPos lair;
    /** The ore her caravans have brought her: it bursts out of her when she dies. */
    private final List<ItemStack> hoard = new ArrayList<>();
    /** Ores waiting to burst out of her (gathered when she dies, flung out at BURST_AT). */
    private final List<ItemStack> shower = new ArrayList<>();
    private int gifts;
    private int attackCooldown;
    private int roarCooldown;
    private int lairCheck = 20;

    public CaravanQueen(EntityType<? extends Caravan> type, Level level) {
        super(type, level);
        this.xpReward = 40;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 160.0)
                .add(Attributes.ARMOR, 12.0)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0)
                .add(Attributes.ATTACK_DAMAGE, 10.0)
                .add(Attributes.MOVEMENT_SPEED, 0.18)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 28.0)
                .add(Attributes.STEP_HEIGHT, 1.0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FloatGoal(this));
        this.goalSelector.addGoal(2, new AttackGoal());
        this.goalSelector.addGoal(5, new LairGoal());
        this.goalSelector.addGoal(8, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(9, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        // players are warned first (Caravan#warnIntruders); any other mob in her lair is attacked at once
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Mob.class, true, (target, level) -> this.trespasser(target)));
    }

    // ------------------------------------------------------------------ the Queen of her own caravans

    /** She keeps her colour - from her spawn group, else the caravans around her - and a full crust. */
    @Override
    protected SpawnGroupData joinCaravan(ServerLevelAccessor level, EntitySpawnReason reason, @Nullable SpawnGroupData data) {
        CrystalColor colour = data instanceof Group group ? group.colour() : caravanColour(level, this.blockPosition(), this.random);
        this.setVariant(colour);
        this.setGrowth(100);
        return new Group(colour);
    }

    /** A Queen serves no other: she is her own caravans' Queen. */
    @Override
    public CaravanQueen queen() {
        return this;
    }

    /** A Queen is never a soldier (her strength is her own) and never grows or shrinks. */
    @Override
    public void setSoldier(boolean soldier) {
    }

    @Override
    protected void updateSize() {
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    public boolean requiresCustomPersistence() {
        return true;
    }

    /** The heart of her lair, or null while she is still looking for one. */
    public @Nullable BlockPos lair() {
        return this.lair;
    }

    /** True for a spot inside her lair (a Queen still looking for one has none). */
    public boolean inLair(Vec3 at) {
        BlockPos l = this.lair;
        if (l == null) {
            return false;
        }
        Vec3 c = Vec3.atBottomCenterOf(l);
        double dx = at.x - c.x;
        double dz = at.z - c.z;
        return dx * dx + dz * dz < LAIR_RADIUS * LAIR_RADIUS && Math.abs(at.y - c.y) < LAIR_DEPTH;
    }

    /** Mobs that are no part of her caravans, in her lair: she spits at them on sight. */
    private boolean trespasser(LivingEntity target) {
        return !this.isCalm() && outsider(target) && this.inLair(target.position());
    }

    /** A cave to settle in: no sky over it, out of the daylight, firm ground underfoot. */
    private boolean goodLair(BlockPos pos) {
        Level level = this.level();
        return this.onGround() && !level.canSeeSky(pos.above(3)) && level.getBrightness(LightLayer.SKY, pos.above()) <= 4
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    private void settle(ServerLevel level) {
        this.lair = this.blockPosition();
        level.broadcastEntityEvent(this, EVENT_SETTLE);
        this.playSound(ModSounds.CARAVAN_QUEEN_SETTLE.get(), 1.6F, 0.9F + this.random.nextFloat() * 0.1F);
        BlockState ground = level.getBlockState(this.blockPosition().below());
        if (!ground.isAir()) {
            level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, ground), this.getX(), this.getY() + 0.1, this.getZ(), 40, 1.1, 0.1, 1.1, 0.15);
        }
        level.sendParticles(new DustParticleOptions(this.colour().rgb(), 1.4F), this.getX(), this.getY() + 1.2, this.getZ(), 24, 1.0, 0.8, 1.0, 0.0);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.level() instanceof ServerLevel server) {
            if (this.attackCooldown > 0) {
                this.attackCooldown--;
            }
            if (this.roarCooldown > 0) {
                this.roarCooldown--;
            }
            if (this.lair == null && --this.lairCheck <= 0) {
                this.lairCheck = 40;
                if (this.goodLair(this.blockPosition())) {
                    this.settle(server);
                }
            }
            // she gives up on whoever flees far from her lair
            LivingEntity t = this.getTarget();
            if (t != null && (this.distanceToSqr(t) > 32.0 * 32.0
                    || this.lair != null && t.distanceToSqr(Vec3.atCenterOf(this.lair)) > (LAIR_RADIUS + 16.0) * (LAIR_RADIUS + 16.0))) {
                this.setTarget(null);
            }
        } else if (this.random.nextInt(5) == 0) {
            // motes of light rising off the gems on her shell
            this.level().addParticle(ModParticles.GLOW_DUST.get(), this.getRandomX(0.7), this.getY() + this.getBbHeight() * (0.6 + this.random.nextFloat() * 0.4),
                    this.getRandomZ(0.7), 0.0, 0.025, 0.0);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        boolean hurt = super.hurtServer(level, source, damage);
        if (hurt && this.isAlive() && source.getEntity() instanceof LivingEntity attacker && !(attacker instanceof Caravan)
                && !(attacker instanceof CaravanLarva) && this.roarCooldown <= 0) {
            // she screams for her caravans, and every Caravan around comes to her defence
            this.roarCooldown = 200;
            level.broadcastEntityEvent(this, EVENT_ROAR);
            this.playSound(ModSounds.CARAVAN_QUEEN_ROAR.get(), 2.5F, 0.95F + this.random.nextFloat() * 0.1F);
            for (Caravan c : level.getEntitiesOfClass(Caravan.class, this.getBoundingBox().inflate(32.0), o -> o != this && o.isAlive())) {
                c.defend(attacker);
            }
            for (CaravanLarva l : level.getEntitiesOfClass(CaravanLarva.class, this.getBoundingBox().inflate(16.0), CaravanLarva::isAlive)) {
                l.defend(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void handleEntityEvent(byte id) {
        switch (id) {
            case EVENT_SPIT -> this.spitAnimation.start(this.tickCount);
            case EVENT_SWAT -> this.swatAnimation.start(this.tickCount);
            case EVENT_FEED -> this.feedAnimation.start(this.tickCount);
            case EVENT_ROAR -> this.roarAnimation.start(this.tickCount);
            case EVENT_SETTLE -> this.settleAnimation.start(this.tickCount);
            default -> super.handleEntityEvent(id);
        }
    }

    // ------------------------------------------------------------------ her caravans' gifts

    /** Where her mouth is, under the lip of her shell. */
    private Vec3 mouth() {
        return this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.15)).add(0.0, 0.75, 0.0);
    }

    /** A Caravan lays its ore before her: she eats it, and it goes into her hoard (and sometimes a crystal grows). */
    public void receive(Caravan from, ItemStack gift) {
        if (!(this.level() instanceof ServerLevel level) || gift.isEmpty()) {
            return;
        }
        level.broadcastEntityEvent(this, EVENT_FEED);
        this.playSound(ModSounds.CARAVAN_QUEEN_FEED.get(), 1.3F, 0.9F + this.random.nextFloat() * 0.2F);
        Vec3 m = this.mouth();
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, ItemStackTemplate.fromNonEmptyStack(gift)), m.x, m.y, m.z, 12, 0.25, 0.2, 0.25, 0.08);
        level.sendParticles(new DustParticleOptions(this.colour().rgb(), 1.0F), m.x, m.y + 0.2, m.z, 6, 0.3, 0.2, 0.3, 0.0);
        this.hoard(gift.copy());
        this.heal(4.0F);
        this.getLookControl().setLookAt(from, 30.0F, 30.0F);
        if (++this.gifts % 3 == 0) {
            this.growCrystal(level, gift);
        }
    }

    private void hoard(ItemStack stack) {
        for (ItemStack h : this.hoard) {
            if (ItemStack.isSameItemSameComponents(h, stack) && h.getCount() < h.getMaxStackSize()) {
                int move = Math.min(stack.getCount(), h.getMaxStackSize() - h.getCount());
                h.grow(move);
                stack.shrink(move);
                if (stack.isEmpty()) {
                    return;
                }
            }
        }
        if (this.hoard.size() < MAX_HOARD) {
            this.hoard.add(stack);
        }
    }

    /** A music crystal of her colour grows up somewhere in her lair from what she has eaten - perhaps with the ore frozen inside. */
    private void growCrystal(ServerLevel level, ItemStack ore) {
        BlockPos h = this.lair != null ? this.lair : this.blockPosition();
        int crystals = 0;
        for (BlockPos p : BlockPos.betweenClosed(h.offset(-8, -3, -8), h.offset(8, 5, 8))) {
            if (level.getBlockState(p).is(ModBlocks.MUSIC_CRYSTAL.get())) {
                crystals++;
            }
        }
        if (crystals >= MAX_CRYSTALS) {
            return;
        }
        for (int attempt = 0; attempt < 24; attempt++) {
            BlockPos p = h.offset(this.random.nextInt(13) - 6, this.random.nextInt(7) - 2, this.random.nextInt(13) - 6);
            if (!level.getBlockState(p).isAir()) {
                continue;
            }
            for (Direction facing : new Direction[]{Direction.UP, Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST}) {
                BlockState state = ModBlocks.MUSIC_CRYSTAL.get().defaultBlockState().setValue(MusicCrystalBlock.COLOR, this.colour())
                        .setValue(MusicCrystalBlock.FACING, facing);
                if (!state.canSurvive(level, p)) {
                    continue;
                }
                ItemStack treasure = ItemStack.EMPTY;
                if (this.random.nextInt(3) == 0 && ore.getItem() instanceof BlockItem bi) {
                    List<ItemStack> drops = Block.getDrops(bi.getBlock().defaultBlockState(), level, p, null, this, new ItemStack(Items.DIAMOND_PICKAXE));
                    if (!drops.isEmpty()) {
                        treasure = drops.get(0);
                    }
                }
                level.setBlock(p, state.setValue(MusicCrystalBlock.FROZEN, !treasure.isEmpty()), Block.UPDATE_ALL);
                if (!treasure.isEmpty() && level.getBlockEntity(p) instanceof MusicCrystalBlockEntity be) {
                    be.setItem(treasure);
                }
                level.playSound(null, p, ModSounds.CARAVAN_BUILD.get(), SoundSource.HOSTILE, 1.0F, 0.9F + this.random.nextFloat() * 0.3F);
                level.sendParticles(ModParticles.STAR_SPARKLE.get(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 10, 0.3, 0.3, 0.3, 0.02);
                return;
            }
        }
    }

    // ------------------------------------------------------------------ fighting

    private ItemStack pickGem() {
        int total = 0;
        for (int w : GEM_WEIGHTS) {
            total += w;
        }
        int r = this.random.nextInt(total);
        int i = 0;
        while (r >= GEM_WEIGHTS[i]) {
            r -= GEM_WEIGHTS[i];
            i++;
        }
        Item gem = switch (i) {
            case 0 -> Items.AMETHYST_SHARD;
            case 1 -> Items.LAPIS_LAZULI;
            case 2 -> Items.QUARTZ;
            case 3 -> Items.EMERALD;
            case 4 -> ModItems.PRISM_GEM.get();
            default -> Items.DIAMOND;
        };
        return new ItemStack(gem);
    }

    /** One to three gems spat in a little fan at the target; each drops where it lands. */
    private void spitGems(ServerLevel level, LivingEntity target) {
        Vec3 from = this.mouth();
        int n = 1 + this.random.nextInt(3);
        for (int i = 0; i < n; i++) {
            SpatGem gem = new SpatGem(level, this, this.pickGem());
            gem.setPos(from.x, from.y, from.z);
            double dx = target.getX() - from.x;
            double dz = target.getZ() - from.z;
            double dy = target.getY(0.5) - from.y;
            double flat = Math.sqrt(dx * dx + dz * dz);
            Vec3 dir = new Vec3(dx, dy + flat * 0.18, dz).yRot((i - (n - 1) * 0.5F) * 8.0F * Mth.DEG_TO_RAD);
            gem.shoot(dir.x, dir.y, dir.z, 1.15F, 3.0F);
            level.addFreshEntity(gem);
        }
        this.playSound(ModSounds.CARAVAN_QUEEN_SPIT.get(), 2.0F, 0.9F + this.random.nextFloat() * 0.2F);
        level.sendParticles(new DustParticleOptions(this.colour().rgb(), 1.2F), from.x, from.y, from.z, 10, 0.3, 0.2, 0.3, 0.0);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), from.x, from.y, from.z, 8, 0.3, 0.2, 0.3, 0.06);
    }

    /** The great claw swings round in front of her: it hurts and knocks back everything it meets. */
    private void swat(ServerLevel level) {
        Vec3 at = this.position().add(Vec3.directionFromRotation(0.0F, this.yBodyRot).scale(1.9));
        this.playSound(ModSounds.CARAVAN_QUEEN_SWAT.get(), 1.8F, 0.85F + this.random.nextFloat() * 0.15F);
        float damage = (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE);
        for (LivingEntity victim : level.getEntitiesOfClass(LivingEntity.class, new AABB(at, at).inflate(1.8, 1.4, 1.8),
                le -> le != this && le.isAlive() && !(le instanceof Caravan) && !(le instanceof CaravanLarva))) {
            if (victim.hurtServer(level, this.damageSources().mobAttack(this), damage)) {
                Vec3 push = victim.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
                push = push.lengthSqr() < 1.0E-4 ? Vec3.directionFromRotation(0.0F, this.getYRot()) : push.normalize();
                victim.push(push.x * 1.2, 0.4, push.z * 1.2);
            }
        }
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, at.x, this.getY() + 1.0, at.z, 1, 0.0, 0.0, 0.0, 0.0);
    }

    /** Spits gems from range and swats whoever closes in; she only chases so far from her lair. */
    private final class AttackGoal extends Goal {
        private static final int NONE = 0;
        private static final int SPIT = 1;
        private static final int SWAT = 2;
        private int move = NONE;
        private int ticks;

        AttackGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = CaravanQueen.this.getTarget();
            return t != null && t.isAlive() && !CaravanQueen.this.isCalm();
        }

        @Override
        public boolean canContinueToUse() {
            return this.move != NONE || this.canUse();
        }

        @Override
        public void start() {
            this.move = NONE;
        }

        @Override
        public void stop() {
            this.move = NONE;
            CaravanQueen.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            CaravanQueen q = CaravanQueen.this;
            LivingEntity t = q.getTarget();
            if (!(q.level() instanceof ServerLevel level)) {
                return;
            }
            if (this.move == NONE) {
                if (t == null) {
                    return;
                }
                q.getLookControl().setLookAt(t, 30.0F, 30.0F);
                double d = q.distanceToSqr(t);
                double reach = q.getBbWidth() * 0.5 + t.getBbWidth() * 0.5 + 1.4;
                if (q.attackCooldown <= 0 && d < reach * reach) {
                    this.begin(level, SWAT, EVENT_SWAT);
                } else if (q.attackCooldown <= 0 && d < 24.0 * 24.0 && q.hasLineOfSight(t)) {
                    this.begin(level, SPIT, EVENT_SPIT);
                } else if (q.tickCount % 10 == 0) {
                    BlockPos l = q.lair;
                    if (l == null || t.distanceToSqr(Vec3.atCenterOf(l)) < (LAIR_RADIUS + 4.0) * (LAIR_RADIUS + 4.0)) {
                        q.getNavigation().moveTo(t, 1.0);
                    } else {
                        q.getNavigation().stop();
                    }
                }
                return;
            }
            this.ticks++;
            q.getNavigation().stop();
            if (t != null && this.ticks < (this.move == SWAT ? SWAT_HIT - 2 : SPIT_AT)) {
                // turn to face the target while winding up
                float yaw = (float) (Mth.atan2(t.getZ() - q.getZ(), t.getX() - q.getX()) * Mth.RAD_TO_DEG) - 90.0F;
                float turned = Mth.approachDegrees(q.getYRot(), yaw, 10.0F);
                q.setYRot(turned);
                q.setYBodyRot(turned);
                q.setYHeadRot(turned);
            }
            if (this.move == SWAT) {
                if (this.ticks == SWAT_HIT) {
                    q.swat(level);
                } else if (this.ticks >= SWAT_END) {
                    this.move = NONE;
                    q.attackCooldown = 18 + q.random.nextInt(12);
                }
            } else if (this.ticks == SPIT_AT && t != null && t.isAlive()) {
                q.spitGems(level, t);
            } else if (this.ticks >= SPIT_END) {
                this.move = NONE;
                q.attackCooldown = 30 + q.random.nextInt(25);
            }
        }

        private void begin(ServerLevel level, int what, byte event) {
            this.move = what;
            this.ticks = 0;
            CaravanQueen.this.getNavigation().stop();
            level.broadcastEntityEvent(CaravanQueen.this, event);
        }
    }

    // ------------------------------------------------------------------ her lair

    /** Paces slowly about her lair; while she has none she roams the caves looking for one. */
    private final class LairGoal extends Goal {
        LairGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            CaravanQueen q = CaravanQueen.this;
            return q.getTarget() == null && !q.isJamming() && q.random.nextInt(q.lair == null ? 25 : 50) == 0;
        }

        @Override
        public boolean canContinueToUse() {
            CaravanQueen q = CaravanQueen.this;
            return !q.getNavigation().isDone() && q.getTarget() == null && !q.isJamming();
        }

        @Override
        public void start() {
            CaravanQueen q = CaravanQueen.this;
            BlockPos l = q.lair;
            if (l == null) {
                Vec3 to = DefaultRandomPos.getPos(q, 14, 6);
                if (to != null) {
                    q.getNavigation().moveTo(to.x, to.y, to.z, 0.8);
                }
            } else if (q.distanceToSqr(Vec3.atBottomCenterOf(l)) > LAIR_RADIUS * LAIR_RADIUS) {
                q.getNavigation().moveTo(l.getX() + 0.5, l.getY(), l.getZ() + 0.5, 0.8);
            } else {
                double a = q.random.nextDouble() * Math.PI * 2.0;
                double r = 2.0 + q.random.nextDouble() * 5.0;
                q.getNavigation().moveTo(l.getX() + 0.5 + Math.cos(a) * r, l.getY(), l.getZ() + 0.5 + Math.sin(a) * r, 0.6);
            }
        }
    }

    // ------------------------------------------------------------------ death: a shower of ores

    /** Ores for the burst, by weight: what grew into her body besides what her caravans brought. */
    private Item pickOre() {
        int r = this.random.nextInt(100);
        if (r < 18) {
            return Items.COAL_ORE;
        } else if (r < 32) {
            return Items.COPPER_ORE;
        } else if (r < 46) {
            return Items.IRON_ORE;
        } else if (r < 54) {
            return Items.GOLD_ORE;
        } else if (r < 64) {
            return Items.REDSTONE_ORE;
        } else if (r < 72) {
            return Items.LAPIS_ORE;
        } else if (r < 78) {
            return Items.NETHER_QUARTZ_ORE;
        } else if (r < 84) {
            return ModItems.SIFTITE_ORE.get();
        } else if (r < 89) {
            return Items.EMERALD_ORE;
        } else if (r < 94) {
            return Items.DIAMOND_ORE;
        } else if (r < 97) {
            return ModItems.PRISM_ORE.get();
        }
        return Items.DEEPSLATE_DIAMOND_ORE;
    }

    /** Gathers the burst: her whole hoard and a heap of ores of every kind (only when mobs drop loot). */
    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        this.shower.addAll(this.hoard);
        this.hoard.clear();
        int ores = 8 + this.random.nextInt(5) + (killedByPlayer ? 4 : 0);
        for (int i = 0; i < ores; i++) {
            this.shower.add(new ItemStack(this.pickOre()));
        }
    }

    /** She swells and shudders as she dies, then bursts in a shower of ores. */
    @Override
    protected void tickDeath() {
        this.deathTime++;
        if (!(this.level() instanceof ServerLevel level)) {
            return;
        }
        if (this.deathTime < BURST_AT && this.deathTime % 4 == 0) {
            level.sendParticles(new DustParticleOptions(this.colour().rgb(), 1.5F), this.getX(), this.getY() + 1.2, this.getZ(), 6, 0.9, 0.7, 0.9, 0.0);
        }
        if (this.deathTime == BURST_AT) {
            this.burst(level);
        }
        if (this.deathTime >= DEATH_END && !this.isRemoved()) {
            level.broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    private void burst(ServerLevel level) {
        double x = this.getX();
        double y = this.getY() + 1.1;
        double z = this.getZ();
        level.playSound(null, x, y, z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2.0F, 1.2F);
        level.playSound(null, x, y, z, ModSounds.CARAVAN_QUEEN_BURST.get(), SoundSource.HOSTILE, 2.5F, 1.0F);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        level.sendParticles(new DustParticleOptions(this.colour().rgb(), 2.0F), x, y, z, 60, 1.2, 1.0, 1.2, 0.0);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), x, y, z, 50, 1.4, 1.2, 1.4, 0.2);
        for (ItemStack stack : this.shower) {
            double a = this.random.nextDouble() * Math.PI * 2.0;
            double s = 0.15 + this.random.nextDouble() * 0.3;
            ItemEntity ore = new ItemEntity(level, x, y, z, stack, Math.cos(a) * s, 0.35 + this.random.nextDouble() * 0.35, Math.sin(a) * s);
            ore.setDefaultPickUpDelay();
            level.addFreshEntity(ore);
        }
        this.shower.clear();
        // the blast throws back whoever stands too close (it does not break the cave)
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(3.0), le -> le != this && le.isAlive())) {
            Vec3 push = e.position().subtract(this.position()).multiply(1.0, 0.0, 1.0);
            if (push.lengthSqr() > 1.0E-4) {
                push = push.normalize();
                e.push(push.x * 0.9, 0.35, push.z * 0.9);
            }
        }
    }

    // ------------------------------------------------------------------ sounds and saving

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModSounds.CARAVAN_QUEEN_AMBIENT.get();
    }

    @Override
    public int getAmbientSoundInterval() {
        return 200;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.CARAVAN_QUEEN_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.CARAVAN_QUEEN_DEATH.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        this.playSound(ModSounds.CARAVAN_QUEEN_STEP.get(), 0.6F, 0.9F + this.random.nextFloat() * 0.15F);
    }

    @Override
    public float getVoicePitch() {
        return 0.9F + (this.random.nextFloat() - 0.5F) * 0.1F;
    }

    @Override
    protected float getSoundVolume() {
        return 1.6F;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (this.lair != null) {
            output.store("Lair", BlockPos.CODEC, this.lair);
        }
        if (!this.hoard.isEmpty()) {
            output.store("Hoard", ItemStack.CODEC.listOf(), List.copyOf(this.hoard));
        }
        output.putInt("Gifts", this.gifts);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.lair = input.read("Lair", BlockPos.CODEC).orElse(null);
        this.hoard.clear();
        input.read("Hoard", ItemStack.CODEC.listOf()).ifPresent(this.hoard::addAll);
        this.gifts = input.getIntOr("Gifts", 0);
    }

    /** A great burst of crystal in her colour. */
    @Override
    public void makePoofParticles() {
        KillBurst.pop(this, this.colour().rgb(), 0xFFFFFF, KillBurst.STAR, ModParticles.STAR_SPARKLE.get());
        KillBurst.pop(this, this.colour().rgb(), this.colour().rgb(), KillBurst.NOTE, ModParticles.GLOW_DUST.get());
    }
}
