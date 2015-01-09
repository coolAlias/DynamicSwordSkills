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
import dynamicswordskills.util.LogHelper;

/**
 * 
 * Synchronizes all PlayerInfo data on the client
 *
 */
public class SyncPlayerInfoPacket implements IMessage
{
	/** NBTTagCompound used to store and transfer the Player's Info */
	private NBTTagCompound compound;

	/** Whether skills should validate; only false when skills reset */
	private boolean validate = true;

	public SyncPlayerInfoPacket() {}

	public SyncPlayerInfoPacket(DSSPlayerInfo info) {
		compound = new NBTTagCompound();
		info.saveNBTData(compound);
	}

	/**
	 * Sets validate to false for reset skills packets
	 */
	public SyncPlayerInfoPacket setReset() {
		validate = false;
		return this;
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		compound = ByteBufUtils.readTag(buffer);
		validate = buffer.readBoolean();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		ByteBufUtils.writeTag(buffer, compound);
		buffer.writeBoolean(validate);
	}

	public static class Handler extends AbstractClientMessageHandler<SyncPlayerInfoPacket> {
		@Override
		public IMessage handleClientMessage(EntityPlayer player, SyncPlayerInfoPacket msg, MessageContext ctx) {
			DSSPlayerInfo info = DSSPlayerInfo.get(player);
			if (info == null) {
				LogHelper.warn("Player's extended properties were NULL while trying to handle SyncPlayerInfo Packet!");
			} else {
				info.loadNBTData(msg.compound);
				if (msg.validate) {
					info.validateSkills();
				}
			}
			return null;
		}
	}
}
