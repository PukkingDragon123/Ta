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
    public static final ModelLayerLocation CONDUCTOR_MASK = layer("conductor_mask");
    public static final ModelLayerLocation SLUMBLER = layer("slumbler");
    public static final ModelLayerLocation SIFTER = layer("sifter");
    public static final ModelLayerLocation ENCHOER = layer("enchoer");
    public static final ModelLayerLocation RIVETER = layer("riveter");

    private static ModelLayerLocation layer(String name) {
        return new ModelLayerLocation(TheSift.id(name), "main");
    }

    private ModModelLayers() {
    }
}
