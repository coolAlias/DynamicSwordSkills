/**
    Copyright (C) <2017> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.api.WeaponRegistry;
import dynamicswordskills.client.gui.IGuiOverlay.HALIGN;
import dynamicswordskills.client.gui.IGuiOverlay.VALIGN;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.client.SyncConfigPacket;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;

public class Config
{
	/** Config ID for GuiConfig */
	public static final String CONFIG_ID = "dss.config";
	public static Configuration config;
	/** Flag set after {@link #postInit()} has been called */
	private static boolean loaded;
	/*================== CLIENT SIDE SETTINGS =====================*/
	/* General client settings */
	private static boolean enableAdditionalControls;
	private static boolean enableAutoTarget;
	private static boolean enableTargetPassive;
	private static boolean enableTargetPlayer;
	private static boolean allowVanillaControls;
	private static boolean requireDoubleTap;
	private static boolean requireLockOn;
	/* Skill Manual GUI */
	private static boolean clickedGroupFilterSound;
	private static boolean clickedPageSound;
	private static boolean clickedSkillSound;
	private static boolean showBannedSkills;
	private static boolean showPaginationLabels;
	private static boolean showPlainTextIndex;
	private static boolean showSkillGroupTooltips;
	private static boolean showUnknownSkills;
	private static Map<String, Set<String>> skillGroupLists = Maps.<String, Set<String>>newHashMap();
	/* Combo HUD */
	public static int comboHudDisplayTime;
	private static int comboHudMaxHits;
	public static HALIGN comboHudXAlign;
	public static VALIGN comboHudYAlign;
	public static int comboHudXOffset;
	public static int comboHudYOffset;
	/* Ending Blow HUD */
	public static int endingBlowHudDisplayTime;
	public static boolean endingBlowHudResult;
	public static boolean endingBlowHudText;
	public static HALIGN endingBlowHudXAlign;
	public static VALIGN endingBlowHudYAlign;
	public static int endingBlowHudXOffset;
	public static int endingBlowHudYOffset;
	/*================== WEAPON REGISTRY =====================*/
	/** Items that are considered Swords for all intents and purposes */
	private static String[] swords = new String[0];
	/** Items that are considered Melee Weapons for all intents and purposes */
	private static String[] weapons = new String[0];
	/** Items that are forbidden from being considered as Swords */
	private static String[] forbidden_swords = new String[0];
	/** Items that are forbidden from being considered as Melee Weapons */
	private static String[] forbidden_weapons = new String[0];
	/*================== SERVER =====================*/
	/* General server settings */
	private static boolean backSliceDisarmorPlayer;
	private static Set<String> bannedSkills = Sets.<String>newHashSet();
	private static int baseSwingSpeed;
	private static float parryDisarmTimingBonus;
	private static float parryDisarmPenalty;
	private static boolean requireFullHealth;
	private static boolean risingCutHighJump;
	private static boolean skillSwordCreative;
	private static int skillSwordCreativeLevel;
	private static boolean skillSwordRandom;
	/* Loot / drops settings */
	private static boolean bonusOrbEnable;
	private static int orbLootWeight;
	private static Map<Integer, Float> orbDropChance;
	private static boolean orbDropEnable;
	private static float orbDropGeneralChance;
	private static float orbDropRandomChance;
	private static boolean playerDropEnable;
	private static int playerDropFactor;
	private static int skillSwordLootWeight;

	public static void init(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		refreshClient();
		refreshServer();
	}

	public static void refreshClient() {
		/* General client settings */
		enableAdditionalControls = config.get("client", "dss.config.client.enableAdditionalControls", false, "Enables additional WASD-equivalent keybindings for activating skills with e.g. a gamepad").setRequiresMcRestart(true).getBoolean(false);
		enableAutoTarget = config.get("client", "dss.config.client.enableAutoTarget", true, "Enable auto-targeting when locked on and the current target becomes invalid").getBoolean(true);
		enableTargetPassive = config.get("client", "dss.config.client.enableTargetPassive", true, "Allow targeting passive mobs with the lock-on mechanic").getBoolean(true);
		enableTargetPlayer = config.get("client", "dss.config.client.enableTargetPlayer", true, "Allow targeting players with the lock-on mechanic").getBoolean(true);
		allowVanillaControls = config.get("client", "dss.config.client.enableVanillaControls", true, "Allow vanilla movement keys to be used to activate skills; must be enabled if Additional Controls are disabled").getBoolean(true);
		if (!enableAdditionalControls && !allowVanillaControls) {
			DynamicSwordSkills.logger.warn("Both Vanilla and Additional Controls are disabled - Vanilla Controls were automatically enabled");
			allowVanillaControls = true;
		}
		requireDoubleTap = config.get("client", "dss.config.client.requireDoubleTap", true, "Require double-tap for Dodge and Parry (always required when Vanilla Controls are enabled)").getBoolean(true);
		requireLockOn = config.get("client", "dss.config.client.requireLockOn", false, "Require locking on to activate skills").getBoolean(false);
		/* Skill Manual GUI */
		clickedGroupFilterSound = config.get("skillgui", "dss.config.client.skillGui.clickedGroupFilterSound", true, "Play a sound when applying or removing a Skill Group filter").getBoolean(true);
		clickedPageSound = config.get("skillgui", "dss.config.client.skillGui.clickedPageSound", true, "Play a sound when the page index changes").getBoolean(true);
		clickedSkillSound = config.get("skillgui", "dss.config.client.skillGui.clickedSkillSound", true, "Play a sound when clicking on a Skill entry").getBoolean(true);
		showBannedSkills = config.get("skillgui", "dss.config.client.skillGui.showBannedSkills", false, "Display entries in the Skill Manual for skills disabled by the server").getBoolean(false);
		showPaginationLabels = config.get("skillgui", "dss.config.client.skillGui.showPaginationLabels", true, "Display text labels for 'Prev' and 'Next' page buttons").getBoolean(true);
		showPlainTextIndex = config.get("skillgui", "dss.config.client.skillGui.showPlainTextIndex", true, "Display table of contents without the standard button texture").getBoolean(true);
		showSkillGroupTooltips= config.get("skillgui", "dss.config.client.skillGui.showSkillGroupTooltips", true, "Display tooltips when hovering over the Table of Contents entries for Skill Groups that support them").getBoolean(true);
		showUnknownSkills = config.get("skillgui", "dss.config.client.skillGui.showUnknownSkills", true, "Display entries in the Skill Manual for skills not yet learned").getBoolean(true);
		if (Config.loaded) {
			refreshSkillGroups();
		}
		/* Combo HUD */
		String[] xalign = {"left", "center", "right"};
		String[] yalign = {"top", "center", "bottom"};
		comboHudDisplayTime = config.get("combohud", "dss.config.client.comboHud.displayTime", 5000, "Number of milliseconds Combo HUD will remain on screen (0 to disable)", 0, 20000).getInt();
		comboHudMaxHits = config.get("combohud", "dss.config.client.comboHud.maxHits", 3, "Maximum number of recent hits to display [0-12]", 0, 12).getInt();
		comboHudXAlign = HALIGN.fromString(config.get("combohud", "dss.config.client.comboHud.xalign", "left", "Base HUD alignment on the X-Axis").setValidValues(xalign).getString());
		comboHudXOffset = config.get("combohud", "dss.config.client.comboHud.xoffset", 0, "Number of pixels to offset HUD alignment on the X-Axis").getInt();
		comboHudYAlign = VALIGN.fromString(config.get("combohud", "dss.config.client.comboHud.yalign", "top", "Base HUD alignment on the Y-Axis").setValidValues(yalign).getString());
		comboHudYOffset = config.get("combohud", "dss.config.client.comboHud.yoffset", 0, "Number of pixels to offset HUD alignment on the Y-Axis").getInt();
		/* Ending Blow HUD */
		endingBlowHudDisplayTime = config.get("endingblowhud", "dss.config.client.endingBlowHud.displayTime", 1000, "Number of milliseconds Ending Blow HUD will remain on screen (0 to disable)", 0, 20000).getInt();
		endingBlowHudResult = config.get("endingblowhud", "dss.config.client.endingBlowHud.enableResultNotification", true, "Display success / failure notification when Ending Blow is used").getBoolean(true);
		endingBlowHudText = config.get("endingblowhud", "dss.config.client.endingBlowHud.enableText", false, "Display text instead of icons for Ending Blow notifications").getBoolean(false);
		endingBlowHudXAlign = HALIGN.fromString(config.get("endingblowhud", "dss.config.client.endingBlowHud.xalign", "center", "Base HUD alignment on the X-Axis").setValidValues(xalign).getString());
		endingBlowHudXOffset = config.get("endingblowhud", "dss.config.client.endingBlowHud.xoffset", 0, "Number of pixels to offset HUD alignment on the X-Axis").getInt();
		endingBlowHudYAlign = VALIGN.fromString(config.get("endingblowhud", "dss.config.client.endingBlowHud.yalign", "top", "Base HUD alignment on the Y-Axis").setValidValues(yalign).getString());
		endingBlowHudYOffset = config.get("endingblowhud", "dss.config.client.endingBlowHud.yoffset", 30, "Number of pixels to offset HUD alignment on the Y-Axis").getInt();
		if (config.hasChanged()) {
			config.save();
		}
	}

	public static void refreshServer() {
		/*================== WEAPON REGISTRY =====================*/
		swords = config.get("Weapon Registry", "[Allowed Swords] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", new String[0], "Register an item so that it is considered a SWORD by ZSS, i.e. it be used with skills that\nrequire swords, as well as other interactions that require swords, such as cutting grass.\nAll swords are also considered WEAPONS.").getStringList();
		Arrays.sort(swords);
		// Battlegear2 weapons ALL extend ItemSword, but are not really swords
		String[] forbidden = new String[]{
				"battlegear2:dagger.wood","battlegear2:dagger.stone","battlegear2:dagger.gold","battlegear2:dagger.iron","battlegear2:dagger.diamond",	
				"battlegear2:mace.wood","battlegear2:mace.stone","battlegear2:mace.gold","battlegear2:mace.iron","battlegear2:mace.diamond",
				"battlegear2:spear.wood","battlegear2:spear.stone","battlegear2:spear.gold","battlegear2:spear.iron","battlegear2:spear.diamond",
				"battlegear2:waraxe.wood","battlegear2:waraxe.stone","battlegear2:waraxe.gold","battlegear2:waraxe.iron","battlegear2:waraxe.diamond"
		};
		// Forbidden swords need to be added to the Allowed Weapons list or they can't use any skills at all 
		weapons = config.get("Weapon Registry", "[Allowed Weapons] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", forbidden, "Register an item as a generic melee WEAPON. This means it can be used for all\nskills except those that specifically require a sword, as well as some other things.").getStringList();
		Arrays.sort(weapons);
		forbidden_swords = config.get("Weapon Registry", "[Forbidden Swords] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", forbidden, "Forbid one or more items from acting as SWORDs, e.g. if a mod item extends ItemSword but is not really a sword - be sure to add it to the Allowed Weapons list if it should still be considered a weapon!").getStringList();
		Arrays.sort(forbidden_swords);
		forbidden_weapons = config.get("Weapon Registry", "[Forbidden Weapons] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", new String[0], "Forbid one or more items from acting as WEAPONs, e.g. if an item is added by IMC and you don't want it to be usable with skills.\nNote that this will also prevent the item from behaving as a SWORD.").getStringList();
		Arrays.sort(forbidden_weapons);
		/*================== SERVER =====================*/
		/* General server settings */
		backSliceDisarmorPlayer = config.get("general", "dss.config.server.general.backSliceDisarmorPlayer", true, "Allow Back Slice to potentially knock off player armor").getBoolean(true);
		String[] banned = config.get("general", "dss.config.server.general.bannedSkills", new String[0], "Enter the registry names for each skill disallowed on this server, each on a separate line between the '<' and '>'. Disabling a skill prevents players from using that skill, but does not change the player's known skills. Skill items previously generated as loot may be found but not used, and subsequent loot will not generate with that skill. Skill orb-like items may still drop from mobs / players unless disabled separately, but may not be used to learn the skill. This setting is save-game safe: skills may be disabled and re-enabled without affecting the saved game state.").setRequiresMcRestart(true).getStringList();
		bannedSkills.clear();
		bannedSkills.addAll(Lists.<String>newArrayList(banned));
		baseSwingSpeed = config.get("general", "dss.config.server.general.baseSwingSpeed", 0, "Default swing speed (anti-left-click-spam): Sets base number of ticks between each left-click (0 to disable)[0-20]", 0, 20).setRequiresWorldRestart(true).getInt();
		parryDisarmPenalty = 0.01F * (float)config.get("general", "dss.config.server.general.parryDisarmPenalty", 10, "[Parry] Penalty to disarm chance: percent per Parry level of the opponent, default negates defender's skill bonus so disarm is based entirely on timing [0-20]", 0, 20).getInt();
		parryDisarmTimingBonus = 0.001F * (float)config.get("general", "dss.config.server.general.parryDisarmTimingBonus", 25, "[Parry] Bonus to disarm based on timing: tenths of a percent added per tick remaining on the timer [0-50]", 0, 50).getInt();
		requireFullHealth = config.get("general", "dss.config.server.general.requireFullHealth", false, "True to require a completely full health bar to use Super Spin Attack and Sword Beam, or false to allow a small amount to be missing per level").setRequiresWorldRestart(true).getBoolean(false);
		risingCutHighJump = config.get("general", "dss.config.server.general.risingCutHighJump", false, "Allow the player to activate Rising Cut without hitting a target, i.e. perform a High Jump").getBoolean(false);
		skillSwordCreative = config.get("general", "dss.config.server.general.skillSwordCreative", true, "Enable Skill Swords in the Creative Tab (iron only, as examples)").setRequiresMcRestart(true).getBoolean(true);
		skillSwordCreativeLevel = config.get("general", "dss.config.server.general.skillSwordCreativeLevel", 3, "Skill level provided by the Creative Tab Skill Swords [1-5]", 1, 5).setRequiresMcRestart(true).getInt();
		skillSwordRandom = config.get("general", "dss.config.server.general.skillSwordRandom", true, "Enable randomized Skill Swords to add to loot or drop lists").setRequiresMcRestart(true).getBoolean(true);
		/* Loot / drops settings */
		bonusOrbEnable = config.get("drops", "dss.config.server.drops.bonusOrbEnable", false, "Whether all players should start with a Basic Skill orb").getBoolean(false);
		orbLootWeight = config.get("drops", "dss.config.server.drops.orbLootWeight", 1, "Weight for skill orbs when added to vanilla chest loot (0 to disable) [0-100]", 0, 100).setRequiresMcRestart(true).getInt();
		orbDropEnable = config.get("drops", "dss.config.server.drops.orbDropEnable", true, "Enable skill orbs to drop as loot from mobs (may still be disabled individually)").getBoolean(true);
		orbDropGeneralChance = 0.01F * (float)config.get("drops", "dss.config.server.drops.orbDropGeneralChance", 1, "Chance (as a percent) for generic mobs to drop a random skill orb [0-100]", 0, 100).getInt();
		orbDropRandomChance = 0.01F * (float)config.get("drops", "dss.config.server.drops.orbDropRandomChance", 10, "Chance (as a percent) for mobs with a specific skill orb drop to drop a random one instead [0-100]", 0, 100).getInt();
		orbDropChance = new HashMap<Integer, Float>(Skills.getSkillIdMap().size());
		for (Entry<Integer, ResourceLocation> entry : Skills.getSkillIdMap().entrySet()) {
			SkillBase skill = SkillRegistry.get(entry.getValue());
			int i = config.get("drops", "dss.config.server.drops.orbDropChance." + skill.getRegistryName().getResourcePath(), 5, "Chance (in tenths of a percent) for Skill Orb of " + skill.getDisplayName() + " to drop when available (0 to disable) [0-1000]", 0, 1000).getInt();
			orbDropChance.put((int)skill.getId(), (0.001F * (float) i));
		}
		playerDropEnable = config.get("drops", "dss.config.server.drops.playerDropEnable", true, "Enable skill orbs to drop from players when killed in PvP").getBoolean(true);
		playerDropFactor = config.get("drops", "dss.config.server.drops.playerDropFactor", 5, "Factor by which to multiply chance for skill orb to drop by slain players [1-20]", 1, 20).getInt();
		skillSwordLootWeight = config.get("drops", "dss.config.server.drops.skillSwordLootWeight", 1, "Weight for random skill swords when added to vanilla chest loot (0 to disable) [0-100]", 0, 100).setRequiresMcRestart(true).getInt();
		if (config.hasChanged()) {
			config.save();
		}
	}

	private static void refreshSkillGroups() {
		List<String> groupList = Lists.<String>newArrayList();
		for (SkillGroup group : SkillGroup.getAll()) {
			groupList.add(group.label);
		}
		String[] groups = config.get("skillgui", "dss.config.client.skillGui.skillGroups", groupList.toArray(new String[0]), "Enter desired Skill Group labels in the order you wish them to appear, each on a separate line between the '<' and '>'").getStringList();
		int i = groups.length;
		for (String label : groups) {
			SkillGroup group = new SkillGroup(label).setDisplayName(label).register();
			if (group == null) {
				continue;
			}
			group.priority = i--;
			// Skill List for this group
			List<String> skillList = Lists.<String>newArrayList();
			for (SkillBase skill : SkillRegistry.getValues()) {
				if (skill.displayInGroup(group)) {
					skillList.add(skill.getRegistryName().toString());
				}
			}
			String[] groupSkills = config.get("skillgrouplists", label, skillList.toArray(new String[0]), "Enter skill registry names for each skill you wish to appear in this category, each on a separate line between the '<' and '>'").getStringList();
			Set<String> set = Sets.newHashSet(groupSkills);
			skillGroupLists.put(group.label, set);
		}
	}

	public static void postInit() {
		WeaponRegistry.INSTANCE.registerItems(swords, "Config", true);
		WeaponRegistry.INSTANCE.registerItems(weapons, "Config", false);
		WeaponRegistry.INSTANCE.forbidItems(forbidden_swords, "Config", true);
		WeaponRegistry.INSTANCE.forbidItems(forbidden_weapons, "Config", false);
		refreshSkillGroups();
		Config.loaded = true;
		if (config.hasChanged()) {
			config.save();
		}
	}
	/*================== CLIENT SIDE SETTINGS =====================*/
	public static int getHitsToDisplay() { return comboHudMaxHits; }
	public static boolean allowVanillaControls() { return allowVanillaControls; }
	public static boolean enableAdditionalControls() { return enableAdditionalControls; }
	public static boolean requiresDoubleTap() { return requireDoubleTap; }
	public static boolean requiresLockOn() { return requireLockOn; }
	public static boolean autoTargetEnabled() { return enableAutoTarget; }
	public static boolean canTargetPassiveMobs() { return enableTargetPassive; }
	public static boolean canTargetPlayers() { return enableTargetPlayer; }
	/* Skill GUI */
	public static boolean clickedGroupFilterSound() { return clickedGroupFilterSound; }
	public static boolean clickedPageSound() { return clickedPageSound; }
	public static boolean clickedSkillSound() { return clickedSkillSound; }
	public static boolean showBannedSkills() { return showBannedSkills; }
	public static boolean showPaginationLabels() { return showPaginationLabels; }
	public static boolean showPlainTextIndex() { return showPlainTextIndex; }
	public static boolean showSkillGroupTooltips() { return showSkillGroupTooltips; }
	public static boolean showUnknownSkills() { return showUnknownSkills; }
	public static boolean isSkillInGroup(SkillBase skill, SkillGroup group) {
		if (skill.getRegistryName() == null) { return false; }
		Set<String> set = skillGroupLists.get(group.label);
		String name = skill.getRegistryName().toString();
		String alt = skill.getRegistryName().getResourceDomain() + ":*";
		return set != null && (set.contains(name) || set.contains(alt));
	}
	/*================== SKILLS =====================*/
	public static boolean giveBonusOrb() { return bonusOrbEnable; }
	public static int getOrbLootWeight() { return orbLootWeight; }
	public static int getBaseSwingSpeed() { return baseSwingSpeed; }
	public static boolean areRandomSwordsEnabled() { return skillSwordRandom; }
	public static boolean areCreativeSwordsEnabled() { return skillSwordCreative; }
	public static boolean canDisarmorPlayers() { return backSliceDisarmorPlayer; }
	public static float getDisarmPenalty() { return parryDisarmPenalty; }
	public static float getDisarmTimingBonus() { return parryDisarmTimingBonus; }
	public static boolean canHighJump() { return risingCutHighJump; }
	public static int getSkillSwordLevel() { return skillSwordCreativeLevel; }
	public static int getSkillSwordLootWeight() { return skillSwordLootWeight; }
	/** Returns amount of health that may be missing and still be able to activate certain skills (e.g. Sword Beam) */
	public static float getHealthAllowance(int level) {
		return (requireFullHealth ? 0.0F : (0.6F * level));
	}
	/** @return true if the skill has been disabled either by the server or client settings, or if it is null */
	public static final boolean isSkillDisabled(EntityPlayer player, @Nullable SkillBase skill) {
		return !Config.isSkillAllowed(skill) || DSSPlayerInfo.get(player).isSkillDisabled(skill);
	}
	/** @return true if the skill is allowed by the server, i.e. not banned */
	public static final boolean isSkillAllowed(@Nullable SkillBase skill) {
		return skill != null && skill.getRegistryName() != null && !bannedSkills.contains(skill.getRegistryName().toString());
	}
	/*================== DROPS =====================*/
	public static boolean arePlayerDropsEnabled() { return playerDropEnable; }
	public static float getPlayerDropFactor() { return playerDropFactor; }
	public static boolean areOrbDropsEnabled() { return orbDropEnable; }
	public static float getChanceForRandomDrop() { return orbDropRandomChance; }
	public static float getRandomMobDropChance() { return orbDropGeneralChance; }
	public static float getDropChance(int orbID) {
		return (orbDropChance.containsKey(orbID) ? orbDropChance.get(orbID) : 0.0F);
	}

	/**
	 * Updates client settings from server packet
	 */
	public static void syncClientSettings(SyncConfigPacket msg) {
		if (!msg.isMessageValid()) {
			DynamicSwordSkills.logger.error("Invalid SyncConfigPacket attempting to process!");
			return;
		}
		Config.baseSwingSpeed = msg.baseSwingSpeed;
		Config.requireFullHealth = msg.requireFullHealth;
		Config.bannedSkills.clear();
		for (Byte b : msg.disabledIds) {
			SkillBase skill = SkillRegistry.getSkillById(b);
			if (skill != null && skill.getRegistryName() != null) {
				Config.bannedSkills.add(skill.getRegistryName().toString());
			}
		}
	}
}
