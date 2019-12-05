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

package dynamicswordskills.network.bidirectional;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Sets the player's left- or right-click action timer on either the client or the server
 *
 */
public class ActionTimePacket extends AbstractMessage<ActionTimePacket>
{
	private int ticks;

	private boolean isAttack;

	public ActionTimePacket() {}

	public ActionTimePacket(int ticks, boolean isAttack) {
		this.ticks = ticks;
		this.isAttack = isAttack;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		this.ticks = buffer.readInt();
		this.isAttack = buffer.readBoolean();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeInt(ticks);
		buffer.writeBoolean(isAttack);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		// handled identically on both sides
		if (isAttack) {
			DSSPlayerInfo.get(player).setAttackCooldown(ticks);
		} else {
			DSSPlayerInfo.get(player).setUseItemCooldown(ticks);
		}
	}
}
