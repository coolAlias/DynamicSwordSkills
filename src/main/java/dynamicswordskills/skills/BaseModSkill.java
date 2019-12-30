package dynamicswordskills.skills;

public abstract class BaseModSkill extends SkillActive
{
	/**
	 * Base active mod skill with standard tooltip entry
	 */
	public BaseModSkill(String translationKey) {
		super(translationKey);
		this.addDescriptions(1);
	}

	public BaseModSkill(SkillActive skill) {
		super(skill);
	}
}
