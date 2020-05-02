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

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.item.IModItem;
import dynamicswordskills.loot.functions.SkillFunction;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
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
public class ItemRandomSkill extends ItemSword implements IModItem, IRandomSkill, ISkillProviderInfusable
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
	}

	@Override
	public SkillBase getRandomSkill(Random rand) {
		SkillBase skill = SkillFunction.getRandomSkill(rand);
		return (skill instanceof SkillActive ? skill : null);
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
			String name = tag.getString("ItemSkillName");
			// For backwards compatibility:
			if (name.lastIndexOf(':') == -1) {
				name = ModInfo.ID + ":" + name;
				tag.setString("ItemSkillName", name);
			}
			skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(name));
		}
		// For backwards compatibility:
		if (skill == null && tag.hasKey("ItemSkillId")) {
			skill = SkillRegistry.getSkillById(tag.getInteger("ItemSkillId"));
			if (skill != null) {
				tag.setString("ItemSkillName", skill.getRegistryName().toString());
				tag.removeTag("ItemSkillId");
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
	public int getInfusionCost(ItemStack stack, SkillBase skill) {
		if (skill.is(Skills.swordBasic) && !grantsBasicSwordSkill(stack)) {
			return 1;
		} else if (!skill.is(getSkill(stack))) {
			return 0;
		}
		int i = getSkillLevel(stack);
		return (i < skill.getMaxLevel() ? i + 1 : 0);
	}

	@Override
	public ItemStack getInfusionResult(ItemStack stack, SkillBase skill) {
		ItemStack result = stack.copy();
		if (skill.is(Skills.swordBasic) && !grantsBasicSwordSkill(stack)) {
			result.getTagCompound().setBoolean("grantsBasicSword", true);
		} else {
			int level = Math.min(skill.getMaxLevel(), getSkillLevel(stack) + 1);
			result.getTagCompound().setByte("ItemSkillLevel", (byte) level);
		}
		return result;
	}

	@Override
	public boolean isRepairable() {
		return false;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = getSkill(stack);
		return new TextComponentTranslation(getUnlocalizedName(stack) + ".name", (skill == null ? "" : skill.getDisplayName())).getUnformattedText();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List<String> list, boolean advanced) {
		SkillBase skill = getSkill(stack);
		if (skill != null) {
			list.add(new TextComponentTranslation("tooltip.dss.skill_provider.desc.skill", skill.getLevel(), TextFormatting.GOLD + skill.getDisplayName() + TextFormatting.GRAY).getUnformattedText());
			if (grantsBasicSwordSkill(stack)) {
				String name = TextFormatting.DARK_GREEN + Skills.swordBasic.getDisplayName() + TextFormatting.GRAY;
				list.add(new TextComponentTranslation("tooltip.dss.skill_provider.desc.provider", name).getUnformattedText());
			}
			if (advanced) {
				list.addAll(skill.getTooltip(player, true));
			}
		}
	}

	@Override
	public String[] getVariants() {
		return null;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerResources() {
		ModelBakery.registerItemVariants(this, new ModelResourceLocation(texture, "inventory"));
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
		tag.setString("ItemSkillName", skill.getRegistryName().toString());
		int level = 1 + rand.nextInt(Math.min(this.quality + 2, skill.getMaxLevel()));
		tag.setByte("ItemSkillLevel", (byte) level);
		boolean flag = (!skill.is(Skills.swordBasic) && rand.nextInt(16) > 9 - this.quality); 
		tag.setBoolean("grantsBasicSword", flag);
	}
}
