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

package net.minecraft.entity;

import java.lang.reflect.Method;

import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.util.DamageSource;

public class DirtyEntityAccessor
{
	/** Accessible reference to {@code PlayerControllerMP#syncCurrentPlayItem} */
	private static Method syncCurrentPlayItem;

	/**
	 * Returns the amount of damage the entity will receive after armor and potions are taken into account
	 */
	public static float getModifiedDamage(EntityLivingBase entity, DamageSource source, float amount) {
		// Don't want to actually damage the entity's armor at this point, so
		// reproduce parts of EntityLivingBase#applyArmorCalculations here:
		if (!source.isUnblockable()) {
			int armor = 25 - entity.getTotalArmorValue();
			amount = (amount * (float) armor) / 25.0F;
		}
		amount = entity.applyPotionDamageCalculations(source, amount);
		return Math.max(amount - entity.getAbsorptionAmount(), 0.0F);
	}

	/** Sets or adds to the amount of xp the entity will drop when killed */
	public static void setLivingXp(EntityLiving entity, int xp, boolean add) {
		entity.experienceValue = (add ? entity.experienceValue + xp : xp);
	}

	/** Calls {@link PlayerControllerMP#syncCurrentPlayItem()} */
	@SideOnly(Side.CLIENT)
	public static void syncCurrentPlayItem(PlayerControllerMP player) {
		if (syncCurrentPlayItem == null) {
			syncCurrentPlayItem = ReflectionHelper.findMethod(PlayerControllerMP.class, player, new String[]{"func_78750_j","syncCurrentPlayItem"});
		}
		try {
			syncCurrentPlayItem.invoke(player);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
