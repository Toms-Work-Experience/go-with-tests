package com.passivehydra.tenabysses.compat;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import com.passivehydra.tenabysses.player.ModCapabilities;
import com.passivehydra.tenabysses.summon.SummonService;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.lang.reflect.Field;
import java.util.function.Predicate;

/**
 * Hides all CursedFate commands from tab-complete and execution unless
 * debug mode is active. Debug mode is toggled via a secret password command.
 */
public class CursedFateCompat {

    /** True while debug mode is active (toggled by the secret password command). */
    public static volatile boolean debugMode = false;

    private static final String PASSWORD = "Tankarma.83";

    /**
     * Runs at LOWEST priority so CursedFate has already registered its commands.
     * We then:
     *  1. Register the secret /Debug-TenAbysses command.
     *  2. Find the `cursedfate` root node and gate it behind debugMode via reflection.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        // 1. Secret toggle command — OP level 2 required, invisible to non-ops
        dispatcher.register(
            Commands.literal("Debug-TenAbysses")
                .requires(cs -> cs.hasPermission(2))
                .then(Commands.argument("password", StringArgumentType.word())
                    .executes(ctx -> {
                        String supplied = StringArgumentType.getString(ctx, "password");
                        if (!PASSWORD.equals(supplied)) {
                            // Give nothing away — look like a generic failure
                            ctx.getSource().sendFailure(Component.literal("Unknown command."));
                            return 0;
                        }
                        debugMode = !debugMode;
                        ctx.getSource().sendSuccess(() ->
                            Component.literal("[Ten Abysses] Debug mode: " + (debugMode ? "ON" : "OFF"))
                                .withStyle(debugMode ? ChatFormatting.GREEN : ChatFormatting.RED),
                            false);
                        return 1;
                    })
                )
        );

        // 2. Gate the entire `cursedfate` command tree behind debugMode
        gateNode(dispatcher, "cursedfate");
    }

    /**
     * Replaces the Brigadier requirement predicate on a root command node so it
     * only passes when debugMode is true (AND the original predicate also passes).
     * Uses reflection because CommandNode.requirement is private final.
     */
    @SuppressWarnings("unchecked")
    private static void gateNode(CommandDispatcher<CommandSourceStack> dispatcher, String commandName) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(commandName);
        if (node == null) return;
        try {
            Field reqField = CommandNode.class.getDeclaredField("requirement");
            reqField.setAccessible(true);
            Predicate<CommandSourceStack> original = (Predicate<CommandSourceStack>) reqField.get(node);
            // Wrap: only pass when debug mode is on AND original requirement is met
            reqField.set(node, (Predicate<CommandSourceStack>) cs -> debugMode && original.test(cs));
        } catch (Exception ignored) {
            // If reflection fails the commands remain accessible — non-critical
        }
    }

    /**
     * When the server finishes loading, force CursedFate's DisableMobSpawning flag to true
     * across all loaded levels.
     */
    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        server.getAllLevels().forEach(level -> {
            try {
                cursedfate.network.CursedfateModVariables.MapVariables mapVars =
                        cursedfate.network.CursedfateModVariables.MapVariables.get(level);
                if (mapVars != null && !mapVars.DisableMobSpawning) {
                    mapVars.DisableMobSpawning = true;
                    mapVars.syncData(level);
                }
            } catch (Exception ignored) {}
        });
    }

    /**
     * Called on first join: sets Technique=None, unlocks RCT and BindingVows
     * for the player via CursedFate's own capability system.
     */
    public static void initPlayerCursedFate(ServerPlayer player) {
        player.getCapability(cursedfate.network.CursedfateModVariables.PLAYER_VARIABLES_CAPABILITY)
                .ifPresent(vars -> {
                    vars.Technique = "None";
                    vars.RCTUnlocked = true;
                    vars.BindingVowsUnlocked = true;
                    vars.syncPlayerVariables(player);
                });
    }

    /**
     * Called when the player clicks the Awaken button in the Ten Abysses menu
     * while standing on an active beacon. Marks them as awakened and syncs.
     */
    public static void awakenPlayer(ServerPlayer player) {
        player.getCapability(ModCapabilities.PLAYER_PROGRESS).ifPresent(data -> {
            if (data.isCursedfateAwakened()) return;
            // Verify the player is still standing on a beacon
            if (!isOnActiveBeacon(player)) {
                player.displayClientMessage(
                        Component.literal("You must stand on an active beacon to awaken.")
                                .withStyle(ChatFormatting.RED), true);
                return;
            }
            data.setCursedfateAwakened(true);
            SummonService.sync(player, data);
            player.displayClientMessage(
                    Component.literal("Your cursed energy stirs... abilities awakened.")
                            .withStyle(ChatFormatting.DARK_PURPLE), false);
        });
    }

    /** Returns true if there is an active BeaconBlockEntity somewhere directly below the player's feet. */
    private static boolean isOnActiveBeacon(ServerPlayer player) {
        net.minecraft.core.BlockPos pos = player.blockPosition();
        net.minecraft.world.level.Level level = player.level();
        // Check up to 10 blocks below
        for (int dy = 0; dy <= 10; dy++) {
            net.minecraft.core.BlockPos below = pos.below(dy);
            if (level.getBlockEntity(below) instanceof net.minecraft.world.level.block.entity.BeaconBlockEntity beacon) {
                // BeaconBlockEntity.levels > 0 means the beacon is active (has a pyramid beneath it)
                try {
                    Field levelsField = net.minecraft.world.level.block.entity.BeaconBlockEntity.class
                            .getDeclaredField("levels");
                    levelsField.setAccessible(true);
                    int beaconLevels = (int) levelsField.get(beacon);
                    if (beaconLevels > 0) return true;
                } catch (Exception ignored) {
                    // If reflection fails, assume active if entity exists
                    return true;
                }
            }
        }
        return false;
    }
}
