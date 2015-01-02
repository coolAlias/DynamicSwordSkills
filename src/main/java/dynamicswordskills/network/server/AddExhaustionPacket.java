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

package dynamicswordskills.network.server;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;

/**
 * 
 * Used for skills that activate client-side only and need to add exhaustion to the player.
 *
 */
public class AddExhaustionPacket implements IMessage
{
	private float amount;

	public AddExhaustionPacket() {}

	public AddExhaustionPacket(float amount) {
		this.amount = amount;
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		this.amount = buffer.readFloat();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeFloat(amount);
	}

	public static class Handler extends AbstractServerMessageHandler<AddExhaustionPacket> {
		@Override
		public IMessage handleServerMessage(EntityPlayer player, AddExhaustionPacket message, MessageContext ctx) {
			player.addExhaustion(message.amount);
			return null;
		}
	}
}
