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

package dynamicswordskills.loot.functions;

import java.util.Random;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSyntaxException;

import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.item.ItemStack;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

/**
 * 
 * Sets item metadata to the id of a random enabled skill
 * or to that of one specified by name in the JSON file.
 *
 */
public class SetSkillMetadata extends SkillFunction
{
	public SetSkillMetadata() {
		super();
	}

	public SetSkillMetadata(LootCondition[] conditions) {
		super(conditions);
	}

	public SetSkillMetadata(LootCondition[] conditions, String skill_name) {
		super(conditions, skill_name);
	}

	@Override
	public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
		SkillBase skill = getSkill(rand);
		if (skill != null) {
			stack.setItemDamage(skill.getId());
		} else {
			DynamicSwordSkills.logger.warn("Failed to generate a random, enabled skill");
			stack.stackSize = 0; // invalidate loot stack
		}
		return stack;
	}

	public static class Serializer extends LootFunction.Serializer<SetSkillMetadata>
	{
		public Serializer() {
			super(new ResourceLocation(ModInfo.ID, "gen_random_skill_meta"), SetSkillMetadata.class);
		}
		@Override
		public void serialize(JsonObject json, SetSkillMetadata instance, JsonSerializationContext context) {
			if (instance.skill_name != null) {
				SkillBase skill = SkillBase.getSkillByName(instance.skill_name);
				if (skill == null) {
					throw new JsonSyntaxException("Unknown skill '" + instance.skill_name + "'");
				}
				json.addProperty("skill_name", instance.skill_name);
			}
		}
		@Override
		public SetSkillMetadata deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
			if (json.has("skill_name")) {
				String name = JsonUtils.getString(json, "skill_name");
				SkillBase skill = SkillBase.getSkillByName(name);
				if (skill == null) {
					throw new JsonSyntaxException("Unknown skill '" + name + "'");
				}
				return new SetSkillMetadata(conditions, name);
			}
			return new SetSkillMetadata(conditions);
		}
	}
}
