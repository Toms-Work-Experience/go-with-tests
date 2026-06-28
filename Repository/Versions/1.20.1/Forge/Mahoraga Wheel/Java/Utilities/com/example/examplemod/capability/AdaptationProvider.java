package com.example.examplemod.capability;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AdaptationProvider implements ICapabilitySerializable<CompoundTag> {
   public static final Capability<AdaptationCapability> ADAPTATION_CAP = CapabilityManager.get(new CapabilityToken<AdaptationCapability>() {});
   private final AdaptationCapability instance = new AdaptationCapability();
   private final LazyOptional<AdaptationCapability> optional = LazyOptional.of(() -> this.instance);

   @NotNull
   public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
      return cap == ADAPTATION_CAP ? this.optional.cast() : LazyOptional.empty();
   }

   public CompoundTag serializeNBT() {
      return this.instance.serializeNBT();
   }

   public void deserializeNBT(CompoundTag nbt) {
      this.instance.deserializeNBT(nbt);
   }
}
