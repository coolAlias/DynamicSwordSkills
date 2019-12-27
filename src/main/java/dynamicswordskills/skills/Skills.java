package dynamicswordskills.skills;

import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.util.ResourceLocation;

public class Skills
{
	public static final SkillBase swordBasic = new SwordBasic("swordbasic").addDescriptions(1).register("basic_technique");
	public static final SkillBase armorBreak = new ArmorBreak("armorbreak").addDescriptions(1).register("armor_break");
	public static final SkillBase dodge = new Dodge("dodge").addDescriptions(1).register("dodge");
	public static final SkillBase leapingBlow = new LeapingBlow("leapingblow").addDescriptions(1).register("leaping_blow");
	public static final SkillBase parry = new Parry("parry").addDescriptions(1).register("parry");
	public static final SkillBase dash = new Dash("dash").addDescriptions(1).register("dash");
	public static final SkillBase spinAttack = new SpinAttack("spinattack").addDescriptions(1).register("spin_attack");
	public static final SkillBase superSpinAttack = new SpinAttack("superspinattack").addDescriptions(1).register("super_spin_attack");
	public static final SkillBase mortalDraw = new MortalDraw("mortaldraw").addDescriptions(1).register("mortal_draw");
	public static final SkillBase swordBreak = new SwordBreak("swordbreak").addDescriptions(1).register("sword_break");
	public static final SkillBase risingCut = new RisingCut("risingcut").addDescriptions(1).register("rising_cut");
	public static final SkillBase endingBlow = new EndingBlow("endingblow").addDescriptions(1).register("ending_blow");
	public static final SkillBase backSlice = new BackSlice("backslice").addDescriptions(1).register("back_slice");
	public static final SkillBase swordBeam = new SwordBeam("swordbeam").addDescriptions(1).register("sword_beam");

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
}
