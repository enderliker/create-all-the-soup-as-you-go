package com.ender.allthesoupasyougo.registry;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public enum Soup {
    MUSHROOM_STEW("mushroom_stew", false),
    BEETROOT_SOUP("beetroot_soup", false),
    RABBIT_STEW("rabbit_stew", false),
    SUSPICIOUS_STEW("suspicious_stew", false),
    BAKED_COD_STEW("baked_cod_stew", true),
    BEEF_STEW("beef_stew", true),
    CHICKEN_SOUP("chicken_soup", true),
    FISH_STEW("fish_stew", true),
    NOODLE_SOUP("noodle_soup", true),
    PUMPKIN_SOUP("pumpkin_soup", true),
    VEGETABLE_SOUP("vegetable_soup", true),
    BONE_BROTH("bone_broth", true),
    ONION_SOUP("onion_soup", true);

    private final String id;
    private final boolean farmersDelight;

    Soup(String id, boolean farmersDelight) {
        this.id = id;
        this.farmersDelight = farmersDelight;
    }

    public String id() { return id; }
    public boolean farmersDelight() { return farmersDelight; }
    public boolean available() {
        return BuiltInRegistries.ITEM.containsKey(ResourceLocation.fromNamespaceAndPath(
                farmersDelight ? "farmersdelight" : "minecraft", id));
    }
}
