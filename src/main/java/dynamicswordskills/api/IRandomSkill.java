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

import java.util.Random;

import dynamicswordskills.skills.SkillBase;

/**
 * 
 * Interface for items that can generate with a randomized skill, such as
 * {@link IMetadataSkillItem} or {@link ItemRandomSkill}.
 *
 */
public interface IRandomSkill
{
	/**
	 * Return a random skill that is valid for this item, or null for none
	 */
	public SkillBase getRandomSkill(Random rand);

}
