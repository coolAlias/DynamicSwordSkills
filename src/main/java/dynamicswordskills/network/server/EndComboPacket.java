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

package dynamicswordskills.network.server;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractServerMessage;
import dynamicswordskills.skills.IComboSkill;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * This packet simply informs the server when an attack combo should be ended prematurely.
 * If a combo ends on the server side, the Combo class' own endCombo method should be used
 * directly instead of sending a packet.
 *
 */
public class EndComboPacket extends AbstractServerMessage<EndComboPacket>
{
	/** Id of skill that implements {@link IComboSkill} */
	private byte id;

	public EndComboPacket() {}

	public EndComboPacket(SkillBase skill) {
		this.id = skill.getId();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		id = buffer.readByte();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(id);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		SkillBase skill = DSSPlayerInfo.get(player).getPlayerSkill(SkillRegistry.getSkillById(id));
		if (skill instanceof IComboSkill) {
			if (((IComboSkill) skill).isComboInProgress()) {
				((IComboSkill) skill).getCombo().endCombo(player);
			}
		}
	}
}
