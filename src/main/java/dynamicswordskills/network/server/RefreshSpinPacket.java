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

package dynamicswordskills.network.server;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.SpinAttack;

/**
 * 
 * Packet to notify server that player is trying to add one more spin to SpinAttack.
 * 
 * It does not require any data to be sent other than the packet itself.
 *
 */
public class RefreshSpinPacket implements IMessage
{
	public RefreshSpinPacket() {}

	@Override
	public void fromBytes(ByteBuf buf) {}

	@Override
	public void toBytes(ByteBuf buf) {}

	public static class Handler extends AbstractServerMessageHandler<RefreshSpinPacket> {
		@Override
		public IMessage handleServerMessage(EntityPlayer player, RefreshSpinPacket msg, MessageContext ctx) {
			SkillActive skill = DSSPlayerInfo.get(player).getActiveSkill(SkillBase.spinAttack);
			if (skill instanceof SpinAttack && skill.isActive()) {
				((SpinAttack) skill).refreshServerSpin(player);
			}
			return null;
		}
	}
}
