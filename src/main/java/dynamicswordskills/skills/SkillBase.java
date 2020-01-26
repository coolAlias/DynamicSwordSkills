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
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;
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

	/** Placeholder skill icon used by default */
	public static final ResourceLocation DEFAULT_ICON = new ResourceLocation(ModInfo.ID, "textures/skills/default.png");

	/** Icon texture location, if any */
	private ResourceLocation iconLocation = null;

	/** Icon texture resolution */
	private int iconResolution = 16;

	/** Language registry translation key */
	public final String translationKey;

	/** IDs are determined internally and may change between server sessions; do NOT use these for persistent storage */
	private byte id;

	/** Mutable field storing current level for this instance of SkillBase */
	protected byte level = 0;

	/** Contains descriptions for tooltip display */
	private final List<IChatComponent> tooltip = new ArrayList<IChatComponent>();

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
		this.id = skill.id;
		this.iconLocation = skill.iconLocation;
		this.registryName = skill.registryName;
		this.translationKey = skill.translationKey;
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
	public String getDisplayName() {
		return StatCollector.translateToLocalFormatted(getNameTranslationKey());
	}

	/** Returns the translation key prefixed by 'skill.dss.' */
	public String getTranslationKey() {
		return "skill.dss." + translationKey;
	}

	/** Returns the string used to translate this skill's name */
	public String getNameTranslationKey() {
		return getTranslationKey() + ".name";
	}

	/**
	 * Default implementation returns true if the group's label matches the resource domain of this skill's registry name
	 * @return True if the skill should be displayed in the requested group by default; user settings may override this.
	 */
	public boolean displayInGroup(SkillGroup group) {
		return this.getRegistryName() != null && group.label.equalsIgnoreCase(this.getRegistryName().getResourceDomain());
	}

	/**
	 * Called when the player does not have any levels in this skill and the GUI is configured to display unknown skills.
	 * Note that this method is NOT called on the player's actual skill instance, but a dummy version.
	 * @return true to display this skill's actual icon rather than the placeholder icon
	 */
	@SideOnly(Side.CLIENT)
	public boolean showIconIfUnknown(EntityPlayer player) {
		return false;
	}

	/**
	 * Called when the player does not have any levels in this skill and the GUI is configured to display unknown skills.
	 * Note that this method is NOT called on the player's actual skill instance, but a dummy version.
	 * @return true to display this skill's actual display name rather than the placeholder text
	 */
	@SideOnly(Side.CLIENT)
	public boolean showNameIfUnknown(EntityPlayer player) {
		return false;
	}

	/** Return the texture path for the skill's icon */
	public ResourceLocation getIconLocation() {
		return (iconLocation == null ? DEFAULT_ICON : iconLocation);
	}

	/** Sets the texture path for the skill's icon */
	public SkillBase setIconLocation(String location) {
		this.iconLocation = DynamicSwordSkills.getResourceLocation(location);
		return this;
	}

	/** Returns the skill icon's resolution */
	public int getIconResolution() {
		return this.iconResolution;
	}

	/**
	 * Sets the texture resolution for the skill's icon
	 * @param resolution For best results, use either 16, 32, or 64 
	 */
	public SkillBase setIconResolution(int resolution) {
		this.iconResolution = resolution;
		return this;
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

	/** Calls {@link #addTranslatedTooltip(String)} using the default tooltip language key, i.e. {@link #getTranslationKey()} + ".tooltip"  */
	protected final SkillBase addDefaultTooltip() {
		return addTranslatedTooltip(getTranslationKey() + ".tooltip");
	}

	/** Adds a translation component to the skill's tooltip display */
	protected final SkillBase addTranslatedTooltip(String translationKey) {
		return addTooltip(new ChatComponentTranslation(translationKey));
	}

	/** Adds a text component to the skill's tooltip display; note that formatting may be ignored */
	protected final SkillBase addTooltip(IChatComponent component) {
		tooltip.add(component);
		return this;
	}

	/**
	 * @return unformatted tooltip strings with {@link #addInformation additional information} if showing the advanced tooltip 
	 */
	@SideOnly(Side.CLIENT)
	public final List<String> getTooltip(EntityPlayer player, boolean advanced) {
		List<String> desc = new ArrayList<String>(tooltip.size());
		for (IChatComponent s : tooltip) {
			desc.add(s.getUnformattedText());
		}
		if (advanced) {
			addInformation(desc, player);
		}
		return desc;
	}

	/** Add all pertinent traits (damage, range, etc.) to display in advanced tooltips and the skill GUI */
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {}

	/** Returns the translated description of the skill's activation requirements, if any */
	@Nullable
	public String getActivationDisplay() {
		return null;
	}

	/** Returns a translated description of the skill's AoE, using the value provided */
	public String getAreaDisplay(double area) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.area", String.format("%.1f", area));
	}

	/** Returns a translated description of the skill's charge time in ticks, using the value provided */
	public String getChargeDisplay(int chargeTime) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.charge", chargeTime);
	}

	/** Returns a translated description of the skill's damage, using the value provided and with "+" if desired */
	public String getDamageDisplay(float damage, boolean displayPlus) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.damage", (displayPlus ? "+" : ""), String.format("%.1f", damage));
	}

	/** Returns a translated description of the skill's damage, using the value provided and with "+" if desired */
	public String getDamageDisplay(int damage, boolean displayPlus) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.damage", (displayPlus ? "+" : ""), damage);
	}

	/** Returns a translated description of the skill's duration, in ticks or seconds, using the value provided */
	public String getDurationDisplay(int duration, boolean inTicks) {
		String time = (inTicks ? StatCollector.translateToLocal("skill.dss.ticks") : StatCollector.translateToLocal("skill.dss.seconds"));
		return StatCollector.translateToLocalFormatted("skill.dss.info.duration", (inTicks ? duration : duration / 20), time);
	}

	/** Returns a translated description of the skill's exhaustion, using the value provided */
	public String getExhaustionDisplay(float exhaustion) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.exhaustion", String.format("%.2f", exhaustion));
	}

	/** Returns the translated description of the skill's effect (long version) */
	public String getFullDescription() {
		return StatCollector.translateToLocal(getTranslationKey() + ".description");
	}

	/**
	 * Returns the skill's current level / max level
	 * @param simpleMax whether to replace the numerical display with MAX LEVEL when appropriate
	 */
	public String getLevelDisplay(boolean simpleMax) {
		if (simpleMax && level == getMaxLevel()) {
			return StatCollector.translateToLocal("skill.dss.level.max");
		}
		return StatCollector.translateToLocalFormatted("skill.dss.info.level", level, getMaxLevel());
	}

	/** Returns a translated description of the skill's range, using the value provided */
	public String getRangeDisplay(double range) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.range", String.format("%.1f", range));
	}

	/** Returns a translated description of the skill's time limit, using the value provided */
	public String getTimeLimitDisplay(int time) {
		return StatCollector.translateToLocalFormatted("skill.dss.info.time", time);
	}

	/** Returns true if player meets requirements to learn this skill at target level */
	protected boolean canIncreaseLevel(EntityPlayer player, int targetLevel) {
		return ((level + 1) == targetLevel && targetLevel <= getMaxLevel());
	}

	/**
	 * Called each time a skill's level increases; the {@link #level} value has already been incremented when this is called.
	 * Default implementation calls {@link #resetModifiers(EntityPlayer)} - override if leveling up requires more complexity.
	 */
	protected void levelUp(EntityPlayer player) {
		resetModifiers(player);
	}

	/**
	 * Implementations should remove any modifiers applied to the player (e.g. AttributeModifiers) and
	 * reapply them based on the current skill level.
	 * Skill level may be 0 when e.g. removing a skill completely.
	 */
	protected abstract void resetModifiers(EntityPlayer player);

	/**
	 * Calls {@link #resetModifiers(EntityPlayer)} to ensure modifiers are correct upon respawn, skill removal, etc. 
	 */
	public final void validateSkill(EntityPlayer player) {
		byte lvl = this.level;
		if (Config.isSkillDisabled(player, this)) {
			this.level = 0;
		}
		resetModifiers(player);
		this.level = lvl;
	}

	/** Shortcut method to grant skill at current level + 1 */
	public final boolean grantSkill(EntityPlayer player) {
		return grantSkill(player, level + 1);
	}

	/**
	 * Attempts to level up the skill to target level, returning true if skill's level increased (not necessarily to the target level)
	 */
	public final boolean grantSkill(EntityPlayer player, int targetLevel) {
		if (!Config.isSkillAllowed(this)) {
			return false;
		} else if (targetLevel <= level || targetLevel > getMaxLevel()) {
			return false;
		}
		byte oldLevel = level;
		while (level < targetLevel && canIncreaseLevel(player, level + 1)) {
			++level;
			levelUp(player);
		}
		if (Config.isSkillDisabled(player, this)) {
			validateSkill(player);
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
