package com.thesift.client.model;

import com.thesift.TheSift;
import net.minecraft.client.model.geom.ModelLayerLocation;

public final class ModModelLayers {
    public static final ModelLayerLocation BULB = layer("bulb");
    public static final ModelLayerLocation HARMONER = layer("harmoner");
    public static final ModelLayerLocation DICTATOR = layer("dictator");
    public static final ModelLayerLocation THUMPER = layer("thumper");
    public static final ModelLayerLocation WHISTLER = layer("whistler");
    public static final ModelLayerLocation STRUMMER = layer("strummer");
    public static final ModelLayerLocation THUMPLING = layer("thumpling");
    public static final ModelLayerLocation WHISTLING = layer("whistling");
    public static final ModelLayerLocation STRUMLING = layer("strumling");
    public static final ModelLayerLocation SCULK_PARASITE = layer("sculk_parasite");
    public static final ModelLayerLocation CONDUCTOR_MASK = layer("conductor_mask");
    public static final ModelLayerLocation SLUMBLER = layer("slumbler");
    public static final ModelLayerLocation SIFTER = layer("sifter");
    public static final ModelLayerLocation ENCHOER = layer("enchoer");
    public static final ModelLayerLocation RIVETER = layer("riveter");
    // ---- the wild creatures
    public static final ModelLayerLocation STOMPER = layer("stomper");
    public static final ModelLayerLocation FANFARE_EEL = layer("fanfare_eel");
    public static final ModelLayerLocation KAZOO_FISH = layer("kazoo_fish");
    public static final ModelLayerLocation TUBAFISH = layer("tubafish");
    public static final ModelLayerLocation SKY_WHALE = layer("sky_whale");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(TheSift.id(name), "main");
    }

    private ModModelLayers() {
    }
}
