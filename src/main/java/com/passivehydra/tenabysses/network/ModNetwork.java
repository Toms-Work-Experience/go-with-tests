package com.passivehydra.tenabysses.network;

import com.passivehydra.tenabysses.TenAbyssesMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(TenAbyssesMod.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId;

    private ModNetwork() {
    }

    public static void init() {
        CHANNEL.registerMessage(packetId++, C2SSummonRequestPacket.class, C2SSummonRequestPacket::encode, C2SSummonRequestPacket::decode, C2SSummonRequestPacket::handle);
        CHANNEL.registerMessage(packetId++, S2CPlayerProgressPacket.class, S2CPlayerProgressPacket::encode, S2CPlayerProgressPacket::decode, S2CPlayerProgressPacket::handle);
    }
}
