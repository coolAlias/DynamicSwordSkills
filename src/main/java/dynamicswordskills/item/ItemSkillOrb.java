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

package dynamicswordskills.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IIcon;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;

public class ItemSkillOrb extends Item
{
	@SideOnly(Side.CLIENT)
	private List<IIcon> icons;

	public ItemSkillOrb() {
		super();
		setMaxDamage(0);
		setHasSubtypes(true);
		setCreativeTab(DynamicSwordSkills.tabSkills);
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if (!player.worldObj.isRemote) {
			SkillBase skill = SkillBase.getSkill(stack.getItemDamage());
			if (skill != null) {
				if (!Config.isSkillEnabled(skill.getId())) {
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.use.disabled", new ChatComponentTranslation(skill.getTranslationString()));
				} else if (DSSPlayerInfo.get(player).grantSkill(skill)) {
					world.playSoundAtEntity(player, ModInfo.SOUND_LEVELUP, 1.0F, 1.0F);
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.levelup",
							new ChatComponentTranslation(skill.getTranslationString()), DSSPlayerInfo.get(player).getTrueSkillLevel(skill));
					if (!player.capabilities.isCreativeMode) {
						--stack.stackSize;
					}
				} else {
					PlayerUtils.sendTranslatedChat(player, "chat.dss.skill.maxlevel", new ChatComponentTranslation(skill.getTranslationString()));
				}
			}
		}

		return stack;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = SkillBase.getSkill(stack.getItemDamage());
		return StatCollector.translateToLocalFormatted(super.getUnlocalizedName() + ".name", (skill == null ? "" : skill.getDisplayName()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public IIcon getIconFromDamage(int damage) {
		return icons.get(damage % icons.size());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List list) {
		for (SkillBase skill : SkillBase.getSkills()) {
			list.add(new ItemStack(item, 1, skill.getId()));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		icons = new ArrayList<IIcon>(SkillBase.getNumSkills());
		for (SkillBase skill : SkillBase.getSkills()) {
			icons.add(register.registerIcon(skill.getIconTexture()));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack,	EntityPlayer player, List list, boolean par4) {
		if (SkillBase.doesSkillExist(stack.getItemDamage())) {
			SkillBase skill = DSSPlayerInfo.get(player).getPlayerSkill(SkillBase.getSkill(stack.getItemDamage()));
			if (skill != null) {
				if (!Config.isSkillEnabled(skill.getId())) {
					list.add(EnumChatFormatting.DARK_RED + StatCollector.translateToLocal("skill.dss.disabled"));
				} else if (skill.getLevel() > 0) {
					list.add(EnumChatFormatting.GOLD + skill.getLevelDisplay(true));
					list.addAll(skill.getTranslatedTooltip(player));
				} else {
					list.add(EnumChatFormatting.ITALIC + StatCollector.translateToLocal("tooltip.dss.skillorb.desc.0"));
				}
			}
		}
	}
}
