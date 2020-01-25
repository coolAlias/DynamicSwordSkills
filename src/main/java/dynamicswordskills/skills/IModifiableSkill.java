package dynamicswordskills.skills;

import java.util.Set;

import dynamicswordskills.api.SkillRegistry;
import net.minecraft.entity.player.EntityPlayer;

public interface IModifiableSkill
{
	/**
	 * Use only skill instances retrieved from the {@link SkillRegistry}; the player's actual skill instance will be used as appropriate
	 * @param <T> Any skill modifier type
	 * @return set of skill modifiers that should be queried when appropriate to see if they should be applied; do not return null
	 */
	<T extends SkillBase & ISkillModifier> Set<T> getSkillModifiers();

	/**
	 * Called at any time to apply a modifier - it's up to the current skill to determine what
	 * effect, if any, the modifier should have based on the skill's internal state.
	 * @param <T> Any skill modifier type
	 * @param modifier The skill modifier instance requesting to be applied
	 * @param player The skill-using player
	 */
	<T extends SkillBase & ISkillModifier> void applySkillModifier(T modifier, EntityPlayer player);

}
