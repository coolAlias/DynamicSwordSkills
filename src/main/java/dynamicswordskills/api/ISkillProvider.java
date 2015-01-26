/**
    Copyright (C) <2015> <coolAlias>

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

import net.minecraft.item.ItemStack;

/**
 * 
 * Interface for Items that provide the player with the ability to use a certain
 * skill at a certain level, as though the player himself had the skill at that level
 * 
 * As an example, please see {@link ItemSkillProvider} or {@link ItemRandomSkill}.
 * Either of those can be extended directly as well, if desired.
 *
 */
public interface ISkillProvider extends ISkillItem {

	/**
	 * Returns the ID of the skill this Item provides; should return SkillBase.{skill}.id
	 * @return returning a negative value or invalid id will prevent the Item from granting a skill
	 * @param stack allows returning different values based on the ItemStack's data or damage
	 */
	public int getSkillId(ItemStack stack);

	/**
	 * Returns the skill level endowed by this item; automatically capped at the skill's max level
	 * @param stack allows returning different values based on the ItemStack's data or damage
	 * @return returning a value less than 1 will prevent the skill from being endowed
	 */
	public byte getSkillLevel(ItemStack stack);

	/**
	 * Return true to grant the player temporary basic sword skills when necessary to
	 * use the primary skill (i.e. the player has zero levels in basic sword skill)
	 * @param stack allows returning different values based on the ItemStack's data or damage
	 */
	public boolean grantsBasicSwordSkill(ItemStack stack);

}
