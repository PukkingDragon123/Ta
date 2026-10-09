package com.thesift.block.entity;

import com.thesift.music.Resonance;
import com.thesift.music.Song;
import com.thesift.registry.ModEffects;
import com.thesift.registry.ModEurophy;
import com.thesift.registry.ModParticles;
import com.thesift.registry.ModSounds;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Europhy Table: four ingredient stations on its arms and the output in the centre. Music played close by
 * winds it up - each new note (repeats do not count) adds a charge, a whole song fills it - and when it is
 * full the gears race, the arms lift and the output forms in the centre over {@link #FORM_TICKS}.
 */
public class EurophyTableBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUTS = 4;
    public static final int OUTPUT = 4;
    public static final int SIZE = 5;
    public static final int DATA = 5;
    public static final int FORM_TICKS = 60;
    public static final double HEARING = 8.0;
    /** Menu status values (data slot 4). */
    public static final int IDLE = 0, READY = 1, NEEDS_HELPER = 2, FULL = 3, FORMING = 4;
    /** Model geometry shared with the renderer (tools/materials.py): arm hinge and pan distance in 1/16 block. */
    public static final float ARM_HINGE = 2.2F, PAN_AT = 3.6F, ARM_LIFT = 0.6F;

    private static final Set<EurophyTableBlockEntity> LOADED = Collections.newSetFromMap(new WeakHashMap<>());

    private final SimpleContainer items = new SimpleContainer(SIZE) {
        @Override
        public void setChanged() {
            super.setChanged();
            EurophyTableBlockEntity.this.contentsChanged();
        }

        @Override
        public boolean stillValid(Player player) {
            return EurophyTableBlockEntity.this.stillValid(player);
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int id) {
            EurophyTableBlockEntity t = EurophyTableBlockEntity.this;
            return switch (id) {
                case 0 -> t.charge;
                case 1 -> t.need;
                case 2 -> t.forming;
                case 3 -> FORM_TICKS;
                default -> t.status;
            };
        }

        @Override
        public void set(int id, int value) {
        }

        @Override
        public int getCount() {
            return DATA;
        }
    };

    private int charge;
    private int need;
    private int forming;
    private int lastPitch = -1;
    private int pitches;        // a bit per pitch heard in this tune
    private boolean song;       // a whole song filled it
    private int quiet;          // ticks since the last note
    private int status;
    private @Nullable String recipeId;

    // client-side animation
    public float spin, spinO, spinSpeed;
    public float crown, crownO;
    public float lift, liftO;
    public float pulse;
    public int age;
    private int seenCharge;

    public EurophyTableBlockEntity(BlockPos pos, BlockState state) {
        super(ModEurophy.EUROPHY_TABLE.get(), pos, state);
    }

    // ------------------------------------------------------------------ contents

    public SimpleContainer getItems() {
        return this.items;
    }

    public List<ItemStack> inputs() {
        List<ItemStack> l = new ArrayList<>(INPUTS);
        for (int i = 0; i < INPUTS; i++) {
            l.add(this.items.getItem(i));
        }
        return l;
    }

    public EurophyRecipes.@Nullable Match match() {
        return EurophyRecipes.match(this.inputs());
    }

    public int charge() {
        return this.charge;
    }

    public int need() {
        return this.need;
    }

    public boolean isForming() {
        return this.forming > 0;
    }

    public float formProgress(float partial) {
        return this.forming <= 0 ? 0.0F : Mth.clamp(1.0F - (this.forming - partial) / FORM_TICKS, 0.0F, 1.0F);
    }

    private boolean stillValid(Player player) {
        return this.level != null && this.level.getBlockEntity(this.worldPosition) == this
                && player.distanceToSqr(Vec3.atCenterOf(this.worldPosition)) <= 64.0;
    }

    private void contentsChanged() {
        if (this.level == null || this.level.isClientSide()) {
            return;
        }
        EurophyRecipes.Match m = this.match();
        String id = m == null ? null : m.recipe().id();
        if (id == null || !id.equals(this.recipeId)) {  // a different recipe starts a new tune
            this.charge = 0;
            this.lastPitch = -1;
            this.pitches = 0;
            this.song = false;
        }
        this.recipeId = id;
        this.need = m == null ? 0 : m.recipe().notes();
        this.status = this.computeStatus(m);
        this.sync();
    }

    private int computeStatus(EurophyRecipes.@Nullable Match m) {
        if (this.forming > 0) {
            return FORMING;
        }
        if (m == null) {
            return IDLE;
        }
        if (!this.outputFits(m)) {
            return FULL;
        }
        if (m.recipe().helper() != null && this.level instanceof ServerLevel server && this.findHelper(server, m.recipe().helper()) == null) {
            return NEEDS_HELPER;
        }
        return READY;
    }

    private boolean outputFits(EurophyRecipes.Match m) {
        ItemStack out = this.items.getItem(OUTPUT);
        if (out.isEmpty()) {
            return true;
        }
        ItemStack next = m.preview(this.inputs());
        return ItemStack.isSameItemSameComponents(out, next) && out.getCount() + next.getCount() <= out.getMaxStackSize();
    }

    private @Nullable LivingEntity findHelper(ServerLevel level, TagKey<EntityType<?>> tag) {
        List<LivingEntity> l = level.getEntitiesOfClass(LivingEntity.class, new AABB(this.worldPosition).inflate(HEARING),
                e -> e.isAlive() && e.getType().builtInRegistryHolder().is(tag));
        return l.isEmpty() ? null : l.get(0);
    }

    // ------------------------------------------------------------------ music

    /** SongEvents note listener (registered in ModEurophy): every table within earshot hears the note. */
    public static void hearNote(ServerLevel level, @Nullable Player player, Vec3 at, int pitch) {
        for (EurophyTableBlockEntity t : List.copyOf(LOADED)) {
            if (t.level == level && !t.isRemoved() && Vec3.atCenterOf(t.worldPosition).distanceToSqr(at) <= HEARING * HEARING) {
                t.onNote(level, player, pitch);
            }
        }
    }

    /** SongEvents song listener: a whole song fills the table at once. */
    public static void hearSong(ServerLevel level, @Nullable Player player, Vec3 at, Song song) {
        for (EurophyTableBlockEntity t : List.copyOf(LOADED)) {
            if (t.level == level && !t.isRemoved() && Vec3.atCenterOf(t.worldPosition).distanceToSqr(at) <= 12.0 * 12.0) {
                t.onSong(level, player);
            }
        }
    }

    private EurophyRecipes.@Nullable Match playable(ServerLevel level, @Nullable Player player) {
        if (this.forming > 0) {
            return null;
        }
        EurophyRecipes.Match m = this.match();
        if (m == null) {
            return null;
        }
        if (!this.outputFits(m)) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.thesift.europhy.output_full"));
            }
            return null;
        }
        if (m.recipe().helper() != null && this.findHelper(level, m.recipe().helper()) == null) {
            if (player != null) {
                player.sendOverlayMessage(Component.translatable("message.thesift.europhy.needs_helper"));
            }
            this.setStatus(NEEDS_HELPER);
            return null;
        }
        return m;
    }

    private void onNote(ServerLevel level, @Nullable Player player, int pitch) {
        EurophyRecipes.Match m = this.playable(level, player);
        if (m == null) {
            return;
        }
        this.quiet = 0;
        Vec3 c = Vec3.atCenterOf(this.worldPosition);
        // the note is drawn into the lens
        level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x, c.y + 1.1, c.z, 0, pitch / 24.0, 0.0, 0.0, 1.0);
        if (pitch == this.lastPitch) {
            return;  // a repeated note does not wind it up
        }
        this.lastPitch = pitch;
        this.pitches |= 1 << Mth.clamp(pitch, 0, 24);
        this.need = m.recipe().notes();
        this.charge = Math.min(this.need, this.charge + 1);
        level.playSound(null, this.worldPosition, ModSounds.EUROPHY_NOTE.get(), SoundSource.BLOCKS, 0.6F,
                0.6F + this.charge / (float) Math.max(1, this.need) * 0.9F);
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, c.x, c.y - 0.1, c.z, 4, 0.45, 0.1, 0.45, 0.05);
        if (this.charge >= this.need) {
            this.startForming(level);
        }
        this.status = this.computeStatus(m);
        this.sync();
    }

    private void onSong(ServerLevel level, @Nullable Player player) {
        EurophyRecipes.Match m = this.playable(level, player);
        if (m == null) {
            return;
        }
        this.need = m.recipe().notes();
        this.charge = this.need;
        this.song = true;
        this.startForming(level);
        this.status = FORMING;
        this.sync();
    }

    private void startForming(ServerLevel level) {
        this.forming = FORM_TICKS;
        this.status = FORMING;
        level.playSound(null, this.worldPosition, ModSounds.EUROPHY_CHARGE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        Resonance.pulse(level, this.worldPosition, 0.6F, 6);
    }

    private void setStatus(int s) {
        if (this.status != s) {
            this.status = s;
            this.setChanged();
        }
    }

    // ------------------------------------------------------------------ ticking

    public static void serverTick(Level level, BlockPos pos, BlockState state, EurophyTableBlockEntity t) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        LOADED.add(t);
        if (t.forming > 0) {
            t.forming--;
            int elapsed = FORM_TICKS - t.forming;
            if (elapsed % 20 == 1) {
                server.playSound(null, pos, ModSounds.EUROPHY_HUM.get(), SoundSource.BLOCKS, 0.9F, 0.8F + elapsed / (float) FORM_TICKS * 0.8F);
            }
            EurophyRecipes.Match m = t.match();
            if (elapsed % 4 == 0 && m != null && m.recipe().helper() != null) {
                // the helper's calm flows into the table as a stream of motes
                LivingEntity helper = t.findHelper(server, m.recipe().helper());
                if (helper != null) {
                    Vec3 from = helper.position().add(0, helper.getBbHeight() * 0.6, 0);
                    Vec3 to = Vec3.atCenterOf(pos).add(0, 0.5, 0);
                    for (int i = 0; i < 6; i++) {
                        Vec3 p = from.lerp(to, (i + (elapsed % 8) / 8.0) / 6.0);
                        server.sendParticles(ModParticles.GLOW_DUST.get(), p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0.0);
                    }
                }
            }
            if (t.forming == 0) {
                t.finish(server);
            }
        } else if (t.charge > 0 && ++t.quiet > 120 && t.quiet % 10 == 0) {
            // the tune fades when the music stops
            t.charge--;
            if (t.charge == 0) {
                t.lastPitch = -1;
                t.pitches = 0;
            }
            t.sync();
        } else if (level.getGameTime() % 40 == 0) {
            int s = t.computeStatus(t.match());
            if (s != t.status) {
                t.status = s;
                t.sync();
            }
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, EurophyTableBlockEntity t) {
        t.age++;
        t.spinO = t.spin;
        t.crownO = t.crown;
        t.liftO = t.lift;
        float fill = t.need > 0 ? t.charge / (float) t.need : 0.0F;
        float target = t.forming > 0 ? 0.55F : 0.02F + 0.22F * fill;
        t.spinSpeed += (target - t.spinSpeed) * 0.08F;
        t.spin += t.spinSpeed;
        t.crown += t.spinSpeed * (t.forming > 0 ? 0.35F : 0.2F);
        float liftTarget = t.forming > 0 ? 1.0F : fill * 0.3F;
        t.lift += (liftTarget - t.lift) * 0.12F;
        if (t.charge > t.seenCharge) {
            t.pulse = 1.0F;
        }
        t.seenCharge = t.charge;
        t.pulse *= 0.86F;
        var random = level.getRandom();
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        if (t.forming > 0) {
            t.forming--;
            // motes stream from every loaded arm into the centre, sparks fly from the racing gears
            for (int k = 0; k < INPUTS; k++) {
                if (t.items.getItem(k).isEmpty() || random.nextInt(2) != 0) {
                    continue;
                }
                Vec3 p = t.panPos(k, 1.0F);
                Vec3 to = new Vec3(cx, pos.getY() + 0.98, cz);
                Vec3 v = to.subtract(p).scale(0.12);
                level.addParticle(k % 2 == 1 ? ModParticles.STAR_SPARKLE.get() : ParticleTypes.END_ROD, p.x, p.y, p.z, v.x, v.y, v.z);
            }
            if (random.nextInt(3) == 0) {
                double a = random.nextDouble() * Math.PI * 2;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, cx + Math.cos(a) * 0.47, pos.getY() + 0.33, cz + Math.sin(a) * 0.47, 0, 0.02, 0);
            }
            if (random.nextInt(2) == 0) {  // light falling from the lens onto the forming output
                level.addParticle(ModParticles.STAR_SPARKLE.get(), cx + (random.nextDouble() - 0.5) * 0.2, pos.getY() + 1.25, cz
                        + (random.nextDouble() - 0.5) * 0.2, 0, -0.03, 0);
            }
        } else if (t.charge > 0 && random.nextInt(8) == 0) {
            level.addParticle(ModParticles.SIFT_NOTE.get(), cx + (random.nextDouble() - 0.5) * 0.6, pos.getY() + 1.1, cz + (random.nextDouble() - 0.5) * 0.6,
                    random.nextFloat(), 0, 0);
        }
    }

    /** Where ingredient k sits (on its arm's pan), in world space - shared by the renderer and the particles. */
    public Vec3 panPos(int k, float partial) {
        float yaw = Mth.lerp(partial, this.crownO, this.crown) + k * Mth.HALF_PI;
        float ang = Mth.lerp(partial, this.liftO, this.lift) * ARM_LIFT;
        double d = (ARM_HINGE + PAN_AT * Mth.cos(ang)) / 16.0;
        double up = 0.75 + PAN_AT * Mth.sin(ang) / 16.0 + 0.1;
        return new Vec3(this.worldPosition.getX() + 0.5 + d * Mth.sin(yaw), this.worldPosition.getY() + up,
                this.worldPosition.getZ() + 0.5 - d * Mth.cos(yaw));
    }

    private void finish(ServerLevel level) {
        EurophyRecipes.Match m = this.match();
        boolean helperOk = m != null && (m.recipe().helper() == null || this.findHelper(level, m.recipe().helper()) != null);
        if (m == null || !helperOk || !this.outputFits(m)) {
            // something was taken away (or the helper left): the table fizzles and the tune is lost
            level.playSound(null, this.worldPosition, ModSounds.EUROPHY_FIZZLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            Vec3 c = Vec3.atCenterOf(this.worldPosition);
            level.sendParticles(ParticleTypes.SMOKE, c.x, c.y + 0.5, c.z, 12, 0.2, 0.2, 0.2, 0.02);
            this.resetTune();
            this.status = this.computeStatus(m);
            this.sync();
            return;
        }
        int quality = this.song ? 25 : Integer.bitCount(this.pitches);
        List<ItemStack> table = this.inputs();
        ItemStack result = m.recipe().result().make(m.inputs(table), level, quality);
        for (int i = 0; i < m.slots().length; i++) {
            this.items.getItem(m.slots()[i]).shrink(m.recipe().inputs().get(i).count());
        }
        ItemStack out = this.items.getItem(OUTPUT);
        if (out.isEmpty()) {
            this.items.getItems().set(OUTPUT, result);
        } else {
            out.grow(result.getCount());
        }
        this.resetTune();
        Vec3 c = Vec3.atCenterOf(this.worldPosition);
        level.playSound(null, this.worldPosition, ModSounds.EUROPHY_CRAFT.get(), SoundSource.BLOCKS, 1.4F, 1.0F);
        level.sendParticles(ModParticles.STAR_SPARKLE.get(), c.x, c.y + 0.5, c.z, 36, 0.4, 0.3, 0.4, 0.12);
        level.sendParticles(ParticleTypes.END_ROD, c.x, c.y + 0.5, c.z, 10, 0.1, 0.1, 0.1, 0.08);
        level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y + 0.45, c.z, 0, 2.0, 0, 0, 1.0);
        for (int i = 0; i < 5; i++) {
            level.sendParticles(ModParticles.SIFT_NOTE.get(), c.x + (i - 2) * 0.2, c.y + 0.9, c.z, 0, i / 5.0, 0.0, 0.0, 1.0);
        }
        Resonance.pulse(level, this.worldPosition, 1.0F, 8);
        // everyone near feels the Euphoria
        for (ServerPlayer p : level.getEntitiesOfClass(ServerPlayer.class, new AABB(this.worldPosition).inflate(HEARING))) {
            p.addEffect(new MobEffectInstance(ModEffects.EUPHORIA, 20 * 20, 0));
        }
        this.items.setChanged();
    }

    private void resetTune() {
        this.forming = 0;
        this.charge = 0;
        this.lastPitch = -1;
        this.pitches = 0;
        this.song = false;
        this.quiet = 0;
    }

    // ------------------------------------------------------------------ menu

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.thesift.europhy_table");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new EurophyTableMenu(containerId, inventory, this.items, this.data);
    }

    // ------------------------------------------------------------------ sync & save

    private void sync() {
        this.setChanged();
        if (this.level != null) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (!level.isClientSide()) {
            LOADED.add(this);
        }
    }

    @Override
    public void setRemoved() {
        LOADED.remove(this);
        super.setRemoved();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        for (int i = 0; i < SIZE; i++) {
            ItemStack s = this.items.getItem(i);
            if (!s.isEmpty()) {
                output.store("Slot" + i, ItemStack.CODEC, s);
            }
        }
        output.putInt("Charge", this.charge);
        output.putInt("Need", this.need);
        output.putInt("Forming", this.forming);
        output.putInt("LastPitch", this.lastPitch);
        output.putInt("Pitches", this.pitches);
        output.putBoolean("Song", this.song);
        output.putInt("Status", this.status);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        for (int i = 0; i < SIZE; i++) {
            this.items.getItems().set(i, input.read("Slot" + i, ItemStack.CODEC).orElse(ItemStack.EMPTY));
        }
        this.charge = input.getIntOr("Charge", 0);
        this.need = input.getIntOr("Need", 0);
        this.forming = input.getIntOr("Forming", 0);
        this.lastPitch = input.getIntOr("LastPitch", -1);
        this.pitches = input.getIntOr("Pitches", 0);
        this.song = input.getBooleanOr("Song", false);
        this.status = input.getIntOr("Status", IDLE);
        EurophyRecipes.Match m = this.match();
        this.recipeId = m == null ? null : m.recipe().id();
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
        if (this.level != null) {
            Containers.dropContents(this.level, pos, this.items);
        }
        super.preRemoveSideEffects(pos, state);
    }
}
