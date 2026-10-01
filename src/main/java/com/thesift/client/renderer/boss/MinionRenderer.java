package com.thesift.client.renderer.boss;

import com.thesift.TheSift;
import com.thesift.client.renderer.state.MinionRenderState;
import com.thesift.entity.boss.OrchestraMinion;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/** Renders an orchestra member; its sculk glows brighter while it winds up an attack. */
public class MinionRenderer<T extends OrchestraMinion, M extends EntityModel<MinionRenderState>> extends MobRenderer<T, MinionRenderState, M> {
    private final Identifier texture;

    public MinionRenderer(EntityRendererProvider.Context context, M model, String name, float shadow) {
        super(context, model, shadow);
        this.texture = TheSift.id("textures/entity/" + name + "/" + name + ".png");
        Identifier glow = TheSift.id("textures/entity/" + name + "/" + name + "_glow.png");
        this.addLayer(new LivingEntityEmissiveLayer<>(this, s -> glow,
                (s, age) -> s.windingUp ? 1.0F : 0.45F + 0.2F * Mth.sin(age * 0.1F), this.model, RenderTypes::entityTranslucentEmissive, false));
    }

    @Override
    public Identifier getTextureLocation(MinionRenderState state) {
        return this.texture;
    }

    @Override
    public MinionRenderState createRenderState() {
        return new MinionRenderState();
    }

    @Override
    public void extractRenderState(T entity, MinionRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.windingUp = entity.isWindingUp();
        state.attack.copyFrom(entity.attackAnimation);
    }
}
