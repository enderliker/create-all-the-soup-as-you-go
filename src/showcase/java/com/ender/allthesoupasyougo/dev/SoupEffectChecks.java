package com.ender.allthesoupasyougo.dev;

import com.ender.allthesoupasyougo.registry.ModFluids;
import com.ender.allthesoupasyougo.registry.Soup;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.SuspiciousStewEffects;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import java.util.List;

final class SoupEffectChecks {
    static final SuspiciousStewEffects MACHINE_EFFECTS = new SuspiciousStewEffects(List.of(
            new SuspiciousStewEffects.Entry(MobEffects.NIGHT_VISION, 180),
            new SuspiciousStewEffects.Entry(MobEffects.REGENERATION, 160)));

    static void run(Level level) {
        var variants = List.of(MobEffects.NIGHT_VISION, MobEffects.JUMP, MobEffects.WEAKNESS,
                MobEffects.BLINDNESS, MobEffects.POISON, MobEffects.SATURATION,
                MobEffects.FIRE_RESISTANCE, MobEffects.REGENERATION, MobEffects.WITHER);
        for (var effect : variants) {
            verifyVariant(level, new SuspiciousStewEffects(List.of(new SuspiciousStewEffects.Entry(effect, 187))));
        }
        verifyVariant(level, MACHINE_EFFECTS);
        verifyVariant(level, SuspiciousStewEffects.EMPTY);
        FluidTank tank = new FluidTank(2000);
        FluidStack first = fluid(1000, MACHINE_EFFECTS);
        tank.fill(first, FluidAction.EXECUTE);
        require(tank.fill(fluid(1000, SuspiciousStewEffects.EMPTY), FluidAction.EXECUTE) == 0,
                "Different suspicious stew effects must not mix in a tank");
    }

    private static void verifyVariant(Level level, SuspiciousStewEffects effects) {
        ItemStack soup = new ItemStack(Items.SUSPICIOUS_STEW);
        soup.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
        var preview = GenericItemEmptying.emptyItem(level, soup, true);
        require(soup.getCount() == 1 && preview.getFirst().getAmount() == 250, "Drain simulation mutated input");
        require(effects.equals(preview.getFirst().get(DataComponents.SUSPICIOUS_STEW_EFFECTS)), "Drain preview lost effects");
        var drained = GenericItemEmptying.emptyItem(level, soup, false);
        require(soup.isEmpty() && drained.getSecond().is(Items.BOWL), "Drain did not consume exactly one stew");
        FluidStack fluid = drained.getFirst();
        require(effects.equals(fluid.get(DataComponents.SUSPICIOUS_STEW_EFFECTS)), "Drain lost effects");
        fluid.setAmount(1000);
        ItemStack buckets = new ItemStack(Items.BUCKET);
        int bucketAmount = FillingBySpout.getRequiredAmountForItem(level, buckets, fluid);
        require(bucketAmount == 1000, "Spout bucket amount is not 1000 mB");
        ItemStack bucket = FillingBySpout.fillItem(level, bucketAmount, buckets, fluid);
        require(buckets.isEmpty() && fluid.isEmpty(), "Spout bucket fill consumption mismatch");
        require(effects.equals(bucket.get(DataComponents.SUSPICIOUS_STEW_EFFECTS)), "Bucket fill lost effects");
        var cap = bucket.getCapability(Capabilities.FluidHandler.ITEM);
        require(cap != null && effects.equals(cap.getFluidInTank(0).get(DataComponents.SUSPICIOUS_STEW_EFFECTS)),
                "Bucket capability lost effects");
        var bucketDrained = GenericItemEmptying.emptyItem(level, bucket, false);
        require(bucketDrained.getSecond().is(Items.BUCKET), "Draining soup bucket did not return bucket");
        fluid = bucketDrained.getFirst();
        for (int i = 0; i < 4; i++) {
            ItemStack bowl = new ItemStack(Items.BOWL);
            require(FillingBySpout.getRequiredAmountForItem(level, bowl, fluid) == 250, "Wrong bowl amount");
            ItemStack refilled = FillingBySpout.fillItem(level, 250, bowl, fluid);
            require(bowl.isEmpty() && refilled.is(Items.SUSPICIOUS_STEW), "Bowl refill failed");
            require(effects.equals(refilled.get(DataComponents.SUSPICIOUS_STEW_EFFECTS)), "Refill lost effects");
        }
        require(fluid.isEmpty(), "Four bowls must consume one bucket");
    }

    static FluidStack fluid(int amount, SuspiciousStewEffects effects) {
        FluidStack fluid = new FluidStack(ModFluids.source(Soup.SUSPICIOUS_STEW).get(), amount);
        fluid.set(DataComponents.SUSPICIOUS_STEW_EFFECTS, effects);
        return fluid;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
