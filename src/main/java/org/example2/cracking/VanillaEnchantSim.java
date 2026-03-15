package org.example2.cracking;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public final class VanillaEnchantSim {
    private VanillaEnchantSim() {}

    private static Stream<Holder<Enchantment>> enchantmentStream(Registry<Enchantment> reg) {
        return reg.holders().map(h -> (Holder<Enchantment>) h);
    }

    public static List<EnchantmentInstance> getEnchantmentList(
            Registry<Enchantment> reg,
            int xpSeed,
            ItemStack stack,
            int slot,
            int cost,
            int bookshelfPower
    ) {
        // bookshelfPower は今の実装では未使用。
        // すでに caller 側で cost が確定しているので、ここでは cost をそのまま使う。
        if (cost <= 0 || stack.isEmpty()) {
            return Collections.emptyList();
        }

        RandomSource rand = RandomSource.create();
        rand.setSeed(xpSeed + slot);

        return EnchantmentHelper.selectEnchantment(
                rand,
                stack.copy(),
                cost,
                enchantmentStream(reg)
        );
    }

    public static EnchantmentInstance pickDisplayedClue(
            Registry<Enchantment> reg,
            int xpSeed,
            ItemStack stack,
            int slot,
            int cost
    ) {
        if (cost <= 0 || stack.isEmpty()) {
            return null;
        }

        RandomSource rand = RandomSource.create();
        rand.setSeed(xpSeed + slot);

        List<EnchantmentInstance> list = EnchantmentHelper.selectEnchantment(
                rand,
                stack.copy(),
                cost,
                enchantmentStream(reg)
        );

        if (list.isEmpty()) {
            return null;
        }

        return list.get(rand.nextInt(list.size()));
    }
}