package dynamicswordskills.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Lists;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.util.ResourceLocation;

public class SkillRegistry
{
	private static final Map<ResourceLocation, SkillBase> map = new HashMap<ResourceLocation, SkillBase>();

	private static final Map<ResourceLocation, ResourceLocation> remap = new HashMap<ResourceLocation, ResourceLocation>();

	/** Map providing original {@link SkillBase#getId()} functionality */
	private static final BiMap<ResourceLocation, Integer> ids = HashBiMap.create();

	/** Counter for integer-based skill IDs */
	private static int index = 0;

	/**
	 * Registers the skill using its registry name; skills must be registered to be usable.
	 * Recommended to register all skills during {@link FMLPreInitializationEvent}.
	 * @param skill Must not be null or have a null registry name
	 * @return The new or existing registered skill instance
	 */
	public static SkillBase register(SkillBase skill) {
		Preconditions.checkArgument(skill != null, "Skill instance can not be null");
		Preconditions.checkArgument(skill.getRegistryName() != null, String.format("Registry name can not be null for %s", skill.getTranslationKey()));
		if (map.containsKey(skill.getRegistryName())) {
			DynamicSwordSkills.logger.error(String.format("Registry name %s is already in use by %s", skill.getRegistryName().toString(), map.get(skill.getRegistryName()).getTranslationKey()));
			return map.get(skill.getRegistryName());
		}
		map.put(skill.getRegistryName(), skill);
		ids.put(skill.getRegistryName(), index);
		index++;
		return skill.onRegistered();
	}

	/**
	 * Registers a remapping pair, use if a skill's registry name is changed
	 * @param _old The old registry name
	 * @param _new The new registry name must have the same resource domain as the old
	 */
	public static void remap(ResourceLocation _old, ResourceLocation _new) {
		Preconditions.checkArgument(_old.getResourceDomain().equals(_new.getResourceDomain()), String.format("Remapping entries must have the same resource domain! Old: %s | New: %s", _old.getResourceDomain(), _new.getResourceDomain()));
		Preconditions.checkArgument(!remap.containsKey(_old), String.format("A remapping entry already exists for %s", _old.getResourceDomain()));
		remap.put(_old, _new);
	}

	public static SkillBase get(ResourceLocation location) {
		SkillBase skill = map.get(location);
		if (skill == null) {
			skill = map.get(remap.get(location));
		}
		return skill;
	}

	public static Set<ResourceLocation> getKeys() {
		return Collections.unmodifiableSet(map.keySet());
	}

	/**
	 * Returns an unmodifiable collection of all registered skills, in no particular order
	 */
	public static Collection<SkillBase> getValues() {
		return Collections.unmodifiableCollection(map.values());
	}

	/**
	 * Returns an unmodifiable sorted list of all registered skills;
	 * if order is not important, use {@link #getValues()} instead.
	 */
	public static List<SkillBase> getSortedList(Comparator<SkillBase> sort) {
		List<SkillBase> skills = Lists.newArrayList(map.values());
		Collections.sort(skills, sort);
		return Collections.unmodifiableList(skills);
	}

	/**
	 * @return -1 instead of null for an invalid skill
	 */
	public static int getSkillId(SkillBase skill) {
		Integer id = ids.get(skill.getRegistryName());
		if (id == null) {
			id = ids.get(remap.get(skill.getRegistryName()));
		}
		return (id == null ? -1 : id);
	}

	public static SkillBase getSkillById(int id) {
		return get(ids.inverse().get(id));
	}

	/**
	 * Sorts skills by order of registration
	 */
	public static final Comparator<SkillBase> SORT_BY_ID = (new Comparator<SkillBase>() {
		@Override
		public int compare(SkillBase a, SkillBase b) {
			return a.getId() - b.getId();
		}
	});

	public static final Comparator<SkillBase> SORT_BY_REGISTRY_NAME = (new Comparator<SkillBase>() {
		@Override
		public int compare(SkillBase a, SkillBase b) {
			return a.getRegistryName().toString().compareTo(b.getRegistryName().toString());
		}
	});
}
