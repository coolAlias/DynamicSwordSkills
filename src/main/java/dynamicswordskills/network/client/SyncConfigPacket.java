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
import java.util.List;

import com.google.common.collect.Lists;

import cpw.mods.fml.relauncher.Side;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.network.AbstractMessage.AbstractClientMessage;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Sent to each player as they log in to synchronize certain configuration settings.
 *
 */
public class SyncConfigPacket extends AbstractClientMessage<SyncConfigPacket>
{
	/** Processing calls a static method in Config, so use this field to indicate that it is a valid packet */
	private boolean isValid;
	public int baseSwingSpeed;
	public boolean requireFullHealth;
	public List<Byte> disabledIds = Lists.<Byte>newArrayList();

	public SyncConfigPacket() {}

	/**
	 * Returns whether packet is valid
	 */
	public final boolean isMessageValid() {
		return isValid;
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		baseSwingSpeed = buffer.readInt();
		requireFullHealth = buffer.readBoolean();
		isValid = true;
		int n = buffer.readInt();
		for (int i = 0; i < n; ++i) {
			disabledIds.add(buffer.readByte());
		}
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeInt(Config.getBaseSwingSpeed());
		buffer.writeBoolean(Config.getHealthAllowance(1) == 0.0F);
		for (SkillBase skill : SkillRegistry.getValues()) {
			if (!Config.isSkillAllowed(skill)) {
				disabledIds.add(skill.getId());
			}
		}
		buffer.writeInt(disabledIds.size());
		for (Byte b : disabledIds) {
			buffer.writeByte(b);
		}
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		Config.syncClientSettings(this);
	}
}
