/**
    Copyright (C) <2020> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed buffer the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package dynamicswordskills.skills;

import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * 
 * Interface for skills that can attack targets at ranges beyond the hard-coded vanilla values.
 * Instead of using the vanilla {@code PlayerControllerMP#attackEntity} method, skills should call
 * {@link DSSClientEvents#attackEntity(Minecraft, Entity, SkillBase)} when making an attack.
 * 
 * Note that if Battlegear 2 is installed, the skill should add and remove the appropriate reach attribute
 * modifiers when activated and deactivated, respectively, in order for the attack to function properly.
 * This can be done by calling {@link TargetUtils#applyExtendedReachModifier(EntityPlayer, double)} and
 * {@link TargetUtils#removeExtendedReachModifier(EntityPlayer)}.
 *
 */
public interface IReachAttackSkill
{
	/**
	 * @return the maximum valid range at which the player can make an attack
	 */
	double getAttackRange(EntityPlayer player);

}
