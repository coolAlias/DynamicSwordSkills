/**
    Copyright (C) <2020> <coolAlias>

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
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.AbstractMessage.AbstractServerMessage;
import dynamicswordskills.skills.IModifiableSkill;
import dynamicswordskills.skills.ISkillModifier;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.network.PacketBuffer;

/**
 * 
 * Packet notifying the server of an ISkillModifier applied via key press on the client.
 *
 */
public class ApplySkillModifierPacket extends AbstractServerMessage<ApplySkillModifierPacket>
{
	private byte skillId;

	private byte modifierId;

	public ApplySkillModifierPacket() {}

	public <T extends SkillActive & IModifiableSkill, M extends SkillBase & ISkillModifier> ApplySkillModifierPacket(T skill, M modifier) {
		this.skillId = skill.getId();
		this.modifierId = modifier.getId();
	}

	@Override
	protected void read(PacketBuffer buffer) throws IOException {
		this.skillId = buffer.readByte();
		this.modifierId = buffer.readByte();
	}

	@Override
	protected void write(PacketBuffer buffer) throws IOException {
		buffer.writeByte(this.skillId);
		buffer.writeByte(this.modifierId);
	}

	@Override
	protected void process(EntityPlayer player, Side side) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		SkillActive skill = skills.getActiveSkill(SkillRegistry.getSkillById(this.skillId));
		SkillBase refMod = SkillRegistry.getSkillById(this.modifierId);
		SkillBase modifier = skills.getPlayerSkill(refMod);
		if (skill instanceof IModifiableSkill && skill.isActive() && modifier instanceof ISkillModifier && modifier.getLevel() > 0) {
			if (!((IModifiableSkill) skill).getSkillModifiers().contains(refMod)) {
				DynamicSwordSkills.logger.error(String.format("Received invalid skill modifier %s for skill %s", modifier.getRegistryName().toString(), skill.getRegistryName().toString()));
			} else {
				this.applySkillModifier((IModifiableSkill) skill, modifier, player);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <M extends SkillBase & ISkillModifier> void applySkillModifier(IModifiableSkill skill, SkillBase modifier, EntityPlayer player) {
		skill.applySkillModifier((M) modifier, player);
	}
}
