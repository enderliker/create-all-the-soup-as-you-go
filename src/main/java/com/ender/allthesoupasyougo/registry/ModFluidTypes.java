package com.ender.allthesoupasyougo.registry;

import com.ender.allthesoupasyougo.AllTheSoupAsYouGoMod;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.EnumMap;
import java.util.Map;

public final class ModFluidTypes {
    private static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, AllTheSoupAsYouGoMod.MODID);
    private static final Map<Soup, DeferredHolder<FluidType, FluidType>> TYPES = new EnumMap<>(Soup.class);

    static {
        for (Soup soup : Soup.values()) {
            TYPES.put(soup, FLUID_TYPES.register(soup.id(), () -> new FluidType(
                    FluidType.Properties.create()
                            .density(1100).viscosity(1200).temperature(300)
                            .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                            .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                            .canSwim(false).canDrown(false).canConvertToSource(false)
            ) {
                @Override
                public ItemStack getBucket(FluidStack stack) {
                    ItemStack bucket = super.getBucket(stack);
                    var effects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
                    if (soup == Soup.SUSPICIOUS_STEW && effects != null)
                        bucket.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
                    return bucket;
                }
            }));
        }
    }

    public static DeferredHolder<FluidType, FluidType> get(Soup soup) { return TYPES.get(soup); }
    public static void register(IEventBus modBus) { FLUID_TYPES.register(modBus); }
}
