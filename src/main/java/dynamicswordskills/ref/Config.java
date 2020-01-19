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

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.api.WeaponRegistry;
import dynamicswordskills.client.gui.IGuiOverlay.HALIGN;
import dynamicswordskills.client.gui.IGuiOverlay.VALIGN;
import dynamicswordskills.network.client.SyncConfigPacket;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.common.config.Property;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

public class Config
{
	public static Configuration config;
	/*================== CLIENT SIDE SETTINGS =====================*/
	/* General client settings */
	private static boolean enableAdditionalControls;
	private static boolean enableAutoTarget;
	private static boolean enableTargetPassive;
	private static boolean enableTargetPlayer;
	private static boolean allowVanillaControls;
	private static boolean requireDoubleTap;
	private static boolean requireLockOn;
	private static Set<String> deactivatedSkills = Sets.<String>newHashSet();
	/* Skill Manual GUI */
	private static boolean clickedGroupFilterSound;
	private static boolean clickedPageSound;
	private static boolean clickedSkillSound;
	private static boolean showBannedSkills;
	private static boolean showOneGroupPerPage;
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
	/*================== GENERAL =====================*/
	/** [SYNC] Default swing speed (anti-left-click-spam): Sets base number of ticks between each left-click (0 to disable)[0-20] */
	private static int baseSwingSpeed;
	/** Whether all players should start with a Basic Skill orb */
	private static boolean enableBonusOrb;
	/** Weight for skill orbs when added to vanilla chest loot (0 to disable) [0-10] */
	private static int chestLootWeight;
	/** [Back Slice] Allow Back Slice to potentially knock off player armor */
	private static boolean allowDisarmorPlayer;
	/** [Parry] Bonus to disarm based on timing: tenths of a percent added per tick remaining on the timer [0-50] */
	private static float disarmTimingBonus;
	/** [Parry] Penalty to disarm chance: percent per Parry level of the opponent, default negates defender's skill bonus so disarm is based entirely on timing [0-20] */
	private static float disarmPenalty;
	/** [Rising Cut] Allow the player to activate Rising Cut without hitting a target, i.e. perform a High Jump */
	private static boolean enableHighJump;
	/** [Skill Swords] Enable randomized Skill Swords to appear as loot in various chests */
	private static boolean enableRandomSkillSwords;
	/** [Skill Swords] Enable Skill Swords in the Creative Tab (iron only, as examples) */
	private static boolean enableCreativeSkillSwords;
	/** [Skill Swords] Skill level provided by the Creative Tab Skill Swords */
	private static int skillSwordLevel;
	/** [Skill Swords][Super Spin Attack] Require player to have at least one level in Spin Attack to perform extra spins using a skill item */
	private static boolean requireSpinAttack;
	/** [SYNC] [Super Spin Attack | Sword Beam] True to require a completely full health bar to use, or false to allow a small amount to be missing per level */
	private static boolean requireFullHealth;
	/** Skills not allowed on this server */
	private static Set<String> bannedSkills = Sets.<String>newHashSet();
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
	private static Map<Integer, Float> orbDropChance;

	public static void init(FMLPreInitializationEvent event) {
		config = new Configuration(event.getSuggestedConfigurationFile());
		config.load();
		refreshClient();
		refreshServer();
	}

	private static Property getDeactivatedSkillsProperty() {
		return config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.deactivatedSkills", new String[0], "Deactivating a skill on the client prevents it from being activated only for that client. This can be used to disable a skill temporarily, perhaps to swap between skills with the same activation key sequence. Enter the registry names for each skill to deactivate, each on a separate line between the '<' and '>'");
	}

	public static void refreshClient() {
		/* General client settings */
		config.addCustomCategoryComment(Configuration.CATEGORY_CLIENT, "This category contains client side settings; i.e. they are not synchronized with the server.");
		enableAdditionalControls = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.enableAdditionalControls", false, "Enables additional WASD-equivalent keybindings for activating skills with e.g. a gamepad").setRequiresMcRestart(true).getBoolean(false);
		enableAutoTarget = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.enableAutoTarget", true, "Enable auto-targeting when locked on and the current target becomes invalid").getBoolean(true);
		enableTargetPassive = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.enableTargetPassive", true, "Allow targeting passive mobs with the lock-on mechanic").getBoolean(true);
		enableTargetPlayer = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.enableTargetPlayer", true, "Allow targeting players with the lock-on mechanic").getBoolean(true);
		allowVanillaControls = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.enableVanillaControls", true, "Allow vanilla movement keys to be used to activate skills; must be enabled if Additional Controls are disabled").getBoolean(true);
		if (!enableAdditionalControls && !allowVanillaControls) {
			DynamicSwordSkills.logger.warn("Both Vanilla and Additional Controls are disabled - Vanilla Controls were automatically enabled");
			allowVanillaControls = true;
		}
		requireDoubleTap = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.requireDoubleTap", true, "Require double-tap for Dodge and Parry (always required when Vanilla Controls are enabled)").getBoolean(true);
		requireLockOn = config.get(Configuration.CATEGORY_CLIENT, "dss.config.client.requireLockOn", false, "Require locking on to activate skills").getBoolean(false);
		String[] deactivated = getDeactivatedSkillsProperty().getStringList();
		deactivatedSkills.clear();
		deactivatedSkills.addAll(Lists.<String>newArrayList(deactivated));
		/* Skill Manual GUI */
		clickedGroupFilterSound = config.get("skillgui", "dss.config.client.skillGui.clickedGroupFilterSound", true, "Play a sound when applying or removing a Skill Group filter").getBoolean(true);
		clickedPageSound = config.get("skillgui", "dss.config.client.skillGui.clickedPageSound", true, "Play a sound when the page index changes").getBoolean(true);
		clickedSkillSound = config.get("skillgui", "dss.config.client.skillGui.clickedSkillSound", true, "Play a sound when clicking on a Skill entry").getBoolean(true);
		showBannedSkills = config.get("skillgui", "dss.config.client.skillGui.showBannedSkills", false, "Display entries in the Skill Manual for skills disabled by the server").getBoolean(false);
		showOneGroupPerPage= config.get("skillgui", "dss.config.client.skillGui.showOneGroupPerPage", true, "Display all skills in a group on a single page (enables scrolling)").getBoolean(true);
		showPaginationLabels = config.get("skillgui", "dss.config.client.skillGui.showPaginationLabels", true, "Display text labels for 'Prev' and 'Next' page buttons").getBoolean(true);
		showPlainTextIndex = config.get("skillgui", "dss.config.client.skillGui.showPlainTextIndex", true, "Display table of contents without the standard button texture").getBoolean(true);
		showSkillGroupTooltips= config.get("skillgui", "dss.config.client.skillGui.showSkillGroupTooltips", true, "Display tooltips when hovering over the Table of Contents entries for Skill Groups that support them").getBoolean(true);
		showUnknownSkills = config.get("skillgui", "dss.config.client.skillGui.showUnknownSkills", true, "Display entries in the Skill Manual for skills not yet learned").getBoolean(true);
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
		weapons = config.get("Weapon Registry", "[Allowed Weapons] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", new String[0], "Register an item as a generic melee WEAPON. This means it can be used for all\nskills except those that specifically require a sword, as well as some other things.").getStringList();
		Arrays.sort(weapons);
		// Battlegear2 weapons ALL extend ItemSword, but are not really swords
		String[] forbidden = new String[]{
				"battlegear2:dagger.wood","battlegear2:dagger.stone","battlegear2:dagger.gold","battlegear2:dagger.iron","battlegear2:dagger.diamond",	
				"battlegear2:mace.wood","battlegear2:mace.stone","battlegear2:mace.gold","battlegear2:mace.iron","battlegear2:mace.diamond",
				"battlegear2:spear.wood","battlegear2:spear.stone","battlegear2:spear.gold","battlegear2:spear.iron","battlegear2:spear.diamond",
				"battlegear2:waraxe.wood","battlegear2:waraxe.stone","battlegear2:waraxe.gold","battlegear2:waraxe.iron","battlegear2:waraxe.diamond"
		};
		forbidden_swords = config.get("Weapon Registry", "[Forbidden Swords] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", forbidden, "Forbid one or more items from acting as SWORDs, e.g. if a mod item extends ItemSword but is not really a sword").getStringList();
		Arrays.sort(forbidden_swords);
		forbidden_weapons = config.get("Weapon Registry", "[Forbidden Weapons] Enter items as modid:registered_item_name, each on a separate line between the '<' and '>'", new String[0], "Forbid one or more items from acting as WEAPONs, e.g. if an item is added by IMC and you don't want it to be usable with skills.\nNote that this will also prevent the item from behaving as a SWORD.").getStringList();
		Arrays.sort(forbidden_weapons);
		/*================== GENERAL =====================*/
		baseSwingSpeed = MathHelper.clamp_int(config.get("general", "Default swing speed (anti-left-click-spam): Sets base number of ticks between each left-click (0 to disable)[0-20]", 0).getInt(), 0, 20);
		enableBonusOrb = config.get("general", "Whether all players should start with a Basic Skill orb", true).getBoolean(true);
		chestLootWeight = MathHelper.clamp_int(config.get("general", "Weight for skill orbs when added to vanilla chest loot (0 to disable) [0-10]", 1).getInt(), 0, 10);
		allowDisarmorPlayer = config.get("general", "[Back Slice] Allow Back Slice to potentially knock off player armor", true).getBoolean(true);
		disarmTimingBonus = 0.001F * (float) MathHelper.clamp_int(config.get("general", "[Parry] Bonus to disarm based on timing: tenths of a percent added per tick remaining on the timer [0-50]", 25).getInt(), 0, 50);
		disarmPenalty = 0.01F * (float) MathHelper.clamp_int(config.get("general", "[Parry] Penalty to disarm chance: percent per Parry level of the opponent, default negates defender's skill bonus so disarm is based entirely on timing [0-20]", 10).getInt(), 0, 20);
		enableHighJump = config.get("general", "[Rising Cut] Allow the player to activate Rising Cut without hitting a target, i.e. perform a High Jump", false).getBoolean(false);
		enableRandomSkillSwords = config.get("general", "[Skill Swords] Enable randomized Skill Swords to appear as loot in various chests", true).getBoolean(true);
		enableCreativeSkillSwords = config.get("general", "[Skill Swords] Enable Skill Swords in the Creative Tab (iron only, as examples)", true).getBoolean(true);
		skillSwordLevel = MathHelper.clamp_int(config.get("general", "[Skill Swords] Skill level provided by the Creative Tab Skill Swords [1-5]", 3).getInt(), 1, 5);
		requireSpinAttack = config.get("general", "[Skill Swords][Super Spin Attack] Require player to have at least one level in Spin Attack to perform extra spins using a skill item", false).getBoolean(false);
		requireFullHealth = config.get("general", "[Super Spin Attack | Sword Beam] True to require a completely full health bar to use, or false to allow a small amount to be missing per level", false).getBoolean(false);
		config.addCustomCategoryComment("general.bannedskills",
				"Disabling a skill on the server prevents players from using that skill, but does not change the player\'s known skills."
				+ "\nSkill items previously generated as loot may be found but not used, and subsequent loot will not generate with that skill."
				+ "\nSkill orb-like items may still drop from mobs / players unless disabled separately, but may not be used to learn the skill."
				+ "\nThis setting is save-game safe: skills may be disabled and re-enabled without affecting the saved game state.");
		String[] banned = config.get("general.bannedskills", "bannedSkills", new String[0], "Enter the registry names for each skill disallowed on this server, each on a separate line between the '<' and '>'").getStringList();
		bannedSkills.clear();
		bannedSkills.addAll(Lists.<String>newArrayList(banned));
		/*================== DROPS =====================*/
		enablePlayerDrops = config.get("drops", "[Player] Enable skill orbs to drop from players when killed in PvP", true).getBoolean(true);
		playerDropFactor = MathHelper.clamp_int(config.get("drops", "[Player] Factor by which to multiply chance for skill orb to drop by slain players [1-20]", 5).getInt(), 1, 20);
		enableOrbDrops = config.get("drops", "Enable skill orbs to drop as loot from mobs (may still be disabled individually)", true).getBoolean(true);
		randomDropChance = 0.01F * (float) MathHelper.clamp_int(config.get("drops", "Chance (as a percent) for specified mobs to drop a random orb [0-100]", 10).getInt(), 0, 100);
		genericMobDropChance = 0.01F * (float) MathHelper.clamp_int(config.get("drops", "Chance (as a percent) for random mobs to drop a random orb [0-100]", 1).getInt(), 0, 100);
		orbDropChance = new HashMap<Integer, Float>(Skills.getSkillIdMap().size());
		for (Entry<Integer, ResourceLocation> entry : Skills.getSkillIdMap().entrySet()) {
			SkillBase skill = SkillRegistry.get(entry.getValue());
			int i = MathHelper.clamp_int(config.get("drops", "Chance (in tenths of a percent) for " + skill.getDisplayName() + " (0 to disable) [0-10]", 5).getInt(), 0, 10);
			orbDropChance.put((int)skill.getId(), (0.001F * (float) i));
		}
		if (config.hasChanged()) {
			config.save();
		}
	}

	public static void postInit() {
		WeaponRegistry.INSTANCE.registerItems(swords, "Config", true);
		WeaponRegistry.INSTANCE.registerItems(weapons, "Config", false);
		WeaponRegistry.INSTANCE.forbidItems(forbidden_swords, "Config", true);
		WeaponRegistry.INSTANCE.forbidItems(forbidden_weapons, "Config", false);
		/*===================== SKILLS =====================*/
		// Skill Groups
		List<String> groupList = Lists.<String>newArrayList();
		for (SkillGroup group : SkillGroup.getAll()) {
			groupList.add(group.label);
		}
		String[] groups = config.get("skillGroups", "skillGroups", groupList.toArray(new String[0]), "Enter desired Skill Groups in the order you wish them to appear, each on a separate line between the '<' and '>'").getStringList();
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
			String[] groupSkills = config.get("skillGroupSkills", label, skillList.toArray(new String[0]), "Enter skill registry names for each skill you wish to appear in this category, each on a separate line between the '<' and '>'").getStringList();
			Set<String> set = Sets.newHashSet(groupSkills);
			skillGroupLists.put(group.label, set);
		}
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
	public static boolean showOneGroupPerPage() { return showOneGroupPerPage; }
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
	public static boolean giveBonusOrb() { return enableBonusOrb; }
	public static int getLootWeight() { return chestLootWeight; }
	public static int getBaseSwingSpeed() { return baseSwingSpeed; }
	public static boolean areRandomSwordsEnabled() { return enableRandomSkillSwords; }
	public static boolean areCreativeSwordsEnabled() { return enableCreativeSkillSwords; }
	public static boolean canDisarmorPlayers() { return allowDisarmorPlayer; }
	public static float getDisarmPenalty() { return disarmPenalty; }
	public static float getDisarmTimingBonus() { return disarmTimingBonus; }
	public static boolean canHighJump() { return enableHighJump; }
	public static int getSkillSwordLevel() { return skillSwordLevel; }
	public static boolean isSpinAttackRequired() { return requireSpinAttack; }
	/** Returns amount of health that may be missing and still be able to activate certain skills (e.g. Sword Beam) */
	public static float getHealthAllowance(int level) {
		return (requireFullHealth ? 0.0F : (0.6F * level));
	}
	/**
	 * Toggles the skill's client-side deactivation state
	 * @param skill
	 * @return current deactivation state
	 */
	public static final boolean toggleDeactivatedSkill(SkillBase skill) {
		String s = skill.getRegistryName().toString();
		if (deactivatedSkills.contains(s)) {
			deactivatedSkills.remove(s);
			return false;
		}
		deactivatedSkills.add(s);
		return true;
	}
	/** Saves changes made to the {@link #deactivatedSkills} list */
	public static void updateDeactivatedSkills() {
		getDeactivatedSkillsProperty().set(deactivatedSkills.toArray(new String[0]));
		if (Config.config.hasChanged()) {
			Config.config.save();
		}
	}
	/** @return true if the skill has been disabled either by the server or client settings */
	public static final boolean isSkillDisabled(@Nullable SkillBase skill) {
		if (skill == null || skill.getRegistryName() == null) {
			return true;
		}
		String s = skill.getRegistryName().toString();
		return bannedSkills.contains(s) || deactivatedSkills.contains(s);
	}
	/** @return true if the skill is allowed by the client, i.e. not deactivated */
	public static final boolean isSkillActive(@Nullable SkillBase skill) {
		return skill != null && skill.getRegistryName() != null && !deactivatedSkills.contains(skill.getRegistryName().toString());
	}
	/** @return true if the skill is allowed by the server, i.e. not banned */
	public static final boolean isSkillAllowed(@Nullable SkillBase skill) {
		return skill != null && skill.getRegistryName() != null && !bannedSkills.contains(skill.getRegistryName().toString());
	}
	/*================== DROPS =====================*/
	public static boolean arePlayerDropsEnabled() { return enablePlayerDrops; }
	public static float getPlayerDropFactor() { return playerDropFactor; }
	public static boolean areOrbDropsEnabled() { return enableOrbDrops; }
	public static float getChanceForRandomDrop() { return randomDropChance; }
	public static float getRandomMobDropChance() { return genericMobDropChance; }
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
