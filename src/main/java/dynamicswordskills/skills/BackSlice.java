/**
    Copyright (C) <2015> <coolAlias>

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

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.DamageSource;
import net.minecraft.util.StatCollector;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import dynamicswordskills.client.DSSClientEvents;
import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.bidirectional.ActivateSkillPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModInfo;
import dynamicswordskills.util.ArmorIndex;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;

/**
 * 
 * BACK SLICE
 * Description: Circle around a foe to strike at its vulnerable backside!
 * Activation: Hold left or right arrow key and double-tap forward
 * Exhaustion: Light [1.0F - (level * 0.05F)]
 * Damage: Adds +10% to the damage per level, possibly more against vulnerable enemies
 * Effective Angle: 40 degrees plus 20 per level
 * Special:
 * 		- Considered 'dodging' against current target for the first 5 + (2 * level) ticks
 * 		- Bonus damage only applies when target is struck in the back
 * 		- Some enemies do not take extra damage, usually because they have no back (e.g. slimes)
 * 		- Other enemies may take no damage at all, if they have a hard carapace or some such
 * 		- Chance to remove enemy's chest armor
 */
public class BackSlice extends SkillActive
{
	/** Key that was pressed to initiate movement, either left or right */
	@SideOnly(Side.CLIENT)
	private KeyBinding keyPressed;

	/** Current number of ticks remaining before skill will fail to activate */
	@SideOnly(Side.CLIENT)
	private int ticksTilFail;

	/** Timer during which player may evade incoming attacks */
	private int dodgeTimer = 0;

	/** Used client side to get an extra renderTick for the targeting camera */
	private SkillActive targetingSkill;

	public BackSlice(String name) {
		super(name);
	}

	private BackSlice(BackSlice skill) {
		super(skill);
	}

	@Override
	public BackSlice newInstance() {
		return new BackSlice(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(StatCollector.translateToLocalFormatted(getInfoString("info", 1), 360 - (2 * getAttackAngle())));
		desc.add(StatCollector.translateToLocalFormatted(getInfoString("info", 2),
				String.format("%.2f", getDisarmorChance(null, player.getHeldItem(), level))));
		desc.add(getDamageDisplay(level * 10, true) + "%");
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	/** Number of ticks for which skill will remain active */
	private int getActiveTime() {
		return 15 + level;
	}

	/** Angle at which player will be considered behind the target */
	private int getAttackAngle() {
		return (160 - (level * 10));
	}

	/** Number of ticks for which player will dodge targeted entity's attacks */
	private int getDodgeTime() {
		return 10 - level;
	}

	/**
	 * Returns chance to remove armor, with higher tier armor being more difficult to remove:
	 * each point of armor above 5 reduces the chance by 5%, and each point below 5 increases
	 * the chance by 5%, e.g. diamond plate gives damage reduction 8, so incurs a -15% penalty
	 * to the attacker's chance to disarmor. Each level of Unbreaking adds another -5%.
	 * @param armor	null is allowed to return base chance for addInformation
	 * @param weapon attacking entity's held item adds bonus for Sharpness; null is allowed, adding a -100% penalty
	 * @param level	base chance is 5% per skill level
	 * @return chance that the armor stack will be knocked off of the damaged entity, a value between 0.0F and 1.0F inclusive
	 */
	public static float getDisarmorChance(ItemStack armorStack, ItemStack weapon, int level) {
		float chance = ((float) level * 0.05F);
		if (armorStack != null && armorStack.getItem() instanceof ItemArmor) {
			ItemArmor armor = (ItemArmor) armorStack.getItem();
			int i = armor.getArmorMaterial().getDamageReductionAmount(armor.armorType);
			chance += (float)(5 - i) * 0.05F;
			i = EnchantmentHelper.getEnchantmentLevel(Enchantment.unbreaking.effectId, armorStack);
			if (i > 0) { // -5% per level of Unbreaking
				chance -= (float) i * 0.05F;
			}
		}
		if (weapon != null) {
			int i = EnchantmentHelper.getEnchantmentLevel(Enchantment.sharpness.effectId, weapon);
			if (i > 0) { // +5% per level of Sharpness
				chance += (float) i * 0.05F;
			}
		} else {
			chance -= 1.0F;
		}
		return chance;
	}

	@Override
	public boolean isActive() {
		return (dodgeTimer > 0);
	}

	@Override
	protected float getExhaustion() {
		return (1.0F - (0.1F * level));
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this) && DSSPlayerInfo.get(player).isSkillActive(swordBasic);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return player.onGround && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key) {
		if (isActive()) {
			return (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_ATTACK] || (Config.allowVanillaControls() && key == mc.gameSettings.keyBindAttack));
		}
		return key == mc.gameSettings.keyBindForward || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_LEFT] || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT] ||
				((Config.allowVanillaControls() && (key == mc.gameSettings.keyBindLeft || key == mc.gameSettings.keyBindRight)));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (canExecute(player)) {
			if (keyPressed != null && keyPressed.getIsKeyPressed() && key == mc.gameSettings.keyBindForward) {
				if (ticksTilFail > 0) {
					PacketDispatcher.sendToServer(new ActivateSkillPacket(this));
					ticksTilFail = 0;
					return true;
				} else {
					ticksTilFail = 6;
				}
			} else if (key != mc.gameSettings.keyBindForward) {
				keyPressed = key;
			}
		} else if (isActive() && (key == mc.gameSettings.keyBindAttack || key == DSSKeyHandler.keys[DSSKeyHandler.KEY_ATTACK])) {
			DSSClientEvents.performComboAttack(mc, DSSPlayerInfo.get(player).getTargetingSkill());
		}
		return false; // allow other skills to receive this key press (e.g. Spin Attack)
	}

	@Override
	public boolean onActivated(World world, EntityPlayer player) {
		dodgeTimer = getActiveTime();
		targetingSkill = DSSPlayerInfo.get(player).getActiveSkill(swordBasic);
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		dodgeTimer = 0;
		targetingSkill = null;
		if (world.isRemote) {
			keyPressed = null;
		}
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (isActive()) {
			--dodgeTimer;
		} else if (player.worldObj.isRemote && ticksTilFail > 0) {
			if (--ticksTilFail == 0) {
				keyPressed = null;
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return (dodgeTimer > level);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		if (player.onGround) {
			// force extra camera update so player can more easily hit target:
			if (targetingSkill != null && targetingSkill.isActive()) {
				targetingSkill.onRenderTick(player, partialTickTime);
			}
			double speed = 1.0D + 10.0D * (player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.movementSpeed).getAttributeValue() - Dash.BASE_MOVE);
			if (speed > 1.0D) {
				speed = 1.0D;
			}
			double d = 0.15D * speed * speed;
			if (player.isInWater() || player.handleLavaMovement()) {
				d *= 0.15D;
			}
			Vec3 vec3 = player.getLookVec();
			if (keyPressed == DSSKeyHandler.keys[DSSKeyHandler.KEY_RIGHT] || keyPressed == Minecraft.getMinecraft().gameSettings.keyBindRight) {
				player.addVelocity(-vec3.zCoord * d, 0.0D, vec3.xCoord * d);
			} else {
				player.addVelocity(vec3.zCoord * d, 0.0D, -vec3.xCoord * d);
			}
			player.addVelocity(vec3.xCoord * d * 1.15D, 0.0D, vec3.zCoord * d * 1.15D);
		}
		return false; // allow camera to update again
	}

	@Override
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		ILockOnTarget targeting = DSSPlayerInfo.get(player).getTargetingSkill();
		return (dodgeTimer > getDodgeTime() && targeting != null && targeting.getCurrentTarget() == source.getEntity());
	}

	@Override
	public float postImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		if (isActive() && dodgeTimer <= (getActiveTime() - 5)) { // can strike any time after 5 ticks have passed
			ILockOnTarget targeting = DSSPlayerInfo.get(player).getTargetingSkill();
			if (targeting != null && targeting.getCurrentTarget() == entity) {
				if (!TargetUtils.isTargetInFrontOf(entity, player, getAttackAngle())) {
					amount *= 1.0F + (level * 0.1F);
					PlayerUtils.playSoundAtEntity(player.worldObj, player, ModInfo.SOUND_MORTALDRAW, 0.4F, 0.5F);
					if (Config.canDisarmorPlayers() || !(entity instanceof EntityPlayer)) {
						ItemStack armor = entity.getEquipmentInSlot(ArmorIndex.EQUIPPED_CHEST);
						if (armor != null && player.worldObj.rand.nextFloat() < getDisarmorChance(armor, player.getHeldItem(), level)) {
							PlayerUtils.spawnItemWithRandom(entity.worldObj, armor, entity.posX, entity.posY, entity.posZ);
							entity.setCurrentItemOrArmor(ArmorIndex.EQUIPPED_CHEST, null);
						}
					}
				}
			}
		}
		deactivate(player); // now deactivate on server side; if player missed, they just have to wait
		return amount;
	}
}
