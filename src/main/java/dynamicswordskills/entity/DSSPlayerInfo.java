/**
    Copyright (C) <2015> <coolAlias>

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
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.world.World;
import net.minecraftforge.common.IExtendedEntityProperties;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.ISkillProvider;
import dynamicswordskills.client.DSSKeyHandler;
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

	private final EntityPlayer player;

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

	/** Currently active skills */
	private final List<SkillActive> activeSkills = new LinkedList<SkillActive>();

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
	 * Removes the skill with the given name, or "all" skills
	 * @param name	Unlocalized skill name or "all" to remove all skills
	 * @return		False if no skill was removed
	 */
	public boolean removeSkill(String name) {
		if (("all").equals(name)) {
			resetSkills();
			return true;
		} else {
			// TODO change skill storage to use unlocalized name instead of id
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
		return hasSkill(skill.getId());
	}

	/** Returns true if the player has at least one level in the specified skill (of any class) */
	private boolean hasSkill(byte id) {
		return getSkillLevel(id) > 0;
	}

	/** Returns the player's skill level for given skill, or 0 if the player doesn't have that skill */
	public byte getSkillLevel(SkillBase skill) {
		return getSkillLevel(skill.getId());
	}

	/**
	 * Returns the player's skill level for given skill, or 0 if the player doesn't have that skill
	 */
	public byte getSkillLevel(byte id) {
		byte level = 0;
		if (itemSkill != null && itemSkill.getId() == id) {
			level = itemSkill.getLevel();
		} else if (id == SkillBase.swordBasic.getId()) {
			if (player.getHeldItem() == null) {
				retrieveDummySwordSkill();
			}
			if (dummySwordSkill != null) {
				level = dummySwordSkill.getLevel();
			}
		} else if (id == SkillBase.mortalDraw.getId() && (itemSkill == null || dummySwordSkill == null)) {
			for (int i = 0; i < 9; ++i) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (stack != null && stack.getItem() instanceof ISkillProvider &&
						((ISkillProvider) stack.getItem()).getSkillId(stack) == id)
				{
					if (itemSkill == null) {
						itemSkill = SkillBase.getSkillFromItem(stack, (ISkillProvider) stack.getItem());
						if (itemSkill != null && itemSkill.getLevel() > getTrueSkillLevel(id)) {
							level = itemSkill.getLevel();
						}
					}
					if (dummySwordSkill == null && ((ISkillProvider) stack.getItem()).grantsBasicSwordSkill(stack)
							&& getTrueSkillLevel(SkillBase.swordBasic.getId()) < 1)
					{
						dummySwordSkill = SkillBase.createLeveledSkill(SkillBase.swordBasic.getId(), (byte) 1);
						persistentDummySkillSlot = i;
					}
					break;
				}
			}
		}
		return (byte) Math.max(level, getTrueSkillLevel(id));
	}

	/** Returns the player's true skill level, ignoring any ISkillItem that may be equipped */
	public byte getTrueSkillLevel(SkillBase skill) {
		return getTrueSkillLevel(skill.getId());
	}

	/** Returns the player's true skill level, ignoring any ISkillItem that may be equipped */
	private byte getTrueSkillLevel(byte id) {
		return (skills.containsKey(id) ? skills.get(id).getLevel() : 0);
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
		// don't set the current skill to null just yet if it is still animating
		// this allows skills to prevent key/mouse input without having to be 'active'
		if (animatingSkill != null && !animatingSkill.isActive() && !animatingSkill.isAnimating()) {//!isSkillActive(currentActiveSkill)) {
			animatingSkill = null;
		}
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
	 * Called from LivingAttackEvent to trigger {@link SkillActive#onBeingAttacked} for each
	 * currently active skill, potentially canceling the event. If the event is canceled, it
	 * returns immediately without processing any remaining active skills.
	 */
	public void onBeingAttacked(LivingAttackEvent event) {
		for (SkillActive skill : activeSkills) {
			if (skill.isActive() && skill.onBeingAttacked(player, event.source)) {
				event.setCanceled(true);
				return;
			}
		}
		if (itemSkill instanceof SkillActive && ((SkillActive) itemSkill).isActive()) {
			((SkillActive) itemSkill).onBeingAttacked(player, event.source);
		}
	}

	/**
	 * Called from LivingHurtEvent to trigger {@link SkillActive#postImpact} for each
	 * currently active skill, potentially altering the value of event.ammount, as
	 * well as calling {@link ICombo#onHurtTarget onHurtTarget} for the current ICombo.
	 */
	public void onPostImpact(LivingHurtEvent event) {
		for (SkillActive skill : activeSkills) {
			if (skill.isActive()) {
				event.ammount = skill.postImpact(player, event.entityLiving, event.ammount);
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
	private void retrieveDummySwordSkill() {
		boolean needsDummy = (getTrueSkillLevel(SkillBase.swordBasic.getId()) < 1 && dummySwordSkill == null);
		if ((needsDummy || itemSkill == null) && persistentDummySkillSlot == -1) {
			for (int i = 0; i < 9; ++i) {
				ItemStack stack = player.inventory.getStackInSlot(i);
				if (stack != null && stack.getItem() instanceof ISkillProvider &&
						((ISkillProvider) stack.getItem()).getSkillId(stack) == SkillBase.mortalDraw.getId())
				{
					if (needsDummy && ((ISkillProvider) stack.getItem()).grantsBasicSwordSkill(stack)) {
						dummySwordSkill = SkillBase.createLeveledSkill(SkillBase.swordBasic.getId(), (byte) 1);
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
		SkillBase active = getPlayerSkill(skill.getId());
		return (active instanceof SkillActive ? (SkillActive) active : null);
	}

	/** Returns the skill instance for actual use, whether from the player or an ISkillItem or null */
	public SkillBase getPlayerSkill(SkillBase skill) {
		return getPlayerSkill(skill.getId());
	}

	/**
	 * Returns the skill instance for actual use, whether from the player or an ISkillItem or null
	 */
	public SkillBase getPlayerSkill(byte id) {
		if (itemSkill != null && itemSkill.getId() == id) {
			return itemSkill;
		} else if (id == SkillBase.swordBasic.getId()) {
			if (player.getHeldItem() == null) {
				retrieveDummySwordSkill();
			}
			return (dummySwordSkill == null ? getTruePlayerSkill(id) : dummySwordSkill);
		} else {
			return getTruePlayerSkill(id);
		}
	}

	/** Returns the player's actual skill instance or null if the player doesn't have the skill */
	public SkillBase getTruePlayerSkill(SkillBase skill) {
		return getTruePlayerSkill(skill.getId());
	}

	/** Returns the player's actual skill instance or null if the player doesn't have the skill */
	private SkillBase getTruePlayerSkill(byte id) {
		return (skills.containsKey(id) ? skills.get(id) : null);
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
		return grantSkill(skill.getId(), (byte)(getSkillLevel(skill) + 1));
	}

	/**
	 * Grants skill to player if player meets the requirements; returns true if skill learned
	 */
	public boolean grantSkill(byte id, byte targetLevel) {
		SkillBase skill = skills.containsKey(id) ? (SkillBase) skills.get(id) : SkillBase.getNewSkillInstance(id);
		if (skill.grantSkill(player, targetLevel)) {
			skills.put(id, skill);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Called after {@link SkillActive#onActivated} returns true to add the skill to the
	 * list of currently active skills, as well as set the currently animating skill
	 */
	private void onSkillActivated(World world, SkillActive skill) {
		if (skill.isActive()) {
			activeSkills.add(skill);
			if (world.isRemote) {
				setCurrentlyAnimatingSkill(skill);
			}
		}
	}

	/**
	 * Returns true if the player has this skill and {@link SkillActive#activate} returns true
	 */
	public boolean activateSkill(World world, SkillBase skill) {
		return activateSkill(world, skill.getId());
	}

	/**
	 * Returns true if the player has this skill and {@link SkillActive#activate} returns true
	 */
	public boolean activateSkill(World world, byte id) {
		SkillBase skill = getPlayerSkill(id);
		if (skill instanceof SkillActive && ((SkillActive) skill).activate(world, player)) {
			onSkillActivated(world, (SkillActive) skill);
			return true;
		}
		return false;
	}

	/**
	 * Returns true if the player has this skill and {@link SkillActive#trigger} returns true
	 */
	public boolean triggerSkill(World world, SkillBase skill) {
		return triggerSkill(world, skill.getId());
	}

	/**
	 * Returns true if the player has this skill and {@link SkillActive#trigger} returns true
	 */
	public boolean triggerSkill(World world, byte id) {
		SkillBase skill = getPlayerSkill(id);
		if (skill instanceof SkillActive && ((SkillActive) skill).trigger(world, player, true)) {
			onSkillActivated(world, (SkillActive) skill);
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
		if (SkillBase.doesSkillExist(id)) {
			SkillBase skill = SkillBase.getNewSkillInstance(id).loadFromNBT(compound);
			if (skill.getLevel() > 0) {
				skills.put(id, skill);
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
			if (animatingSkill.isAnimating()) {
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
		// must use iterators to avoid concurrent modification exceptions to list
		Iterator<SkillActive> iterator = activeSkills.iterator();
		while (iterator.hasNext()) {
			SkillActive skill = iterator.next();
			if (!skill.isActive()) {
				iterator.remove();
			}
		}
		if (player.worldObj.isRemote) {
			if (DSSKeyHandler.keys[DSSKeyHandler.KEY_BLOCK].getIsKeyPressed() &&
					isSkillActive(SkillBase.swordBasic) && player.getHeldItem() != null)
			{
				Minecraft.getMinecraft().playerController.sendUseItem(player, player.worldObj, player.getHeldItem());
			}
		}
	}

	/**
	 * Updates the current itemSkill and dummySwordSkill based on the player's currently held item
	 */
	private void updateISkillItem() {
		ItemStack stack = player.getHeldItem();
		if (itemSkill != null && itemSkill.getId() == SkillBase.mortalDraw.getId() &&
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
					if (itemSkill.getLevel() <= getTrueSkillLevel(itemSkill.getId())) {
						itemSkill = null;
					}
					if (item.grantsBasicSwordSkill(stack) && skill.getId() != SkillBase.swordBasic.getId()
							&& getTrueSkillLevel(SkillBase.swordBasic.getId()) < 1)
					{
						if (dummySwordSkill == null) {
							dummySwordSkill = SkillBase.createLeveledSkill(SkillBase.swordBasic.getId(), (byte) 1);
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
			NBTTagCompound skillTag = new NBTTagCompound();
			skill.writeToNBT(skillTag);
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
			NBTTagCompound skill = taglist.getCompoundTagAt(i);
			byte id = skill.getByte("id");
			skills.put(id, SkillBase.getSkill(id).loadFromNBT(skill));
		}
		receivedGear = compound.getBoolean("receivedGear");
	}
}
