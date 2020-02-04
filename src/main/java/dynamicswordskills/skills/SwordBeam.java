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
import dynamicswordskills.entity.EntitySwordBeam;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

/**
 * 
 * SWORD BEAM
 * Description: Shoot a beam of energy from the sword tip
 * Activation: Attack while sneaking and at near full health
 * Effect: Shoots a ranged beam capable of damaging one or possibly more targets
 * Damage: 30 + (level * 10) percent of the base sword damage (without other bonuses)
 * Range: Approximately 12 blocks, plus one block per level
 * Exhaustion: 2.0F - (0.1F * level)
 * Special:
 *  - Amount of health required decreases with skill level, down to 1-1/2 hearts below max
 *  - Hitting a target with the beam counts as a direct strike for combos
 *  - At max level, the beam can penetrate multiple targets
 *  - Each additional target receives 20% less damage than the previous
 */
public class SwordBeam extends SkillActive
{
	/** Used to end combo if the sword beam fails to strike a target */
	private int missTimer;

	public SwordBeam(String translationKey) {
		super(translationKey);
	}

	private SwordBeam(SwordBeam skill) {
		super(skill);
	}

	@Override
	public SwordBeam newInstance() {
		return new SwordBeam(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.SWORD_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getDamageDisplay(getDamageFactor(player), false) + "%");
		desc.add(getRangeDisplay(12 + level));
		desc.add(StatCollector.translateToLocalFormatted(getTranslationKey() + ".info.health", String.format("%.1f", Config.getHealthAllowance(level) / 2.0F)));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return false;
	}

	@Override
	public boolean hasAnimation() {
		return false;
	}

	@Override
	protected float getExhaustion() {
		return 2.0F - (0.1F * level);
	}

	/** Returns true if players current health is within the allowed limit */
	private boolean checkHealth(EntityPlayer player) {
		return player.capabilities.isCreativeMode || PlayerUtils.getHealthMissing(player) <= Config.getHealthAllowance(level);
	}

	/** The percent of base sword damage that should be inflicted, as an integer */
	private int getDamageFactor(EntityPlayer player) {
		return 30 + (level * 10);
	}

	/** Returns player's base damage (with sword) plus 1.0F per level */
	private float getDamage(EntityPlayer player) {
		return (float)((double)(getDamageFactor(player)) * 0.01D * player.getEntityAttribute(SharedMonsterAttributes.attackDamage).getAttributeValue());
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && checkHealth(player) && !player.isUsingItem() && DSSPlayerInfo.get(player).canAttack() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this);
	}

	/**
	 * Player must be on ground to prevent conflict with RisingCut
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return player.onGround && player.isSneaking() && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
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
		if (!world.isRemote) {
			missTimer = 12 + level;
			PlayerUtils.playSoundAtEntity(world, player, ModInfo.SOUND_WHOOSH, 0.4F, 0.5F);
			Vec3 vec3 = player.getLookVec();
			EntitySwordBeam beam = new EntitySwordBeam(world, player).setLevel(level).setDamage(getDamage(player));
			beam.setPosition(beam.posX + vec3.xCoord * 0.5, beam.posY + vec3.yCoord * 0.5, beam.posZ + vec3.zCoord * 0.5);
			world.spawnEntityInWorld(beam);
		} else {
			player.swingItem();
			DSSPlayerInfo.get(player).setAttackCooldown(20 - level);
		}
		return true;
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		missTimer = 0;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (missTimer > 0) {
			--missTimer;
			if (missTimer == 0 && !player.worldObj.isRemote) {
				IComboSkill combo = DSSPlayerInfo.get(player).getComboSkill();
				if (combo != null && combo.isComboInProgress()) {
					combo.getCombo().endCombo(player);
				}
			}
		}
	}

	/**
	 * Call from {@link EntitySwordBeam#onImpact} to allow handling of ICombo;
	 * striking an entity sets the missTimer to zero
	 * @param hitBlock true if sword beam hit a block rather than an entity
	 */
	public void onImpact(EntityPlayer player, boolean hitBlock) {
		missTimer = (hitBlock && missTimer > 0 ? 1 : 0);
	}
}
