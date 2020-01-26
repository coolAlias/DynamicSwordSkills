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
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;
import net.minecraft.world.World;

/**
 * 
 * PARRY
 * Activation: Double-tap back then right-click while wielding a weapon
 * Effect: A defensive flourish that blocks incoming weapon attacks and may disarm the opponent
 * Exhaustion: 0.3F minus 0.02F per level (0.2F at level 5)
 * Chance to Disarm: 0.1F per level + a time bonus of up to 0.2F - 0.05F for each attack parried beyond the first
 * Duration: Timing window starts at 4 ticks and increases to 8 by max level
 * Max Attacks Parried: 1 + (level / 2)
 * Notes:
 *   - Only works on attacks made with an item, not against raw physical attacks such as a zombie touch
 *   - For players of equal parry skill, chance to disarm is based solely on timing
 * 
 */
public class Parry extends SkillActive
{
	/** Timer during which player is considered actively parrying */
	private int parryTimer;

	/** Number of attacks parried this activation cycle */
	private int attacksParried;

	/** Counter incremented when next correct key in sequence pressed; reset when activated or if ticksTilFail timer reaches 0 */
	@SideOnly(Side.CLIENT)
	private int keysPressed;

	/** Only for double-tap activation: Current number of ticks remaining before skill will not activate */
	@SideOnly(Side.CLIENT)
	private int ticksTilFail;

	/** Notification to play miss sound; set to true when activated and false when attack parried */
	private boolean playMissSound;

	public Parry(String translationKey) {
		super(translationKey);
	}

	private Parry(Parry skill) {
		super(skill);
	}

	@Override
	public Parry newInstance() {
		return new Parry(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.chance", (int)(getDisarmChance(player, null) * 100)));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.bonus", (int)(2.5F * (getActiveTime() - getParryDelay()))));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.max", getMaxParries()));
		desc.add(getTimeLimitDisplay(getActiveTime() - getParryDelay()));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return (parryTimer > 0);
	}

	@Override
	protected float getExhaustion() {
		return 0.3F - (0.02F * level);
	}

	/** Number of ticks that skill will be considered active */
	private int getActiveTime() {
		return 9 + (level / 2);
	}

	/** Number of ticks before player may attempt to use this skill again */
	private int getParryDelay() {
		return (5 - (level / 2));
	}

	/** The maximum number of attacks that may be parried per use of the skill */
	private int getMaxParries() {
		return (1 + level) / 2;
	}

	/**
	 * Returns player's chance to disarm an attacker
	 * @param attacker if the attacker is an EntityPlayer, their Parry score will decrease their chance of being disarmed
	 */
	private float getDisarmChance(EntityPlayer player, EntityLivingBase attacker) {
		float penalty = 0.05F * attacksParried;
		float bonus = Config.getDisarmTimingBonus() * (parryTimer > 0 ? (parryTimer - getParryDelay()) : 0);
		if (attacker instanceof EntityPlayer) {
			penalty += Config.getDisarmPenalty() * DSSPlayerInfo.get((EntityPlayer) attacker).getSkillLevel(this);
		}
		return ((level * 0.1F) - penalty + bonus);
	}

	/**
	 * Returns the strength of the knockback effect when an attack is parried
	 */
	public float getKnockbackStrength() {
		return 0.4F; // 0.5F is the base line per blocking with a shield
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() 
				&& !player.isUsingItem() 
				&& PlayerUtils.isWeapon(player.getHeldItem());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return canUse(player) && keysPressed > 1 && ticksTilFail > 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			return false;
		}
		return true;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if ((Config.allowVanillaControls() && key == mc.gameSettings.keyBindBack) || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_BACK].getKey()) {
			ticksTilFail = 6;
			if (keysPressed < 2) {
				if (!Config.requiresDoubleTap() && key == DSSKeyHandler.keys[DSSKeyHandler.KEY_BACK].getKey()) {
					keysPressed++;
				}
				keysPressed++;
			}
		} else if (key == mc.gameSettings.keyBindUseItem) {
			boolean flag = (canExecute(player) && activate(player));
			ticksTilFail = 0;
			keysPressed = 0;
			return flag;
		} else {
			ticksTilFail = 0;
			keysPressed = 0;
		}
		return false;
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		parryTimer = getActiveTime();
		attacksParried = 0;
		playMissSound = true;
		player.swingItem();
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		parryTimer = 0;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (isActive()) {
			if (--parryTimer <= getParryDelay() && playMissSound) {
				playMissSound = false;
				PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_SWORDMISS, 0.4F, 0.5F);
			}
		} else if (player.worldObj.isRemote && ticksTilFail > 0) {
			--ticksTilFail;
			if (ticksTilFail < 1) {
				keysPressed = 0;
			}
		}
	}

	@Override
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		if (source.getEntity() instanceof EntityLivingBase) {
			EntityLivingBase attacker = (EntityLivingBase) source.getEntity();
			if (attacksParried < getMaxParries() && parryTimer > getParryDelay() && attacker.getHeldItem() != null && PlayerUtils.isWeapon(player.getHeldItem())) {
				if (player.worldObj.rand.nextFloat() < getDisarmChance(player, attacker)) {
					PlayerUtils.dropHeldItem(attacker);
				}
				++attacksParried; // increment after disarm check
				PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_SWORDSTRIKE, 0.4F, 0.5F);
				playMissSound = false;
				TargetUtils.knockTargetBack(attacker, player, getKnockbackStrength());
				return true;
			} // don't deactivate early, as there is a delay between uses
		}
		return false;
	}
}
