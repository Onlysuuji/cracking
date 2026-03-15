package org.example2.cracking;


import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Objects;

public final class ObservedEnchantState {
    private final ItemStack item;
    private final int maskedSeed;
    private final int[] costs;
    private final int[] clueIds;
    private final int[] clueLevels;

    public ObservedEnchantState(ItemStack item, int maskedSeed, int[] costs, int[] clueIds, int[] clueLevels) {
        this.item = item.copy();
        this.maskedSeed = maskedSeed & 0x0000fff0;
        this.costs = costs.clone();
        this.clueIds = clueIds.clone();
        this.clueLevels = clueLevels.clone();
    }

    public ItemStack item() { return item; }
    public int maskedSeed() { return maskedSeed; }
    public int[] costs() { return costs.clone(); }
    public int[] clueIds() { return clueIds.clone(); }
    public int[] clueLevels() { return clueLevels.clone(); }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObservedEnchantState other)) return false;
        return maskedSeed == other.maskedSeed
                && ItemStack.isSameItemSameComponents(item, other.item)
                && Arrays.equals(costs, other.costs)
                && Arrays.equals(clueIds, other.clueIds)
                && Arrays.equals(clueLevels, other.clueLevels);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                item.getItem(),
                item.getComponents(),
                maskedSeed,
                Arrays.hashCode(costs),
                Arrays.hashCode(clueIds),
                Arrays.hashCode(clueLevels)
        );
    }
}