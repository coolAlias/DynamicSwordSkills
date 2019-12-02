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

package dynamicswordskills.inventory;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.IMetadataSkillItem;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

/**
 * 
 * Container class for interacting with the Skills Gui; slots hold skill orbs for
 * each skill the player has, but cannot be taken or moved.
 *
 */
public class ContainerSkills extends Container
{
	private final InventoryBasic inventory;

	public ContainerSkills(EntityPlayer player) {
		DSSPlayerInfo skills = DSSPlayerInfo.get(player);
		inventory = new InventoryBasic("", true, SkillRegistry.getValues().size());
		int x, y;
		for (ResourceLocation location : Skills.getSkillIdMap().values()) {
			SkillBase skill = SkillRegistry.get(location);
			if (skill != null && skills.hasSkill(skill)) {
				int dmg = ((IMetadataSkillItem) DynamicSwordSkills.skillOrb).getItemDamage(skill);
				if (dmg > -1) {
					inventory.setInventorySlotContents(dmg, new ItemStack(DynamicSwordSkills.skillOrb, 1, dmg));
				}
			}
		}
		addSlotToContainer(new Slot(inventory, 0, 65, 141));
		for (int i = 1; i < inventory.getSizeInventory(); ++i) {
			int bottom = 3;
			int sideBar = 5;
			int rightSide = bottom + sideBar;
			if (i > bottom) {
				x = (i > rightSide ? 108 : 22);
				y = 120 - (i > rightSide ? (i - (rightSide + 1)) : (i - (sideBar - 1))) * 21;
			} else {
				x = 44 + (i - 1) * 21;
				y = 120;
			}
			addSlotToContainer(new Slot(inventory, i, x, y));
		}
	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		return null;
	}

	@Override
	public ItemStack slotClick(int slotIndex, int button, int par3, EntityPlayer player) {
		return null;
	}
}
