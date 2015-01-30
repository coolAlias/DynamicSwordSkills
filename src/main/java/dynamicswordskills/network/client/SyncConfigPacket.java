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

package dynamicswordskills.network.client;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.network.AbstractMessage.AbstractClientMessage;
import dynamicswordskills.ref.Config;

/**
 * 
 * Sent to each player as they log in to synchronize certain configuration settings.
 *
 */
public class SyncConfigPacket extends AbstractClientMessage<SyncConfigPacket>
{
	/** Processing calls a static method in Config, so use this field to indicate that it is a valid packet */
	private boolean isValid;
	public int baseSwingSpeed;
	public boolean requireFullHealth;

	public SyncConfigPacket() {}

	/**
	 * Returns whether packet is valid
	 */
	public final boolean isMessageValid() {
		return isValid;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		baseSwingSpeed = buffer.readInt();
		requireFullHealth = buffer.readBoolean();
		isValid = true;
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeInt(Config.getBaseSwingSpeed());
		buffer.writeBoolean(Config.getHealthAllowance(1) == 0.0F);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		Config.syncClientSettings(this);
	}
}
