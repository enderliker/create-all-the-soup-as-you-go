package com.ender.allthesoupasyougo.registry;

import com.ender.allthesoupasyougo.AllTheSoupAsYouGoMod;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class ModItems {
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(AllTheSoupAsYouGoMod.MODID);
    private static final Map<Soup, DeferredItem<BucketItem>> BUCKETS = new EnumMap<>(Soup.class);

    static {
        for (Soup soup : Soup.values()) {
            BUCKETS.put(soup, ITEMS.register(soup.id() + "_bucket", () -> new BucketItem(
                    ModFluids.source(soup).get(), new Item.Properties().stacksTo(1).craftRemainder(Items.BUCKET)
            )));
        }
    }

    public static DeferredItem<BucketItem> get(Soup soup) { return BUCKETS.get(soup); }
    public static void register(IEventBus modBus) { ITEMS.register(modBus); }
}
