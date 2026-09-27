package com.ender.allthesoupasyougo.mixin;

import com.ender.allthesoupasyougo.registry.ModFluids;
import com.ender.allthesoupasyougo.registry.Soup;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = FillingBySpout.class, remap = false)
abstract class SoupFillingMixin {
    @Inject(method = "fillItem", at = @At("HEAD"), cancellable = true)
    private static void restoreEffects(Level level, int amount, ItemStack stack, FluidStack fluid,
            CallbackInfoReturnable<ItemStack> cir) {
        if (!stack.is(Items.BOWL) || !fluid.is(ModFluids.source(Soup.SUSPICIOUS_STEW).get())) return;
        if (amount != 250 || fluid.getAmount() < 250) {
            cir.setReturnValue(ItemStack.EMPTY);
            return;
        }
        ItemStack result = new ItemStack(Items.SUSPICIOUS_STEW);
        var effects = fluid.get(DataComponents.SUSPICIOUS_STEW_EFFECTS);
        if (effects != null) result.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
        fluid.shrink(250);
        stack.shrink(1);
        cir.setReturnValue(result);
    }
}
