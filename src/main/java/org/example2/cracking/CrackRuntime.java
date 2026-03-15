package org.example2.cracking;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class CrackRuntime {
    private CrackRuntime() {}

    public static final int BOOKSHELF_POWER = 15; // まずは固定で確認

    public static volatile ObservedEnchantState latestObservation = null;
    public static final List<ObservedEnchantState> observations = new ArrayList<>();

    public static volatile int candidateCount = 0;
    public static volatile Integer crackedSeed = null;
    public static volatile boolean running = false;
    public static volatile String status = "idle";

    public static final AtomicInteger epoch = new AtomicInteger(0);

    public static void reset() {
        latestObservation = null;
        observations.clear();
        candidateCount = 0;
        crackedSeed = null;
        running = false;
        status = "idle";
        epoch.incrementAndGet();
    }
}