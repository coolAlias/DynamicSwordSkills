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

import com.google.common.base.Predicate;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.api.IComboDamage;
import dynamicswordskills.api.IComboDamage.IComboDamageFull;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.EndComboPacket;
import dynamicswordskills.network.server.TargetIdPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.DirtyEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * 
 * BASIC SWORD SKILL
 * Description: Basic targeting and combo skill
 * Activation: Toggled with the Z-targeting key while looking at a valid target within range
 * Effects:	Chain up to (2 + level) attacks, adding bonus damage based on the combo size.
 * Exhaustion: 0.0F - does not cost exertion to use
 * Duration: (a) targeting: unlimited
 * 			 (b) combo: time allowed between strikes is 20 ticks + (2 * level)
 * Range: 6 + level, distance within which targets can be acquired, in blocks
 * Special: Missing an attack or taking too much damage breaks the current combo.
 * 
 */
public class SwordBasic extends SkillActive implements IComboSkill, ILockOnTarget
{
	/** True if this skill is currently active */
	private boolean isActive = false;

	/** The current target, if any; kept synchronized between the client and server */
	private EntityLivingBase currentTarget = null;

	/** The previous target; only used client side */
	@SideOnly(Side.CLIENT)
	private EntityLivingBase prevTarget;

	/** Set to a new instance each time a combo begins */
	private Combo combo = null;

	/** Flag for {@link #setComboDamageOnlyMode(boolean)} */
	private boolean comboDamageOnlyMode;

	public SwordBasic(String translationKey) {
		super(translationKey);
	}

	private SwordBasic(SwordBasic skill) {
		super(skill);
	}

	@Override
	public SwordBasic newInstance() {
		return new SwordBasic(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getRangeDisplay(getRange()));
		desc.add(new ChatComponentTranslation(getTranslationKey() + ".info.max", getMaxComboSize()).getUnformattedText());
		desc.add(getTimeLimitDisplay(getComboTimeLimit()));
		desc.add(new ChatComponentTranslation(getTranslationKey() + ".info.tolerance", String.format("%.1f", getDamageTolerance())).getUnformattedText());
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return level > 0;
	}

	@Override
	public boolean isActive() {
		return isActive;
	}

	@Override
	public boolean hasAnimation() {
		return false;
	}

	@Override
	protected float getExhaustion() {
		return 0.0F;
	}

	@Override
	public byte getMaxLevel() {
		return (MAX_LEVEL * 2);
	}

	/** Returns amount of time allowed between successful attacks before combo terminates */
	private final int getComboTimeLimit() {
		return (20 + (level * 2));
	}

	/** Returns the max combo size attainable (2 plus skill level) */
	private final int getMaxComboSize() {
		return (2 + level);
	}

	/** Returns max distance at which targets may be acquired or remain targetable */
	private final int getRange() {
		return (6 + level);
	}

	private float getDamageTolerance() {
		return (0.5F * level);
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		isActive = true;
		if (!isComboInProgress()) {
			combo = null;
		}
		currentTarget = TargetUtils.acquireLookTarget(player, getRange(), getRange(), true, getTargetSelectors());
		return true;
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		isActive = false;
		currentTarget = null;
		if (world.isRemote) {
			prevTarget = null;
		}
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (isActive() && player.worldObj.isRemote) {
			if (Minecraft.getMinecraft().currentScreen != null  || !updateTargets(player)) {
				deactivate(player);
			}
		}
		if (isComboInProgress()) {
			combo.onUpdate(player);
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		double dx = player.posX - currentTarget.posX;
		double dz = player.posZ - currentTarget.posZ;
		double angle = Math.atan2(dz, dx) * 180 / Math.PI;
		double pitch = Math.atan2(player.posY - (currentTarget.posY + (currentTarget.height / 2.0F)), Math.sqrt(dx * dx + dz * dz)) * 180 / Math.PI;
		double distance = player.getDistanceToEntity(currentTarget);
		float rYaw = (float)(angle - player.rotationYaw);
		while (rYaw > 180) { rYaw -= 360; }
		while (rYaw < -180) { rYaw += 360; }
		rYaw += 90F;
		float rPitch = (float) pitch - (float)(10.0F / Math.sqrt(distance)) + (float)(distance * Math.PI / 90);
		player.setAngles(rYaw, -(rPitch - player.rotationPitch));
		return false;
	}

	@Override
	public final boolean isLockedOn() {
		return currentTarget != null;
	}

	@Override
	public final EntityLivingBase getCurrentTarget() {
		return currentTarget;
	}

	@Override
	public void setCurrentTarget(EntityPlayer player, Entity target) {
		if (target instanceof EntityLivingBase) {
			currentTarget = (EntityLivingBase) target;
		} else {
			deactivate(player);
		}
	}

	/**
	 * Returns the next closest new target or locks on to the previous target, if any
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public final void getNextTarget(EntityPlayer player) {
		EntityLivingBase nextTarget = null;
		double dTarget = 0;
		List<EntityLivingBase> list = TargetUtils.acquireAllLookTargets(player, getRange(), getRange(), getTargetSelectors());
		for (EntityLivingBase entity : list) {
			if (entity == player) { continue; }
			if (entity != currentTarget && entity != prevTarget && isTargetValid(player, entity)) {
				if (nextTarget == null) {
					dTarget = player.getDistanceSqToEntity(entity);
					nextTarget = entity;
				} else {
					double distance = player.getDistanceSqToEntity(entity);
					if (distance < dTarget) {
						nextTarget = entity;
						dTarget = distance;
					}
				}
			}
		}
		if (nextTarget != null) {
			prevTarget = currentTarget;
			currentTarget = nextTarget;
		} else {
			nextTarget = currentTarget;
			currentTarget = prevTarget;
			prevTarget = nextTarget;
		}
		PacketDispatcher.sendToServer(new TargetIdPacket(this));
	}

	/**
	 * See {@link TargetUtils#getDefaultSelectors()}
	 */
	protected List<Predicate<Entity>> getTargetSelectors() {
		List<Predicate<Entity>> list = TargetUtils.getDefaultSelectors();
		if (!Config.canTargetPassiveMobs()) {
			list.add(TargetUtils.HOSTILE_MOB_SELECTOR);
		}
		if (!Config.canTargetPlayers()) {
			list.add(TargetUtils.NON_PLAYER_SELECTOR);
		}
		return list;
	}

	/**
	 * Updates targets, setting to null if no longer valid and acquiring new target if necessary
	 * @return returns true if the current target is valid
	 */
	@SideOnly(Side.CLIENT)
	private boolean updateTargets(EntityPlayer player) {
		if (!isTargetValid(player, prevTarget) || !TargetUtils.isTargetInSight(player, prevTarget)) {
			prevTarget = null;
		}
		if (!isTargetValid(player, currentTarget) || !player.canEntityBeSeen(currentTarget)) {
			currentTarget = null;
			if (Config.autoTargetEnabled()) {
				getNextTarget(player);
			}
		}
		return isTargetValid(player, currentTarget);
	}

	/**
	 * Returns true if target entity is valid: not dead and still within lock-on range
	 */
	@SideOnly(Side.CLIENT)
	private boolean isTargetValid(EntityPlayer player, EntityLivingBase target) {
		return (target != null && !target.isDead && target.getHealth() > 0F 
				&& player.getDistanceToEntity(target) < (float) getRange() 
				&& !target.isInvisible());
	}

	@Override
	public final Combo getCombo() {
		return combo;
	}

	@Override
	public final void setCombo(Combo combo) {
		this.combo = combo;
	}

	@Override
	public final boolean isComboInProgress() {
		return (combo != null && !combo.isFinished());
	}

	@Override
	public void setComboDamageOnlyMode(boolean flag) {
		this.comboDamageOnlyMode = flag;
	}

	@Override
	public void onMiss(EntityPlayer player) {
		if (PlayerUtils.isWeapon(player.getHeldItem())) {
			PlayerUtils.playRandomizedSound(player, ModInfo.SOUND_SWORDMISS, 0.4F, 0.5F);
		}
		if (isComboInProgress()) {
			PacketDispatcher.sendToServer(new EndComboPacket(this));
		}
	}

	@Override
	public float onImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		if (combo != null && !combo.isFinished()) {
			amount += combo.getNumHits();
		}
		return amount;
	}

	@Override
	public void onHurtTarget(EntityPlayer player, LivingHurtEvent event) {
		if (!isLockedOn() || !isValidComboDamage(player, event.source)) { return; }
		if (combo == null || combo.isFinished()) {
			combo = new Combo(player, this, getMaxComboSize(), getComboTimeLimit());
		}
		float damage = DirtyEntityAccessor.getModifiedDamage(event.entityLiving, event.source, event.ammount);
		if (damage > 0) {
			if (!comboDamageOnlyMode && (!(event.source instanceof IComboDamageFull) || ((IComboDamageFull) event.source).increaseComboCount(player))) {
				combo.add(player, event.entityLiving, damage);
			} else {
				combo.addDamageOnly(player, damage);
			}
		}
		String sound = getComboDamageSound(player, event.source);
		if (sound != null) {
			PlayerUtils.playSoundAtEntity(player.worldObj, player, sound, 0.4F, 0.5F);
		}
	}

	private boolean isValidComboDamage(EntityPlayer player, DamageSource source) {
		return source instanceof IComboDamage || !source.isProjectile();
	}

	private String getComboDamageSound(EntityPlayer player, DamageSource source) {
		if (source instanceof IComboDamageFull && !((IComboDamageFull) source).playDefaultSound(player)) {
			return ((IComboDamageFull) source).getHitSound(player);
		} else if (source.getDamageType().equals("player")) {
			return (PlayerUtils.isSword(player.getHeldItem()) ? ModInfo.SOUND_SWORDCUT : ModInfo.SOUND_HURT_FLESH);
		}
		return null;
	}

	@Override
	public void onPlayerHurt(EntityPlayer player, LivingHurtEvent event) {
		if (isComboInProgress() && DirtyEntityAccessor.getModifiedDamage(player, event.source, event.ammount) > getDamageTolerance()) {
			combo.endCombo(player);
		}
	}
}
