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

package dynamicswordskills.loot.functions;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.loot.conditions.SkillCondition;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

/**
 * 
 * Provides an immutable list of enabled skills from which to select.
 *
 */
public abstract class SkillFunction extends LootFunction
{
	/** List of skills that are enabled and allowed as loot */
	private static final List<SkillBase> SKILLS;

	/** Skill to grant if not random */
	protected String skill_name;

	/**
	 * Creates the function with a single {@link SkillCondition} LootCondition.
	 */
	public SkillFunction() {
		this(new LootCondition[]{new SkillCondition()});
	}

	public SkillFunction(LootCondition[] conditions) {
		super(conditions);
	}

	public SkillFunction(LootCondition[] conditions, String skill_name) {
		super(conditions);
		this.skill_name = skill_name;
	}

	/**
	 * Returns the skill id of {@link #skill_name} if specified and valid
	 */
	protected SkillBase getSkill() {
		if (this.skill_name != null) {
			SkillBase skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(this.skill_name));
			if (skill == null) {
				DynamicSwordSkills.logger.warn("Unknown skill '" + this.skill_name + "' - a random skill may be selected instead.");
			} else if (!Config.isSkillAllowed(skill)) {
				DynamicSwordSkills.logger.warn(skill.getDisplayName() + " has been disabled in the Config settings; a random skill may be selected instead.");
			} else {
				return skill;
			}
		}
		return null;
	}

	/**
	 * Returns true if at least one skills is enabled
	 */
	public static boolean areSkillsEnabled() {
		return !SkillFunction.SKILLS.isEmpty();
	}

	/**
	 * Returns a random skill from among all enabled skills, possibly null
	 */
	public static SkillBase getRandomSkill(Random rand) {
		if (!SkillFunction.areSkillsEnabled()) {
			return null;
		}
		int i = MathHelper.getRandomIntegerInRange(rand, 0, SkillFunction.SKILLS.size() - 1);
		return SkillFunction.SKILLS.get(i);
	}

	static {
		SKILLS = SkillRegistry.getValues().stream().filter(s -> Config.isSkillAllowed(s)).collect(Collectors.toList());
	}
}
