package dynamicswordskills.api;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.SkillBase;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

/**
 * 
 * Recommended for each mod that adds skills to create a SkillGroup with the mod's ID as the label,
 * and to ensure each of that mod's skills is included in that group. This allows the mod's skill to
 * appear in the GUI when first installed even if the user's SkillGroup config has already generated.
 *
 */
public class SkillGroup
{
	private static final Map<String, SkillGroup> map = Maps.<String, SkillGroup>newHashMap();

	/** Group label and translation key */
	public final String label;

	/** Groups with higher priority are generally listed first */
	public int priority;

	/** Custom display name string, mainly for user-defined skill groups */
	protected String displayName;

	/** Flag indicating this group uses the standard tooltip format */
	private boolean hasTooltip;

	/**
	 * @param label Skills with matching group labels will be grouped together
	 */
	public SkillGroup(String label) {
		this(label, -1);
	}

	/**
	 * @param label Skills with matching group labels will be grouped together
	 * @param priority Groups with higher priority are generally listed first
	 */
	public SkillGroup(String label, int priority) {
		Preconditions.checkArgument(label != null, "Group label can not be null");
		this.label = label;
		this.priority = priority;
	}

	/**
	 * Registers this group and returns itself or the existing registry instance
	 */
	public SkillGroup register() {
		if (SkillGroup.register(this)) {
			return this;
		}
		return SkillGroup.get(this.label);
	}

	/**
	 * @return The language key for this group's display name
	 */
	public String getTranslationKey() {
		return "skillGroup." + this.label;
	}

	/**
	 * @return The name to display in e.g. client-side GUI screens
	 */
	public String getDisplayName() {
		return (this.displayName == null ? new ChatComponentTranslation(this.getTranslationKey()).getUnformattedText() : this.displayName);
	}

	/**
	 * Sets a custom display name for this group
	 */
	public SkillGroup setDisplayName(String name) {
		this.displayName = name;
		return this;
	}

	/**
	 * Sets the {@link #hasTooltip} flag; standard tooltip returns a single translated string
	 * for the language entry key {@link #getTranslationKey()} + ".tooltip"
	 */
	public SkillGroup setHasTooltip() {
		this.hasTooltip = true;
		return this;
	}

	/**
	 * Tooltip usually contains the group name followed by a short description of the type of skills contained within.
	 * @return List of strings to display; null or empty for no tooltip
	 */
	public List<String> getTooltip() {
		if (!this.hasTooltip) {
			return null;
		}
		return Lists.<String>newArrayList(
				this.getDisplayName(),
				EnumChatFormatting.GRAY + new ChatComponentTranslation(this.getTranslationKey() + ".tooltip").getUnformattedText()
				);
	}

	/**
	 * Returns a list of all skills in this group
	 */
	public final List<SkillBase> getSkills() {
		return getSkills(new Predicate<SkillBase>() {
			@Override
			public boolean test(SkillBase t) {
				return true;
			}
		});
	}

	/**
	 * @return A list of all skills in this group that meet the specified criteria
	 */
	public final List<SkillBase> getSkills(Predicate<SkillBase> filter) {
		List<SkillBase> skills = Lists.<SkillBase>newArrayList();
		for (SkillBase skill : SkillRegistry.getValues()) {
			if (Config.isSkillInGroup(skill, this) && filter.test(skill)) {
				skills.add(skill);
			}
		}
		return skills;
	}

	/**
	 * Override to implement a custom sort order, default sorts by registry name
	 * @param skills The list of skills to display
	 */
	public void sort(List<SkillBase> skills) {
		Collections.sort(skills, SkillRegistry.SORT_BY_REGISTRY_NAME);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.label);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		} else if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		return ((SkillGroup) obj).label.equals(this.label);
	}

	/**
	 * Register a new skill group
	 * @return false if a group with the same label has already been registered
	 */
	private static final boolean register(SkillGroup group) {
		if (map.containsKey(group.label)) {
			return false;
		}
		map.put(group.label, group);
		return true;
	}

	/**
	 * Returns the SkillGroup instance for the given label
	 * Use this to add skills to groups created by other mods, be sure to have a fallback in case of null
	 */
	public static final SkillGroup get(String label) {
		return map.get(label);
	}

	/**
	 * Returns all registered skill groups, sorted by priority and label
	 */
	public static final List<SkillGroup> getAll() {
		List<SkillGroup> groups = Lists.newArrayList(map.values());
		Collections.sort(groups, new Comparator<SkillGroup>() {
			@Override
			public int compare(SkillGroup a, SkillGroup b) {
				return (a == b ? 0 : (a.priority == b.priority ? a.label.compareTo(b.label) : b.priority - a.priority));
			}
		});
		return Collections.unmodifiableList(groups);
	}
}
