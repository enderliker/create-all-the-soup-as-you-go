package com.ender.allthesoupasyougo.registry;

import com.ender.allthesoupasyougo.AllTheSoupAsYouGoMod;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModBlocks {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(AllTheSoupAsYouGoMod.MODID);
    private static final Map<Soup, DeferredBlock<LiquidBlock>> FLUID_BLOCKS = new EnumMap<>(Soup.class);

    static {
        for (Soup soup : Soup.values()) {
            FLUID_BLOCKS.put(soup, BLOCKS.register(soup.id() + "_fluid", () -> new LiquidBlock(
                    ModFluids.source(soup).get(),
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BROWN)
                            .replaceable().noCollission().liquid().strength(100f).noLootTable()
            )));
        }
    }

    public static DeferredBlock<LiquidBlock> get(Soup soup) { return FLUID_BLOCKS.get(soup); }
    public static void register(IEventBus modBus) { BLOCKS.register(modBus); }
}
