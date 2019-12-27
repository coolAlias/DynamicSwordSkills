/**
    Copyright (C) <2017> <coolAlias>

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

import java.util.List;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ISkillInfusionFuelItem;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModSounds;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
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
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSkillOrb extends Item implements IModItem, ISkillInfusionFuelItem
{
	public ItemSkillOrb() {
		super();
		setMaxDamage(0);
		setHasSubtypes(true);
		setCreativeTab(DynamicSwordSkills.tabSkills);
	}

	@Override
	public SkillBase getSkillToInfuse(ItemStack stack) {
		return SkillRegistry.getSkillById(stack.getItemDamage());
	}

	@Override
	public int getAdjustedInfusionCost(ItemStack orb, ItemStack base, int required) {
		return required;
	}

	@Override
	public ActionResult<ItemStack> onItemRightClick(ItemStack stack, World world, EntityPlayer player, EnumHand hand) {
		if (!player.worldObj.isRemote) {
			SkillBase skill = SkillRegistry.getSkillById(stack.getItemDamage());
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
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = SkillRegistry.getSkillById(stack.getItemDamage());
		return new TextComponentTranslation(super.getUnlocalizedName() + ".name", (skill == null ? "" : skill.getDisplayName())).getUnformattedText();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, List<ItemStack> list) {
		// Hack to maintain original display order
		List<SkillBase> skills = SkillRegistry.getSortedList(new SkillRegistry.SortById());
		for (SkillBase skill : skills) {
			list.add(new ItemStack(item, 1, skill.getId()));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean par4) {
		SkillBase skill = DSSPlayerInfo.get(player).getPlayerSkill(SkillRegistry.getSkillById(stack.getItemDamage()));
		if (skill != null) {
			if (!Config.isSkillEnabled(skill)) {
				list.add(TextFormatting.DARK_RED + new TextComponentTranslation("skill.dss.disabled").getUnformattedText());
			} else if (skill.getLevel() > 0) {
				list.add(TextFormatting.GOLD + skill.getLevelDisplay(true));
				list.addAll(skill.getTranslatedTooltip(player));
			} else {
				list.add(TextFormatting.ITALIC + new TextComponentTranslation("tooltip.dss.skillorb.desc.0").getUnformattedText());
			}
		}
	}

	@Override
	public String[] getVariants() {
		String[] variants = new String[SkillRegistry.getValues().size()];
		for (SkillBase skill : SkillRegistry.getValues()) {
			variants[skill.getId()] = skill.getIconTexture();
		}
		return variants;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerResources() {
		String[] variants = getVariants();
		for (int i = 0; i < variants.length; ++i) {
			ModelLoader.setCustomModelResourceLocation(this, i, new ModelResourceLocation(variants[i], "inventory"));
		}
	}
}
