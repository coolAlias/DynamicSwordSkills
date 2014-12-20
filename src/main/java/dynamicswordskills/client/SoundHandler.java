/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
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

package dynamicswordskills.client;

import net.minecraftforge.client.event.sound.SoundLoadEvent;
import net.minecraftforge.event.ForgeSubscribe;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.lib.ModInfo;

@SideOnly(Side.CLIENT)
public class SoundHandler {
	
	@ForgeSubscribe
	public void onLoadSound(SoundLoadEvent event) {
		event.manager.addSound(ModInfo.SOUND_LEVELUP + ".ogg");
		event.manager.addSound(ModInfo.SOUND_SPECIAL_DROP + ".ogg");
		event.manager.addSound(ModInfo.SOUND_MORTALDRAW + "1.ogg");
		event.manager.addSound(ModInfo.SOUND_MORTALDRAW + "2.ogg");
		for (int i = 1; i < 4; ++i) {
			event.manager.addSound(ModInfo.SOUND_ARMORBREAK + String.valueOf(i) + ".ogg");
			event.manager.addSound(ModInfo.SOUND_HURT_FLESH + String.valueOf(i) + ".ogg");
			event.manager.addSound(ModInfo.SOUND_LEAPINGBLOW + String.valueOf(i) + ".ogg");
			event.manager.addSound(ModInfo.SOUND_SLAM + String.valueOf(i) + ".ogg");
			event.manager.addSound(ModInfo.SOUND_SPINATTACK + String.valueOf(i) + ".ogg");
		}
		for (int i = 1; i < 5; ++i) {
			event.manager.addSound(ModInfo.SOUND_SWORDCUT + String.valueOf(i) + ".ogg");
			event.manager.addSound(ModInfo.SOUND_SWORDMISS + String.valueOf(i) + ".ogg");
			event.manager.addSound(ModInfo.SOUND_SWORDSTRIKE + String.valueOf(i) + ".ogg");
		}
	}
}
