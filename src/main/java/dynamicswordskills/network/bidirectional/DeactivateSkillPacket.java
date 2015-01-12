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

package dynamicswordskills.network.bidirectional;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.LogHelper;

/**
 * 
 * Send to either side to {@link SkillActive#deactivate deactivate} a skill.
 *
 */
public class DeactivateSkillPacket implements IMessage
{
	private byte skillId;

	public DeactivateSkillPacket() {}

	public DeactivateSkillPacket(SkillActive skill) {
		this.skillId = skill.getId();
	}

	@Override
	public void fromBytes(ByteBuf buffer) {
		skillId = buffer.readByte();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeByte(skillId);
	}

	public static class Handler extends AbstractBiMessageHandler<DeactivateSkillPacket> {
		@Override
		protected IMessage handleMessage(EntityPlayer player, DeactivateSkillPacket msg, MessageContext ctx) {
			SkillBase skill = DSSPlayerInfo.get(player).getPlayerSkill(msg.skillId);
			if (skill instanceof SkillActive) {
				((SkillActive) skill).deactivate(player);
			} else {
				LogHelper.warn("Error processing DeactivateSkillPacket for " + player + "; skill with ID " + msg.skillId + " was not valid for this player.");
			}
			return null;
		}
	}
}
