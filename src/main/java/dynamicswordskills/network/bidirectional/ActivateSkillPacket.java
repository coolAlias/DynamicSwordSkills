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

import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 
 * Attempts to activate a skill for player on whichever side the packet is received.
 *
 */
public class ActivateSkillPacket extends AbstractMessage<ActivateSkillPacket>
{
	private boolean wasTriggered = false;

	private byte skillId;

	public ActivateSkillPacket() {}

	/**
	 * See {@link DSSPlayerInfo#activateSkill(SkillBase, boolean)}
	 */
	public ActivateSkillPacket(SkillBase skill, boolean wasTriggered) {
		this.wasTriggered = wasTriggered;
		this.skillId = skill.getId();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		wasTriggered = buffer.readBoolean();
		skillId = buffer.readByte();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeBoolean(wasTriggered);
		buffer.writeByte(skillId);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		// handled identically on both sides
		DSSPlayerInfo info = DSSPlayerInfo.get(player);
		info.activateSkill(info.getPlayerSkill(skillId), wasTriggered);
	}
}
