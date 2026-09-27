package com.ender.allthesoupasyougo;

import com.ender.allthesoupasyougo.registry.ModFluidTypes;
import com.ender.allthesoupasyougo.registry.Soup;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

@Mod(value = AllTheSoupAsYouGoMod.MODID, dist = Dist.CLIENT)
public class AllTheSoupAsYouGoClient {
    public AllTheSoupAsYouGoClient(IEventBus modBus) {
        modBus.addListener(this::registerClientExtensions);
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        for (Soup soup : Soup.values()) {
            event.registerFluidType(new IClientFluidTypeExtensions() {
                private final ResourceLocation still = ResourceLocation.fromNamespaceAndPath(
                        AllTheSoupAsYouGoMod.MODID, "block/" + soup.id() + "_still");
                private final ResourceLocation flowing = ResourceLocation.fromNamespaceAndPath(
                        AllTheSoupAsYouGoMod.MODID, "block/" + soup.id() + "_flow");

                @Override
                public ResourceLocation getStillTexture() { return still; }

                @Override
                public ResourceLocation getFlowingTexture() { return flowing; }

                @Override
                public int getTintColor() { return 0xFFFFFFFF; }
            }, ModFluidTypes.get(soup).get());
        }
    }
}
