package com.thesift.client.sky;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.commands.RenderPass;
import com.mojang.renderpearl.api.pipeline.PrimitiveTopology;
import com.thesift.TheSift;
import java.util.Optional;
import java.util.OptionalDouble;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraft.client.renderer.state.level.SkyRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.CustomSkyboxRenderer;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;

/**
 * The Sift's sky: a seamless cube map (baked by tools/gen_textures.py) holding a clean cyan
 * gradient, turquoise overhead and pale mint at the horizon. Faces are laid out 3x2 in one texture;
 * each face maps (s, t) in [-1, 1] to a direction exactly like the generator does, so the edges
 * line up. Rainbows, aurora ribbons, colour clouds and shooting stars are drawn over it by
 * {@link SiftSkyFx} in the same pass.
 */
public class SiftSkyRenderer implements CustomSkyboxRenderer {
    private static final Identifier TEXTURE = TheSift.id("textures/environment/nebula.png");
    private static final int FACES = 6;
    private GpuBuffer buffer;

    private static Vector3f dir(int face, float s, float t) {
        return switch (face) {
            case 0 -> new Vector3f(1, -t, -s);
            case 1 -> new Vector3f(-1, -t, s);
            case 2 -> new Vector3f(s, 1, t);
            case 3 -> new Vector3f(s, -1, -t);
            case 4 -> new Vector3f(s, -t, 1);
            default -> new Vector3f(-s, -t, -1);
        };
    }

    private GpuBuffer build() {
        // two windings per face so culling can never hide the inside of the box
        try (ByteBufferBuilder bytes = ByteBufferBuilder.exactlySized(FACES * 8 * DefaultVertexFormat.POSITION_TEX_COLOR.getVertexSize())) {
            BufferBuilder b = new BufferBuilder(bytes, PrimitiveTopology.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            float[][] corners = {{-1, -1}, {-1, 1}, {1, 1}, {1, -1}};
            for (int face = 0; face < FACES; face++) {
                float u0 = (face % 3) / 3.0F;
                float v0 = (face / 3) / 2.0F;
                for (int winding = 0; winding < 2; winding++) {
                    for (int k = 0; k < 4; k++) {
                        float[] c = corners[winding == 0 ? k : 3 - k];
                        Vector3f d = dir(face, c[0], c[1]).mul(100.0F);
                        float u = u0 + (c[0] + 1) * 0.5F / 3.0F;
                        float v = v0 + (c[1] + 1) * 0.5F / 2.0F;
                        b.addVertex(d.x, d.y, d.z).setUv(u, v).setColor(-1);
                    }
                }
            }
            try (MeshData mesh = b.buildOrThrow()) {
                return RenderSystem.getDevice().createBuffer(() -> "Sift nebula sky", 40, mesh.vertexBuffer());
            }
        }
    }

    @Override
    public boolean renderSky(LevelRenderState levelRenderState, SkyRenderState skyRenderState, Matrix4fc modelViewMatrix, GpuBufferSlice skyFog) {
        Minecraft mc = Minecraft.getInstance();
        if (this.buffer == null) {
            this.buffer = this.build();
        }
        AbstractTexture texture = mc.getTextureManager().getTexture(TEXTURE);
        // the gradient must stay level with the horizon, so the dome never tilts or turns
        Matrix4f view = new Matrix4f(modelViewMatrix);
        RenderSystem.setShaderFog(skyFog);
        SiftSkyFx.prepare(); // B1 Portal & sky FX: rainbows, aurora, colour clouds, shooting stars
        SiftSkyFx.collect(view);
        RenderSystem.AutoStorageIndexBuffer indices = RenderSystem.getSequentialBuffer(PrimitiveTopology.QUADS);
        GpuBuffer indexBuffer = indices.getBuffer(Math.max(FACES * 2 * 6, SiftSkyFx.MAX_INDICES));
        GpuBufferSlice transform = RenderSystem.getDynamicUniforms().writeTransform(view);
        try (RenderPass pass = RenderSystem.getDevice().createCommandEncoder().createRenderPass(() -> "Sift sky",
                mc.gameRenderer.mainRenderTarget().getColorTextureView(), Optional.empty(), mc.gameRenderer.mainRenderTarget().getDepthTextureView(), OptionalDouble.empty())) {
            pass.setPipeline(RenderSystem.getCompiledPipeline(RenderPipelines.END_SKY));
            RenderSystem.bindDefaultUniforms(pass);
            pass.setUniform("DynamicTransforms", transform);
            pass.setUniform("Sampler0", texture.getTextureView(), texture.getSampler());
            pass.setVertexBuffer(0, this.buffer.slice());
            pass.setIndexBuffer(indexBuffer, indices.type());
            pass.drawIndexed(FACES * 2 * 6, 1, 0, 0, 0);
            SiftSkyFx.render(pass);
        }
        return true;
    }
}
