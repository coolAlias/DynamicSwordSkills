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
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

/**
 * 
 * ARMOR BREAK
 * Description: Unleash a powerful blow that ignores armor
 * Activation: Hold attack for (20 - level) ticks
 * Effect: Unleashes an attack that inflicts normal weapon damage but ignores armor
 * Exhaustion: 2.0F - (0.1F * level)
 * 
 * Using this skill performs an attack that ignores armor but otherwise deals exactly the
 * same damage as a normal attack with the given item would, including all bonuses from other
 * skills and enchantments.
 * 
 * Armor Break must be charged by holding the 'attack' key; once the charge reaches full,
 * the player will perform the Armor Break attack automatically.
 * 
 */
public class ArmorBreak extends SkillActive
{
	/** Set when triggered; set to 0 when target struck in onImpact() */
	private int activeTimer = 0;

	/** Current charge time */
	private int charge = 0;

	/** Flag to allow armor break to begin charging even if mouse is over a block */
	private boolean wasLockedOn;

	@SideOnly(Side.CLIENT)
	private KeyBinding attackKey;

	public ArmorBreak(String translationKey) {
		super(translationKey);
	}

	private ArmorBreak(ArmorBreak skill) {
		super(skill);
	}

	@Override
	public ArmorBreak newInstance() {
		return new ArmorBreak(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getChargeDisplay(getChargeTime(player)));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	protected boolean allowUserActivation() {
		return false;
	}

	@Override
	public boolean isActive() {
		return activeTimer > 0;
	}

	@Override
	protected float getExhaustion() {
		return 2.0F - (0.1F * level);
	}

	/** Returns number of ticks required before attack will execute: 20 - level */
	private int getChargeTime(EntityPlayer player) {
		return 20 - level;
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isWeapon(player.getHeldItem());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			// Need to receive key release when not locked on to stop charging
			return charge > 0 && key == mc.gameSettings.keyBindAttack;
		}
		wasLockedOn = isLockedOn;
		return key == mc.gameSettings.keyBindAttack;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		// Only begin charging if not mousing over a block or locked on, otherwise player cannot harvest blocks
		if (wasLockedOn || mc.objectMouseOver == null || mc.objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) {
			attackKey = key;
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void keyReleased(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == attackKey) {
			attackKey = null;
			charge = 0;
			DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
		}
	}

	@SideOnly(Side.CLIENT)
	private void initCharging(EntityPlayer player) {
		if (charge == 0 && attackKey != null && attackKey.getIsKeyPressed() && DSSPlayerInfo.get(player).canInteract()) {
			charge = getChargeTime(player);
			// Unset the keybind state to prevent issues if the player mouses over a block while charging
			KeyBinding.setKeyBindState(attackKey.getKeyCode(), false);
		}
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		activeTimer = 4; // needs to be active for attack event to process correctly
		if (world.isRemote) { // only attack after server has been activated, i.e. client receives activation packet back
			attackKey = null;
			DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
			DSSClientEvents.handlePlayerAttack(Minecraft.getMinecraft());
		}
		return true;
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		activeTimer = 0;
		charge = 0;
		DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (player.worldObj.isRemote) {
			initCharging(player);
		}
		if (isActive()) {
			--activeTimer;
		} else if (charge > 0) {
			if (PlayerUtils.isWeapon(player.getHeldItem())) {
				int maxCharge = getChargeTime(player);
				if (charge < maxCharge - 1) {
					float f = 0.25F + 0.75F * ((float)(maxCharge - charge) / (float) maxCharge);
					DSSPlayerInfo.get(player).setArmSwingProgress(f, 0.0F);
				}
				--charge;
				if (charge == 0) {
					PacketDispatcher.sendToServer(new ActivateSkillPacket(this, true));
				}
			} else {
				DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
				charge = 0;
			}
		}
	}

	@Override
	public boolean onAttack(EntityPlayer player, EntityLivingBase entity, DamageSource source, float amount) {
		activeTimer = 0;
		entity.attackEntityFrom(DamageUtils.causeArmorBreakDamage(player), amount);
		if (!player.worldObj.isRemote) { 
			PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_ARMORBREAK, 0.4F, 0.5F);
		}
		return true;
	}
}
