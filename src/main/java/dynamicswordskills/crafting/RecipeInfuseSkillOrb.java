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

package dynamicswordskills.crafting;

import dynamicswordskills.api.ISkillInfusionFuelItem;
import dynamicswordskills.api.ISkillProviderInfusable;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.inventory.InventoryCrafting;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

public class RecipeInfuseSkillOrb implements IRecipe
{
	public static final ResourceLocation registryName = new ResourceLocation(ModInfo.ID, "infuse_skill_orb");

	private ItemStack input;

	private ItemStack output;

	private SkillBase skill;

	public RecipeInfuseSkillOrb() {
	}

	@Override
	public boolean matches(InventoryCrafting grid, World world) {
		ItemStack fuel = ItemStack.EMPTY;
		ItemStack base = ItemStack.EMPTY;
		int found = 0;
		for (int i = 0; i < grid.getSizeInventory(); ++i) {
			ItemStack stack = grid.getStackInSlot(i);
			if (stack.isEmpty()) {
				// no-op
			} else if (stack.getItem() instanceof ISkillInfusionFuelItem) {
				if (fuel.isEmpty()) {
					fuel = stack;
					this.skill = ((ISkillInfusionFuelItem) fuel.getItem()).getSkillToInfuse(fuel);
				} else if (this.skill == null || !this.skill.is(((ISkillInfusionFuelItem) stack.getItem()).getSkillToInfuse(stack))) {
					return false;
				}
				found++;
			} else if (stack.getItem() instanceof ISkillProviderInfusable) {
				if (!base.isEmpty()) {
					return false;
				}
				base = stack;
			} else {
				return false;
			}
		}
		if (fuel.isEmpty() || base.isEmpty() || this.skill == null) {
			return false;
		}
		this.input = base;
		int required = ((ISkillProviderInfusable) base.getItem()).getInfusionCost(base, this.skill);
		required = ((ISkillInfusionFuelItem) fuel.getItem()).getAdjustedInfusionCost(fuel, base, required);
		return required > 0 && found == required;
	}

	@Override
	public ItemStack getCraftingResult(InventoryCrafting grid) {
		this.output = ((ISkillProviderInfusable) this.input.getItem()).getInfusionResult(this.input, this.skill);
		return (this.output == null ? ItemStack.EMPTY : this.output.copy());
	}

	@Override
	public boolean canFit(int width, int height) {
		return width + height > 1;
	}

	@Override
	public ItemStack getRecipeOutput() {
		return ItemStack.EMPTY;
	}

	@Override
	public IRecipe setRegistryName(ResourceLocation name) {
		return this;
	}

	@Override
	public ResourceLocation getRegistryName() {
		return RecipeInfuseSkillOrb.registryName;
	}

	@Override
	public Class<IRecipe> getRegistryType() {
		return IRecipe.class;
	}
}
