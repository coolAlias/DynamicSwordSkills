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
 * Interface for items that can be used to infuse {@link ISkillProviderInfusable} items.
 *
 */
public interface ISkillInfusionFuelItem
{
	/**
	 * Return the skill this item may bestow when infusing an {@link ISkillProviderInfusable} item
	 * @param stack The {@link ISkillInfusionFuelItem} ItemStack
	 */
	public SkillBase getSkillToInfuse(ItemStack stack);

	/**
	 * Provides control over which items may be used to infuse as well as the cost to do so
	 * @param fuel The skill item to be consumed, e.g. a skill orb
	 * @param base The base {@link ISkillProviderInfusable} item to be infused
	 * @param required The number of orbs to be consumed per {@link ISkillProviderInfusable#getInfusionCost(ItemStack, SkillBase)}
	 * @return the new value for required (up to 8), 0 or less to prevent infusion
	 */
	public int getAdjustedInfusionCost(ItemStack fuel, ItemStack base, int required);

}
