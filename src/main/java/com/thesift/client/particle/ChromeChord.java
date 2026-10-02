package com.thesift.client.particle;

import com.thesift.registry.ModChrome;
import com.thesift.registry.ModFluids;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.NoRenderParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.material.FluidState;

/**
 * A note rang out near Chrome: never drawn itself, it finds the Chrome surface around the note and
 * sends a ring of colour rolling out across it, a block further each tick, every ring a step on
 * round the rainbow from the note's own colour.
 */
public class ChromeChord extends NoRenderParticle {
    private static final int RADIUS = 6;

    /** x, y (the surface), z, distance from the note. */
    private final List<double[]> surface = new ArrayList<>();
    private final float hue;

    ChromeChord(ClientLevel level, double x, double y, double z, double pitch) {
        super(level, x, y, z);
        this.hue = (float) pitch;
        this.lifetime = RADIUS * 2 + 2;
        BlockPos centre = BlockPos.containing(x, y, z);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -RADIUS; dx <= RADIUS; dx++) {
            for (int dz = -RADIUS; dz <= RADIUS; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > RADIUS + 0.5) {
                    continue;
                }
                for (int dy = 2; dy >= -3; dy--) {
                    p.set(centre.getX() + dx, centre.getY() + dy, centre.getZ() + dz);
                    FluidState fs = level.getFluidState(p);
                    if (fs.getType().isSame(ModFluids.CHROME.get())) {
                        if (!level.getFluidState(p.above()).getType().isSame(ModFluids.CHROME.get())) {
                            this.surface.add(new double[]{p.getX() + 0.5, p.getY() + fs.getHeight(level, p) + 0.02, p.getZ() + 0.5, dist});
                        }
                        break;
                    }
                }
            }
        }
    }

    @Override
    public void tick() {
        if (this.age++ >= this.lifetime || this.surface.isEmpty()) {
            this.remove();
            return;
        }
        double inner = (this.age - 1) * 0.5;
        double outer = inner + 0.5;
        RandomSource random = this.random;
        for (double[] s : this.surface) {
            if (s[3] < inner || s[3] >= outer) {
                continue;
            }
            float h = this.hue + (float) s[3] * 0.07F;
            this.level.addParticle(ModChrome.CHROME_RIPPLE.get(), s[0], s[1], s[2], h, 0.5, 0.0);
            this.level.addParticle(ModChrome.CHROME_SPARK.get(), s[0] + (random.nextDouble() - 0.5) * 0.8, s[1] + 0.05,
                    s[2] + (random.nextDouble() - 0.5) * 0.8, h + 0.05, 0.06 + random.nextDouble() * 0.06, 0.0);
        }
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        public Provider(SpriteSet sprites) {
        }

        @Override
        public Particle createParticle(SimpleParticleType options, ClientLevel level, double x, double y, double z, double xa, double ya, double za,
                RandomSource random) {
            return new ChromeChord(level, x, y, z, xa);
        }
    }
}
