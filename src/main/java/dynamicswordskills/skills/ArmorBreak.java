/**
    Copyright (C) <2017> <coolAlias>

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

import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.ref.ModSounds;
import dynamicswordskills.util.DamageUtils;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * 
 * ARMOR BREAK
 * Description: Unleash a powerful blow that ignores armor
 * Activation: Hold attack for (20 - level) ticks
 * Effect: Unleashes an attack that inflicts normal weapon damage but ignores armor
 * Exhaustion: 2.0F - (0.1F * level)
 * Special: May only be used while locked on to a target
 * 			Charge time is reduced by 5 ticks when wielding a Master Sword
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
	/** Set to 1 when triggered; set to 0 when target struck in onImpact() */
	private int activeTimer = 0;

	/** Current charge time */
	private int charge = 0;

	public ArmorBreak(String name) {
		super(name);
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

	/** Returns true if the skill is still charging up; always false on the server, as charge is handled client-side */
	public boolean isCharging(EntityPlayer player) {
		ILockOnTarget target = DSSPlayerInfo.get(player).getTargetingSkill();
		return charge > 0 && target != null && target.isLockedOn();
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isWeapon(player.getHeldItemMainhand());
	}

	/**
	 * ArmorBreak does not listen for any keys so that there is no chance it is bypassed by
	 * another skill processing first; instead, keyPressed must be called manually, both
	 * when the attack key is pressed (and, to handle mouse clicks, when released)
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (!isLockedOn) {
			return false;
		}
		return false;
	}

	/**
	 * Must be called manually when the attack key is pressed (and, for the mouse, when released);
	 * this is necessary to allow charging to start from a single key press, when other skills
	 * might otherwise preclude ArmorBreak's keyPressed from being called.
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		charge = getChargeTime(player);
		// manually set the keybind state, since it will not be set by the canceled mouse event
		// releasing the mouse unsets it normally, but it must be manually unset if the skill is triggered
		// note that doing so causes player to try to dig blocks when mousing over them
		KeyBinding.setKeyBindState(mc.gameSettings.keyBindAttack.getKeyCode(), true);
		return false;
	}

	/**
	 * Returns true if the attack key is still pressed (i.e. ArmorBreak should continue to charge)
	 */
	@SideOnly(Side.CLIENT)
	public boolean isKeyPressed() {
		return Minecraft.getMinecraft().gameSettings.keyBindAttack.isKeyDown();
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		activeTimer = 1; // needs to be active for hurt event to process correctly
		if (world.isRemote) {
			player.swingArm(EnumHand.MAIN_HAND);
			player.resetCooldown();
		} else {
			// Attack first so skill still active upon impact, then set timer to zero
			ILockOnTarget skill = DSSPlayerInfo.get(player).getTargetingSkill();
			if (skill != null && skill.isLockedOn() && TargetUtils.canReachTarget(player, skill.getCurrentTarget())) {
				player.attackTargetEntityWithCurrentItem(skill.getCurrentTarget());
			}
		}
		return false;
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		activeTimer = 0;
		charge = 0;
		DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (isCharging(player)) {
			if (isKeyPressed() && PlayerUtils.isWeapon(player.getHeldItemMainhand())) {
				int maxCharge = getChargeTime(player);
				if (charge < maxCharge - 1) {
					float f = 0.25F + 0.75F * ((float)(maxCharge - charge) / (float) maxCharge);
					DSSPlayerInfo.get(player).setArmSwingProgress(f, 0.0F);
				}
				--charge;
				// ArmorBreak triggers here, on the client side first, so onActivated need not process on the client
				if (charge == 0) {
					// can't use the standard animation methods to prevent key/mouse input,
					// since Armor Break will not return true for isActive
					DSSPlayerInfo.get(player).setAttackCooldown(4); // flag for isAnimating? no player parameter
					// Manually unset the key state to prevent continually attacking while attack key held down
					KeyBinding.setKeyBindState(Minecraft.getMinecraft().gameSettings.keyBindAttack.getKeyCode(), false);
					SwordBasic skill = (SwordBasic) DSSPlayerInfo.get(player).getPlayerSkill(swordBasic);
					if (skill != null && skill.onAttack(player)) {
						// don't swing arm here, it screws up the attack damage calculations
						PacketDispatcher.sendToServer(new ActivateSkillPacket(this, true));
					} else { // player missed - swing arm manually since no activation packet will be sent
						player.swingArm(EnumHand.MAIN_HAND);
						player.resetCooldown();
					}
				}
			} else {
				DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
				charge = 0;
			}
		} else {
			DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
		}
		if (isActive()) {
			activeTimer = 0;
		}
	}

	@Override
	public boolean onAttack(EntityPlayer player, EntityLivingBase entity, DamageSource source, float amount) {
		activeTimer = 0;
		entity.attackEntityFrom(DamageUtils.causeArmorBreakDamage(player), amount);
		if (!player.worldObj.isRemote) { 
			PlayerUtils.playSoundAtEntity(player.worldObj, player, ModSounds.ARMOR_BREAK, SoundCategory.PLAYERS, 0.4F, 0.5F);
		}
		return true;
	}
}
