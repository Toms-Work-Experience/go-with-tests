package com.passivehydra.tenabysses.client.screen;

import com.passivehydra.tenabysses.client.ClientHooks;
import com.passivehydra.tenabysses.client.ClientProgressState;
import com.passivehydra.tenabysses.network.C2SSummonRequestPacket;
import com.passivehydra.tenabysses.network.ModNetwork;
import com.passivehydra.tenabysses.summon.ShikigamiType;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.List;

public class SummonMenuScreen extends Screen {
    private static final int COLS = 3;
    private static final int BUTTON_W = 156;
    private static final int BUTTON_H = 26;
    private static final int PAD = 6;

    // Tab state: false = Ten Shadows, true = Absorptions
    private boolean showAbsorptions = false;

    private final List<Entry> tenShadowsEntries = new ArrayList<>();
    private final List<AbsorbedEntry> absorbedEntries = new ArrayList<>();

    public SummonMenuScreen() {
        super(Component.literal("Ten Shadows — Summoning Menu"));
    }

    @Override
    protected void init() {
        super.init();
        tenShadowsEntries.clear();
        absorbedEntries.clear();

        int panelW = COLS * (BUTTON_W + PAD) + 20;
        int panelX = (width - panelW) / 2;
        int startX = panelX + 10;
        int startY = 72; // below title + tabs

        if (!showAbsorptions) {
            buildTenShadowsSection(startX, startY);
        } else {
            buildAbsorptionsSection(startX, startY);
        }

        // Tab buttons at top
        int tabW = panelW / 2 - 4;
        int tabY = 40;
        Button tenTab = Button.builder(Component.literal("Ten Shadows").withStyle(showAbsorptions ? ChatFormatting.GRAY : ChatFormatting.AQUA), b -> {
                    showAbsorptions = false;
                    rebuildWidgets();
                })
                .bounds(panelX + 2, tabY, tabW, 20)
                .build();
        addRenderableWidget(tenTab);

        Button absorbTab = Button.builder(Component.literal("Absorptions").withStyle(showAbsorptions ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GRAY), b -> {
                    showAbsorptions = true;
                    rebuildWidgets();
                })
                .bounds(panelX + tabW + 6, tabY, tabW, 20)
                .build();
        addRenderableWidget(absorbTab);
    }

    private void buildTenShadowsSection(int startX, int startY) {
        ShikigamiType[] values = ShikigamiType.values();
        int visibleIndex = 0;
        for (ShikigamiType type : values) {
            if (!type.menuVisible()) continue;
            int row = visibleIndex / COLS;
            int col = visibleIndex % COLS;
            int x = startX + col * (BUTTON_W + PAD);
            int y = startY + row * (BUTTON_H + 8);

            Availability av = availability(type);
            Button btn = Button.builder(buttonLabel(type), b -> {
                        ClientHooks.lastSummonedId = type.id();
                        ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(type.id()));
                        onClose();
                    })
                    .bounds(x, y, BUTTON_W, BUTTON_H)
                    .build();
            btn.active = av.available;
            addRenderableWidget(btn);
            tenShadowsEntries.add(new Entry(type, x, y, av));
            visibleIndex++;
        }

        int rows = (visibleIndex + COLS - 1) / COLS;
        int ctrlY = startY + rows * (BUTTON_H + 8) + PAD;
        int ctrlW = BUTTON_W * 2 + PAD;
        int ctrlX = (width - ctrlW) / 2;

        addRenderableWidget(Button.builder(Component.literal("Unsummon All Shikigami"), b -> {
                    ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.UNSUMMON_REQUEST));
                    onClose();
                }).bounds(ctrlX, ctrlY, ctrlW, BUTTON_H).build());

        if (ClientProgressState.isUnlocked(ShikigamiType.MAHORAGA.id())) {
            boolean equipped = isWheelEquipped();
            addRenderableWidget(Button.builder(
                    Component.literal(equipped ? "Unequip Mahoraga Wheel" : "Equip Mahoraga Wheel"), b -> {
                        ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.TOGGLE_WHEEL_REQUEST));
                        onClose();
                    }).bounds(ctrlX, ctrlY + BUTTON_H + PAD, ctrlW, BUTTON_H).build());
        }

        // Awaken button: only show if not yet awakened AND standing on a beacon
        if (!ClientProgressState.isCursedfateAwakened() && isStandingOnBeacon()) {
            int awakenY = ctrlY + (ClientProgressState.isUnlocked(ShikigamiType.MAHORAGA.id()) ? (BUTTON_H + PAD) * 2 : BUTTON_H + PAD);
            addRenderableWidget(Button.builder(
                    Component.literal("\u25c6 Awaken \u25c6").withStyle(ChatFormatting.GOLD), b -> {
                        ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.AWAKEN_CURSEDFATE_REQUEST));
                        onClose();
                    }).bounds(ctrlX, awakenY, ctrlW, BUTTON_H).build());
        }
    }

    private void buildAbsorptionsSection(int startX, int startY) {
        // Absorbing Mode toggle at top of section
        boolean absorbMode = ClientProgressState.isAbsorbMode();
        int ctrlW = BUTTON_W * 2 + PAD;
        int ctrlX = (width - ctrlW) / 2;

        addRenderableWidget(Button.builder(
                Component.literal("Absorbing Mode: ").append(
                        Component.literal(absorbMode ? "ON" : "OFF")
                                .withStyle(absorbMode ? ChatFormatting.GREEN : ChatFormatting.RED)), b -> {
                    ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.TOGGLE_ABSORB_REQUEST));
                    onClose();
                }).bounds(ctrlX, startY, ctrlW, BUTTON_H).build());

        // Absorbed mob grid
        List<String> absorbed = new ArrayList<>(ClientProgressState.absorbedMobsView());
        absorbed.sort(String::compareTo);

        if (absorbed.isEmpty()) {
            // no buttons, just render a hint in render()
        } else {
            for (int i = 0; i < absorbed.size(); i++) {
                String typeId = absorbed.get(i);
                String displayName = friendlyName(typeId);
                int row = i / COLS;
                int col = i % COLS;
                int x = startX + col * (BUTTON_W + PAD);
                int y = startY + BUTTON_H + 10 + row * (BUTTON_H + 8);

                Button btn = Button.builder(Component.literal(displayName), b -> {
                            ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(
                                    C2SSummonRequestPacket.ABSORBED_PREFIX + typeId));
                            onClose();
                        })
                        .bounds(x, y, BUTTON_W, BUTTON_H)
                        .build();
                addRenderableWidget(btn);
                absorbedEntries.add(new AbsorbedEntry(typeId, displayName, x, y));
            }
        }

        // Unsummon all stays at bottom
        int rows = absorbed.isEmpty() ? 0 : (absorbed.size() + COLS - 1) / COLS;
        int unsummonY = startY + BUTTON_H + 10 + rows * (BUTTON_H + 8) + PAD;
        addRenderableWidget(Button.builder(Component.literal("Unsummon All Shikigami"), b -> {
                    ModNetwork.CHANNEL.sendToServer(new C2SSummonRequestPacket(C2SSummonRequestPacket.UNSUMMON_REQUEST));
                    onClose();
                }).bounds(ctrlX, unsummonY, ctrlW, BUTTON_H).build());
    }

    private String friendlyName(String typeId) {
        // typeId is like "minecraft:zombie" — get the registered name from EntityType
        ResourceLocation rl = ResourceLocation.tryParse(typeId);
        if (rl != null) {
            var type = ForgeRegistries.ENTITY_TYPES.getValue(rl);
            if (type != null) {
                return type.getDescription().getString();
            }
        }
        // fallback: strip namespace, replace underscores
        String path = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
        return path.replace('_', ' ');
    }

    private boolean isWheelEquipped() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(mc.player.getInventory().armor.get(3).getItem());
        return key != null && "mahoragawheel".equals(key.getNamespace()) && "mahoraga_wheel".equals(key.getPath());
    }

    /** Returns true if the local player has an active beacon within 10 blocks below their feet. */
    private static boolean isStandingOnBeacon() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return false;
        BlockPos pos = mc.player.blockPosition();
        for (int dy = 0; dy <= 10; dy++) {
            if (mc.level.getBlockEntity(pos.below(dy)) instanceof BeaconBlockEntity beacon) {
                // BeaconBlockEntity exposes getLevels() in some mappings; use reflection as fallback
                try {
                    java.lang.reflect.Field f = BeaconBlockEntity.class.getDeclaredField("levels");
                    f.setAccessible(true);
                    return (int) f.get(beacon) > 0;
                } catch (Exception e) {
                    return true; // if we can find the entity, assume active
                }
            }
        }
        return false;
    }

    private Component buttonLabel(ShikigamiType type) {
        boolean unlocked = ClientProgressState.isUnlocked(type.id());
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        long cooldown = Math.max(0L, ClientProgressState.cooldownEnd(type.id()) - now);
        String prefix = unlocked ? "Unlocked" : "Ritual";
        String cd = cooldown > 0L ? " | " + (cooldown / 20L) + "s" : "";
        return Component.literal("[" + prefix + cd + "] ").append(type.displayName());
    }

    private Availability availability(ShikigamiType type) {
        if (type == ShikigamiType.DIVINE_DOG_TOTALITY && !ClientProgressState.isDogLost()) {
            return new Availability(false, "Requires a fallen Divine Dog");
        }
        for (String required : type.prerequisiteUnlocks()) {
            if (!ClientProgressState.isUnlocked(required)) {
                return new Availability(false, "Requires " + required.replace('_', ' '));
            }
        }
        long now = Minecraft.getInstance().level != null ? Minecraft.getInstance().level.getGameTime() : 0L;
        long remaining = ClientProgressState.cooldownEnd(type.id()) - now;
        if (remaining > 0L) {
            return new Availability(false, "Cooldown: " + (remaining / 20L) + "s");
        }
        return new Availability(true, "Ready");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);

        int panelW = COLS * (BUTTON_W + PAD) + 20;
        int panelX = (width - panelW) / 2;
        int panelY = 24;
        int panelH = height - 28;

        // Panel background
        graphics.fillGradient(panelX, panelY, panelX + panelW, panelY + panelH, 0xC0101014, 0xD01C1C24);
        graphics.fill(panelX + 2, panelY + 2, panelX + panelW - 2, panelY + panelH - 2, 0x90111118);

        graphics.drawCenteredString(font, title, width / 2, panelY + 8, 0xEAF8FF);
        graphics.drawCenteredString(font, Component.literal("Ten Abysses Invocation").withStyle(ChatFormatting.DARK_AQUA), width / 2, panelY + 18, 0x7FD7DF);

        super.render(graphics, mouseX, mouseY, partialTick);

        if (!showAbsorptions) {
            // Render shikigami icons + tooltips
            for (Entry entry : tenShadowsEntries) {
                ResourceLocation texture = entry.type.iconTexture();
                graphics.blit(texture, entry.x + 4, entry.y + 5, 0, 0, 16, 16, 16, 16);
                if (!entry.availability.available) {
                    graphics.fill(entry.x, entry.y, entry.x + BUTTON_W, entry.y + BUTTON_H, 0x50000000);
                }
                if (mouseX >= entry.x && mouseX <= entry.x + BUTTON_W && mouseY >= entry.y && mouseY <= entry.y + BUTTON_H) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(entry.type.displayName());
                    for (String line : entry.type.tooltipLines()) {
                        tooltip.add(Component.literal(line).withStyle(ChatFormatting.GRAY));
                    }
                    tooltip.add(Component.literal(entry.availability.reason)
                            .withStyle(entry.availability.available ? ChatFormatting.GREEN : ChatFormatting.RED));
                    List<FormattedCharSequence> lines = tooltip.stream().map(Component::getVisualOrderText).toList();
                    graphics.renderTooltip(font, lines, mouseX, mouseY);
                }
            }
        } else {
            if (absorbedEntries.isEmpty() && ClientProgressState.absorbedMobsView().isEmpty()) {
                graphics.drawCenteredString(font,
                        Component.literal("No absorptions yet. Enable Absorbing Mode and kill mobs.").withStyle(ChatFormatting.GRAY),
                        width / 2, 100, 0xAAAAAA);
            }
            // Tooltip for absorbed entries
            for (AbsorbedEntry entry : absorbedEntries) {
                if (mouseX >= entry.x && mouseX <= entry.x + BUTTON_W && mouseY >= entry.y && mouseY <= entry.y + BUTTON_H) {
                    List<Component> tooltip = new ArrayList<>();
                    tooltip.add(Component.literal(entry.displayName).withStyle(ChatFormatting.LIGHT_PURPLE));
                    tooltip.add(Component.literal(entry.typeId).withStyle(ChatFormatting.DARK_GRAY));
                    tooltip.add(Component.literal("Click to summon").withStyle(ChatFormatting.GREEN));
                    graphics.renderTooltip(font, tooltip.stream().map(Component::getVisualOrderText).toList(), mouseX, mouseY);
                }
            }
        }
    }

    private record Entry(ShikigamiType type, int x, int y, Availability availability) {}
    private record AbsorbedEntry(String typeId, String displayName, int x, int y) {}
    private record Availability(boolean available, String reason) {}
}
