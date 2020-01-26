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

package dynamicswordskills.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.collect.Sets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.IComboDamage;
import dynamicswordskills.api.IMetadataSkillItem;
import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.api.SkillRegistry;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.SyncPlayerInfoPacket;
import dynamicswordskills.network.client.SyncSkillPacket;
import dynamicswordskills.network.server.ApplySkillModifierPacket;
import dynamicswordskills.network.server.SyncDisabledSkillsPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.IComboSkill;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.IModifiableSkill;
import dynamicswordskills.skills.ISkillModifier;
import dynamicswordskills.skills.MortalDraw;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;
import dynamicswordskills.skills.Skills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerFlyableFallEvent;

public class DSSPlayerInfo implements IExtendedEntityProperties
{
	private final static String EXT_PROP_NAME = "DSSPlayerInfo";

	/** Maximum time the player may be prevented from taking a left- or right-click action */
	private final static int MAX_CLICK_COOLDOWN = 50;

	private final EntityPlayer player;

	/** Time remaining until player may perform another right-click action, such as blocking with a shield */
	private int useItemCooldown;

	/** Stores information on the player's skills */
	private final Map<Byte, SkillBase> skills;

	/** List of user-disabled skill IDs */
	private Set<Byte> disabledSkillIds = Sets.<Byte>newHashSet();

	/** Reference to last active {@link IComboSkill} */
	private IComboSkill comboSkill = null;

	/** Flag to set comboSkill to null next update cycle, allowing combo HUD to receive final update packet */
	private boolean invalidateCombo;

	/** Reference to last active ILockOnTarget skill */
	private ILockOnTarget targetingSkill = null;

	/** Skill instance provided by currently held {@link ISkillProvider}, if any */
	private SkillBase itemSkill = null;

	/** Stores the last held ItemStack that was checked for {@link ISkillProvider} */
	private ItemStack lastCheckedStack = null;

	/** A dummy version of Basic Sword skill provided by an {@link ISkillProvider} when the player's skill level is 0 */
	private SkillBase dummySwordSkill = null;

	/**
	 * Currently animating skill that {@link SkillActive#hasAnimation() has an animation};
	 * it may or may not currently be {@link SkillActive#isAnimating() animating}
	 */
	@SideOnly(Side.CLIENT)
	private SkillActive animatingSkill;

	/** Whether the player has received the starting bonus gear or not yet */
	private boolean receivedGear = false;

	/** Used by certain skills for controlling the player's main arm rendering; set to 0.0F for vanilla behavior */
	public float swingProgress = 0.0F;

	/** Used by certain skills for controlling the player's main arm rendering; set to 0.0F for vanilla behavior */
	public float prevSwingProgress = 0.0F;

	public DSSPlayerInfo(EntityPlayer player) {
		this.player = player;
		this.skills = new HashMap<Byte, SkillBase>(SkillRegistry.getValues().size());
	}

	@Override
	public void init(Entity entity, World world) {}

	/**
	 * True if the player can perform a left-click action (i.e. the action timer is zero)
	 */
	public boolean canAttack() {
		return player.attackTime == 0 || player.capabilities.isCreativeMode;
	}

	/**
	 * Returns the current amount of time remaining before a left-click action may be performed
	 */
	public int getAttackTime() {
		return player.attackTime;
	}

	/**
	 * Sets the number of ticks remaining before another action may be performed, but
	 * no less than the current value and no more than MAX_ATTACK_DELAY.
	 */
	public void setAttackCooldown(int ticks) {
		player.attackTime = MathHelper.clamp_int(ticks, player.attackTime, MAX_CLICK_COOLDOWN);
	}

	/**
	 * True if the player can perform a right-click action (i.e. the action timer is zero)
	 */
	public boolean canUseItem() {
		return useItemCooldown == 0 || player.capabilities.isCreativeMode;
	}

	/**
	 * Returns the current amount of time remaining before a right-click action may be performed
	 */
	public int getUseItemCooldown() {
		return useItemCooldown;
	}

	/**
	 * Sets the number of ticks remaining before another right-click action may be performed, but
	 * no less than the current value and no more than MAX_ATTACK_DELAY.
	 */
	public void setUseItemCooldown(int ticks) {
		this.useItemCooldown = MathHelper.clamp_int(ticks, useItemCooldown, MAX_CLICK_COOLDOWN);
	}

	/**
	 * Sets the current and previous arm swing amount; used by some skills for rendering the player's arm position
	 * @param current  See {@link #swingProgress}
	 * @param previous See {@link #prevSwingProgress}
	 */
	public void setArmSwingProgress(float current, float previous) {
		this.swingProgress = current;
		this.prevSwingProgress = previous;
	}

	/**
	 * Removes the skill with the given name, or "all" skills
	 * @param name	ResourceLocation string or "all" to remove all skills
	 * @return		False if no skill was removed
	 */
	public boolean removeSkill(String name) {
		if (("all").equals(name)) {
			resetSkills();
			return true;
		} else {
			SkillBase dummy = null;
			for (SkillBase skill : skills.values()) {
				if (skill.getRegistryName().toString().equals(name)) {
					dummy = skill;
					break;
				}
			}
			if (dummy != null) {
				removeSkill(dummy);
				return true;
			}
		}
		return false;
	}

	private void removeSkill(SkillBase skill) {
		SkillBase dummy = skill.newInstance();
		skills.put(dummy.getId(), dummy);
		validateSkills();
		skills.remove(dummy.getId());
		if (player instanceof EntityPlayerMP) {
			PacketDispatcher.sendTo(new SyncSkillPacket(dummy), (EntityPlayerMP) player);
		}
	}

	/**
	 * Resets all data related to skills
	 */
	public void resetSkills() {
		// need level zero skills for validation, specifically for attribute-affecting skills
		for (SkillBase skill : SkillRegistry.getValues()) {
			skills.put(skill.getId(), skill.newInstance());
		}
		validateSkills();
		skills.clear();
		if (player instanceof EntityPlayerMP) {
			PacketDispatcher.sendTo(new SyncPlayerInfoPacket(this).setReset(), (EntityPlayerMP) player);
		}
	}

	/** Returns true if the player has at least one level in the specified skill */
	public boolean hasSkill(SkillBase skill) {
		return getSkillLevel(skill) > 0;
	}

	/**
	 * Returns the player's skill level for given skill, or 0 if the player doesn't have that skill
	 */
	public byte getSkillLevel(SkillBase skill) {
		byte level = 0;
		if (skill == null) {
			return 0;
		} else if (skill.is(itemSkill)) {
			level = itemSkill.getLevel();
		} else if (skill.is(dummySwordSkill)) {
			level = dummySwordSkill.getLevel();
		}
		return (byte) Math.max(level, getTrueSkillLevel(skill));
	}

	/** Returns the player's true skill level, ignoring any {@link ISkillProvider} that may be equipped */
	public byte getTrueSkillLevel(SkillBase skill) {
		return (skills.containsKey(skill.getId()) ? skills.get(skill.getId()).getLevel() : 0);
	}

	/**
	 * Returns true if the player can currently use the skill; i.e. the player has the skill
	 * and {@link SkillActive#canUse(EntityPlayer) skill.canUse} returns true. This implies
	 * that the skill will trigger successfully if activated.
	 */
	public boolean canUseSkill(SkillBase skill) {
		SkillActive active = getActiveSkill(skill);
		return active != null && active.canUse(player);
	}

	/**
	 * Returns true if the player has the skill and the skill is currently active
	 */
	public boolean isSkillActive(SkillBase skill) {
		SkillBase active = getPlayerSkill(skill);
		return (active instanceof SkillActive && ((SkillActive) active).isActive());
	}

	/**
	 * Returns the {@link #animatingSkill}, which may be null
	 */
	@SideOnly(Side.CLIENT)
	public SkillActive getCurrentlyAnimatingSkill() {
		return animatingSkill;
	}

	/**
	 * This method is called automatically from {@link #onSkillActivated} for each skill activated.
	 * @param skill If this skill {@link SkillActive#hasAnimation has an animation}, it will be set
	 * 				as the currently animating skill.
	 */
	@SideOnly(Side.CLIENT)
	public void setCurrentlyAnimatingSkill(SkillActive skill) {
		animatingSkill = (skill == null || skill.hasAnimation() ? skill : animatingSkill);
	}

	/**
	 * Returns whether key/mouse input and skill interactions are currently allowed,
	 * i.e. the {@link #animatingSkill} is either null or not currently animating
	 */
	@SideOnly(Side.CLIENT)
	public boolean canInteract() {
		return animatingSkill == null || !animatingSkill.isAnimating();
	}

	/**
	 * Called when a key is pressed while a skill is animating (i.e. {@link #canInteract()} returns false);
	 * calls {@link SkillActive#keyPressedWhileAnimating} for the animating skill if {@link SkillActive#isKeyListener} returns true
	 */
	@SuppressWarnings("unchecked")
	@SideOnly(Side.CLIENT)
	public <T extends SkillActive & IModifiableSkill> void onKeyPressedWhileAnimating(Minecraft mc, KeyBinding key) {
		boolean isLockedOn = (targetingSkill != null && targetingSkill.isLockedOn());
		if (animatingSkill != null && animatingSkill.isKeyListener(mc, key, isLockedOn)) {
			animatingSkill.keyPressedWhileAnimating(mc, key, player);
		}
		if (animatingSkill instanceof IModifiableSkill) {
			applyKeyPressSkillModifiers((T) animatingSkill, mc, key);
		}
		if (isLockedOn && targetingSkill instanceof SkillActive && targetingSkill != animatingSkill) {
			if (((SkillActive) targetingSkill).isKeyListener(mc, key, isLockedOn)) {
				((SkillActive) targetingSkill).keyPressedWhileAnimating(mc, key, player);
			}
			if (targetingSkill instanceof IModifiableSkill) {
				applyKeyPressSkillModifiers((T) targetingSkill, mc, key);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@SideOnly(Side.CLIENT)
	private <T extends SkillActive & IModifiableSkill, M extends SkillBase & ISkillModifier> void applyKeyPressSkillModifiers(T parent, Minecraft mc, KeyBinding key) {
		for (SkillBase t : parent.getSkillModifiers()) {
			if (Config.isSkillDisabled(player, t)) {
				continue;
			}
			SkillBase instance = this.getPlayerSkill(t);
			if (instance instanceof ISkillModifier && instance.getLevel() > 0 && ((ISkillModifier) instance).applyOnKeyPress(mc, key, player)) {
				parent.applySkillModifier((M) instance, player);
				PacketDispatcher.sendToServer(new ApplySkillModifierPacket(parent, (M) instance));
			}
		}
	}

	/**
	 * Call when a key is pressed to pass the key press to the player's skills'
	 * {@link SkillActive#keyPressed keyPressed} method, but only if the skill returns
	 * true from {@link SkillActive#isKeyListener isKeyListener} for the key pressed.
	 * The first skill to return true from keyPressed precludes any remaining skills
	 * from receiving the key press.
	 * @return	True if a listening skill's {@link SkillActive#keyPressed} signals that the key press was handled
	 */
	@SideOnly(Side.CLIENT)
	public boolean onKeyPressed(Minecraft mc, KeyBinding key) {
		boolean isLockedOn = (targetingSkill != null && targetingSkill.isLockedOn());
		for (SkillBase skill : skills.values()) {
			if (Config.isSkillDisabled(player, skill)) {
				continue;
			}
			if (skill instanceof SkillActive && ((SkillActive) skill).isKeyListener(mc, key, isLockedOn)) {
				if (((SkillActive) skill).keyPressed(mc, key, player)) {
					return true;
				}
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isKeyListener(mc, key, isLockedOn) && ((SkillActive) itemSkill).keyPressed(mc, key, player)) {
			return true;
		}
		if (dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isKeyListener(mc, key, isLockedOn) && ((SkillActive) dummySwordSkill).keyPressed(mc, key, player)) {
			return true;
		}
		return false;
	}

	/**
	 * Call when a key is released to pass that information to each skills' {@link SkillActive#keyReleased keyReleased} method
	 */
	@SideOnly(Side.CLIENT)
	public void onKeyReleased(Minecraft mc, KeyBinding key) {
		boolean isLockedOn = (targetingSkill != null && targetingSkill.isLockedOn());
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isKeyListener(mc, key, isLockedOn)) {
				((SkillActive) skill).keyReleased(mc, key, player);
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isKeyListener(mc, key, isLockedOn)) {
			((SkillActive) itemSkill).keyReleased(mc, key, player);
		}
		if (dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isKeyListener(mc, key, isLockedOn)) {
			((SkillActive) dummySwordSkill).keyReleased(mc, key, player);
		}
	}

	/**
	 * Called from LivingAttackEvent to trigger {@link SkillActive#onAttack} for each
	 * currently active skill, potentially canceling the event. If the event is canceled, it
	 * returns immediately without processing any remaining active skills.
	 */
	public void onAttack(LivingAttackEvent event) {
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isActive() && ((SkillActive) skill).onAttack(player, event.entityLiving, event.source, event.ammount)) {
				event.setCanceled(true);
				return;
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			event.setCanceled(((SkillActive) itemSkill).onAttack(player, event.entityLiving, event.source, event.ammount));
		}
		if (!event.isCanceled() && dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isActive()) {
			event.setCanceled(((SkillActive) dummySwordSkill).onAttack(player, event.entityLiving, event.source, event.ammount));
		}
	}

	/**
	 * Called from LivingAttackEvent to trigger {@link SkillActive#onBeingAttacked} for each
	 * currently active skill, potentially canceling the event. If the event is canceled, it
	 * returns immediately without processing any remaining active skills.
	 */
	public void onBeingAttacked(LivingAttackEvent event) {
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isActive() && ((SkillActive) skill).onBeingAttacked(player, event.source)) {
				event.setCanceled(true);
				return;
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			((SkillActive) itemSkill).onBeingAttacked(player, event.source);
			event.setCanceled(((SkillActive) itemSkill).onBeingAttacked(player, event.source));
		}
		if (!event.isCanceled() && dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isActive()) {
			event.setCanceled(((SkillActive) dummySwordSkill).onBeingAttacked(player, event.source));
		}
	}

	/**
	 * Called from LivingHurtEvent to trigger {@link SkillActive#onImpact} for each
	 * currently active skill, potentially altering the value of event.amount
	 */
	public void onImpact(LivingHurtEvent event) {
		for (SkillBase skill : skills.values()) {
			if (event.isCanceled() || event.ammount <= 0.0F) {
				return;
			} else if (skill instanceof SkillActive && ((SkillActive) skill).isActive()) {
				event.ammount = ((SkillActive) skill).onImpact(player, event.entityLiving, event.ammount);
			}
		}
		if (!event.isCanceled() && event.ammount > 0.0F && itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			event.ammount = (((SkillActive) itemSkill).onImpact(player, event.entityLiving, event.ammount));
		}
		if (!event.isCanceled() && event.ammount > 0.0F && dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isActive()) {
			event.ammount = ((SkillActive) dummySwordSkill).onImpact(player, event.entityLiving, event.ammount);
		}
	}

	/**
	 * Calls {@link SkillActive#postImpact} for each currently active skill,
	 * as well as calling {@link IComboSkill#onHurtTarget} for the current ICombo.
	 */
	public void onPostImpact(LivingHurtEvent event) {
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isActive()) {
				((SkillActive) skill).postImpact(player, event.entityLiving, event.ammount);
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			((SkillActive) itemSkill).postImpact(player, event.entityLiving, event.ammount);
		}
		if (dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isActive()) {
			((SkillActive) dummySwordSkill).postImpact(player, event.entityLiving, event.ammount);
		}
		// combo gets updated last, after all damage modifications are completed
		if (!event.isCanceled() && event.ammount > 0.0F && getComboSkill() != null && ((SkillActive) getComboSkill()).isActive()) {
			if (!(event.source instanceof IComboDamage) || ((IComboDamage) event.source).isComboDamage(player)) {
				getComboSkill().onHurtTarget(player, event);
			}
		}
	}

	/**
	 * Called from LivingFallEvent to trigger {@link SkillActive#onFall} for each currently active skill
	 */
	public void onFall(LivingFallEvent event) {
		for (SkillBase skill : skills.values()) {
			if (event.isCanceled() || event.distance <= 0.0F) {
				return;
			} else if (skill instanceof SkillActive && ((SkillActive) skill).isActive()) {
				if (((SkillActive) skill).onFall(player, event)) {
					return;
				}
			}
		}
		if (!event.isCanceled() && event.distance > 0.0F && itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			if (((SkillActive) itemSkill).onFall(player, event)) {
				return;
			}
		}
		if (!event.isCanceled() && event.distance > 0.0F && dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isActive()) {
			if (((SkillActive) dummySwordSkill).onFall(player, event)) {
				return;
			}
		}
	}

	/**
	 * Called from PlayerFlyableFallEvent to trigger {@link SkillActive#onCreativeFall} for each currently active skill
	 */
	public void onCreativeFall(PlayerFlyableFallEvent event) {
		for (SkillBase skill : skills.values()) {
			if (event.distance <= 0.0F) {
				return;
			} else if (skill instanceof SkillActive && ((SkillActive) skill).isActive()) {
				if (((SkillActive) skill).onCreativeFall(player, event)) {
					return;
				}
			}
		}
		if (event.distance > 0.0F && itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			if (((SkillActive) itemSkill).onCreativeFall(player, event)) {
				return;
			}
		}
		if (event.distance > 0.0F && dummySwordSkill instanceof SkillActive && ((SkillActive) dummySwordSkill).isActive()) {
			if (((SkillActive) dummySwordSkill).onCreativeFall(player, event)) {
				return;
			}
		}
	}

	/**
	 * Checks for the first Mortal Draw-eligible sword item in the player's hotbar,
	 * setting {@link #itemSkill} and {@link #dummySwordSkill} accordingly.
	 * @return true if the eligible item is or will be providing the Mortal Draw or Targeting skill
	 */
	private boolean retrieveDummySwordSkill() {
		int swordSlot = MortalDraw.getSwordSlot(player);
		if (swordSlot > -1) {
			ItemStack stack = player.inventory.getStackInSlot(swordSlot);
			if (stack != null && stack.getItem() instanceof ISkillProvider) {
				boolean flag = false;
				boolean needsDummy = (getTrueSkillLevel(Skills.swordBasic) < 1);
				if (needsDummy && ((ISkillProvider) stack.getItem()).grantsBasicSwordSkill(stack)) {
					flag = true;
					if (dummySwordSkill == null) {
						dummySwordSkill = SkillBase.createLeveledSkill(Skills.swordBasic, (byte) 1);
					}
				}
				byte plvl = getTrueSkillLevel(Skills.mortalDraw);
				SkillBase skill = SkillBase.getSkillFromItem(stack, (ISkillProvider) stack.getItem());
				if (Skills.mortalDraw.is(skill) && skill.getLevel() > plvl) {
					flag = true;
					if (itemSkill == null || !skill.equals(itemSkill)) {
						itemSkill = skill;
					}
				}
				// Found item is providing targeting skill but not mortal draw while held item is null
				if (flag && !Skills.mortalDraw.is(itemSkill)) {
					itemSkill = null;
				}
				return flag;
			}
		}
		return false;
	}

	/**
	 * Returns a SkillActive version of the player's actual skill instance,
	 * or null if the player doesn't have the skill or it is not the correct type
	 */
	@Nullable
	public SkillActive getActiveSkill(@Nullable SkillBase skill) {
		SkillBase active = getPlayerSkill(skill);
		return (active instanceof SkillActive ? (SkillActive) active : null);
	}

	/**
	 * Returns the skill instance for actual use, whether from the player or an {@link ISkillProvider} or null 
	 */
	@Nullable
	public SkillBase getPlayerSkill(@Nullable SkillBase skill) {
		if (skill == null) {
			return null;
		} else if (skill.is(itemSkill)) {
			return itemSkill;
		} else if (skill.is(dummySwordSkill)) {
			return dummySwordSkill;
		} else {
			return getTruePlayerSkill(skill);
		}
	}

	/**
	 * Returns the player's actual skill instance or null if the player doesn't have the skill 
	 */
	@Nullable
	public SkillBase getTruePlayerSkill(SkillBase skill) {
		return (skills.containsKey(skill.getId()) ? skills.get(skill.getId()) : null);
	}

	/**
	 * Returns first {@link IComboSkill} from a currently active skill, if any, or the last active one;
	 * Combo skill may no longer be active and a combo may or may not be in progress
	 */
	public IComboSkill getComboSkill() {
		if (comboSkill == null || comboSkill.getCombo() == null || !((SkillActive) comboSkill).isActive()) {
			IComboSkill combo = getFirstActiveComboSkill();
			if (combo != null) {
				comboSkill = combo;
				invalidateCombo = false;
			} else if (comboSkill != null && !comboSkill.isComboInProgress()) {
				invalidateCombo = true;
			}
		}
		return comboSkill;
	}

	/**
	 * Returns the first active {@link IComboSkill} instance, if any; combo may or may not be in progress
	 */
	private IComboSkill getFirstActiveComboSkill() {
		for (SkillBase skill : SkillRegistry.getValues()) {
			if (skill instanceof IComboSkill && skill instanceof SkillActive) {
				SkillBase instance = getPlayerSkill(skill);
				if (instance != null && ((SkillActive) instance).isActive()) {
					return (IComboSkill) instance;
				}
			}
		}
		return null;
	}

	/**
	 * Activates the first found targeting skill unless another is already active
	 */
	public void activateTargetingSkill() {
		if (getTargetingSkill() != null) {
			return;
		}
		for (SkillBase skill : SkillRegistry.getValues()) {
			if (skill instanceof ILockOnTarget && skill instanceof SkillActive) {
				SkillBase instance = getPlayerSkill(skill);
				if (instance != null && ((SkillActive) instance).activate(player)) {
					break;
				}
			}
		}
	}

	/**
	 * Deactivates the currently active targeting skill, if any
	 */
	public void deactivateTargetingSkill() {
		getTargetingSkill();
		if (targetingSkill != null) {
			((SkillActive) targetingSkill).deactivate(player);
		}
	}

	/**
	 * Returns the first active ILockOnTarget skill instance, if any
	 */
	public ILockOnTarget getTargetingSkill() {
		if (targetingSkill == null || !((SkillActive) targetingSkill).isActive()) {
			targetingSkill = getFirstActiveTargetingSkill();
		}
		return targetingSkill;
	}

	/**
	 * Returns the first active ILockOnTarget instance, if any
	 */
	private ILockOnTarget getFirstActiveTargetingSkill() {
		for (SkillBase skill : SkillRegistry.getValues()) {
			if (skill instanceof ILockOnTarget && skill instanceof SkillActive) {
				SkillBase instance = getPlayerSkill(skill);
				if (instance != null && ((SkillActive) instance).isActive()) {
					return (ILockOnTarget) instance;
				}
			}
		}
		return null;
	}

	/** Grants a skill with target level of current skill level plus one */
	public boolean grantSkill(SkillBase skill) {
		return grantSkill(skill, (byte)(getTrueSkillLevel(skill) + 1));
	}

	/**
	 * Grants skill to player if player meets the requirements; returns true if skill learned
	 */
	public boolean grantSkill(SkillBase skill, byte targetLevel) {
		byte id = skill.getId();
		SkillBase instance = skills.containsKey(id) ? (SkillBase) skills.get(id) : skill.newInstance();
		if (instance.grantSkill(player, targetLevel)) {
			skills.put(id, instance);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Activates the skill if possible
	 * @param wasTriggered Whether the skill was triggered via some means other than direct user interaction (see {@link SkillActive#allowUserActivation})
	 * @return true if the player has this skill and {@link SkillActive#trigger} returns true
	 */
	public boolean activateSkill(SkillBase skill, boolean wasTriggered) {
		return (skill instanceof SkillActive && onSkillActivated((SkillActive) skill, wasTriggered));
	}

	private boolean onSkillActivated(SkillActive skill, boolean wasTriggered) {
		if (skill.trigger(player.worldObj, player, wasTriggered)) {
			if (player.worldObj.isRemote && skill.isActive()) {
				setCurrentlyAnimatingSkill(skill);
			}
			return true;
		}
		return false;
	}

	/**
	 * @return true if the skill has been disabled by the user
	 */
	public final boolean isSkillDisabled(@Nullable SkillBase skill) {
		return skill != null && this.disabledSkillIds.contains(skill.getId());
	}

	/**
	 * @return Set of user-disabled skill IDs
	 */
	public Set<Byte> getDisabledSkillIds() {
		return Collections.unmodifiableSet(this.disabledSkillIds);
	}

	/**
	 * Toggles the skill's per-user disabled state, but does not notify the server.
	 * Calling code should call {@link #syncDisabledSkills()} when finished making changes.
	 */
	@SideOnly(Side.CLIENT)
	public void toggleDisabledSkill(SkillBase skill) {
		if (disabledSkillIds.contains(skill.getId())) {
			disabledSkillIds.remove(skill.getId());
		} else {
			disabledSkillIds.add(skill.getId());
		}
	}

	/**
	 * Sends a packet to update the server side user-disabled skill ID list
	 */
	@SideOnly(Side.CLIENT)
	public void syncDisabledSkills() {
		PacketDispatcher.sendToServer(new SyncDisabledSkillsPacket(this.player));
	}

	/**
	 * Should only be called from {@link SyncDisabledSkillsPacket} to set the server side user-disabled skill ID list
	 */
	public void setDisabledSkills(Set<Byte> disabledIds) {
		this.disabledSkillIds = disabledIds;
		this.validateSkills();
	}

	/**
	 * Updates the local skills map with the skill, removing it if level is < 1.
	 * Called client side only for synchronizing a skill with the server version.
	 */
	@SideOnly(Side.CLIENT)
	public void syncClientSideSkill(SkillBase skill) {
		if (skill != null) {
			if (skill.getLevel() > 0) {
				skills.put(skill.getId(), skill);
			} else {
				skills.remove(skill.getId());
			}
		}
	}

	/**
	 * Call during the render tick to update animating and ILockOnTarget skills
	 */
	@SideOnly(Side.CLIENT)
	public void onRenderTick(float partialRenderTick) {
		// flags whether a skill is currently animating
		boolean flag = false;
		if (animatingSkill != null) {
			if (getSkillLevel(animatingSkill) < 1) {
				// Clear animating skill if no longer valid
				setCurrentlyAnimatingSkill(null);
			} else if (animatingSkill.isAnimating()) {
				// Allow animations to complete even if skill is no longer considered active
				flag = animatingSkill.onRenderTick(player, partialRenderTick);
			} else if (!animatingSkill.isActive()) {
				setCurrentlyAnimatingSkill(null);
			}
		}
		ILockOnTarget skill = getTargetingSkill();
		if (!flag && skill != null && skill.isLockedOn()) {
			((SkillActive) skill).onRenderTick(player, partialRenderTick);
		}
	}

	/**
	 * This method should be called every update tick; currently called from LivingUpdateEvent
	 */
	public void onUpdate() {
		updateISkillProvider();
		if (useItemCooldown > 0) {
			--useItemCooldown;
		}
		if (itemSkill != null) {
			itemSkill.onUpdate(player);
		}
		if (dummySwordSkill != null) {
			dummySwordSkill.onUpdate(player);
		}
		for (SkillBase skill : skills.values()) {
			skill.onUpdate(player);
		}
		if (invalidateCombo) {
			comboSkill = null;
			invalidateCombo = false;
		}
	}

	/**
	 * Updates the current itemSkill and dummySwordSkill based on the player's currently held item
	 */
	private void updateISkillProvider() {
		ItemStack stack = player.getHeldItem();
		// Mortal Draw from skill item requires special handling
		boolean skipUpdate = false;
		if (Skills.mortalDraw.is(itemSkill) && ((SkillActive) itemSkill).isActive()) {
			skipUpdate = true;
		} else if (stack == null) {
			lastCheckedStack = null;
			skipUpdate = retrieveDummySwordSkill();
		}
		if (skipUpdate) {
			// no-op
		} else if (stack != null && stack.getItem() instanceof ISkillProvider) {
			if (stack == lastCheckedStack) {
				return;
			}
			lastCheckedStack = stack;
			ISkillProvider provider = (ISkillProvider) stack.getItem();
			SkillBase skill = SkillBase.getSkillFromItem(stack, provider);
			if (itemSkill == null || !itemSkill.equals(skill)) {
				if (skill.getLevel() > getTrueSkillLevel(skill)) {
					itemSkill = skill;
				} else {
					itemSkill = null;
				}
			}
			if (provider.grantsBasicSwordSkill(stack)) {
				if (dummySwordSkill == null && !skill.is(Skills.swordBasic) && getTrueSkillLevel(Skills.swordBasic) < 1) {
					dummySwordSkill = SkillBase.createLeveledSkill(Skills.swordBasic, (byte) 1);
				}
			} else if (dummySwordSkill != null) {
				dummySwordSkill = null; // held item does not provide basic sword skill
			}
		} else {
			dummySwordSkill = null;
			itemSkill = null;
			lastCheckedStack = null;
		}
	}

	/**
	 * If player has not received starting gear, it is provided
	 */
	public void verifyStartingGear() {
		if (!receivedGear && Config.giveBonusOrb()) {
			int damage = ((IMetadataSkillItem) DynamicSwordSkills.skillOrb).getItemDamage(Skills.swordBasic);
			receivedGear = player.inventory.addItemStackToInventory(new ItemStack(DynamicSwordSkills.skillOrb, 1, damage));
		}
	}

	/** Used to register these extended properties for the player during EntityConstructing event */
	public static final void register(EntityPlayer player) {
		player.registerExtendedProperties(EXT_PROP_NAME, new DSSPlayerInfo(player));
	}

	/** Returns ExtendedPlayer properties for player */
	public static final DSSPlayerInfo get(EntityPlayer player) {
		return (DSSPlayerInfo) player.getExtendedProperties(EXT_PROP_NAME);
	}

	/**
	 * Call when the player logs in for the first time
	 */
	public void onPlayerLoggedIn() {
		verifyStartingGear();
	}

	/**
	 * Call each time the player joins the world to sync data to the client
	 */
	public void onJoinWorld() {
		validateSkills();
		if (player instanceof EntityPlayerMP) {
			PacketDispatcher.sendTo(new SyncPlayerInfoPacket(this), (EntityPlayerMP) player);
		}
	}

	/**
	 * Copies given data to this one
	 */
	public void copy(DSSPlayerInfo info) {
		NBTTagCompound compound = new NBTTagCompound();
		info.saveNBTData(compound);
		this.loadNBTData(compound);
	}

	/**
	 * Validates each skill upon player respawn, ensuring all bonuses are correct
	 */
	public final void validateSkills() {
		for (SkillBase skill : skills.values()) {
			skill.validateSkill(player);
		}
	}

	@Override
	public void saveNBTData(NBTTagCompound compound) {
		NBTTagList taglist = new NBTTagList();
		for (SkillBase skill : skills.values()) {
			taglist.appendTag(skill.writeToNBT());
		}
		compound.setTag("DynamicSwordSkills", taglist);
		compound.setBoolean("receivedGear", receivedGear);
		// User-disabled skills
		NBTTagList disabled = new NBTTagList();
		for (SkillBase skill : SkillRegistry.getValues()) {
			if (disabledSkillIds.contains(skill.getId())) {
				disabled.appendTag(new NBTTagString(skill.getRegistryName().toString()));
			}
		}
		compound.setTag("UserDisabledSkills", disabled);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		skills.clear(); // allows skills to reset on client without re-adding all the skills
		NBTTagList taglist = compound.getTagList("DynamicSwordSkills", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < taglist.tagCount(); ++i) {
			NBTTagCompound tag = taglist.getCompoundTagAt(i);
			SkillBase skill = SkillBase.loadFromNBT(tag);
			if (skill != null) {
				skills.put(skill.getId(), skill);
			}
		}
		receivedGear = compound.getBoolean("receivedGear");
		// User-disabled skills
		disabledSkillIds.clear();
		NBTTagList disabled = compound.getTagList("UserDisabledSkills", Constants.NBT.TAG_STRING);
		for (int i = 0; i < disabled.tagCount(); ++i) {
			String s = disabled.getStringTagAt(i);
			SkillBase skill = SkillRegistry.get(DynamicSwordSkills.getResourceLocation(s));
			if (skill != null) {
				disabledSkillIds.add(skill.getId());
			}
		}
	}
}
