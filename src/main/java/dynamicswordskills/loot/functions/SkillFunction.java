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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import dynamicswordskills.loot.conditions.SkillCondition;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

/**
 * 
 * Provides an immutable list of enabled skills from which to select.
 *
 */
public abstract class SkillFunction extends LootFunction
{
	/** List of skill ids that are enabled and allowed as loot */
	public static final List<Integer> SKILL_IDS;

	/**
	 * Creates the function with a single {@link SkillCondition} LootCondition.
	 */
	public SkillFunction() {
		this(new LootCondition[]{new SkillCondition()});
	}

	public SkillFunction(LootCondition[] conditions) {
		super(conditions);
	}

	static {
		List<Integer> ids = new ArrayList<Integer>();
		for (SkillBase skill : SkillBase.getSkills()) {
			if (Config.isSkillEnabled(skill.getId())) {
				ids.add((int) skill.getId());
			}
		}
		SKILL_IDS = Collections.unmodifiableList(ids);
	}
}
