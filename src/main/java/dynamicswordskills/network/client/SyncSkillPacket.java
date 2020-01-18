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

package dynamicswordskills.network.client;

import java.io.IOException;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractClientMessage;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Synchronizes the client-side version of a skill with the server-side data.
 *
 */
public class SyncSkillPacket extends AbstractClientMessage<SyncSkillPacket>
{
	/** Stores the skill's data */
	private NBTTagCompound compound;

	public SyncSkillPacket() {}

	/**
	 * Synchronizes the client side skill with the server skill's data
	 * @param skill A level 0 skill will be removed
	 */
	public SyncSkillPacket(SkillBase skill) {
		compound = skill.writeToNBT();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		compound = buffer.readNBTTagCompoundFromBuffer();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeNBTTagCompoundToBuffer(compound);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		DSSPlayerInfo.get(player).syncClientSideSkill(SkillBase.loadFromNBT(compound));
	}
}
