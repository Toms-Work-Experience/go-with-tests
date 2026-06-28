package com.example.examplemod.client.screen;

import com.example.examplemod.client.KeyBinds;
import com.example.examplemod.compat.JJKCompat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class MaharagaWheelScreen extends Screen {
   private EditBox searchBox;
   private String searchText = "";
   private Map<String, Float> adaptations;
   private Map<String, Integer> pendingHits;
   private Map<String, Integer> cooldowns;
   private static final int W = 252;
   private static final int H = 234;
   private static final int HEADER_H = 60;
   private static final int ENTRY_H = 38;
   private static final int SCROLL_PADDING = 4;
   private int categoryFilter = 0;
   private int sortFilter = 0;
   private static final int MC_BG = -3750202;
   private static final int MC_LIGHT = -1;
   private static final int MC_SHADOW = -11184811;
   private static final int MC_DARK = -13158601;
   private static final int MC_SLOT = -7631989;
   private int left;
   private int top;
   private int scrollOffset = 0;
   private int maxScroll = 0;

   public MaharagaWheelScreen(Map<String, Float> adaptations, Map<String, Integer> pendingHits, Map<String, Integer> cooldowns) {
      super(Component.m_237113_("Wheel of Mahoraga"));
      this.adaptations = adaptations;
      this.pendingHits = pendingHits;
      this.cooldowns = cooldowns;
   }

   public static void openWithData(Map<String, Float> adaptations, Map<String, Integer> pendingHits, Map<String, Integer> cooldowns) {
      Minecraft.m_91087_().m_91152_(new MaharagaWheelScreen(adaptations, pendingHits, cooldowns));
   }

   protected void m_7856_() {
      this.left = (this.f_96543_ - 252) / 2;
      this.top = (this.f_96544_ - 234) / 2;
      this.searchBox = new EditBox(this.f_96547_, this.left + 252 - 110, this.top + 15, 100, 14, Component.m_237113_("Search..."));
      this.searchBox.m_94151_(search -> {
         this.searchText = search.toLowerCase();
         this.scrollOffset = 0;
         this.recalcScroll();
      });
      this.m_142416_(this.searchBox);
      Button catBtn = Button.m_253074_(Component.m_237113_("Category: All"), b -> {
         this.categoryFilter = (this.categoryFilter + 1) % 5;
         String[] cats = new String[]{"All", "JJK", "Melee", "Resist", "Other"};
         b.m_93666_(Component.m_237113_("Category: " + cats[this.categoryFilter]));
         this.scrollOffset = 0;
         this.recalcScroll();
      }).m_252987_(this.left + 10, this.top + 35, 90, 16).m_253136_();
      this.m_142416_(catBtn);
      Button sortBtn = Button.m_253074_(Component.m_237113_("Sort: Newest"), b -> {
         this.sortFilter = (this.sortFilter + 1) % 3;
         String[] sorts = new String[]{"Newest", "Progress", "A-Z"};
         b.m_93666_(Component.m_237113_("Sort: " + sorts[this.sortFilter]));
         this.scrollOffset = 0;
         this.recalcScroll();
      }).m_252987_(this.left + 105, this.top + 35, 90, 16).m_253136_();
      this.m_142416_(sortBtn);
      this.recalcScroll();
   }

   private void recalcScroll() {
      int totalContent = this.getTypes().size() * 38 + 8;
      int visibleH = 170;
      this.maxScroll = Math.max(0, totalContent - visibleH);
      this.scrollOffset = Math.min(this.scrollOffset, this.maxScroll);
   }

   private List<String> getTypes() {
      List<String> list = new ArrayList<>(this.adaptations.keySet());
      this.pendingHits.keySet().forEach(k -> {
         if (!list.contains(k)) {
            list.add(k);
         }
      });
      list.removeIf(type -> {
         boolean isJJK = JJKCompat.isJJKKey(type);
         boolean isMelee = type.contains("_melee") || type.contains("mob_attack") || type.contains("player_attack");
         boolean isResist = type.contains("_resistance");
         boolean isOther = !isJJK && !isMelee && !isResist;
         if (this.categoryFilter == 1 && !isJJK) {
            return true;
         } else if (this.categoryFilter == 2 && !isMelee) {
            return true;
         } else {
            return this.categoryFilter == 3 && !isResist ? true : this.categoryFilter == 4 && !isOther;
         }
      });
      if (!this.searchText.isEmpty()) {
         list.removeIf(
            type -> {
               String name = JJKCompat.isJJKKey(type)
                  ? JJKCompat.getFriendlyName(type).toLowerCase()
                  : (type.contains(":") ? type.split(":")[1].toLowerCase() : type.toLowerCase());
               return !name.contains(this.searchText);
            }
         );
      }

      if (this.sortFilter == 0) {
         Collections.reverse(list);
      } else if (this.sortFilter == 1) {
         list.sort((a, b) -> Float.compare(this.adaptations.getOrDefault(b, 0.0F), this.adaptations.getOrDefault(a, 0.0F)));
      } else if (this.sortFilter == 2) {
         list.sort((a, b) -> {
            String nameA = JJKCompat.isJJKKey(a) ? JJKCompat.getFriendlyName(a) : (a.contains(":") ? a.split(":")[1] : a);
            String nameB = JJKCompat.isJJKKey(b) ? JJKCompat.getFriendlyName(b) : (b.contains(":") ? b.split(":")[1] : b);
            return nameA.compareToIgnoreCase(nameB);
         });
      }

      return list;
   }

   public boolean m_6050_(double mx, double my, double delta) {
      this.scrollOffset = (int)Math.max(0.0, Math.min(this.maxScroll, this.scrollOffset - delta * 10.0));
      return true;
   }

   private void raised(GuiGraphics g, int x, int y, int w, int h) {
      g.m_280509_(x, y, x + w, y + h, -3750202);
      g.m_280509_(x, y, x + w - 1, y + 1, -1);
      g.m_280509_(x, y, x + 1, y + h - 1, -1);
      g.m_280509_(x + 1, y + h - 2, x + w - 1, y + h - 1, -11184811);
      g.m_280509_(x + w - 2, y + 1, x + w - 1, y + h - 1, -11184811);
      g.m_280509_(x, y + h - 1, x + w, y + h, -13158601);
      g.m_280509_(x + w - 1, y, x + w, y + h, -13158601);
   }

   private void inset(GuiGraphics g, int x, int y, int w, int h) {
      g.m_280509_(x, y, x + w, y + h, -7631989);
      g.m_280509_(x, y, x + w, y + 1, -13158601);
      g.m_280509_(x, y, x + 1, y + h, -13158601);
      g.m_280509_(x + 1, y + h - 2, x + w, y + h - 1, -5592406);
      g.m_280509_(x + w - 1, y + 1, x + w, y + h - 1, -5592406);
      g.m_280509_(x, y + h - 1, x + w, y + h, -1);
      g.m_280509_(x + w - 1, y, x + w, y + h, -1);
   }

   public void m_88315_(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
      this.m_280273_(g);
      this.raised(g, this.left, this.top, 252, 234);
      g.m_280137_(this.f_96547_, "Wheel of Mahoraga", this.left + 126, this.top + 6, -12566464);
      g.m_280056_(this.f_96547_, "Adaptation Status", this.left + 10, this.top + 18, -11184811, false);
      g.m_280509_(this.left + 8, this.top + 60 - 8, this.left + 252 - 8, this.top + 60 - 7, -11184811);
      g.m_280509_(this.left + 8, this.top + 60 - 7, this.left + 252 - 8, this.top + 60 - 6, -1);
      if (this.searchBox != null) {
         this.searchBox.m_88315_(g, mouseX, mouseY, partialTick);
      }

      int pL = this.left + 7;
      int pR = this.left + 252 - 7;
      int pT = this.top + 60;
      int pB = this.top + 234 - 6;
      int pH = pB - pT;
      int iW = pR - pL;
      this.inset(g, pL, pT, iW, pH);
      List<String> types = this.getTypes();
      if (types.isEmpty()) {
         g.m_280137_(this.f_96547_, "No adaptations yet", this.left + 126, pT + pH / 2 - 4, -11184811);
      } else {
         g.m_280588_(pL + 1, pT + 1, pR - 1, pB - 1);

         for (int i = 0; i < types.size(); i++) {
            String type = types.get(i);
            float adapt = this.adaptations.getOrDefault(type, 0.0F);
            int hits = this.pendingHits.getOrDefault(type, 0);
            int cd = this.cooldowns.getOrDefault(type, 0);
            int pct = (int)(adapt * 100.0F);
            boolean isJJKEntry = JJKCompat.isJJKKey(type);
            boolean isDomainEntry = JJKCompat.isDomainKey(type);
            String name = isJJKEntry ? JJKCompat.getFriendlyName(type) : (type.contains(":") ? type.split(":")[1] : type);
            int ey = pT + 4 + i * 38 - this.scrollOffset;
            if (ey + 38 >= pT && ey <= pB) {
               int rowBg = i % 2 == 0 ? -6645094 : -7303024;
               g.m_280509_(pL + 1, ey, pR - 1, ey + 38 - 1, rowBg);
               int tx = pL + 8;
               int barW = iW - 22;
               int nameColor = type.contains("_resistance")
                  ? -5636096
                  : (type.contains("_melee") ? -3364352 : (isDomainEntry ? -12320666 : (isJJKEntry ? -16777094 : -15066598)));
               g.m_280056_(this.f_96547_, name, tx, ey + 5, nameColor, false);
               String pStr = pct + "%";
               int pCol = pct >= 100 ? -15042022 : (pct >= 50 ? -15066454 : (pct > 0 ? -8758784 : -11184811));
               g.m_280056_(this.f_96547_, pStr, pR - 10 - this.f_96547_.m_92895_(pStr), ey + 5, pCol, false);
               int barX = tx;
               int barY = ey + 17;
               this.inset(g, barX, barY, barW, 7);
               int filled = (int)(Math.max(0, barW - 2) * adapt);
               int fillCol = pct >= 100 ? -11154347 : (pct >= 50 ? -11171585 : (pct > 0 ? -13278 : -8947849));
               if (filled > 0) {
                  g.m_280509_(barX + 1, barY + 1, barX + 1 + filled, barY + 6, fillCol);
               }

               String hint;
               if (pct >= 100) {
                  hint = type.contains("_resistance") ? "Armor bypassed!" : "Full immunity!";
               } else if (cd > 0) {
                  hint = "Wait: " + (int)Math.ceil(cd / 20.0F) + "s / +10%";
               } else {
                  hint = "Inactive";
               }

               int hintCol = pct >= 100 ? -15042022 : (cd > 0 ? -7855582 : -13421773);
               g.m_280056_(this.f_96547_, hint, tx, ey + 28, hintCol, false);
            }
         }

         g.m_280618_();
         if (this.maxScroll > 0) {
            int totalContent = types.size() * 38 + 8;
            int sbX = pR - 7;
            int sbTY = pT + 2;
            int sbH = pH - 4;
            this.inset(g, sbX, sbTY, 5, sbH);
            int thumbH = Math.max(14, sbH * pH / totalContent);
            int thumbY = sbTY + (int)((float)(sbH - thumbH) * this.scrollOffset / this.maxScroll);
            this.raised(g, sbX, thumbY, 5, thumbH);
         }
      }

      super.m_88315_(g, mouseX, mouseY, partialTick);
   }

   public boolean m_7043_() {
      return false;
   }

   public boolean m_7933_(int keyCode, int scanCode, int modifiers) {
      if (KeyBinds.OPEN_MENU_KEY.m_90832_(keyCode, scanCode)) {
         this.m_7379_();
         return true;
      } else {
         return super.m_7933_(keyCode, scanCode, modifiers);
      }
   }
}
