package com.example.examplemod;

import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameRules.Category;
import net.minecraft.world.level.GameRules.IntegerValue;
import net.minecraft.world.level.GameRules.Key;

public class GameRuleTest {
   public static final Key<IntegerValue> MAHORAGA_COOLDOWN = GameRules.m_46189_("mahoragaCooldown", Category.PLAYER, IntegerValue.m_46312_(50));
}
