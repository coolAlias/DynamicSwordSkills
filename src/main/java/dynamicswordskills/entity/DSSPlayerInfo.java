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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.SyncPlayerInfoPacket;
import dynamicswordskills.network.client.SyncSkillPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.skills.ICombo;
import dynamicswordskills.skills.ILockOnTarget;
import dynamicswordskills.skills.SkillActive;
import dynamicswordskills.skills.SkillBase;

public class DSSPlayerInfo implements IExtendedEntityProperties
{
	private final static String EXT_PROP_NAME = "DSSPlayerInfo";

	/** Maximum time the player may be prevented from taking a left- or right-click action */
	private final static int MAX_CLICK_COOLDOWN = 50;

	private final EntityPlayer player;

	/** Time remaining until player may perform another left-click action, such as an attack */
	private int attackTime;

	/** Time remaining until player may perform another right-click action, such as blocking with a shield */
	private int useItemCooldown;

	/** Stores information on the player's skills */
	private final Map<Byte, SkillBase> skills;

	/** Used to temporarily store skill used from ISkillItem */
	private SkillBase itemSkill = null;

	/** Stores the last held ItemStack that was checked for ISkillItem */
	private ItemStack lastCheckedStack = null;

	/** A dummy version of Basic Sword skill for use with ISkillItem skills when player's skill level is 0 */
	private SkillBase dummySwordSkill = null;

	/** Slot of the item providing the persistent dummy sword skill, if any */
	private int persistentDummySkillSlot = -1;

	/**
	 * Currently animating skill that {@link SkillActive#hasAnimation() has an animation};
	 * it may or may not currently be {@link SkillActive#isAnimating() animating}
	 */
	@SideOnly(Side.CLIENT)
	private SkillActive animatingSkill;

	/** Whether the player has received the starting bonus gear or not yet */
	private boolean receivedGear = false;

	/** Reduces fall damage next impact; used for Rising Cut */
	public float reduceFallAmount = 0.0F;

	public DSSPlayerInfo(EntityPlayer player) {
		this.player = player;
		this.skills = new HashMap<Byte, SkillBase>(SkillBase.getNumSkills());
	}

	@Override
	public void init(Entity entity, World world) {}

	/**
	 * True if the player can perform a left-click action (i.e. the action timer is zero)
	 */
	public boolean canAttack() {
		return attackTime == 0 || player.capabilities.isCreativeMode;
	}

	/**
	 * Returns the current amount of time remaining before a left-click action may be performed
	 */
	public int getAttackTime() {
		return attackTime;
	}

	/**
	 * Sets the number of ticks remaining before another action may be performed, but
	 * no less than the current value and no more than MAX_ATTACK_DELAY.
	 */
	public void setAttackCooldown(int ticks) {
		this.attackTime = MathHelper.clamp_int(ticks, attackTime, MAX_CLICK_COOLDOWN);
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
	 * Removes the skill with the given name, or "all" skills
	 * @param name	Unlocalized skill name or "all" to remove all skills
	 * @return		False if no skill was removed
	 */
	public boolean removeSkill(String name) {
		if (("all").equals(name)) {
			resetSkills();
			return true;
		} else {
			SkillBase dummy = null;
			for (SkillBase skill : skills.values()) {
				if (skill.getUnlocalizedName().equals(name)) {
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
		for (SkillBase skill : SkillBase.getSkills()) {
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
		} else if (itemSkill != null && itemSkill.is(skill)) {
			level = itemSkill.getLevel();
		} else if (skill.is(SkillBase.swordBasic)) {
			if (player.getHeldItem() == null) {
				retrieveDummySwordSkill();
			}
			if (dummySwordSkill != null) {
				level = dummySwordSkill.getLevel();
			}
		} else if (skill.is(SkillBase.mortalDraw) && (itemSkill == null || dummySwordSkill == null)) {
			for (int i = 0; i < 9; ++i) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (stack != null && stack.getItem() instanceof ISkillProvider &&
						((ISkillProvider) stack.getItem()).getSkillId(stack) == skill.getId())
				{
					if (itemSkill == null) {
						itemSkill = SkillBase.getSkillFromItem(stack, (ISkillProvider) stack.getItem());
						if (itemSkill != null && itemSkill.getLevel() > getTrueSkillLevel(skill)) {
							level = itemSkill.getLevel();
						}
					}
					if (dummySwordSkill == null && ((ISkillProvider) stack.getItem()).grantsBasicSwordSkill(stack)
							&& getTrueSkillLevel(SkillBase.swordBasic) < 1)
					{
						dummySwordSkill = SkillBase.createLeveledSkill(SkillBase.swordBasic, (byte) 1);
						persistentDummySkillSlot = i;
					}
					break;
				}
			}
		}
		return (byte) Math.max(level, getTrueSkillLevel(skill));
	}

	/** Returns the player's true skill level, ignoring any ISkillItem that may be equipped */
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
	 * Call when a key is pressed to pass the key press to the player's skills'
	 * {@link SkillActive#keyPressed keyPressed} method, but only if the skill returns
	 * true from {@link SkillActive#isKeyListener isKeyListener} for the key pressed.
	 * The first skill to return true from keyPressed precludes any remaining skills
	 * from receiving the key press.
	 * @return	True if a listening skill's {@link SkillActive#keyPressed} signals that the key press was handled
	 */
	@SideOnly(Side.CLIENT)
	public boolean onKeyPressed(Minecraft mc, KeyBinding key) {
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isKeyListener(mc, key)) {
				if (((SkillActive) skill).keyPressed(mc, key, player)) {
					return true;
				}
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isKeyListener(mc, key)) {
			return ((SkillActive) itemSkill).keyPressed(mc, key, player);
		}
		return false;
	}

	/**
	 * Call when a key is released to pass that information to each skills' {@link SkillActive#keyReleased keyReleased} method
	 */
	@SideOnly(Side.CLIENT)
	public void onKeyReleased(Minecraft mc, KeyBinding key) {
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isKeyListener(mc, key)) {
				((SkillActive) skill).keyReleased(mc, key, player);
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isKeyListener(mc, key)) {
			((SkillActive) itemSkill).keyReleased(mc, key, player);
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
	}

	/**
	 * Called from LivingHurtEvent to trigger {@link SkillActive#onImpact} for each
	 * currently active skill, potentially altering the value of event.amount
	 */
	public void onImpact(LivingHurtEvent event) {
		for (SkillBase skill : skills.values()) {
			if (event.ammount <= 0.0F) {
				return;
			} else if (skill instanceof SkillActive && ((SkillActive) skill).isActive()) {
				event.ammount = ((SkillActive) skill).onImpact(player, event.entityLiving, event.ammount);
			}
		}
		if (event.ammount > 0.0F && itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			event.ammount = (((SkillActive) itemSkill).onImpact(player, event.entityLiving, event.ammount));
		}
	}

	/**
	 * Calls {@link SkillActive#postImpact} for each currently active skill,
	 * as well as calling {@link ICombo#onHurtTarget} for the current ICombo.
	 */
	public void onPostImpact(LivingHurtEvent event) {
		for (SkillBase skill : skills.values()) {
			if (skill instanceof SkillActive && ((SkillActive) skill).isActive()) {
				event.ammount = ((SkillActive) skill).postImpact(player, event.entityLiving, event.ammount);
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			event.ammount = ((SkillActive) itemSkill).postImpact(player, event.entityLiving, event.ammount);
		}
		// combo gets updated last, after all damage modifications are completed
		if (getComboSkill() != null) {
			getComboSkill().onHurtTarget(player, event);
		}
	}

	/**
	 * Checks hot bar for an ISkillItem that provides a persistent SwordBasic skill
	 * when the dummy skill is otherwise null; if itemSkill is null, it will also
	 * search for a Mortal Draw skill, using the same item if a dummy is needed
	 */
	public void retrieveDummySwordSkill() {
		boolean needsDummy = (getTrueSkillLevel(SkillBase.swordBasic) < 1 && dummySwordSkill == null);
		if ((needsDummy || itemSkill == null) && persistentDummySkillSlot == -1) {
			for (int i = 0; i < 9; ++i) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (stack != null && stack.getItem() instanceof ISkillProvider &&
						((ISkillProvider) stack.getItem()).getSkillId(stack) == SkillBase.mortalDraw.getId())
				{
					if (needsDummy && ((ISkillProvider) stack.getItem()).grantsBasicSwordSkill(stack)) {
						dummySwordSkill = SkillBase.createLeveledSkill(SkillBase.swordBasic, (byte) 1);
						persistentDummySkillSlot = i;
					}
					if (itemSkill == null) {
						itemSkill = SkillBase.getSkillFromItem(stack, (ISkillProvider) stack.getItem());
					}
					if (!needsDummy || dummySwordSkill != null) {
						break;
					}
				}
			}
			// prevent the for loop from running every single tick when nothing found
			if (dummySwordSkill == null) {
				persistentDummySkillSlot = -30;
			}
		}
	}

	/**
	 * Returns a SkillActive version of the player's actual skill instance,
	 * or null if the player doesn't have the skill or it is not the correct type
	 */
	public SkillActive getActiveSkill(SkillBase skill) {
		SkillBase active = getPlayerSkill(skill);
		return (active instanceof SkillActive ? (SkillActive) active : null);
	}

	/**
	 * Returns the skill instance for actual use, whether from the player or an ISkillItem or null 
	 */
	public SkillBase getPlayerSkill(@Nullable SkillBase skill) {
		if (skill == null) {
			return null;
		} else if (itemSkill != null && itemSkill.is(skill)) {
			return itemSkill;
		} else if (skill.is(SkillBase.spinAttack) && itemSkill != null && itemSkill.is(SkillBase.superSpinAttack)) {
			SkillBase instance = getTruePlayerSkill(skill);
			return (instance == null && !Config.isSpinAttackRequired() ? itemSkill : instance);
		} else if (skill.is(SkillBase.swordBasic)) {
			if (player.getHeldItem() == null) {
				retrieveDummySwordSkill();
			}
			return (dummySwordSkill == null ? getTruePlayerSkill(skill) : dummySwordSkill);
		} else {
			return getTruePlayerSkill(skill);
		}
	}

	/**
	 * Returns the player's actual skill instance or null if the player doesn't have the skill 
	 */
	public SkillBase getTruePlayerSkill(SkillBase skill) {
		return (skills.containsKey(skill.getId()) ? skills.get(skill.getId()) : null);
	}

	/**
	 * Returns first ICombo from a currently active skill, if any; ICombo may or may not be in progress
	 */
	public ICombo getComboSkill() {
		SkillBase skill = getPlayerSkill(SkillBase.swordBasic);
		if (skill != null && (((ICombo) skill).getCombo() != null || ((SkillActive) skill).isActive())) {
			return (ICombo) skill;
		}
		return null;
	}

	/** Returns an ILockOnTarget skill, if any, with preference for currently active skill */
	public ILockOnTarget getTargetingSkill() {
		return (ILockOnTarget) getPlayerSkill(SkillBase.swordBasic);
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
		SkillBase instance = skills.containsKey(id) ? (SkillBase) skills.get(id) : SkillBase.getNewSkillInstance(skill);
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
	 * Reads a SkillBase from stream and updates the local skills map; if the skill
	 * loaded from NBT is level 0, that skill will be removed.
	 * Called client side only for synchronizing a skill with the server version.
	 */
	@SideOnly(Side.CLIENT)
	public void syncClientSideSkill(byte id, NBTTagCompound compound) {
		SkillBase skill = SkillBase.getSkill(id);
		if (skill != null) {
			SkillBase instance = SkillBase.getNewSkillInstance(skill).loadFromNBT(compound);
			if (instance.getLevel() > 0) {
				skills.put(id, instance);
			} else {
				skills.remove(id);
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
			if (!skills.containsKey(animatingSkill.getId()) && itemSkill != animatingSkill) {
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
		updateISkillItem();
		if (attackTime > 0) {
			--attackTime;
		}
		if (useItemCooldown > 0) {
			--useItemCooldown;
		}
		if (itemSkill != null) {
			itemSkill.onUpdate(player);
		}
		if (dummySwordSkill != null) {
			dummySwordSkill.onUpdate(player);
		}
		if (persistentDummySkillSlot < -1) {
			++persistentDummySkillSlot;
		}
		for (SkillBase skill : skills.values()) {
			skill.onUpdate(player);
		}
	}

	/**
	 * Updates the current itemSkill and dummySwordSkill based on the player's currently held item
	 */
	private void updateISkillItem() {
		ItemStack stack = player.getHeldItem();
		if (itemSkill != null && itemSkill.is(SkillBase.mortalDraw) &&
				(stack == null || ((SkillActive) itemSkill).isActive()))
		{
			// do not replace Mortal Draw until it is no longer active
			if (persistentDummySkillSlot > -1 && stack == null) {
				ItemStack dummyStack = player.inventory.getStackInSlot(persistentDummySkillSlot);
				if (dummyStack == null || (!(dummyStack.getItem() instanceof ISkillProvider)) ||
						!SkillBase.getSkillFromItem(dummyStack, (ISkillProvider) dummyStack.getItem()).equals(itemSkill))
				{
					boolean wasFound = false;
					for (int i = 0; i < 9; ++i) {
						ItemStack newStack = player.inventory.getStackInSlot(i);
						if (newStack != null && newStack.getItem() instanceof ISkillProvider &&
								SkillBase.getSkillFromItem(newStack, (ISkillProvider) newStack.getItem()).equals(itemSkill))
						{
							persistentDummySkillSlot = i;
							wasFound = true;
							break;
						}
					}
					if (!wasFound) {
						itemSkill = null;
						dummySwordSkill = null;
						persistentDummySkillSlot = -1;
					}
				}
			}
		} else if (stack != null && stack.getItem() instanceof ISkillProvider) {
			if (stack == lastCheckedStack) {
				return;
			}
			lastCheckedStack = stack;
			ISkillProvider item = (ISkillProvider) stack.getItem();
			SkillBase skill = SkillBase.getSkillFromItem(stack, item);
			if (itemSkill == null || !itemSkill.equals(skill)) {
				itemSkill = skill;
				if (itemSkill != null) {
					if (itemSkill.getLevel() <= getTrueSkillLevel(itemSkill)) {
						itemSkill = null;
					}
					if (item.grantsBasicSwordSkill(stack) && !skill.is(SkillBase.swordBasic)
							&& getTrueSkillLevel(SkillBase.swordBasic) < 1)
					{
						if (dummySwordSkill == null) {
							dummySwordSkill = SkillBase.createLeveledSkill(SkillBase.swordBasic, (byte) 1);
							persistentDummySkillSlot = -1;
						}
					} else {
						dummySwordSkill = null;
						persistentDummySkillSlot = -1;
					}
				}
			}
		} else {
			itemSkill = null;
			dummySwordSkill = null;
			lastCheckedStack = null;
			if (persistentDummySkillSlot > -1) {
				persistentDummySkillSlot = -1;
			}
		}
	}

	/**
	 * If player has not received starting gear, it is provided
	 */
	public void verifyStartingGear() {
		if (!receivedGear && Config.giveBonusOrb()) {
			receivedGear = player.inventory.addItemStackToInventory(
					new ItemStack(DynamicSwordSkills.skillOrb,1,SkillBase.swordBasic.getId()));
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
		if (!player.worldObj.isRemote) {
			verifyStartingGear();
		}
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
			NBTTagCompound skillTag = new NBTTagCompound();
			skill.writeToNBT(skillTag);
			skillTag.setString("id", skill.getUnlocalizedName());
			taglist.appendTag(skillTag);
		}
		compound.setTag("DynamicSwordSkills", taglist);
		compound.setBoolean("receivedGear", receivedGear);
	}

	@Override
	public void loadNBTData(NBTTagCompound compound) {
		skills.clear(); // allows skills to reset on client without re-adding all the skills
		NBTTagList taglist = compound.getTagList("DynamicSwordSkills", Constants.NBT.TAG_COMPOUND);
		for (int i = 0; i < taglist.tagCount(); ++i) {
			NBTTagCompound skillTag = taglist.getCompoundTagAt(i);
			SkillBase skill = null;
			if (skillTag.hasKey("id", Constants.NBT.TAG_BYTE)) {
				skill = SkillBase.getSkill(skillTag.getByte("id"));
			} else {
				skill = SkillBase.getSkillByName(skillTag.getString("id"));
			}
			if (skill != null) {
				skills.put(skill.getId(), skill.loadFromNBT(skillTag));
			}
		}
		receivedGear = compound.getBoolean("receivedGear");
	}
}
