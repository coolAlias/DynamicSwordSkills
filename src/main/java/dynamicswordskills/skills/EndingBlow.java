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
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActionTimePacket;
import dynamicswordskills.network.client.EndingBlowPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.DirtyEntityAccessor;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * 
 * ENDING BLOW
 * Description: Finish off an enemy made vulnerable by your flurry of blows
 * Activation: Forward, forward, and attack during combo
 * Effect:	Build up combo momentum and then finish off your enemy with a decisive strike,
 * 			gaining bonus xp if successful or becoming flat-footed if not
 * Damage: +(level * 20) percent
 * Duration of vulnerability: 45 - (level * 5) ticks
 * Exhaustion: 2.0F - (level * 0.1F)
 * XP Bonus: level + (value between 1 and the opponent's last remaining health)
 * Special:
 * - May only be used after two or more consecutive strikes on the same target
 * - Slaying an opponent with this move grants additional experience
 * - Failure to slay the target results in not being able to attack for the duration
 * 
 */
public class EndingBlow extends SkillActive
{
	/** Flag for isActive() so that skill can trigger upon impact from LivingHurtEvent */
	private int activeTimer = 0;

	/** Only for vanilla activation: Current number of ticks remaining before skill will not activate */
	@SideOnly(Side.CLIENT)
	private int ticksTilFail;

	/** Number of times the forward key has been pressed this activation cycle */
	@SideOnly(Side.CLIENT)
	private int keyPressed;

	/** Only for double-tap activation; true after the first key press and release */
	@SideOnly(Side.CLIENT)
	private boolean keyReleased;

	/** The last time this skill was activated (so HUD element can display or hide as appropriate) */
	@SideOnly(Side.CLIENT)
	private long lastActivationTime;

	/** Flag indicating the skill's result: 0 - result pending; +1 - success; -1 - failure; only used for HUD  */
	public byte skillResult;

	/** Number of consecutive hits the combo had when the skill was last used */
	private int lastNumHits;

	/** Workaround for armor / potions changing damage: checks next tick if entity is dead or not */
	private EntityLivingBase entityHit;

	/** Xp amount to grant if entityHit is dead on update tick */
	private int xp;

	public EndingBlow(String translationKey) {
		super(translationKey);
	}

	private EndingBlow(EndingBlow skill) {
		super(skill);
	}

	@Override
	public EndingBlow newInstance() {
		return new EndingBlow(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP || group == Skills.TARGETED_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getDamageDisplay(level * 20, true) + "%");
		desc.add(getDurationDisplay(getDuration(), true));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return activeTimer > 0;
	}

	@Override
	protected float getExhaustion() {
		return 2.0F - (level * 0.1F);
	}

	/** Returns the duration of the defense down effect */
	public int getDuration() {
		return 45 - (level * 5);
	}

	/** Returns the {@link #lastActivationTime} */
	@SideOnly(Side.CLIENT)
	public long getLastActivationTime() {
		return this.lastActivationTime;
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		if (!isActive() && super.canUse(player) && PlayerUtils.isWeapon(player.getHeldItem())) {
			IComboSkill combo = DSSPlayerInfo.get(player).getComboSkill();
			if (combo != null && combo.isComboInProgress()) {
				ILockOnTarget lock = DSSPlayerInfo.get(player).getTargetingSkill();
				if (lock == null || (lock.isLockedOn() && lock.getCurrentTarget() != combo.getCombo().getLastEntityHit())) {
					return false;
				} else if (lastNumHits > 0) {
					return combo.getCombo().getConsecutiveHits() > 1 && combo.getCombo().getNumHits() > lastNumHits + 2;
				} else {
					return combo.getCombo().getConsecutiveHits() > 1;
				}
			}
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return ticksTilFail > 0 && keyPressed > 1 && keyReleased && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			return false;
		}
		return (key == mc.gameSettings.keyBindAttack || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_FORWARD].getKey()
				|| (Config.allowVanillaControls() && key == mc.gameSettings.keyBindForward));
	}

	/**
	 * Increments the number of times the key has been pressed and starts the fail timer if not yet set,
	 * or triggers the skill if the right conditions are met
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_FORWARD].getKey() || (Config.allowVanillaControls() && key == mc.gameSettings.keyBindForward)) {
			if (ticksTilFail == 0) {
				ticksTilFail = 6;
			}
			++keyPressed;
		} else if (canExecute(player)) {
			ticksTilFail = 0;
			keyPressed = 0;
			keyReleased = false;
			return activate(player);
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void keyReleased(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_FORWARD].getKey() || (Config.allowVanillaControls() && key == mc.gameSettings.keyBindForward)) {
			keyReleased = (keyPressed > 0 && ticksTilFail > 0);
		}
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		activeTimer = 3; // gives server some time for client attack to occur
		entityHit = null;
		IComboSkill skill = DSSPlayerInfo.get(player).getComboSkill();
		if (skill != null && skill.getCombo() != null) {
			lastNumHits = skill.getCombo().getNumHits();
		}
		if (world.isRemote) { // only attack after server has been activated, i.e. client receives activation packet back
			DSSClientEvents.handlePlayerAttack(Minecraft.getMinecraft());
			this.lastActivationTime = Minecraft.getSystemTime();
			this.skillResult = 0;
		}
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		activeTimer = 0;
		entityHit = null;
		xp = 0;
		if (world.isRemote) {
			keyPressed = 0;
			keyReleased = false;
			ticksTilFail = 0;
		}
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (player.worldObj.isRemote && ticksTilFail > 0) {
			--ticksTilFail;
			if (ticksTilFail == 0) {
				keyPressed = 0;
				keyReleased = false;
			}
		}
		if (lastNumHits > 0) {
			if (entityHit != null && xp > 0) {
				updateEntityState(player);
			}
			IComboSkill skill = DSSPlayerInfo.get(player).getComboSkill();
			if (skill == null || !skill.isComboInProgress()) {
				lastNumHits = 0;
			}
		}
		if (isActive()) {
			--activeTimer;
			if (activeTimer == 0 && !player.worldObj.isRemote) {
				onFail(player, true);
			}
		}
	}

	/**
	 * Checks if entity hit is dead, granting Xp or causing defensive penalty
	 */
	private void updateEntityState(EntityPlayer player) {
		if (!player.worldObj.isRemote) {
			if (entityHit.getHealth() <= 0.0F) {
				if (entityHit instanceof EntityLiving) {
					DirtyEntityAccessor.setLivingXp((EntityLiving) entityHit, xp, true);
				} else {
					PlayerUtils.spawnXPOrbsWithRandom(player.worldObj, player.worldObj.rand, MathHelper.floor_double(entityHit.posX),
							MathHelper.floor_double(entityHit.posY), MathHelper.floor_double(entityHit.posZ), xp);
				}
				PacketDispatcher.sendTo(new EndingBlowPacket((byte) 1), (EntityPlayerMP) player);
			} else {
				onFail(player, false);
			}
		}
		entityHit = null;
		xp = 0;
	}

	@Override
	public float onImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		IComboSkill combo = DSSPlayerInfo.get(player).getComboSkill();
		if (combo != null && combo.isComboInProgress() && entity == combo.getCombo().getLastEntityHit() && combo.getCombo().getConsecutiveHits() > 1) {
			amount *= 1.0F + (level * 0.2F);
			PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_MORTALDRAW, 0.4F, 0.5F);
			entityHit = entity;
		} else if (!player.worldObj.isRemote) {
			onFail(player, false);
		}
		return amount;
	}

	@Override
	public void postImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		activeTimer = 0;
		if (entityHit != null) {
			xp = level + 1 + player.worldObj.rand.nextInt(Math.max(2, MathHelper.ceiling_float_int(entity.getHealth())));
		}
	}

	private void onFail(EntityPlayer player, boolean timedOut) {
		if (!player.capabilities.isCreativeMode) {
			DSSPlayerInfo skills = DSSPlayerInfo.get(player);
			int t = getDuration() * (timedOut ? 2 : 1);
			skills.setAttackCooldown(t);
			PacketDispatcher.sendTo(new ActionTimePacket(skills.getAttackTime(), true), (EntityPlayerMP) player);
		}
		if (!timedOut) {
			PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_HURT_FLESH, 0.3F, 0.8F);
		}
		PacketDispatcher.sendTo(new EndingBlowPacket((byte)-1), (EntityPlayerMP) player);
	}
}
