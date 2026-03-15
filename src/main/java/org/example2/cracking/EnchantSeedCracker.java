package org.example2.cracking;


import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.core.IdMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EnchantSeedCracker {
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "EnchantSeedCracker");
        t.setDaemon(true);
        return t;
    });

    private EnchantSeedCracker() {}

    public static void submit(RegistryAccess access) {
        int runEpoch = CrackRuntime.epoch.incrementAndGet();
        List<ObservedEnchantState> snapshot = List.copyOf(CrackRuntime.observations);

        CrackRuntime.running = true;
        CrackRuntime.status = "running";

        EXEC.submit(() -> run(access, snapshot, runEpoch));
    }

    private static void run(RegistryAccess access, List<ObservedEnchantState> observations, int runEpoch) {
        try {
            if (observations.isEmpty()) {
                finishIfLatest(runEpoch, 0, null, "idle");
                return;
            }

            IntArrayList candidates = initCandidates(observations.getFirst().maskedSeed());

            for (ObservedEnchantState obs : observations) {
                candidates = filterByCosts(candidates, obs);
                if (candidates.isEmpty()) {
                    finishIfLatest(runEpoch, 0, null, "no candidates after costs");
                    return;
                }

                int before = candidates.size();
                candidates = filterByCosts(candidates, obs);
                int after = candidates.size();

                System.out.println(
                        "[cost-filter] item=" + obs.item().getItem()
                                + " maskedSeed=0x" + Integer.toHexString(obs.maskedSeed())
                                + " costs=" + java.util.Arrays.toString(obs.costs())
                                + " before=" + before
                                + " after=" + after
                );
            }

            Integer cracked = (candidates.size() == 1) ? candidates.getInt(0) : null;
            finishIfLatest(runEpoch, candidates.size(), cracked, cracked != null ? "cracked" : "partial");
        } catch (Exception e) {
            finishIfLatest(runEpoch, 0, null, "error: " + e.getMessage());
        }
    }

    private static void finishIfLatest(int runEpoch, int count, Integer crackedSeed, String status) {
        if (runEpoch != CrackRuntime.epoch.get()) return;
        CrackRuntime.candidateCount = count;
        CrackRuntime.crackedSeed = crackedSeed;
        CrackRuntime.status = status;
        CrackRuntime.running = false;
    }

    private static IntArrayList initCandidates(int maskedSeed) {
        IntArrayList out = new IntArrayList(1 << 20);
        int base = maskedSeed & 0x0000fff0;

        for (int highBits = 0; highBits < 65536; highBits++) {
            int upper = highBits << 16;
            for (int low4 = 0; low4 < 16; low4++) {
                out.add(upper | base | low4);
            }
        }
        return out;
    }

    private static IntArrayList filterByCosts(IntArrayList input, ObservedEnchantState obs) {
        IntArrayList out = new IntArrayList(input.size());
        RandomSource rand = RandomSource.create();

        for (int i = 0; i < input.size(); i++) {
            int xpSeed = input.getInt(i);
            rand.setSeed(xpSeed);

            boolean ok = true;
            int[] actual = obs.costs();

            for (int slot = 0; slot < 3; slot++) {
                int cost = EnchantmentHelper.getEnchantmentCost(
                        rand, slot, CrackRuntime.BOOKSHELF_POWER, obs.item()
                );
                if (cost < slot + 1) cost = 0;

                if (cost != actual[slot]) {
                    ok = false;
                    break;
                }
            }

            if (ok) out.add(xpSeed);
        }

        return out;
    }

    private static IntArrayList filterByClues(IntArrayList input, ObservedEnchantState obs, RegistryAccess access) {
        IntArrayList out = new IntArrayList(input.size());

        Registry<Enchantment> reg = access.registryOrThrow(Registries.ENCHANTMENT);
        IdMap<net.minecraft.core.Holder<Enchantment>> idMap = reg.asHolderIdMap();

        for (int i = 0; i < input.size(); i++) {
            int xpSeed = input.getInt(i);
            if (matchesClues(xpSeed, obs, reg, idMap)) {
                out.add(xpSeed);
            }
        }

        return out;
    }
    private static boolean matchesClues(
            int xpSeed,
            ObservedEnchantState obs,
            Registry<Enchantment> reg,
            IdMap<net.minecraft.core.Holder<Enchantment>> idMap
    ) {
        for (int slot = 0; slot < 3; slot++) {
            int cost = obs.costs()[slot];

            if (cost <= 0) {
                if (obs.clueIds()[slot] != -1 || obs.clueLevels()[slot] != -1) {
                    return false;
                }
                continue;
            }

            List<EnchantmentInstance> list = VanillaEnchantSim.getEnchantmentList(
                    reg,
                    xpSeed,
                    obs.item(),
                    slot,
                    cost,
                    CrackRuntime.BOOKSHELF_POWER
            );

            if (list.isEmpty()) {
                if (obs.clueIds()[slot] != -1 || obs.clueLevels()[slot] != -1) {
                    return false;
                }
                continue;
            }

            EnchantmentInstance clue = VanillaEnchantSim.pickDisplayedClue(
                    reg,
                    xpSeed,
                    obs.item(),
                    slot,
                    cost
            );

            if (clue == null) {
                return false;
            }

            int clueId = idMap.getId(clue.enchantment);
            int clueLv = clue.level;

            if (clueId != obs.clueIds()[slot] || clueLv != obs.clueLevels()[slot]) {
                return false;
            }
        }

        return true;
    }
}