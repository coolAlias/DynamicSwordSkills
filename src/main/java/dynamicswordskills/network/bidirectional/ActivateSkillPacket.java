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
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Attempts to activate a skill for player on whichever side the packet is received.
 *
 */
public class ActivateSkillPacket extends AbstractMessage<ActivateSkillPacket>
{
	private byte id;

	private boolean wasTriggered = false;

	public ActivateSkillPacket() {}

	/**
	 * See {@link DSSPlayerInfo#activateSkill(SkillBase, boolean)}
	 */
	public ActivateSkillPacket(SkillBase skill, boolean wasTriggered) {
		this.id = skill.getId();
		this.wasTriggered = wasTriggered;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		id = buffer.readByte();
		wasTriggered = buffer.readBoolean();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(id);
		buffer.writeBoolean(wasTriggered);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		// handled identically on both sides
		DSSPlayerInfo info = DSSPlayerInfo.get(player);
		SkillBase skill = info.getPlayerSkill(SkillRegistry.getSkillById(id));
		if (skill instanceof SkillActive) {
			info.activateSkill(skill, wasTriggered);
		} else {
			DynamicSwordSkills.logger.warn(String.format("Skill ID %d was not valid for %s while processing ActivateSkillPacket", id, player));
		}
	}
}
