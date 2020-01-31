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

import java.util.Map.Entry;
import java.util.Random;

import com.google.common.collect.BiMap;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.IMetadataSkillItem;
import dynamicswordskills.api.IRandomSkill;
import dynamicswordskills.api.ISkillInfusionFuelItem;
import dynamicswordskills.api.ItemGrantSkill;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ItemSkillOrb extends ItemGrantSkill implements IModItem, ISkillInfusionFuelItem, IMetadataSkillItem, IRandomSkill
{
	private final BiMap<Integer, ResourceLocation> skill_id_map;

	public ItemSkillOrb(BiMap<Integer, ResourceLocation> skill_id_map) {
		super();
		if (skill_id_map == null || skill_id_map.isEmpty()) {
			throw new IllegalArgumentException("Skill orb items require a valid ID map with at least one entry");
		}
		this.skill_id_map = skill_id_map;
		setMaxDamage(0);
		setHasSubtypes(true);
		setCreativeTab(DynamicSwordSkills.tabSkills);
	}

	@Override
	public SkillBase getSkillToGrant(ItemStack stack) {
		return getSkillFromDamage(stack.getItemDamage());
	}

	@Override
	public SkillBase getSkillToInfuse(ItemStack stack) {
		return getSkillFromDamage(stack.getItemDamage());
	}

	@Override
	public int getAdjustedInfusionCost(ItemStack orb, ItemStack base, int required) {
		return required;
	}

	@Override
	public int getItemDamage(SkillBase skill) {
		Integer i = skill_id_map.inverse().get(skill.getRegistryName());
		return (i == null ? -1 : i);
	}

	@Override
	public SkillBase getSkillFromDamage(int damage) {
		return SkillRegistry.get(skill_id_map.get(damage));
	}

	@Override
	public SkillBase getRandomSkill(Random rand) {
		return getSkillFromDamage(rand.nextInt(skill_id_map.size()));
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = getSkillFromDamage(stack.getItemDamage());
		return new TextComponentTranslation(super.getUnlocalizedName() + ".name", (skill == null ? "" : skill.getDisplayName())).getUnformattedText();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void getSubItems(Item item, CreativeTabs tab, NonNullList<ItemStack> list) {
		for (int i : skill_id_map.keySet()) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	public String[] getVariants() {
		String[] variants = new String[skill_id_map.size()];
		for (Entry<Integer, ResourceLocation> entry : skill_id_map.entrySet()) {
			variants[entry.getKey()] = entry.getValue().getResourceDomain() + ":skill_orb_" + entry.getValue().getResourcePath().toLowerCase();
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
