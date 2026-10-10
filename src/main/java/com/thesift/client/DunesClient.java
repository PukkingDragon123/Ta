package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.ModelGeometry;
import com.thesift.client.renderer.GrubRenderer;
import com.thesift.client.renderer.JaberoraRenderer;
import com.thesift.client.renderer.KerkorerRenderer;
import com.thesift.client.renderer.ReservoirRenderer;
import com.thesift.registry.ModDunes;
import com.thesift.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jspecify.annotations.Nullable;

/**
 * P4-DESERT, client side: the Rocky Dunes creatures' models and renderers, and what Deafened does to your ears.
 *
 * <p>Deafened: for the first two seconds (and the whole time at amplifier 1+) the world is silent but for a high
 * ringing; after that it is muffled - every sound comes through faint and low, as if through cotton. Creatures are
 * not heard at all (no growls, no hisses, no footsteps, no subtitles for them), and no music plays: records,
 * the soundtrack, every looping or moving sound fall silent. The ringing comes back every few seconds.</p>
 */
public final class DunesClient {
    public static final ModelLayerLocation KERKORER = new ModelLayerLocation(TheSift.id("kerkorer"), "main");
    public static final ModelLayerLocation JABERORA = new ModelLayerLocation(TheSift.id("jaberora"), "main");
    public static final ModelLayerLocation RESERVOIR = new ModelLayerLocation(TheSift.id("reservoir"), "main");
    public static final ModelLayerLocation GRUB = new ModelLayerLocation(TheSift.id("grub"), "main");
    private static final RandomSource RANDOM = RandomSource.create();
    private static int deafTicks;
    private static @Nullable SoundInstance ringing;

    private DunesClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(DunesClient::registerLayers);
        modBus.addListener(DunesClient::registerRenderers);
        NeoForge.EVENT_BUS.addListener(DunesClient::onPlaySound);
        NeoForge.EVENT_BUS.addListener(DunesClient::onClientTick);
    }

    private static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(KERKORER, ModelGeometry::kerkorer);
        event.registerLayerDefinition(JABERORA, ModelGeometry::jaberora);
        event.registerLayerDefinition(RESERVOIR, ModelGeometry::reservoir);
        event.registerLayerDefinition(GRUB, ModelGeometry::grub);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModDunes.KERKORER.get(), KerkorerRenderer::new);
        event.registerEntityRenderer(ModDunes.JABERORA.get(), JaberoraRenderer::new);
        event.registerEntityRenderer(ModDunes.RESERVOIR.get(), ctx -> new ReservoirRenderer(ctx, false));
        event.registerEntityRenderer(ModDunes.MONARCH_RESERVOIR.get(), ctx -> new ReservoirRenderer(ctx, true));
        event.registerEntityRenderer(ModDunes.GRUB.get(), GrubRenderer::new);
    }

    // ------------------------------------------------------------------ Deafened

    private static @Nullable MobEffectInstance deafness() {
        LocalPlayer p = Minecraft.getInstance().player;
        return p == null ? null : p.getEffect(ModEffects.DEAFENED);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        LocalPlayer p = Minecraft.getInstance().player;
        MobEffectInstance deaf = deafness();
        if (p == null || deaf == null) {
            deafTicks = 0;
            return;
        }
        // the ringing: loud as it lands, then fainter every few seconds
        if (deafTicks == 0 || deafTicks % 80 == 0) {
            float volume = deafTicks == 0 ? 0.6F : 0.22F;
            SoundInstance ring = new SimpleSoundInstance(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.MASTER, volume, 2.0F, RANDOM,
                    p.getX(), p.getEyeY(), p.getZ());
            ringing = ring;
            Minecraft.getInstance().getSoundManager().play(ring);
        }
        deafTicks++;
    }

    /** Silences (or muffles) every sound the deafened local player would hear. */
    private static void onPlaySound(PlaySoundEvent event) {
        SoundInstance s = event.getSound();
        if (s == null || s == ringing) {
            return;
        }
        MobEffectInstance deaf = deafness();
        if (deaf == null) {
            return;
        }
        SoundSource src = s.getSource();
        String category = src.getName();
        if (src == SoundSource.MASTER || "ui".equals(category)) {
            return; // menus and buttons still click
        }
        boolean silent = deafTicks < 40 || deaf.getAmplifier() > 0;
        if (silent || src == SoundSource.MUSIC || src == SoundSource.RECORDS || src == SoundSource.HOSTILE || src == SoundSource.NEUTRAL
                || s.isLooping() || s.isRelative() || s instanceof TickableSoundInstance) {
            event.setSound(null);
            return;
        }
        // muffled: a faint, low copy of it from the same place
        event.setSound(new SimpleSoundInstance(SoundEvent.createVariableRangeEvent(s.getIdentifier()), src, 0.15F, 0.55F, RANDOM,
                s.getX(), s.getY(), s.getZ()));
    }
}
