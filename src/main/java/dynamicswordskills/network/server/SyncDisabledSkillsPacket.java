/**
    Copyright (C) <2020> <coolAlias>

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

package dynamicswordskills.network.server;

import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractServerMessage;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Informs the server of user-disabled skills.
 *
 */
public class SyncDisabledSkillsPacket extends AbstractServerMessage<SyncDisabledSkillsPacket>
{
	private Set<Byte> disabledIds = Sets.<Byte>newHashSet();

	public SyncDisabledSkillsPacket() {}

	public SyncDisabledSkillsPacket(EntityPlayer player) {
		this.disabledIds = DSSPlayerInfo.get(player).getDisabledSkillIds();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		int n = buffer.readInt();
		for (int i = 0; i < n; ++i) {
			this.disabledIds.add(buffer.readByte());
		}
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeInt(this.disabledIds.size());
		for (Byte b : this.disabledIds) {
			buffer.writeByte(b);
		}
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		DSSPlayerInfo.get(player).setDisabledSkills(this.disabledIds);
	}
}
