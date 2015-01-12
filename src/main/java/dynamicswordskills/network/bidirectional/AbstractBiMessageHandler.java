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

package dynamicswordskills.network.bidirectional;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.network.AbstractMessageHandler;

/**
 * Handler for messages which can be sent to both sides.
 * 
 * If a message is handled identically on both sides, just override {@link #process};
 * otherwise, override both {@link #handleClientMessage} and {@link #handleServerMessage}
 */
public abstract class AbstractBiMessageHandler<T extends IMessage> extends AbstractMessageHandler<T>
{
	@Override
	protected IMessage handleClientMessage(EntityPlayer player, T msg, MessageContext ctx) {
		return handleMessage(player, msg, ctx);
	}

	@Override
	protected IMessage handleServerMessage(EntityPlayer player, T msg, MessageContext ctx) {
		return handleMessage(player, msg, ctx);
	}

	/**
	 * Called by both handleClientMessage and handleServerMessage unless they are overridden.
	 * Most useful for messages with identical handling on either side
	 * @return Reply message, if any
	 */
	protected IMessage handleMessage(EntityPlayer player, T msg, MessageContext ctx) {
		return null;
	}
}
