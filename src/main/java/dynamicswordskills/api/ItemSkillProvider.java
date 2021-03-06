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

import javax.annotation.Nullable;

import com.google.common.collect.Multimap;

import dynamicswordskills.item.IModItem;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemMeshDefinition;
import net.minecraft.client.renderer.block.model.ModelBakery;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import swordskillsapi.api.item.IWeapon;

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
 * Note that an ISkillProvider item will always be able to use the skill it provides,
 * even if the item is not a sword and the skill would otherwise require such.
 * 
 * It is a simple class, having a set skill and level per item rather than using NBT.
 * 
 * Easily create a new skill-providing weapon simply by extending this class.
 *
 */
public class ItemSkillProvider extends Item implements IModItem, ISkillProvider
{
	/** The weapon's tool material determines damage and durability */
	private final ToolMaterial material;

	/** String used as the ModelResourceLocation for this item's model */
	private final String texture;

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
	 * Shortcut method sets ISkillProvider to always grant Basic Sword skill if needed to use
	 * the main skill designated by the skill id below.
	 * Standard sword-like weapon with max stack size of 1; be sure to set the unlocalized
	 * name, texture, and creative tab using chained methods if using the class as is.
	 * @param material the tool material determines both durability and damage
	 * @param texture  the string used as the ModelResourceLocation for this item's model
	 * @param skill    use SkillBase.{skill} during construction to ensure a valid skill
	 * @param level    should be at least 1, and will be capped automatically at the skill's max level
	 */
	public ItemSkillProvider(ToolMaterial material, String texture, SkillBase skill, byte level) {
		this(material, texture, skill, level, true);
	}

	/**
	 * Standard sword-like weapon with max stack size of 1; be sure to set the unlocalized
	 * name, texture, and creative tab using chained methods if using the class as is.
	 * @param material         the tool material determines both durability and damage
	 * @param texture          the string used as the ModelResourceLocation for this item's model
	 * @param skill            use SkillBase.{skill} during construction to ensure a valid skill
	 * @param level            should be at least 1, and will be capped automatically at the skill's max level
	 * @param grantsBasicSkill if true, the player will be temporarily granted Basic Sword skill in
	 *                         order to use the ISkillProvider main skill, if other than Basic Sword
	 */
	public ItemSkillProvider(ToolMaterial material, String texture, SkillBase skill, byte level, boolean grantsBasicSkill) {
		super();
		this.material = material;
		this.texture = texture;
		this.weaponDamage = 2.0F + this.material.getAttackDamage();
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

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isFull3D() {
		return true;
	}

	@Override
	public int getItemEnchantability() {
		return material.getEnchantability();
	}

	@Override
	public boolean getIsRepairable(ItemStack toRepair, ItemStack stack) {
		return ItemStack.areItemsEqual(stack, material.getRepairItemStack()) || super.getIsRepairable(toRepair, stack);
	}

	@Override
	public boolean canDestroyBlockInCreative(World world, BlockPos pos, ItemStack stack, EntityPlayer player) {
		return false;
	}

	@Override
	public boolean onBlockDestroyed(ItemStack stack, World world, IBlockState state, BlockPos pos, EntityLivingBase entity) {
		if (state.getBlockHardness(world, pos) != 0.0D) {
			stack.damageItem(2, entity);
		}
		return true;
	}

	@Override
	public String getItemStackDisplayName(ItemStack stack) {
		SkillBase skill = getSkill(stack);
		return new TextComponentTranslation(getTranslationKey(stack) + ".name", (skill == null ? "" : skill.getDisplayName())).getUnformattedText();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
		SkillBase skill = getSkill(stack);
		if (skill != null && world != null) {
			list.add(new TextComponentTranslation("tooltip.dss.skill_provider.desc.skill", skill.getLevel(), TextFormatting.GOLD + skill.getDisplayName() + TextFormatting.GRAY).getUnformattedText());
			if (grantsBasicSwordSkill(stack)) {
				String name = TextFormatting.DARK_GREEN + Skills.swordBasic.getDisplayName() + TextFormatting.GRAY;
				list.add(new TextComponentTranslation("tooltip.dss.skill_provider.desc.provider", name).getUnformattedText());
			}
			if (flag.isAdvanced()) {
				list.addAll(skill.getTooltip(Minecraft.getMinecraft().player, true));
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

	@Override
	public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot slot, ItemStack stack) {
		Multimap<String, AttributeModifier> multimap = super.getAttributeModifiers(slot, stack);
		if (slot == EntityEquipmentSlot.MAINHAND) {
			multimap.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", weaponDamage, 0));
			multimap.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Weapon modifier", -2.4000000953674316D, 0));
		}
		return multimap;
	}
}
