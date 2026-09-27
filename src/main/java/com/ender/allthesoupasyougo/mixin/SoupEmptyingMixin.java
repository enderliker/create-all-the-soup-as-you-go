package com.ender.allthesoupasyougo.mixin;

import com.ender.allthesoupasyougo.registry.ModFluids;
import com.ender.allthesoupasyougo.registry.Soup;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.createmod.catnip.data.Pair;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GenericItemEmptying.class, remap = false)
abstract class SoupEmptyingMixin {
    @Inject(method = "emptyItem", at = @At("HEAD"), cancellable = true)
    private static void preserveEffects(Level level, ItemStack stack, boolean simulate,
            CallbackInfoReturnable<Pair<FluidStack, ItemStack>> cir) {
        if (!stack.is(Items.SUSPICIOUS_STEW)) return;
        FluidStack fluid = new FluidStack(ModFluids.source(Soup.SUSPICIOUS_STEW).get(), 250);
        var effects = stack.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (effects != null) fluid.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
        if (!simulate) stack.shrink(1);
        cir.setReturnValue(Pair.of(fluid, new ItemStack(Items.BOWL)));
    }
}
