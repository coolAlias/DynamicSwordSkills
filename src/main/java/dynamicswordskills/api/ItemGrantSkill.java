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

import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModSounds;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

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
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
		if (!player.worldObj.isRemote) {
			SkillBase skill = getSkillToGrant(stack);
			if (skill != null) {
				if (!Config.isSkillEnabled(skill)) {
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.use.disabled", new TextComponentTranslation(skill.getNameTranslationKey()));
				} else if (DSSPlayerInfo.get(player).grantSkill(skill)) {
					PlayerUtils.playSound(player, ModSounds.LEVEL_UP, SoundCategory.PLAYERS, 1.0F, 1.0F);
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.levelup",
							new TextComponentTranslation(skill.getNameTranslationKey()), DSSPlayerInfo.get(player).getTrueSkillLevel(skill));
					if (!player.capabilities.isCreativeMode) {
						--stack.stackSize;
					}
				} else {
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.maxlevel", new TextComponentTranslation(skill.getNameTranslationKey()));
				}
			}
		}
		return new ActionResult<ItemStack>(EnumActionResult.SUCCESS, stack);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> tooltip, boolean advanced) {
		SkillBase skill = DSSPlayerInfo.get(player).getPlayerSkill(getSkillToGrant(stack));
		if (skill != null) {
			if (!Config.isSkillEnabled(skill)) {
				tooltip.add(TextFormatting.DARK_RED + new TextComponentTranslation("skill.dss.disabled").getUnformattedText());
			} else if (skill.getLevel() > 0) {
				tooltip.add(TextFormatting.GOLD + skill.getLevelDisplay(true));
				tooltip.addAll(skill.getTranslatedTooltip(player, advanced));
			} else {
				tooltip.add(TextFormatting.ITALIC + new TextComponentTranslation("tooltip.dss.skillorb.desc.0").getUnformattedText());
			}
		}
	}
}
