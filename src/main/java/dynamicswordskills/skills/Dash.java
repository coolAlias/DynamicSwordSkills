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

import dynamicswordskills.client.DSSKeyHandler;
import dynamicswordskills.entity.DSSPlayerInfo;
import dynamicswordskills.network.PacketDispatcher;
import dynamicswordskills.network.server.DashImpactPacket;
import dynamicswordskills.ref.Config;
import dynamicswordskills.ref.ModSounds;
import dynamicswordskills.util.PlayerUtils;
import dynamicswordskills.util.TargetUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.network.play.server.SPacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import swordskillsapi.api.item.IDashItem;

/**
 * 
 * Attacking while blocking and locked on to a target will execute a bash attack.
 * The player charges into the target, inflicting damage and knocking the target back.
 * 
 * Range: 4 blocks plus 1 block per additional level
 * Damage: 2 plus 1 per additional level
 * Knockback: 2 blocks, plus 1 per additional level
 * Exhaustion: Light [1.0F - (level * 0.05F)]
 * Special: Must be at least 2 blocks away from target when skill is activated to
 * 			inflict damage, minus 0.2F per level (down to 1 block at level 5)
 * 
 */
public class Dash extends SkillActive
{
	/** Player's base movement speed */
	public static final double BASE_MOVE = 0.10000000149011612D;

	/** True when Slam is used and while the player is in motion towards the target */
	private boolean isActive = false;

	/** Total distance currently traveled */
	private double distance;

	/**
	 * The dash trajectory is set once when activated, to prevent the vec3 coordinates from
	 * shrinking as the player nears the target; as a bonus, Dash is no longer 'homing'
	 */
	@SideOnly(Side.CLIENT)
	private Vec3d trajectory;

	/** Player's starting position is used to determine actual distance traveled upon impact */
	private Vec3d initialPosition;

	/** Target acquired from ILockOnTarget skill; set to the entity hit upon impact */
	private Entity target;

	/** Impact timer used to make player immune to damage from struck target only, vs. setting hurtResistantTime */
	private int impactTime;

	public Dash(String name) {
		super(name);
	}

	private Dash(Dash skill) {
		super(skill);
	}

	@Override
	public Dash newInstance() {
		return new Dash(this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getDamageDisplay(getDamage(), false));
		desc.add(new TextComponentTranslation(getInfoString("info", 1), 2 + level).getUnformattedText());
		desc.add(new TextComponentTranslation(getRangeDisplay(getRange())).getUnformattedText());
		desc.add(new TextComponentTranslation(getInfoString("info", 2), String.format("%.1f", getMinDistance())).getUnformattedText());
		desc.add(new TextComponentTranslation(getExhaustionDisplay(getExhaustion())).getUnformattedText());
	}

	@Override
	public boolean isActive() {
		return isActive || impactTime > 0;
	}

	@Override
	protected float getExhaustion() {
		return 1.0F - (0.05F * level);
	}

	/** Damage is base damage plus one per level */
	private int getDamage() {
		return (2 + level);
	}

	/** Range increases by 1 block per level */
	private double getRange() {
		return (3.0D + level);
	}

	/** Minimum distance the player must cover before the dash is effective */
	private double getMinDistance() {
		return 2.0D - (0.2D * level);
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		boolean flag = PlayerUtils.isBlocking(player);
		for (EnumHand hand : EnumHand.values()) {
			if (canItemDash(player, hand)) {
				flag = true;
				break;
			}
		}
		return (flag && super.canUse(player) && !isActive());
	}

	/**
	 * Returns true if the item held in the given hand is an {@link IDashItem}
	 * and {@link IDashItem#canDash(ItemStack, EntityPlayer, EnumHand) IDashItem#canDash} returns true.
	 */
	private boolean canItemDash(EntityPlayer player, EnumHand hand) {
		ItemStack stack = player.getHeldItem(hand);
		if (stack != null && stack.getItem() instanceof IDashItem) {
			return ((IDashItem) stack.getItem()).canDash(stack, player, hand);
		}
		return false;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return player.onGround && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key) {
		return (key == DSSKeyHandler.keys[DSSKeyHandler.KEY_ATTACK].getKey() || (Config.allowVanillaControls() && key == mc.gameSettings.keyBindAttack));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		return canExecute(player) && activate(player);
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		isActive = true;
		initialPosition = new Vec3d(player.posX, player.posY + player.getEyeHeight() - 0.10000000149011612D, player.posZ);
		ILockOnTarget skill = DSSPlayerInfo.get(player).getTargetingSkill();
		if (skill != null && skill.isLockedOn()) {
			target = skill.getCurrentTarget();
		} else {
			target = TargetUtils.acquireLookTarget(player, (int) getRange(), getRange(), true);
		}
		if (target != null && world.isRemote) {
			double d0 = (target.posX - player.posX);
			double d1 = (target.posY + (double)(target.height / 3.0F) - player.posY);
			double d2 = (target.posZ - player.posZ);
			trajectory = new Vec3d(d0, d1, d2).normalize();
		}
		return isActive();
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		impactTime = 0; // no longer active, target will be set to null from setNotDashing
		setNotDashing(); // sets all remaining fields to 0 or null
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (impactTime > 0) {
			--impactTime;
			if (impactTime == 0) {
				target = null;
			}
		}

		// don't use isActive() method, as that also returns true after impact
		if (isActive) {
			// Only check for impact on the client, as the server is not reliable for this step
			// If a collision is detected, DashImpactPacket is sent to conclude the server-side
			if (player.worldObj.isRemote) {
				RayTraceResult result = TargetUtils.checkForImpact(player.worldObj, player, player, 0.5D, false);
				if (result != null) {
					PacketDispatcher.sendToServer(new DashImpactPacket(player, result));
					player.resetCooldown(); // player effectively made an attack
					impactTime = 5;
					if (result.typeOfHit == RayTraceResult.Type.ENTITY) {
						target = result.entityHit;
					}
					double d = Math.sqrt((player.motionX * player.motionX) + (player.motionZ * player.motionZ));
					player.setVelocity(-player.motionX * d, 0.15D * d, -player.motionZ * d);
					trajectory = null; // set to null so player doesn't keep moving forward
					setNotDashing();
				}
			}
			// increment distance AFTER update, otherwise Dash thinks it can damage entities right in front of player
			++distance;
			if (distance > (getRange() + 1.0D) || !(target instanceof EntityLivingBase)) {
				setNotDashing();
			}
		}
	}

	/**
	 * Called on the server from {@link DashImpactPacket} to process the impact data from the client
	 * @param player	Player's motionX and motionZ have been set by the packet, so the values may be used
	 * @param mop	Null assumes a block was hit (none of the block data is needed, so it is not sent),
	 * 				or a valid MovingObjectPosition for the entity hit
	 */
	public void onImpact(World world, EntityPlayer player, RayTraceResult result) {
		if (result != null && result.typeOfHit == RayTraceResult.Type.ENTITY) {
			target = result.entityHit;
			double dist = target.getDistance(initialPosition.xCoord, initialPosition.yCoord, initialPosition.zCoord);
			// Subtract half the width for each entity to account for their bounding box size
			dist -= (target.width / 2.0F) + (player.width / 2.0F);

			// Base player speed is 0.1D; heavy boots = 0.04D, pegasus = 0.13D
			double speed = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue();
			double sf = (1.0D + (speed - BASE_MOVE)); // speed factor
			if (speed > 0.075D && dist > getMinDistance() && player.getDistanceSqToEntity(target) < 6.0D) {
				float dmg = (float) getDamage() + (float)((dist / 2.0D) - 2.0D);
				impactTime = 5; // time player will be immune to damage from the target entity
				target.attackEntityFrom(DamageSource.causePlayerDamage(player), (float)(dmg * sf * sf));
				double resist = 1.0D;
				if (target instanceof EntityLivingBase) {
					resist -= ((EntityLivingBase) target).getEntityAttribute(SharedMonsterAttributes.KNOCKBACK_RESISTANCE).getAttributeValue();
				}
				double k = sf * resist * (distance / 3.0F) * 0.6000000238418579D;
				target.addVelocity(player.motionX * k * (0.2D + (0.1D * level)), 0.1D + k * (level * 0.025D), player.motionZ * k * (0.2D + (0.1D * level)));
				if (target instanceof EntityPlayerMP && !player.worldObj.isRemote) {
					((EntityPlayerMP) target).connection.sendPacket(new SPacketEntityVelocity(target));
				}
			}
		}
		PlayerUtils.playSoundAtEntity(player.worldObj, player, ModSounds.SLAM, SoundCategory.PLAYERS, 0.4F, 0.5F);
		setNotDashing();
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isAnimating() {
		return isActive; // don't continue render tick updates after impact
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		if (target instanceof EntityLivingBase && trajectory != null) {
			double speed = player.getAttributeMap().getAttributeInstance(SharedMonsterAttributes.MOVEMENT_SPEED).getAttributeValue() - BASE_MOVE;
			double dfactor = (1.0D + (speed) + (speed * (1.0D - ((getRange() - distance) / getRange()))));
			player.motionX = trajectory.xCoord * dfactor * dfactor;
			player.motionZ = trajectory.zCoord * dfactor * dfactor;
		}
		return false; // this skill doesn't need to control the camera
	}

	@Override
	public boolean onBeingAttacked(EntityPlayer player, DamageSource source) {
		if (impactTime > 0 && source.getEntity() == target) {
			return true;
		} else if (source.damageType.equals("mob") && source.getEntity() != null && player.getDistanceSqToEntity(source.getEntity()) < 6.0D) {
			return true; // stop stupid zombies from hitting player right before impact
		}
		return false;
	}

	/**
	 * After calling this method, {@link #isAnimating()} will always return false;
	 * {@link #isActive()} will return false if no entity was impacted, otherwise it
	 * will still be true for {@link #impactTime} ticks to prevent damage from the {@link #target}. 
	 */
	private void setNotDashing() {
		isActive = false;
		distance = 0.0D;
		initialPosition = null;
		if (!isActive()) {
			target = null;
		}
	}
}
