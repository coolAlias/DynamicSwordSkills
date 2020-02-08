/**
    Copyright (C) <2019> <coolAlias>

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

package dynamicswordskills.api;

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * 
 * Provides basic functionality for items that increase the player's skill level by one when used.
 *
 */
public abstract class ItemGrantSkill extends Item
{
	/**
	 * Return the skill that can be learned when this item is used
	 */
	public abstract SkillBase getSkillToGrant(ItemStack stack);

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (!player.worldObj.isRemote) {
			SkillBase skill = getSkillToGrant(stack);
			if (skill != null) {
				if (!Config.isSkillAllowed(skill)) {
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.use.disabled", new ChatComponentTranslation(skill.getNameTranslationKey()));
				} else if (DSSPlayerInfo.get(player).grantSkill(skill)) {
					PlayerUtils.playSound(player, ModInfo.SOUND_LEVELUP, 1.0F, 1.0F);
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.levelup", new ChatComponentTranslation(skill.getNameTranslationKey()), DSSPlayerInfo.get(player).getTrueSkillLevel(skill));
					if (!player.capabilities.isCreativeMode) {
						--stack.stackSize;
					}
				} else {
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.maxlevel", new ChatComponentTranslation(skill.getNameTranslationKey()));
				}
			}
		}
		return stack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List tooltip, boolean advanced) {
		SkillBase skill = getSkillToGrant(stack);
		if (skill != null) {
			SkillBase instance = DSSPlayerInfo.get(player).getPlayerSkill(skill);
			if (!Config.isSkillAllowed(skill)) {
				tooltip.add(EnumChatFormatting.DARK_RED + StatCollector.translateToLocal("skill.dss.disabled.server"));
			} else if (instance != null && instance.getLevel() > 0) {
				tooltip.add(EnumChatFormatting.GOLD + instance.getLevelDisplay(true));
				tooltip.addAll(instance.getTooltip(player, advanced));
			} else {
				tooltip.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("tooltip.dss.skillorb.desc.0"));
			}
		}
	}
}
