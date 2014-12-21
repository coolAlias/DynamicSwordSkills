/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.ref;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import net.minecraft.util.MathHelper;
import net.minecraftforge.common.config.Configuration;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dynamicswordskills.skills.SkillBase;

public class Config
{
	/*================== GENERAL =====================*/
	/** Whether to use default movement controls to activate skills such as Dodge */
	private static boolean allowVanillaControls;
	/** Default swing speed (anti-left-click-spam): Sets base number of ticks between each left-click (0 to disable)[0-20] */
	private static int baseSwingSpeed;
	/** Whether Dodge and Parry require double-tap or not */
	private static boolean doubleTap;
	/** Whether auto-targeting is enabled or not */
	private static boolean autoTarget;
	/** Whether players can be targeted */
	private static boolean enablePlayerTarget;
	/** Number of combo hits to display */
	private static int hitsToDisplay;
	/** Whether all players should start with a Basic Skill orb */
	private static boolean enableBonusOrb;
	/** Weight for skill orbs when added to vanilla chest loot (0 to disable) [0-10] */
	private static int chestLootWeight;
	/** [Back Slice] Allow Back Slice to potentially knock off player armor */
	private static boolean allowDisarmorPlayer;
	/** [Combo HUD] Whether the combo hit counter will display by default (may be toggled in game) */
	private static boolean enableComboHud;
	/** [Parry] Bonus to disarm based on timing: tenths of a percent added per tick remaining on the timer [0-50] */
	private static float disarmTimingBonus;
	/** [Parry] Penalty to disarm chance: percent per Parry level of the opponent, default negates defender's skill bonus so disarm is based entirely on timing [0-20] */
	private static float disarmPenalty;
	/** [Skill Swords] Enable randomized Skill Swords to appear as loot in various chests */
	private static boolean enableRandomSkillSwords;
	/** [Skill Swords] Enable Skill Swords in the Creative Tab (iron only, as examples) */
	private static boolean enableCreativeSkillSwords;
	/** [Skill Swords] Skill level provided by the Creative Tab Skill Swords */
	private static int skillSwordLevel;
	/** [Super Spin Attack | Sword Beam] True to require a completely full health bar to use, or false to allow a small amount to be missing per level */
	private static boolean requireFullHealth;
	/** Enable use of a skill */
	private static boolean[] enableSkill;
	/*================== DROPS =====================*/
	/** [Player] Enable skill orbs to drop from players when killed in PvP */
	private static boolean enablePlayerDrops;
	/** [Player] Factor by which to multiply chance for skill orb to drop by slain players */
	private static int playerDropFactor;
	/** Enable skill orbs to drop as loot from mobs */
	private static boolean enableOrbDrops;
	/** Chance of dropping random orb */
	private static float randomDropChance;
	/** Chance for unmapped mob to drop an orb */
	private static float genericMobDropChance;
	/** Individual drop chances for skill orbs and heart pieces */
	private static Map<Byte, Float> orbDropChance;

	public static void init(FMLPreInitializationEvent event) {
		Configuration config = new Configuration(new File(event.getModConfigurationDirectory().getAbsolutePath() + ModInfo.CONFIG_PATH));
		config.load();
		
		/*================== GENERAL =====================*/
		allowVanillaControls = config.get("General", "Allow vanilla controls to activate skills", true).getBoolean(true);
		autoTarget = config.get("General", "Enable auto-targeting of next opponent", true).getBoolean(true);
		baseSwingSpeed = MathHelper.clamp_int(config.get("General", "Default swing speed (anti-left-click-spam): Sets base number of ticks between each left-click (0 to disable)[0-20]", 0).getInt(), 0, 20);
		enablePlayerTarget = config.get("General", "Enable targeting of players by default (can be toggled in game)", true).getBoolean(true);
		doubleTap = config.get("General", "Require double tap activation", true).getBoolean(true);
		hitsToDisplay = Math.max(0, config.get("General", "Max hits to display in Combo HUD [0-12]", 3).getInt());
		enableBonusOrb = config.get("General", "Whether all players should start with a Basic Skill orb", true).getBoolean(true);
		chestLootWeight = MathHelper.clamp_int(config.get("General", "Weight for skill orbs when added to vanilla chest loot (0 to disable) [0-10]", 1).getInt(), 0, 10);
		allowDisarmorPlayer = config.get("General", "[Back Slice] Allow Back Slice to potentially knock off player armor", true).getBoolean(true);
		enableComboHud = config.get("General", "[Combo HUD] Whether the combo hit counter will display by default (may be toggled in game)", true).getBoolean(true);
		disarmTimingBonus = 0.001F * (float) MathHelper.clamp_int(config.get("General", "[Parry] Bonus to disarm based on timing: tenths of a percent added per tick remaining on the timer [0-50]", 25).getInt(), 0, 50);
		disarmPenalty = 0.01F * (float) MathHelper.clamp_int(config.get("General", "[Parry] Penalty to disarm chance: percent per Parry level of the opponent, default negates defender's skill bonus so disarm is based entirely on timing [0-20]", 10).getInt(), 0, 20);
		enableRandomSkillSwords = config.get("General", "[Skill Swords] Enable randomized Skill Swords to appear as loot in various chests", true).getBoolean(true);
		enableCreativeSkillSwords = config.get("General", "[Skill Swords] Enable Skill Swords in the Creative Tab (iron only, as examples)", true).getBoolean(true);
		skillSwordLevel = MathHelper.clamp_int(config.get("General", "[Skill Swords] Skill level provided by the Creative Tab Skill Swords [1-5]", 3).getInt(), 1, 5);
		requireFullHealth = config.get("General", "[Super Spin Attack | Sword Beam] True to require a completely full health bar to use, or false to allow a small amount to be missing per level", false).getBoolean(false);
		
		String category = "enabledskills";
		config.addCustomCategoryComment(category,
				"Disabling a skill prevents players from learning or using that skill, but does not change the player\'s known skills."
				+ "\nSkill items previously generated as loot may be found but not used, and subsequent loot will not generate with that skill."
				+ "\nSkill orbs may still drop from mobs / players unless disabled separately, but may not be used."
				+ "\nThis setting is save-game safe: it may be disabled and re-enabled without affecting the saved game state.");
		enableSkill = new boolean[SkillBase.getNumSkills()];
		for (SkillBase skill : SkillBase.getSkills()) {
			enableSkill[skill.getId()] = config.get(category, "Enable use of the skill " + skill.getDisplayName(), true).getBoolean(true);
		}
		/*================== DROPS =====================*/
		enablePlayerDrops = config.get("Drops", "[Player] Enable skill orbs to drop from players when killed in PvP", true).getBoolean(true);
		playerDropFactor = MathHelper.clamp_int(config.get("Drops", "[Player] Factor by which to multiply chance for skill orb to drop by slain players [1-20]", 5).getInt(), 1, 20);
		enableOrbDrops = config.get("Drops", "Enable skill orbs to drop as loot from mobs (may still be disabled individually)", true).getBoolean(true);
		randomDropChance = 0.01F * (float) MathHelper.clamp_int(config.get("Drops", "Chance (as a percent) for specified mobs to drop a random orb [0-100]", 10).getInt(), 0, 100);
		genericMobDropChance = 0.01F * (float) MathHelper.clamp_int(config.get("Drops", "Chance (as a percent) for random mobs to drop a random orb [0-100]", 1).getInt(), 0, 100);
		orbDropChance = new HashMap<Byte, Float>(SkillBase.getNumSkills());
		for (SkillBase skill : SkillBase.getSkills()) {
			int i = MathHelper.clamp_int(config.get("Drops", "Chance (in tenths of a percent) for " + skill.getDisplayName() + " (0 to disable) [0-10]", 5).getInt(), 0, 10);
			orbDropChance.put(skill.getId(), (0.001F * (float) i));
		}
		config.save();
	}
	/*================== SKILLS =====================*/
	public static boolean giveBonusOrb() { return enableBonusOrb; }
	public static int getLootWeight() { return chestLootWeight; }
	public static boolean allowVanillaControls() { return allowVanillaControls; }
	public static int getBaseSwingSpeed() { return baseSwingSpeed; }
	public static boolean requiresDoubleTap() { return doubleTap; }
	public static boolean autoTargetEnabled() { return autoTarget; }
	public static boolean toggleAutoTarget() { autoTarget = !autoTarget; return autoTarget; }
	public static boolean canTargetPlayers() { return enablePlayerTarget; }
	public static boolean toggleTargetPlayers() { enablePlayerTarget = !enablePlayerTarget; return enablePlayerTarget; }
	public static boolean isComboHudEnabled() { return enableComboHud; }
	public static int getHitsToDisplay() { return hitsToDisplay; }
	public static boolean areRandomSwordsEnabled() { return enableRandomSkillSwords; }
	public static boolean areCreativeSwordsEnabled() { return enableCreativeSkillSwords; }
	public static boolean canDisarmorPlayers() { return allowDisarmorPlayer; }
	public static float getDisarmPenalty() { return disarmPenalty; }
	public static float getDisarmTimingBonus() { return disarmTimingBonus; }
	public static int getSkillSwordLevel() { return skillSwordLevel; }
	/** Returns amount of health that may be missing and still be able to activate certain skills (e.g. Sword Beam) */
	public static float getHealthAllowance(int level) {
		return (requireFullHealth ? 0.0F : (0.6F * level));
	}
	public static final boolean isSkillEnabled(byte id) { return (id > -1 && id < enableSkill.length ? enableSkill[id] : false); }
	/*================== DROPS =====================*/
	public static boolean arePlayerDropsEnabled() { return enablePlayerDrops; }
	public static float getPlayerDropFactor() { return playerDropFactor; }
	public static boolean areOrbDropsEnabled() { return enableOrbDrops; }
	public static float getChanceForRandomDrop() { return randomDropChance; }
	public static float getRandomMobDropChance() { return genericMobDropChance; }
	public static float getDropChance(int orbID) {
		return (orbDropChance.containsKey((byte) orbID) ? orbDropChance.get((byte) orbID) : 0.0F);
	}
}
