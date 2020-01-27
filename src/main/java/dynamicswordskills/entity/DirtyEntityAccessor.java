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

package dynamicswordskills.entity;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.DamageSource;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class DirtyEntityAccessor {

	/** Accessible reference to {@code EntityLivingBase#applyPotionDamageCalculations */
	private static Method applyPotionDamageCalculations;
	/** Accessible reference to {@code EntityLiving#experienceValue */
	private static Field experienceValue;
	/** Accessible reference to {@code PlayerControllerMP#syncCurrentPlayItem} */
	private static Method syncCurrentPlayItem;

	/**
	 * Returns the amount of damage the entity will receive after armor and potions are taken into account
	 */
	public static float getModifiedDamage(EntityLivingBase entity, DamageSource source, float amount) {
		if (applyPotionDamageCalculations == null) {
			applyPotionDamageCalculations = ReflectionHelper.findMethod(EntityLivingBase.class, entity, new String[]{"func_70672_c","applyPotionDamageCalculations"}, DamageSource.class, float.class);
		}
		// Don't want to actually damage the entity's armor at this point, so
		// reproduce parts of EntityLivingBase#applyArmorCalculations here:
		if (!source.isUnblockable()) {
			int armor = 25 - entity.getTotalArmorValue();
			amount = (amount * (float) armor) / 25.0F;
		}
		try {
			applyPotionDamageCalculations.invoke(entity, source, amount);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Math.max(amount - entity.getAbsorptionAmount(), 0.0F);
	}

	/** Sets or adds to the amount of xp the entity will drop when killed */
	public static void setLivingXp(EntityLiving entity, int xp, boolean add) {
		if (experienceValue == null) {
			experienceValue = ReflectionHelper.findField(EntityLiving.class, "field_70728_aV", "experienceValue");
		}
		try {
			int value = experienceValue.getInt(entity);
			experienceValue.set(entity, (add ? value + xp : xp));
		} catch (Exception e) {
			e.printStackTrace();
		}
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
