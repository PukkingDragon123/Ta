package com.thesift.block.entity;

import com.thesift.block.EchoerDeviceBlock;
import com.thesift.registry.ModEchoer;
import com.thesift.registry.ModParticles;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** The Echoer's charge-up and beam (see {@link EchoerDeviceBlock}). */
public class EchoerDeviceBlockEntity extends BlockEntity {
    public static final int CHARGE_TIME = 24;
    /** It mines like a diamond pickaxe would (no enchantments). */
    private static final ItemStack TOOL = new ItemStack(Items.DIAMOND_PICKAXE);

    private int charge;
    private int range;

    public EchoerDeviceBlockEntity(BlockPos pos, BlockState state) {
        super(ModEchoer.ECHOER_DEVICE_BE.get(), pos, state);
    }

    /** Starts charging a shot of the given range (ignored while already charging). */
    public void charge(ServerLevel level, BlockPos pos, BlockState state, int blocks) {
        if (this.charge > 0) {
            return;
        }
        this.charge = CHARGE_TIME;
        this.range = Mth.clamp(blocks, 1, 16);
        level.setBlock(pos, state.setValue(EchoerDeviceBlock.CHARGING, true), Block.UPDATE_CLIENTS);
        level.playSound(null, pos, ModEchoer.DEVICE_CHARGE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EchoerDeviceBlockEntity device) {
        if (device.charge <= 0 || !(level instanceof ServerLevel server)) {
            return;
        }
        if (--device.charge == 0) {
            device.fire(server, pos, state);
            server.setBlock(pos, server.getBlockState(pos).setValue(EchoerDeviceBlock.CHARGING, false), Block.UPDATE_CLIENTS);
        }
        device.setChanged();
    }

    private void fire(ServerLevel level, BlockPos pos, BlockState state) {
        Direction f = state.getValue(EchoerDeviceBlock.FACING);
        Vec3 from = Vec3.atCenterOf(pos).add(f.getStepX() * 0.5, f.getStepY() * 0.5, f.getStepZ() * 0.5);
        BlockPos hit = null;
        boolean broke = false;
        int steps = this.range;
        for (int i = 1; i <= this.range; i++) {
            BlockPos at = pos.relative(f, i);
            BlockState st = level.getBlockState(at);
            if (st.isAir() || st.getCollisionShape(level, at).isEmpty() && !st.getFluidState().isEmpty()) {
                continue; // through air and fluids
            }
            hit = at;
            steps = i;
            if (st.getDestroySpeed(level, at) >= 0.0F && !st.hasBlockEntity()) {
                List<ItemStack> drops = Block.getDrops(st, level, at, null, null, TOOL);
                level.destroyBlock(at, false);
                for (ItemStack d : drops) {
                    this.deliver(level, pos, f, d);
                }
                broke = true;
            }
            break;
        }
        // the beam: a line of sonic rings out to what it hit (or as far as it reaches)
        Vec3 dir = new Vec3(f.getStepX(), f.getStepY(), f.getStepZ());
        double len = hit == null ? this.range : steps - 0.5;
        for (double d = 0.0; d <= len; d += 1.0) {
            Vec3 p = from.add(dir.scale(d));
            level.sendParticles(ParticleTypes.SONIC_BOOM, p.x, p.y, p.z, 1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, pos, ModEchoer.DEVICE_FIRE.get(), SoundSource.BLOCKS, 1.2F, 1.0F);
        if (hit != null && !broke) {
            Vec3 c = Vec3.atCenterOf(hit);
            level.sendParticles(ModParticles.RESONANCE_RING.get(), c.x, c.y, c.z, 0, 1.0, 0.0, 0.0, 1.0);
            level.playSound(null, hit, ModEchoer.DEVICE_FIZZLE.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
    }

    /** Into a touching container (the back first), otherwise out of the top. */
    private void deliver(ServerLevel level, BlockPos pos, Direction facing, ItemStack stack) {
        ItemStack left = stack;
        for (int i = -1; i < Direction.values().length && !left.isEmpty(); i++) {
            Direction d = i < 0 ? facing.getOpposite() : Direction.values()[i];
            if (d == facing || (i >= 0 && d == facing.getOpposite())) {
                continue;
            }
            Container c = HopperBlockEntity.getContainerAt(level, pos.relative(d));
            if (c != null) {
                left = HopperBlockEntity.addItem(null, c, left, d.getOpposite());
            }
        }
        if (!left.isEmpty()) {
            Block.popResourceFromFace(level, pos, facing == Direction.UP ? Direction.NORTH : Direction.UP, left);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("Charge", this.charge);
        output.putInt("Range", this.range);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.charge = input.getIntOr("Charge", 0);
        this.range = input.getIntOr("Range", 8);
    }
}
