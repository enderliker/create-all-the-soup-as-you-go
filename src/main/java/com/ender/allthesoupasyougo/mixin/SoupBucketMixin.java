package com.ender.allthesoupasyougo.mixin;

import com.ender.allthesoupasyougo.registry.ModItems;
import com.ender.allthesoupasyougo.registry.Soup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.wrappers.FluidBucketWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FluidBucketWrapper.class, remap = false)
abstract class SoupBucketMixin {
    @Shadow protected ItemStack container;

    @Inject(method = "getFluid", at = @At("RETURN"))
    private void preserveBucketEffects(CallbackInfoReturnable<FluidStack> cir) {
        if (!container.is(ModItems.get(Soup.SUSPICIOUS_STEW).get())) return;
        var effects = container.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (effects != null) cir.getReturnValue().set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
    }
}
