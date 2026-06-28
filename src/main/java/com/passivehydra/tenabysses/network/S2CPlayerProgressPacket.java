package com.passivehydra.tenabysses.network;

import com.passivehydra.tenabysses.client.ClientProgressState;
import com.passivehydra.tenabysses.player.PlayerProgressData;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

public class S2CPlayerProgressPacket {
    private final boolean voidAwakened;
    private final boolean dogLost;
    private final Set<String> unlocked;
    private final Map<String, Long> cooldownEnd;
    private final boolean absorbMode;
    private final Set<String> absorbedMobs;
    private final boolean cursedfateAwakened;

    public S2CPlayerProgressPacket(boolean voidAwakened, boolean dogLost, Set<String> unlocked,
                                   Map<String, Long> cooldownEnd, boolean absorbMode, Set<String> absorbedMobs,
                                   boolean cursedfateAwakened) {
        this.voidAwakened = voidAwakened;
        this.dogLost = dogLost;
        this.unlocked = unlocked;
        this.cooldownEnd = cooldownEnd;
        this.absorbMode = absorbMode;
        this.absorbedMobs = absorbedMobs;
        this.cursedfateAwakened = cursedfateAwakened;
    }

    public static S2CPlayerProgressPacket fromData(PlayerProgressData data) {
        return new S2CPlayerProgressPacket(data.isVoidAwakened(), data.isDogLost(),
                new HashSet<>(data.unlockedView()), new HashMap<>(data.cooldownView()),
                data.isAbsorbMode(), new HashSet<>(data.getAbsorbedMobs()),
                data.isCursedfateAwakened());
    }

    public static void encode(S2CPlayerProgressPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.voidAwakened);
        buf.writeBoolean(msg.dogLost);

        buf.writeVarInt(msg.unlocked.size());
        for (String value : msg.unlocked) {
            buf.writeUtf(value);
        }

        buf.writeVarInt(msg.cooldownEnd.size());
        for (Map.Entry<String, Long> entry : msg.cooldownEnd.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeLong(entry.getValue());
        }

        buf.writeBoolean(msg.absorbMode);
        buf.writeVarInt(msg.absorbedMobs.size());
        for (String id : msg.absorbedMobs) {
            buf.writeUtf(id);
        }
        buf.writeBoolean(msg.cursedfateAwakened);
    }

    public static S2CPlayerProgressPacket decode(FriendlyByteBuf buf) {
        boolean voidAwakened = buf.readBoolean();
        boolean dogLost = buf.readBoolean();

        int unlockedSize = buf.readVarInt();
        Set<String> unlocked = new HashSet<>();
        for (int i = 0; i < unlockedSize; i++) {
            unlocked.add(buf.readUtf());
        }

        int cooldownSize = buf.readVarInt();
        Map<String, Long> cooldown = new HashMap<>();
        for (int i = 0; i < cooldownSize; i++) {
            cooldown.put(buf.readUtf(), buf.readLong());
        }

        boolean absorbMode = buf.readBoolean();
        int absorbedSize = buf.readVarInt();
        Set<String> absorbedMobs = new HashSet<>();
        for (int i = 0; i < absorbedSize; i++) {
            absorbedMobs.add(buf.readUtf());
        }

        return new S2CPlayerProgressPacket(voidAwakened, dogLost, unlocked, cooldown, absorbMode, absorbedMobs,
                buf.readBoolean());
    }

    public static void handle(S2CPlayerProgressPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> ClientProgressState.update(msg.voidAwakened, msg.dogLost, msg.unlocked, msg.cooldownEnd, msg.absorbMode, msg.absorbedMobs, msg.cursedfateAwakened));
        context.get().setPacketHandled(true);
    }
}
