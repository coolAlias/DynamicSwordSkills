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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.network.AbstractMessageHandler;

/**
 * 
 * Handler for messages sent to the CLIENT
 * Only allows implementation of {@link AbstractMessageHandler#handleClientMessage handleClientMessage}
 * 
 */
public abstract class AbstractClientMessageHandler<T extends IMessage> extends AbstractMessageHandler<T> {
	@Override
	protected final IMessage handleServerMessage(EntityPlayer player, T msg, MessageContext ctx) {
		return null;
	}
}
