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

package dynamicswordskills.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.IThreadListener;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;

/**
 * 
 * Base message handler class using SimpleNetworkWrapper / IMessage framework;
 * its sole purpose is to call function based on side and pass a player object.
 * 
 */
public abstract class AbstractMessageHandler<T extends IMessage> implements IMessageHandler <T, IMessage>
{
	/**
	 * Allows reply message to be set during thread-checking
	 * (but handler classes are declared as static inner classes - sounds like trouble...)
	 */
	private IMessage reply;

	/**
	 * Handle a message received on the client side
	 * @return a message to send back to the Server, or null if no reply is necessary
	 */
	@SideOnly(Side.CLIENT)
	protected abstract IMessage handleClientMessage(EntityPlayer player, T msg, MessageContext ctx);

	/**
	 * Handle a message received on the server side
	 * @return a message to send back to the Client, or null if no reply is necessary
	 */
	protected abstract IMessage handleServerMessage(EntityPlayer player, T msg, MessageContext ctx);

	@Override
	public final IMessage onMessage(T msg, MessageContext ctx) {
		return checkThreadAndEnqueue(msg, this, ctx);
	}

	/**
	 * Passes the handling off to handleClientMessage or handleServerMessage, depending on side
	 */
	private final IMessage processMessage(T msg, MessageContext ctx) {
		EntityPlayer player = DynamicSwordSkills.proxy.getPlayerEntity(ctx);
		if (ctx.side.isClient()) {
			return handleClientMessage(player, msg, ctx);
		} else {
			return handleServerMessage(player, msg, ctx);
		}
	}

	/**
	 * Ensures that the message is being handled on the main thread
	 * @return Optional reply message - see {@link IMessageHandler#onMessage}
	 */
	private static final IMessage checkThreadAndEnqueue(final IMessage msg, final AbstractMessageHandler handler, final MessageContext ctx) {
		IThreadListener thread = DynamicSwordSkills.proxy.getThreadFromContext(ctx);
		if (!thread.isCallingFromMinecraftThread()) {
			thread.addScheduledTask(new Runnable() {
				public void run() {
					handler.reply = handler.processMessage(msg, ctx);
				}
			});
		}
		return handler.reply;
	}
}
