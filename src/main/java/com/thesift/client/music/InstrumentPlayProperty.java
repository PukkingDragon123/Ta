package com.thesift.client.music;

import com.mojang.serialization.MapCodec;
import com.thesift.TheSift;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.music.Instrument;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.numeric.RangeSelectItemModelProperty;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterRangeSelectItemModelPropertyEvent;
import org.jspecify.annotations.Nullable;

/**
 * INS free play: {@code thesift:instrument_play}, the item model property that animates an
 * instrument's moving parts while it is played. It returns 0 at rest, or 1 + the index of the frame
 * of its family to show (the order is tools/instrument_models.py FRAMES):
 * <ul>
 *   <li>strings and harp: the strings vibrating one way and the other (a, b), fast while they ring;</li>
 *   <li>drums: the left head dipped (l), the right head dipped (r), both springing back up (up);</li>
 *   <li>chimes: the tubes swung wide one way (a) and the other (b), then less (c, d), a slow fading pendulum;</li>
 *   <li>flutes with keys: six, four, two or no keys closed (k6..k0) for the note being fingered.</li>
 * </ul>
 */
public record InstrumentPlayProperty() implements RangeSelectItemModelProperty {
    public static final MapCodec<InstrumentPlayProperty> MAP_CODEC = MapCodec.unit(new InstrumentPlayProperty());

    public static void register(IEventBus modBus) {
        modBus.addListener(InstrumentPlayProperty::registerProperty);
    }

    private static void registerProperty(RegisterRangeSelectItemModelPropertyEvent event) {
        event.register(TheSift.id("instrument_play"), MAP_CODEC);
    }

    @Override
    public float get(ItemStack stack, @Nullable ClientLevel level, @Nullable ItemOwner owner, int seed) {
        LivingEntity entity = owner == null ? null : owner.asLivingEntity();
        if (entity == null || !entity.isUsingItem() || !(stack.getItem() instanceof SiftInstrumentItem item)) {
            return 0.0F;
        }
        return frame(item.instrument(), PlayAnim.get(entity.getId()));
    }

    /** The frame (0 at rest) for this instrument's latest notes. */
    public static float frame(Instrument ins, PlayAnim.@Nullable State s) {
        if (s == null) {
            return 0.0F;
        }
        double now = PlayAnim.now();
        switch (ins.family()) {
            case STRINGS -> {
                if (PlayAnim.energy(s, 0.55F) < 0.12F) {
                    return 0.0F;
                }
                return ((long) (now * 30.0) & 1L) == 0L ? 1.0F : 2.0F;
            }
            case DRUM -> {
                double left = now - s.leftAt;
                double right = now - s.rightAt;
                if (left < 0.07 && left <= right) {
                    return 1.0F;
                }
                if (right < 0.07) {
                    return 2.0F;
                }
                return Math.min(left, right) < 0.22 ? 3.0F : 0.0F;
            }
            case CHIMES -> {
                float v = PlayAnim.swing(s);
                if (Math.abs(v) < 0.08F) {
                    return 0.0F;
                }
                if (Math.abs(v) > 0.45F) {
                    return v > 0.0F ? 1.0F : 2.0F;
                }
                return v > 0.0F ? 3.0F : 4.0F;
            }
            case FLUTE -> {
                if (PlayAnim.since(s) > 0.8F && s.sustainUntil < now) {
                    return 0.0F;
                }
                // low notes close the keys, high notes open them
                int closed = Math.round((1.0F - s.place) * 6.0F);
                return closed >= 5 ? 1.0F : closed >= 3 ? 2.0F : closed >= 1 ? 3.0F : 4.0F;
            }
            default -> {
                return 0.0F;
            }
        }
    }

    @Override
    public MapCodec<InstrumentPlayProperty> type() {
        return MAP_CODEC;
    }
}
