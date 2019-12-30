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

import com.google.common.collect.Multimap;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.block.Block;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * 
 * Base class providing all the functionality needed for ISkillProvider as well as
 * acting almost exactly like a regular sword (aside from the web-cutting ability),
 * without actually being a sword.
 * 
 * If the item cannot for some reason extend {@link #ItemSword} but should still be
 * considered a sword, simply return true from {@link IWeapon#isSword}. This will
 * allow the item to be used with skills that require swords, such as Mortal Draw,
 * even if it does not itself provide that skill.
 * 
 * Note that an {@link ISkillProvider} item will always be able to use the skill it provides,
 * even if the item is not a sword and the skill would otherwise require such.
 * 
 * It is a simple class, having a set skill and level per item rather than using NBT.
 * 
 * Easily create a new skill-providing weapon simply by extending this class.
 *
 */
public class ItemSkillProvider extends Item implements ISkillProvider
{
	/** The weapon's tool material determines damage and durability */
	private final ToolMaterial material;

	/** Weapon damage is based on tool material, just like swords */
	private float weaponDamage;

	/** The registry name of the skill provided by this item */
	private final ResourceLocation skillName;

	/** The skill level of the SkillBase.{skill} granted by this Item */
	private final byte level;

	/** This is used mainly for the tooltip display */
	private SkillBase skill;

	/** Whether the player should be granted basic sword skill should they not have it */
	private final boolean grantsBasicSkill;

	/**
	 * Shortcut method sets {@link ISkillProvider} to always grant Basic Sword skill if needed to use
	 * the main skill designated by the skill id below.
	 * Standard sword-like weapon with max stack size of 1; be sure to set the unlocalized
	 * name, texture, and creative tab using chained methods if using the class as is.
	 * @param material	the tool material determines both durability and damage
	 * @param skill		use SkillBase.{skill} during construction to ensure a valid skill
	 * @param level		should be at least 1, and will be capped automatically at the skill's max level
	 */
	public ItemSkillProvider(ToolMaterial material, SkillBase skill, byte level) {
		this(material, skill, level, true);
	}

	/**
	 * Standard sword-like weapon with max stack size of 1; be sure to set the unlocalized
	 * name, texture, and creative tab using chained methods if using the class as is.
	 * @param material			the tool material determines both durability and damage
	 * @param skill				use SkillBase.{skill} during construction to ensure a valid skill
	 * @param level				should be at least 1, and will be capped automatically at the skill's max level
	 * @param grantsBasicSkill	if true, the player will be temporarily granted Basic Sword skill in
	 * 							order to use the {@link ISkillProvider} main skill, if other than Basic Sword
	 */
	public ItemSkillProvider(ToolMaterial material, SkillBase skill, byte level, boolean grantsBasicSkill) {
		super();
		this.material = material;
		this.weaponDamage = 2.0F + this.material.getDamageVsEntity();
		this.skillName = skill.getRegistryName();
		this.level = level;
		this.grantsBasicSkill = grantsBasicSkill;
		setMaxDamage(this.material.getMaxUses());
		setMaxStackSize(1);
	}

	@Override
	public boolean isSword(ItemStack stack) {
		return false;
	}

	@Override
	public boolean isWeapon(ItemStack stack) {
		return true;
	}

	/**
	 * A convenience / optimizer for displaying item tooltips; never used by the rest of the API
	 * Store the leveled SkillBase locally in the Item class the first time the method is
	 * called to improve efficiency (since the level will never change) using the method
	 * {@link SkillBase#getSkillFromItem(ItemStack, ISkillProvider) SkillBase.getSkillFromItem}
	 * @param stack not used in this implementation, but required for the SkillBase method above
	 * @return	DO NOT use the returned skill as a player's active instance - it is not unique!
	 */
	protected SkillBase getSkill(ItemStack stack) {
		if (skill == null) {
			skill = SkillBase.getSkillFromItem(stack, this);
		}
		return skill;
	}

	@Override
	public int getSkillId(ItemStack stack) {
		SkillBase skill = SkillRegistry.get(this.skillName);
		return (skill == null ? -1 : skill.getId());
	}

	@Override
	public byte getSkillLevel(ItemStack stack) {
		return level;
	}

	@Override
	public boolean grantsBasicSwordSkill(ItemStack stack) {
		return grantsBasicSkill;
	}

	@Override
	public boolean hitEntity(ItemStack stack, EntityLivingBase target, EntityLivingBase attacker) {
		stack.damageItem(1, attacker);
		return true;
	}

	@SideOnly(Side.CLIENT)
	public boolean isFull3D() {
		return true;
	}

	@Override
	public EnumAction getItemUseAction(ItemStack stack) {
		return EnumAction.block;
	}

	@Override
	public int getItemEnchantability() {
		return material.getEnchantability();
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack stack) {
		return material.func_150995_f() == stack.getItem() ? true : super.getIsRepairable(toRepair, stack);
	}

	@Override
	public int getMaxItemUseDuration(ItemStack stack) {
		return 72000;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, Block block, int x, int y, int z, EntityLivingBase entity) {
		if (block.getBlockHardness(world, x, y, z) != 0.0D) {
			stack.damageItem(2, entity);
		}
		return true;
	}

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		player.setItemInUse(stack, getMaxItemUseDuration(stack));
		return stack;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = getSkill(stack);
		return StatCollector.translateToLocalFormatted(getUnlocalizedName(stack) + ".name", (skill == null ? "" : skill.getDisplayName()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, EntityPlayer player, List list, boolean advanced) {
		SkillBase skill = getSkill(stack);
		if (skill != null) {
			list.add(StatCollector.translateToLocalFormatted("tooltip.dss.skill_provider.desc.skill", skill.getLevel(), EnumChatFormatting.GOLD + skill.getDisplayName() + EnumChatFormatting.GRAY));
			if (grantsBasicSwordSkill(stack)) {
				String name = EnumChatFormatting.DARK_GREEN + Skills.swordBasic.getDisplayName() + EnumChatFormatting.GRAY;
				list.add(StatCollector.translateToLocalFormatted("tooltip.dss.skill_provider.desc.provider", name));
			}
			if (advanced) {
				list.addAll(skill.getTooltip(player, true));
			}
		}
	}

	@Override
	public Multimap getAttributeModifiers(ItemStack stack) {
		Multimap multimap = super.getAttributeModifiers(stack);
		multimap.put(SharedMonsterAttributes.attackDamage.getAttributeUnlocalizedName(), new AttributeModifier(field_111210_e, "Weapon modifier", weaponDamage, 0));
		return multimap;
	}
}
