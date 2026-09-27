package com.ender.allthesoupasyougo.registry;

import com.ender.allthesoupasyougo.AllTheSoupAsYouGoMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModFluids {
    private static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(BuiltInRegistries.FLUID, AllTheSoupAsYouGoMod.MODID);
    private static final Map<Soup, DeferredHolder<Fluid, FlowingFluid>> SOURCES = new EnumMap<>(Soup.class);
    private static final Map<Soup, DeferredHolder<Fluid, FlowingFluid>> FLOWING = new EnumMap<>(Soup.class);

    static {
        for (Soup soup : Soup.values()) {
            SOURCES.put(soup, FLUIDS.register(soup.id(), () -> new BaseFlowingFluid.Source(properties(soup))));
            FLOWING.put(soup, FLUIDS.register(soup.id() + "_flowing", () -> new BaseFlowingFluid.Flowing(properties(soup))));
        }
    }

    private static BaseFlowingFluid.Properties properties(Soup soup) {
        return new BaseFlowingFluid.Properties(ModFluidTypes.get(soup), SOURCES.get(soup), FLOWING.get(soup))
                .block(ModBlocks.get(soup)).bucket(ModItems.get(soup))
                .slopeFindDistance(2).levelDecreasePerBlock(2);
    }

    public static DeferredHolder<Fluid, FlowingFluid> source(Soup soup) { return SOURCES.get(soup); }
    public static void register(IEventBus modBus) { FLUIDS.register(modBus); }
}
