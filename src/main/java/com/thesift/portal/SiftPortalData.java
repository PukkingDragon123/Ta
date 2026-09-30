package com.thesift.portal;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thesift.TheSift;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jspecify.annotations.Nullable;

/** Remembers which portal leads to which, across dimensions. Stored with the overworld. */
public class SiftPortalData extends SavedData {
    public record Link(GlobalPos a, GlobalPos b) {
        public static final Codec<Link> CODEC = RecordCodecBuilder.create(i -> i.group(
                GlobalPos.CODEC.fieldOf("a").forGetter(Link::a),
                GlobalPos.CODEC.fieldOf("b").forGetter(Link::b)).apply(i, Link::new));
    }

    public static final Codec<SiftPortalData> CODEC = Link.CODEC.listOf().xmap(SiftPortalData::new, d -> d.links);
    public static final SavedDataType<SiftPortalData> TYPE = new SavedDataType<>(TheSift.id("portal_links"), SiftPortalData::new, CODEC);

    private final List<Link> links;

    public SiftPortalData() {
        this(new ArrayList<>());
    }

    private SiftPortalData(List<Link> links) {
        this.links = new ArrayList<>(links);
    }

    public static SiftPortalData get(MinecraftServer server) {
        return server.getDataStorage().computeIfAbsent(TYPE);
    }

    public @Nullable GlobalPos linked(GlobalPos from) {
        for (Link l : this.links) {
            if (l.a().equals(from)) return l.b();
            if (l.b().equals(from)) return l.a();
        }
        return null;
    }

    public void link(GlobalPos a, GlobalPos b) {
        this.links.removeIf(l -> l.a().equals(a) || l.b().equals(a) || l.a().equals(b) || l.b().equals(b));
        this.links.add(new Link(a, b));
        this.setDirty();
    }
}
