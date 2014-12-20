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

package dynamicswordskills.network;

import java.io.IOException;

import net.minecraft.entity.player.EntityPlayer;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.LeapingBlow;
import dynamicswordskills.skills.SkillBase;

public class SpawnLeapingBlowPacket extends CustomPacket
{
	public SpawnLeapingBlowPacket() {}

	@Override
	public void write(ByteArrayDataOutput out) throws IOException {}

	@Override
	public void read(ByteArrayDataInput in) throws IOException {}

	@Override
	public void execute(EntityPlayer player, Side side) throws ProtocolException {
		if (side.isServer()) {
			if (DSSPlayerInfo.get(player) != null) {
				if (DSSPlayerInfo.get(player).hasSkill(SkillBase.leapingBlow)) {
					((LeapingBlow) DSSPlayerInfo.get(player).getPlayerSkill(SkillBase.leapingBlow)).spawnLeapingBlowEntity(player.worldObj, player);
				}
			} else {
				throw new ProtocolException("ZSSPlayerInfo is null while handling Spawn Leaping Blow Packet");
			}
		} else {
			throw new ProtocolException("Leaping Blow entity can only be spawned on the server");
		}
	}
}
