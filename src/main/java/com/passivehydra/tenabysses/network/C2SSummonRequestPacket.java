package com.passivehydra.tenabysses.network;

import com.passivehydra.tenabysses.compat.CursedFateCompat;
import com.passivehydra.tenabysses.summon.ShikigamiType;
import com.passivehydra.tenabysses.summon.SummonService;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class C2SSummonRequestPacket {
    public static final String UNSUMMON_REQUEST = "__unsummon_all__";
    public static final String TOGGLE_WHEEL_REQUEST = "__toggle_mahoraga_wheel__";
    public static final String SUMMON_LAST_REQUEST = "__summon_last__";
    public static final String TOGGLE_ABSORB_REQUEST = "__toggle_absorb__";
    public static final String ABSORBED_PREFIX = "__absorbed__";
    public static final String AWAKEN_CURSEDFATE_REQUEST = "__awaken_cursedfate__";
    /** Tracks the last shikigami type name requested from the client, for the summon-last keybind. */
    public static volatile String lastSummonedType = null;
    private final String shikigamiId;

    public C2SSummonRequestPacket(String shikigamiId) {
        this.shikigamiId = shikigamiId;
    }

    public static void encode(C2SSummonRequestPacket msg, FriendlyByteBuf buf) {
        buf.writeUtf(msg.shikigamiId);
    }

    public static C2SSummonRequestPacket decode(FriendlyByteBuf buf) {
        return new C2SSummonRequestPacket(buf.readUtf());
    }

    public static void handle(C2SSummonRequestPacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            ServerPlayer player = context.get().getSender();
            if (player == null) {
                return;
            }
            if (UNSUMMON_REQUEST.equals(msg.shikigamiId)) {
                SummonService.unsummonAll(player);
                return;
            }
            if (TOGGLE_WHEEL_REQUEST.equals(msg.shikigamiId)) {
                SummonService.toggleMahoragaWheel(player);
                return;
            }
            if (TOGGLE_ABSORB_REQUEST.equals(msg.shikigamiId)) {
                SummonService.toggleAbsorbMode(player);
                return;
            }
            if (AWAKEN_CURSEDFATE_REQUEST.equals(msg.shikigamiId)) {
                CursedFateCompat.awakenPlayer(player);
                return;
            }
            if (msg.shikigamiId.startsWith(ABSORBED_PREFIX)) {
                String entityTypeId = msg.shikigamiId.substring(ABSORBED_PREFIX.length());
                SummonService.summonAbsorbed(player, entityTypeId);
                return;
            }
            ShikigamiType type = ShikigamiType.byId(msg.shikigamiId);
            if (type != null) {
                SummonService.trySummon(player, type);
            }
        });
        context.get().setPacketHandled(true);
    }
}
