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

package dynamicswordskills;

import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.client.ComboOverlay;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.client.RenderNothing;
import dynamicswordskills.client.SoundHandler;
import dynamicswordskills.client.TargetingTickHandler;
import dynamicswordskills.entity.EntityLeapingBlow;

public class ClientProxy extends CommonProxy {

	@Override
	public void registerRenderers() {
		DSSKeyHandler.init();
		MinecraftForge.EVENT_BUS.register(new SoundHandler());
		MinecraftForge.EVENT_BUS.register(new ComboOverlay());
		MinecraftForge.EVENT_BUS.register(new DSSClientEvents());
		TickRegistry.registerTickHandler(new TargetingTickHandler(), Side.CLIENT);
		RenderingRegistry.registerEntityRenderingHandler(EntityLeapingBlow.class, new RenderNothing());
	}
}
