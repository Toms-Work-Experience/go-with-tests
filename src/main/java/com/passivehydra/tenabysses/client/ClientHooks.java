package com.passivehydra.tenabysses.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.passivehydra.tenabysses.TenAbyssesMod;
import com.passivehydra.tenabysses.client.render.TenShadowsShikigamiRenderer;
import com.passivehydra.tenabysses.client.screen.SummonMenuScreen;
import com.passivehydra.tenabysses.compat.CursedFateCompat;
import com.passivehydra.tenabysses.network.C2SSummonRequestPacket;
import com.passivehydra.tenabysses.network.ModNetwork;
import com.passivehydra.tenabysses.registry.ModEntities;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
// ClientProgressState is in the same package — no import needed
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.CreativeModeTabRegistry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = TenAbyssesMod.MOD_ID, value = Dist.CLIENT)
public final class ClientHooks {
    public static final KeyMapping OPEN_MENU = new KeyMapping(
            "key.thetenabysses.open_menu",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_RIGHT_BRACKET,
            "key.categories.thetenabysses"
    );
    public static final KeyMapping TOGGLE_WHEEL = new KeyMapping(
            "key.thetenabysses.toggle_wheel",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.thetenabysses"
    );
    public static final KeyMapping SUMMON_LAST = new KeyMapping(
            "key.thetenabysses.summon_last",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.thetenabysses"
    );
    public static final KeyMapping UNSUMMON_ALL = new KeyMapping(
            "key.thetenabysses.unsummon_all",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_UNKNOWN,
            "key.categories.thetenabysses"
    );

    /** Tracks the last shikigami ID sent from this client (for the Summon Last keybind). */
    public static String lastSummonedId = null;

    private ClientHooks() {
    }

    public static void register() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(ClientHooks::onRegisterKeys);
        modBus.addListener(ClientHooks::onRegisterRenderers);
        // Run after CursedFate registers its keys so we can override + hide them
        modBus.addListener(EventPriority.LOWEST, ClientHooks::patchCursedfateKeybinds);
        // Clear CursedFate creative tab contents (also hides from JEI) when debug is off
        modBus.addListener(EventPriority.LOWEST, ClientHooks::onBuildCreativeTabContents);
        MinecraftForge.EVENT_BUS.register(ClientHooks.class);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU);
        event.register(TOGGLE_WHEEL);
        event.register(SUMMON_LAST);
        event.register(UNSUMMON_ALL);
    }

    /**
     * Runs at LOWEST priority (after CursedFate's own RegisterKeyMappingsEvent handler).
     * Sets [ for the CursedFate menu and Tab for combat mode, then hides all CursedFate
     * keybinds from the controls screen so they cannot be seen or changed by the player.
     */
    @SuppressWarnings("unchecked")
    private static void patchCursedfateKeybinds(RegisterKeyMappingsEvent event) {
        InputConstants.Key menuKey   = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_LEFT_BRACKET);
        InputConstants.Key combatKey = InputConstants.Type.KEYSYM.getOrCreate(GLFW.GLFW_KEY_TAB);
        cursedfate.init.CursedfateModKeyMappings.MENU.setKey(menuKey);
        cursedfate.init.CursedfateModKeyMappings.COMBAT_MODE.setKey(combatKey);
        KeyMapping.resetMapping();
        // Use reflection to access private static ALL map and CATEGORIES set
        // to remove all CursedFate keybinds from the controls screen
        try {
            java.lang.reflect.Field allField = KeyMapping.class.getDeclaredField("ALL");
            allField.setAccessible(true);
            java.util.Map<String, KeyMapping> all = (java.util.Map<String, KeyMapping>) allField.get(null);
            all.keySet().removeIf(k -> k.startsWith("key.cursedfate."));

            java.lang.reflect.Field catField = KeyMapping.class.getDeclaredField("CATEGORIES");
            catField.setAccessible(true);
            java.util.Set<String> categories = (java.util.Set<String>) catField.get(null);
            categories.remove("key.categories.cursedfate");
        } catch (Exception e) {
            // Reflection failed — keybinds will still work, just visible in controls screen
        }
    }

    private static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntities.SHIKIGAMI.get(), TenShadowsShikigamiRenderer::new);
    }

    /**
     * Clear all items from CursedFate creative tabs when debug mode is off.
     * BuildCreativeModeTabContentsEvent fires on the MOD bus and its entries map
     * is what JEI reads — so clearing it hides items from JEI too.
     */
    @SuppressWarnings("unchecked")
    private static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        if (CursedFateCompat.debugMode) return;
        ResourceLocation tabKey = CreativeModeTabRegistry.getName(event.getTab());
        if (tabKey != null && "cursedfate".equals(tabKey.getNamespace())) {
            // MutableHashedLinkedMap has no clear() — collect keys then remove each
            var entries = event.getEntries();
            java.util.List<net.minecraft.world.item.ItemStack> keys = new java.util.ArrayList<>();
            for (var entry : entries) keys.add(entry.getKey());
            keys.forEach(entries::remove);
        }
    }

    /**
     * Before the creative mode screen opens, remove CursedFate tabs from the
     * sorted tab list so their icons don't appear in the creative UI at all.
     * We restore them when debug mode is ON by leaving them in place.
     */
    @SubscribeEvent
    @SuppressWarnings("unchecked")
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (!(event.getScreen() instanceof CreativeModeInventoryScreen)) return;
        if (CursedFateCompat.debugMode) return;
        try {
            java.lang.reflect.Field sortedField = CreativeModeTabRegistry.class.getDeclaredField("SORTED_TABS");
            sortedField.setAccessible(true);
            java.util.List<CreativeModeTab> tabs = (java.util.List<CreativeModeTab>) sortedField.get(null);
            tabs.removeIf(tab -> {
                ResourceLocation name = CreativeModeTabRegistry.getName(tab);
                return name != null && "cursedfate".equals(name.getNamespace());
            });
        } catch (Exception ignored) {}
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return;
        }

        // Gate CursedFate keys unless the player has performed the beacon awakening
        if (!ClientProgressState.isCursedfateAwakened()) {
            cursedfate.init.CursedfateModKeyMappings.MENU.consumeClick();
            cursedfate.init.CursedfateModKeyMappings.COMBAT_MODE.consumeClick();
        }

        if (OPEN_MENU.consumeClick()) {
            if (!ClientProgressState.isVoidAwakened()) {
                return;
            }
            mc.setScreen(new SummonMenuScreen());
            return;
        }

        if (TOGGLE_WHEEL.consumeClick()) {
            if (ClientProgressState.isUnlocked("mahoraga")) {
                ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.TOGGLE_WHEEL_REQUEST));
            }
            return;
        }

        if (SUMMON_LAST.consumeClick()) {
            if (ClientProgressState.isVoidAwakened() && lastSummonedId != null) {
                ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(lastSummonedId));
            }
            return;
        }

        if (UNSUMMON_ALL.consumeClick()) {
            if (ClientProgressState.isVoidAwakened()) {
                ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.UNSUMMON_REQUEST));
            }
        }
    }
}
