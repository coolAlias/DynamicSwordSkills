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

package dynamicswordskills.loot;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.loot.conditions.SkillCondition;
import dynamicswordskills.loot.functions.RandomSkillSword;
import dynamicswordskills.loot.functions.SetSkillMetadata;
import dynamicswordskills.ref.Config;
import net.minecraft.item.Item;
import net.minecraft.world.storage.loot.LootEntry;
import net.minecraft.world.storage.loot.LootEntryItem;
import net.minecraft.world.storage.loot.LootPool;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraft.world.storage.loot.RandomValueRange;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.conditions.LootConditionManager;
import net.minecraft.world.storage.loot.conditions.RandomChance;
import net.minecraft.world.storage.loot.functions.LootFunction;
import net.minecraft.world.storage.loot.functions.LootFunctionManager;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class LootHandler
{
	private LootEntry skillOrbs;
	private LootPool skillOrbsPool;
	private LootEntry[] skillSwords;
	private LootPool skillSwordsPool;

	static {
		LootConditionManager.registerCondition(new SkillCondition.Serializer());
		LootFunctionManager.registerFunction(new RandomSkillSword.Serializer());
		LootFunctionManager.registerFunction(new SetSkillMetadata.Serializer());
	}

	public LootHandler() {
		if (Config.getLootWeight() > 0) {
			this.skillOrbs = createLootEntry(DynamicSwordSkills.skillOrb, Config.getLootWeight(), 0, new LootFunction[]{new SetSkillMetadata(new LootCondition[0])});
			RandomValueRange rolls = new RandomValueRange(1, 1);
			RandomValueRange bonus = new RandomValueRange(0.0F, 0.0F);
			LootCondition chance = new RandomChance((float)Config.getLootWeight() * 0.01F);
			this.skillOrbsPool = new LootPool(new LootEntry[]{this.skillOrbs}, new LootCondition[]{chance}, rolls, bonus, "skill_orbs");
		}
		if (Config.areRandomSwordsEnabled()) {
			LootFunction[] functions = new LootFunction[]{new RandomSkillSword()};
			LootCondition enabled = new SkillCondition();
			this.skillSwords = new LootEntry[] {
					createLootEntry(DynamicSwordSkills.skillWood, Config.getLootWeight(), 0, functions, new LootCondition[]{enabled}),
					createLootEntry(DynamicSwordSkills.skillStone, Config.getLootWeight(), 1, functions, new LootCondition[]{enabled}),
					createLootEntry(DynamicSwordSkills.skillIron, Config.getLootWeight(), 3, functions, new LootCondition[]{enabled}),
					createLootEntry(DynamicSwordSkills.skillGold, Config.getLootWeight(), 2, functions, new LootCondition[]{enabled}),
					createLootEntry(DynamicSwordSkills.skillDiamond, Config.getLootWeight(), 4, functions, new LootCondition[]{enabled})
			};
			RandomValueRange rolls = new RandomValueRange(1, 2);
			RandomValueRange bonus = new RandomValueRange(0.0F, 0.5F);
			LootCondition chance = new RandomChance((float)Config.getLootWeight() * 0.02F); // twice as likely as a skill orb
			this.skillSwordsPool = new LootPool(this.skillSwords, new LootCondition[]{enabled, chance}, rolls, bonus, "skill_swords");
		}
	}

	@SubscribeEvent
	public void addLoot(LootTableLoadEvent event) {
		if(!event.getName().getResourceDomain().equalsIgnoreCase("minecraft") || !event.getName().getResourcePath().toLowerCase().contains("chest")) {
			return; // only modify vanilla chest loot tables
		}
		// Add skill orbs as an additional pool to bonus chest; other chests add to existing pool
		if (this.skillOrbs != null) {
			if (event.getName().equals(LootTableList.CHESTS_SPAWN_BONUS_CHEST)) {
				event.getTable().addPool(this.skillOrbsPool);
			} else {
				event.getTable().getPool("main").addEntry(this.skillOrbs);
			}
		}
		// Add skill swords with randomized skills to chest loot
		if (Config.areRandomSwordsEnabled()) {
			if (event.getName().equals(LootTableList.CHESTS_SPAWN_BONUS_CHEST)) {
				event.getTable().addPool(this.skillSwordsPool);
			} else {
				for (LootEntry entry : this.skillSwords) {
					event.getTable().getPool("main").addEntry(entry);
				}
			}
		}
	}

	/**
	 * Creates a LootEntry of the given item using the item's registry name as the loot entry name.
	 */
	public static LootEntry createLootEntry(Item item, int weight, int quality, LootFunction[] functions) {
		return createLootEntry(item, weight, quality, functions, new LootCondition[0]);
	}

	/**
	 * Creates a LootEntry of the given item using the item's registry name as the loot entry name.
	 */
	public static LootEntry createLootEntry(Item item, int weight, int quality, LootFunction[] functions, LootCondition[] conditions) {
		return new LootEntryItem(item, weight, quality, functions, conditions, item.getRegistryName().toString());
	}
}
