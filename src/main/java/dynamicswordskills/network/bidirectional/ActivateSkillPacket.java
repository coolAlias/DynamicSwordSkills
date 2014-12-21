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
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillBase;

/**
 * 
 * Attempts to activate a skill for player. When activated on the server, a packet is automatically
 * sent to the client, so skills shouldn't be manually activated client side.
 *
 */
public class ActivateSkillPacket implements IMessage
{
	/** If true, calls triggerSkill(), otherwise uses activateSkill() */
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
	public void fromBytes(ByteBuf buffer) {
		wasTriggered = buffer.readBoolean();
		skillId = buffer.readByte();
	}

	@Override
	public void toBytes(ByteBuf buffer) {
		buffer.writeBoolean(wasTriggered);
		buffer.writeByte(skillId);
	}

	public static class Handler implements IMessageHandler<ActivateSkillPacket, IMessage> {
		@Override
		public IMessage onMessage(ActivateSkillPacket message, MessageContext ctx) {
			EntityPlayer player = DynamicSwordSkills.proxy.getPlayerEntity(ctx);
			if (DSSPlayerInfo.get(player) != null) {
				if (message.wasTriggered) {
					DSSPlayerInfo.get(player).triggerSkill(player.worldObj, message.skillId);
				} else {
					DSSPlayerInfo.get(player).activateSkill(player.worldObj, message.skillId);
				}
			}
			return null;
		}
	}
}
