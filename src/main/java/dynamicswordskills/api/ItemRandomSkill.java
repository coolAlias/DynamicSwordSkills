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

package dynamicswordskills.api;

import java.util.List;
import java.util.Random;

import javax.annotation.Nullable;

import dynamicswordskills.item.IModItem;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * Similar to {@link ItemSkillProvider}, but providing items with random skills at random
 * levels by using NBT, with a maximum level determined by item material.
 * 
 * An item such as this is useful for generating random loot, but not much use out of the
 * Creative Tab.
 * 
 * Due to these items being implemented as though they were swords that grant skills,
 * they extend ItemSword instead of Item, but could just as well be anything.
 *
 */
public class ItemRandomSkill extends ItemSword implements IModItem, ISkillProvider
{
	/** Item quality based on tool material; higher quality tends toward higher levels */
	private final int quality;

	/** String used as the ModelResourceLocation for this item's model */
	private final String texture;

	public ItemRandomSkill(ToolMaterial material, String textureName) {
		super(material);
		this.texture = textureName;
		this.quality = material.getHarvestLevel() + (material == ToolMaterial.GOLD ? 3 : 0);
		setCreativeTab(null);
		setRegistryName(ModInfo.ID, "skillsword_" + material.name());
		setUnlocalizedName("dss.skill" + material.name());
	}

	@Override
	public boolean isSword(ItemStack stack) {
		return true;
	}

	@Override
	public boolean isWeapon(ItemStack stack) {
		return true;
	}

	/**
	 * A convenience method for ensuring the stack has an NBT tag before retrieving the skill instance
	 * using the method {@link SkillBase#getSkillFromItem(ItemStack, ISkillProvider) SkillBase.getSkillFromItem}
	 * Note again that this method is only used for the tooltip, not be any part of the API
	 * @param stack allows returning different values based on the ItemStack's data or damage;
	 * 				these should be handled from the getSkillLevel and getSkillId methods
	 */
	protected SkillBase getSkill(ItemStack stack) {
		return (stack.hasTagCompound() ? SkillBase.getSkillFromItem(stack, this) : null);
	}

	@Override
	public int getSkillId(ItemStack stack) {
		if (!stack.hasTagCompound()) {
			return -1;
		}
		NBTTagCompound tag = stack.getTagCompound();
		SkillBase skill = null;
		if (tag.hasKey("ItemSkillName")) {
			skill = SkillBase.getSkillByName(tag.getString("ItemSkillName"));
		}
		// For backwards compatibility:
		if (tag.hasKey("ItemSkillId")) {
			if (skill == null) {
				skill = SkillBase.getSkill(tag.getInteger("ItemSkillId"));
				if (skill != null) {
					tag.setString("ItemSkillName", skill.getUnlocalizedName());
				}
			} else if (tag.getInteger("ItemSkillId") != skill.getId()) {
				tag.setInteger("ItemSkillId", skill.getId());
			}
		}
		return (skill == null ? -1 : skill.getId());
	}

	@Override
	public byte getSkillLevel(ItemStack stack) {
		return (stack.hasTagCompound() && stack.getTagCompound().hasKey("ItemSkillLevel") ?
				stack.getTagCompound().getByte("ItemSkillLevel") : 0);
	}

	@Override
	public boolean grantsBasicSwordSkill(ItemStack stack) {
		return (stack.hasTagCompound() && stack.getTagCompound().getBoolean("grantsBasicSword"));
	}

	@Override
	public boolean isRepairable() {
		return false;
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack stack) {
		return true;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = getSkill(stack);
		return new TextComponentTranslation("item.dss.skillitem.name", (skill == null ? "" : skill.getDisplayName())).getUnformattedText();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
		SkillBase skill = getSkill(stack);
		if (skill != null && world != null) {
			list.add(new TextComponentTranslation("tooltip.dss.skillprovider.desc.skill", TextFormatting.GOLD + skill.getDisplayName()).getUnformattedText());
			list.add(new TextComponentTranslation("tooltip.dss.skillprovider.desc.level", skill.getLevel(), skill.getMaxLevel()).getUnformattedText());
			if (grantsBasicSwordSkill(stack)) {
				String name = TextFormatting.DARK_GREEN + SkillBase.swordBasic.getDisplayName() + TextFormatting.RESET;
				list.add(new TextComponentTranslation("tooltip.dss.skillprovider.desc.provider", name).getUnformattedText());
			}
			list.addAll(skill.getDescription(Minecraft.getMinecraft().player));
		}
	}

	@Override
	public String[] getVariants() {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerResources() {
		ModelLoader.registerItemVariants(this, new ModelResourceLocation(texture, "inventory"));
		ModelLoader.setCustomMeshDefinition(this, new ItemMeshDefinition() {
			@Override
			public ModelResourceLocation getModelLocation(ItemStack stack) {
				return ModelLoader.getInventoryVariant(texture);
			} 
		});
	}

	/**
	 * Adds all necessary skill data to the stack's NBT Tag:
	 * skill id, random level not exceeding the maximum, and
	 * random ability to grant basic sword skill
	 */
	public void generateSkillTag(ItemStack stack, SkillBase skill, Random rand) {
		if (!stack.hasTagCompound()) {
			stack.setTagCompound(new NBTTagCompound());
		}
		NBTTagCompound tag = stack.getTagCompound();
		tag.setString("ItemSkillName", skill.getUnlocalizedName());
		int level = 1 + rand.nextInt(Math.min(this.quality + 2, skill.getMaxLevel()));
		tag.setByte("ItemSkillLevel", (byte) level);
		boolean flag = (skill.getId() != SkillBase.swordBasic.getId() && rand.nextInt(16) > 9 - this.quality); 
		tag.setBoolean("grantsBasicSword", flag);
	}
}
