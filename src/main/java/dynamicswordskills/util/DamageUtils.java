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

package dynamicswordskills.util;

import net.minecraft.entity.Entity;
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

	public static class DamageSourceArmorBreak extends EntityDamageSource {
		/** Creates an armor-bypassing physical DamageSource */
		public DamageSourceArmorBreak(String name, Entity entity) {
			super(name, entity);
			setDamageBypassesArmor();
		}
	}
	
	/**
	 * Returns an indirect sword-based DamageSource
	 * @param direct - entity directly responsible for causing the damage
	 * @param indirect - entity indirectly responsible, typically the player
	 */
	public static DamageSource causeIndirectSwordDamage(Entity direct, Entity indirect) {
		return new EntityDamageSourceIndirect(INDIRECT_SWORD, direct, indirect);
	}
}
