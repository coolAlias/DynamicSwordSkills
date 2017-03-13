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

import java.util.Random;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ItemRandomSkill;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;

/**
 * 
 * Calls {@link ItemRandomSkill#getRandomSkillTag} and sets the ItemStack's
 * NBT Tag accordingly.
 *
 */
public class RandomSkillSword extends SkillFunction
{
	public RandomSkillSword() {
		super();
	}

	public RandomSkillSword(LootCondition[] conditions) {
		super(conditions);
	}

	@Override
	public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
		int i = SkillFunction.SKILL_IDS.get(MathHelper.getRandomIntegerInRange(rand, 0, SkillFunction.SKILL_IDS.size()));
		if (!(stack.getItem() instanceof ItemRandomSkill)) {
			DynamicSwordSkills.logger.warn("Invalid item for RandomSkillSword function: " + stack.toString());
		} else if (SkillBase.doesSkillExist(i)) {
			((ItemRandomSkill) stack.getItem()).generateSkillTag(stack, SkillBase.getSkill(i), rand);
		} else {
			DynamicSwordSkills.logger.warn("Skill with ID " + i + " does not exist");
		}
		return stack;
	}
}
