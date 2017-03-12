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

package dynamicswordskills.ref;

import net.minecraft.init.Bootstrap;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;

public class ModSounds
{
	public static final SoundEvent ARMOR_BREAK;
	public static final SoundEvent HURT_FLESH;
	public static final SoundEvent LEAPING_BLOW;
	public static final SoundEvent LEVEL_UP;
	public static final SoundEvent MORTAL_DRAW;
	public static final SoundEvent SLAM;
	public static final SoundEvent SPECIAL_DROP;
	public static final SoundEvent SPIN_ATTACK;
	public static final SoundEvent SWORD_CUT;
	public static final SoundEvent SWORD_MISS;
	public static final SoundEvent SWORD_STRIKE;
	public static final SoundEvent WHOOSH;

	private static SoundEvent getSound(String name) {
		ResourceLocation location = new ResourceLocation(ModInfo.ID, name);
		SoundEvent sound = (SoundEvent) SoundEvent.REGISTRY.getObject(location);
		if (sound == null) {
			throw new IllegalStateException("Invalid Sound requested: " + location.toString());
		}
		return sound;
	}

	static
	{
		if (!Bootstrap.isRegistered()) {
			throw new RuntimeException("Accessed Sounds before Bootstrap!");
		}
		ARMOR_BREAK = getSound("armor_break");
		HURT_FLESH = getSound("hurt_flesh");
		LEAPING_BLOW = getSound("leaping_blow");
		LEVEL_UP = getSound("level_up");
		MORTAL_DRAW = getSound("mortal_draw");
		SLAM = getSound("slam");
		SPECIAL_DROP = getSound("special_drop");
		SPIN_ATTACK = getSound("spin_attack");
		SWORD_CUT = getSound("sword_cut");
		SWORD_MISS = getSound("sword_miss");
		SWORD_STRIKE = getSound("sword_strike");
		WHOOSH = getSound("whoosh");
	}
}
