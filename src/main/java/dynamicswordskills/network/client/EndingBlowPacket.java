/**
    Copyright (C) <2019> <coolAlias>

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

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractClientMessage;
import dynamicswordskills.skills.EndingBlow;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Updates the client's EndingBlow skill result for EndingBlow HUD
 *
 */
public class EndingBlowPacket extends AbstractClientMessage<EndingBlowPacket>
{
	private byte result;

	public EndingBlowPacket() {}

	public EndingBlowPacket(byte result) {
		this.result = result;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		this.result = buffer.readByte();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(result);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		SkillBase skill = DSSPlayerInfo.get(player).getActiveSkill(Skills.endingBlow);
		if (skill instanceof EndingBlow) {
			((EndingBlow) skill).skillResult = this.result;
		}
	}
}
