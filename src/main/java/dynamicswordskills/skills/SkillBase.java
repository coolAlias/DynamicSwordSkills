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

package dynamicswordskills.skills;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

import cpw.mods.fml.common.FMLContainer;
import cpw.mods.fml.common.InjectedModContainer;
import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.ModContainer;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.SyncSkillPacket;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import net.minecraftforge.common.util.Constants;

/**
 * 
 * Abstract base skill class provides foundation for both passive and active skills
 * 
 */
public abstract class SkillBase
{
	/** Default maximum skill level */
	public static final byte MAX_LEVEL = 5;

	/** Unique ResourceLocation for this skill */
	private ResourceLocation registryName = null;

	/** Language registry translation key */
	public final String translationKey;

	/** IDs are determined internally and may change between server sessions; do NOT use these for persistent storage */
	private byte id;

	/** Mutable field storing current level for this instance of SkillBase */
	protected byte level = 0;

	/** Contains descriptions for tooltip display */
	private final List<String> tooltip = new ArrayList<String>();

	/**
	 * @param translationKey String used as the language translation key
	 */
	public SkillBase(String translationKey) {
		this.translationKey = translationKey;
	}

	/**
	 * Copy constructor creates a level zero version of the skill
	 */
	protected SkillBase(SkillBase skill) {
		this.registryName = skill.registryName;
		this.translationKey = skill.translationKey;
		this.id = skill.id;
		this.tooltip.addAll(skill.tooltip);
	}

	/**
	 * Sets the registry name and registers this skill to the SkillRegistry
	 * @param registryName
	 * @return The registered skill instance
	 */
	public SkillBase register(String registryName) {
		this.setRegistryName(registryName);
		return SkillRegistry.register(this);
	}

	/**
	 * Called by the SkillRegistry after successfully registering a new skill to set the skill ID
	 */
	public final SkillBase onRegistered() {
		this.id = (byte) SkillRegistry.getSkillId(this);
		return this;
	}

	// Copied from IForgeRegistryEntry
	public SkillBase setRegistryName(String name) {
		if (getRegistryName() != null) {
			throw new IllegalStateException("Attempted to set registry name with existing registry name! New: " + name + " Old: " + getRegistryName());
		}
		int index = name.lastIndexOf(':');
		String oldPrefix = index == -1 ? "" : name.substring(0, index);
		name = index == -1 ? name : name.substring(index + 1);
		ModContainer mc = Loader.instance().activeModContainer();
		String prefix = mc == null || (mc instanceof InjectedModContainer && ((InjectedModContainer)mc).wrappedContainer instanceof FMLContainer) ? ModInfo.ID : mc.getModId().toLowerCase();
		if (!oldPrefix.equals(prefix) && oldPrefix.length() > 0) {
			DynamicSwordSkills.logger.warn("Dangerous alternative prefix `%s` for name `%s`, expected `%s` invalid registry invocation/invalid name?", oldPrefix, name, prefix);
			prefix = oldPrefix;
		}
		this.registryName = new ResourceLocation(prefix, name);
		return this;
	}

	public SkillBase setRegistryName(ResourceLocation name) {
		return this.setRegistryName(name.toString());
	}

	public ResourceLocation getRegistryName() {
		return this.registryName;
	}

	/**
	 * Returns a leveled skill from an ISkillProvider using {@link ISkillProvider#getSkillId(ItemStack)}
	 * and {@link ISkillProvider#getSkillLevel(ItemStack)}, or null if not possible
	 */
	public static final SkillBase getSkillFromItem(final ItemStack stack, final ISkillProvider item) {
		SkillBase skill = SkillRegistry.getSkillById(item.getSkillId(stack));
		return createLeveledSkill(skill, item.getSkillLevel(stack));
	}

	/**
	 * Returns a leveled skill from an id and level, capped at the max level for the skill;
	 * May return null if the id is invalid or level is less than 1
	 */
	public static final SkillBase createLeveledSkill(@Nullable final SkillBase skill, final byte level) {
		if (skill != null && level > 0) {
			SkillBase instance = skill.newInstance();
			instance.level = (level > skill.getMaxLevel() ? skill.getMaxLevel() : level);
			return instance;
		}
		return null;
	}

	/**
	 * Note that mutable objects such as this are not suitable as Map keys
	 */
	@Override
	public int hashCode() {
		return 31 * (31 + id) + level;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null || getClass() != obj.getClass()) {
			return false;
		}
		SkillBase skill = (SkillBase) obj;
		return (skill.id == this.id && skill.level == this.level);
	}

	/**
	 * Use this method instead of equals when level is not relevant to the equality comparison
	 * @return true if this skill is the same as another based solely on {@link #getId()}
	 */
	public boolean is(@Nullable SkillBase skill) {
		return (skill != null && this.getId() == skill.getId());
	}

	/** Return a new instance of the skill with appropriate class type */
	public abstract SkillBase newInstance();

	/** Returns the translated skill name */
	public final String getDisplayName() {
		return StatCollector.translateToLocal(getNameTranslationKey());
	}

	/** Returns the translation key prefixed by 'skill.dss.' */
	public final String getTranslationKey() {
		return "skill.dss." + translationKey;
	}

	/** Returns the string used to translate this skill's name */
	public final String getNameTranslationKey() {
		return getTranslationKey() + ".name";
	}

	/**
	 * Default implementation returns true if the group's label matches the resource domain of this skill's registry name
	 * @return True if the skill should be displayed in the requested group by default; user settings may override this.
	 */
	public boolean displayInGroup(SkillGroup group) {
		return this.getRegistryName() != null && group.label.equalsIgnoreCase(this.getRegistryName().getResourceDomain());
	}

	/** Each skill's ID can be used as a key to retrieve it from the map */
	public final byte getId() {
		return id;
	}

	/** Returns current skill level */
	public final byte getLevel() {
		return level;
	}

	/** Returns max level this skill can reach; override to change */
	public byte getMaxLevel() {
		return MAX_LEVEL;
	}

	/**
	 * Returns the key used by the language file for getting tooltip description n
	 * Language file should contain key "skill.dss.{unlocalizedName}.desc.{label}.n"
	 * @param label the category for the data, usually "tooltip" or "info"
	 * @param n if less than zero, ".n" will not be appended
	 */
	protected final String getInfoString(String label, int n) {
		return getTranslationKey() + ".desc." + label + (n < 0 ? "" : ("." + n));
	}

	/** Adds a single untranslated string to the skill's tooltip display */
	protected final SkillBase addDescription(String string) {
		tooltip.add(string);
		return this;
	}

	/** Adds all entries in the provided list to the skill's tooltip display */
	protected final SkillBase addDescription(List<String> list) {
		for (String s : list) { tooltip.add(s); }
		return this;
	}

	/**
	 * Adds n descriptions to the tooltip using the default 'tooltip' label:
	 * {@link SkillBase#getInfoString(String label, int n) getInfoString}
	 * @param n the number of descriptions to add should be at least 1
	 */
	protected final SkillBase addDescriptions(int n) {
		for (int i = 1; i <= n; ++i) {
			tooltip.add(getInfoString("tooltip", i));
		}
		return this;
	}

	/**
	 * Returns the translated tooltip, possibly with advanced display with player information
	 */
	@SideOnly(Side.CLIENT)
	public final List<String> getTranslatedTooltip(EntityPlayer player, boolean advanced) {
		List<String> desc = new ArrayList<String>(tooltip.size());
		for (String s : tooltip) {
			desc.add(StatCollector.translateToLocal(s));
		}
		if (advanced) {
			addInformation(desc, player);
		}
		return desc;
	}

	/** Returns the translated list containing Strings for tooltip display */
	@SideOnly(Side.CLIENT)
	public final List<String> getDescription() {
		List<String> desc = new ArrayList<String>(tooltip.size());
		for (String s : tooltip) {
			desc.add(StatCollector.translateToLocal(s));
		}
		return desc;
	}

	/** Returns a personalized tooltip display containing info about skill at current level */
	@SideOnly(Side.CLIENT)
	public List<String> getDescription(EntityPlayer player) {
		List<String> desc = getDescription();
		addInformation(desc, player);
		return desc;
	}

	/** Allows subclasses to add descriptions of pertinent traits (damage, range, etc.) */
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {}

	/** Returns the translated description of the skill's activation requirements, if any */
	@Nullable
	public String getActivationDisplay() {
		return null;
	}

	/** Returns a translated description of the skill's AoE, using the value provided */
	public String getAreaDisplay(double area) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.area", String.format("%.1f", area));
	}

	/** Returns a translated description of the skill's charge time in ticks, using the value provided */
	public String getChargeDisplay(int chargeTime) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.charge", chargeTime);
	}

	/** Returns a translated description of the skill's damage, using the value provided and with "+" if desired */
	public String getDamageDisplay(float damage, boolean displayPlus) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.damage", (displayPlus ? "+" : ""), String.format("%.1f", damage));
	}

	/** Returns a translated description of the skill's damage, using the value provided and with "+" if desired */
	public String getDamageDisplay(int damage, boolean displayPlus) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.damage", (displayPlus ? "+" : ""), damage);
	}

	/** Returns a translated description of the skill's duration, in ticks or seconds, using the value provided */
	public String getDurationDisplay(int duration, boolean inTicks) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.duration", (inTicks ? duration : duration / 20),
				(inTicks ? StatCollector.translateToLocal("skill.dss.ticks") : StatCollector.translateToLocal("skill.dss.seconds")));
	}

	/** Returns a translated description of the skill's exhaustion, using the value provided */
	public String getExhaustionDisplay(float exhaustion) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.exhaustion", String.format("%.2f", exhaustion));
	}

	/** Returns the translated description of the skill's effect (long version) */
	public String getFullDescription() {
		return StatCollector.translateToLocal(getTranslationKey() + ".desc.full");
	}

	/**
	 * Returns the skill's current level / max level
	 * @param simpleMax whether to replace the numerical display with MAX LEVEL when appropriate
	 */
	public String getLevelDisplay(boolean simpleMax) {
		return ((simpleMax && level == getMaxLevel()) ? StatCollector.translateToLocal("skill.dss.level.max") :
			StatCollector.translateToLocalFormatted("skill.dss.desc.level", level, getMaxLevel()));
	}

	/** Returns a translated description of the skill's range, using the value provided */
	public String getRangeDisplay(double range) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.range", String.format("%.1f", range));
	}

	/** Returns a translated description of the skill's time limit, using the value provided */
	public String getTimeLimitDisplay(int time) {
		return StatCollector.translateToLocalFormatted("skill.dss.desc.time", time);
	}

	/** Returns true if player meets requirements to learn this skill at target level */
	protected boolean canIncreaseLevel(EntityPlayer player, int targetLevel) {
		return ((level + 1) == targetLevel && targetLevel <= getMaxLevel());
	}

	/** Called each time a skill's level increases; responsible for everything OTHER than increasing the skill's level: applying any bonuses, handling Xp, etc. */
	protected abstract void levelUp(EntityPlayer player);

	/** Recalculates bonuses, etc. upon player respawn; Override if levelUp does things other than just calculate bonuses! */
	public void validateSkill(EntityPlayer player) {
		levelUp(player);
	}

	/** Shortcut method to grant skill at current level + 1 */
	public final boolean grantSkill(EntityPlayer player) {
		return grantSkill(player, level + 1);
	}

	/**
	 * Attempts to level up the skill to target level, returning true if skill's level increased (not necessarily to the target level)
	 */
	public final boolean grantSkill(EntityPlayer player, int targetLevel) {
		if (targetLevel <= level || targetLevel > getMaxLevel()) {
			return false;
		}
		byte oldLevel = level;
		while (level < targetLevel && canIncreaseLevel(player, level + 1)) {
			++level;
			levelUp(player);
		}
		if (!player.worldObj.isRemote && oldLevel < level) {
			PacketDispatcher.sendTo(new SyncSkillPacket(this), (EntityPlayerMP) player);
		}
		return oldLevel < level;
	}

	/** This method should be called every update tick */
	public void onUpdate(EntityPlayer player) {}

	/**
	 * Calls {@link #writeAdditionalData(NBTTagCompound)} with a new tag and appends this skill's registry name and level
	 */
	public final NBTTagCompound writeToNBT() {
		NBTTagCompound tag = new NBTTagCompound();
		this.writeAdditionalData(tag);
		tag.setString("id", this.getRegistryName().toString());
		tag.setByte("level", level);
		return tag;
	}

	/**
	 * Calls {@link #readAdditionalData(NBTTagCompound)} after loading the skill's {@link #level} field from NBT.
	 */
	public void readFromNBT(NBTTagCompound tag) {
		this.level = tag.getByte("level");
		this.readAdditionalData(tag);
	}

	/**
	 * Called from {@link #writeToNBT()} to write additional data to the skill's NBT tag.
	 */
	public void writeAdditionalData(NBTTagCompound tag) {}

	/**
	 * Called from {@link #readFromNBT()} to read additional data from the skill's NBT tag.
	 */
	public void readAdditionalData(NBTTagCompound tag) {}

	/**
	 * Creates a new skill instance of the appropriate type based on the NBT data
	 * and calls {@link #readFromNBT(NBTTagCompound)} prior to returning it.
	 * @return May be null for an invalid NBT tag
	 */
	public static final SkillBase loadFromNBT(NBTTagCompound tag) {
		SkillBase skill = null;
		if (tag.hasKey("id", Constants.NBT.TAG_BYTE)) {
			skill = SkillRegistry.getSkillById(tag.getByte("id"));
		} else {
			String name = tag.getString("id");
			if (name.lastIndexOf(':') == -1) {
				name = ModInfo.ID + ":" + name;
			}
			skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(name));
		}
		if (skill != null) {
			skill = skill.newInstance();
			skill.readFromNBT(tag);
		}
		return skill;
	}
}
