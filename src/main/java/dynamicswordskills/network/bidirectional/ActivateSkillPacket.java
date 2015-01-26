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

package dynamicswordskills.network.bidirectional;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage;
import dynamicswordskills.skills.SkillBase;

/**
 * 
 * Attempts to activate a skill for player. When activated on the server, a packet is automatically
 * sent to the client, so skills shouldn't be manually activated client side.
 *
 */
public class ActivateSkillPacket extends AbstractMessage<ActivateSkillPacket>
{
	/** If true, calls {@link DSSPlayerInfo#triggerSkill}, otherwise uses {@link DSSPlayerInfo#activateSkill} */
	private boolean wasTriggered = false;

	/** Skill to activate */
	private byte skillId;

	public ActivateSkillPacket() {}

	public ActivateSkillPacket(SkillBase skill) {
		this(skill, false);
	}

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
		if (wasTriggered) {
			DSSPlayerInfo.get(player).triggerSkill(player.worldObj, skillId);
		} else {
			DSSPlayerInfo.get(player).activateSkill(player.worldObj, skillId);
		}
	}
}
