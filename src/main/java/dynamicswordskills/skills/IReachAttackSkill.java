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

package dynamicswordskills.skills;

import dynamicswordskills.entity.DirtyEntityAccessor;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.ReachAttackSkillPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * Interface for skills that can attack targets at ranges beyond the hard-coded vanilla values.
 * Instead of using the vanilla {@code PlayerControllerMP#attackEntity} method, skills should call
 * {@link IReachAttackSkill#attackEntity(Minecraft, Entity, SkillBase)} when making an attack.
 *
 */
public interface IReachAttackSkill
{
	/**
	 * @return the maximum valid range at which the player can make an attack
	 */
	double getAttackRange(EntityPlayer player);

	/**
	 * If the skill attacks multiple targets in quick succession, suggest returning 20 to allow each attack to deal full damage.
	 * @return 0 for the default attack cooldown period or a custom value
	 */
	int getTicksSinceLastSwing(EntityPlayer player);

	/**
	 * Equivalent of {@link PlayerControllerMP#attackEntity(EntityPlayer, Entity)} but sends a custom attack packet to bypass the hard-coded vanilla reach values.
	 * @param <T> The IReachAttackSkill type
	 * @param mc Current Minecraft player will be used as the attacker
	 * @param target The target to attack
	 * @param skill The IReachAttackSkill skill
	 */
	@SideOnly(Side.CLIENT)
	public static <T extends SkillBase & IReachAttackSkill> void attackEntity(Minecraft mc, Entity target, T skill) {
		DirtyEntityAccessor.syncCurrentPlayItem(mc.playerController);
		IReachAttackSkill.multiAttack(mc, target, skill);
	}

	/**
	 * Use this method when attacking multiple entities e.g. in a loop.
	 * Be sure to call {@link DirtyEntityAccessor#syncCurrentPlayItem(PlayerControllerMP)} first.
	 * Same parameters as {@link IReachAttackSkill#attackEntity(Minecraft, Entity, SkillBase)}.
	 */
	@SideOnly(Side.CLIENT)
	public static <T extends SkillBase & IReachAttackSkill> void multiAttack(Minecraft mc, Entity target, T skill) {
		PacketDispatcher.sendToServer(new ReachAttackSkillPacket(skill, target));
		if (!mc.player.isSpectator()) {
			mc.player.attackTargetEntityWithCurrentItem(target);
			mc.player.resetCooldown();
			int ticks = skill.getTicksSinceLastSwing(mc.player);
			if (ticks > 0) {
				DirtyEntityAccessor.setTicksSinceLastSwing(mc.player, ticks);
			}
		}
	}
}
