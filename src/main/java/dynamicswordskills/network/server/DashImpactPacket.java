/**
    Copyright (C) <2017> <coolAlias>

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

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractServerMessage;
import dynamicswordskills.skills.Dash;
import dynamicswordskills.skills.Skills;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraftforge.fml.relauncher.Side;

/**
 * 
 * Informs the server about that the player has impacted either a block or an entity
 * while using Dash, and calls {@link Dash#onImpact}, thereby handling and terminating
 * the server side instance of the skill.
 * 
 * This is a workaround for the server having difficulties detecting impacts, at least
 * from the viewpoint of the player; whereas the client impact detection reflects perfectly
 * what the user would expect based on what they see, the server fails to detect entity
 * collisions and usually collides with blocks that the player is standing on, which is
 * weird, considering it works fine for entities like arrows. Hm.
 * 
 * Also need to send the player's motionX and motionZ, as the server values are typically zero.
 *
 */
public class DashImpactPacket extends AbstractServerMessage<DashImpactPacket>
{
	/** Stores the type of hit, as a byte (0: None 1: BLOCK 2: ENTITY) */
	private byte hitType;

	/** Stores the entity's ID until it can be retrieved from the world during handling */
	private int entityId;

	public DashImpactPacket() {}

	/**
	 * Creates dash packet with given moving object position
	 * @param mop Must not be null
	 */
	public DashImpactPacket(EntityPlayer player, RayTraceResult result) {
		this.hitType = (result != null ? (byte) result.typeOfHit.ordinal() : (byte) 0);
		if (this.hitType == RayTraceResult.Type.ENTITY.ordinal()) {
			this.entityId = result.entityHit.getEntityId();
		}
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		hitType = buffer.readByte();
		if (hitType == RayTraceResult.Type.ENTITY.ordinal()) {
			entityId = buffer.readInt();
		}
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(hitType);
		if (hitType == RayTraceResult.Type.ENTITY.ordinal()) {
			buffer.writeInt(entityId);
		}
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		Dash dash = (Dash) DSSPlayerInfo.get(player).getActiveSkill(Skills.dash);
		if (dash != null && dash.isActive()) {
			RayTraceResult result = null;
			if (hitType == RayTraceResult.Type.ENTITY.ordinal()) {
				Entity entityHit = player.getEntityWorld().getEntityByID(entityId);
				if (entityHit != null) {
					result = new RayTraceResult(entityHit);
				} else {
					DynamicSwordSkills.logger.warn("Could not retrieve valid entity for MovingObjectPosition while handling Dash Packet!");
				}
			}
			dash.onImpact(player.getEntityWorld(), player, result);
		}
	}
}
