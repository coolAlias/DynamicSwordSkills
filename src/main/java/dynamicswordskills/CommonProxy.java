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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.client.GuiSkills;
import dynamicswordskills.inventory.ContainerSkills;
import dynamicswordskills.item.IModItem;

public class CommonProxy implements IGuiHandler
{
	/** Gui indices */
	public static final int GUI_SKILLS = 0;

	/**
	 * Registers all model / texture variants for items and blocks
	 * Call during FMLPreInitializationEvent after all Items and Blocks have been initialized
	 */
	public void registerVariants() {}

	/**
	 * Call during FMLInitializationEvent so that Minecraft's renderers are ready to use
	 */
	public void registerRenderers() {}

	/**
	 * Calls {@link IModItem#registerRenderer} for the item provided
	 */
	public void registerItemRenderer(IModItem item) {}
	
	/**
	 * Returns the current thread based on side during message handling,
	 * used for ensuring that the message is being handled by the main thread
	 */
	public IThreadListener getThreadFromContext(MessageContext ctx) {
		return ctx.getServerHandler().playerEntity.getServerForPlayer();
	}

	/**
	 * Returns a side-appropriate EntityPlayer for use during message handling
	 */
	public EntityPlayer getPlayerEntity(MessageContext ctx) {
		return ctx.getServerHandler().playerEntity;
	}

	@Override
	public Object getClientGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		switch(id) {
		case GUI_SKILLS:
			return new GuiSkills(player);
		}
		return null;
	}

	@Override
	public Object getServerGuiElement(int id, EntityPlayer player, World world, int x, int y, int z) {
		switch(id) {
		case GUI_SKILLS:
			return new ContainerSkills(player);
		}
		return null;
	}
}
