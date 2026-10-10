package com.thesift.client.music;

import com.mojang.blaze3d.vertex.PoseStack;
import com.thesift.item.SiftInstrumentItem;
import com.thesift.music.Instrument;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.client.IArmPoseTransformer;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import org.jspecify.annotations.Nullable;

/**
 * INS free play: how a player holds an instrument while playing it, as everyone sees it.
 *
 * <p>The playing stance is a custom arm pose (an {@code ArmPose} added through
 * META-INF/enumextensions.json) used while the instrument is raised: it is two-handed and poses
 * the free arm itself. The arm that holds the instrument takes the family's stance (the numbers come
 * from tools/instrument_models.py FAMILY, where the play models' display transforms are solved for
 * the very same arm), and the free arm reaches for the instrument: along the neck to the fret of
 * the note, across the harp's strings, to the flute's upper joint, down onto the left drum, along
 * the row of chimes - with each family's motion on every note (a strum, a pluck, a hit that dips
 * and springs back, a strike). The first-person hold is {@link FreePlayHand}.
 */
public final class InstrumentPoses {
    /** The enum extension's parameters (two-handed, poses the other arm, the transformer). */
    public static final class EnumParams {
        public static final EnumProxy<HumanoidModel.ArmPose> STANCE = new EnumProxy<>(HumanoidModel.ArmPose.class, true, true,
                (IArmPoseTransformer) InstrumentPoses::pose);

        private EnumParams() {
        }
    }

    private InstrumentPoses() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(InstrumentPoses::registerExtensions);
    }

    private static void registerExtensions(RegisterClientExtensionsEvent event) {
        List<Item> items = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            if (item instanceof SiftInstrumentItem) {
                items.add(item);
            }
        }
        event.registerItem(new IClientItemExtensions() {
            @Override
            public HumanoidModel.@Nullable ArmPose getArmPose(LivingEntity entity, InteractionHand hand, ItemStack stack) {
                if (!stack.isEmpty() && entity.isUsingItem() && entity.getUsedItemHand() == hand && entity.getUseItemRemainingTicks() > 0) {
                    return EnumParams.STANCE.getValue();
                }
                return null;
            }

            @Override
            public boolean applyForgeHandTransform(PoseStack poseStack, PlayerRenderState playerRenderState, HumanoidArm arm, ItemStack itemInHand,
                    float partialTick, float equipProcess, float swingProcess) {
                return FreePlayHand.transform(poseStack, arm, itemInHand, partialTick, equipProcess);
            }
        }, items.toArray(new Item[0]));
    }

    // ------------------------------------------------------------------ the stances (model space: pixels, y down, z back)

    /** The holding arm's angles in each stance (x, y) - tools/instrument_models.py arm_angles(). */
    private static float[] holdAngles(Instrument ins) {
        if (ins == Instrument.HARP) {
            return new float[]{-0.9633F, -0.3617F};
        }
        return switch (ins.family()) {
            case STRINGS -> new float[]{-0.5852F, -0.6435F};
            case FLUTE -> new float[]{-2.0474F, -0.0449F};
            case DRUM -> new float[]{-0.7643F, -0.3753F};
            case CHIMES -> new float[]{-2.0083F, 0.1340F};
        };
    }

    /** Where the free hand reaches, low notes then high (two points, model space) - tools/instrument_models.py grips. */
    private static float[] grips(Instrument ins) {
        return switch (ins) {
            case GUITAR, WEAVER_GUITAR -> new float[]{6.91F, 3.82F, -11.02F, 4.14F, 5.66F, -9.43F};
            case HARP -> new float[]{1.65F, 5.28F, -9.94F, -2.13F, 4.98F, -9.02F};
            case FLUTE, SERBIM_FLUTE, PRISM_FLUTE -> new float[]{-3.14F, -3.26F, -6.68F, -1.9F, -2.79F, -6.02F};
            case DRUM -> new float[]{1.71F, 8.77F, -8.22F, 1.71F, 8.77F, -8.22F};
            case THUNDER_DRUMS -> new float[]{1.19F, 8.77F, -11.93F, 1.64F, 8.47F, -8.15F};
            case PRISM_DRUM -> new float[]{1.71F, 8.69F, -8.2F, 1.71F, 8.69F, -8.2F};
            case WIND_CHIMES -> new float[]{-10.51F, 1.49F, -6.36F, -3.95F, 0.09F, -10.13F};
            case GLASS_BELLS -> new float[]{-10.93F, 1.42F, -6.12F, -3.53F, 0.02F, -10.37F};
            case PRISM_CHIMES -> new float[]{-9.27F, 1.91F, -5.62F, -5.36F, 0.86F, -10.61F};
            default -> new float[]{4.0F, 6.0F, -8.0F, 2.0F, 6.0F, -8.0F};
        };
    }

    /** The stance transformer: called for the arm that holds the instrument; it poses both arms. */
    private static void pose(HumanoidModel<?> model, HumanoidRenderState state, HumanoidArm arm) {
        ItemStack stack = arm == HumanoidArm.RIGHT ? state.rightHandItemStack : state.leftHandItemStack;
        if (!(stack.getItem() instanceof SiftInstrumentItem item)) {
            return;
        }
        Instrument ins = item.instrument();
        PlayAnim.State s = state instanceof AvatarRenderState avatar ? PlayAnim.get(avatar.id) : null;
        boolean right = arm == HumanoidArm.RIGHT;
        ModelPart hold = right ? model.rightArm : model.leftArm;
        ModelPart free = right ? model.leftArm : model.rightArm;
        float m = right ? 1.0F : -1.0F;
        float[] a = holdAngles(ins);
        float[] g = grips(ins);
        float place = s == null ? 0.5F : s.place;
        float tx = Mth.lerp(place, g[0], g[3]);
        float ty = Mth.lerp(place, g[1], g[4]);
        float tz = Mth.lerp(place, g[2], g[5]);
        float hx = a[0];
        float pitch = 0.0F;
        float time = (float) (PlayAnim.now() % 1000.0);
        if (ins == Instrument.HARP) {
            // a pluck: the free hand draws back off the string
            float h = s == null ? 0.0F : PlayAnim.hit(s.noteAt);
            tz += 0.9F * h;
        } else {
            switch (ins.family()) {
                case STRINGS -> {
                    // a strum: the holding hand sweeps down across the strings and back; the fretting hand presses
                    float h = s == null ? 0.0F : PlayAnim.hit(s.noteAt);
                    hx += 0.11F * h;
                    ty += 0.35F * h;
                }
                case FLUTE -> {
                    // the flute stays at the lips: both arms follow where the head looks
                    pitch = Mth.clamp(state.xRot * Mth.DEG_TO_RAD, -0.8F, 0.8F);
                    hx += pitch + 0.015F * Mth.sin(time * 2.2F);
                }
                case DRUM -> {
                    // the holding hand plays the low drum (its dip moves the drums a little, like a strap);
                    // the free hand lifts and comes down on the high drum
                    float hr = s == null ? 0.0F : PlayAnim.hit(s.rightAt);
                    float hl = s == null ? 0.0F : PlayAnim.hit(s.leftAt);
                    hx += 0.14F * hr;
                    ty -= 2.2F * (1.0F - Math.min(1.0F, hl));
                    tz += 0.6F * (1.0F - Math.min(1.0F, hl));
                }
                case CHIMES -> {
                    // the chimes are held still; the free hand strikes along the row, the struck tube first
                    hx += 0.025F * Mth.sin(time * 1.3F);
                    float h = s == null ? 0.0F : PlayAnim.hit(s.noteAt);
                    float pad = s == null || s.instrument == null ? 0.5F
                            : s.instrument.padIndex(s.pitch) / (float) Math.max(1, s.instrument.layoutSize() - 1);
                    tx = Mth.lerp(pad, g[0], g[3]);
                    ty = Mth.lerp(pad, g[1], g[4]);
                    tz = Mth.lerp(pad, g[2], g[5]) - 1.4F * h + 0.8F;
                }
            }
        }
        hold.xRot = hx;
        hold.yRot = a[1] * m;
        hold.zRot = 0.0F;
        reach(free, tx * m, ty, tz);
        free.xRot += pitch;
        if (ins.family() == Instrument.Family.FLUTE && s != null) {
            // fingers on the keys
            free.zRot += 0.05F * PlayAnim.energy(s, 0.4F) * Mth.sin(time * 18.0F) * m;
        }
    }

    /** Points an arm from its shoulder at a model-space point (instrument_models.ik). */
    static void reach(ModelPart arm, float tx, float ty, float tz) {
        float dx = tx - arm.x;
        float dy = ty - arm.y;
        float dz = tz - arm.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1.0E-4F) {
            return;
        }
        dx /= len;
        dy /= len;
        dz /= len;
        float a = -(float) Math.acos(Mth.clamp(dy, -1.0F, 1.0F));
        float sa = Mth.sin(a);
        arm.xRot = a;
        arm.yRot = Math.abs(sa) > 1.0E-4F ? (float) Math.atan2(dx / sa, dz / sa) : 0.0F;
        arm.zRot = 0.0F;
    }
}
