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

import java.util.List;
import java.util.Map.Entry;
import java.util.Random;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.IMetadataSkillItem;
import dynamicswordskills.api.IRandomSkill;
import dynamicswordskills.api.ISkillInfusionFuelItem;
import dynamicswordskills.api.ItemGrantSkill;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

public class ItemSkillOrb extends ItemGrantSkill implements ISkillInfusionFuelItem, IMetadataSkillItem, IRandomSkill
{
	@SideOnly(Side.CLIENT)
	private BiMap<Integer, IIcon> icons;

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
		for (int i : skill_id_map.keySet()) {
			list.add(new ItemStack(item, 1, i));
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void registerIcons(IIconRegister register) {
		icons = HashBiMap.<Integer, IIcon>create(skill_id_map.size());
		for (Entry<Integer, ResourceLocation> entry : skill_id_map.entrySet()) {
			icons.put(entry.getKey(), register.registerIcon(ModInfo.ID + ":skillorb_" + entry.getValue().getResourcePath().toLowerCase()));
		}
	}
}
