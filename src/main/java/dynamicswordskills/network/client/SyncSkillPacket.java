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

package dynamicswordskills.network.client;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.fml.common.network.ByteBufUtils;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillBase;

/**
 * 
 * Synchronizes the client-side version of a skill with the server-side data.
 *
 */
public class SyncSkillPacket implements IMessage
{
	/** The ID of the skill to update */
	private byte id;

	/** Stores the skill's data */
	private NBTTagCompound compound;

	public SyncSkillPacket() {}

	/**
	 * Synchronizes the client side skill with the server skill's data
	 * @param skill A level 0 skill will be removed
	 */
	public SyncSkillPacket(SkillBase skill) {
		id = skill.getId();
		compound = new NBTTagCompound();
		skill.writeToNBT(compound);
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeByte(id);
		ByteBufUtils.writeTag(buffer, compound);
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		id = buffer.readByte();
		compound = ByteBufUtils.readTag(buffer);
	}

	public static class Handler extends AbstractClientMessageHandler<SyncSkillPacket> {
		@Override
		protected IMessage handleClientMessage(EntityPlayer player, SyncSkillPacket msg, MessageContext ctx) {
			DSSPlayerInfo.get(player).syncClientSideSkill(msg.id, msg.compound);
			return null;
		}
	}
}
