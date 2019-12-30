package dynamicswordskills.skills;

import dynamicswordskills.ref.ModInfo;

public abstract class BaseModSkill extends SkillActive
{
	/** Base directory for skill icons */
	public static final String ICON_LOCATION = ModInfo.ID + ":textures/skills/";

	/**
	 * Base active mod skill with standard tooltip entry and icon location
	 * @param translationKey Assumed to match the icon file name; if that is not the case, call {@link #setIconLocation(String)} separately
	 */
	public BaseModSkill(String translationKey) {
		super(translationKey);
		this.addTranslatedTooltip(getTranslationKey() + ".tooltip");
		this.setIconLocation(ICON_LOCATION + translationKey + ".png");
	}

	public BaseModSkill(SkillActive skill) {
		super(skill);
	}
}
