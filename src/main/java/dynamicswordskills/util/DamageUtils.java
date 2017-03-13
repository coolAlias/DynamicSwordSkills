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

package dynamicswordskills.util;

import dynamicswordskills.api.IComboDamage.IComboDamageFull;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EntityDamageSourceIndirect;

/**
 * 
 * A collection of methods and classes related to damage, such as new DamageSource types
 *
 */
public class DamageUtils
{
	public static final String
	/** Standard armor break damage string */
	ARMOR_BREAK = "armorBreak",
	/** Indirect damage caused by sword skills such as Leaping Blow */
	INDIRECT_SWORD = "indirectSword";

	/**
	 * Returns an armor-bypassing physical DamageSource
	 */
	public static DamageSource causeArmorBreakDamage(Entity entity) {
		return new DamageSourceArmorBreak(ARMOR_BREAK, entity);
	}

	public static class DamageSourceArmorBreak extends EntityDamageSource implements IComboDamageFull
	{
		private final boolean addHit;

		/** Creates an armor-bypassing physical DamageSource */
		public DamageSourceArmorBreak(String name, Entity direct) {
			this(name, direct, true);
		}

		/**
		 * Creates an armor-bypassing physical DamageSource
		 * @param addHit True to add the damage as its own hit, false to merge into previous combo damage
		 */
		public DamageSourceArmorBreak(String name, Entity direct, boolean addHit) {
			super(name, direct);
			this.addHit = addHit;
			setDamageBypassesArmor();
		}

		@Override
		public boolean isComboDamage(EntityPlayer player) {
			return true;
		}

		@Override
		public boolean increaseComboCount(EntityPlayer player) {
			return addHit;
		}

		@Override
		public boolean applyDamageToPrevious(EntityPlayer player) {
			return true; // already didn't add as its own hit, so merge with previous damage
		}

		@Override
		public boolean playDefaultSound(EntityPlayer player) {
			return true;
		}

		@Override
		public String getHitSound(EntityPlayer player) {
			return null;
		}
	}

	/**
	 * Returns an indirect sword-based DamageSource that does NOT add to the combo hit counter
	 * @param direct - entity directly responsible for causing the damage
	 * @param indirect - entity indirectly responsible, typically the player
	 */
	public static DamageSource causeIndirectSwordDamage(Entity direct, Entity indirect) {
		return new DamageSourceComboIndirect(INDIRECT_SWORD, direct, indirect, false);
	}

	/**
	 * Returns an indirect sword-based DamageSource that will count as a hit for combos
	 * @param direct - entity directly responsible for causing the damage
	 * @param indirect - entity indirectly responsible, typically the player
	 */
	public static DamageSource causeIndirectComboDamage(Entity direct, Entity indirect) {
		return new DamageSourceComboIndirect(INDIRECT_SWORD, direct, indirect, true);
	}

	public static class DamageSourceComboIndirect extends EntityDamageSourceIndirect implements IComboDamageFull
	{
		private final boolean increaseCount;
		private final boolean mergeDamage;

		public DamageSourceComboIndirect(String name, Entity direct, Entity indirect) {
			this(name, direct, indirect, true);
		}

		public DamageSourceComboIndirect(String name, Entity direct, Entity indirect, boolean increaseCount) {
			this(name, direct, indirect, increaseCount, false);
		}

		public DamageSourceComboIndirect(String name, Entity direct, Entity indirect, boolean increaseCount, boolean mergeDamage) {
			super(name, direct, indirect);
			this.increaseCount = increaseCount;
			this.mergeDamage = mergeDamage;
		}

		@Override
		public boolean isComboDamage(EntityPlayer player) {
			return true;
		}

		@Override
		public boolean increaseComboCount(EntityPlayer player) {
			return increaseCount;
		}

		@Override
		public boolean applyDamageToPrevious(EntityPlayer player) {
			return mergeDamage;
		}

		@Override
		public boolean playDefaultSound(EntityPlayer player) {
			return true;
		}

		@Override
		public String getHitSound(EntityPlayer player) {
			return null;
		}
	}
}
