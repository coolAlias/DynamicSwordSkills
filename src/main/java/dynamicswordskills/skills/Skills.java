package dynamicswordskills.skills;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.util.ResourceLocation;

public class Skills
{
	/** Base directory for skill icons */
	public static final String ICON_LOCATION = ModInfo.ID + ":textures/skills/";
	// Skill Groups
	public static final SkillGroup DEFAULT_GROUP = new SkillGroup(ModInfo.ID, -1).register();
	public static final SkillGroup SWORD_GROUP = new SkillGroup("sword", 1).setHasTooltip().register();
	public static final SkillGroup TARGETED_GROUP = new SkillGroup("targeted", 0).setHasTooltip().register();
	public static final SkillGroup WEAPON_GROUP = new SkillGroup("weapon", 1).setHasTooltip().register();
	// Skills
	public static final SkillBase swordBasic = new SwordBasic("basic_technique").setIconLocation(ICON_LOCATION + "basic_technique.png").addDefaultTooltip().register("basic_technique");
	public static final SkillBase armorBreak = new ArmorBreak("armor_break").setIconLocation(ICON_LOCATION + "armor_break.png").addDefaultTooltip().register("armor_break");
	public static final SkillBase dodge = new Dodge("dodge").setIconLocation(ICON_LOCATION + "dodge.png").addDefaultTooltip().register("dodge");
	public static final SkillBase leapingBlow = new LeapingBlow("leaping_blow").setIconLocation(ICON_LOCATION + "leaping_blow.png").addDefaultTooltip().register("leaping_blow");
	public static final SkillBase parry = new Parry("parry").setIconLocation(ICON_LOCATION + "parry.png").addDefaultTooltip().register("parry");
	public static final SkillBase dash = new Dash("dash").setIconLocation(ICON_LOCATION + "dash.png").addDefaultTooltip().register("dash");
	public static final SkillBase spinAttack = new SpinAttack("spin_attack").setIconLocation(ICON_LOCATION + "basic_technique.png").addDefaultTooltip().register("spin_attack");
	public static final SkillBase superSpinAttack = new SuperSpinAttack("super_spin_attack").setIconLocation(ICON_LOCATION + "super_spin_attack.png").addDefaultTooltip().register("super_spin_attack");
	public static final SkillBase mortalDraw = new MortalDraw("mortal_draw").setIconLocation(ICON_LOCATION + "mortal_draw.png").addDefaultTooltip().register("mortal_draw");
	public static final SkillBase swordBreak = new SwordBreak("sword_break").setIconLocation(ICON_LOCATION + "sword_break.png").addDefaultTooltip().register("sword_break");
	public static final SkillBase risingCut = new RisingCut("rising_cut").setIconLocation(ICON_LOCATION + "rising_cut.png").addDefaultTooltip().register("rising_cut");
	public static final SkillBase endingBlow = new EndingBlow("ending_blow").setIconLocation(ICON_LOCATION + "ending_blow.png").addDefaultTooltip().register("ending_blow");
	public static final SkillBase backSlice = new BackSlice("back_slice").setIconLocation(ICON_LOCATION + "back_slice.png").addDefaultTooltip().register("back_slice");
	public static final SkillBase swordBeam = new SwordBeam("sword_beam").setIconLocation(ICON_LOCATION + "sword_beam.png").addDefaultTooltip().register("sword_beam");

	public static void init() {
		// Skills are registered during declaration
		registerRemaps();
	}

	private static void registerRemaps() {
		remap("armorbreak", "armor_break");
		remap("backslice", "back_slice");
		remap("endingblow", "ending_blow");
		remap("leapingblow", "leaping_blow");
		remap("mortaldraw", "mortal_draw");
		remap("risingcut", "rising_cut");
		remap("spinattack", "spin_attack");
		remap("superspinattack", "super_spin_attack");
		remap("swordbasic", "basic_technique");
		remap("swordbeam", "sword_beam");
		remap("swordbreak", "sword_break");
	}

	private static void remap(String _old, String _new) {
		SkillRegistry.remap(new ResourceLocation(ModInfo.ID, _old), new ResourceLocation(ModInfo.ID, _new));
	}

	private static BiMap<Integer, ResourceLocation> skills_map = null;

	/**
	 * Returns a BiMap of historic ID values to skill registry names
	 */
	public static BiMap<Integer, ResourceLocation> getSkillIdMap() {
		if (Skills.skills_map != null) {
			return skills_map;
		}
		int i = 0;
		skills_map = HashBiMap.create(16);
		skills_map.put(i++, Skills.swordBasic.getRegistryName());
		skills_map.put(i++, Skills.armorBreak.getRegistryName());
		skills_map.put(i++, Skills.dodge.getRegistryName());
		skills_map.put(i++, Skills.leapingBlow.getRegistryName());
		skills_map.put(i++, Skills.parry.getRegistryName());
		skills_map.put(i++, Skills.dash.getRegistryName());
		skills_map.put(i++, Skills.spinAttack.getRegistryName());
		skills_map.put(i++, Skills.superSpinAttack.getRegistryName());
		skills_map.put(i++, Skills.mortalDraw.getRegistryName());
		skills_map.put(i++, Skills.swordBreak.getRegistryName());
		skills_map.put(i++, Skills.risingCut.getRegistryName());
		skills_map.put(i++, Skills.endingBlow.getRegistryName());
		skills_map.put(i++, Skills.backSlice.getRegistryName());
		skills_map.put(i++, Skills.swordBeam.getRegistryName());
		return skills_map;
	}
}
