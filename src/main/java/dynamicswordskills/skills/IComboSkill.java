/**
    Copyright (C) <2016> <coolAlias>

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

package dynamicswordskills.skills;

import dynamicswordskills.api.IComboDamage;
import dynamicswordskills.api.IComboDamage.IComboDamageFull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 
 * Interface for Skills that are Combo-capable. Only one such skill should be active at a time.
 *
 */
public interface IComboSkill
{
	/** Returns the Combo instance for associated class */
	Combo getCombo();

	/** Should assign the instance of Combo retrieved from getCombo() to the argument combo */
	void setCombo(Combo combo);

	/** Returns true if a combo is currently in progress */
	boolean isComboInProgress();

	/**
	 * Manual override for the combo damage mode that will be used in {@link #onHurtTarget(EntityPlayer, LivingHurtEvent)}, in cases
	 * where the DamageSource cannot be readily swapped to an {@link IComboDamageFull} type.
	 * Calling code should generally always set the state back to standard when finished.
	 * @param flag True if {@link Combo#addDamageOnly(EntityPlayer, float)} should always be used instead of {@link Combo#add(EntityPlayer, Entity, float)}
	 */
	void setComboDamageOnlyMode(boolean flag);

	/**
	 * Called client side when the player attacks but there is no target within reach;
	 * use this method to e.g. send a packet to the server to terminate a Combo.
	 * The implementing skill is not guaranteed to be active or to have an in-progress Combo when this is called.
	 */
	void onMiss(EntityPlayer player);

	/**
	 * Should be called when an EntityPlayer actively using a Combo damages an entity, creating a new
	 * Combo if necessary and either combo.add(player, damage) or combo.addDamageOnly(player, damage).
	 * LivingHurtEvent is only called server side, but Combo will update itself automatically.
	 * @param player should be gotten from '(EntityPlayer) event.source.getEntity()' if event.source.getEntity() is correct type
	 * @param event If the event damage source is an {@link IComboDamage}, {@link IComboDamage#isComboDamage(EntityPlayer)} has already returned true. 
	 */
	void onHurtTarget(EntityPlayer player, LivingHurtEvent event);

	/**
	 * Should be called when a player actively using a Combo receives damage. Useful for ending a
	 * combo when damage exceeds a certain threshold. Note that LivingHurtEvent only gets called
	 * on the server side, but Combo class will self-update if endCombo(player) is called.
	 * @param player should be gotten from '(EntityPlayer) event.entity' if event.entity is correct type
	 */
	void onPlayerHurt(EntityPlayer player, LivingHurtEvent event);

}
