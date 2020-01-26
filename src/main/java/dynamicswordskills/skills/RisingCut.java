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
import dynamicswordskills.ref.Config;
import dynamicswordskills.util.PlayerUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.util.DamageSource;
import net.minecraft.world.World;
import net.minecraftforge.event.entity.living.LivingFallEvent;

/**
 * 
 * RISING CUT
 * Description: Rising slash flings enemy upward
 * Activation: Crouch by pressing the sneak key, then jump and attack
 * Effect: Attacks target for normal sword damage and knocks the target into the air
 * Range: Up to 4 + 0.5 blocks per level
 * Exhaustion: 3.0F - (level * 0.2F)
 * 
 */
public class RisingCut extends SkillActive
{
	/** Flag for activation; set when player jumps while sneaking */
	private int ticksTilFail;

	/** Set when activated and lasts until the player hits the ground or the duration expires */
	private int activeTimer;

	/** True while animation is in progress */
	private int animationTimer;

	/** Stores the entity struck to add velocity on the next update */
	private Entity entityHit;

	/** Flag to prevent a second attack from adding even more motionY */
	private boolean hitEntity;

	public RisingCut(String translationKey) {
		super(translationKey);
	}

	private RisingCut(RisingCut skill) {
		super(skill);
	}

	@Override
	public RisingCut newInstance() {
		return new RisingCut(this);
	}

	@Override
	public boolean displayInGroup(SkillGroup group) {
		return super.displayInGroup(group) || group == Skills.SWORD_GROUP;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void addInformation(List<String> desc, EntityPlayer player) {
		desc.add(getRangeDisplay(4 + (level / 2)));
		desc.add(getExhaustionDisplay(getExhaustion()));
	}

	@Override
	public boolean isActive() {
		return activeTimer > 0;
	}

	@Override
	public boolean isAnimating() {
		return animationTimer > 0;
	}

	@Override
	protected float getExhaustion() {
		return 3.0F - (level * 0.2F);
	}

	/** The amount of upward velocity to apply to affected entities */
	protected double getMotionY() {
		return (0.4D + (0.065D * level));
	}

	private void jump(EntityPlayer player) {
		player.motionY += getMotionY();
		if (player instanceof EntityPlayerMP) {
			((EntityPlayerMP) player).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(player));
		}
	}

	@Override
	public boolean canUse(EntityPlayer player) {
		return super.canUse(player) && !isActive() && !player.isUsingItem() && PlayerUtils.isSwordOrProvider(player.getHeldItem(), this);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean canExecute(EntityPlayer player) {
		return ticksTilFail > 0 && player.motionY > 0.0D && canUse(player);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean isKeyListener(Minecraft mc, KeyBinding key, boolean isLockedOn) {
		if (Config.requiresLockOn() && !isLockedOn) {
			return false;
		}
		return (key == mc.gameSettings.keyBindJump || key == mc.gameSettings.keyBindAttack);
	}

	/**
	 * Flags the skill as ready to be activated when the player next attacks,
	 * provided {@link #canExecute} still returns true at that time
	 */
	@Override
	@SideOnly(Side.CLIENT)
	public boolean keyPressed(Minecraft mc, KeyBinding key, EntityPlayer player) {
		if (key == mc.gameSettings.keyBindJump) {
			if (player.onGround && ticksTilFail == 0 && !isActive() && !player.isUsingItem() && player.isSneaking()) {
				ticksTilFail = 3; // this allows canExecute to return true for 3 ticks
			}
		} else if (canExecute(player) && activate(player)) {
			DSSClientEvents.handlePlayerAttack(mc);
			return true;
		}
		return false;
	}

	@Override
	protected boolean onActivated(World world, EntityPlayer player) {
		// Approximate time it should take to hit the ground with about 5 ticks of leeway
		// If the player misses, they will have a short delay before they can use Rising Cut again
		activeTimer = (20 + 3 * level);
		animationTimer = 5 + level;
		entityHit = null;
		hitEntity = false;
		if (!player.worldObj.isRemote && Config.canHighJump()) {
			jump(player);
		}
		return isActive();
	}

	private boolean canAttack(EntityLivingBase entity) {
		return (!(entity instanceof EntityPlayer) || !PlayerUtils.isBlocking((EntityPlayer) entity));
	}

	@Override
	public boolean onAttack(EntityPlayer player, EntityLivingBase entity, DamageSource source, float amount) {
		if (!hitEntity && canAttack(entity)) {
			hitEntity = true;
			if (!player.worldObj.isRemote && !Config.canHighJump()) {
				jump(player);
			}
		}
		return false;
	}

	@Override
	protected void onDeactivated(World world, EntityPlayer player) {
		activeTimer = 0;
		animationTimer = 0;
		hitEntity = false;
		entityHit = null;
	}

	@Override
	public void onUpdate(EntityPlayer player) {
		if (ticksTilFail > 0) {
			--ticksTilFail;
		}
		if (animationTimer > 0) {
			--animationTimer;
		}
		if (activeTimer > 0) {
			--activeTimer;
			if (player.onGround) {
				if (!player.worldObj.isRemote && (hitEntity || Config.canHighJump())) {
					deactivate(player);
				}
			} else if (entityHit != null) {
				if (!entityHit.isDead) {
					double resist = 1.0D;
					if (entityHit instanceof EntityLivingBase) {
						resist = 1.0D - ((EntityLivingBase) entityHit).getEntityAttribute(SharedMonsterAttributes.knockbackResistance).getAttributeValue();
					}
					double dy = getMotionY() * resist;
					entityHit.addVelocity(0.0D, dy, 0.0D);
					if (entityHit instanceof EntityPlayerMP && !player.worldObj.isRemote) {
						((EntityPlayerMP) entityHit).playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(entityHit));
					}
				}
				entityHit = null;
			}
		}
	}

	@Override
	@SideOnly(Side.CLIENT)
	public boolean onRenderTick(EntityPlayer player, float partialTickTime) {
		player.swingProgress = 0.5F;
		return false;
	}

	/**
	 * Call when an entity is damaged to flag the entity for velocity update next tick.
	 * This is necessary because adding velocity right before the entity is damaged fails.
	 */
	@Override
	public void postImpact(EntityPlayer player, EntityLivingBase entity, float amount) {
		if (canAttack(entity)) {
			entityHit = entity;
		}
	}

	@Override
	public boolean onFall(EntityPlayer player, LivingFallEvent event) {
		if (isActive() && (hitEntity || Config.canHighJump())) {
			event.distance -= (1.0F + level);
			onDeactivated(player.worldObj, player);
		}
		return false;
	}
}
