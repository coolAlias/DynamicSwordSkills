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
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.LeapingBlow;
import dynamicswordskills.skills.SkillBase;

public class SpawnLeapingBlowPacket implements IMessage
{
	public SpawnLeapingBlowPacket() {}

	@Override
	public void fromBytes(ByteBuf buffer) {}

	@Override
	public void toBytes(ByteBuf buffer) {}

	public static class Handler extends AbstractServerMessageHandler<SpawnLeapingBlowPacket> {
		@Override
		public IMessage handleServerMessage(EntityPlayer player, SpawnLeapingBlowPacket message, MessageContext ctx) {
			DSSPlayerInfo props = DSSPlayerInfo.get(player);
			if (props != null && props.hasSkill(SkillBase.leapingBlow)) {
				((LeapingBlow) props.getPlayerSkill(SkillBase.leapingBlow)).spawnLeapingBlowEntity(player.worldObj, player);
			}
			return null;
		}
	}
}

/*
public class SpawnLeapingBlowPacket extends AbstractPacket
{
	public SpawnLeapingBlowPacket() {}

	public SpawnLeapingBlowPacket(boolean isMaster) {}

	@Override
	public void encodeInto(ChannelHandlerContext ctx, ByteBuf buffer) {}

	@Override
	public void decodeInto(ChannelHandlerContext ctx, ByteBuf buffer) {}

	@Override
	public void handleClientSide(EntityPlayer player) {}

	@Override
	public void handleServerSide(EntityPlayer player) {
		if (DSSPlayerInfo.get(player) != null && DSSPlayerInfo.get(player).hasSkill(SkillBase.leapingBlow)) {
			((LeapingBlow) DSSPlayerInfo.get(player).getPlayerSkill(SkillBase.leapingBlow)).spawnLeapingBlowEntity(player.worldObj, player);
		}
	}
}
 */
