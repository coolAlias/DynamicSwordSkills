/**
    Copyright (C) <2015> <coolAlias>

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

package dynamicswordskills.network;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.network.bidirectional.AttackTimePacket;
import dynamicswordskills.network.bidirectional.DeactivateSkillPacket;
import dynamicswordskills.network.bidirectional.PlaySoundPacket;
import dynamicswordskills.network.client.MortalDrawPacket;
import dynamicswordskills.network.client.SyncConfigPacket;
import dynamicswordskills.network.client.SyncPlayerInfoPacket;
import dynamicswordskills.network.client.SyncSkillPacket;
import dynamicswordskills.network.client.UpdateComboPacket;
import dynamicswordskills.network.server.AddExhaustionPacket;
import dynamicswordskills.network.server.DashImpactPacket;
import dynamicswordskills.network.server.EndComboPacket;
import dynamicswordskills.network.server.OpenGuiPacket;
import dynamicswordskills.network.server.RefreshSpinPacket;
import dynamicswordskills.network.server.TargetIdPacket;
import dynamicswordskills.ref.ModInfo;

/**
 * 
 * Houses the SimpleNetworkWrapper instance and provides wrapper methods for sending packets.
 *
 */
public class PacketDispatcher
{
	private static byte packetId = 0;

	private static final SimpleNetworkWrapper dispatcher = NetworkRegistry.INSTANCE.newSimpleChannel(ModInfo.CHANNEL);

	/**
	 *  Registers all packets and handlers - call this during {@code FMLPreInitializationEvent}
	 */
	public static final void initialize() {
		// Bidirectional packets
		registerMessage(ActivateSkillPacket.class);
		registerMessage(AttackTimePacket.class);
		registerMessage(DeactivateSkillPacket.class);
		registerMessage(PlaySoundPacket.class);

		// Packets handled on CLIENT
		registerMessage(MortalDrawPacket.class);
		registerMessage(SyncConfigPacket.class);
		registerMessage(SyncPlayerInfoPacket.class);
		registerMessage(SyncSkillPacket.class);
		registerMessage(UpdateComboPacket.class);

		// Packets handled on SERVER
		registerMessage(AddExhaustionPacket.class);
		registerMessage(DashImpactPacket.class);
		registerMessage(EndComboPacket.class);
		registerMessage(OpenGuiPacket.class);
		registerMessage(RefreshSpinPacket.class);
		registerMessage(TargetIdPacket.class);
	}

	/**
	 * Registers an AbstractMessage to the appropriate side(s)
	 */
	private static final <T extends AbstractMessage<T> & IMessageHandler<T, IMessage>> void registerMessage(Class<T> clazz) {
		if (AbstractMessage.AbstractClientMessage.class.isAssignableFrom(clazz)) {
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId++, Side.CLIENT);
		} else if (AbstractMessage.AbstractServerMessage.class.isAssignableFrom(clazz)) {
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId++, Side.SERVER);
		} else {
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId, Side.CLIENT);
			PacketDispatcher.dispatcher.registerMessage(clazz, clazz, packetId++, Side.SERVER);
		}
	}

	/**
	 * Send this message to the specified player.
	 * See {@link SimpleNetworkWrapper#sendTo(IMessage, EntityPlayerMP)}
	 */
	public static final void sendTo(IMessage message, EntityPlayerMP player) {
		PacketDispatcher.dispatcher.sendTo(message, player);
	}

	/**
	 * Send this message to everyone.
	 * See {@link SimpleNetworkWrapper#sendToAll(IMessage)}
	 */
	public static void sendToAll(IMessage message) {
		PacketDispatcher.dispatcher.sendToAll(message);
	}

	/**
	 * Send this message to everyone within a certain range of a point.
	 * See {@link SimpleNetworkWrapper#sendToDimension(IMessage, NetworkRegistry.TargetPoint)}
	 */
	public static final void sendToAllAround(IMessage message, NetworkRegistry.TargetPoint point) {
		PacketDispatcher.dispatcher.sendToAllAround(message, point);
	}

	/**
	 * Sends a message to everyone within a certain range of the coordinates in the same dimension.
	 */
	public static final void sendToAllAround(IMessage message, int dimension, double x, double y, double z, double range) {
		PacketDispatcher.sendToAllAround(message, new NetworkRegistry.TargetPoint(dimension, x, y, z, range));
	}

	/**
	 * Sends a message to everyone within a certain range of the player provided.
	 */
	public static final void sendToAllAround(IMessage message, EntityPlayer player, double range) {
		PacketDispatcher.sendToAllAround(message, player.worldObj.provider.dimensionId, player.posX, player.posY, player.posZ, range);
	}

	/**
	 * Send this message to everyone within the supplied dimension.
	 * See {@link SimpleNetworkWrapper#sendToDimension(IMessage, int)}
	 */
	public static final void sendToDimension(IMessage message, int dimensionId) {
		PacketDispatcher.dispatcher.sendToDimension(message, dimensionId);
	}

	/**
	 * Send this message to the server.
	 * See {@link SimpleNetworkWrapper#sendToServer(IMessage)}
	 */
	public static final void sendToServer(IMessage message) {
		PacketDispatcher.dispatcher.sendToServer(message);
	}
}
