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
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.DynamicSwordSkills;
import dynamicswordskills.api.SkillGroup;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.DirtyEntityAccessor;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * Player spins in a 360 degree arc, attacking all enemies within it.
 * Super Spin Attack greatly improves the Spin Attack and allows spinning extra times
 * by tapping the attack key, but it requires full or near-full health.
 * 
 * Activation: Hold both left and right movement keys to charge up until spin attack commences
 * Super Spin Attack: Tap attack while spinning to spin again, up to once per level
 * Arc: 360 degrees, plus an extra 360 degrees for every level of Super Spin Attack
 * Charge time: 20 ticks, minus 2 per level
 * Range: 3.0D plus 0.3D per level each of Spin and Super Spin Attack
 * Exhaustion: 3.0F - 0.2F per level, added each spin
 *
 */
public class SpinAttack extends SkillActive implements IModifiableSkill, IReachAttackSkill
{
	/** Current charge time; only ever set on the client - server is never charging */
	private int charge;

	/** Current spin progress is incremented each tick and signals that the skill is active */
	private float currentSpin;

	/** Number of degrees to spin and used as flag for isActive(); incremented by 360F each time spin is refreshed */
	private float arc;

	/** Number of times the spin has been 'refreshed' during this activation cycle; incremented in {@link #startSpin} */
	private int refreshed;

	/** Direction in which to spin */
	@SideOnly(Side.CLIENT)
	private boolean clockwise;

	/** Entities within range upon activation so no entity targeted more than once */
	@SideOnly(Side.CLIENT)
	private List<EntityLivingBase> targets;

	/** Whether flame particles should render along the sword's arc */
	private boolean isFlaming;

	/** The player's Super Spin Attack level will allow multiple spins and extended range */
	private int superLevel;

	/** Bonus sword range provided by Super Spin Attack modifier, if any */
	private float bonusRange;

	public SpinAttack(String translationKey) {
		super(translationKey);
	}

	private SpinAttack(SpinAttack skill) {
		super(skill);
	}

	@Override
	public SpinAttack newInstance() {
		return new SpinAttack(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.WEAPON_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		superLevel = 0;
		bonusRange = 0.0F;
		SkillActive.applyActivationSkillModifiers(this, player);
		desc.add(getChargeDisplay(getChargeTime()));
		desc.add(getRangeDisplay(getRange()));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.spins", superLevel + 1));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return arc > 0;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return isActive() && !isCharging();
	}

	@Override
	protected float getExhaustion() {
		return 3.0F - (0.2F * level);
	}

	/** Returns time required before spin will execute */
	private int getChargeTime() {
		return 20 - (level * 2);
	}

	/** Returns true if the skill is still charging up; always false on the server */
	private boolean isCharging() {
		return charge > 0;
	}

	/** Returns true if the arc may be extended by 360 more degrees */
	private boolean canRefreshArc() {
		return (refreshed < (superLevel + 1) && arc == (360F * refreshed));
	}

	/** Max sword range for striking targets */
	private float getRange() {
		return 3.0F + (level * 0.3F) + bonusRange;
	}

	/** Returns the spin speed modified based on the skill's level */
	private float getSpinSpeed() {
		return 120 + (3 * (superLevel + level));
	}

	/** Returns true if the player can spin, i.e. holding a weapon, not using an item, and super#canUse returns true */
	protected boolean canSpin(EntityPlayer player) {
		// return super.canUse instead of this.canUse to avoid !isActive() check
		return super.canUse(player) && !player.isUsingItem() && PlayerUtils.isWeapon(player.getHeldItem());
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return !isActive() && canSpin(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return canSpin(player);
	}

	/**
	 * Returns true if both activation keys are currently being pressed
	 */
	@SideOnly(Side.CLIENT)
	private boolean isKeyPressed() {
		return (DSSKeyHandler.keys[DSSKeyHandler.KEY_LEFT].getIsKeyPressed() && DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT].getIsKeyPressed())
				|| (Config.allowVanillaControls() && (Minecraft.getMinecraft().gameSettings.keyBindLeft.getIsKeyPressed()
						&& Minecraft.getMinecraft().gameSettings.keyBindRight.getIsKeyPressed()));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			return false;
		}
		return ((Config.allowVanillaControls() && (key == mc.gameSettings.keyBindLeft || key == mc.gameSettings.keyBindRight)) ||
				key == DSSKeyHandler.keys[DSSKeyHandler.KEY_LEFT].getKey() || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT].getKey());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (!isCharging() && isKeyPressed() && canSpin(player)) {
			clockwise = (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT].getKey() || key == mc.gameSettings.keyBindRight);
			charge = getChargeTime();
			return true;
		}
		return false;
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		currentSpin = 0.0F;
		arc = 360F;
		refreshed = 0;
		superLevel = 0;
		bonusRange = 0.0F;
		isFlaming = EnchantmentHelper.getFireAspectModifier(player) > 0;
		return true;
	}

	@Override
	protected void postActivated(EntityPlayer player) {
		TargetUtils.applyExtendedReachModifier(player, getRange());
		IComboSkill combo = DSSPlayerInfo.get(player).getComboSkill();
		if (combo != null) {
			combo.setComboDamageOnlyMode(true);
		}
		startSpin(player);
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		charge = 0;
		currentSpin = 0.0F;
		arc = 0.0F;
		DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
		TargetUtils.removeExtendedReachModifier(player);
		IComboSkill combo = DSSPlayerInfo.get(player).getComboSkill();
		if (combo != null) {
			combo.setComboDamageOnlyMode(false);
		}
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		// isCharging can only be true on the client, which is where charging is handled
		if (isCharging()) { // check isRemote before accessing @client stuff anyway, just in case charge somehow set on server
			if (player.worldObj.isRemote && canSpin(player) && isKeyPressed()) {
				--charge;
				int maxCharge = getChargeTime();
				if (charge < maxCharge) {
					float f = 1F - 0.5F * ((float)(maxCharge - charge) / (float) maxCharge);
					DSSPlayerInfo.get(player).setArmSwingProgress(f, f);
				}
				--charge;
				if (charge == 0 && canExecute(player)) {
					activate(player);
				}
			} else {
				charge = 0;
				DSSPlayerInfo.get(player).setArmSwingProgress(0.0F, 0.0F);
			}
		} else if (isActive()) {
			if (player.worldObj.isRemote) {
				if (!PlayerUtils.isWeapon(player.getHeldItem()) || !isKeyPressed()) {
					deactivate(player);
				} else {
					incrementSpin(player);
				}
			} else {
				incrementSpin(player);
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		if (PlayerUtils.isWeapon(player.getHeldItem())) {
			DirtyEntityAccessor.syncCurrentPlayItem(Minecraft.getMinecraft().playerController);
			attackTargetsInSight(player);
			spawnParticles(player);
			DSSPlayerInfo.get(player).setArmSwingProgress(0.5F, 0.5F);
			float fps = (DynamicSwordSkills.BASE_FPS / (float) DynamicSwordSkills.proxy.getDebugFPS());
			float speed = fps * this.getSpinSpeed();
			player.setAngles((clockwise ? speed: -speed), 0);
		}
		return true;
	}

	private void attackTargetsInSight(EntityPlayer player) {
		List<EntityLivingBase> list = TargetUtils.acquireAllLookTargets(player, (int)(getRange() + 0.5F), 1.0D, getTargetSelectors());
		for (EntityLivingBase target : list) {
			if (targets != null && targets.contains(target)) {
				DSSClientEvents.multiAttack(Minecraft.getMinecraft(), target, this);
				targets.remove(target);
			}
		}
	}

	/**
	 * Unlike the default targeting, Spin Attack damages invisible entities
	 */
	protected List<Predicate<Entity>> getTargetSelectors() {
		List<Predicate<Entity>> list = Lists.<Predicate<Entity>>newArrayList();
		list.add(TargetUtils.COLLIDABLE_ENTITY_SELECTOR);
		list.add(TargetUtils.NON_RIDING_SELECTOR);
		list.add(TargetUtils.NON_TEAM_SELECTOR);
		return list;
	}

	/**
	 * Initiates spin attack and increments refreshed
	 * Client populates the nearby target list
	 * Server plays spin sound and, if not the first spin, adds exhaustion
	 */
	private void startSpin(EntityPlayer player) {
		++refreshed;
		if (player.worldObj.isRemote) {
			targets = player.worldObj.getEntitiesWithinAABB(EntityLivingBase.class, player.boundingBox.expand(getRange(), 0.0D, getRange()));
			if (targets.contains(player)) {
				targets.remove(player);
			}
		} else {
			PlayerUtils.playRandomizedSound(player, ModInfo.SOUND_SPINATTACK, 0.4F, 0.5F);
			if (refreshed > 1) {
				player.addExhaustion(getExhaustion());
			}
		}
	}

	/**
	 * Updates the spin progress counter and terminates the spin once it reaches the max spin arc
	 */
	private void incrementSpin(EntityPlayer player) {
		// 0.15D is the multiplier from Entity.setAngles, but that is too little now that no longer in render tick
		// 0.21D results in a near-perfect circle per spin at all levels
		currentSpin += getSpinSpeed() * 0.21D;
		if (currentSpin >= arc) {
			deactivate(player);
		} else if (currentSpin > (360F * refreshed)) {
			startSpin(player);
		}
	}

	@SideOnly(Side.CLIENT)
	private void spawnParticles(EntityPlayer player) {
		// TODO these will not be seen by other players
		String particle = (isFlaming ? "flame" : (superLevel > 0 ? "magicCrit" : "crit"));
		Vec3 vec3 = player.getLookVec();
		double posX = player.posX + (vec3.xCoord * getRange());
		double posY = player.posY + player.getEyeHeight() - 0.1D;
		double posZ = player.posZ + (vec3.zCoord * getRange());
		for (int i = 0; i < 2; ++i) {
			player.worldObj.spawnParticle(particle, posX, posY, posZ, vec3.xCoord * 0.15D, 0.01D, vec3.zCoord * 0.15D);
		}
	}

	@Override
	public double getAttackRange(EntityPlayer player) {
		return getRange();
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T extends SkillBase & ISkillModifier> Set<T> getSkillModifiers() {
		return Sets.<T>newHashSet((T) Skills.superSpinAttack);
	}

	@Override
	public <T extends SkillBase & ISkillModifier> void applySkillModifier(T modifier, EntityPlayer player) {
		if (currentSpin < 0.01F) {
			superLevel = modifier.getLevel();
			bonusRange = ((SuperSpinAttack) modifier).getRangeModifier();
		} else if (canRefreshArc() && canSpin(player)) {
			arc += 360F;
		}
	}
}
