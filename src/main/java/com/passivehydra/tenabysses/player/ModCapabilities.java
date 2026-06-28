package com.passivehydra.tenabysses.player;

import com.passivehydra.tenabysses.TenAbyssesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;

public final class ModCapabilities {
    public static final Capability<PlayerProgressData> PLAYER_PROGRESS = CapabilityManager.get(new CapabilityToken<>() {
    });
    public static final ResourceLocation PLAYER_PROGRESS_ID = new ResourceLocation(TenAbyssesMod.MOD_ID, "player_progress");

    private ModCapabilities() {
    }
}
