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
import dynamicswordskills.api.ItemRandomSkill;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.JsonToNBT;
import net.minecraft.nbt.NBTException;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.JsonUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.storage.loot.LootContext;
import net.minecraft.world.storage.loot.conditions.LootCondition;
import net.minecraft.world.storage.loot.functions.LootFunction;

/**
 * 
 * Calls {@link ItemRandomSkill#getRandomSkillTag} and sets the ItemStack's
 * NBT Tag accordingly.
 * 
 * If "skill_name" is specified and valid, the generated tag will be for that skill.
 * 
 * If "skill_tag" is specified, that will be used instead of generating a random tag.
 * 
 * JSON Tag Format: "skill_tag": "{ItemSkillName:\"mod_id:skill_name\",ItemSkillLevel:xb,grantsBasicSword:yb}"
 * where ItemSkillLevel 'x' is a number from 1 to max skill level (usually 5) and
 * "grantsBasicSword" is either 0 or 1; both entries are followed by the letter 'b'.
 *
 */
public class RandomSkillSword extends SkillFunction
{
	protected NBTTagCompound skill_tag;

	public RandomSkillSword() {
		super();
	}

	public RandomSkillSword(LootCondition[] conditions) {
		super(conditions);
	}

	public RandomSkillSword(LootCondition[] conditions, String skill_name) {
		super(conditions, skill_name);
	}

	public RandomSkillSword(LootCondition[] conditions, NBTTagCompound tag) {
		super(conditions);
		this.skill_tag = tag;
	}

	@Override
	public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
		SkillBase skill = getSkill(rand);
		if (!(stack.getItem() instanceof ItemRandomSkill)) {
			DynamicSwordSkills.logger.warn("Invalid item for RandomSkillSword function: " + stack.toString());
		} else if (this.skill_tag != null) {
			stack.setTagCompound(this.skill_tag);
		} else if (skill != null) {
			((ItemRandomSkill) stack.getItem()).generateSkillTag(stack, skill, rand);
		} else {
			DynamicSwordSkills.logger.warn("Failed to generate a random, enabled skill");
			stack.stackSize = 0; // invalidate loot stack
		}
		return stack;
	}

	public static class Serializer extends LootFunction.Serializer<RandomSkillSword>
	{
		public Serializer() {
			super(new ResourceLocation(ModInfo.ID, "get_random_skill_sword"), RandomSkillSword.class);
		}
		@Override
		public void serialize(JsonObject json, RandomSkillSword instance, JsonSerializationContext context) {
			if (instance.skill_tag != null) {
				json.addProperty("skill_tag", instance.skill_tag.toString());
			} else if (instance.skill_name != null) {
				SkillBase skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(instance.skill_name));
				if (skill == null) {
					throw new JsonSyntaxException("Unknown skill '" + instance.skill_name + "'");
				}
				json.addProperty("skill_name", instance.skill_name);
			}
		}
		@Override
		public RandomSkillSword deserialize(JsonObject json, JsonDeserializationContext context, LootCondition[] conditions) {
			if (json.has("skill_tag")) {
				try {
					NBTTagCompound tag = JsonToNBT.getTagFromJson(JsonUtils.getString(json, "skill_tag"));
					if (!tag.hasKey("ItemSkillName") || !tag.hasKey("ItemSkillLevel")) {
						throw new JsonSyntaxException("Invalid skill tag; correct format is: {ItemSkillName:\"mod_id:skill_name\",ItemSkillLevel:xb,grantsBasicSword:yb}");
					} else {
						String name = tag.getString("ItemSkillName");
						if (SkillRegistry.get(DynamicSwordSkills.getResourceLocation(name)) == null) {
							throw new JsonSyntaxException("Unknown skill '" + name + "'");
						}
					}
					return new RandomSkillSword(conditions, tag);
				} catch (NBTException e) {
					throw new JsonSyntaxException(e);
				}
			} else if (json.has("skill_name")) {
				String name = JsonUtils.getString(json, "skill_name");
				SkillBase skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(name));
				if (skill == null) {
					throw new JsonSyntaxException("Unknown skill '" + name + "'");
				}
				return new RandomSkillSword(conditions, name);
			}
			return new RandomSkillSword(conditions);
		}
	}
}
