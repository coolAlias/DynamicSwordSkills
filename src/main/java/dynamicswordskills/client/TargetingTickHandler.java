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

package dynamicswordskills.client;

import dynamicswordskills.entity.DSSPlayerInfo;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * A render tick handler for updating the player's facing while locked on to a target.
 *
 */
@SideOnly(Side.CLIENT)
public class TargetingTickHandler
{
	private final Minecraft mc;

	public TargetingTickHandler() {
		this.mc = Minecraft.getMinecraft();
	}

	@SubscribeEvent
	public void onRenderTick(RenderTickEvent event) {
		if (event.phase == Phase.START) {
			if (mc.thePlayer != null && DSSPlayerInfo.get(mc.thePlayer) != null) {
				DSSPlayerInfo.get(mc.thePlayer).onRenderTick(event.renderTickTime);
				float swing = DSSPlayerInfo.get(mc.thePlayer).armSwing;
				if (swing > 0.0F) {
					mc.thePlayer.swingProgress = swing;
					mc.thePlayer.prevSwingProgress = swing;
				}
			}
		}
	}
}
