package com.passivehydra.tenabysses.player;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.util.INBTSerializable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PlayerProgressData implements INBTSerializable<CompoundTag> {
    private boolean voidAwakened;
    private boolean dogLost;
    private boolean absorbMode;
    /** True once CursedFate initial setup has been run for this player (CT=None, RCT+Vows unlocked). */
    private boolean cursedfateInitDone;
    /** True once the player has awakened CursedFate abilities via the beacon ritual. */
    private boolean cursedfateAwakened;
    private final Set<String> unlocked = new HashSet<>();
    private final Map<String, Long> cooldownEndTick = new HashMap<>();
    private final Set<String> absorbedMobs = new HashSet<>();

    public boolean isVoidAwakened() {
        return voidAwakened;
    }

    public void setVoidAwakened(boolean voidAwakened) {
        this.voidAwakened = voidAwakened;
    }

    public boolean isDogLost() {
        return dogLost;
    }

    public void setDogLost(boolean dogLost) {
        this.dogLost = dogLost;
    }

    public boolean isAbsorbMode() { return absorbMode; }
    public void setAbsorbMode(boolean absorbMode) { this.absorbMode = absorbMode; }
    public Set<String> getAbsorbedMobs() { return absorbedMobs; }
    public boolean addAbsorbed(String entityTypeId) { return absorbedMobs.add(entityTypeId); }

    public boolean isCursedfateInitDone() { return cursedfateInitDone; }
    public void setCursedfateInitDone(boolean v) { cursedfateInitDone = v; }
    public boolean isCursedfateAwakened() { return cursedfateAwakened; }
    public void setCursedfateAwakened(boolean v) { cursedfateAwakened = v; }

    public boolean isUnlocked(String id) {
        return unlocked.contains(id);
    }

    public void unlock(String id) {
        unlocked.add(id);
    }

    public Set<String> unlockedView() {
        return unlocked;
    }

    public long cooldownEnd(String id) {
        return cooldownEndTick.getOrDefault(id, 0L);
    }

    public boolean isOnCooldown(String id, long gameTime) {
        return cooldownEnd(id) > gameTime;
    }

    public void setCooldownEnd(String id, long endTick) {
        cooldownEndTick.put(id, endTick);
    }

    public Map<String, Long> cooldownView() {
        return cooldownEndTick;
    }

    @Override
    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("VoidAwakened", voidAwakened);
        tag.putBoolean("DogLost", dogLost);

        ListTag unlockedList = new ListTag();
        for (String id : unlocked) {
            unlockedList.add(StringTag.valueOf(id));
        }
        tag.put("Unlocked", unlockedList);

        CompoundTag cooldowns = new CompoundTag();
        for (Map.Entry<String, Long> entry : cooldownEndTick.entrySet()) {
            cooldowns.putLong(entry.getKey(), entry.getValue());
        }
        tag.put("Cooldowns", cooldowns);

        tag.putBoolean("AbsorbMode", absorbMode);
        tag.putBoolean("CursedfateInitDone", cursedfateInitDone);
        tag.putBoolean("CursedfateAwakened", cursedfateAwakened);
        ListTag absorbedList = new ListTag();
        for (String id : absorbedMobs) {
            absorbedList.add(StringTag.valueOf(id));
        }
        tag.put("AbsorbedMobs", absorbedList);

        return tag;
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        voidAwakened = nbt.getBoolean("VoidAwakened");
        dogLost = nbt.getBoolean("DogLost");

        unlocked.clear();
        ListTag unlockedList = nbt.getList("Unlocked", Tag.TAG_STRING);
        for (Tag entry : unlockedList) {
            unlocked.add(entry.getAsString());
        }

        cooldownEndTick.clear();
        CompoundTag cooldowns = nbt.getCompound("Cooldowns");
        for (String key : cooldowns.getAllKeys()) {
            cooldownEndTick.put(key, cooldowns.getLong(key));
        }

        absorbMode = nbt.getBoolean("AbsorbMode");
        cursedfateInitDone = nbt.getBoolean("CursedfateInitDone");
        cursedfateAwakened = nbt.getBoolean("CursedfateAwakened");
        absorbedMobs.clear();
        ListTag absorbedList = nbt.getList("AbsorbedMobs", Tag.TAG_STRING);
        for (Tag entry : absorbedList) {
            absorbedMobs.add(entry.getAsString());
        }
    }

    public void copyFrom(PlayerProgressData other) {
        this.voidAwakened = other.voidAwakened;
        this.dogLost = other.dogLost;
        this.unlocked.clear();
        this.unlocked.addAll(other.unlocked);
        this.cooldownEndTick.clear();
        this.cooldownEndTick.putAll(other.cooldownEndTick);
        this.absorbMode = other.absorbMode;
        this.cursedfateInitDone = other.cursedfateInitDone;
        this.cursedfateAwakened = other.cursedfateAwakened;
        this.absorbedMobs.clear();
        this.absorbedMobs.addAll(other.absorbedMobs);
    }
}
