package com.thesift.client;

import com.thesift.TheSift;
import com.thesift.client.model.MansionIllagerModel;
import com.thesift.client.renderer.state.MansionIllagerRenderState;
import com.thesift.entity.mansion.Bard;
import com.thesift.entity.mansion.Hornblower;
import com.thesift.registry.ModMansion;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.IllagerRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * MANSION, client side: the Hornblower and the Bard are drawn with the vanilla illager model (baked from the vanilla
 * pillager layer, see {@link MansionIllagerModel}) and the vanilla pillager's texture re-dressed (tools/mansion.py), with
 * their horn or guitar in hand like any illager's weapon.
 */
public final class MansionClient {
    private MansionClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(MansionClient::registerRenderers);
    }

    private static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModMansion.HORNBLOWER.get(), ctx -> new Renderer<>(ctx, TheSift.id("textures/entity/mansion/hornblower.png")));
        event.registerEntityRenderer(ModMansion.BARD.get(), ctx -> new Renderer<>(ctx, TheSift.id("textures/entity/mansion/bard.png")));
    }

    /** The vanilla pillager's renderer with its own texture and the instruments' poses. */
    public static final class Renderer<T extends AbstractIllager> extends IllagerRenderer<T, MansionIllagerRenderState> {
        private final Identifier texture;

        public Renderer(EntityRendererProvider.Context context, Identifier texture) {
            super(context, new MansionIllagerModel(context.bakeLayer(ModelLayers.PILLAGER)), 0.5F);
            this.texture = texture;
            this.addLayer(new ItemInHandLayer<>(this));
        }

        @Override
        public Identifier getTextureLocation(MansionIllagerRenderState state) {
            return this.texture;
        }

        @Override
        public MansionIllagerRenderState createRenderState() {
            return new MansionIllagerRenderState();
        }

        @Override
        public void extractRenderState(T entity, MansionIllagerRenderState state, float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.playing = entity.isUsingItem();
            state.bard = entity instanceof Bard;
            int at = entity instanceof Hornblower h ? h.blastTick : entity instanceof Bard b ? b.healTick : -1000;
            state.sinceFlourish = entity.tickCount + partialTicks - at;
        }
    }
}
