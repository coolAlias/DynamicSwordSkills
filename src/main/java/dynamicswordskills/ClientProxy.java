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

package dynamicswordskills;

import java.lang.reflect.Field;

import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import cpw.mods.fml.relauncher.ReflectionHelper;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.client.RenderEntitySwordBeam;
import dynamicswordskills.client.RenderNothing;
import dynamicswordskills.client.TargetingTickHandler;
import dynamicswordskills.entity.EntityLeapingBlow;
import dynamicswordskills.entity.EntitySwordBeam;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.MinecraftForge;

public class ClientProxy extends CommonProxy
{
	/** Accessible reference to {@code Minecraft#debugFPS */
	private static Field debugFPS;

	@Override
	public void registerRenderers() {
		MinecraftForge.EVENT_BUS.register(new DSSClientEvents());
		FMLCommonHandler.instance().bus().register(new DSSKeyHandler());
		FMLCommonHandler.instance().bus().register(new TargetingTickHandler());
		RenderingRegistry.registerEntityRenderingHandler(EntityLeapingBlow.class, new RenderNothing());
		RenderingRegistry.registerEntityRenderingHandler(EntitySwordBeam.class, new RenderEntitySwordBeam());
	}

	@Override
	public EntityPlayer getPlayerEntity(MessageContext ctx) {
		return (ctx.side.isClient() ? Minecraft.getMinecraft().thePlayer : super.getPlayerEntity(ctx));
	}

	public int getDebugFPS() {
		if (debugFPS == null) {
			debugFPS = ReflectionHelper.findField(Minecraft.class, "field_71470_ab", "debugFPS");
		}
		try {
			return debugFPS.getInt(Minecraft.class);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return Minecraft.getMinecraft().getLimitFramerate();
	}
}
