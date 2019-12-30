package dynamicswordskills.skills;

public abstract class BaseModSkill extends SkillActive
{
	/**
	 * Base active mod skill with standard tooltip entry
	 */
	public BaseModSkill(String translationKey) {
		super(translationKey);
		this.addTranslatedTooltip(getTranslationKey() + ".tooltip");
	}

	public BaseModSkill(SkillActive skill) {
		super(skill);
	}
}
