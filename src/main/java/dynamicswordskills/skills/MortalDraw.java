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

import java.util.List;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.client.MortalDrawPacket;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;

/**
 * 
 * MORTAL DRAW
 * Activation: While empty-handed and locked on, hold the block key and attack
 * Effect: The art of drawing the sword, or Battoujutsu, is a risky but deadly move, capable
 * of inflicting deadly wounds on unsuspecting opponents with a lightning-fast blade strike
 * Exhaustion: 3.0F - (0.2F * level)
 * Damage: If successful, inflicts damage + (damage * multiplier)
 * Duration: Window of attack opportunity is (level + 2) ticks
 * Notes:
 * - The first sword found in the action bar will be used for the strike; plan accordingly
 * - There is a 1.5s cooldown between uses, representing re-sheathing of the sword
 *
 */
public class MortalDraw extends SkillActive
{
	/** Delay before skill can be used again */
	private static final int DELAY = 30;

	/** The time remaining during which the skill will succeed; also used as animation flag */
	private int attackTimer;

	/** Nearest sword slot index */
	private int swordSlot;

	/**
	 * The entity that attacked and was successfully drawn against the first time
	 * Used to continue canceling the attack event until mortal draw has finished executing
	 */
	private Entity target;

	public MortalDraw(String translationKey) {
		super(translationKey);
	}

	private MortalDraw(MortalDraw skill) {
		super(skill);
	}

	@Override
	public MortalDraw newInstance() {
		return new MortalDraw(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.SWORD_GROUP || group == Skills.TARGETED_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getDamageDisplay(100, true) + "%");
		desc.add(getTimeLimitDisplay(getAttackTime() - DELAY));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		// subtract 2 to allow short window in which still considered active so that
		// the attacker defended against is not able to immediately damage the defender
		return attackTimer > DELAY - 2;
	}

	/**
	 * Animation flag should continue for a few ticks after the skill finishes to
	 * prevent immediate switching of weapons or attacking (it just 'feels' better)
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return attackTimer > (DELAY - 5);
	}

	@Override
	protected float getExhaustion() {
		return 3.0F - (0.2F * level);
	}

	/**
	 * The number of ticks for which this skill is considered 'active', plus the DELAY
	 * Set the attackTimer to this amount upon activation.
	 */
	private int getAttackTime() {
		return level + DELAY + 2;
	}

	/** Returns the amount by which damage will be increased, as a percent: [damage + (damage * x)] */
	private int getDamageMultiplier() {
		return 100 + (10 * level);
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		swordSlot = -1;
		if (super.canUse(player) && player.getHeldItem() == null && attackTimer == 0) {
			swordSlot = getSwordSlot(player);
		}
		return swordSlot > -1;
	}

	/**
	 * Returns the inventory slot index for the first Mortal Draw-eligible sword item in the player's hotbar
	 * @return 0-8 or -1 if no eligible sword was found
	 */
	public static int getSwordSlot(EntityPlayer player) {
		int plvl = DSSPlayerInfo.get(player).getTrueSkillLevel(Skills.mortalDraw);
		boolean needsDummy = (DSSPlayerInfo.get(player).getTrueSkillLevel(Skills.swordBasic) < 1);
		for (int i = 0; i < 9; ++i) {
			ItemStack stack = player.inventory.getStackInSlot(i);
			if (stack != null 
					&& ((plvl > 0 && PlayerUtils.isSword(stack)) || PlayerUtils.isProvider(stack, Skills.mortalDraw))
					&& (!needsDummy || PlayerUtils.isProvider(stack, Skills.swordBasic))
					)
			{
				return i;
			}
		}
		return -1;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return player.getHeldItem() == null && Minecraft.getMinecraft().gameSettings.keyBindUseItem.getIsKeyPressed();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (!isLockedOn) {
			return false;
		}
		return key == mc.gameSettings.keyBindAttack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		return canExecute(player) && activate(player);
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		attackTimer = getAttackTime();
		target = null;
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		attackTimer = 0;
		swordSlot = -1;
		target = null;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (attackTimer > 0) {
			--attackTimer;
			if (attackTimer == DELAY && !player.worldObj.isRemote && player.getHeldItem() == null) {
				drawSword(player, null);
				if (player.getHeldItem() != null) {
					PacketDispatcher.sendTo(new MortalDrawPacket(), (EntityPlayerMP) player);
				}
			}
		}
	}

	@Override
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		if (!player.worldObj.isRemote && source.getEntity() != null) {
			// Changed isActive to return true for an extra 2 ticks to allow canceling damage
			if (target == source.getEntity()) {
				return true;
			} else if (attackTimer > DELAY) {
				if (drawSword(player, source.getEntity())) {
					PacketDispatcher.sendTo(new MortalDrawPacket(), (EntityPlayerMP) player);
					target = source.getEntity();
					return true;
				} else { // failed - do not continue trying
					attackTimer = DELAY;
					target = null;
				}
			}
		}
		return false;
	}

	/**
	 * Call upon landing a mortal draw blow
	 */
	@Override
	public float onImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		// need to check time again, due to 2-tick delay for damage prevention
		if (attackTimer > DELAY) {
			attackTimer = DELAY;
			PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_MORTALDRAW, 0.4F, 0.5F);
			return (amount * (1.0F + ((float) getDamageMultiplier() / 100F)));
		} else { // too late - didn't defend against this target!
			target = null;
		}
		return amount;
	}

	/**
	 * Returns true if the player was able to draw a sword
	 * @return	true if the skill should be triggered (ignored on client)
	 */
	public boolean drawSword(EntityPlayer player, Entity attacker) {
		boolean flag = false;
		// letting this run on both sides is fine - client will sync from server later anyway
		if (swordSlot > -1 && swordSlot != player.inventory.currentItem && player.getHeldItem() == null) {
			ItemStack sword = player.inventory.getStackInSlot(swordSlot);
			if (!player.worldObj.isRemote) {
				player.inventory.setInventorySlotContents(swordSlot, null);
			}
			player.setCurrentItemOrArmor(0, sword);
			// attack will happen before entity#onUpdate refreshes equipment, so apply it now:
			player.getAttributeMap().applyAttributeModifiers(sword.getAttributeModifiers());
			ILockOnTarget skill = DSSPlayerInfo.get(player).getTargetingSkill();
			flag = (skill != null && skill.getCurrentTarget() == attacker);
		}
		swordSlot = -1;
		return flag;
	}
}
