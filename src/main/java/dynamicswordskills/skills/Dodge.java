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
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * DODGE
 * Description: Avoid damage by quickly dodging out of the way
 * Activation: Double-tap left or right to dodge in that direction
 * Exhaustion: 0.05F
 * Duration: (5 + level) ticks; this is the amount of time during which damage may be avoided
 * Special: Chance to avoid damage is 10% per level, plus a timing bonus of up to 20%
 * 
 */
public class Dodge extends SkillActive
{
	/** Key that was pressed to initiate dodge */
	@SideOnly(Side.CLIENT)
	private KeyBinding keyPressed;

	/** Current number of ticks remaining before dodge will not activate */
	private int ticksTilFail;

	/** Only for double-tap activation; true after the first key press and release */
	private boolean keyReleased;

	/** Trajectory based on player's look vector and Dodge direction */
	private Vec3 trajectory;

	/** Timer during which player may evade incoming attacks */
	private int dodgeTimer = 0;

	/** Entity dodged, since the attack event may fire multiple times in quick succession for mobs like zombies */
	private Entity entityDodged;

	public Dodge(String translationKey) {
		super(translationKey);
	}

	private Dodge(Dodge skill) {
		super(skill);
	}

	@Override
	public Dodge newInstance() {
		return new Dodge(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.chance", (int)(getBaseDodgeChance(player) * 100)));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.bonus", level * 4));
		desc.add(getTimeLimitDisplay(getDodgeTime()));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	/**
	 * Prevents Dodge from being activated in quick succession, but does not prevent
	 * other skills from being activated once Dodge has finished animating
	 */
	@Override
	public boolean isActive() {
		return (dodgeTimer > 0);
	}

	@Override
	protected float getExhaustion() {
		return 0.05F;
	}

	/** Returns player's base chance to successfully evade an attack, including bonuses from buffs */
	private float getBaseDodgeChance(EntityPlayer player) {
		float speedBonus = 2.0F * (float)(player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed).getAttributeValue() - Dash.BASE_MOVE);
		return ((level * 0.1F) + speedBonus);
	}

	/** Returns full chance to dodge an attack, including all bonuses */
	private float getDodgeChance(EntityPlayer player) {
		return getBaseDodgeChance(player) + getTimeBonus();
	}

	/** Amount of time dodge will remain active */
	private int getDodgeTime() {
		return (5 + level);
	}

	/** Returns timing evasion bonus */
	private float getTimeBonus() {
		return ((dodgeTimer + level - 5) * 0.02F);
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && !PlayerUtils.isBlocking(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return player.onGround && canUse(player) && keyReleased && ticksTilFail > 0;
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
		if (key == keyPressed) {
			boolean flag = canExecute(player) && activate(player);
			resetKeyState(flag);
			return flag;
		} else if (Config.allowVanillaControls() && (key == mc.gameSettings.keyBindLeft || key == mc.gameSettings.keyBindRight)) {
			firstKeyPress(mc, key, player);
		} else if (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_LEFT].getKey() || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT].getKey()) {
			if (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_LEFT].getKey() && mc.gameSettings.keyBindRight.getIsKeyPressed()) {
				return false;
			} else if (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT].getKey() && mc.gameSettings.keyBindLeft.getIsKeyPressed()) {
				return false;
			}
			firstKeyPress(mc, key, player);
			if (!Config.requiresDoubleTap()) {
				keyReleased = true;
				boolean flag = canExecute(player) && activate(player);
				resetKeyState(true);
				return flag;
			}
		} else {
			keyPressed = null;
		}
		return false;
	}

	@SideOnly(Side.CLIENT)
	private void firstKeyPress(Minecraft mc, KeyBinding key, EntityPlayer player) {
		keyPressed = key;
		keyReleased = false;
		ticksTilFail = 6;
	}

	@SideOnly(Side.CLIENT)
	private void resetKeyState(boolean flag) {
		keyReleased = false;
		ticksTilFail = 0;
		if (!flag) {
			keyPressed = null;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void keyReleased(Minecraft mc, KeyBinding key, EntityPlayer player) {
		keyReleased = (key == keyPressed);
		if (keyReleased && ticksTilFail < 1) {
			keyPressed = null;
		}
	}

	@Override
	public boolean onActivated(World world, EntityPlayer player) {
		dodgeTimer = getDodgeTime();
		entityDodged = null;
		if (player.worldObj.isRemote) {
			trajectory = player.getLookVec();
			if (keyPressed == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT].getKey() || keyPressed == Minecraft.getMinecraft().gameSettings.keyBindRight) {
				trajectory = Vec3.createVectorHelper(-trajectory.zCoord, 0.0D, trajectory.xCoord);
			} else {
				trajectory = Vec3.createVectorHelper(trajectory.zCoord, 0.0D, -trajectory.xCoord);
			}
			keyPressed = null;
		}
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		dodgeTimer = 0;
		entityDodged = null;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (isActive()) {
			--dodgeTimer;
		}
		if (ticksTilFail > 0) {
			--ticksTilFail;
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return (dodgeTimer > level && trajectory != null);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		double speed = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed).getAttributeValue();
		double fps = (DynamicSwordSkills.BASE_FPS / (float) DynamicSwordSkills.proxy.getDebugFPS()); 
		double d = 1.15D * fps * speed;
		if (player.isInWater() || player.handleLavaMovement()) {
			d *= 0.15D;
		}
		player.addVelocity(trajectory.xCoord * d, -0.02D * fps, trajectory.zCoord * d);
		return true;
	}

	@Override
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		if (dodgeTimer > level) { // still able to dodge (used to use isActive(), but changed for animating)
			Entity attacker = source.getEntity();
			if (attacker != null) {
				return (attacker == entityDodged || dodgeAttack(player, attacker));
			}
		}
		return false;
	}

	/**
	 * Returns true if the attack was dodged and the attack event should be canceled
	 */
	private boolean dodgeAttack(EntityPlayer player, Entity attacker) {
		if (player.worldObj.rand.nextFloat() < getDodgeChance(player)) {
			entityDodged = attacker;
			PlayerUtils.playRandomizedSound(player, ModInfo.SOUND_SWORDMISS, 0.4F, 0.5F);
			return true;
		}
		return false;
	}
}
