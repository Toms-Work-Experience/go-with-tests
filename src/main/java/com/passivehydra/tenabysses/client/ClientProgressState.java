package com.passivehydra.tenabysses.client;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ClientProgressState {
    private static boolean voidAwakened;
    private static boolean dogLost;
    private static boolean absorbMode;
    private static boolean cursedfateAwakened;
    private static final Set<String> unlocked = new HashSet<>();
    private static final Map<String, Long> cooldownEnd = new HashMap<>();
    private static final Set<String> absorbedMobs = new HashSet<>();

    private ClientProgressState() {
    }

    public static void update(boolean newVoidAwakened, boolean newDogLost, Set<String> newUnlocked,
                              Map<String, Long> newCooldownEnd, boolean newAbsorbMode, Set<String> newAbsorbedMobs,
                              boolean newCursedfateAwakened) {
        voidAwakened = newVoidAwakened;
        dogLost = newDogLost;
        absorbMode = newAbsorbMode;
        cursedfateAwakened = newCursedfateAwakened;
        unlocked.clear();
        unlocked.addAll(newUnlocked);
        cooldownEnd.clear();
        cooldownEnd.putAll(newCooldownEnd);
        absorbedMobs.clear();
        absorbedMobs.addAll(newAbsorbedMobs);
    }

    public static boolean isVoidAwakened() {
        return voidAwakened;
    }

    public static boolean isDogLost() {
        return dogLost;
    }

    public static boolean isAbsorbMode() { return absorbMode; }
    public static boolean isCursedfateAwakened() { return cursedfateAwakened; }

    public static Set<String> absorbedMobsView() {
        return Collections.unmodifiableSet(absorbedMobs);
    }

    public static boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    public static long cooldownEnd(String id) {
        return cooldownEnd.getOrDefault(id, 0L);
    }

    public static Set<String> unlockedView() {
        return Collections.unmodifiableSet(unlocked);
    }
}
