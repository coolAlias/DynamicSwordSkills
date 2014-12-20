/**
    Copyright (C) <2014> <coolAlias>

    This file is part of coolAlias' Zelda Sword Skills Minecraft Mod; as such,
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

import net.minecraft.client.Minecraft;
import net.minecraft.entity.DirtyEntityAccessor;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.lib.Config;
import dynamicswordskills.lib.ModInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;

/**
 * 
 * ARMOR BREAK
 * Description: Unleash a powerful blow that ignores armor
 * Activation: Hold attack for (20 - level) ticks
 * Effect: Unleashes an attack that inflicts normal weapon damage but ignores armor
 * Exhaustion: 2.0F - (0.1F * level)
 * Special: May only be used while locked on to a target
 * 
 * Using this skill performs an attack that ignores armor but otherwise deals exactly the
 * same damage as a normal attack with the given item would, including all bonuses from other
 * skills and enchantments.
 * 
 * Armor Break cannot be activated by normal means. It must be charged by holding the 'attack'
 * key, and once the charge reaches full, the player will perform the Armor Break attack.
 * 
 */
public class ArmorBreak extends SkillActive
{
	/** Set to 1 when triggered; set to 0 when target struck in onImpact() */
	private int activeTimer = 0;

	/** Current charge time */
	private int charge = 0;

	/**
	 * Fixes weapons taking double durability damage from non-canceled mouse event
	 * by letting event be canceled and still allow skill to charge;
	 * must be set and un-set manually on LMB click or release
	 */
	private boolean buttonState = false;

	public ArmorBreak(String name) {
		super(name);
		setDisablesLMB();
		disableUserActivation();
	}

	/** Returns number of ticks required before attack will execute: 20 - level */
	private int getChargeTime(EntityPlayer player) {
		return 20 - level;
	}

	/** Returns true if the skill is still charging up */
	public boolean isCharging(EntityPlayer player) {
		ILockOnTarget target = DSSPlayerInfo.get(player).getTargetingSkill();
		return charge > 0 && target != null && target.isLockedOn();
	}

	private ArmorBreak(ArmorBreak skill) {
		super(skill);
	}

	@Override
	public ArmorBreak newInstance() {
		return new ArmorBreak(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getChargeDisplay(getChargeTime(player)));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return activeTimer > 0;
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isHoldingSkillItem(player);
	}

	@Override
	protected float getExhaustion() {
		return 2.0F - (0.1F * level);
	}

	/**
	 * Called when key pressed or released; initiates or cancels charging
	 * @param state the state of LMB; bound key press should always pass false
	 */
	@SideOnly(Side.CLIENT)
	public void keyPressed(EntityPlayer player, boolean state) {
		buttonState = state;
		charge = (isKeyPressed() ? getChargeTime(player) : 0);
	}

	/** Returns true if skill should continue charging up (key is still held down) */
	@SideOnly(Side.CLIENT)
	private boolean isKeyPressed() {
		return DSSKeyHandler.keys[DSSKeyHandler.KEY_ATTACK].getIsKeyPressed() || (Config.allowVanillaControls() && buttonState);
	}

	@Override
	public boolean trigger(World world, EntityPlayer player) {
		if (super.trigger(world, player)) {
			activeTimer = 1;
			ILockOnTarget skill = DSSPlayerInfo.get(player).getTargetingSkill();
			if (skill != null && skill.isLockedOn()) {
				player.attackTargetEntityWithCurrentItem(skill.getCurrentTarget());
			}
		}

		return isActive();
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (isCharging(player)) {
			if (isKeyPressed() && PlayerUtils.isHoldingSkillItem(player)) {
				if (!player.isSwingInProgress) {
					if (charge < (getChargeTime(player) - 1)) {
						Minecraft.getMinecraft().playerController.sendUseItem(player, player.worldObj, player.getHeldItem());
					}
					--charge;
				}
				if (charge == 0) {
					// can't use the standard disable LMB method, since Armor Break will not return true for isActive
					buttonState = false;
					player.attackTime = 4;
					player.swingItem();
					SwordBasic skill = (SwordBasic) DSSPlayerInfo.get(player).getPlayerSkill(swordBasic);
					if (skill != null && skill.onAttack(player)) {
						PacketDispatcher.sendToServer(new ActivateSkillPacket(this, true));
					}
				}
			} else {
				buttonState = false;
				charge = 0;
			}
		}

		if (isActive()) {
			--activeTimer;
		}
	}

	/**
	 * WARNING: Something REALLY dirty is about to go down here.
	 * Uses a custom accessor class planted in net.minecraft.entity package to access
	 * protected method 'damageEntity'; sets event amount to zero to prevent further processing
	 */
	public void onImpact(EntityPlayer player, LivingHurtEvent event) {
		activeTimer = 0;
		PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_ARMORBREAK, 0.4F, 0.5F);
		DirtyEntityAccessor.damageEntity(event.entityLiving, DamageUtils.causeArmorBreakDamage(player), event.ammount);
		event.ammount = 0.0F;
	}
}
