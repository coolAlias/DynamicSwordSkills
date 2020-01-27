/**
    Copyright (C) <2020> <coolAlias>

    This file is part of coolAlias' Dynamic Sword Skills Minecraft Mod; as such,
    you can redistribute it and/or modify it under the terms of the GNU
    General Public License as published by the Free Software Foundation,
    either version 3 of the License, or (at your option) any later version.

    This program is distributed buffer the hope that it will be useful,
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
import dynamicswordskills.skills.IReachAttackSkill;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Processes an {@link IReachAttackSkill}'s attack on the server.
 *
 */
public class ReachAttackSkillPacket extends AbstractServerMessage<ReachAttackSkillPacket>
{
	/** The currently active reach attack skill */
	private byte skillId;

	/** Stores the entity's ID until it can be retrieved from the world during handling */
	private int entityId;

	public ReachAttackSkillPacket() {}

	public ReachAttackSkillPacket(SkillBase skill, Entity target) {
		this.skillId = skill.getId();
		this.entityId = target.getEntityId();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		this.skillId = buffer.readByte();
		this.entityId = buffer.readInt();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(this.skillId);
		buffer.writeInt(this.entityId);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		SkillActive skill = skills.getActiveSkill(SkillRegistry.getSkillById(this.skillId));
		Entity target = player.worldObj.getEntityByID(this.entityId);
		if (skill instanceof IReachAttackSkill && skill.isActive() && target != null) {
			((EntityPlayerMP) player).func_143004_u();
			double range = ((IReachAttackSkill) skill).getAttackRange(player);
			if (player.canEntityBeSeen(target) && player.getDistanceSqToEntity(target) <= (range * range)) {
				player.attackTargetEntityWithCurrentItem(target);
			}
		}
	}
}
