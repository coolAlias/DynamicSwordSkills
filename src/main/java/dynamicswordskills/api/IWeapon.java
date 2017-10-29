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

package dynamicswordskills.api;

import dynamicswordskills.skills.LeapingBlow;
import dynamicswordskills.skills.MortalDraw;
import dynamicswordskills.skills.RisingCut;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.SwordBeam;
import net.minecraft.item.ItemStack;

/**
 * 
 * Items implementing this interface are always considered weapons and thus will
 * be compatible with most of the {@link SkillBase skills} by default.
 * 
 * If a skill's activation requires blocking, the item must be able to block or
 * it will not be able to activate such skills.
 * 
 * Some skills may only be performed while wielding a {@link #isSword sword}; these are:
 * {@link LeapingBlow}, {@link MortalDraw}, {@link RisingCut}, and {@link SwordBeam}.
 * 
 * For items that do not use NBT or stack damage, consider registering them as weapons
 * or as swords via the {@link WeaponRegistry} using FML's Inter-Mod Communications.
 *
 */
public interface IWeapon {

	/**
	 * Return true if the ItemStack is considered a sword.
	 * Consider returning !{@link WeaponRegistry#isSwordForbidden} to allow users to choose the item's sword status.
	 */
	boolean isSword(ItemStack stack);

	/**
	 * Return true if the ItemStack is considered a weapon
	 * (should return true if {@link #isSword} returns true)
	 * Consider returning !{@link WeaponRegistry#isWeaponForbidden} to allow users to choose the item's weapon status.
	 */
	boolean isWeapon(ItemStack stack);

}
