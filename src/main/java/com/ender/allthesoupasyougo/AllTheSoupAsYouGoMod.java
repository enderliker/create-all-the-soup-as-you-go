package com.ender.allthesoupasyougo;

import com.ender.allthesoupasyougo.registry.ModBlocks;
import com.ender.allthesoupasyougo.registry.ModFluidTypes;
import com.ender.allthesoupasyougo.registry.ModFluids;
import com.ender.allthesoupasyougo.registry.ModItems;
import com.ender.allthesoupasyougo.registry.Soup;
import net.minecraft.world.item.CreativeModeTabs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

@Mod(AllTheSoupAsYouGoMod.MODID)
public class AllTheSoupAsYouGoMod {

    public static final String MODID = "allthesoupasyougo";

    public AllTheSoupAsYouGoMod(IEventBus modBus) {
        ModFluidTypes.register(modBus);
        ModFluids.register(modBus);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        modBus.addListener(this::addCreativeItems);
    }

    private void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() != CreativeModeTabs.TOOLS_AND_UTILITIES) return;
        for (Soup soup : Soup.values()) {
            if (soup.available()) {
                event.accept(ModItems.get(soup));
            }
        }
    }
}
