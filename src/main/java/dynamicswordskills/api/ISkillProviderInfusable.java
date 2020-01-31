/**
    Copyright (C) <2019> <coolAlias>

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

package dynamicswordskills.api;

import dynamicswordskills.skills.SkillBase;
import net.minecraft.item.ItemStack;

/**
 * 
 * Interface for {@link ISkillProvider} Items that can be upgraded by infusing them with {@link ISkillInfusionFuelItem} items.
 * 
 */
public interface ISkillProviderInfusable extends ISkillProvider {

	/**
	 * Number of matching {@link ISkillInfusionFuelItem}s required to infuse the stack with the new or upgraded skill
	 * @param stack The {@link ISkillProviderInfusable} stack
	 * @param skill The skill to be infused
	 * @return 0 or less to prevent infusion, otherwise the number of items to be consumed [1 - 8]
	 */
	public int getInfusionCost(ItemStack stack, SkillBase skill);

	/**
	 * Infuse the ItemStack with the skill, usually increasing the level provided by 1.
	 * @param stack The {@link ISkillProviderInfusable} stack being infused
	 * @param skill The skill being infused into the stack
	 * @return The crafting result
	 */
	public ItemStack getInfusionResult(ItemStack stack, SkillBase skill);

}
